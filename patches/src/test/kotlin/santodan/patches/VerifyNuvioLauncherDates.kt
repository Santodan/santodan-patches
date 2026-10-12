package santodan.patches

import software.santodan.extension.nuviolauncher.LauncherDates
import software.santodan.extension.nuviolauncher.LauncherModels
import java.util.Locale

fun main() {
    check(LauncherModels.validArtworkName("a".repeat(64) + ".png"))
    for (name in listOf("../private.png", "/data/file.png", "a.png", "a".repeat(64) + ".png/../secret", "a".repeat(64) + ".tmp"))
        check(!LauncherModels.validArtworkName(name))
    check(LauncherDates.label("2026-11-19", null, Locale.US) == "19-Nov")
    check(LauncherDates.label("2026-11-19T12:00:00Z", null, Locale.US) == "19-Nov")
    check(LauncherDates.label("unknown", "Tomorrow", Locale.US) == "Tomorrow")
    check(LauncherDates.label(null, "", Locale.US) == null)
    val tile = LauncherModels.Tile("tt1", "series", "Series", description = "Episode", dateLabel = "19-Nov")
    check(LauncherModels.datedTitle(tile) == "Series · 19-Nov")
    check(LauncherModels.datedDescription(tile) == "19-Nov · Episode")
    check(LauncherModels.datedDescription(tile.copy(description = "19-Nov")) == "19-Nov")
    check(LauncherModels.datedTitle(tile.copy(dateLabel = null)) == "Series")
    val first = LauncherModels.Source("a", "Popular · Same name (movie)", "catalog", emptyList(), "url-a", "Same name")
    val second = first.copy(key = "b", sourceId = "url-b")
    check(LauncherModels.sourceGroups(listOf(first, second), "catalog").size == 2)
    check(LauncherModels.sourceGroups(listOf(first, first.copy(key = "c")), "catalog").single().entries.size == 2)
    check(LauncherModels.sourceGroups(listOf(first.copy(sourceId = "", sourceName = ""),
        first.copy(key = "old", sourceId = "", sourceName = "")), "catalog").single().title == "Same name")
    println("PASS: date formatting/fallback, metadata propagation, source identities and legacy cached grouping")
}
