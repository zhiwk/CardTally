package com.example.cardtally.util

import com.example.cardtally.model.Asset
import com.example.cardtally.model.Record

/** Resolve current asset names for presentation without rewriting historical record snapshots. */
object RecordAssetDisplay {
    fun names(assets: List<Asset>): Map<Long, String> = assets.associate { it.id to it.name }

    fun name(assetId: Long?, snapshot: String?, names: Map<Long, String>): String? =
        assetId?.let(names::get)?.takeIf { it.isNotBlank() }
            ?: snapshot?.takeIf { it.isNotBlank() }

    fun transferRoute(record: Record, names: Map<Long, String>): String = listOfNotNull(
        name(record.assetId, record.assetSource, names),
        name(record.destinationAssetId, record.destinationAssetSource, names)
    ).joinToString(" → ")
}
