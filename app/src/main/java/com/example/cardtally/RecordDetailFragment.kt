package com.example.cardtally

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.util.IncomeExpenseColorScheme
import com.example.cardtally.util.Money
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import kotlin.math.roundToInt

class RecordDetailFragment : Fragment() {
    companion object {
        private const val KEY_RECORD_ID = "record_id"
        fun newInstance(recordId: Long) = RecordDetailFragment().apply {
            arguments = Bundle().apply { putLong(KEY_RECORD_ID, recordId) }
        }
    }

    private var databaseHelper: DatabaseHelper? = null
    private var imageExecutor: ExecutorService? = null
    private var photoDialog: Dialog? = null
    private var deleteDialog: androidx.appcompat.app.AlertDialog? = null
    private var leaving = false
    private val recordId get() = requireArguments().getLong(KEY_RECORD_ID)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        leaving = false
        imageExecutor = Executors.newSingleThreadExecutor()
        return inflater.inflate(R.layout.fragment_record_detail, container, false).apply {
            findViewById<View>(R.id.button_detail_back).setOnClickListener { returnToSource() }
            findViewById<View>(R.id.button_detail_edit).setOnClickListener {
                if (leaving) return@setOnClickListener
                val record = databaseHelper?.getRecordById(recordId)
                if (record == null) {
                    missingRecord()
                } else {
                    leaving = true
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, EditRecordFragment.newInstance(record.id))
                        .addToBackStack(null)
                        .commit()
                }
            }
            findViewById<View>(R.id.button_detail_delete).setOnClickListener { confirmDelete() }
        }
    }

    override fun onResume() {
        super.onResume()
        leaving = false
        refreshRecord()
    }

    private fun refreshRecord() {
        val screen = view ?: return
        val ledgerId = DatabaseHelper(requireContext()).use { it.getLedgerIdForRecord(recordId) }
        if (ledgerId == null) {
            missingRecord()
            return
        }
        databaseHelper?.close()
        val helper = DatabaseHelper(requireContext(), ledgerId).also { databaseHelper = it }
        val record = helper.getRecordById(recordId) ?: run { missingRecord(); return }
        val details = screen.findViewById<LinearLayout>(R.id.layout_record_details)
        details.removeAllViews()
        details.addView(TextView(requireContext()).apply {
            text = getString(R.string.record_detail_amount)
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.onSurfaceVariant_light))
        })
        val amount = TextView(requireContext()).apply {
            text = when (record.type) {
                0 -> "-¥${Money.formatYuan(record.amount)}"
                1 -> "+¥${Money.formatYuan(record.amount)}"
                else -> "¥${Money.formatYuan(record.amount)}"
            }
            textSize = 28f
            setTextColor(when (record.type) {
                0 -> IncomeExpenseColorScheme.expensePrimary(context)
                1 -> IncomeExpenseColorScheme.incomePrimary(context)
                else -> ContextCompat.getColor(context, R.color.onSurface_light)
            })
            setPadding(0, 0, 0, dp(16))
        }
        details.addView(amount)
        addRow(details, R.string.record_detail_type, getString(when (record.type) {
            1 -> R.string.record_type_income
            2 -> R.string.record_type_transfer
            else -> R.string.record_type_expense
        }))
        addRow(details, R.string.record_detail_date, record.date)
        addRow(details, R.string.record_detail_ledger, helper.getCurrentLedger()?.name)
        val assets = (helper.getAllAssets() + helper.getArchivedAssets()).associateBy { it.id }
        fun assetName(id: Long?, snapshot: String?) = id?.let { assets[it]?.name } ?: snapshot
        if (record.type == 2) {
            addRow(details, R.string.record_transfer_from_asset, assetName(record.assetId, record.assetSource))
            addRow(details, R.string.record_destination_asset_label, assetName(record.destinationAssetId, record.destinationAssetSource))
            addRow(details, R.string.record_fee_label, "¥${Money.formatYuan(record.fee)}")
        } else {
            val categoryPath = record.categoryId?.let { id ->
                helper.getCategoryById(id)?.let { helper.buildCategoryPathLabel(id) }
            } ?: record.categoryPathSnapshot?.takeIf { it.isNotBlank() }
                ?: record.categoryNameSnapshot?.takeIf { it.isNotBlank() } ?: record.category
            addRow(details, R.string.record_category_label, categoryPath)
            addRow(details, R.string.record_asset_label, assetName(record.assetId, record.assetSource))
        }
        addRow(details, R.string.record_detail_note, record.description)
        val photos = record.photoUris.ifEmpty { record.photoUri?.let(::listOf).orEmpty() }
        val grid = screen.findViewById<RecyclerView>(R.id.recycler_detail_photos)
        grid.layoutManager = GridLayoutManager(requireContext(), 3)
        grid.adapter = PhotoAdapter(photos)
        grid.visibility = if (photos.isEmpty()) View.GONE else View.VISIBLE
        screen.findViewById<View>(R.id.text_detail_attachments).visibility = grid.visibility
    }

    private fun addRow(parent: LinearLayout, label: Int, value: String?) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
        }
        row.addView(TextView(requireContext()).apply {
            text = getString(label)
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.onSurfaceVariant_light))
        }, LinearLayout.LayoutParams(dp(88), ViewGroup.LayoutParams.WRAP_CONTENT))
        row.addView(TextView(requireContext()).apply {
            text = value?.takeIf { it.isNotBlank() } ?: getString(R.string.record_detail_empty)
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.onSurface_light))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        parent.addView(row)
    }

    private fun confirmDelete() {
        if (leaving || deleteDialog?.isShowing == true) return
        deleteDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_record_title)
            .setMessage(R.string.delete_record_message)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                val helper = databaseHelper ?: return@setPositiveButton
                leaving = true
                val success = runCatching {
                    helper.deleteRecord(recordId)
                    // A successful transfer deletion returns no undo token.
                    helper.getRecordById(recordId) == null
                }.getOrDefault(false)
                if (success) {
                    Toast.makeText(requireContext(), R.string.toast_delete_success, Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    leaving = false
                    Toast.makeText(requireContext(), R.string.record_detail_delete_failed, Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun missingRecord() {
        if (leaving) return
        Toast.makeText(requireContext(), R.string.record_detail_missing, Toast.LENGTH_SHORT).show()
        returnToSource()
    }

    private fun returnToSource() {
        if (leaving) return
        leaving = true
        parentFragmentManager.popBackStack()
    }

    private inner class PhotoAdapter(private val photos: List<String>) : RecyclerView.Adapter<PhotoHolder>() {
        override fun getItemCount() = photos.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
            val frame = FrameLayout(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(112)).apply {
                    setMargins(dp(4), dp(4), dp(4), dp(4))
                }
            }
            val image = ImageView(parent.context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                isFocusable = true
            }
            val unavailable = TextView(parent.context).apply {
                text = getString(R.string.record_detail_photo_unavailable)
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, R.color.onSurface_light))
            }
            frame.addView(image, FrameLayout.LayoutParams(-1, -1))
            frame.addView(unavailable, FrameLayout.LayoutParams(-1, -1))
            return PhotoHolder(frame, image, unavailable)
        }

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            val uri = photos[position]
            holder.image.tag = uri
            holder.image.setImageDrawable(null)
            holder.unavailable.visibility = View.GONE
            holder.image.contentDescription = getString(R.string.record_photo_preview_description, position + 1)
            holder.image.setOnClickListener { showPhoto(uri) }
            val resolver = requireContext().contentResolver
            val size = dp(160)
            imageExecutor?.execute {
                val bitmap = decodePhoto(resolver, uri, size)
                holder.image.post {
                    if (view != null && holder.image.tag == uri) {
                        holder.image.setImageBitmap(bitmap)
                        holder.unavailable.visibility = if (bitmap == null) View.VISIBLE else View.GONE
                    }
                }
            }
        }

        override fun onViewRecycled(holder: PhotoHolder) {
            holder.image.tag = null
            holder.image.setImageDrawable(null)
            holder.image.setOnClickListener(null)
        }
    }

    private class PhotoHolder(view: View, val image: ImageView, val unavailable: TextView) : RecyclerView.ViewHolder(view)

    private fun showPhoto(uri: String) {
        if (photoDialog?.isShowing == true) return
        val dialog = Dialog(requireContext())
        val image = ImageView(requireContext()).apply {
            setBackgroundColor(Color.BLACK)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = getString(R.string.record_photo_fullscreen_description)
            setOnClickListener { dialog.dismiss() }
        }
        dialog.setContentView(image)
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.black)
        dialog.window?.setLayout(-1, -1)
        photoDialog = dialog
        val resolver = requireContext().contentResolver
        imageExecutor?.execute {
            val bitmap = decodePhoto(resolver, uri, 2048)
            image.post {
                if (view != null && dialog.isShowing) {
                    if (bitmap == null) {
                        dialog.dismiss()
                        Toast.makeText(requireContext(), R.string.record_detail_photo_unavailable, Toast.LENGTH_SHORT).show()
                    } else image.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun decodePhoto(resolver: android.content.ContentResolver, uriString: String, edge: Int): Bitmap? = runCatching {
        val uri = Uri.parse(uriString)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth <= 0 || options.outHeight <= 0) return@runCatching null
        var sample = 1
        while (maxOf(options.outWidth, options.outHeight) / sample > edge) sample *= 2
        options.inSampleSize = sample
        options.inJustDecodeBounds = false
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    override fun onDestroyView() {
        photoDialog?.dismiss()
        photoDialog = null
        deleteDialog?.dismiss()
        deleteDialog = null
        imageExecutor?.shutdownNow()
        imageExecutor = null
        view?.findViewById<RecyclerView>(R.id.recycler_detail_photos)?.adapter = null
        databaseHelper?.close()
        databaseHelper = null
        super.onDestroyView()
    }
}
