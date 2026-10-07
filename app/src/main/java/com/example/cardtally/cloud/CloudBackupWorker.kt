package com.example.cardtally.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.IOException

internal class WifiRequiredException : IOException()
internal object CloudNetwork {
    fun isWifi(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

class CloudBackupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    @Volatile private var operation: CloudBackupManager? = null
    override fun onStopped() { operation?.cancel(); super.onStopped() }
    override fun doWork(): Result {
        return try {
            val config = CloudSettings.active(applicationContext) ?: return Result.success()
            if (!config.automatic) return Result.success()
            val gate = {
                if (isStopped) throw InterruptedException()
                if (!CloudNetwork.isWifi(applicationContext)) throw WifiRequiredException()
            }
            gate()
            operation = CloudBackupManager(applicationContext, config, gate)
            val changed = requireNotNull(operation).backup(true)
            CloudSettings.status(applicationContext, if (requireNotNull(operation).cleanupWarning) "success_cleanup" else if (changed) "success" else "unchanged")
            Result.success()
        } catch (_: WifiRequiredException) {
            CloudSettings.status(applicationContext, "waiting_wifi"); Result.retry()
        } catch (error: Exception) {
            CloudSettings.status(applicationContext, "failed")
            if (error is IllegalArgumentException || (error is CloudHttpException && error.status in listOf(400, 401, 403))) Result.failure()
            else Result.retry()
        } finally { operation?.close(); operation = null }
    }
}
