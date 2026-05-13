
package app.crossword.yourealwaysbe.forkyz.puz

import java.util.Collections
import java.util.LinkedList
import java.util.Objects
import java.util.logging.Logger
import kotlinx.datetime.LocalDate

import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock

enum class PuzzleKind { CROSSWORD, ACROSTIC }

/**
 * Represent a puzzle
 *
 * String should use HTML for formatting (including new lines), except
 * Note objects which do not support it.
 *
 * @param boxes boxes in row, col order, null means black square. Must
 * be a true grid and non-empty.
 */
class Puzzle(var boxes : Array<Array<Box?>>) {
    var author : String? = null
    var copyright : String? = null
    var notes : String? = null
    var title : String? = null
    var introMessage : String? = null
    var completionMessage : String? = null
    var date : LocalDate? = null
    var source : String? = null

    /**
      * The actual URL of the puzzle
      */
    var sourceUrl : String? = null

    /**
     * The URL to use if you want to share the puzzle
     *
     * E.g. the puzzle page of the site it came from (as opposed to
     * the backend .puz link
     */
    var shareUrl : String? = null

    /**
     * A URL where the puzzle creator can be supported
     *
     * Either by visits or money &c.
     */
    var supportUrl : String? = null

    /**
     * Time taken so far in milliseconds
     */
    var time : Long = 0

    var isScrambled : Boolean = false
    var solutionChecksum : Short = 0
    var rating : Char = 0.toChar()

    /**
     * Currently selected clue id or null
     */
    var currentClueID : ClueID? = null

    /**
     * Clue that should be displayed permanently somewhere.
     *
     * E.g. a solution word for a crossword.
     */
    var pinnedClueID : ClueID? = null

    var kind : PuzzleKind? = PuzzleKind.CROSSWORD
    var playerNote : Note? = null

    private val _images : MutableList<PuzImage> = mutableListOf()
    private val clueLists : MutableMap<String, MutableClueList> = mutableMapOf()
    private val clueNotes : MutableMap<ClueID, Note> = mutableMapOf()
    private val historyList : MutableList<ClueID> = mutableListOf()

    /**
     * @return the height
     */
    var height : Int = 0
        private set

    /**
     * @return the width
     */
    var width : Int = 0
        private set

    init {
        this.height = boxes.size
        this.width = if (height > 0) boxes[0].size else 0

        // check is grid
        for (row in boxes.indices) {
            require(boxes[row].size == width) { "Boxes do not form a grid" }
        }
    }

    /**
     * Current play position data needed for saving state.
     *
     * Will only take if position is not a block
     *
     * Note: this field must be initialised after the height/width and
     * clues are added to boxes (see init above)
     */
    var position : Position = getFirstPosition()
        set(value) {
            if (!isBlock(checkedGetBox(value)))
                field = value
        }

    val images : List<PuzImage> = _images

    fun addClue(clue : Clue) {
        val listName = clue.clueID.listName
        if (!clueLists.containsKey(listName))
            clueLists.put(listName, MutableClueList())
        clueLists.get(listName)?.addClue(clue)
        addClueToBoxes(clue)
        if (currentClueID == null) {
            if (position in clue.zone)
                currentClueID = clue.clueID
        }
    }

    fun getClues(listName : String?) : ClueList? {
        if (listName != null)
            return clueLists.get(listName)
        else
            return null
    }

    val allClues : Sequence<Clue>
        get() {
            var clues = sequenceOf<Clue>()
            for (listName in this.clueListNames) {
                clues = clues + getClues(listName)!!.getClues().asSequence()
            }
            return clues
        }

    val clueListNames : MutableSet<String>
        get() = clueLists.keys

    fun hasClue(clueID : ClueID?) : Boolean {
        if (clueID == null)
            return false

        val clues = getClues(clueID.listName)

        if (clues == null)
            return false

        if (!clues.hasClueByIndex(clueID.index))
            return false

        return true
    }

    /**
     * Get clue by id
     *
     * @return clue or null
     */
    fun getClue(clueID : ClueID?) : Clue? {
        if (clueID == null)
            return null

        return getClues(clueID.listName)?.getClueByIndex(clueID.index)
    }

    val numberOfClues : Int
        get() {
            var count = 0
            for (listName in this.clueListNames)
                count += getClues(listName)!!.size()
            return count
        }

