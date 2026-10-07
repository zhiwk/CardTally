package com.example.cardtally.cloud

/** Retention owns only this installation's automatic snapshots. */
internal object CloudRetention {
    fun expired(snapshots: List<CloudSnapshot>, device: String, keep: Int): List<CloudSnapshot> {
        require(keep in 1..365)
        return snapshots.filter { it.device == device && it.automatic }.sortedByDescending { it.date }.drop(keep)
    }
}
