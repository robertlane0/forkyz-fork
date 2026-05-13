
package app.crossword.yourealwaysbe.forkyz.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SortedSetViewTest {
    @Test
    fun testContainsPresent() {
        val set = SortedSetView(listOf(1, 3, 5, 7))
        assertTrue(set.contains(3))
    }

    @Test
    fun testContainsAbsent() {
        val set = SortedSetView(listOf(1, 3, 5, 7))
        assertFalse(set.contains(4))
    }

    @Test
    fun testFirst() {
        val set = SortedSetView(listOf(2, 5, 9))
        assertEquals(2, set.first())
    }

    @Test
    fun testHigherPresent() {
        val set = SortedSetView(listOf(1, 3, 5, 7))
        assertEquals(5, set.higher(3))
    }

    @Test
    fun testHigherLast() {
        val set = SortedSetView(listOf(1, 3, 5, 7))
        assertNull(set.higher(7))
    }

    @Test
    fun testHigherAbsentInRange() {
        val set = SortedSetView(listOf(1, 3, 5, 7))
        assertEquals(5, set.higher(4))
    }

    @Test
    fun testHigherAbsentBeyondMax() {
        val set = SortedSetView(listOf(1, 3, 5, 7))
        assertNull(set.higher(8))
    }

    @Test
    fun testHigherAbsentBelowMin() {
        val set = SortedSetView(listOf(1, 3, 5, 7))
        assertEquals(1, set.higher(0))
    }

    @Test
    fun testIsEmptyTrue() {
        val set = SortedSetView(listOf<Int>())
        assertTrue(set.isEmpty())
    }

    @Test
    fun testIsEmptyFalse() {
        val set = SortedSetView(listOf(1, 2))
        assertFalse(set.isEmpty())
    }

    @Test
    fun testIteratorOrder() {
        val items = listOf(1, 3, 5, 7)
        val set = SortedSetView(items)
        assertEquals(items, set.toList())
    }

    @Test
    fun testEquals() {
        val set1 = SortedSetView(listOf(1, 3, 5))
        val set2 = SortedSetView(listOf(1, 3, 5))
        assertEquals(set1, set2)
    }

    @Test
    fun testNotEqualsDifferentItems() {
        val set1 = SortedSetView(listOf(1, 3, 5))
        val set2 = SortedSetView(listOf(1, 3, 7))
        assertFalse(set1.equals(set2))
    }

    @Test
    fun testNotEqualsWrongType() {
        val set = SortedSetView(listOf(1, 3, 5))
        assertFalse(set.equals(listOf(1, 3, 5)))
    }
}

