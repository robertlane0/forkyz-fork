
package app.crossword.yourealwaysbe.forkyz.puz

import app.crossword.yourealwaysbe.forkyz.util.MutableSortedMap
import app.crossword.yourealwaysbe.forkyz.util.SortedMap
import app.crossword.yourealwaysbe.forkyz.util.SortedSet

class Box {
    companion object {
        const val DEFAULT_FLAG_COLOR = -1
        const val BLANK = " "
        private const val NOCOLOR = -1

        fun isBlock(box : Box?) : Boolean = box == null || box.isBlock
    }

    enum class Bar { NONE, SOLID, DASHED, DOTTED }

    enum class Shape {
        CIRCLE, ARROW_LEFT, ARROW_RIGHT, ARROW_UP, ARROW_DOWN,
        TRIANGLE_LEFT, TRIANGLE_RIGHT, TRIANGLE_UP, TRIANGLE_DOWN,
        DIAMOND, CLUB, HEART, SPADE, STAR, SQUARE, RHOMBUS,
        FORWARD_SLASH, BACK_SLASH, X
    }

    // use if block has styles (such as colour)
    var isBlock : Boolean = false
    var responder : String? = null
    var isCheated : Boolean = false

    /**
     * When setting this, you should also setResponse
     *
     * This field will not actually be rendered, it's just used when
     * outputting the puzzle without its saved state.
     */
    var initialValue : String? = null
    var response : String = BLANK
    var solution : String? = null
    var clueNumber : String? = null
    // for each clue this box is a part of, the index of the cell it is
    // the clue word, sorted for consistency of cycling through clues
    private val cluePositions = MutableSortedMap<ClueID, Int>()

    var barTop : Bar = Bar.NONE
    var barBottom : Bar = Bar.NONE
    var barLeft : Bar = Bar.NONE
    var barRight : Bar = Bar.NONE

    var shape : Shape? = null

    // 3x3 grid of small text marks
    var marks : Array<Array<String?>>? = null
        private set

    // 24-bit representation 0x00rrggbb
    var color : Int = NOCOLOR
    var textColor : Int = NOCOLOR
    var barColor : Int = NOCOLOR

    /**
     * Flag box
     *
     * During play, call Playboard.flagPosition to make sure listeners
     * updated.
     */
    var isFlagged : Boolean = false

    // 0x00rrggbb or DEFAULT_FLAG_COLOR
    var flagColor : Int = DEFAULT_FLAG_COLOR

    override fun equals(other : Any?) : Boolean {
        if (this === other) return true
        if (other !is Box) return false

        if (cluePositions != other.cluePositions) return false
        if (isCheated != other.isCheated) return false
        if (clueNumber != other.clueNumber) return false
        if (shape != other.shape) return false
        if (isBlock != other.isBlock) return false
        if (responder != other.responder) return false
        if (initialValue != other.initialValue) return false
        if (response != other.response) return false
        if (solution != other.solution) return false
        if (barTop != other.barTop) return false
        if (barBottom != other.barBottom) return false
        if (barLeft != other.barLeft) return false
        if (barRight != other.barRight) return false
        if (color != other.color) return false
        if (textColor != other.textColor) return false
        if (barColor != other.barColor) return false

        // Annoying Arrays.equals doesn't do arrays of arrays..
        val marks = marks
        val otherMarks = other.marks
        if (marks != null || otherMarks != null) {
            if (marks == null || otherMarks == null) return false
            if (!marks.contentDeepEquals(otherMarks)) return false
        }

        if (isFlagged != other.isFlagged) return false
        if (flagColor != other.flagColor) return false

        return true
    }

    override fun hashCode() : Int {
        val prime = 31
        var result = 1
        result = (prime * result) + cluePositions.hashCode()
        result = (prime * result) + (if (isCheated) 1231 else 1237)
        result = (prime * result) + (clueNumber?.hashCode() ?: 0)
        result = (prime * result) + (shape?.hashCode() ?: 0)
        result = (prime * result) + arrayOf(
            barTop, barBottom, barLeft, barRight
        ).contentHashCode()
        result = (prime * result) + (responder?.hashCode() ?: 0)
        result = (prime * result) + (initialValue?.hashCode() ?: 0)
        result = (prime * result) + response.hashCode()
        result = (prime * result) + (solution?.hashCode() ?: 0)
        result = (prime * result) + color + textColor + barColor
        result = (prime * result) + (if (isBlock) 1231 else 1237)
        // ignore marks and flags, too awkward and probably empty
        return result
    }

