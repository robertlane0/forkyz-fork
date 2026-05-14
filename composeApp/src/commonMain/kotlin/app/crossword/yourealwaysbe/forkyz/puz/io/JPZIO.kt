
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlin.math.max
import kotlinx.datetime.LocalDate
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.Clue
import app.crossword.yourealwaysbe.forkyz.puz.ClueID
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleKind
import app.crossword.yourealwaysbe.forkyz.puz.Zone
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.HtmlUtil.parseHtmlColor
import app.crossword.yourealwaysbe.forkyz.util.HtmlUtil.unHtmlString
import app.crossword.yourealwaysbe.forkyz.util.XMLElement
import app.crossword.yourealwaysbe.forkyz.util.parseXMLElement

/**
 * Converts a puzzle from the JPZ Crossword Compiler XML format.
 *
 * This is not necessarily a complete implementation, but works for the
 * sources tested.
 *
 * Acrostic format is an extension from Alex Boisvert
 *
 * The (supported) XML format is:
 *
 * <crossword-compiler>
 *   <rectangular-puzzle>
 *     <metadata>
 *       <title>[Title]</title>
 *       <creator>[Author]</creator>
 *       <copyright>[Copyright]</copyright>
 *       <description>[Description]</description>
 *     </metadata>
 *     <crossword> or <acrostic>
 *       <grid width="[width]" height="[height]">
 *         <cell x="[x]" y="[y]" solution="[letter]" ?number="[number]"/>
 *         <cell x="[x]" y="[y]" type="block" .../>
 *         ...
 *       </grid>
 *       <clues ordering="normal">
 *         <title><b>Across [or] Down</b></title>
 *         <clue number="[number]" format="[length]" citation="[explanation]">
 *           [clue]
 *         </clue>
 *         <clue number="[number]" is-link="[ordering num]">
 *           [clue]
 *         </clue>
 *       </clues>
 *     </crossword> or </acrostic>
 *   </rectangular-puzzle>
 * </crossword-compiler>
 *
 * Other cell attributes include background-color, background-shape,
 * solve-state, top|left|right|bottom-bar, top-right-number. Also
 * assuming top-left-number, top-number, left-number,
 * bottom-right-number &c. are valid (no
 * examples found in wild).
 *
 * Does not unzip the JPZ, use StreamUnits.unzipOrPassThrough if needed.
 */
class JPZIO : PuzzleParser {
    class JPZIOException(msg : String) : Exception(msg)

    private data class ClueInfo(
        val listName : String,
        val index : Int,
        val clueNumber : String?,
        val hint : String,
        val zoneID : String?,
        val citation : String?,
    )

    /**
     * Extract puzzle info from content
     */
    private class JPZXMLParser(content : XMLElement) {
        var title : String = ""
            private set
        var creator : String = ""
            private set
        var copyright : String = ""
            private set
        var description : String = ""
            private set
        var completion : String? = null
            private set
        var instructions : String? = null
            private set
        var width : Int = 0
            private set
        var height : Int = 0
            private set
        var boxes : Array<Array<Box?>>? = null
            private set
        val clues : MutableList<ClueInfo> = mutableListOf()
        val zoneMap : MutableMap<String, Zone> = mutableMapOf()
        private val charBuffer = StringBuilder()

        val isAcrostic = content.selectFirst("acrostic") != null
        // sanity checks
        private val hasRectangularPuzzleEle
            = content.selectFirst("rectangular-puzzle") != null
        private val hasGridEle = content.selectFirst("grid") != null
        private val hasCluesEle = content.selectFirst("clues") != null

        val isSuccessfulRead : Boolean
            /**
             * Best assessment of whether read succeeded (i.e. was a JPZ
             * file)
             */
            get() = hasRectangularPuzzleEle
                    && hasGridEle
                    && hasCluesEle
                    && this.width > 0
                    && this.height > 0
                    && (this.clues.size > 0)

        init {
            getMetaData(content)
            getGrid(content)
            getClues(content)
            getWords(content)
        }

