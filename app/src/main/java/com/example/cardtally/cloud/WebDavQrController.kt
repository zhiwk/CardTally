package com.example.cardtally.cloud

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.cardtally.R
import com.journeyapps.barcodescanner.ScanOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** QR transfer edits only the host's draft. It never saves credentials or calls a DAV server. */
internal class WebDavQrController(
    private val fragment: Fragment,
    private val readForm: (includeBackupPassword: Boolean) -> WebDavQrConnection?,
    private val fillForm: (WebDavQrConnection) -> Unit
) {
    private var root: View? = null
    private var dialog: AlertDialog? = null
    private var executor: ExecutorService? = null
    private var task: Future<*>? = null
    private val main = Handler(Looper.getMainLooper())
    private var generation = 0
    private var busy = false
    private var cloudBusy = false
    private val buttons = listOf(R.id.cloud_qr_share, R.id.cloud_qr_import)

    // Registered with the Fragment before creation, in a stable order across recreation.
    private val camera = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val payload = WebDavQrScanResults.take(result.data?.getStringExtra(WebDavQrScanResults.EXTRA_TOKEN))
            if (canAct()) {
                try { preview(WebDavQrCodec.decode(requireNotNull(payload))) }
                catch (_: Exception) { toast(R.string.cloud_qr_invalid) }
            }
        }
    }
    private val picture = fragment.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && canAct()) {
            val app = fragment.requireContext().applicationContext
            background(R.string.cloud_qr_image_failed,
                action = { WebDavQrCodec.decode(WebDavQrImages.read(app, uri)) },
                success = ::preview)
        }
    }
    private val permission = fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && canAct()) scanCamera()
        else if (!granted) toast(R.string.cloud_qr_camera_denied)
    }

    fun bind(view: View) {
        root = view
        cloudBusy = false
        executor = Executors.newSingleThreadExecutor()
        val app = fragment.requireContext().applicationContext
        executor?.execute { WebDavQrImages.clearExpiredShares(app) }
        view.findViewById<View>(R.id.cloud_qr_share).setOnClickListener { consent() }
        view.findViewById<View>(R.id.cloud_qr_import).setOnClickListener {
            if (canAct()) show(AlertDialog.Builder(fragment.requireContext())
                .setTitle(R.string.cloud_qr_import)
                .setItems(arrayOf(fragment.getString(R.string.cloud_qr_scan_camera),
                    fragment.getString(R.string.cloud_qr_scan_image))) { _, which ->
                    if (!canAct()) return@setItems
                    if (which == 0) requestCamera()
                    else try { picture.launch(arrayOf("image/*")) }
                    catch (_: Exception) { toast(R.string.cloud_qr_image_failed) }
                }.setNegativeButton(android.R.string.cancel, null).create())
        }
        updateButtons()
    }

    fun setCloudBusy(value: Boolean) {
        cloudBusy = value
        updateButtons()
    }

    private fun canAct(): Boolean = root != null && !busy && !cloudBusy &&
        fragment.isAdded && !fragment.parentFragmentManager.isStateSaved

    private fun requestCamera() {
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) scanCamera()
        else permission.launch(Manifest.permission.CAMERA)
    }

    private fun scanCamera() {
        try {
            camera.launch(ScanOptions().setCaptureActivity(WebDavQrCaptureActivity::class.java)
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setOrientationLocked(false).setBeepEnabled(false).setBarcodeImageEnabled(false)
                .setPrompt(fragment.getString(R.string.cloud_qr_scan_prompt))
                .createScanIntent(fragment.requireContext()))
        } catch (_: Exception) { toast(R.string.cloud_qr_camera_failed) }
    }

    private fun consent() {
        if (!canAct()) return
        val content = fragment.layoutInflater.inflate(R.layout.dialog_webdav_qr_consent, null)
        val include = content.findViewById<CheckBox>(R.id.cloud_qr_include_password)
        include.isChecked = false
        include.setOnCheckedChangeListener { _, checked ->
            content.findViewById<View>(R.id.cloud_qr_full_warning).visibility = if (checked) View.VISIBLE else View.GONE
        }
        val prompt = AlertDialog.Builder(fragment.requireContext()).setTitle(R.string.cloud_qr_share)
            .setView(content).setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.cloud_qr_generate, null).create()
        show(prompt)
        prompt.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (!canAct()) return@setOnClickListener
            val connection = readForm(include.isChecked)
            // Inline errors belong to the cloud form, not a second credential editor.
            if (connection == null) { prompt.dismiss(); return@setOnClickListener }
            val payload = try { WebDavQrCodec.encode(connection) }
            catch (_: Exception) { toast(R.string.cloud_qr_generate_failed); return@setOnClickListener }
            prompt.dismiss()
            background(R.string.cloud_qr_generate_failed, action = { WebDavQrImages.generate(payload) },
                success = ::displayCode, discard = { it.bitmap.recycle() })
        }
    }

    private fun displayCode(code: WebDavQrImage) {
        val content = fragment.layoutInflater.inflate(R.layout.dialog_webdav_qr_code, null)
        val image = content.findViewById<ImageView>(R.id.cloud_qr_image)
        image.setImageBitmap(code.bitmap)
        val display = AlertDialog.Builder(fragment.requireContext()).setTitle(R.string.cloud_qr_share)
            .setView(content).setNegativeButton(R.string.cloud_qr_close, null)
            .setPositiveButton(R.string.cloud_qr_share_image, null).create()
        show(display) { current ->
            if (current) cancelTask()
            image.setImageDrawable(null)
            code.bitmap.recycle()
        }
        display.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (!canAct()) return@setOnClickListener
            val app = fragment.requireContext().applicationContext
            background(R.string.cloud_qr_share_failed,
                action = { WebDavQrImages.shareFile(app, code.png) },
                success = { file ->
                    try {
                        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            clipData = ClipData.newUri(app.contentResolver, "WebDAV QR", uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        fragment.startActivity(Intent.createChooser(send, fragment.getString(R.string.cloud_qr_share_image)))
                    } catch (_: Exception) { file.delete(); toast(R.string.cloud_qr_share_failed) }
                }, discard = { it.delete() })
        }
    }

    private fun preview(connection: WebDavQrConnection) {
        val message = fragment.getString(R.string.cloud_qr_import_summary, connection.endpoint,
            connection.user, connection.prefix, fragment.getString(if (connection.backupPassword == null)
                R.string.cloud_qr_password_missing else R.string.cloud_qr_password_included))
        show(AlertDialog.Builder(fragment.requireContext()).setTitle(R.string.cloud_qr_import_preview)
            .setMessage(message).setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.cloud_qr_fill_form) { _, _ ->
                if (canAct()) fillForm(connection)
            }.create())
    }

    private fun show(next: AlertDialog, onDismiss: (current: Boolean) -> Unit = {}) {
        dialog?.dismiss()
        dialog = next
        // These windows must not leak their QR/connection preview into screenshots or recents.
        next.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        next.setOnDismissListener {
            val current = dialog === next
            if (current) dialog = null
            onDismiss(current)
        }
        next.show()
    }

    private fun <T : Any> background(
        failure: Int,
        action: () -> T,
        success: (T) -> Unit,
        discard: (T) -> Unit = {}
    ) {
        if (!canAct()) return
        val token = ++generation
        busy = true
        updateButtons()
        task = executor?.submit {
            val result: Result<T> = try { Result.success(action()) }
            catch (error: Exception) { Result.failure(error) }
            catch (_: OutOfMemoryError) { Result.failure(IllegalStateException("QR image memory limit")) }
            main.post {
                if (token != generation || root == null) {
                    result.getOrNull()?.let(discard)
                    return@post
                }
                task = null
                busy = false
                updateButtons()
                if (!canAct()) result.getOrNull()?.let(discard)
                else result.fold(onSuccess = success, onFailure = { toast(failure) })
            }
        }
    }

    private fun updateButtons() {
        val enabled = !busy && !cloudBusy
        buttons.forEach { root?.findViewById<View>(it)?.isEnabled = enabled }
        root?.findViewById<View>(R.id.cloud_qr_progress)?.visibility = if (busy) View.VISIBLE else View.GONE
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = enabled
    }

    private fun cancelTask() {
        generation++
        task?.cancel(true)
        task = null
        busy = false
        updateButtons()
    }

    fun destroyView() {
        root = null
        cancelTask()
        dialog?.dismiss()
        dialog = null
        executor?.shutdownNow()
        executor = null
    }

    private fun toast(message: Int) {
        fragment.context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
    }
}
