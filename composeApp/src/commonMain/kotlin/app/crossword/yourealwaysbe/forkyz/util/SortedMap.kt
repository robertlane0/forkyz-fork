/**
 * Simple KMP sorted map implementation for light use in puz package
 */

package app.crossword.yourealwaysbe.forkyz.util

interface SortedMap<K : Comparable<K>, V> {
    val entries : Iterator<SortedMapEntry<K, V>>
    val keys : SortedSet<K>
    fun containsKey(key : K) : Boolean
    fun isEmpty() : Boolean
    fun put(key : K, value : V)
    operator fun get(key : K) : V?
}

data class SortedMapEntry<K, V> (
    val key : K,
    val value : V,
)

class MutableSortedMap<K : Comparable<K>, V>(
    val initVals : Map<K, V> = mapOf<K, V>(),
) : SortedMap<K, V> {
    private val map = mutableMapOf<K, V>()
    private val keyList = mutableListOf<K>()

    init {
        for (entry in initVals) {
            put(entry.key, entry.value)
        }
    }

    override val entries : Iterator<SortedMapEntry<K, V>>
        get() {
            return sequence {
                for (key in keyList) {
                    get(key)?.let { yield(SortedMapEntry(key, it as V)) }
                }
            }.iterator()
        }

    override val keys : SortedSet<K>
        get() { return SortedSetView(keyList) }

    override fun containsKey(key : K) : Boolean {
        return map.containsKey(key)
    }

    override operator fun get(key : K) : V? {
        return map.get(key)
    }

    override fun isEmpty() : Boolean {
        return map.isEmpty()
    }

    override fun put(key : K, value : V) {
        if (!map.containsKey(key)) {
            val insertionPoint = -keyList.binarySearch(key) - 1
            keyList.add(insertionPoint, key)
        }
        map.put(key, value)
    }

    override fun hashCode() : Int {
        return map.hashCode()
    }

    override fun equals(other : Any?) : Boolean {
        if (other is MutableSortedMap<*, *>) {
            return map.equals(other.map)
        } else {
            return false
        }
    }

    override fun toString() : String {
        return "Order: $keyList\nMap: $map"
    }
}