        private fun getMetaData(content : XMLElement) {
            content.selectFirst("metadata")?.let { metadata ->
                metadata.selectFirst("title")?.let { title = it.html() }
                metadata.selectFirst("creator")?.let { creator = it.html() }
                metadata.selectFirst("copyright")?.let { copyright = it.html() }
                metadata.selectFirst("description")?.let {
                    description = it.html()
                }
                metadata.selectFirst("instructions")?.let {
                    instructions = it.html()
                }
            }
            content.selectFirst("completion")?.let {
                completion = it.html()
            }
        }

        private fun getGrid(content : XMLElement) {
            content.selectFirst("grid")?.let { grid ->
                 try {
                    width = grid.attr("width").toInt()
                    height = grid.attr("height").toInt()
                    boxes = Array<Array<Box?>>(height) {
                        arrayOfNulls<Box>(width)
                    }

                    for (cell in grid.select("cell")) {
                        parseCell(cell)
                    }
                } catch (e : NumberFormatException) {
                    AppLogger.e(TAG, "Could not read JPZ XML cell data: " + e)
                }
            }
        }

        private fun parseCell(cell : XMLElement) {
            parseCellAttributes(cell)?.let { pos ->
                for (arrow in cell.select("arrow")) {
                    parseArrow(pos, arrow)
                }
            }
        }

        private fun parseCellAttributes(cell : XMLElement) : Position? {
            val x = cell.attr("x").toInt() - 1
            val y = cell.attr("y").toInt() - 1
            val pos = Position(y, x)

            if (0 <= x && x < width && 0 <= y && y < height) {
                val box = Box()

                // keep track of whether the cell is interesting
                // uninteresting blocks get set to null
                var hasData = false
                if (!isCell(cell))
                    box.isBlock = true

                val solution = cell.attr("solution")
                if (!solution.isEmpty() && !isBlock(box)) {
                    box.solution = solution
                }

                val response = cell.attr("solve-state")
                if (!response.isEmpty()) {
                    box.initialValue = response
                    box.response = response
                    hasData = true
                } else {
                    box.setBlank()
                }

                val number = cell.attr("number")
                if (!number.isEmpty()) {
                    box.clueNumber = number
                    hasData = true
                }

                val shape = cell.attr("background-shape")
                if ("circle".equals(shape, ignoreCase = true)) {
                    box.shape = Box.Shape.CIRCLE
                    hasData = true
                } else {
                    // see if it's an IPuz value why not
                    val boxShape = IPuzIO.getShape(shape)
                    if (boxShape != null) {
                        box.shape = boxShape
                        hasData = true
                    }
                }

                val color = cell.attr("background-color")
                if (!color.isEmpty()) {
                    val iColor = parseHtmlColor(color)
                    if (iColor >= 0) {
                        box.color = iColor
                        hasData = true
                    }
                }

                val topBar = cell.attr("top-bar")
                if ("true".equals(topBar, ignoreCase = true))
                    box.barTop = Box.Bar.SOLID
                val bottomBar = cell.attr("bottom-bar")
                if ("true".equals(bottomBar, ignoreCase = true))
                    box.barBottom = Box.Bar.SOLID
                val leftBar = cell.attr("left-bar")
                if ("true".equals(leftBar, ignoreCase = true))
                    box.barLeft = Box.Bar.SOLID
                val rightBar = cell.attr("right-bar")
                if ("true".equals(rightBar, ignoreCase = true))
                    box.barRight = Box.Bar.SOLID
                hasData = hasData or box.hasBars()

                var hasMarks = false
                val marks = Array<Array<String?>>(3) {
                    arrayOfNulls<String>(3)
                }
                for (row in 0..2) {
                    for (col in 0..2) {
                        val mark = cell.attr(markAttributes[row][col])
                        if (!mark.isEmpty()) {
                            marks[row][col] = mark
                            hasMarks = true
                        }
                    }
                }
                if (hasMarks) {
                    box.setMarks(marks)
                    hasData = true
                }

                if (!box.isBlock || hasData)
                    boxes?.let { it[y][x] = box }

                return pos
            } else {
                return null
            }
        }

        /**
         * Checks if cell is a block or not
         *
         * Defaults true unless evidence to the contrary
         */
        private fun isCell(cell : XMLElement) : Boolean {
            val cellType = cell.attr("type")
            if ("block".equals(cellType, ignoreCase = true))
                return false
            if ("void".equals(cellType, ignoreCase = true))
                return false
            if ("clue".equals(cellType, ignoreCase = true))
                return false
            return true
        }

