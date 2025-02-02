
package app.crossword.yourealwaysbe.forkyz.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class FilteredList<T> extends AbstractList<T> {

    private List<T> unfilteredList = new ArrayList<T>();
    private Function<T, Boolean> filter = null;

    /**
     * Maps position i of the filtered list (if filtered) to position j
     * of the unfiltered one
     */
    private List<Integer> filterMap = null;

    public FilteredList(T... elements) {
        for (T e : elements)
            unfilteredList.add(e);
    }

    public boolean hasFilter() {
        return filter != null;
    }

    public void clearFilter() {
        applyFilter(null);
    }

    public void applyFilter(Function<T, Boolean> filter) {
        this.filter = filter;
        this.filterMap = new ArrayList<>();
        for (int i = 0; i < unfilteredList.size(); i++) {
            if (filter == null || filter.apply(unfilteredList.get(i))) {
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
            if (filter == null || filter.apply(element))
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
