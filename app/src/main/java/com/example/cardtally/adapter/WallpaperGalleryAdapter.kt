package com.example.cardtally.adapter

import android.graphics.Bitmap
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.util.ThemeColorHelper
import com.example.cardtally.util.WallpaperHelper
import com.google.android.material.card.MaterialCardView
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Only visible thumbnails decode; full-size wallpaper bitmaps never enter the gallery cache. */
class WallpaperGalleryAdapter(
    private val onSelect: (String) -> Unit,
    private val onDelete: (String) -> Unit
) :
    RecyclerView.Adapter<WallpaperGalleryAdapter.Holder>() {
    private var entries = emptyList<WallpaperHelper.ImageEntry>()
    private var selectedId = WallpaperHelper.DEFAULT_IMAGE_ID
    private var enabled = true
    @Volatile private var closed = false
    private val holders = mutableSetOf<Holder>()
    private val executor = Executors.newSingleThreadExecutor()
    private val cache = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    init {
        // Keep RecyclerView's saved scroll state until the background directory read completes.
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    fun submitImages(images: List<WallpaperHelper.ImageEntry>, selected: String) {
        entries = images
        selectedId = selected
        val retainedIds = images.map { it.id }.toSet()
        cache.snapshot().keys.filterNot(retainedIds::contains).forEach { cache.remove(it) }
        notifyDataSetChanged()
    }

    fun setSelectionEnabled(value: Boolean) {
        enabled = value
        holders.forEach {
            it.itemView.isEnabled = value
            it.delete.isEnabled = value && it.boundId?.let(WallpaperHelper::defaultImageNumber) == null
        }
    }

    override fun getItemCount() = entries.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_wallpaper_image, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holders.add(holder)
        holder.clear()
        val entry = entries[position]
        val context = holder.itemView.context
        val defaultNumber = WallpaperHelper.defaultImageNumber(entry.id)
        val label = if (defaultNumber != null) {
            context.getString(R.string.theme_wallpaper_default, defaultNumber)
        } else context.getString(R.string.theme_wallpaper_image_number, position - WallpaperHelper.defaultImageCount + 1)
        val selected = entry.id == selectedId
        holder.title.text = label
        holder.marker.visibility = if (selected) View.VISIBLE else View.INVISIBLE
        holder.card.strokeWidth = if (selected) (2 * context.resources.displayMetrics.density).toInt() else 0
        holder.card.setStrokeColor(ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary))
        holder.itemView.isSelected = selected
        holder.itemView.isEnabled = enabled
        holder.itemView.contentDescription = if (selected) context.getString(R.string.theme_wallpaper_selected_image, label) else label
        holder.itemView.setOnClickListener { if (enabled && !closed) onSelect(entry.id) }
        holder.delete.visibility = if (defaultNumber != null) View.GONE else View.VISIBLE
        holder.delete.isEnabled = enabled && defaultNumber == null
        holder.delete.contentDescription = context.getString(R.string.theme_wallpaper_delete_image, label)
        holder.delete.setOnClickListener {
            if (enabled && !closed && defaultNumber == null) onDelete(entry.id)
        }
        holder.boundId = entry.id
        cache.get(entry.id)?.let { holder.image.setImageBitmap(it); return }
        val appContext = context.applicationContext
        holder.task = executor.submit {
            val bitmap = WallpaperHelper.loadThumbnail(appContext, entry.id)
            if (closed || Thread.currentThread().isInterrupted) return@submit
            if (bitmap != null) cache.put(entry.id, bitmap)
            holder.image.post {
                if (!closed && holder.boundId == entry.id) {
                    if (bitmap != null) holder.image.setImageBitmap(bitmap)
                }
            }
        }
    }

    override fun onViewRecycled(holder: Holder) {
        holder.clear()
        holders.remove(holder)
        super.onViewRecycled(holder)
    }

    fun close() {
        closed = true
        holders.forEach { it.clear() }
        holders.clear()
        executor.shutdownNow()
        // Eviction drops references; do not recycle bitmaps still held by an ImageView.
        cache.evictAll()
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.card_wallpaper_thumbnail)
        val image: ImageView = view.findViewById(R.id.image_wallpaper_thumbnail)
        val title: TextView = view.findViewById(R.id.text_wallpaper_image)
        val marker: TextView = view.findViewById(R.id.text_wallpaper_selected)
        val delete: View = view.findViewById(R.id.button_delete_wallpaper)
        var boundId: String? = null
        var task: Future<*>? = null
        fun clear() {
            boundId = null
            task?.cancel(true)
            task = null
            image.setImageDrawable(null)
        }
    }
}
