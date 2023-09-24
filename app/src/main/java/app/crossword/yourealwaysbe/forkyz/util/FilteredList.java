
package app.crossword.yourealwaysbe.forkyz.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class FilteredList<T> extends AbstractList<T> {

    @FunctionalInterface
    public interface MatchingFunction<T> {
        public boolean matches(T item, String filter);
    }

    private List<T> unfilteredList = new ArrayList<T>();
    private String filter = null;
    /**
     * Maps position i of the filtered list (if filtered) to position j
     * of the unfiltered one
     */
    private List<Integer> filterMap = null;
    private MatchingFunction<T> matcher;

    public FilteredList(@NonNull MatchingFunction<T> matcher) {
        this.matcher = matcher;
    }

    public boolean hasFilter() {
        return filter != null && !filter.isEmpty();
    }

    public void clearFilter() {
        applyFilter(null);
    }

    public void applyFilter(String filter) {
        this.filter = filter;
        this.filterMap = new ArrayList<>();
        for (int i = 0; i < unfilteredList.size(); i++) {
            if (
                filter == null
                || matcher.matches(unfilteredList.get(i), filter)
            ) {
                filterMap.add(i);
            }
        }
    }

    @Override
    public int size() {
        return hasFilter() ? filterMap.size() : unfilteredList.size();
    }

    @Override
    public T get(int index) {
        return unfilteredList.get(getTrueIndex(index));
    }

    @Override
    public T set(int index, T element) {
        return unfilteredList.set(getTrueIndex(index), element);
    }

    @Override
    public void add(int index, T element) {
        if (hasFilter()) {
            int trueIndex = getTrueIndex(index);

            unfilteredList.add(trueIndex, element);

            // bump everything up in filter map
            for (int i = 0; i < filterMap.size(); i++) {
                int filterIndex = filterMap.get(i);
                if (filterIndex >= trueIndex)
                    filterMap.set(i, filterIndex + 1);
            }

            // add new index to filter map if needed
            if (matcher.matches(element, filter))
                filterMap.add(index, trueIndex);
        } else {
            unfilteredList.add(index, element);
        }
    }

    @Override
    public T remove(int index) {
        if (hasFilter()) {
            int trueIndex = getTrueIndex(index);

            filterMap.remove(index);

            // bump everything down in filter map
            for (int i = 0; i < filterMap.size(); i++) {
                int filterIndex = filterMap.get(i);
                if (filterIndex > trueIndex)
                    filterMap.set(i, filterIndex - 1);
            }

            return unfilteredList.remove(trueIndex);
        } else {
            return unfilteredList.remove(index);
        }
    }

    /**
     * Convert an index into an index in the unfiltered list
     *
     * Returns as is if no filter
     */
    private int getTrueIndex(int index) {
        return hasFilter() ? filterMap.get(index) : index;
    }
}
