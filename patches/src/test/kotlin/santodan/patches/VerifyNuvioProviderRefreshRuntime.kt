package santodan.patches

import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import software.santodan.extension.nuviomerged.NuvioSourceContinuation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import software.santodan.extension.nuviomerged.NuvioProviderRefresh

enum class RefreshIntent { AUTOMATIC, USER_INITIATED }

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
