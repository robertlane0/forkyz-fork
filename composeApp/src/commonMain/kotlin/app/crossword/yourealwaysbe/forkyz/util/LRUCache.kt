
package app.crossword.yourealwaysbe.forkyz.util

class LRUCache<K, V>(val capacity : Int) {
    private val cache = LinkedHashMap<K, V>()

    suspend fun getOrPut(k : K, defaultValue : suspend () -> V) : V {
        var v = cache.get(k)

        if (v == null) {
            v = defaultValue()
            cache.put(k, v)
        } else {
            cache.remove(k)
            cache.put(k, v)
        }

        while (cache.size > capacity && !cache.isEmpty()) {
            cache.remove(cache.iterator().next().key)
        }

        return v!!
    }
}
