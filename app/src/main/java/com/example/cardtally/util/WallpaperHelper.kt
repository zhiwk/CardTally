package com.example.cardtally.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.widget.ImageView
import com.example.cardtally.R
import java.io.File
import java.io.InterruptedIOException
import java.lang.ref.WeakReference
import java.util.UUID

/** App-private static wallpaper library. No external URI or persistable permission is retained. */
object WallpaperHelper {
    private const val MAX_SOURCE_BYTES = 32L * 1024 * 1024
    private const val MAX_EDGE = 1600
    const val DEFAULT_IMAGE_ID = "default"
    private const val LEGACY_IMAGE_ID = "legacy"
    private val defaultImages = linkedMapOf(
        DEFAULT_IMAGE_ID to R.drawable.appearance_wallpaper,
        "1c1b789c-7f29-4e9b-a9b7-9db0ba3e14d1" to R.raw.appearance_wallpaper_option_one,
        "1c1b789c-7f29-4e9b-a9b7-9db0ba3e14d2" to R.raw.appearance_wallpaper_option_two
    )
    private val imageIdPattern = Regex("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")
    val defaultImageCount: Int get() = defaultImages.size

    /** Fixed package order; these IDs and numbers survive upgrades and cannot be deleted. */
    fun defaultImageNumber(id: String): Int? =
        defaultImages.keys.indexOf(id).takeIf { it >= 0 }?.plus(1)

    data class ImageEntry(val id: String)
    private var cachedPath: String? = null
    private var cachedBitmap = WeakReference<Bitmap>(null)

    private fun directory(context: Context) = File(context.filesDir, "appearance")
    private fun libraryDirectory(context: Context) = File(directory(context), "wallpapers")
    private fun legacyFile(context: Context) = File(directory(context), "wallpaper.webp")
    private fun selectionFile(context: Context) = File(directory(context), "selected_wallpaper")

    private fun imageFile(context: Context, id: String): File? = when {
        id == LEGACY_IMAGE_ID -> legacyFile(context)
        id in defaultImages -> null
        imageIdPattern.matches(id) -> File(libraryDirectory(context), "$id.webp")
        else -> null
    }

    fun currentImageId(context: Context): String {
        val selection = selectionFile(context)
        if (!selection.isFile) return if (legacyFile(context).isFile) LEGACY_IMAGE_ID else DEFAULT_IMAGE_ID
        val id = try { selection.inputStream().use { input ->
            val bytes = ByteArray(64)
            val count = input.read(bytes)
            if (count > 0) String(bytes, 0, count, Charsets.UTF_8) else DEFAULT_IMAGE_ID
        } } catch (_: Exception) { DEFAULT_IMAGE_ID }
        return if (id in defaultImages || imageFile(context, id)?.isFile == true) id else DEFAULT_IMAGE_ID
    }

    /** Read off the UI thread. The previous single-file wallpaper remains a library entry. */
    fun listImages(context: Context): List<ImageEntry> {
        return buildList {
            defaultImages.keys.forEach { add(ImageEntry(it)) }
            if (legacyFile(context).isFile) add(ImageEntry(LEGACY_IMAGE_ID))
            libraryDirectory(context).listFiles()?.filter {
                it.isFile && it.extension == "webp" && imageIdPattern.matches(it.nameWithoutExtension) &&
                    it.nameWithoutExtension !in defaultImages
            }?.sortedWith(compareBy<File> { it.lastModified() }.thenBy { it.name })?.forEach {
                add(ImageEntry(it.nameWithoutExtension))
            }
        }
    }

    fun hasCustomImage(context: Context): Boolean = currentImageId(context) != DEFAULT_IMAGE_ID

    fun bindImage(image: ImageView) {
        val id = currentImageId(image.context)
        val resource = defaultImages[id]
        val bitmap = if (resource != null) {
            loadBitmap(image.context, resource)
        } else imageFile(image.context, id)?.takeIf { it.isFile }?.let(::loadBitmap)
        if (bitmap != null) image.setImageBitmap(bitmap) else image.setImageResource(R.drawable.appearance_wallpaper)
    }

