package com.example.cardtally.update

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GitHubReleaseTest {
    private val hash = "a".repeat(64)
    private fun release(name: String = "XiaomaoJizhang-0.0.4.apk", url: String = "https://github.com/zhiwk/CardTally/releases/download/v0.0.4/$name"): JSONObject =
        JSONObject().put("tag_name", "v0.0.4").put("draft", false).put("prerelease", false)
            .put("assets", JSONArray().put(JSONObject().put("name", name).put("size", 1024)
                .put("browser_download_url", url).put("digest", "sha256:$hash")))

    @Test fun versionsCompareNumericallyAndRejectUnsupportedFormats() {
        assertTrue(ReleaseVersion.parse("0.0.10")!! > ReleaseVersion.parse("v0.0.9")!!)
        listOf("1.0", "0.00.4", "v0.0.4-beta", "2147483648.0.0", "0.0.-1").forEach {
            assertNull(ReleaseVersion.parse(it))
        }
    }

    @Test fun onlyStableReleaseAndMatchingChannelAreAccepted() {
        val json = release()
        assertEquals(hash, GitHubRelease.fromJson(json.toString(), "com.example.cardtally.release")!!.sha256)
        assertNull(GitHubRelease.fromJson(json.toString(), "com.example.cardtally"))
        assertNotNull(GitHubRelease.fromJson(release("XiaomaoJizhang-dev-0.0.4.apk").toString(), "com.example.cardtally"))
        assertNull(GitHubRelease.fromJson(json.put("draft", true).toString(), "com.example.cardtally.release"))
        assertNull(GitHubRelease.fromJson(json.put("draft", false).put("prerelease", true).toString(), "com.example.cardtally.release"))
    }

    @Test fun unrelatedOrInsecureDownloadUrlsAreRejected() {
        listOf("http://github.com/zhiwk/CardTally/releases/download/v0.0.4/XiaomaoJizhang-0.0.4.apk",
            "https://example.com/zhiwk/CardTally/releases/download/v0.0.4/XiaomaoJizhang-0.0.4.apk",
            "https://github.com/other/CardTally/releases/download/v0.0.4/XiaomaoJizhang-0.0.4.apk").forEach { url ->
            try { GitHubRelease.fromJson(release(url = url).toString(), "com.example.cardtally.release"); fail("URL accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun checksumMustIdentifyExactlyOneMatchingFile() {
        assertEquals(hash, GitHubRelease.checksumFromText("$hash  app.apk", "app.apk"))
        assertNull(GitHubRelease.checksumFromText("$hash  other.apk", "app.apk"))
        assertNull(GitHubRelease.checksumFromText("$hash  app.apk\n$hash  app.apk", "app.apk"))
    }
}
