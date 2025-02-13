
package app.crossword.yourealwaysbe.forkyz.util

import java.util.function.Consumer
import java.util.function.Supplier

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

fun <T> LiveData<T>.observeOnce(cb : Consumer<T>) {
    val liveData = this
    liveData.observeForever(object : Observer<T> {
        override fun onChanged(value : T) {
            liveData.removeObserver(this)
            cb.accept(value)
        }
    })
}

/**
 * Watch multiple live data instances
 *
 * Get a callback when they all have an initial value, and further
 * notifications when any of them change. Put together into a live data.
 *
 * The provider arg is called each time all values are available /
 * changed. The returned T object is set as the value of the live data
 * object returned. In this way, the returned live data is kind of a
 * fan-in of the passed liveDatas arguments.
 */
fun <T> liveDataFanIn(
    supplier : Supplier<T>,
    vararg liveDatas : LiveData<*>,
) : MutableLiveData<T> {
    val mediator = MediatorLiveData<T>()
    val seen : MutableSet<Int>
        = liveDatas.map(System::identityHashCode).toMutableSet()

    for (ld in liveDatas) {
        val liveData = ld
        mediator.addSource(liveData, {
            seen.remove(System.identityHashCode(liveData))
            if (seen.isEmpty())
                mediator.setValue(supplier.get())
        })
    }

    return mediator
}
