package santodan.patches

import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.io.File
import software.santodan.extension.nuviolauncher.LauncherModels
import software.santodan.extension.nuviolauncher.LauncherModels.Tile

/** Real original APK contracts, injected callbacks, and launcher identity/list rules. */
fun main(args: Array<String>) {
    val movie = Tile("tt1", "movie", "Movie")
    val series = Tile("tt1", "series", "Series")
    check(LauncherModels.tiles(listOf(movie, movie, series, Tile("__placeholder_row", "movie", "Placeholder"))) == listOf(movie, series))
    check(LauncherModels.tiles((1..200).map { movie.copy(id = "tt$it") }).size == 100)
    check(LauncherModels.key("catalog", "url1", "addon", "movie", "popular") !=
        LauncherModels.key("catalog", "url2", "addon", "movie", "popular"))
    check(LauncherModels.key("tracking", "TRAKT", "watchlist") != LauncherModels.key("tracking", "SIMKL", "watchlist"))
    check(LauncherModels.key("catalog", "a_b", "c") != LauncherModels.key("catalog", "a", "b_c"))
    check(LauncherModels.listItems(listOf(movie to setOf("a"), series to setOf("a", "b")), "a",
        mapOf("movie:tt1" to 2, "series:tt1" to 1)) == listOf(series, movie))
    check(LauncherModels.listItems(listOf(movie to setOf("a"), series to setOf("b")), "b", emptyMap()) == listOf(series))
    println("PASS: catalog/provider identities, type-aware deduplication, cap, membership and provider ordering")

    for ((version, directory) in listOf(NuvioLayout.BETA4 to args[0], NuvioLayout.BETA5 to args[1])) {
        NuvioLayout.use(version)
        val classes = File(directory).listFiles()!!.filter { it.extension == "dex" }.flatMap { file ->
            file.inputStream().buffered().use { DexBackedDexFile.fromInputStream(null, it).classes.toList() }
        }.associateBy { it.type }
        fun owner(type: String): ClassDef = classes[type] ?: error("Missing $type")
        fun field(type: String, name: String, expected: String) {
            check(owner(type).fields.any { it.name == name && it.type == expected }) { "$version missing $type->$name:$expected" }
        }
        fun method(type: String, name: String, count: Int, returns: String? = null) {
            check(owner(type).methods.count { it.name == name && it.parameterTypes.size == count && (returns == null || it.returnType == returns) } == 1) {
                "$version missing/ambiguous $type->$name/$count"
            }
        }
        val homeType = NuvioLayout.current("Lla/z3;")
        val libraryType = if (version == NuvioLayout.BETA5) "Lv9/t0;" else "Lv9/u0;"
        val stateType = if (version == NuvioLayout.BETA5) "Lla/z2;" else "Lla/x2;"
        field(homeType, "x", "Lkotlinx/coroutines/flow/MutableStateFlow;")
        field(homeType, "b", "Lz9/a;")
        field(homeType, "d", if (version == NuvioLayout.BETA5) "Lv9/s;" else "Lv9/t;")
        field(stateType, "c", "Ljava/util/List;")
        field(if (version == NuvioLayout.BETA5) "Lv9/k;" else "Lv9/l;", "r", "Lkotlinx/coroutines/flow/StateFlow;")
        field(libraryType, "m", "Lkotlinx/coroutines/flow/Flow;")
        field(libraryType, "e", "Lo9/j;")
        field(libraryType, "f", "Le9/f;")
        field("Le9/f;", "f", "Lkotlinx/coroutines/flow/StateFlow;")
        field("Lp8/e;", "y", "Lnb/c;")
        method("Lo9/j;", "b", 0, "Ljava/util/List;")
        for (name in listOf("b", "n", "c")) method("Lo9/i;", name, 0, "Lkotlinx/coroutines/flow/Flow;")
        method("Lo9/i;", "a", 0, "Lo9/g0;")
        method(if (version == NuvioLayout.BETA5) "Lv9/s;" else "Lv9/t;", "a", 11, "Lkotlinx/coroutines/flow/Flow;")
        for ((model, getters) in mapOf(
            "Addon" to listOf("getEnabled", "getCatalogs", "getBaseUrl", "getId", "getDisplayName"),
            "CatalogDescriptor" to listOf("getExtra", "getExtraRequired", "getId", "getName", "getApiType"),
            "CatalogExtra" to listOf("getDefaultValue", "getName", "isRequired"),
            "CatalogRow" to listOf("getItems"),
            "MetaPreview" to listOf("getApiType", "getId", "getName", "getPoster", "getBackground", "getDescription"),
            "LibraryEntry" to listOf("getType", "getId", "getName", "getPoster", "getBackground", "getDescription", "getListKeys", "getListRanks"),
            "LibraryListTab" to listOf("getKey", "getTitle")
        )) for (getter in getters) method("Lcom/nuvio/tv/domain/model/$model;", getter, 0)
        val next = NuvioLayout.current("Lla/aa;")
        for (name in listOf("a", "b", "c", "d", "e", "o")) field(next, name, "Ljava/lang/String;")
        field(if (version == NuvioLayout.BETA5) "Lla/f0;" else "Lla/e0;", "a", next)
        val component = MutableClass(owner("Lp8/e;"))
        val home = MutableClass(owner(homeType))
        val library = MutableClass(owner(libraryType))
        NuvioLauncherChannelsPatch.hookReturn(NuvioRemainingEpisodesPatch.unique(component, "<init>", 1), "registerComponent")
        NuvioLauncherChannelsPatch.hookReturn(NuvioRemainingEpisodesPatch.unique(home, "<init>", 25), "observeHome")
        NuvioLauncherChannelsPatch.insert(NuvioRemainingEpisodesPatch.unique(home, "onCleared", 0), 0, "stopHome")
        NuvioLauncherChannelsPatch.hookReturn(NuvioRemainingEpisodesPatch.unique(library, "<init>", 8), "observeLibrary")
        val calls = listOf(component, home, library).flatMap { it.methods }.flatMap { method ->
            method.implementation?.instructions?.mapNotNull {
                ((it as? ReferenceInstruction)?.reference as? MethodReference)?.takeIf { ref -> ref.definingClass == NuvioLauncherChannelsPatch.EXTENSION }?.name
            }?.toList().orEmpty()
        }
        check(calls.sorted() == listOf("observeHome", "observeLibrary", "registerComponent", "stopHome").sorted())
        println("PASS: $version native source, profile, model and settings contracts; all four bytecode hooks")
    }
}
