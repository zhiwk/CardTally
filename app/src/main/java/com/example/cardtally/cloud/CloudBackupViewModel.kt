package com.example.cardtally.cloud

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cardtally.util.BackupArchiveManager
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

internal class CloudBackupViewModel : ViewModel() {
    data class Ui(val busy: Boolean = false, val message: String = "idle", val bytes: Long = 0,
        val expected: Long = 0, val listing: CloudListing? = null, val preview: BackupArchiveManager.MergeResult? = null,
        val snapshot: CloudSnapshot? = null, val useBalances: Boolean = false, val httpStatus: Int? = null, val httpMethod: String = "")
    val state = MutableLiveData(Ui())
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val generation = AtomicInteger()
    private var task: Future<*>? = null
    private var prepared: java.io.File? = null
    private var preparedName: String? = null
    @Volatile private var manager: CloudBackupManager? = null
    fun start(context: Context, config: CloudConfig, action: String, snapshot: CloudSnapshot? = null, useBalances: Boolean = false) {
        if (state.value?.busy == true) return
        val token = generation.incrementAndGet(); val app = context.applicationContext
        state.value = Ui(true, action)
        task = executor.submit {
            try {
                if (action == "restore") {
                    val archive = requireNotNull(prepared).also { require(it.isFile && preparedName == snapshot?.name) }
                    val result = archive.inputStream().use { BackupArchiveManager(app).importFrom(it, useBackupBalances = useBalances) }
                    archive.delete(); prepared = null; preparedName = null
                    emit(token, Ui(message = "restored", preview = result))
                    return@submit
                }
                if (action != "preview") { prepared?.delete(); prepared = null; preparedName = null }
                CloudBackupManager(app, config, progress = { message, bytes, expected ->
                    emit(token, Ui(true, message, bytes, expected))
                }).use { operation ->
                    manager = operation
                    when (action) {
                        "test" -> { operation.test(); emit(token, Ui(message = "tested")) }
                        "backup" -> { operation.backup(false); CloudSettings.status(app, "success"); emit(token, Ui(message = if (operation.cleanupWarning) "success_cleanup" else "success", listing = operation.list())) }
                        "list" -> emit(token, Ui(message = "listed", listing = operation.list()))
                        "preview" -> { val target = requireNotNull(snapshot)
                            if (preparedName != target.name || prepared?.isFile != true) {
                                prepared?.delete(); prepared = operation.prepareRestore(target); preparedName = target.name
                            }
                            val result = requireNotNull(prepared).inputStream().use {
                                BackupArchiveManager(app).importFrom(it, dryRun = true, useBackupBalances = useBalances)
                            }
                            emit(token, Ui(message = "preview_ready", preview = result, snapshot = target, useBalances = useBalances)) }
                        "delete" -> { operation.delete(requireNotNull(snapshot)); emit(token, Ui(message = "deleted", listing = operation.list())) }
                    }
                }
            } catch (error: Exception) {
                val message = when (error) {
                    is BackupArchiveManager.AssetBalanceConflictException -> "balance_conflict"
                    is javax.crypto.AEADBadTagException, is BackupArchiveManager.BackupIntegrityException -> "integrity"
                    is CloudVaultPasswordException -> "vault_password"
                    is CloudCompatibilityException -> "create_protection"
                    is IllegalArgumentException, is org.json.JSONException -> "remote_format"
                    is CloudHttpException -> when (error.status) {
                        401, 403 -> "auth"
                        404 -> "not_found"
                        409 -> "path_conflict"
                        405, 501 -> "compatibility"
                        429 -> "rate_limited"
                        in 300..399 -> "redirect"
                        else -> "failed"
                    }
                    else -> "failed"
                }
                emit(token, Ui(message = message, snapshot = snapshot, useBalances = useBalances,
                    httpStatus = (error as? CloudHttpException)?.status,
                    httpMethod = (error as? CloudHttpException)?.method.orEmpty()))
            } finally { manager = null }
        }
    }
    private fun emit(token: Int, ui: Ui) { handler.post { if (generation.get() == token) state.value = ui } }
    fun cancel() { generation.incrementAndGet(); manager?.cancel(); task?.cancel(true); state.value = Ui(message = "cancelled") }
    fun dismissPreview() { prepared?.delete(); prepared = null; preparedName = null; state.value = Ui(message = "idle") }
    override fun onCleared() { cancel(); executor.submit { prepared?.delete(); prepared = null }; executor.shutdown(); super.onCleared() }
}
