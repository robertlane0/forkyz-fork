
package app.crossword.yourealwaysbe.forkyz.puz

import kotlin.io.encoding.Base64
import kotlinx.datetime.LocalDate
import kotlinx.io.IOException

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.request
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headers

import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.util.PuzzleUtils
import app.crossword.yourealwaysbe.forkyz.util.resolveUrl
import app.crossword.yourealwaysbe.forkyz.util.StreamUtils

class PuzzleBuilder {
    /**
     * Provides access to puzzle methods directly
     */
    val puzzle : Puzzle
    private var numberPositions : MutableMap<String, Position>? = null

    /**
     * Convenience class for partial clues
     */
    class BasicClue(val num : String, val hint : String)

    constructor(boxes : Array<Array<Box?>>) {
        puzzle = Puzzle(boxes)
    }

    constructor(boxesList : Array<Box?>, width : Int, height : Int) {
        val boxes = Array<Array<Box?>>(height) { row ->
            Array<Box?>(width) { col ->
                boxesList[row * width + col]
            }
        }
        puzzle = Puzzle(boxes)
    }

    val width : Int
        get() = puzzle.width
    val height : Int
        get() = puzzle.height
    val boardClueIDs : Iterable<ClueID>
        get() = puzzle.boardClueIDs

    fun getBox(row : Int, col : Int) : Box? {
        return puzzle.checkedGetBox(row, col)
    }

    fun getBox(position : Position?) : Box? {
        return puzzle.checkedGetBox(position)
    }

    fun setBoxClueNumber(
        row : Int,
        col : Int,
        number : String?,
    ) : PuzzleBuilder {
        puzzle.checkedGetBox(row, col)?.let { box ->
            if (!isBlock(box)) {
                box.clueNumber = number
                invalidateNumberPosition()
            }
        }
        return this
    }

    /**
     * Add a clue to the puzzle
     *
     * Will check the zone matches the boxes and delete any positions
     * in the zone that it finds are not valid (e.g. blocks can't be
     * part of clue positions.
     */
    fun addClue(clue : Clue) : PuzzleBuilder {
        val cleanClue = sanitizeClueZone(clue)
        puzzle.addClue(cleanClue)
        return this
    }

    /**
     * Removed bad positions from clue if needed
     *
     * Returns original clue object if all good, else a clone with bad
     * zone positions removed.
     */
    private fun sanitizeClueZone(clue : Clue) : Clue {
        val badPositions : MutableSet<Position> = mutableSetOf<Position>()
        for (pos in clue.zone) {
            if (isBlock(getBox(pos)))
                badPositions.add(pos)
        }

        if (badPositions.isEmpty()) {
            return clue
        } else {
            val newZone = Zone()
            for (pos in clue.zone) {
                if (!badPositions.contains(pos))
                    newZone.addPosition(pos)
            }
            return Clue(
                clue.clueID.listName,
                clue.clueID.index,
                clue.clueNumber,
                clue.label,
                clue.hint,
                newZone,
            )
        }
    }

    fun setSolutionChecksum(checkSum : Short) : PuzzleBuilder {
        puzzle.solutionChecksum = checkSum
        return this
    }

    fun setScrambled(scrambled : Boolean) : PuzzleBuilder {
        puzzle.isScrambled = scrambled
        return this
    }

    fun setTitle(title : String?) : PuzzleBuilder {
        puzzle.title = title
        return this
    }

    fun setAuthor(author : String?) : PuzzleBuilder {
        puzzle.author = author
        return this
    }

    fun setCopyright(copyright : String?) : PuzzleBuilder {
        puzzle.copyright = copyright
        return this
    }

    fun setNotes(notes : String?) : PuzzleBuilder {
        puzzle.notes = notes
        return this
    }

    fun setIntroMessage(introMessage : String?) : PuzzleBuilder {
        puzzle.introMessage = introMessage
        return this
    }

    fun setCompletionMessage(completionMessage : String?) : PuzzleBuilder {
        puzzle.completionMessage = completionMessage
        return this
    }

    fun setRating(rating : Char) : PuzzleBuilder {
        puzzle.rating = rating
        return this
    }

    fun setPlayerNote(playerNote : Note?) : PuzzleBuilder {
        puzzle.playerNote = playerNote
        return this
    }

    fun setNote(cid : ClueID?, note : Note?) : PuzzleBuilder {
        puzzle.setNote(cid, note)
        return this
    }

    fun setDate(date : LocalDate?) : PuzzleBuilder {
        puzzle.date = date
        return this
    }

    fun setSource(source : String?) : PuzzleBuilder {
        puzzle.source = source
        return this
    }

