package software.santodan.extension.nuviolauncher

import java.security.MessageDigest

/** No Android dependencies: channel identity, list membership, and ordering. */
object LauncherModels {
    data class Tile @JvmOverloads constructor(val id: String, val type: String, val title: String,
        val poster: String? = null, val background: String? = null, val description: String? = null,
        val dateLabel: String? = null)
    data class Source @JvmOverloads constructor(val key: String, val title: String, val group: String, val tiles: List<Tile>,
        val sourceId: String = "", val sourceName: String = "")

    data class SourceGroup(val key: String, val title: String, val entries: List<Source>)
    fun validArtworkName(name: String) = name.matches(Regex("[a-f0-9]{64}\\.png"))
    private fun sourceName(source: Source, group: String): String = source.sourceName.ifBlank {
        source.title.substringAfter(" · ", if (group == "catalog") "Addon" else "Tracking provider").substringBeforeLast(" (")
    }
    fun sourceGroups(sources: List<Source>, group: String): List<SourceGroup> = sources.filter { it.group == group }
        .groupBy { it.sourceId.ifBlank { "legacy:" + sourceName(it, group) } }.map { (key, entries) ->
            SourceGroup(key, sourceName(entries.first(), group),
                entries.sortedBy { it.title.lowercase(java.util.Locale.ROOT) })
        }.sortedBy { it.title.lowercase(java.util.Locale.ROOT) }

    fun datedTitle(tile: Tile) = tile.dateLabel?.takeIf { it.isNotBlank() }?.let { "${tile.title} · $it" } ?: tile.title
    fun datedDescription(tile: Tile) = listOfNotNull(tile.dateLabel?.takeIf { it.isNotBlank() },
        tile.description?.takeIf { it.isNotBlank() && it != tile.dateLabel }).joinToString(" · ").ifBlank { null }

    fun key(group: String, vararg identity: String): String = group + ":" +
        MessageDigest.getInstance("SHA-256").digest(identity.joinToString("\u0000").toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }

    fun tiles(items: List<Tile>): List<Tile> = items.asSequence()
        .filter { it.id.isNotBlank() && !it.id.startsWith("__placeholder_") && it.title.isNotBlank() }
        .distinctBy { it.type to it.id }.take(100).toList()

    fun listItems(items: List<Pair<Tile, Set<String>>>, listKey: String,
        ranks: Map<String, Int>): List<Tile> = tiles(items.filter { listKey in it.second }
            .map { it.first }.sortedBy { ranks[it.type + ":" + it.id] ?: Int.MAX_VALUE })
}
