
package app.crossword.yourealwaysbe.forkyz.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Thanks ChatGPT. (After fixes and improvements.)
 */
public class FilteredListTest {

    private FilteredList<String> filteredList;

    @Before
    public void setUp() {
        // Create a new FilteredList with a simple matching function
        filteredList = new FilteredList<>(String::contains);

        // Add some initial data to the list
        filteredList.addAll(Arrays.asList(new String[] {
            "apple", "banana", "cherry", "date", "fig", "grape", "kiwi",
            "lemon", "melon", "orange"
        }));
    }

    @Test
    public void testSize() {
        // Initial size should be the same as the number of elements added
        assertEquals(10, filteredList.size());

        // Apply a filter and check the size
        filteredList.applyFilter("a");

        assertEquals(5, filteredList.size());

        // Clear the filter and check the size
        filteredList.clearFilter();
        assertEquals(10, filteredList.size());
    }

    @Test
    public void testGet() {
        // Get elements at specific indices
        assertEquals("banana", filteredList.get(1));
        assertEquals("kiwi", filteredList.get(6));

        // Apply a filter and get elements
        filteredList.applyFilter("p");
        assertEquals("apple", filteredList.get(0));
        assertEquals("grape", filteredList.get(1));
    }

    @Test
    public void testSet() {
        // Set an element at a specific index
        filteredList.set(2, "blueberry");
        assertEquals("blueberry", filteredList.get(2));

        // Apply a filter and set an element
        filteredList.applyFilter("a");
        filteredList.set(1, "apricot");
        assertEquals("apricot", filteredList.get(1));
    }

    @Test
    public void testAdd() {
        // Add an element at a specific index
        filteredList.add(5, "lime");
        assertEquals("lime", filteredList.get(5));
        assertEquals("grape", filteredList.get(6));
        assertEquals(11, filteredList.size());

        // Apply a filter and add an element
        filteredList.applyFilter("a");
        assertEquals(5, filteredList.size());

        filteredList.add(2, "avocado");
        assertEquals(6, filteredList.size());
        assertEquals("avocado", filteredList.get(2));
        assertEquals("date", filteredList.get(3));

        filteredList.add(3, "gooseberry");
        assertEquals(6, filteredList.size());
        assertEquals("avocado", filteredList.get(2));
        assertEquals("date", filteredList.get(3));

        filteredList.applyFilter("g");
        assertEquals(4, filteredList.size());
        assertEquals("gooseberry", filteredList.get(0));
        assertEquals("grape", filteredList.get(2));

        filteredList.clearFilter();
        assertEquals(13, filteredList.size());
    }

    @Test
    public void testRemove() {
        // Remove an element at a specific index
        filteredList.remove(3);
        assertEquals("fig", filteredList.get(3));
        assertEquals(9, filteredList.size());

        // Apply a filter and remove an element
        filteredList.applyFilter("a");
        filteredList.remove(1);
        assertEquals("grape", filteredList.get(1));
        assertEquals(3, filteredList.size());

        filteredList.clearFilter();
        assertEquals(8, filteredList.size());
        assertEquals("cherry", filteredList.get(1));
        assertEquals("kiwi", filteredList.get(4));
    }
}

