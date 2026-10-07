package com.example.cardtally.update

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject

/** Stable X.Y.Z releases only; never compare version names lexicographically. */
data class ReleaseVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int =
        compareValuesBy(this, other, ReleaseVersion::major, ReleaseVersion::minor, ReleaseVersion::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        fun parse(value: String): ReleaseVersion? {
            val text = value.removePrefix("v")
            if (!Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)").matches(text)) return null
            val parts = text.split('.').map { it.toIntOrNull() ?: return null }
            return ReleaseVersion(parts[0], parts[1], parts[2])
        }
    }
}

data class GitHubRelease(
    val version: ReleaseVersion,
    val notes: String,
    val assetName: String,
    val downloadUrl: String,
    val size: Long,
    val sha256: String?,
    val checksumUrl: String?
) {
    companion object {
        const val REPOSITORY_URL = "https://github.com/zhiwk/CardTally"
        const val RELEASES_URL = "$REPOSITORY_URL/releases"
        const val API_URL = "https://api.github.com/repos/zhiwk/CardTally/releases/latest"
        const val MAX_APK_BYTES = 100L * 1024 * 1024

        /** Only accept the artifact for the installed application channel. */
        fun fromJson(json: String, applicationId: String): GitHubRelease? {
            val root = JSONObject(json)
            if (root.optBoolean("draft") || root.optBoolean("prerelease")) return null
            val tag = root.getString("tag_name")
            val version = ReleaseVersion.parse(tag) ?: return null
            val names = when (applicationId) {
                "com.example.cardtally.release" -> listOf("cardtally-$version.apk", "XiaomaoJizhang-$version.apk")
                "com.example.cardtally" -> listOf("cardtally-dev-$version.apk", "XiaomaoJizhang-dev-$version.apk")
                else -> return null
            }
            val assets = root.getJSONArray("assets")
            val apks = mutableMapOf<String, JSONObject>()
            var checksum: JSONObject? = null
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                when (asset.optString("name")) {
                    in names -> { val name = asset.getString("name"); require(apks.put(name, asset) == null) }
                    "SHA256SUMS.txt" -> { require(checksum == null); checksum = asset }
                }
            }
            val name = names.firstOrNull { it in apks } ?: return null
            val artifact = apks.getValue(name)
            val size = artifact.getLong("size")
            require(size in 1..MAX_APK_BYTES)
            val url = checkedAssetUrl(artifact.getString("browser_download_url"), tag, name)
            val digest = artifact.optString("digest").takeIf { it.startsWith("sha256:") }
                ?.removePrefix("sha256:")?.lowercase()?.takeIf { Regex("[a-f0-9]{64}").matches(it) }
            return GitHubRelease(version, root.optString("body").take(8000), name, url, size, digest,
                checksum?.let { checkedAssetUrl(it.getString("browser_download_url"), tag, "SHA256SUMS.txt") })
        }

        private fun checkedAssetUrl(value: String, tag: String, name: String): String {
            val url = requireNotNull(value.toHttpUrlOrNull())
            require(url.scheme == "https" && url.host == "github.com" && url.port == 443)
            require(url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null)
            require(url.pathSegments == listOf("zhiwk", "CardTally", "releases", "download", tag, name))
            return url.toString()
        }

        fun checksumFromText(text: String, name: String): String? {
            val hashes = text.lineSequence().mapNotNull { line ->
                val match = Regex("^([a-fA-F0-9]{64})[ \\t]+\\*?(.+)$").matchEntire(line.trim())
                match?.takeIf { it.groupValues[2] == name }?.groupValues?.get(1)?.lowercase()
            }.toList()
            return hashes.singleOrNull()
        }
    }
}
