
package app.crossword.yourealwaysbe.puz

data class ClueID(val listName: String, val index: Int) : Comparable<ClueID> {
    override fun compareTo(other: ClueID): Int {
        val list = listName
        val otherList = other.listName
        val listCompare = listName.compareTo(other.listName)
        if (listCompare != 0)
            return listCompare

        return Integer.compare(index, other.index)
    }

    override fun toString() : String {
        return listName + "[" + index + "]"
    }
}

