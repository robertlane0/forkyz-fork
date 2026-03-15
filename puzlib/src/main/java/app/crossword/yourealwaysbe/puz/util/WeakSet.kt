
package app.crossword.yourealwaysbe.puz.util

import java.util.Collections
import java.util.WeakHashMap

object WeakSet {
    /**
     * Returns a new set of weak references
     */
    fun <T> buildSet() : MutableSet<T?> {
        return Collections.newSetFromMap<T?>(
            Collections.synchronizedMap<T?, Boolean?>(
                WeakHashMap<T?, Boolean?>(),
            )
        )
    }
}

