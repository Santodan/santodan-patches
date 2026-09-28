package software.santodan.extension.nuviomerged

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Runtime bridge for the optional merged watch-progress snapshot. */
object NuvioMergedProgress {
    private const val TAG = "SantodanMergedProgress"
    private const val PREFS = "santodan_nuvio_merged_progress"
    private const val ENABLED = "enabled"
    private const val STRATEGY = "strategy"
    private const val RECENT = "recent"
    private val merging = AtomicBoolean(false)
    @Volatile private var repository: Any? = null
    @Volatile private var allProgressMethod = "q"
    @Volatile private var proxyRepository: Any? = null
    @Volatile private var providerProxy: Any? = null
    private val mergedProgress = MutableStateFlow<List<Any>>(emptyList())
    private val mergedNextUpSeeds = MutableStateFlow<List<Any>>(emptyList())
    private val authenticated = MutableStateFlow(true)
    private val originByContent = ConcurrentHashMap<String, String>()
    private val providerBySource = ConcurrentHashMap<String, Any>()

    @JvmStatic fun registerRepository(value: Any) {
        repository = value
        // In beta.2 j() is a Set-valued flow and must not be replaced with a
        // list-backed flow.
        allProgressMethod = "q"
        if (enabled()) {
            mergeAsync()
        }
    }

    /** Called from the repository's selected-provider mapper. */
    @JvmStatic fun mergedProvider(nativeProvider: Any?): Any? {
        if (!enabled()) return nativeProvider
        val repo = repository ?: return nativeProvider
        val existing = providerProxy
        if (existing != null && proxyRepository === repo) return existing
        synchronized(this) {
            providerProxy?.takeIf { proxyRepository === repo }?.let { return it }
            val providerInterface = repo.javaClass.classLoader.loadClass("ca.a0")
            val delegate = firstProvider(repo)
            val created = Proxy.newProxyInstance(repo.javaClass.classLoader, arrayOf(providerInterface)) { proxy, method, args ->
                when {
                    method.declaringClass == Any::class.java && method.name == "toString" -> "SantodanMergedProgressProvider"
                    method.declaringClass == Any::class.java && method.name == "hashCode" -> System.identityHashCode(proxy)
                    method.declaringClass == Any::class.java && method.name == "equals" -> proxy === args?.firstOrNull()
                    !enabled() && delegate != null -> method.invoke(delegate, *(args ?: emptyArray()))
                    method.parameterCount == 0 && method.name == "a" -> mergedIdentity(delegate)
                    // The obfuscated allProgress accessor differs by app version;
                    // f() remains nextUpSeeds. These must remain
                    // separate: completed history records are valid progress but
                    // must never be published wholesale as Continue Watching seeds.
                    method.parameterCount == 0 && method.name == allProgressMethod -> mergedProgress
                    method.parameterCount == 0 && method.name == "f" -> mergedNextUpSeeds
                    method.parameterCount == 0 && method.name == "b" -> authenticated
                    // Route provider-specific reconciliation back to the provider
                    // that supplied this show's winning Continue Watching seed.
                    method.name == "l" && method.parameterCount == 1 ->
                        providerForContent(args?.getOrNull(0)?.toString())?.let { method.invoke(it, *(args ?: emptyArray())) }
                            ?: delegate?.let { method.invoke(it, *(args ?: emptyArray())) }
                    method.name == "i" && method.parameterCount == 2 ->
                        providerForProgress(args?.getOrNull(0))?.let { method.invoke(it, *(args ?: emptyArray())) }
                            ?: delegate?.let { method.invoke(it, *(args ?: emptyArray())) }
                    method.name == "h" && method.parameterCount == 2 ->
                        providerForProgress(args?.getOrNull(0))?.let { method.invoke(it, *(args ?: emptyArray())) }
                            ?: delegate?.let { method.invoke(it, *(args ?: emptyArray())) }
                    delegate != null -> method.invoke(delegate, *(args ?: emptyArray()))
                    else -> defaultValue(method.returnType)
                }
            }
            proxyRepository = repo
            providerProxy = created
            return created
        }
    }

    /** Called from effectiveWatchProgressSource; merged snapshots are read as Nuvio Sync. */
    @JvmStatic fun effectiveSource(requested: Any, isAuthenticated: Any): Any {
        val name = (requested as? Enum<*>)?.name
        when (name) {
            "MERGED_HIGHEST" -> preferences().edit().putBoolean(ENABLED, true).putString(STRATEGY, "highest").commit()
            "MERGED_RECENT" -> preferences().edit().putBoolean(ENABLED, true).putString(STRATEGY, RECENT).commit()
            else -> if (!enabled()) return requested
        }
        mergeAsync()
        val carrier = authenticatedCarrier(requested, isAuthenticated)
        Log.d(TAG, "effective merged carrier=${(carrier as? Enum<*>)?.name}")
        return carrier
    }

