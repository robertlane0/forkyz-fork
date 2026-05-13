
package app.crossword.yourealwaysbe.forkyz.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MutableSortedMapTest {
    @Test
    fun testGetMissingKey() {
        val map = MutableSortedMap<String, Int>()
        assertNull(map.get("missing"))
    }

    @Test
    fun testPutAndGet() {
        val map = MutableSortedMap<String, Int>()
        map.put("a", 1)
        assertEquals(1, map.get("a"))
    }

    @Test
    fun testPutUpdatesValue() {
        val map = MutableSortedMap<String, Int>()
        map.put("a", 1)
        map.put("a", 2)
        assertEquals(2, map.get("a"))
    }

    @Test
    fun testKeysAfterOutOfOrderPuts() {
        val map = MutableSortedMap<Int, String>()
        map.put(3, "c")
        map.put(1, "a")
        map.put(2, "b")
        assertEquals(listOf(1, 2, 3), map.keys.toList())
    }

    @Test
    fun testIteratorInKeyOrder() {
        val map = MutableSortedMap<Int, String>()
        map.put(3, "c")
        map.put(1, "a")
        map.put(2, "b")
        assertEquals(
            listOf(
                SortedMapEntry(1, "a"),
                SortedMapEntry(2, "b"),
                SortedMapEntry(3, "c"),
            ),
            map.entries.asSequence().toList(),
        )
    }

    @Test
    fun testInitValsKeysOrdered() {
        val map = MutableSortedMap(mapOf(3 to "c", 1 to "a", 2 to "b"))
        assertEquals(listOf(1, 2, 3), map.keys.toList())
    }

    @Test
    fun testInitValsGet() {
        val map = MutableSortedMap(mapOf(3 to "c", 1 to "a", 2 to "b"))
        assertEquals("a", map.get(1))
        assertEquals("b", map.get(2))
        assertEquals("c", map.get(3))
    }

    @Test
    fun testEmptyKeys() {
        val map = MutableSortedMap<Int, String>()
        assertTrue(map.keys.isEmpty())
    }

    @Test
    fun testEmptyIterator() {
        val map = MutableSortedMap<Int, String>()
        assertEquals(
            emptyList<SortedMapEntry<Int, String>>(),
            map.entries.asSequence().toList(),
        )
    }

    @Test
    fun testContainsKeyPresent() {
        val map = MutableSortedMap<String, Int>()
        map.put("a", 1)
        assertTrue(map.containsKey("a"))
    }

    @Test
    fun testContainsKeyAbsent() {
        val map = MutableSortedMap<String, Int>()
        map.put("a", 1)
        assertFalse(map.containsKey("b"))
    }

    @Test
    fun testIsEmptyTrue() {
        val map = MutableSortedMap<Int, String>()
        assertTrue(map.isEmpty())
    }

    @Test
    fun testIsEmptyFalse() {
        val map = MutableSortedMap<Int, String>()
        map.put(1, "a")
        assertFalse(map.isEmpty())
    }

    @Test
    fun testEquals() {
        val map1 = MutableSortedMap<Int, String>()
        map1.put(1, "a")
        map1.put(2, "b")
        val map2 = MutableSortedMap<Int, String>()
        map2.put(2, "b")
        map2.put(1, "a")
        assertEquals(map1, map2)
    }

    @Test
    fun testNotEqualsDifferentValues() {
        val map1 = MutableSortedMap<Int, String>()
        map1.put(1, "a")
        val map2 = MutableSortedMap<Int, String>()
        map2.put(1, "b")
        assertFalse(map1.equals(map2))
    }

    @Test
    fun testNotEqualsWrongType() {
        val map = MutableSortedMap<Int, String>()
        map.put(1, "a")
        assertFalse(map.equals(mapOf(1 to "a")))
    }
}

