package com.example.cardtally.cloud

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InterruptedIOException
import java.util.UUID

internal class WebDavQrImage(val bitmap: Bitmap, val png: ByteArray)

/** Call only on a worker thread. Imported images and generated codes never leave the device here. */
internal object WebDavQrImages {
    private const val MAX_IMAGE_BYTES = 16 * 1024 * 1024
    private const val MAX_IMAGE_EDGE = 2048
    private const val SHARE_DIRECTORY = "webdav_qr"
    private const val SHARE_MAX_AGE_MS = 60 * 60 * 1000L

    fun generate(payload: String): WebDavQrImage {
        require(payload.toByteArray(Charsets.UTF_8).size <= WebDavQrCodec.MAX_BYTES)
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 1024, 1024,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 4,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
        val pixels = IntArray(matrix.width * matrix.height)
        for (y in 0 until matrix.height) {
            checkInterrupted()
            for (x in 0 until matrix.width) {
                pixels[y * matrix.width + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        try {
            val output = ByteArrayOutputStream()
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            return WebDavQrImage(bitmap, output.toByteArray())
        } catch (error: Exception) {
            bitmap.recycle()
            throw error
        }
    }

    fun read(context: Context, uri: Uri): String {
        // Bound even providers with an absent or dishonest size, before decoding any image.
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                checkInterrupted()
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= MAX_IMAGE_BYTES) { "QR image too large" }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: throw IllegalArgumentException("Unreadable QR image")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0 &&
            bounds.outWidth.toLong() * bounds.outHeight <= 64_000_000L) { "Invalid QR image dimensions" }
        var sample = 1
        while ((bounds.outWidth + sample - 1) / sample > MAX_IMAGE_EDGE ||
            (bounds.outHeight + sample - 1) / sample > MAX_IMAGE_EDGE) sample *= 2
        checkInterrupted()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 })
            ?: throw IllegalArgumentException("Unreadable QR image")
        val source = try {
            require(bitmap.width <= MAX_IMAGE_EDGE && bitmap.height <= MAX_IMAGE_EDGE)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            // Transparent image areas are white, not black, for QR decoding.
            for (i in pixels.indices) {
                val color = pixels[i]
                val alpha = Color.alpha(color)
                if (alpha < 255) pixels[i] = Color.rgb(
                    (Color.red(color) * alpha + 255 * (255 - alpha)) / 255,
                    (Color.green(color) * alpha + 255 * (255 - alpha)) / 255,
                    (Color.blue(color) * alpha + 255 * (255 - alpha)) / 255)
            }
            RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        } finally { bitmap.recycle() }
        val hints = mapOf(DecodeHintType.TRY_HARDER to true, DecodeHintType.CHARACTER_SET to "UTF-8")
        for (candidate in listOf(source, source.invert())) {
            checkInterrupted()
            val reader = QRCodeReader()
            try {
                return reader.decode(BinaryBitmap(HybridBinarizer(candidate)), hints).text
            } catch (_: com.google.zxing.ReaderException) {
                // Try inverted images as well; do not expose decoder output in errors.
            } finally { reader.reset() }
        }
        throw NotFoundException.getNotFoundInstance()
    }

    fun shareFile(context: Context, png: ByteArray): File {
        clearExpiredShares(context)
        val directory = File(context.cacheDir, SHARE_DIRECTORY)
        check(directory.isDirectory || directory.mkdirs())
        val file = File(directory, "webdav-${UUID.randomUUID()}.png")
        try {
            file.outputStream().use { it.write(png) }
            checkInterrupted()
            return file
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }

    /** Keep files long enough for asynchronous share receivers; clean only our own old PNGs. */
    fun clearExpiredShares(context: Context) {
        val cutoff = System.currentTimeMillis() - SHARE_MAX_AGE_MS
        File(context.cacheDir, SHARE_DIRECTORY).listFiles()?.forEach { file ->
            if (file.name.matches(Regex("webdav-[a-f0-9-]+\\.png")) && file.lastModified() < cutoff) file.delete()
        }
    }

    private fun checkInterrupted() {
        if (Thread.currentThread().isInterrupted) throw InterruptedIOException("QR task cancelled")
    }
}