    @JvmStatic fun selectSource(requested: Any): Any {
        val name = (requested as? Enum<*>)?.name
        val merged = name == "MERGED_HIGHEST" || name == "MERGED_RECENT"
        preferences().edit()
            .putBoolean(ENABLED, merged)
            .putString(STRATEGY, if (name == "MERGED_RECENT") RECENT else "highest")
            .commit()
        Log.d(TAG, "selected source=$name merged=$merged")
        if (merged) {
            mergeAsync()
        }
        return if (merged) {
            // Persist a real provider source so Nuvio creates an active provider.
            // mergedProvider() replaces that provider with the aggregate proxy.
            requested.javaClass.enumConstants.firstOrNull { (it as Enum<*>).name == "TRAKT" }
                ?: requested
        } else requested
    }

    @JvmStatic fun selectedSource(nativeSource: Any): Any {
        if (!enabled()) return nativeSource
        val wanted = if (preferences().getString(STRATEGY, "highest") == RECENT) "MERGED_RECENT" else "MERGED_HIGHEST"
        return nativeSource.javaClass.enumConstants.firstOrNull { (it as Enum<*>).name == wanted } ?: nativeSource
    }

    @JvmStatic fun appendEnum(array: Any, value: Any): Any {
        val length = java.lang.reflect.Array.getLength(array)
        val result = java.lang.reflect.Array.newInstance(array.javaClass.componentType, length + 1)
        System.arraycopy(array, 0, result, 0, length)
        java.lang.reflect.Array.set(result, length, value)
        return result
    }

    @JvmStatic fun appendMergedSources(original: List<Any>): List<Any> {
        val result = ArrayList(original)
        val enumClass = original.firstOrNull()?.javaClass ?: return result
        val additions = enumClass.enumConstants.orEmpty().filter {
            val name = (it as Enum<*>).name
            name == "MERGED_HIGHEST" || name == "MERGED_RECENT"
        }
        result.addAll(additions)
        Log.d(TAG, "picker append reached original=${original.size} additions=${additions.size}")
        return result
    }

    @JvmStatic fun displayLabel(source: Any): String = when ((source as? Enum<*>)?.name) {
        "MERGED_HIGHEST" -> "Merged - Highest progress"
        "MERGED_RECENT" -> "Merged - Most recently updated"
        "TRAKT" -> "Trakt"
        "SIMKL" -> "Simkl"
        "MDBLIST" -> "MDBList"
        else -> "Nuvio Sync"
    }

    /** The outer settings row already has Nuvio's correct native label. */
    @JvmStatic fun summaryLabel(source: Any, nativeLabel: String): String =
        if (enabled()) {
            if (preferences().getString(STRATEGY, "highest") == RECENT) "Merged - Most recently updated"
            else "Merged - Highest progress"
        } else nativeLabel

