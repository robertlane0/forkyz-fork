
package app.crossword.yourealwaysbe.puz;

public record ClueID(String listName, int index) implements Comparable<ClueID> {
    public String getListName() { return listName; }
    public int getIndex() { return index; }

    @Override
    public int compareTo(ClueID other) {
        if (other == null)
            return 1;

        String list = getListName();
        String otherList = other.getListName();

        if (list == null && otherList != null)
            return -1;

        if (list != null && otherList == null)
            return 1;

        int listCompare = list.compareTo(otherList);

        if (listCompare != 0)
            return listCompare;

        return Integer.compare(getIndex(), other.getIndex());
    }

    public String toString() {
        return getListName() + "[" + getIndex() + "]";
    }
}

