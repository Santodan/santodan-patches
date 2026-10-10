package santodan.patches

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import software.santodan.extension.nuviostreams.engine.NextEpisodePreloader
import software.santodan.extension.nuviostreams.engine.StreamTargets

private class PlayerNext(@JvmField val a: String, @JvmField val b: Int, @JvmField val c: Int,
                         @JvmField val h: Boolean = true, @JvmField val i: Boolean? = true,
                         @JvmField val g: String? = "2026-01-01")
private class PlayerState(@JvmField val w0: String = "tt1:1:1", @JvmField val k1: PlayerNext? = PlayerNext("tt1:1:2", 1, 2),
                          @JvmField val g: String = "series", @JvmField val a: Boolean = true,
                          @JvmField val b: Boolean = false, @JvmField val c: Boolean = false,
                          @JvmField val d: Any? = null, @JvmField val Y0: String? = null)

fun main() = runBlocking {
    val parsed = StreamTargets.player(PlayerState(), 1, true)
    check(parsed.next == StreamTargets.Media("series", "tt1:1:2", 1, 2) && parsed.playing)
    check(!StreamTargets.player(PlayerState(b = true), 1, true).playing)
    check(!StreamTargets.player(PlayerState(c = true), 1, true).playing)
    check(!StreamTargets.player(PlayerState(d = Any()), 1, true).playing)
    check(!StreamTargets.player(PlayerState(Y0 = "failed"), 1, true).playing)
    check(StreamTargets.player(PlayerState(k1 = PlayerNext("tt1:1:2", 1, 2, h = false)), 1, true).next == null)
    check(StreamTargets.player(PlayerState(k1 = PlayerNext("tt1:1:2", 1, 2, i = false, g = null)), 1, true).next == null)
    check(StreamTargets.player(PlayerState(g = "movie"), 1, true).next == null)
    println("PASS: native player fields, exact next IDs, unavailable/unaired episodes and playback readiness")

    val ticks = Channel<Unit>()
    val processed = Channel<Unit>(Channel.UNLIMITED)
    var now = 0L
    var state = parsed.copy(enabled = false)
    var permission: NextEpisodePreloader.Snapshot? = null
    val calls = mutableListOf<NextEpisodePreloader.Snapshot>()
    var blocked = false
    var cancelled = 0
    val job = launch {
        NextEpisodePreloader({ state }, { permission = it }, {
            calls.add(it)
            if (blocked) try { awaitCancellation() } finally { cancelled++ }
            true
        }, now = { now }, tick = { processed.send(Unit); ticks.receive() }).run()
    }
    suspend fun step(time: Long, value: NextEpisodePreloader.Snapshot = state) {
        now = time
        state = value
        ticks.send(Unit)
        processed.receive()
        yield()
    }
    try {
        processed.receive()
        step(60_000)
        check(calls.isEmpty())
        step(60_000, parsed.copy(playing = false))
        step(90_000)
        check(calls.isEmpty())
        step(90_000, parsed)
        step(119_999)
        check(calls.isEmpty())
        step(120_000)
        check(calls.size == 1)
        step(120_001) // Observe completion, retaining the native result rather than forcing refresh.
        check(permission == null)
        repeat(20) { step(120_002L + it) }
        check(calls.size == 1)
        step(180_001)
        check(calls.size == 2)
        println("PASS: default off, initial buffering, exact 30-second grace, duplicate suppression and cache re-observation")

        // A resumed or sought episode cannot borrow elapsed time from the previous episode.
        val episode2 = parsed.copy(currentVideoId = "tt1:1:2", next = StreamTargets.Media("series", "tt1:1:3", 1, 3))
        step(200_000, episode2)
        step(220_000)
        check(calls.size == 2)
        step(220_000, episode2.copy(playing = false))
        step(280_000)
        step(280_000, episode2)
        step(289_999)
        check(calls.size == 2)
        blocked = true
        step(290_000)
        check(calls.size == 3 && permission?.next == episode2.next)
        step(290_001, episode2.copy(enabled = false))
        check(cancelled == 1 && permission == null)
        println("PASS: episode reset, paused grace period and disabling cancels an active search")

        step(300_000, episode2)
        step(330_000)
        check(calls.size == 4)
        step(330_001, episode2.copy(profile = 2))
        check(cancelled == 2 && permission == null)
        step(360_001)
        check(calls.size == 5 && calls.last().profile == 2)
        step(360_002, state.copy(next = null))
        check(cancelled == 3 && permission == null)
        step(360_003, state.copy(next = StreamTargets.Media("series", "addon:exact:3:1", 3, 1)))
        check(calls.size == 6 && calls.last().next?.videoId == "addon:exact:3:1")
        job.cancelAndJoin()
        check(cancelled == 4 && permission == null)
        println("PASS: profile isolation, late metadata, exact cross-season target and disposal cancellation")
    } finally { job.cancelAndJoin() }

    now = 400_000
    var attempts = 0
    val retryJob = launch {
        NextEpisodePreloader({ state }, { permission = it }, {
            attempts++
            if (attempts == 1) throw CancellationException("Native search became stale")
            true
        }, now = { now }, tick = { processed.send(Unit); ticks.receive() }).run()
    }
    try {
        processed.receive()
        step(430_000)
        check(attempts == 1)
        step(430_001)
        check(retryJob.isActive && permission == null)
        step(445_000)
        check(attempts == 1)
        step(445_001)
        check(attempts == 2)
        println("PASS: native search cancellation retains the player observer and retries after 15 seconds")
    } finally { retryJob.cancelAndJoin() }
}