    fun setSourceUrl(sourceUrl : String?) : PuzzleBuilder {
        puzzle.sourceUrl = sourceUrl
        return this
    }

    fun setSupportUrl(supportUrl : String?) : PuzzleBuilder {
        puzzle.supportUrl = supportUrl
        return this
    }

    fun setShareUrl(shareUrl : String?) : PuzzleBuilder {
        puzzle.shareUrl = shareUrl
        return this
    }

    fun setTime(time : Long) : PuzzleBuilder {
        puzzle.time = time
        return this
    }

    fun flagClue(clueId : ClueID?, flag : Boolean) : PuzzleBuilder {
        flagClue(clueId, flag, Clue.DEFAULT_FLAG_COLOR)
        return this
    }

    fun flagClue(
        clueId : ClueID?,
        flag : Boolean,
        flagColor : Int,
    ) : PuzzleBuilder {
        val clue = if (clueId == null) null else puzzle.getClue(clueId)
        if (clue != null) {
            clue.isFlagged = flag
            clue.flagColor = flagColor
        }
        return this
    }

    fun setPosition(position : Position) : PuzzleBuilder {
        puzzle.position = position
        return this
    }

    fun setCurrentClueID(clueId : ClueID?) : PuzzleBuilder {
        puzzle.currentClueID = clueId
        return this
    }

    fun setHistory(history : MutableList<ClueID>) : PuzzleBuilder {
        puzzle.history = history
        return this
    }

    fun addImage(image : PuzImage) : PuzzleBuilder {
        puzzle.addImage(image)
        return this
    }

    fun setPinnedClueID(pinnedClueID : ClueID?) : PuzzleBuilder {
        puzzle.pinnedClueID = pinnedClueID
        return this
    }

    fun setKind(kind : PuzzleKind?) : PuzzleBuilder {
        puzzle.kind = kind
        return this
    }

    /**
     * Number puzzle boxes according to standard system
     *
     * @param skipNumberCells cells not to auto number. They are skipped
     * and the number goes to the next standard cell to number.
     * @throws IllegalArgumentException if mismatch with existing
     * numbers
     */
    @Throws(IllegalArgumentException::class)
    fun autoNumberBoxes(
        skipNumberCells : Set<Position> = setOf<Position>(),
    ) : PuzzleBuilder {
        var clueCount = 1

        for (row in 0..<puzzle.height) {
            for (col in 0..<puzzle.width) {
                val pos = Position(row, col)
                val box = puzzle.checkedGetBox(pos)

                if (box == null || isBlock(box)) {
                    continue
                }

                val isStart = (
                    isStartClue(row, col, true)
                    || isStartClue(row, col, false)
                )

                val boxNumber = box.clueNumber
                val autoNumber = clueCount.toString()

                if (isStart && !skipNumberCells.contains(pos)) {
                    require(!(boxNumber != null && boxNumber != autoNumber)) {
                        (
                            "Box clue number " + boxNumber
                            + " does not match expected "
                            + clueCount
                        )
                    }

                    box.clueNumber = autoNumber
                    clueCount++
                } else {
                    require(boxNumber == null) {
                        (
                            "Box at row " + row
                            + " and col " + col
                            + " numbered " + boxNumber
                            + " expected not to be numbered"
                        )
                    }
                }
            }
        }

        return this
    }

    /**
     * The next contiguous index for a clue in listName
     */
    fun getNextClueIndex(listName : String?) : Int {
        return puzzle.getClues(listName)?.size() ?: 0
    }

    /**
     * Get standard across zone for clue with number
     *
     * Returns null if number not found
     */
    fun getAcrossZone(number : String?) : Zone? {
        val start = getNumberPositions().get(number)
        return getAcrossZone(start)
    }

    /**
     * Get standard across zone from a position
     */
    fun getAcrossZone(start : Position?) : Zone? {
        return if (start != null)
            PuzzleUtils.getAcrossZone(puzzle, start)
        else
            null
    }

    /**
     * Get standard down zone for clue with number
     *
     * Returns null if number not found
     */
    fun getDownZone(number : String?) : Zone? {
        val start = getNumberPositions().get(number)
        return getDownZone(start)
    }

    /**
     * Get standard down zone from a position
     */
    fun getDownZone(start : Position?) : Zone? {
        return if (start != null)
            PuzzleUtils.getDownZone(puzzle, start)
        else
            null
    }

    fun addAcrossClue(
        listName : String,
        basicClue : BasicClue,
    ) : PuzzleBuilder {
        return addAcrossClue(
            listName,
            basicClue.num,
            basicClue.hint,
        )
    }

