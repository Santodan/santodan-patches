package software.santodan.extension.nuviomerged

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicReference

/** CPU work must observe the same cancellation and navigation gate as suspended refreshes. */
object NuvioMergeWork {
    /** Apply newer local playback only; an absent/empty source never clears saved remote entries. */
    fun <T> quickProgress(cached: List<T>, local: List<T>, savedAt: Long,
                         key: (T) -> String, updated: (T) -> Long, completed: (T) -> Boolean): List<T> {
        val retained = cached.associateByTo(LinkedHashMap(), key)
        for (item in local.groupBy(key).values.map { rows -> rows.maxBy(updated) }) {
            val previous = retained[key(item)]
            if ((previous != null && updated(item) > updated(previous)) ||
                (previous == null && savedAt > 0 && updated(item) > savedAt)) {
                if (completed(item)) retained.remove(key(item)) else retained[key(item)] = item
            }
        }
        return retained.values.sortedByDescending(updated)
    }

    /** One post-Home sync per active profile/session; interruption can retry on Home. */
    class HomeRequests {
        private var profile: String? = null
        private var requested = false
        private var complete = false
        @Synchronized fun claim(value: String, presented: Boolean, home: Boolean): Boolean {
            if (!presented || !home) return false
            if (profile != value) { profile = value; requested = false; complete = false }
            if (requested || complete) return false
            requested = true
            return true
        }
        @Synchronized fun finish(value: String, interrupted: Boolean) {
            if (profile != null && profile != value) return
            profile = value
            requested = false
            complete = !interrupted
        }
        @Synchronized fun reset() { profile = null; requested = false; complete = false }
    }

    /** Coalesce a manual request while an automatic worker is finishing cancellation. */
    class ManualRequests {
        private val profile = AtomicReference<String?>()
        fun offer(value: String) { profile.set(value) }
        fun take(value: String): Boolean = profile.getAndSet(null) == value
    }

    /** Disk hydration must not consume the provider refresh deadline. */
    suspend fun <T> stages(valid: () -> Boolean, settleMs: Long = 0,
                          preparationMs: Long = 60_000, refreshMs: Long = 90_000,
                          prepare: suspend (() -> Unit) -> Unit,
                          refresh: suspend (() -> Unit) -> T): T = run(valid, settleMs) {
        withTimeout(preparationMs) { run(valid, work = prepare) }
        withTimeout(refreshMs) { run(valid, work = refresh) }
    }

    /** A queued Main callback must not publish after its worker was cancelled. */
    suspend fun commit(post: (() -> Unit) -> Unit, valid: () -> Boolean, publish: () -> Unit): Boolean =
        suspendCancellableCoroutine { completion ->
            post {
                if (completion.isActive) completion.resumeWith(runCatching {
                    if (valid()) { publish(); true } else false
                })
            }
        }

    suspend fun <T> run(valid: () -> Boolean, settleMs: Long = 0,
                       work: suspend (() -> Unit) -> T): T {
        val context = currentCoroutineContext()
        val checkpoint = {
            context.ensureActive()
            if (!valid()) throw CancellationException("Merge deferred until returning to Home")
        }
        checkpoint()
        if (settleMs > 0) delay(settleMs)
        checkpoint()
        val result = work(checkpoint)
        checkpoint()
        return result
    }
}