    /**
     * Return null if index out of range or not a box
     */
    fun checkedGetBox(row : Int, col : Int) : Box? {
        if (row < 0 || row >= boxes.size)
            return null
        if (col < 0 || col >= boxes[row].size)
            return null
        return boxes[row][col]
    }

    /**
     * Return null if index out of range or not a box
     */
    fun checkedGetBox(p : Position?) : Box? {
        if (p != null)
            return checkedGetBox(p.row, p.col)
        else
            return null
    }

    val boxesList : Array<Box?>
        get() {
            return Array<Box?>(width * height) { i ->
                val row = i / width
                val col = i % width
                boxes[row][col]
            }
        }

    val numberOfBoxes : Int
        /**
         * Get number of entry boxes in puzzle
         */
        get() {
            var totalBoxes = 0
            for (b in this.boxesList) {
                if (!isBlock(b))
                    totalBoxes++
            }
            return totalBoxes
        }

    val numberOfCheatedBoxes : Int
        /**
         * Get number of hinted entry boxes in puzzle
         */
        get() {
            var cheatedBoxes = 0
            for (b in this.boxesList) {
                if (!isBlock(b) && (b?.isCheated ?: false))
                    cheatedBoxes++
            }
            return cheatedBoxes
        }

    val boardClueIDs : Iterable<ClueID>
        /**
         * Iterate over clue starts in board order
         *
         * Left to right, top to bottom, clue list order by name (across
         * before down in particular).
         *
         * Caution: will not find clues whose IDs don't neatly match the
         * numbers on the board.
         */
        get() = object : Iterable<ClueID> {
            override fun iterator() : Iterator<ClueID> {
                return object : Iterator<ClueID> {
                    // next position (0, 0, across) -> (0, 0, down) -> (0, 1,
                    // across) -> ...
                    private var row = 0
                    private var col = 0
                    private var listIndex = 0
                    private val listNames : MutableList<String>
                        = this@Puzzle.clueLists.keys.sorted().toMutableList()

                    init {
                        moveToNext()
                    }

                    override fun hasNext() : Boolean {
                        return row < height
                    }

                    override fun next() : ClueID {
                        val number = boxes[row][col]!!.clueNumber!!
                        val curList = this.currentClueList!!
                        // not be null by moveToNext
                        val curClue = curList.getClueByNumber(number)!!
                        val result = curClue.clueID
                        moveOneStep()
                        moveToNext()
                        return result
                    }

                    /**
                     * Find next clue/dir position including current position
                     */
                    fun moveToNext() {
                        while (row < height) {
                            val box = boxes[row][col]
                            if (box != null && box.hasClueNumber()) {
                                val number = box.clueNumber!!
                                val clues : ClueList?
                                    = this.currentClueList
                                if (
                                    clues != null
                                    && clues.hasClueByNumber(number)
                                )
                                    return
                            }
                            moveOneStep()
                        }
                    }

                    /**
                     * Move to next position, one step, not to next clue/dir
                     * position
                     */
                    fun moveOneStep() {
                        if (listIndex < listNames.size - 1) {
                            listIndex += 1
                        } else {
                            listIndex = 0
                            col = (col + 1) % width
                            if (col == 0)
                                row += 1
                        }
                    }

                    val currentListName : String?
                        get() = listNames.getOrNull(listIndex)

                    val currentClueList : ClueList?
                        get() = this@Puzzle.getClues(this.currentListName)
                }
            }
        }

    val hasNotes : Boolean
        get() { return !(notes?.isEmpty() ?: true) }

    val hasIntroMessage : Boolean
        get() {
            return !(introMessage?.isEmpty() ?: true)
        }

    val percentComplete : Int
        /**
         * Get percentage of filled solutions that are correct
         *
         * If the box has no solutions in the box, then percent complete is
         * 0. If some boxes have some solutions and some don't, then percent
         * is the percent of boxes with a defined solution.
         */
        get() {
            var total = 0
            var correct = 0

            for (x in boxes.indices) {
                for (y in boxes[x].indices) {
                    boxes[x][y]?.let { box ->
                        if (!isBlock(box) && box.hasSolution()) {
                            total++
                            if (box.response == box.solution) {
                                correct++
                            }
                        }
                    }
                }
            }
            if (total == 0) {
                return 0
            }
            return (correct * 100) / (total)
        }

    val percentFilled : Int
        get() {
            var total = 0
            var filled = 0

            for (x in boxes.indices) {
                for (y in boxes[x].indices) {
                    boxes[x][y]?.let { box ->
                        if (!isBlock(box)) {
                            total++
                            if (!box.isBlank())
                                filled++
                        }
                    }
                }
            }

            return (filled * 100) / (total)
        }

