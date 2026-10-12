package software.santodan.extension.nuviolauncher

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.tv.TvContract
import android.net.Uri
import android.os.Build
import software.santodan.extension.nuviolauncher.LauncherModels.Source
import software.santodan.extension.nuviolauncher.LauncherModels.Tile

/** Upsert only our own channels/programs; preserve IDs and launcher visibility choices. */
class LauncherPublisher @JvmOverloads constructor(private val context: Context,
    private val artwork: (Tile) -> String? = { it.background?.takeIf { url -> url.isNotBlank() } ?: it.poster }) {
    companion object {
        // Deliberately does not start with packageName: native CW orphan recovery uses that prefix.
        const val PREFIX = "santodan-launcher/"
        val CHANNELS: Uri = Uri.parse("content://android.media.tv/channel")
        val PROGRAMS: Uri = Uri.parse("content://android.media.tv/preview_program")
    }
    private val resolver get() = context.contentResolver
    fun supported() = Build.VERSION.SDK_INT >= 26 &&
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

    private fun intent(tile: Tile? = null): String {
        val activity = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: throw IllegalStateException("Missing launcher activity")
        activity.action = Intent.ACTION_VIEW
        activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (tile != null) {
            activity.putExtra("contentId", tile.id).putExtra("contentType", tile.type)
            // Native warm-start capture requires stream, but absent videoId/name routes to Detail.
            activity.putExtra("launchMode", "stream")
        }
        return activity.toUri(Intent.URI_INTENT_SCHEME)
    }

    fun reconcile(sources: List<Source>) {
        if (!supported()) return
        val desired = sources.associateBy { PREFIX + it.key }
        val existing = linkedMapOf<String, MutableList<Long>>()
        val metadata = mutableMapOf<Long, Map<String, String?>>()
        val channelColumns = arrayOf("type", "display_name", "app_link_intent_uri")
        resolver.query(CHANNELS, arrayOf("_id", "internal_provider_id", "package_name") + channelColumns, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val key = cursor.getString(1) ?: continue
                if (cursor.getString(2) == context.packageName && key.startsWith(PREFIX)) {
                    existing.getOrPut(key) { mutableListOf() }.add(cursor.getLong(0))
                    metadata[cursor.getLong(0)] = channelColumns.mapIndexed { index, column -> column to cursor.getString(index + 3) }.toMap()
                }
            }
        } ?: throw IllegalStateException("Cannot query launcher channels")
        for ((key, ids) in existing) {
            if (key !in desired) ids.forEach { deleteChannel(it) }
            else ids.drop(1).forEach { deleteChannel(it) }
        }
        for ((key, source) in desired) {
            val values = ContentValues().apply {
                put("type", "TYPE_PREVIEW")
                put("display_name", source.title)
                put("app_link_intent_uri", intent())
                put("internal_provider_id", key)
            }
            val id = existing[key]?.firstOrNull() ?: run {
                val uri = resolver.insert(CHANNELS, values) ?: throw IllegalStateException("Cannot create channel")
                val created = ContentUris.parseId(uri)
                writeLogo(created)
                TvContract.requestChannelBrowsable(context, created)
                created
            }
            if (id in metadata && channelColumns.any { metadata[id]?.get(it) != values.getAsString(it) })
                resolver.update(ContentUris.withAppendedId(CHANNELS, id), values, null, null)
            programs(id, LauncherModels.tiles(source.tiles))
        }
    }

    private fun deleteChannel(id: Long) {
        resolver.delete(ContentUris.withAppendedId(CHANNELS, id), null, null)
    }

    private fun programs(channel: Long, tiles: List<Tile>) {
        val existing = linkedMapOf<String, MutableList<Long>>()
        val storedValues = mutableMapOf<Long, Map<String, String?>>()
        val columns = arrayOf("channel_id", "internal_provider_id", "type", "title", "short_description",
            "intent_uri", "poster_art_uri", "poster_art_aspect_ratio", "weight")
        // Match native CW's Fire OS-compatible query and filter explicitly before mutating.
        resolver.query(PROGRAMS, arrayOf("_id") + columns, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getLong(1) != channel) continue
                val id = cursor.getLong(0)
                existing.getOrPut(cursor.getString(2) ?: "") { mutableListOf() }.add(id)
                storedValues[id] = columns.mapIndexed { index, column -> column to cursor.getString(index + 1) }.toMap()
            }
        } ?: throw IllegalStateException("Cannot query launcher programs")
        val desired = tiles.map { LauncherModels.key("tile", it.type, it.id) }.toSet()
        for ((key, ids) in existing) if (key !in desired)
            ids.forEach { resolver.delete(ContentUris.withAppendedId(PROGRAMS, it), null, null) }
        tiles.forEachIndexed { index, tile ->
            val key = LauncherModels.key("tile", tile.type, tile.id)
            val landscape = !tile.background.isNullOrBlank()
            val values = ContentValues().apply {
                put("channel_id", channel)
                put("internal_provider_id", key)
                put("type", if (tile.type == "movie") 0 else 1) // movie / TV series, never Watch Next
                put("title", LauncherModels.datedTitle(tile))
                put("short_description", LauncherModels.datedDescription(tile))
                put("intent_uri", intent(tile))
                put("poster_art_uri", artwork(tile))
                put("poster_art_aspect_ratio", if (landscape) 0 else 4) // 16:9 / 2:3
                put("weight", tiles.size - index)
            }
            val rows = existing[key]
            if (rows.isNullOrEmpty()) resolver.insert(PROGRAMS, values)
                ?: throw IllegalStateException("Cannot create launcher program")
            else {
                if (columns.any { storedValues[rows.first()]?.get(it) != values.getAsString(it) })
                    resolver.update(ContentUris.withAppendedId(PROGRAMS, rows.first()), values, null, null)
                rows.drop(1).forEach { resolver.delete(ContentUris.withAppendedId(PROGRAMS, it), null, null) }
            }
        }
    }

    private fun writeLogo(id: Long) {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        try {
            drawable.setBounds(0, 0, 128, 128)
            drawable.draw(Canvas(bitmap))
            resolver.openOutputStream(Uri.withAppendedPath(ContentUris.withAppendedId(CHANNELS, id), "logo"))?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } finally { bitmap.recycle() }
    }
}
