
package app.crossword.yourealwaysbe.puz

interface ClueList : Iterable<Clue> {
    fun getClueByIndex(index : Int) : Clue?

    fun getClueByNumber(number : String) : Clue?

    /**
     * Get clues, iterator will go in clue order
     */
    fun getClues() : Collection<Clue>

    fun hasClueByNumber(number : String) : Boolean

    fun hasClueByIndex(index : Int) : Boolean

    fun size() : Int

    /**
     * Index of first clue with a zone, or -1
     */
    fun getFirstZonedIndex() : Int

    /**
     * Index of last clue with a zone, or -1
     */
    fun getLastZonedIndex() : Int

    /**
     * First index after startIndex with zone
     *
     * Non-inclusive, specify whether to wrap around
     */
    fun getNextZonedIndex(startIndex : Int, wrap : Boolean) : Int

    /**
     * First index before startIndex with zone
     *
     * Non-inclusive, specify whether to wrap around
     */
    fun getPreviousZonedIndex(startIndex : Int, wrap : Boolean) : Int

    /**
     * Returns index of clue in clue list
     */
    fun getClueIndex(number : String) : Int
}