    val percentHinted : Int
        /**
         * Percent of boxes hinted, rounded up to 1 if below 0%
         */
        get() {
            var total = 0
            var cheated = 0

            for (x in boxes.indices) {
                for (y in boxes[x].indices) {
                    boxes[x][y]?.let { box ->
                        if (!isBlock(box)) {
                            total++
                            if (box.isCheated)
                                cheated++
                        }
                    }
                }
            }

            val pcnt = (cheated * 100) / (total)
            return if (cheated > 0 && pcnt == 0) 1 else pcnt
        }

    val currentClue : Clue?
        /**
         * Get currently selected clue or null
         */
        get() = getClue(this.currentClueID)

    /**
     * Returns null if no note
     */
    fun getNote(clueID : ClueID?) : Note? {
        return clueNotes.get(clueID)
    }

    fun getNote(clue : Clue?) : Note? {
        return if (clue == null) null else getNote(clue.clueID)
    }

    /**
     * Set note for a clue only if clue exists in puzzle
     */
    fun setNote(clueID : ClueID?, note : Note?) {
        if (clueID == null || note == null || !hasClue(clueID))
            return
        clueNotes.put(clueID, note)
    }

    fun setNote(clue : Clue?, note : Note?) {
        if (clue != null)
            setNote(clue.clueID, note)
    }

    /**
     * Returns true if some box has a solution set
     */
    fun hasSolution() : Boolean {
        for (row in boxes.indices) {
            for (col in boxes[row].indices) {
                boxes[row][col]?.let { box ->
                    if (!isBlock(box) && box.hasSolution())
                        return true
                }
            }
        }
        return false
    }

    /**
     * Returns true if some box has cheated
     */
    fun hasCheated() : Boolean {
        for (row in boxes.indices) {
            for (col in boxes[row].indices) {
                boxes[row][col]?.let { box ->
                    if (!isBlock(box) && box.isCheated)
                        return true
                }
            }
        }
        return false
    }

    /**
     * Returns true if some box has a responder set
     */
    fun hasResponders() : Boolean {
        for (row in boxes.indices) {
            for (col in boxes[row].indices) {
                boxes[row][col]?.let { box ->
                    if (!isBlock(box) && box.responder != null)
                        return true
                }
            }
        }
        return false
    }

    /**
     * Returns true if some box has a flag set
     */
    fun hasBoxFlags() : Boolean {
        for (row in boxes.indices) {
            for (col in boxes[row].indices) {
                boxes[row][col]?.let { box ->
                    if (!isBlock(box) && box.isFlagged)
                        return true
                }
            }
        }
        return false
    }

    /**
     * Returns true if some clue has separators defined
     */
    fun hasClueSeparators() : Boolean {
        for (listName in this.clueListNames) {
            for (clue in getClues(listName)!!) {
                if (clue.hasSeparators())
                    return true
            }
        }
        return false
    }

    /**
     * Returns true if some box has a shape
     */
    fun hasShaped() : Boolean {
        for (row in boxes.indices) {
            for (col in boxes[row].indices) {
                val box = boxes[row][col]
                if (box != null && box.hasShape())
                    return true
            }
        }
        return false
    }

    /**
     * Returns true if some non-block has an initial value
     */
    fun hasInitialValueCells() : Boolean {
        for (row in boxes.indices) {
            for (col in boxes[row].indices) {
                boxes[row][col]?.let { box ->
                    if (!isBlock(box) && box.hasInitialValue())
                        return true
                }
            }
        }
        return false
    }

    val hasPinnedClueID : Boolean
        get() {
            return pinnedClueID != null
        }

    val pinnedClue : Clue?
        get() = getClue(this.pinnedClueID)

    override fun toString() : String {
        return "Puzzle " + height + " x " + width + " " + this.title
    }

    /**
     * Update history of selected clues
     *
     * Returns position in history list where the clue used to be (or
     * -1).
     */
    fun updateHistory(clueID : ClueID?) : Int {
        if (clueID == null)
            return -1

        var lastHistoryIndex = -1

        if (hasClue(clueID)) {
            lastHistoryIndex = historyList.indexOf(clueID)
            // if a new item, not equal to most recent
            if (lastHistoryIndex != 0) {
                if (lastHistoryIndex > -1)
                    historyList.removeAt(lastHistoryIndex)
                historyList.add(0, clueID)
            }
        }

        return lastHistoryIndex
    }