    /** Files are already validated on import; selection only atomically updates a local ID. */
    fun selectImage(context: Context, id: String) {
        require(id in defaultImages || imageFile(context, id)?.isFile == true)
        val directory = directory(context)
        check(directory.isDirectory || directory.mkdirs())
        val staged = File.createTempFile("selection_", ".tmp", directory)
        try {
            staged.outputStream().use { output ->
                output.write(id.toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            checkInterrupted()
            check(staged.renameTo(selectionFile(context))) { "Cannot select wallpaper" }
            clearCache()
        } finally {
            staged.delete()
        }
    }

    /** Bounded thumbnails; caller owns caching and dispatches decoding off the UI thread. */
    fun loadThumbnail(context: Context, id: String): Bitmap? = try {
        checkInterrupted()
        val resource = defaultImages[id]
        if (resource != null) decodeResourceSampled(context, resource, 512)
        else imageFile(context, id)?.let { decodeSampled(it, 512) }
    } catch (_: Exception) { null } catch (_: OutOfMemoryError) { null }

    private fun loadBitmap(file: File): Bitmap? =
        loadCachedBitmap("${file.absolutePath}:${file.lastModified()}:${file.length()}") { decodeSampled(file) }

    private fun loadBitmap(context: Context, resource: Int): Bitmap? =
        loadCachedBitmap("resource:$resource") { decodeResourceSampled(context, resource) }

    @Synchronized
    private fun loadCachedBitmap(key: String, decode: () -> Bitmap?): Bitmap? {
        if (cachedPath == key) cachedBitmap.get()?.takeUnless { it.isRecycled }?.let { return it }
        val bitmap = try {
            decode()
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
        cachedPath = key
        cachedBitmap = WeakReference<Bitmap>(bitmap)
        return bitmap
    }

    @Synchronized
    private fun clearCache() {
        cachedPath = null
        cachedBitmap.clear()
    }

    /** Called off the UI thread; cancellation or failure leaves the previous wallpaper untouched. */
    fun importImage(context: Context, uri: Uri) {
        val id = UUID.randomUUID().toString()
        val target = requireNotNull(imageFile(context, id))
        val directory = requireNotNull(target.parentFile)
        check(directory.isDirectory || directory.mkdirs())
        val source = File.createTempFile("source_", ".image", directory)
        var staged: File? = null
        var bitmap: Bitmap? = null
        var selected = false
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                source.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        checkInterrupted()
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_SOURCE_BYTES) { "Image exceeds import limit" }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: error("Image is unavailable")
            checkInterrupted()
            val decoded = decodeSampled(source) ?: error("Invalid image")
            bitmap = decoded
            checkInterrupted()
            val orientation = try {
                ExifInterface(source.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }
            bitmap = orient(decoded, orientation)
            if (bitmap !== decoded) decoded.recycle()
            checkInterrupted()
            val outputFile = File.createTempFile("ready_", ".webp", directory)
            staged = outputFile
            outputFile.outputStream().use { output ->
                @Suppress("DEPRECATION")
                check(requireNotNull(bitmap).compress(Bitmap.CompressFormat.WEBP, 90, output))
                output.fd.sync()
            }
            checkInterrupted()
            // Commit a new immutable entry, then switch the selection without replacing older images.
            check(outputFile.renameTo(target)) { "Cannot save wallpaper" }
            selectImage(context, id)
            selected = true
        } finally {
            bitmap?.recycle()
            source.delete()
            staged?.delete()
            if (!selected) target.delete()
        }
    }

    fun restoreDefault(context: Context) = selectImage(context, DEFAULT_IMAGE_ID)

    /** Remove only an imported entry; all three packaged defaults are protected. */
    fun deleteImage(context: Context, id: String) {
        require(id !in defaultImages) { "Default wallpapers cannot be deleted" }
        val file = requireNotNull(imageFile(context, id))
        check(file.isFile) { "Wallpaper is unavailable" }
        checkInterrupted()
        val wasSelected = currentImageId(context) == id
        // Persist the fallback before removing the active file; a failed save leaves the file intact.
        if (wasSelected) selectImage(context, DEFAULT_IMAGE_ID)
        try {
            check(file.delete()) { "Cannot delete wallpaper" }
        } catch (failure: Exception) {
            if (wasSelected) {
                try {
                    selectImage(context, id)
                } catch (rollback: Exception) {
                    failure.addSuppressed(rollback)
                }
            }
            throw failure
        }
        clearCache()
    }

    private fun decodeResourceSampled(context: Context, resource: Int, maxEdge: Int = MAX_EDGE): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
        BitmapFactory.decodeResource(context.resources, resource, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        options.inSampleSize = sampleSize(options.outWidth, options.outHeight, maxEdge)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(context.resources, resource, options)
    }

    private fun decodeSampled(file: File, maxEdge: Int = MAX_EDGE): Bitmap? {
        if (file.length() > MAX_SOURCE_BYTES) return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        options.inSampleSize = sampleSize(options.outWidth, options.outHeight, maxEdge)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(file.path, options)
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (maxOf(width, height) / sample > maxEdge) sample *= 2
        return sample
    }

    private fun orient(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun checkInterrupted() {
        if (Thread.currentThread().isInterrupted) throw InterruptedIOException("Wallpaper import cancelled")
    }
}