    override fun toString() : String {
        val number = clueNumber
        return if (number != null) "$number$solution " else "$solution "
    }

    /**
     * Check if this box is the start of a clue
     *
     * @param clueID the ID of the clue to check
     * @return if start of clue with ID
     */
    fun isStartOf(clueID : ClueID) : Boolean {
        val position = cluePositions[clueID]
        return position != null && position == 0
    }

    fun hasShape() : Boolean = shape != null

    fun hasInitialValue() : Boolean = initialValue != null

    fun setResponse(response : Char) {
        this.response = response.toString()
    }

    fun hasSolution() : Boolean = solution != null

    fun setSolution(solution : Char) {
        this.solution = solution.toString()
    }

    fun hasClueNumber() : Boolean = clueNumber != null

    fun isBlank() : Boolean = BLANK == response

    fun setBlank() { response = BLANK }

    /** @returns true if box is part of the clue */
    fun isPartOf(clueID : ClueID?) : Boolean {
        if (clueID == null) return false
        return cluePositions.containsKey(clueID)
    }

    fun isPartOf(clue : Clue?) : Boolean {
        if (clue == null) return false
        return isPartOf(clue.clueID)
    }

    /**
     * The clue ids that have this box in their zones
     *
     * Set will iterate in ClueID order.
     */
    fun getIsPartOfClues() : SortedSet<ClueID>
        = cluePositions.keys

    /**
     * True if this box belongs to at least one clue
     */
    fun isPartOfClues() : Boolean = !cluePositions.isEmpty()

    /**
     * Get a clue that this box is part of from the specified list
     *
     * If there are more than one clues from the same list, returns
     * first in ClueID order
     *
     * Null returned if no clue
     */
    fun getIsPartOfClue(listName : String?) : ClueID? {
        if (listName == null) return null
        for (cid in getIsPartOfClues()) {
            if (listName == cid.listName) return cid
        }
        return null
    }

    /**
     * @param position if part of a clue, the position in the
     * word
     */
    fun setCluePosition(clueID : ClueID, position : Int) {
        cluePositions.put(clueID, position)
    }

    /**
     * If a position has already been set for this clue
     **/
    fun hasCluePosition(clueID : ClueID) : Boolean {
        return cluePositions.containsKey(clueID)
    }

    /**
     * Get position of box in clue
     *
     * @return position or -1 if not in clue
     */
    fun getCluePosition(clueID : ClueID) : Int {
        return cluePositions[clueID] ?: -1
    }

    fun hasBars() : Boolean {
        return barTop != Bar.NONE
            || barBottom != Bar.NONE
            || barLeft != Bar.NONE
            || barRight != Bar.NONE
    }

    fun isBarredTop() : Boolean = barTop != Bar.NONE
    fun isBarredBottom() : Boolean = barBottom != Bar.NONE
    fun isBarredLeft() : Boolean = barLeft != Bar.NONE
    fun isBarredRight() : Boolean = barRight != Bar.NONE
    fun isSolidBarredTop() : Boolean = barTop == Bar.SOLID
    fun isSolidBarredBottom() : Boolean = barBottom == Bar.SOLID
    fun isSolidBarredLeft() : Boolean = barLeft == Bar.SOLID
    fun isSolidBarredRight() : Boolean = barRight == Bar.SOLID

    fun hasMarks() : Boolean = marks != null

    /**
     * 3x3 array of text marks to put in box
     *
     * row x col, can have null entries
     */
    fun setMarks(marks : Array<Array<String?>>?) {
        if (marks != null) {
            if (marks.size != 3) {
                throw IllegalArgumentException("Marks array must be 3x3.")
            }
            for (row in marks) {
                if (row.size != 3) {
                    throw IllegalArgumentException(
                        "Marks array must be 3x3."
                    )
                }
            }
        }
        this.marks = marks
    }

    /** 24-bit 0x00rrggbb when has color */
    fun hasColor() : Boolean = color != NOCOLOR
    fun hasTextColor() : Boolean = textColor != NOCOLOR
    fun hasBarColor() : Boolean = barColor != NOCOLOR

    /**
     * If no custom flag color has been set
     **/
    fun isDefaultFlagColor() : Boolean = flagColor == DEFAULT_FLAG_COLOR
}
