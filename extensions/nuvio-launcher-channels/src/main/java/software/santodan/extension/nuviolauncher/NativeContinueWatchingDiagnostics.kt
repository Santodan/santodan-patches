package software.santodan.extension.nuviolauncher

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import org.json.JSONArray

/** Read-only beta6 diagnostics. Never log credentials, complete artwork URLs or watch-history lists. */
object NativeContinueWatchingDiagnostics {
    private data class Ui(val profile: String, val total: Int, val nextUp: Int, val aired: Int)
    @Volatile private var ui: Ui? = null
    private var lastLine: String? = null

    fun observe(profile: String, items: List<Any>) {
        var nextUp = 0
        var aired = 0
        for (item in items) {
            val data = item.javaClass.getDeclaredField("a").apply { isAccessible = true }.get(item) ?: continue
            if (data.javaClass.name != NuvioBeta6.name("la.ba")) continue // beta6 NextUpInfo
            nextUp++
            if (data.javaClass.getDeclaredField("n").apply { isAccessible = true }.getBoolean(data)) aired++
        }
        ui = Ui(profile, items.size, nextUp, aired)
    }

    private fun snapshot(context: Context, profile: String, name: String): String {
        val file = File(context.filesDir, "cw_enrichment/${name}_$profile.json")
        if (!file.isFile) return "missing"
        val array = JSONArray(file.readText())
        var aired = 0
        var images = 0
        for (index in 0 until array.length()) {
            val value = array.getJSONObject(index)
            if (value.optBoolean("hasAired", value.optBoolean("n", false))) aired++
            if (listOf("poster", "backdrop", "thumbnail", "episodeThumbnail", "d", "e", "l", "o")
                    .any { key -> !value.isNull(key) && value.optString(key).let { url ->
                        url.startsWith("https://") || url.startsWith("http://") || url.startsWith("content://") || url.startsWith("file://")
                    } }) images++
        }
        return "count=${array.length()},aired=$aired,images=$images,mtime=${file.lastModified()}"
    }

    fun report(context: Context, profile: String) {
        try {
            val channels = mutableSetOf<Long>()
            context.contentResolver.query(LauncherPublisher.CHANNELS,
                arrayOf("_id", "package_name", "internal_provider_id"), null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) if (cursor.getString(1) == context.packageName &&
                    cursor.getString(2)?.startsWith(context.packageName) == true) channels.add(cursor.getLong(0))
            }
            var programs = 0
            var missingArt = 0
            val artworkHosts = mutableSetOf<String>()
            context.contentResolver.query(LauncherPublisher.PROGRAMS,
                arrayOf("channel_id", "poster_art_uri"), null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) if (cursor.getLong(0) in channels) {
                    programs++
                    val image = cursor.getString(1)
                    if (image.isNullOrBlank()) missingArt++
                    else {
                        val uri = Uri.parse(image)
                        artworkHosts.add("${uri.scheme}://${uri.host ?: "local"}")
                    }
                }
            }
            val state = ui?.takeIf { it.profile == profile }
            val line = "Native CW diagnostic: profile=$profile ui=${state?.total ?: "unknown"} " +
                "uiNextUp=${state?.nextUp ?: "unknown"} uiAired=${state?.aired ?: "unknown"} " +
                "inProgressCache={${snapshot(context, profile, "inprogress")}} " +
                "nextUpCache={${snapshot(context, profile, "nextup")}} " +
                "nativeChannels=${channels.size} published=$programs missingArt=$missingArt artHosts=$artworkHosts"
            if (line != lastLine) { lastLine = line; Log.i("SantodanLauncher", line) }
        } catch (error: Exception) {
            Log.w("SantodanLauncher", "Native CW diagnostic unavailable: " + error.javaClass.simpleName)
        }
    }
}
