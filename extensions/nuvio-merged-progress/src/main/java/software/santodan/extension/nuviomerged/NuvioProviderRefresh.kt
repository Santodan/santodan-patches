package software.santodan.extension.nuviomerged

import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

/** Await the native refresh before sampling flows that can emit cached values immediately. */
object NuvioProviderRefresh {
    /** Shared by the aggregate's progress and next-up readers. */
    fun retainedSnapshot(native: Flow<Any?>?, merged: StateFlow<List<Any>>,
        ready: StateFlow<Boolean>, allowNative: () -> Boolean): Flow<List<Any>> = flow {
        // Empty is a valid completed snapshot. MDBList/Simkl cold flows can
        // refresh on collection, so racing them against ready.first() is unsafe:
        // first() still waits for cancellation of the native startup operation.
        if (native == null || ready.value || !allowNative()) {
            emitAll(merged)
            return@flow
        }
        val initial = merged.value.takeIf { it.isNotEmpty() } ?: merge(
            native.mapNotNull { (it as? Collection<*>)?.filterNotNull()?.takeIf { values -> values.isNotEmpty() } },
            ready.filter { it }.map { merged.value },
        ).first()
        emit(initial)
        emitAll(merged)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun route(revisions: Flow<Long>, resolve: () -> Flow<Any?>): Flow<Any?> =
        revisions.flatMapLatest { resolve() }

    /** Some native refresh methods signal background work rather than await its completion. */
    @Suppress("UNCHECKED_CAST")
    suspend fun observe(provider: Any, accessors: List<String>, changed: () -> Unit) = coroutineScope {
        accessors.forEach { name ->
            launch {
                val method = provider.javaClass.methods.single { it.name == name && it.parameterCount == 0 }
                val updates = method.invoke(provider) as Flow<Any?>
                updates.distinctUntilChanged().drop(1).collect { changed() }
            }
        }
    }

    suspend fun await(provider: Any, methodName: String) {
        val method = provider.javaClass.methods.single {
            it.name == methodName && it.parameterCount == 2
        }
        val intent = requireNotNull(method.parameterTypes[0].enumConstants).single {
            (it as Enum<*>).name == "AUTOMATIC"
        }
        suspendCancellableCoroutine<Any?> { completion ->
            val result = method.invoke(provider, intent, NuvioSourceContinuation(completion))
            if (result !== COROUTINE_SUSPENDED) completion.resume(result)
        }
    }
}
