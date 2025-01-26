
package app.crossword.yourealwaysbe.forkyz.util

import java.util.function.Consumer

import androidx.lifecycle.LiveData
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
