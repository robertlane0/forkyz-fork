
package app.crossword.yourealwaysbe.puz

class MutableClueList : ClueList {
    private var clues : MutableList<Clue> = mutableListOf<Clue>()
    private var numberedClueMap : MutableMap<String, Clue>
        = mutableMapOf<String, Clue>()

    /**
     * Add a clue to the list
     *
     * Clues must be added in contiguous index order.
     */
    fun addClue(clue : Clue) {
        val index = clue.clueID.index
        require(index >= size()) {
            "Clue has same index as existing clue: " + clue
        }
        require(index <= size()) {
            "Clue index leaves gaps in clue list: " + clue
        }

        clues.add(clue)
        if (clue.hasClueNumber()) {
            clue.clueNumber?.let { number ->
                numberedClueMap.put(number, clue)
            }
        }
    }

    override fun iterator() : Iterator<Clue> {
        return clues.iterator()
    }

    override fun getClueByIndex(index : Int) : Clue? {
        return clues.getOrNull(index)
    }

    override fun getClueByNumber(number : String) : Clue? {
        return numberedClueMap.get(number)
    }

    override fun getClues() : Collection<Clue> {
        return clues
    }

    override fun hasClueByNumber(number : String) : Boolean {
        return numberedClueMap.containsKey(number)
    }

    override fun hasClueByIndex(index : Int) : Boolean {
        return 0 <= index && index < size()
    }

    override fun size() : Int {
        return clues.size
    }

    override fun getFirstZonedIndex() : Int {
        return findClueIndex(0, 1, false, true, false)
    }

    override fun getLastZonedIndex() : Int {
        return findClueIndex(size() - 1, -1, false, true, false)
    }

    override fun getNextZonedIndex(startIndex : Int, wrap : Boolean) : Int {
        return findClueIndex(startIndex + 1, 1, false, true, wrap)
    }

    override fun getPreviousZonedIndex(startIndex : Int, wrap : Boolean) : Int {
        return findClueIndex(startIndex - 1, -1, false, true, wrap)
    }

    override fun getClueIndex(number : String) : Int {
        return getClueByNumber(number)?.clueID?.index ?: -1
    }

    override fun hashCode() : Int {
        return clues.hashCode()
    }

    override fun equals(other : Any?) : Boolean {
        if (other !is MutableClueList)
            return false
        return getClues() == other.getClues()
    }

    override fun toString() : String {
        return clues.toString()
    }

    /**
     * Search for next clue index
     *
     * Starts at startIndex (inclusive, possibly wrapped), moves by
     * delta. Can specify if clue is required to have a number or zone
     * and whether to wrap around list.
     *
     * Returns -1 if nothing found.
     */
    private fun findClueIndex(
        startIndex : Int,
        delta : Int,
        hasNumber : Boolean,
        hasZone : Boolean,
        wrap : Boolean,
    ) : Int {
        var startIndex = startIndex
        val len = size()

        if (len == 0)
            return -1

        startIndex = wrapIf(startIndex, len, wrap)

        if (startIndex < 0 || startIndex >= len)
            return -1

        var index = startIndex

        do {
            val clue = getClueByIndex(index)
            val good = (
                clue != null
                && (!hasNumber || clue.hasClueNumber())
                && (!hasZone || clue.hasZone())
            )
            if (good)
                return index
            index = wrapIf(index + delta, len, wrap)
        } while (index != startIndex && 0 <= index && index < len)

        return -1
    }

    /**
     * Wrap value to 0..base-1 if wrap is true
     */
    private fun wrapIf(value : Int, base : Int, wrap : Boolean) : Int {
        return if (wrap) ((value % base) + base) % base else value
    }
}
