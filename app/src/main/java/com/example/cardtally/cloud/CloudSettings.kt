package com.example.cardtally.cloud

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.work.*
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class CloudConfig(val type: String, val endpoint: String, val user: String, val secret: String,
    val bucket: String, val region: String, val prefix: String, val pathStyle: Boolean,
    val sessionToken: String, val password: String, val automatic: Boolean, val keep: Int) {
    fun json() = JSONObject().put("type", type).put("endpoint", endpoint).put("user", user).put("secret", secret)
        .put("bucket", bucket).put("region", region).put("prefix", prefix).put("pathStyle", pathStyle)
        .put("sessionToken", sessionToken).put("password", password).put("automatic", automatic).put("keep", keep)
    companion object {
        fun from(json: JSONObject) = CloudConfig(json.getString("type"), json.getString("endpoint"),
            json.getString("user"), json.getString("secret"), json.optString("bucket"), json.optString("region", "us-east-1"),
            json.optString("prefix", "cardtally"), json.optBoolean("pathStyle", true), json.optString("sessionToken"),
            json.getString("password"), json.optBoolean("automatic"), json.optInt("keep", 15).also { require(it in 1..365) })
    }
}

object CloudSettings {
    private const val ALIAS = "cardtally_cloud_settings_v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey(ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun file(context: Context) = File(context.noBackupFilesDir, "cloud_settings")
    @Synchronized fun read(context: Context): JSONObject {
        val file = file(context)
        if (!file.exists()) return JSONObject()
        val bytes = file.readBytes(); require(bytes.size in 28..65536)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        }
        return JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
    }
    @Synchronized fun save(context: Context, config: CloudConfig) {
        require((config.password.isEmpty() || config.password.length >= 8) && (!config.automatic || config.password.length >= 8) && config.keep in 1..365)
        val json = read(context).put("active", config.type).put(config.type, config.json())
        write(context, json)
        schedule(context, config.automatic)
    }
    private fun write(context: Context, json: JSONObject) {
        val plaintext = json.toString().toByteArray(Charsets.UTF_8)
        require(plaintext.size + 28 <= 65536)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val target = file(context); val stage = File.createTempFile("cloud-", ".tmp", context.noBackupFilesDir)
        try { stage.outputStream().use { it.write(cipher.iv); it.write(cipher.doFinal(plaintext)); it.fd.sync() }
            check(stage.renameTo(target)) } finally { stage.delete() }
    }
    fun active(context: Context): CloudConfig? { val json = read(context); return json.optJSONObject(json.optString("active"))?.let(CloudConfig::from) }
    @Synchronized fun disconnect(context: Context) {
        schedule(context, false)
        val target = file(context)
        check(!target.exists() || target.delete())
    }
    fun device(context: Context): String {
        val prefs = context.getSharedPreferences("cloud_internal", Context.MODE_PRIVATE)
        return prefs.getString("device", null) ?: UUID.randomUUID().toString().also { check(prefs.edit().putString("device", it).commit()) }
    }
    fun status(context: Context, value: String) { context.getSharedPreferences("cloud_internal", Context.MODE_PRIVATE).edit()
        .putString("status", value).putLong("status_time", System.currentTimeMillis()).apply() }
    fun schedule(context: Context, enabled: Boolean) {
        val work = WorkManager.getInstance(context)
        work.cancelUniqueWork("cloud-daily")
        if (enabled) work.enqueueUniquePeriodicWork("cloud-daily", ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<CloudBackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresStorageNotLow(true).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES).build())
    }
}
