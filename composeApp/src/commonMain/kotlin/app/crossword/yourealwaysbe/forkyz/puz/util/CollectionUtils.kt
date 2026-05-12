
package app.crossword.yourealwaysbe.forkyz.puz.util

import java.util.AbstractCollection
import java.util.stream.Stream

object CollectionUtils {
    /**
     * Simple join of two collections, does not remove duplicates
     */
    fun <T> join(
        collection1 : Collection<T>,
        collection2 : Collection<T>,
    ) : Collection<T> {
        return object : AbstractCollection<T>() {
            override fun iterator() : MutableIterator<T> {
                return Stream.concat<T>(
                    collection1.stream(),
                    collection2.stream(),
                ).iterator()
            }

            override val size : Int
                get () {
                    return collection1.size + collection2.size
                }
        }
    }
}