    fun addAcrossClue(
        listName : String,
        number : String?,
        hint : String,
    ) : PuzzleBuilder {
        return addAcrossClue(listName, number, null, hint)
    }

    fun addAcrossClue(
        listName : String,
        number : String?,
        label : String?,
        hint : String,
    ) : PuzzleBuilder {
        val index = getNextClueIndex(listName)
        val zone = getAcrossZone(number) ?: Zone()

        puzzle.addClue(Clue(listName, index, number, label, hint, zone))

        return this
    }

    fun addDownClue(listName : String, basicClue : BasicClue) : PuzzleBuilder {
        return addDownClue(
            listName,
            basicClue.num,
            basicClue.hint,
        )
    }

    fun addDownClue(
        listName : String,
        number : String?,
        hint : String,
    ) : PuzzleBuilder {
        return addDownClue(listName, number, null, hint)
    }

    fun addDownClue(
        listName : String,
        number : String?,
        label : String?,
        hint : String,
    ) : PuzzleBuilder {
        val index = getNextClueIndex(listName)
        val zone = getDownZone(number) ?: Zone()

        puzzle.addClue(Clue(listName, index, number, label, hint, zone))

        return this
    }

    /**
     * True if box is start of clue in standard numbering system
     *
     * Regardless of whether there is actually a clue in the puzzle
     */
    fun isStartClue(row : Int, col : Int, across : Boolean) : Boolean {
        if (across) {
            return !joinedLeft(row, col) && joinedRight(row, col)
        } else {
            return !joinedTop(row, col) && joinedBottom(row, col)
        }
    }

    /**
     * Replace URLs of images with base64 data
     *
     * Downloads images, relative to relativeUrl (i.e. image in file at
     * relativeUrl).
     */
    suspend fun resolveImages(relativeUrl : String) {
        resolveImages(this.puzzle, relativeUrl)
    }

    fun getNumberPositions() : Map<String, Position> {
        val numberPositions = this.numberPositions
        return if (numberPositions != null) {
            numberPositions
        } else {
            val positions = mutableMapOf<String, Position>()
            val boxes : Array<Array<Box?>> = puzzle.boxes

            for (row in 0..<puzzle.height) {
                for (col in 0..<puzzle.width) {
                    boxes[row][col]?.let { box ->
                        if (!isBlock(box) && box.hasClueNumber()) {
                            box.clueNumber?.let { number ->
                                positions.put(number, Position(row, col))
                            }
                        }
                    }
                }
            }

            this.numberPositions = positions

            positions
        }
    }

    /**
     * Empty the solution in the puzzle
     */
    fun removeSolution() : PuzzleBuilder {
        val boxes : Array<Array<Box?>> = puzzle.boxes

        for (row in 0..<puzzle.height) {
            for (col in 0..<puzzle.width) {
                boxes[row][col]?.let { box ->
                    if (!isBlock(box))
                        box.solution = null
                }
            }
        }

        puzzle.solutionChecksum = 0.toShort()

        return this
    }

    private fun invalidateNumberPosition() {
        numberPositions = null
    }

    private fun joinedTop(row : Int, col : Int) : Boolean {
        return PuzzleUtils.joinedTop(puzzle, row, col)
    }

    private fun joinedBottom(row : Int, col : Int) : Boolean {
        return PuzzleUtils.joinedBottom(puzzle, row, col)
    }

    private fun joinedLeft(row : Int, col : Int) : Boolean {
        return PuzzleUtils.joinedLeft(puzzle, row, col)
    }

    private fun joinedRight(row : Int, col : Int) : Boolean {
        return PuzzleUtils.joinedRight(puzzle, row, col)
    }

    companion object {
        private val TIMEOUT = 30000L

        suspend fun resolveImages(puz : Puzzle, relativeUrl : String) {
            val client = HttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = TIMEOUT
                    connectTimeoutMillis = TIMEOUT
                }
            }

            try {
                for (image in puz.images) {
                    val imgUrl = image.url
                    if (
                        imgUrl.length >= 6
                        && imgUrl.substring(0, 5).equals("data:", ignoreCase = true)
                    ) {
                        continue
                    }

                    try {
                        val imgFullUrl = resolveUrl(relativeUrl, imgUrl)
                        val response = client.request(imgFullUrl)
                        val bytes = response.readRawBytes()
                        response.headers[HttpHeaders.ContentType]?.let { type ->
                            image.url = (
                                "data:"
                                + type
                                + ";base64,"
                                + Base64.Default.encode(bytes)
                            )
                        }
                    } catch (e : IOException) {
                        // ignore
                    } catch (e : IllegalArgumentException) {
                        // ignore
                    }
                }
            } finally {
                client.close()
            }
        }
    }
}

