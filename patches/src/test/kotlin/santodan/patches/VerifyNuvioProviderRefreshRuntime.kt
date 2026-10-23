package santodan.patches

import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import software.santodan.extension.nuviomerged.NuvioSourceContinuation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import software.santodan.extension.nuviomerged.NuvioProviderRefresh

enum class RefreshIntent { AUTOMATIC, USER_INITIATED }

class StartupReporterFixture {
    @JvmField val c = Any()
    @JvmField var f = false
    @JvmField val g = arrayListOf<() -> Unit>()
    fun report() = synchronized(c) {
        f = true
        g.toList().forEach { it() }
        g.clear()
    }
}
class StartupActivityFixture {
    val reporter = StartupReporterFixture()
    fun getFullyDrawnReporter() = reporter
}

class RefreshFixture {
    val seeds = MutableStateFlow<Any?>("episode 8")
    val progress = MutableStateFlow<Any?>("cached progress")
    val watched = MutableStateFlow<Any?>("cached history")
    var completion: Continuation<Any?>? = null
    var intent: RefreshIntent? = null
    fun v(value: RefreshIntent, continuation: NuvioSourceContinuation): Any {
        intent = value
        completion = continuation
        return COROUTINE_SUSPENDED
    }
    fun q(): Flow<Any?> = progress
    fun f(): Flow<Any?> = seeds
    fun d(): Flow<Any?> = watched
}

