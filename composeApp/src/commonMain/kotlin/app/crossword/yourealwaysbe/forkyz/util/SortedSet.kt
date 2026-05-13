/**
 * Simple KMP sorted set implementation for light use in puz package
 */

package app.crossword.yourealwaysbe.forkyz.util

interface SortedSet<T : Comparable<T>> : Iterable<T> {
    fun contains(item : T) : Boolean
    fun first() : T
    fun higher(item : T) : T?
    fun isEmpty() : Boolean
}

/**
 * Sorted set view of a sorted list
 */
class SortedSetView<T : Comparable<T>>(
    private val items : List<T>,
) : SortedSet<T> {
    override fun contains(item : T) : Boolean {
        return items.binarySearch(item) >= 0
    }

    override fun first() : T {
        return items[0]
    }

    override fun higher(item : T) : T? {
        val idx = items.binarySearch(item)
        val next = if (idx < 0) -idx - 1 else idx + 1
        return items.getOrNull(next)
    }

    override fun isEmpty() : Boolean {
        return items.isEmpty()
    }

    override fun iterator() : Iterator<T> {
        return items.iterator()
    }

    override fun hashCode() : Int {
        return items.hashCode()
    }

    override fun equals(other : Any?) : Boolean {
        if (other is SortedSetView<*>) {
            return items.equals(other.items)
        } else {
            return false
        }
    }

    override fun toString() : String {
        return items.toString()
    }
}