    var history : MutableList<ClueID>
        get() = historyList
        set(newHistory) {
            historyList.clear()
            for (item in newHistory) {
                if (hasClue(item))
                    historyList.add(item)
            }
        }

    fun addImage(image : PuzImage) {
        _images.add(image)
    }

    override fun equals(other : Any?) : Boolean {
        if (this === other) {
            return true
        }

        if (other == null) {
            return false
        }

        if (other !is Puzzle) {
            return false
        }

        if (clueLists != other.clueLists) {
            return false
        }

        if (author == null) {
            if (other.author != null) {
                return false
            }
        } else if (author != other.author) {
            return false
        }

        val b1 = boxes
        val b2 = other.boxes

        for (x in b1.indices) {
            for (y in b1[x].indices) {
                if (!((b1[x][y] === b2[x][y]) || b1[x][y] == b2[x][y])) {
                    return false
                }
            }
        }

        if (copyright == null) {
            if (other.copyright != null) {
                return false
            }
        } else if (copyright != other.copyright) {
            return false
        }

        if (height != other.height) {
            return false
        }

        if (notes == null) {
            if (other.notes != null) {
                return false
            }
        } else if (notes != other.notes) {
            return false
        }

        if (this.introMessage != other.introMessage)
            return false

        if (this.completionMessage != other.completionMessage)
            return false

        if (title == null) {
            if (other.title != null) {
                return false
            }
        } else if (title != other.title) {
            return false
        }

        if (width != other.width) {
            return false
        }

        if (this.isScrambled != other.isScrambled) {
            return false
        }

        if (solutionChecksum != other.solutionChecksum) {
            return false
        }

        if (clueNotes != other.clueNotes) {
            return false
        }

        if (playerNote != other.playerNote)
            return false

        if (images != other.images)
            return false

        if (sourceUrl != other.sourceUrl)
            return false

        if (supportUrl != other.supportUrl)
            return false

        if (shareUrl != other.shareUrl)
            return false

        if (pinnedClueID != other.pinnedClueID)
            return false

        if (kind != other.kind)
            return false

        if (rating != other.rating)
            return false

        return true
    }

    override fun hashCode() : Int {
        val prime = 31
        var result = 1
        result = (prime * result) + clueLists.hashCode()
        result = (prime * result) + (if (author == null) 0 else author.hashCode())
        result = (prime * result) + boxes.contentHashCode()
        result = (prime * result) +
                (if (copyright == null) 0 else copyright.hashCode())
        result = (prime * result) + height
        result = (prime * result) + (if (notes == null) 0 else notes.hashCode())
        result = (prime * result) + (if (title == null) 0 else title.hashCode())
        result = (prime * result) + width
        result = (prime * result) + clueNotes.hashCode()
        result = (prime * result) + Objects.hashCode(playerNote)
        result = (prime * result) + Objects.hashCode(images)
        result = (prime * result) + Objects.hash(
            sourceUrl, supportUrl, shareUrl, introMessage, completionMessage,
            pinnedClueID, kind
        )

        return result
    }

    /**
     * Find the first non-block box position
     */
    private fun getFirstPosition() : Position {
        for (row in 0..<height) {
            for (col in 0..<width) {
                if (!isBlock(checkedGetBox(row, col))) {
                    return Position(row, col)
                }
            }
        }
        throw IllegalArgumentException(
            "Can't handled grids with no cells"
        )
    }

    /**
     * Marks clue on current boxes
     *
     * E.g. set box as start of the clue or part of the clue.
     *
     * Only supports across/down clues
     */
    private fun addClueToBoxes(clue : Clue?) {
        val zone = clue?.zone
        if (zone == null || zone.isEmpty)
            return

        val number = clue.clueNumber

        for (offset in 0..<zone.size()) {
            val pos = zone.getPosition(offset)
            val box = checkedGetBox(pos)
            require(!isBlock(box)) {
                ("Clue " + number + " " + clue.clueID.listName
                        + " zone has a block box at position " + pos)
            }

            // prefer earlier positions if multiple
            if (!box!!.hasCluePosition(clue.clueID))
                box.setCluePosition(clue.clueID, offset)
        }
    }

    companion object {
        private val LOG : Logger
            = Logger.getLogger("app.crossword.yourealwaysbe")
    }
}

