package com.example.cardtally.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cardtally.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AppUpdateViewModel : ViewModel() {
    sealed class State {
        object Idle : State()
        object Checking : State()
        object Current : State()
        object NoPackage : State()
        data class Available(val release: GitHubRelease) : State()
        data class Downloading(val percent: Int) : State()
        data class Ready(val release: GitHubRelease, val file: File) : State()
        data class Failed(val verification: Boolean) : State()
    }

    private val mutableState = MutableLiveData<State>(State.Idle)
    val state: LiveData<State> = mutableState
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val generation = AtomicInteger()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.MINUTES).followSslRedirects(false)
        .addNetworkInterceptor { chain ->
            val url = chain.request().url
            if (url.scheme != "https" || url.port != 443 || url.host !in setOf("api.github.com", "github.com",
                    "objects.githubusercontent.com", "release-assets.githubusercontent.com")) throw IOException("Unexpected update host")
            chain.proceed(chain.request())
        }.build()

    fun check() {
        if (state.value is State.Checking || state.value is State.Downloading) return
        val token = generation.incrementAndGet()
        mutableState.value = State.Checking
        executor.execute {
            try {
                val json = readText(GitHubRelease.API_URL, 1024 * 1024)
                val metadata = org.json.JSONObject(json)
                require(!metadata.optBoolean("draft") && !metadata.optBoolean("prerelease"))
                val latest = requireNotNull(ReleaseVersion.parse(metadata.getString("tag_name")))
                val current = requireNotNull(ReleaseVersion.parse(BuildConfig.VERSION_NAME))
                if (latest <= current) emit(token, State.Current)
                else {
                    val release = GitHubRelease.fromJson(json, BuildConfig.APPLICATION_ID)
                    emit(token, if (release == null) State.NoPackage else State.Available(release))
                }
            } catch (_: Exception) { emit(token, State.Failed(false)) }
        }
    }

    /** Called only by the native download confirmation action. */
    fun download(context: Context, release: GitHubRelease) {
        if ((state.value as? State.Available)?.release != release) return
        val app = context.applicationContext
        val token = generation.incrementAndGet()
        mutableState.value = State.Downloading(0)
        executor.execute {
            val folder = File(app.cacheDir, "updates")
            var staged: File? = null
            var verification = false
            try {
                check(folder.isDirectory || folder.mkdirs())
                // This cache contains update packages only, never financial data.
                folder.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > TimeUnit.DAYS.toMillis(1) }?.forEach { it.delete() }
                val expectedHash = release.sha256 ?: release.checksumUrl?.let {
                    GitHubRelease.checksumFromText(readText(it, 64 * 1024), release.assetName)
                }
                verification = true
                requireNotNull(expectedHash)
                verification = false
                val partial = File.createTempFile("download-", ".part", folder)
                staged = partial
                val hash = MessageDigest.getInstance("SHA-256")
                client.newCall(request(release.downloadUrl)).execute().use { response ->
                    check(response.isSuccessful)
                    val body = requireNotNull(response.body)
                    require(body.contentLength() == -1L || body.contentLength() == release.size)
                    var total = 0L
                    var lastPercent = -1
                    body.byteStream().use { input ->
                        partial.outputStream().use { output ->
                            val buffer = ByteArray(32 * 1024)
                            while (true) {
                                if (generation.get() != token || Thread.currentThread().isInterrupted) throw IOException("Cancelled")
                                val count = input.read(buffer)
                                if (count < 0) break
                                total += count
                                require(total <= release.size && total <= GitHubRelease.MAX_APK_BYTES)
                                output.write(buffer, 0, count)
                                hash.update(buffer, 0, count)
                                val percent = (total * 100 / release.size).toInt()
                                if (percent != lastPercent) { lastPercent = percent; emit(token, State.Downloading(percent)) }
                            }
                            output.fd.sync()
                        }
                    }
                    verification = true
                    require(total == release.size && hash.digest().hex() == expectedHash)
                }
                verifyPackage(app, partial, release)
                if (generation.get() != token) throw IOException("Cancelled")
                val target = File(folder, "update-${java.util.UUID.randomUUID()}.apk")
                check(partial.renameTo(target))
                emit(token, State.Ready(release, target))
            } catch (_: Exception) { emit(token, State.Failed(verification)) }
            finally { staged?.delete() }
        }
    }

    fun cancelDownload() {
        if (state.value !is State.Downloading) return
        generation.incrementAndGet()
        client.dispatcher.cancelAll()
        mutableState.value = State.Idle
    }

    private fun emit(token: Int, value: State) {
        main.post { if (generation.get() == token) mutableState.value = value }
    }

    private fun request(url: String) = Request.Builder().url(url)
        .header("Accept", if (url == GitHubRelease.API_URL) "application/vnd.github+json" else "application/octet-stream")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("User-Agent", "CardTally/${BuildConfig.VERSION_NAME}").build()

    private fun readText(url: String, limit: Int): String = client.newCall(request(url)).execute().use { response ->
        check(response.isSuccessful)
        val body = requireNotNull(response.body)
        require(body.contentLength() <= limit)
        body.byteStream().use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= limit)
                output.write(buffer, 0, count)
            }
            output.toString("UTF-8")
        }
    }

    @Suppress("DEPRECATION")
    private fun verifyPackage(context: Context, file: File, release: GitHubRelease) {
        val manager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = requireNotNull(manager.getPackageArchiveInfo(file.path, flags))
        val installed = manager.getPackageInfo(context.packageName, flags)
        require(archive.packageName == context.packageName)
        require(archive.versionName == release.version.toString())
        fun code(info: PackageInfo) = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        require(code(archive) > code(installed))
        require(requireNotNull(archive.applicationInfo).minSdkVersion <= Build.VERSION.SDK_INT)
        fun signers(info: PackageInfo): Set<String> {
            val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
            return signatures.orEmpty().map { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).hex() }.toSet()
        }
        val currentSigners = signers(installed)
        require(currentSigners.isNotEmpty() && signers(archive) == currentSigners)
    }

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it.toInt() and 255) }

    override fun onCleared() {
        generation.incrementAndGet()
        client.dispatcher.cancelAll()
        executor.shutdownNow()
        super.onCleared()
    }
}
