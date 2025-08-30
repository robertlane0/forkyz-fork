
package app.crossword.yourealwaysbe.forkyz.util

import java.util.function.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

// 5000ms is apparently LiveData default for timing out on
// subscriptions, someone said, on the internet
private val SUBSCRIBED_TIMEOUT = 5000L

val defaultScope = CoroutineScope(Dispatchers.Default)
val mainScope = CoroutineScope(Dispatchers.Main)

/**
 * A StateFlow MediatorLiveData-alike
 *
 * To store a state and emit updates to a flow, that may also be updated
 * by changes coming from other flows.
 *
 * The mediate is a flow of values that should trigger an update of T. When
 * they get a new value, onUpdate is called, that should produce the new value
 * of T, that will then go to the output. onUpdate takes the current
 * state value and returns the new.
 *
 * The stateflow runs in scope and is only started when subscribed. The
 * initial value is needed for this. Once subscribed, changes for
 * mediates will also trigger onUpdate calls.
 *
 * The value can be set directly with current. Current will usually be
 * more up to date than the stateFlow.
 *
 * Common usecase: a UIState that gets updated when settings values
 * change, but also might be changed by UI events. The mediates are the
 * settings values, that are combined into a single flow with M being a
 * dataclass containing the items. Setting current is used to make new
 * states from UI events. The current value is up to date.
 */
class MediatedStateWithFlow<T, M>(
    scope : CoroutineScope,
    initialValue : T,
    onUpdate : (T, M) -> T,
    mediate : Flow<M>,
) {
    private val emitter = MutableStateFlow<T>(initialValue)

    /**
     * The current value of the state
     *
     * Setting the value will cause the value to be emitted to the flow.
     *
     * Will be updated by onUpdate when mediates get an update (which
     * requires stateFlow to be subscribed to)
     */
    private var _current : T = initialValue
    var current : T = initialValue
        set(value) {
            field = value
            emitter.value = value
        }

    /**
     * Emits each state update
     *
     * May be behind the value of current, so use current if you need up
     * to date value.
     */
    val stateFlow : StateFlow<T> = merge(
        mediate.map { m ->
            // value reaches end of flow via emitter
            current = onUpdate(current, m)
            // this null will be filtered so that updates exit the
            // flow in the right order, the merge is only to manage
            // subscriptions
            null
        }.filterNotNull(),
        emitter,
    ).stateInSubscribed(scope, initialValue)
}

/**
 * Basically combine, but make a WhileSubscribed StateFlow with stateIn
 *
 * onUpdate takes no arguments but gets called when a source emits. Only
 * really useful if your sources are StateFlows so you can read them in
 * your onUpdate.
 */
fun <T, M> mediate(
    scope : CoroutineScope,
    initialValue : T,
    onUpdate : (M) -> T,
    mediates : Flow<M>,
) : StateFlow<T> {
    return mediates.map(onUpdate)
        .stateInSubscribed(scope, initialValue)
}

/**
 * Shortcut for stateIn with WhileSubscribed with livedata timeout
 */
fun <T> Flow<T>.stateInSubscribed(
    scope : CoroutineScope,
    initialValue : T,
) : StateFlow<T> {
    return this.stateIn(
        scope,
        SharingStarted.WhileSubscribed(SUBSCRIBED_TIMEOUT),
        initialValue,
    )
}

/**
 * Shortcut for shareIn WhileSubscribed with livedata timeout and replay=1
 */
fun <T> Flow<T>.shareInSubscribed(scope : CoroutineScope) : SharedFlow<T> {
    return this.shareIn(
        scope,
        SharingStarted.WhileSubscribed(SUBSCRIBED_TIMEOUT),
        replay = 1,
    )
}

/**
 * Get a value once and return async on main via callback
 *
 * Gets on main scope
 */
fun <T> Flow<T>.getOnce(cb : (T) -> Unit) {
    mainScope.launch { cb(first()) }
}

/**
 * Get a value once and return async on main via callback
 *
 * Gets on Default returns on Main
 */
fun <T> Flow<T?>.getOnceNotNull(cb : (T) -> Unit) {
    this.filterNotNull().getOnce(cb)
}

/**
 * Like observeForever on LiveData
 *
 * Uses mainScope. Consumer for BackgroundDownloadManager.java
 *
 * Uses ProcessLifecycleOwner for lifecycle.
 */
fun <T> Flow<T>.observeForeverWithLifecycle(cb : Consumer<T>) {
    mainScope.launch {
        ProcessLifecycleOwner.get().repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect(cb::accept)
        }
    }
}

