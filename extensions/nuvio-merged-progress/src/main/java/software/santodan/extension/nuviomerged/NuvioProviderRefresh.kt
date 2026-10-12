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

/** Await the native refresh before sampling flows that can emit cached values immediately. */
object NuvioProviderRefresh {
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