    private fun mergeAsync() {
        if (!enabled() || !merging.compareAndSet(false, true)) return
        Thread({
            try { runBlocking { mergeSnapshot() } }
            catch (error: Throwable) { Log.e(TAG, "Snapshot merge failed", error) }
            finally { merging.set(false) }
        }, "SantodanMergedProgress").apply { isDaemon = true }.start()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun mergeSnapshot() {
        val repo = repository ?: return
        // NuvioTV 1.1.0-beta.2 constructor fields: a = WatchProgressPreferences,
        // j = ProfileManager, k = tracking providers.
        val registry = field(repo, "k").get(repo)
        val localStore = field(repo, "a").get(repo)
        val candidates = ArrayList<Any>()
        val localFlow = field(localStore, "q").get(localStore) as Flow<Any>
        val localItems = (localFlow.first() as? Collection<Any>).orEmpty()
        candidates.addAll(localItems)
        val sourceCounts = ArrayList<String>()
        val sourceItems = LinkedHashMap<String, Collection<Any>>()
        val sourceSeeds = LinkedHashMap<String, Collection<Any>>()
        sourceCounts.add("Nuvio Sync=${localItems.size}")
        sourceItems["Nuvio Sync"] = localItems
        val providers = findMethod(registry.javaClass, "b", 0).invoke(registry) as Collection<Any>
        providerBySource.clear()
        for (provider in providers) {
            val authenticated = (findMethod(provider.javaClass, "b", 0).invoke(provider) as Flow<Any>).first() as? Boolean ?: false
            if (!authenticated) continue
            val items = (findMethod(provider.javaClass, allProgressMethod, 0).invoke(provider) as Flow<Any>).first() as? Collection<Any>
            if (items != null) {
                val source = provider.javaClass.simpleName
                providerBySource[sourceName(source)] = provider
                sourceCounts.add("$source=${items.size}")
                sourceItems[source] = items
                candidates.addAll(items)
                val seeds = (findMethod(provider.javaClass, "f", 0).invoke(provider) as Flow<Any>).first() as? Collection<Any>
                sourceSeeds[source] = seeds.orEmpty()
            }
        }
        val recent = preferences().getString(STRATEGY, "highest") == RECENT
        val histories = LinkedHashMap<String, MutableList<Pair<String, List<Any>>>>()
        for ((source, items) in sourceItems) {
            for ((show, records) in items.groupBy(::showKey)) {
                histories.getOrPut(show) { ArrayList() }.add(source to records)
            }
        }
        // A provider's nextUpSeeds is its authoritative Continue Watching
        // projection. Include those shows even when no active playback record
        // exists in allProgress (the normal case for fully watched episodes whose
        // next aired episode should be offered).
        for ((source, seeds) in sourceSeeds) {
            for ((show, records) in seeds.groupBy(::showKey)) {
                if (histories[show].orEmpty().none { it.first == source }) {
                    histories.getOrPut(show) { ArrayList() }.add(source to emptyList())
                }
            }
        }
        val merged = ArrayList<Any>()
        val mergedSeeds = ArrayList<Any>()
        val origins = HashMap<String, String>()
        for ((show, choices) in histories) {
            val winner = choices.maxWithOrNull { left, right ->
                val leftScore = sourceSeeds[left.first].orEmpty().filter { showKey(it) == show }.ifEmpty { left.second }
                val rightScore = sourceSeeds[right.first].orEmpty().filter { showKey(it) == show }.ifEmpty { right.second }
                compareHistories(leftScore, rightScore, recent)
            } ?: continue
            val coherent = LinkedHashMap<String, Any>()
            for (item in winner.second) {
                val episode = key(item)
                val old = coherent[episode]
                if (old == null || better(item, old, recent)) coherent[episode] = item
            }
            merged.addAll(coherent.values)
            val winningSeeds = sourceSeeds[winner.first].orEmpty()
                .filter { showKey(it).equals(show, ignoreCase = true) }
            mergedSeeds.addAll(winningSeeds)
            origins[show] = sourceName(winner.first)
        }
        originByContent.clear()
        originByContent.putAll(origins)
        // Provider lists are individually ordered, but concatenating them leaves
        // every Trakt record ahead of Simkl/MDBList-only records. Nuvio limits
        // work near the front of this history, so restore a global activity order.
        val published = ArrayList(merged.sortedByDescending { number(it, "getLastWatched") })
        val publishedSeeds = ArrayList(mergedSeeds
            .distinctBy(::showKey)
            .sortedByDescending { number(it, "getLastWatched") })
        Handler(Looper.getMainLooper()).post {
            mergedProgress.value = published
            mergedNextUpSeeds.value = publishedSeeds
        }
        Log.d(TAG, "Sources: ${sourceCounts.joinToString()}")
        Log.d(TAG, "Published ${merged.size} progress entries and ${publishedSeeds.size} next-up seeds for ${histories.size} shows from ${candidates.size} provider records")
    }

    private fun key(item: Any): String {
        fun value(name: String) = runCatching { findMethod(item.javaClass, name, 0).invoke(item) }.getOrNull()
        return listOf(value("getContentId"), value("getSeason"), value("getEpisode")).joinToString("|")
    }

    private fun showKey(item: Any): String {
        val id = runCatching { findMethod(item.javaClass, "getContentId", 0).invoke(item) }.getOrNull()
        val type = runCatching { findMethod(item.javaClass, "getContentType", 0).invoke(item) }.getOrNull()
        return "$type|$id"
    }

    private fun compareHistories(left: List<Any>, right: List<Any>, recent: Boolean): Int {
        fun score(records: List<Any>): Double = if (recent) {
            records.maxOfOrNull { number(it, "getLastWatched") } ?: 0.0
        } else {
            records.maxOfOrNull {
                number(it, "getSeason") * 1_000_000_000.0 +
                    number(it, "getEpisode") * 1_000_000.0 +
                    number(it, "getProgressPercent")
            } ?: 0.0
        }
        return score(left).compareTo(score(right))
    }

    private fun sourceName(raw: String): String = when (raw) {
        "cc", "tb" -> "Trakt"
        "l7", "a8" -> "Simkl"
        "d5", "q5" -> "MDBList"
        else -> raw
    }

    @JvmStatic fun sourceForContent(contentId: String?): String? =
        if (contentId == null) null else originByContent.entries.firstOrNull { it.key.endsWith("|$contentId") }?.value

    @JvmStatic fun adjustContinueWatchingCutoff(nativeCutoff: Long?): Long? =
        if (enabled()) null else nativeCutoff

    @JvmStatic fun adjustNextUpSeedDecision(progress: Any, nativeDecision: Boolean): Boolean {
        if (!enabled()) return nativeDecision
        val provider = providerForProgress(progress) ?: return nativeDecision
        return runCatching {
            findMethod(provider.javaClass, "i", 2)
                .invoke(provider, progress, System.currentTimeMillis()) as Boolean
        }.getOrElse { error ->
            Log.e(TAG, "Unable to apply origin provider seed policy", error)
            nativeDecision
        }
    }

    private fun providerForContent(contentId: String?): Any? =
        sourceForContent(contentId)?.let(providerBySource::get)

    private fun providerForProgress(progress: Any?): Any? {
        if (progress == null) return null
        val contentId = runCatching {
            findMethod(progress.javaClass, "getContentId", 0).invoke(progress)?.toString()
        }.getOrNull()
        return providerForContent(contentId)
    }

    private fun better(candidate: Any, current: Any, recent: Boolean): Boolean {
        return if (recent) number(candidate, "getLastWatched") > number(current, "getLastWatched")
        else {
            val candidateProgress = number(candidate, "getProgressPercentage").takeIf { it > 0 } ?: number(candidate, "getProgressPercent")
            val currentProgress = number(current, "getProgressPercentage").takeIf { it > 0 } ?: number(current, "getProgressPercent")
            candidateProgress > currentProgress || candidateProgress == currentProgress && number(candidate, "getLastWatched") > number(current, "getLastWatched")
        }
    }

    private fun number(item: Any, name: String): Double =
        (runCatching { findMethod(item.javaClass, name, 0).invoke(item) }.getOrNull() as? Number)?.toDouble() ?: 0.0

    private fun firstProvider(repo: Any): Any? = runCatching {
        val registry = field(repo, "k").get(repo)
        (findMethod(registry.javaClass, "b", 0).invoke(registry) as? Collection<*>)?.firstOrNull()
    }.getOrNull()

    private fun mergedIdentity(delegate: Any?): Any? = runCatching {
        if (delegate == null) return@runCatching null
        val identity = findMethod(delegate.javaClass, "a", 0).invoke(delegate) ?: return@runCatching null
        if (identity !is Enum<*>) return@runCatching identity
        identity.javaClass.enumConstants?.firstOrNull {
            (it as Enum<*>).name == "NUVIO_SYNC"
        } ?: identity
    }.getOrNull()

    @Suppress("UNCHECKED_CAST")
    private fun authenticatedCarrier(requested: Any, predicate: Any): Any {
        return runCatching {
            val repo = repository ?: return@runCatching requested
            val registry = field(repo, "k").get(repo)
            val providers = findMethod(registry.javaClass, "b", 0).invoke(registry) as Collection<Any>
            val test = predicate as kotlin.Function1<Any, Any?>
            for (provider in providers) {
                val providerId = findMethod(provider.javaClass, "a", 0).invoke(provider) ?: continue
                if (test.invoke(providerId) == true) {
                    val name = (providerId as? Enum<*>)?.name ?: continue
                    requested.javaClass.enumConstants.firstOrNull { (it as Enum<*>).name == name }
                        ?.let { return@runCatching it }
                }
            }
            requested.javaClass.enumConstants.firstOrNull { (it as Enum<*>).name == "NUVIO_SYNC" } ?: requested
        }.getOrElse { error ->
            Log.e(TAG, "Unable to select merged provider carrier", error)
            requested.javaClass.enumConstants.firstOrNull { (it as Enum<*>).name == "NUVIO_SYNC" } ?: requested
        }
    }

    private fun defaultValue(type: Class<*>): Any? = when {
        type == java.lang.Boolean.TYPE -> false
        type == java.lang.Integer.TYPE -> 0
        type == java.lang.Long.TYPE -> 0L
        type == java.lang.Float.TYPE -> 0f
        type == java.lang.Double.TYPE -> 0.0
        type == java.lang.Void.TYPE -> null
        else -> null
    }

    private fun enabled() = preferences().getBoolean(ENABLED, false)
    private fun preferences() = application().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun application(): Application = Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null) as Application
    private fun field(owner: Any, name: String): Field = owner.javaClass.getDeclaredField(name).apply { isAccessible = true }
    private fun findMethod(owner: Class<*>, name: String, count: Int): Method = owner.declaredMethods.single { it.name == name && it.parameterCount == count }.apply { isAccessible = true }
}