        private fun parseArrow(pos : Position, arrow : XMLElement) {
            // only interpret "to" field which gives arrow direction
            // i don't quite understand the "from" field
            var to = arrow.attr("to")
            if (to.isEmpty())
                return

            to = to.trim()

            var arrowShape : Box.Shape? = null
            if (to.equals("left", ignoreCase = true))
                arrowShape = Box.Shape.ARROW_LEFT
            else if (to.equals("right", ignoreCase = true))
                arrowShape = Box.Shape.ARROW_RIGHT
            else if (to.equals("top", ignoreCase = true))
                arrowShape = Box.Shape.ARROW_UP
            else if (to.equals("bottom", ignoreCase = true))
                arrowShape = Box.Shape.ARROW_DOWN

            if (arrowShape == null)
                return

            val row = pos.row
            val col = pos.col
            boxes?.let { boxes ->
                if (boxes[row][col] == null) {
                    boxes[row][col] = Box().apply{
                        isBlock = true
                    }
                }

                boxes[row][col]?.let { it.shape = arrowShape }
            }
        }

        private fun getClues(content : XMLElement) {
            for (clues in content.select("clues")) {
                parseClueList(clues)
            }
        }

        private fun parseClueList(clues : XMLElement) {
            clues.selectFirst("title")?.text()?.let { listName ->
                var clueIndex = 0
                for (clue in clues.select("clue")) {
                    parseClue(listName, clueIndex, clue)
                    clueIndex += 1
                }
            }
        }

        private fun parseClue(
            listName : String,
            index : Int,
            clue : XMLElement,
        ) {
            val num = clue.attr("number")
            val hint = clue.html()
            val link = clue.attr("is-link")
            var format = ""
            var citation : String? = null
            var zoneID : String? = null
            if (link.isEmpty()) {
                format = clue.attr("format")
                citation = clue.attrNull("citation")
                zoneID = clue.attrNull("word")
            }
            val fullClue = if (format.isEmpty()) hint else "$hint ($format)"
            clues.add(
                ClueInfo(listName, index, num, fullClue, zoneID, citation),
            )
        }

        private fun getWords(content : XMLElement) {
            for (word in content.select("word")) {
                parseWord(word)
            }
        }

        private fun parseWord(word : XMLElement) {
            val zoneID = word.attr("id")
            if (zoneID.isEmpty())
                return

            val zone = Zone()

            val x = word.attrNull("x")
            val y = word.attrNull("y")
            if (x != null && y != null) {
                parseCells(zone, x, y)
            }

            for (cells in word.select("cells")) {
                val x = cells.attrNull("x")
                val y = cells.attrNull("y")
                if (x != null && y != null) {
                    parseCells(zone, x, y)
                }
            }

            zoneMap.put(zoneID, zone)
        }

        /**
         * Parse cells data into zone
         *
         * E.g. x="1-3" y = "2" is (2, 1), (2, 2), (2, 3);
         */
        private fun parseCells(zone : Zone, x : String, y : String) {
            val xs = x.split("-".toRegex())
            val xstart = xs[0].toInt() - 1
            val xend = if (xs.size > 1)
                xs[1].toInt() - 1
            else
                xstart

            val ys = y.split("-".toRegex())
            val ystart = ys[0].toInt() - 1
            val yend = if (ys.size > 1)
                ys[1].toInt() - 1
            else
                ystart

            for (row in ystart..yend) {
                for (col in xstart..xend) {
                    zone.addPosition(Position(row, col))
                }
            }
        }
    }

    @Throws(Exception::class)
    override fun parseInput(input : Source) : Puzzle? {
        return readPuzzle(input)
    }

