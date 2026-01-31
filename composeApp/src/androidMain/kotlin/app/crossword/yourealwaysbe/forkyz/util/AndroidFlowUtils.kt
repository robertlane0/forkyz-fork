
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle

@OptIn(DelicateCoroutinesApi::class)
actual fun <T> Flow<T>.observeForeverWithProcessLifecycle(cb : (T) -> Unit) {
    GlobalScope.launch(Dispatchers.Main) {
        ProcessLifecycleOwner.get().repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect(cb)
        }
    }
}

@Composable
actual fun <T> Flow<T>.collectAsStateWithProcessLifecycle(
    initialValue : T,
) : State<T> {
    return this.collectAsStateWithLifecycle(
        initialValue,
        ProcessLifecycleOwner.get(),
    )
}