fun main() = runBlocking {
    withTimeout(5_000) {
        fun launchDocument(xml: String) = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(java.io.ByteArrayInputStream(xml.toByteArray()))
        val launchBackground = launchDocument("""<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle"><solid android:color="@color/splash_background"/></shape>""")
        addNuvioLaunchLogo(launchBackground)
        check(launchBackground.documentElement.tagName == "layer-list")
        check(launchBackground.getElementsByTagName("solid").length == 1)
        check(launchBackground.getElementsByTagName("gradient").length == 2)
        val logoLayer = launchBackground.getElementsByTagName("item").item(3) as org.w3c.dom.Element
        check(logoLayer.getAttribute("android:height") == "48dp" && logoLayer.getAttribute("android:gravity") == "center")
        val logoBitmap = launchBackground.getElementsByTagName("bitmap").item(0) as org.w3c.dom.Element
        check(logoBitmap.getAttribute("android:src") == "@drawable/app_logo_wordmark")
        check(runCatching { addNuvioLaunchLogo(launchBackground) }.isFailure)
        val invalidBackground = launchDocument("""<shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="#ffffff"/></shape>""")
        check(runCatching { addNuvioLaunchLogo(invalidBackground) }.isFailure)
        println("PASS: launch logo preserves native background, centers native artwork and rejects changed/already-patched resources")

        val iconNames = listOf("AppIconDefault", "AppIconArcticBlue", "AppIconEmerald", "AppIconRoseGold", "AppIconCopper", "AppIconGraphite")
        fun manifest() = launchDocument("""<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.nuvio.tv"><application><activity android:name=".MainActivity" android:theme="@style/Theme.MyApplication.Launcher" android:configChanges="orientation|screenSize"><intent-filter><action android:name="android.intent.action.VIEW"/></intent-filter></activity>${iconNames.joinToString("") { """<activity android:name="com.nuvio.tv.launcher.$it" android:enabled="${it == "AppIconDefault"}" android:icon="@mipmap/$it" android:launchMode="singleTop"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LEANBACK_LAUNCHER"/></intent-filter></activity>""" }}</application></manifest>""")
        val startupManifest = manifest()
        configureNuvioStartupManifest(startupManifest)
        val activities = startupManifest.getElementsByTagName("activity")
        check(activities.length == 2)
        val early = activities.item(0) as org.w3c.dom.Element
        check(early.getAttribute("android:name") == NUVIO_STARTUP_ACTIVITY && early.getAttribute("android:exported") == "false")
        check(early.getAttribute("android:noHistory") == "true")
        check((activities.item(1) as org.w3c.dom.Element).getAttribute("android:name") == ".MainActivity")
        check(startupManifest.getElementsByTagName("action").length == 7) // Native deep link preserved.
        val aliases = startupManifest.getElementsByTagName("activity-alias")
        for (i in 0 until aliases.length) {
            val alias = aliases.item(i) as org.w3c.dom.Element
            check(alias.getAttribute("android:targetActivity") == NUVIO_STARTUP_ACTIVITY)
            check(alias.getAttribute("android:enabled") == (i == 0).toString())
            check(alias.getAttribute("android:name") == "com.nuvio.tv.launcher.${iconNames[i]}")
            check(alias.getAttribute("android:icon") == "@mipmap/${iconNames[i]}")
            check(!alias.hasAttribute("android:launchMode"))
        }
        check(runCatching { configureNuvioStartupManifest(startupManifest) }.isFailure)
        val changedManifest = manifest()
        (changedManifest.getElementsByTagName("activity").item(1) as org.w3c.dom.Element).setAttribute("android:name", ".Unexpected")
        check(runCatching { configureNuvioStartupManifest(changedManifest) }.isFailure)

        val startupGate = software.santodan.extension.nuviomerged.NuvioStartupGate()
        startupGate.resumed(true)
        startupGate.drawn()
        check(!startupGate.takeHandoff())
        startupGate.prepared()
        check(!startupGate.takeHandoff()) // An old/default frame cannot release native startup.
        startupGate.drawn()
        startupGate.resumed(false)
        check(!startupGate.takeHandoff())
        startupGate.resumed(true)
        check(startupGate.takeHandoff() && !startupGate.takeHandoff())
        val destroyedGate = software.santodan.extension.nuviomerged.NuvioStartupGate()
        destroyedGate.resumed(true); destroyedGate.prepared(); destroyedGate.drawn(); destroyedGate.destroyed()
        check(!destroyedGate.takeHandoff())
        println("PASS: early splash aliases preserve icon state/deep links; native handoff waits for prepared frame and resume, and runs once")

        val legacyStorage = mutableMapOf<String, Any>(
            "enabled" to true,
            "snapshot_v2_1_main" to "MDBLIST",
            "snapshot_v2_1_last_sync" to "Sync complete",
            "snapshot_v2_1_services" to setOf("MDBLIST"),
            "snapshot_v2_1" to "{\"progress\":[]}",
            "snapshot_v2_2" to "{\"progress\":[1]}"
        )
        val historyStorage = mutableMapOf<String, String>()
        var removals = 0
        check(!NuvioProviderRefresh.migrateSnapshots(legacyStorage, historyStorage::containsKey,
            { false }, { removals++; true }))
        check(removals == 0 && "snapshot_v2_1" in legacyStorage)
        // A failed cleanup leaves both copies intact. Retry must preserve newer
        // history and keep all settings, summaries and other profile choices.
        check(!NuvioProviderRefresh.migrateSnapshots(legacyStorage, historyStorage::containsKey,
            { historyStorage.putAll(it); true }, { false }))
        check(historyStorage.size == 2 && "snapshot_v2_2" in legacyStorage)
        historyStorage["snapshot_v2_1"] = "{\"progress\":[\"newer\"]}"
        var durableWrites = 0
        check(NuvioProviderRefresh.migrateSnapshots(legacyStorage, historyStorage::containsKey,
            { check(it.isEmpty()); durableWrites++; true }, { keys ->
                keys.forEach(legacyStorage::remove)
                true
            }))
        check(durableWrites == 1 && historyStorage.getValue("snapshot_v2_1").contains("newer"))
        check(legacyStorage.size == 4 && legacyStorage["enabled"] == true)
        check(NuvioProviderRefresh.migrateSnapshots(legacyStorage, historyStorage::containsKey,
            { error("Settings-only storage must not load history") }, { error("Unexpected cleanup") }))
        println("PASS: history moves out of settings across profiles; failed writes preserve cache and retries retain newer history")

        val activity = StartupActivityFixture()
        var historyLoads = 0
        var refreshes = 0
        NuvioProviderRefresh.afterPresentation(activity) { historyLoads++; refreshes++ }
        check(historyLoads == 0 && refreshes == 0)
        // Cache readers work independently while expensive startup work is gated.
        val lightweight = MutableStateFlow<List<Any>>(listOf("cached CW"))
        check(NuvioProviderRefresh.hydratedSnapshot(lightweight) {}.first() == listOf("cached CW"))
        check(historyLoads == 0 && refreshes == 0)
        activity.reporter.report()
        check(historyLoads == 1 && refreshes == 1)
        activity.reporter.report()
        check(historyLoads == 1 && refreshes == 1)
        NuvioProviderRefresh.afterPresentation(activity) { refreshes++ }
        check(refreshes == 2) // Already-drawn activities release immediately.
        val otherActivity = StartupActivityFixture()
        NuvioProviderRefresh.afterPresentation(otherActivity) { historyLoads++ }
        check(historyLoads == 1)
        println("PASS: cached CW is available before Home presentation; deferred work waits for the owning reporter")

        // Cold startup must restore disk data before the first publication.
        // A refresh retains that list until its complete replacement is ready.
        val cached = listOf<Any>("unchanged", "old progress", "removed")
        val snapshot = MutableStateFlow<List<Any>>(emptyList())
        val hydrated = CompletableDeferred<Unit>()
        val publications = mutableListOf<List<Any>>()
        val reader = launch(start = CoroutineStart.UNDISPATCHED) {
            NuvioProviderRefresh.hydratedSnapshot(snapshot) {
                hydrated.await()
                snapshot.value = cached
            }.collect { publications.add(it) }
        }
        yield()
        check(publications.isEmpty()) { "Published empty startup snapshot before restoration" }
        hydrated.complete(Unit)
        yield()
        check(publications == listOf(cached))
        // Failed/cancelled refreshes make no publication; successful refreshes
        // preserve unchanged entries and apply updates, additions and removals.
        yield()
        check(publications.last() == cached)
        val refreshed = listOf<Any>("unchanged", "new progress", "added")
        snapshot.value = refreshed
        yield()
        check(publications == listOf(cached, refreshed))
        snapshot.value = refreshed.toList()
        yield()
        check(publications.size == 2) { "Unchanged refresh republished the snapshot" }
        reader.cancelAndJoin()
        val blocked = CompletableDeferred<Unit>()
        val cancelledReader = launch(start = CoroutineStart.UNDISPATCHED) {
            NuvioProviderRefresh.hydratedSnapshot(snapshot) { blocked.await() }
                .collect { error("Cancelled restoration published data") }
        }
        cancelledReader.cancelAndJoin()
        val empty = MutableStateFlow<List<Any>>(emptyList())
        check(NuvioProviderRefresh.hydratedSnapshot(empty) {}.first().isEmpty())
        println("PASS: cache hydration precedes publication and refresh preserves entries")

        // MDBList progress/seed flows run ensureLoaded/refreshAsync on collection.
        // A complete but empty snapshot is valid, especially without Nuvio Sync.
        for ((ready, allowNative) in listOf(true to false, false to false, true to true)) {
            val snapshot = MutableStateFlow<List<Any>>(emptyList())
            var nativeReads = 0
            val networkBacked = flow<Any?> {
                nativeReads++
                awaitCancellation()
            }
            val result = NuvioProviderRefresh.retainedSnapshot(networkBacked, snapshot,
                MutableStateFlow(ready)) { allowNative }.first()
            check(result.isEmpty() && nativeReads == 0) {
                "Playback resume collected a native refresh flow for an empty snapshot (ready=$ready, nativeReads=$nativeReads)"
            }
        }
        val emptySnapshot = MutableStateFlow<List<Any>>(emptyList())
        val snapshotReady = MutableStateFlow(true)
        val snapshotValues = kotlinx.coroutines.channels.Channel<List<Any>>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val snapshotReader = launch {
            NuvioProviderRefresh.retainedSnapshot(flow { error("Unexpected native refresh") }, emptySnapshot,
                snapshotReady) { false }.collect { snapshotValues.send(it) }
        }
        check(snapshotValues.receive().isEmpty())
        emptySnapshot.value = listOf("episode 7")
        check(snapshotValues.receive() == listOf("episode 7")) { "Snapshot reader stopped observing Home sync publication" }
        snapshotReader.cancelAndJoin()

        val startupNative = MutableStateFlow<Any?>(emptyList<Any>())
        val startup = async {
            NuvioProviderRefresh.retainedSnapshot(startupNative, MutableStateFlow(emptyList()),
                MutableStateFlow(false)) { true }.first()
        }
        yield(); yield()
        check(!startup.isCompleted) { "Startup fallback forwarded the provider's temporary empty value" }
        startupNative.value = listOf("cached episode")
        check(startup.await() == listOf("cached episode")) { "Home startup lost the cached native fallback" }
        println("PASS: empty completed snapshots and playback resume never collect refresh-on-start flows; Home startup fallback and live snapshot updates retained")
        for (method in listOf("v")) {
            val provider = RefreshFixture()
            val refresh = async(start = CoroutineStart.UNDISPATCHED) {
                NuvioProviderRefresh.await(provider, method)
                provider.seeds.first()
            }
            check(!refresh.isCompleted && provider.intent == RefreshIntent.AUTOMATIC)
            provider.seeds.value = "episode 20"
            provider.completion!!.resumeWith(Result.success(Unit))
            check(refresh.await() == "episode 20") { "Refresh sampled stale cached seeds" }

            val failed = async {
                runCatching { NuvioProviderRefresh.await(provider, method) }
            }
            yield()
            provider.completion!!.resumeWith(Result.failure(IllegalStateException("remote failure")))
            check(failed.await().isFailure) { "Failed synchronization appeared successful" }
            check(withTimeoutOrNull(50) {
                NuvioProviderRefresh.await(provider, method)
                true
            } == null) { "Stalled refresh ignored cancellation" }
            provider.completion!!.resumeWith(Result.success(Unit))
        }

        val provider = RefreshFixture()
        val changed = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val observer = launch { NuvioProviderRefresh.observe(provider, listOf("q", "f", "d")) { changed.trySend(Unit) } }
        yield(); yield()
        check(changed.tryReceive().isFailure) { "Initial cached emissions caused a refresh loop" }
        for (flow in listOf(provider.progress, provider.seeds, provider.watched)) {
            flow.value = "remote update"
            changed.receive()
            flow.value = "remote update"
            yield()
            check(changed.tryReceive().isFailure) { "Unchanged values caused a refresh loop" }
        }
        observer.cancelAndJoin()

        val revisions = MutableStateFlow(0L)
        val carrier = MutableStateFlow<Any?>("unwatched on carrier")
        val winner = MutableStateFlow<Any?>("watched on winner")
        var selected = carrier
        val values = kotlinx.coroutines.channels.Channel<Any?>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val collector = launch { NuvioProviderRefresh.route(revisions) { selected }.collect { values.send(it) } }
        check(values.receive() == "unwatched on carrier")
        selected = winner
        revisions.value++
        check(values.receive() == "watched on winner") { "Open detail collector stayed on the carrier" }
        carrier.value = "stale carrier update"
        winner.value = "new watched history"
        check(values.receive() == "new watched history")
        collector.cancelAndJoin()
    }
    println("PASS: awaited refresh, failure propagation, delayed provider updates, duplicate suppression and live detail rerouting")
}