    companion object {
        private val TAG = "ForkyzJPZIO"

        private const val ACROSTIC_QUOTE_LISTNAME = "Quote"
        private const val ACROSTIC_QUOTE_HINT = "Quote"

        private val markAttributes = arrayOf(
            arrayOf("top-left-number", "top-number", "top-right-number"),
            arrayOf("left-number", "center-number", "right-number"),
            arrayOf(
                "bottom-left-number",
                "bottom-number",
                "bottom-right-number",
            ),
        )

        @Throws(Exception::class)
        fun readPuzzle(input : Source) : Puzzle? {
            val content = parseXMLElement(input)
            val handler = JPZXMLParser(content)

            if (!handler.isSuccessfulRead)
                return null

            if (handler.isAcrostic || looksLikeAnAcrostic(handler)) {
                return buildAcrostic(handler) ?: buildCrossword(handler)
            } else {
                return buildCrossword(handler)
            }
        }

        /**
         * Build crossword from completed handler
         */
        private fun buildCrossword(handler : JPZXMLParser) : Puzzle? {
            val boxes = handler.boxes
            if (boxes == null)
                return null

            val builder = PuzzleBuilder(boxes)
            builder.setTitle(handler.title)
                .setAuthor(handler.creator)
                .setCopyright(handler.copyright)
                .setIntroMessage(handler.instructions)
                .setNotes(handler.description)

            setClues(builder, handler)
            setCompletionMessage(builder, handler)

            return builder.puzzle
        }

        private fun setClues(builder : PuzzleBuilder, handler : JPZXMLParser) {
            val zones = handler.zoneMap
            val clues = handler.clues
            for (i in clues.indices) {
                val clue = clues.get(i)
                builder.addClue(
                    Clue(
                        clue.listName,
                        clue.index,
                        clue.clueNumber,
                        clue.hint,
                        zones.get(clue.zoneID)
                    ),
                )
            }
        }

        private fun setCompletionMessage(
            builder : PuzzleBuilder,
            handler : JPZXMLParser,
        ) {
            val fullCompletion = StringBuilder()

            val completion = handler.completion
            if (completion != null) {
                fullCompletion.append(completion)
                fullCompletion.append("<br/>")
            }

            // sort lists into order then construct citations text
            val listNotes = mutableMapOf<String, StringBuilder>()

            for (ci in handler.clues) {
                val number = ci.clueNumber
                val listName = ci.listName
                val citation = ci.citation

                if (citation != null) {
                    if (!listNotes.containsKey(listName))
                        listNotes.put(listName, StringBuilder())

                    listNotes.get(listName)?.append("<p>$number: $citation</p>")
                }
            }

            val listNames = listNotes.keys.sorted().toList()
            for (listName in listNames) {
                listNotes.get(listName)?.let { notes ->
                    fullCompletion.append("<h1>" + listName + "</h1>")
                    fullCompletion.append(notes.toString())
                }
            }

            builder.setCompletionMessage(fullCompletion.toString())
        }

        /**
         * Build acrostic from completed handler
         *
         * Should work when handler.isAcrostic() is true. Will try anyway.
         * Returns null if it's not possible to read it as an acrostic.
         */
        private fun buildAcrostic(handler : JPZXMLParser) : Puzzle? {
            val boxes : Array<Array<Box?>>? = getAcrosticBoxes(handler)
            if (boxes == null)
                return null

            val builder = PuzzleBuilder(boxes)
            builder.setTitle(handler.title)
                .setAuthor(handler.creator)
                .setCopyright(handler.copyright)
                .setIntroMessage(handler.instructions)
                .setNotes(handler.description)
                .setKind(PuzzleKind.ACROSTIC)

            try {
                setAcrosticClues(builder, handler)
            } catch (e : JPZIOException) {
                return null
            }

            setCompletionMessage(builder, handler)

            return builder.puzzle
        }

        /**
         * Find the part of an acrostic grid corresponding to the quote
         *
         * Returns null if not derivable
         */
        private fun getAcrosticBoxes(
            handler : JPZXMLParser,
        ) : Array<Array<Box?>>? {
            val lastRow = getAcrosticLastQuoteRow(handler)
            if (lastRow < 0)
                return null

            val allBoxes = handler.boxes
            if (allBoxes == null)
                return null

            val boxes = Array<Array<Box?>>(lastRow + 1) {
                arrayOfNulls<Box>(handler.width)
            }
            for (row in 0..lastRow) {
                for (col in 0..<handler.width) {
                    boxes[row][col] = allBoxes[row][col]
                }
            }

            return boxes
        }

        /**
         * Guess if JPZ is an acrostic
         *
         * If exported from crossword nexus, the <acrostic> tag is changed to the
         * standard crossword one. So look for other evidence this is actually an
         * acrostic.
         */
        private fun looksLikeAnAcrostic(handler : JPZXMLParser) : Boolean {
            return getAcrosticQuoteZone(handler) != null
        }

        /**
         * JPZ acrostics have quote boxes on top and separate answer boxes
         */
        private fun getAcrosticLastQuoteRow(handler : JPZXMLParser) : Int {
            val quoteZone : Zone? = getAcrosticQuoteZone(handler)
            if (quoteZone == null)
                return -1

            var lastRow = -1
            for (pos in quoteZone) {
                if (lastRow < 0)
                    lastRow = pos.row
                else
                    lastRow = max(lastRow, pos.row)
            }

            return lastRow
        }

        private fun getAcrosticQuoteZone(handler : JPZXMLParser) : Zone? {
            var quoteZone : Zone? = null
            val clues = handler.clues
            val zones = handler.zoneMap

            // look for quote clue, start from end as it's usually last
            val li = clues.listIterator(clues.size)
            while (li.hasPrevious()) {
                val clue = li.previous()
                if (isAcrosticQuoteClue(clue)) {
                    quoteZone = zones.get(clue.zoneID)
                    break
                }
            }

            return quoteZone
        }

        private fun isAcrosticQuoteClue(clue : ClueInfo) : Boolean {
            val number = clue.clueNumber
            val hint = clue.hint
            return (
                (number == null || number.isEmpty())
                && "[QUOTE]".equals(hint, ignoreCase = true)
            )
        }

        /**
         * Add the clues to a builder for an acrostic
         *
         * builder must have the quote boxes (but then you can't get a
         * builder without giving the boxes)
         */
        @Throws(JPZIOException::class)
        private fun setAcrosticClues(
            builder : PuzzleBuilder,
            handler : JPZXMLParser,
        ) {
            // the clue zones refer to "non-quote" cells, that map to quote
            // cells via the box number.

            val zones = handler.zoneMap
            val allBoxes = handler.boxes
            val numberPositions = builder.getNumberPositions()
            val clues = handler.clues

            if (allBoxes == null) {
                throw JPZIOException("Puzzle has no boxes")
            }

            for (i in clues.indices) {
                val clue = clues.get(i)
                if (!isAcrosticQuoteClue(clue)) {
                    val indirectZone = zones.get(clue.zoneID) ?: Zone()
                    val directZone = Zone()
                    for (pos in indirectZone) {
                        val row = pos.row
                        val col = pos.col
                        if (row < 0 || row >= allBoxes.size) {
                            throw JPZIOException(
                                "Clue postion " + pos + " is outside of boxes.",
                            )
                        }

                        if (col < 0 || col >= allBoxes[row].size) {
                            throw JPZIOException(
                                "Clue postion " + pos + " is outside of boxes.",
                            )
                        }

                        val box = allBoxes[row][col]
                        if (box == null || isBlock(box)) {
                            throw JPZIOException(
                                "Clue contains position "
                                    + pos
                                    + " which is a block",
                            )
                        }

                        if (!box.hasClueNumber()) {
                            throw JPZIOException(
                                "Acrostic clue contains position "
                                    + pos
                                    + " which is a box not linked to the quote"
                                    + " via its clue number.",
                            )
                        }

                        val truePos = numberPositions.get(box.clueNumber)
                        if (truePos == null) {
                            throw JPZIOException(
                                "Acrostic clue contains position "
                                    + pos
                                    + " with number "
                                    + box.clueNumber
                                    + " that is not a position in the quote.",
                            )
                        }

                        directZone.addPosition(truePos)
                    }

                    val listName = clue.listName
                    val index = builder.getNextClueIndex(listName)
                    builder.addClue(
                        Clue(
                            listName,
                            index,
                            null,  // use label instead of clue number
                            clue.clueNumber,
                            clue.hint,
                            directZone,
                        ),
                    )
                } else {
                    val index
                        = builder.getNextClueIndex(ACROSTIC_QUOTE_LISTNAME)
                    builder.addClue(
                        Clue(
                            ACROSTIC_QUOTE_LISTNAME,
                            index,
                            null,
                            null,
                            ACROSTIC_QUOTE_HINT,
                            zones.get(clue.zoneID) ?: Zone(),
                        )
                    )
                }
            }
        }
    }
}

