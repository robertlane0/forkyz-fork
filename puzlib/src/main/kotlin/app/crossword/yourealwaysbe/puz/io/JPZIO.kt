package app.crossword.yourealwaysbe.puz.io

import java.io.DataOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.util.Collections
import java.util.LinkedList
import java.util.logging.Logger
import javax.xml.parsers.SAXParserFactory
import kotlin.math.max

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.PuzzleKind
import app.crossword.yourealwaysbe.puz.Zone
import app.crossword.yourealwaysbe.puz.util.HtmlUtil.parseHtmlColor
import app.crossword.yourealwaysbe.puz.util.HtmlUtil.unHtmlString

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

    private class ClueInfo(
        listName : String,
        index : Int,
        val clueNumber : String?,
        val hint : String,
        val zoneID : String?,
        val citation : String?,
    ) {
        private val cid = ClueID(listName, index)
        val listName : String
            get() = cid.listName
        val index : Int
            get() = cid.index
    }

    private class JPZXMLParser : DefaultHandler() {
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
        var isAcrostic : Boolean = false
            private set
        val clues : MutableList<ClueInfo> = mutableListOf()
        val zoneMap : MutableMap<String, Zone> = mutableMapOf()
        private val charBuffer = StringBuilder()

        // sanity checks
        private var hasRectangularPuzzleEle = false
        private var hasGridEle = false
        private var hasCluesEle = false

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

        // Use several handlers to maintain three different modes:
        // outerXML, inGrid, and inClues
        private val outerXML : DefaultHandler = object : DefaultHandler() {
            @Throws(SAXException::class)
            override fun startElement(
                nsURI : String?,
                strippedName : String,
                tagName : String,
                attributes : Attributes?,
            ) {
                var strippedName = strippedName
                strippedName = strippedName.trim()
                val name = if (strippedName.length == 0)
                    tagName.trim()
                else
                    strippedName

                if (
                    name.equals("title", ignoreCase = true)
                    || name.equals("creator", ignoreCase = true)
                    || name.equals("copyright", ignoreCase = true)
                    || name.equals("description", ignoreCase = true)
                    || name.equals("instructions", ignoreCase = true)
                    || name.equals("completion", ignoreCase = true)
                ) {
                    charBuffer.delete(0, charBuffer.length)
                } else {
                    charBuffer.append("<" + tagName + ">")
                }
            }

            @Throws(SAXException::class)
            override fun characters(ch : CharArray, start : Int, length : Int) {
                charBuffer.append(ch, start, length)
            }

            @Throws(SAXException::class)
            override fun endElement(
                nsURI : String?,
                strippedName : String,
                tagName : String,
            ) {
                var strippedName = strippedName
                strippedName = strippedName.trim()
                val name = if (strippedName.length == 0)
                    tagName.trim()
                else
                    strippedName

                val charData = charBuffer.toString().trim()

                if (name.equals("title", ignoreCase = true)) {
                    title = charData
                } else if (name.equals("creator", ignoreCase = true)) {
                    creator = charData
                } else if (name.equals("copyright", ignoreCase = true)) {
                    copyright = charData
                } else if (name.equals("description", ignoreCase = true)) {
                    description = charData
                } else if (name.equals("instructions", ignoreCase = true)) {
                    instructions = charData
                } else if (name.equals("completion", ignoreCase = true)) {
                    completion = charData
                } else {
                    charBuffer.append("</" + tagName + ">")
                }
            }
        }

        private val inGrid : DefaultHandler = object : DefaultHandler() {
            private var inCellPosition : Position? = null

            @Throws(SAXException::class)
            override fun startElement(
                nsURI : String?,
                strippedName : String,
                tagName : String,
                attributes : Attributes,
            ) {
                var strippedName = strippedName
                strippedName = strippedName.trim()
                val name = if (strippedName.length == 0)
                    tagName.trim()
                else
                    strippedName

                try {
                    if (name.equals("grid", ignoreCase = true)) {
                        this@JPZXMLParser.width = attributes
                            .getValue("width")
                            .toInt()
                        this@JPZXMLParser.height = attributes
                            .getValue("height")
                            .toInt()
                        this@JPZXMLParser.boxes = Array<Array<Box?>>(height) {
                            arrayOfNulls<Box>(width)
                        }
                    } else if (name.equals("cell", ignoreCase = true)) {
                        parseCell(attributes)
                    } else if (name.equals("arrow", ignoreCase = true)) {
                        parseArrow(attributes)
                    }
                } catch (e : NumberFormatException) {
                    LOG.severe("Could not read JPZ XML cell data: " + e)
                }
            }

            @Throws(SAXException::class)
            override fun endElement(
                nsURI : String?,
                strippedName : String,
                tagName : String,
            ) {
                var strippedName = strippedName
                strippedName = strippedName.trim()
                val name = if (strippedName.length == 0)
                    tagName.trim()
                else
                    strippedName

                if (name.equals("cell", ignoreCase = true)) {
                    inCellPosition = null
                }
            }

            fun parseCell(attributes : Attributes) {
                val x = attributes.getValue("x").toInt() - 1
                val y = attributes.getValue("y").toInt() - 1

                if (
                    0 <= x
                    && x < this@JPZXMLParser.width
                    && 0 <= y
                    && y < this@JPZXMLParser.height
                ) {
                    inCellPosition = Position(y, x)
                    val box = Box()

                    // keep track of whether the cell is interesting
                    // uninteresting blocks get set to null
                    var hasData = false
                    if (!isCell(attributes))
                        box.isBlock = true

                    val solution = attributes.getValue("solution")
                    if (
                        solution != null
                        && solution.length > 0
                        && !isBlock(box)
                    ) {
                        box.solution = solution
                    }

                    val response = attributes.getValue("solve-state")
                    if (response != null && response.length > 0) {
                        box.initialValue = response
                        box.response = response
                        hasData = true
                    } else {
                        box.setBlank()
                    }

                    val number = attributes.getValue("number")
                    if (number != null) {
                        box.clueNumber = number
                        hasData = true
                    }

                    val shape = attributes.getValue("background-shape")
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

                    val color = attributes.getValue("background-color")
                    if (color != null) {
                        val iColor = parseHtmlColor(color)
                        if (iColor >= 0) {
                            box.color = iColor
                            hasData = true
                        }
                    }

                    val topBar = attributes.getValue("top-bar")
                    if ("true".equals(topBar, ignoreCase = true))
                        box.barTop = Box.Bar.SOLID
                    val bottomBar = attributes.getValue("bottom-bar")
                    if ("true".equals(bottomBar, ignoreCase = true))
                        box.barBottom = Box.Bar.SOLID
                    val leftBar = attributes.getValue("left-bar")
                    if ("true".equals(leftBar, ignoreCase = true))
                        box.barLeft = Box.Bar.SOLID
                    val rightBar = attributes.getValue("right-bar")
                    if ("true".equals(rightBar, ignoreCase = true))
                        box.barRight = Box.Bar.SOLID
                    hasData = hasData or box.hasBars()

                    var hasMarks = false
                    val marks = Array<Array<String?>>(3) {
                        arrayOfNulls<String>(3)
                    }
                    for (row in 0..2) {
                        for (col in 0..2) {
                            val mark = attributes.getValue(
                                markAttributes[row][col]
                            )
                            if (mark != null) {
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
                        this@JPZXMLParser.boxes?.let { it[y][x] = box }
                }
            }

            fun parseArrow(attributes : Attributes) {
                if (inCellPosition == null)
                    return

                // only interpret "to" field which gives arrow direction
                // i don't quite understand the "from" field
                var to = attributes.getValue("to")
                if (to == null)
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

                inCellPosition?.row?.let { row ->
                    inCellPosition?.col?.let { col ->
                        boxes?.let { boxes ->
                            if (boxes[row][col] == null) {
                                boxes[row][col] = Box().apply{
                                    isBlock = true
                                }
                            }

                            boxes[row][col]?.let { it.shape = arrowShape }
                        }
                    }
                }
            }
        }

        fun isCell(attributes : Attributes) : Boolean {
            val cellType = attributes.getValue("type")
            if ("block".equals(cellType, ignoreCase = true))
                return false
            if ("void".equals(cellType, ignoreCase = true))
                return false
            if ("clue".equals(cellType, ignoreCase = true))
                return false
            return true
        }

        private val inClues : DefaultHandler = object : DefaultHandler() {
            private var inClueNum : String? = null
            private var inClueFormat : String? = ""
            private var inListName : String? = "No List"
            private var inClueZoneID : String? = null
            private var inClueCitation : String? = null
            private var inClueIndex = -1

            private val charBuffer = StringBuilder()

            @Throws(SAXException::class)
            override fun startElement(
                nsURI : String?,
                strippedName : String,
                tagName : String,
                attributes : Attributes,
            ) {
                var strippedName = strippedName
                strippedName = strippedName.trim()
                val name = if (strippedName.length == 0)
                    tagName.trim()
                else
                    strippedName

                try {
                    if (name.equals("title", ignoreCase = true)) {
                        charBuffer.delete(0, charBuffer.length)
                    } else if (name.equals("clue", ignoreCase = true)) {
                        charBuffer.delete(0, charBuffer.length)

                        inClueNum = attributes.getValue("number")
                        inClueIndex += 1

                        val link = attributes.getValue("is-link")
                        if (link == null) {
                            inClueFormat = attributes.getValue("format")
                            if (inClueFormat == null) inClueFormat = ""

                            inClueCitation = attributes.getValue("citation")
                            inClueZoneID = attributes.getValue("word")

                            // clue appears in characters between start
                            // and end
                        }
                    } else {
                        charBuffer.append("<" + tagName + ">")
                    }
                } catch (e : NumberFormatException) {
                    LOG.severe("Could not read JPZ XML cell data: " + e)
                }
            }

            @Throws(SAXException::class)
            override fun characters(ch : CharArray, start : Int, length : Int) {
                charBuffer.append(ch, start, length)
            }

            @Throws(SAXException::class)
            override fun endElement(
                nsURI : String?,
                strippedName : String,
                tagName : String,
            ) {
                var strippedName = strippedName
                strippedName = strippedName.trim()
                val name = if (strippedName.length == 0)
                    tagName.trim()
                else
                    strippedName

                if (name.equals("title", ignoreCase = true)) {
                    inListName = unHtmlString(charBuffer.toString())
                    inClueIndex = -1
                } else if (name.equals("clue", ignoreCase = true)) {
                    var fullClue = charBuffer.toString()

                    inClueFormat?.let { inClueFormat ->
                        if (inClueFormat.length > 0) {
                            fullClue = String.format(
                                "%s (%s)",
                                fullClue,
                                inClueFormat,
                            )
                        }
                    }

                    inListName?.let { inListName ->
                        clues.add(
                            JPZIO.ClueInfo(
                                inListName,
                                inClueIndex,
                                inClueNum,
                                fullClue,
                                inClueZoneID,
                                inClueCitation,
                            )
                        )
                    }

                    inClueNum = null
                    inClueFormat = ""
                    inClueZoneID = null
                    inClueCitation = null
                } else {
                    charBuffer.append("</" + tagName + ">")
                }
            }
        }

        private val inWord : DefaultHandler = object : DefaultHandler() {
            private var zoneID : String? = null
            private var zone : Zone? = null

            @Throws(SAXException::class)
            override fun startElement(
                nsURI : String?,
                strippedName : String,
                tagName : String,
                attributes : Attributes,
            ) {
                var strippedName = strippedName
                strippedName = strippedName.trim()
                val name = if (strippedName.length == 0)
                    tagName.trim()
                else
                    strippedName

                if (name.equals("word", ignoreCase = true)) {
                    zoneID = attributes.getValue("id")
                    zone = Zone()

                    val x = attributes.getValue("x")
                    val y = attributes.getValue("y")
                    if (x != null && y != null)
                        parseCells(x, y)
                } else if (name.equals("cells", ignoreCase = true)) {
                    parseCells(
                        attributes.getValue("x"),
                        attributes.getValue("y"),
                    )
                }
            }

            @Throws(SAXException::class)
            override fun endElement(
                nsURI : String?,
                strippedName : String,
                tagName : String,
            ) {
                var strippedName = strippedName
                strippedName = strippedName.trim()
                val name = if (strippedName.length == 0)
                    tagName.trim()
                else
                    strippedName

                if (name.equals("word", ignoreCase = true)) {
                    zoneID?.let { zoneID ->
                        zone?.let { zone ->
                            zoneMap.put(zoneID, zone)
                        }
                    }
                    zoneID = null
                    zone = null
                }
            }

            /**
             * Parse cells data into zone
             *
             * E.g. x="1-3" y = "2" is (2, 1), (2, 2), (2, 3);
             */
            fun parseCells(x : String, y : String) {
                val xs = x.split("-".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val xstart = xs[0].toInt() - 1
                val xend = if (xs.size > 1)
                    xs[1].toInt() - 1
                else
                    xstart

                val ys = y.split("-".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val ystart = ys[0].toInt() - 1
                val yend = if (ys.size > 1)
                    ys[1].toInt() - 1
                else
                    ystart

                for (row in ystart..yend) {
                    for (col in xstart..xend) {
                        zone?.addPosition(Position(row, col))
                    }
                }
            }
        }

        private var state : DefaultHandler = outerXML

        @Throws(SAXException::class)
        override fun startElement(
            nsURI : String?,
            strippedName : String,
            tagName : String,
            attributes : Attributes?,
        ) {
            var strippedName = strippedName
            strippedName = strippedName.trim()
            val name = if (strippedName.length == 0)
                tagName.trim()
            else
                strippedName

            if (name.equals("rectangular-puzzle", ignoreCase = true)) {
                hasRectangularPuzzleEle = true
            } else if (name.equals("grid", ignoreCase = true)) {
                hasGridEle = true
                state = inGrid
            } else if (name.equals("clues", ignoreCase = true)) {
                hasCluesEle = true
                state = inClues
            } else if (name.equals("word", ignoreCase = true)) {
                state = inWord
            } else if (name.equals("acrostic", ignoreCase = true)) {
                this.isAcrostic = true
            }

            state.startElement(nsURI, name, tagName, attributes)
        }

        @Throws(SAXException::class)
        override fun characters(ch : CharArray?, start : Int, length : Int) {
            state.characters(ch, start, length)
        }

        @Throws(SAXException::class)
        override fun endElement(
            nsURI : String?,
            strippedName : String,
            tagName : String,
        ) {
            var strippedName = strippedName
            strippedName = strippedName.trim()
            val name = if (strippedName.length == 0)
                tagName.trim()
            else
                strippedName

            state.endElement(nsURI, strippedName, tagName)

            if (name.equals("grid", ignoreCase = true)) {
                state = outerXML
            } else if (name.equals("clues", ignoreCase = true)) {
                state = outerXML
            } else if (name.equals("word", ignoreCase = true)) {
                state = outerXML
            }
        }
    }

    @Throws(Exception::class)
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return readPuzzle(inStream)
    }

    companion object {
        private val LOG = Logger.getLogger("app.crossword.yourealwaysbe")

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

        @JvmStatic
        @Throws(Exception::class)
        fun readPuzzle(inStream : InputStream?) : Puzzle? {
            val factory = SAXParserFactory.newInstance()
            val parser = factory.newSAXParser()
            val xr = parser.getXMLReader()
            val handler = JPZXMLParser()
            xr.setContentHandler(handler)
            xr.parse(InputSource(inStream))

            if (!handler.isSuccessfulRead)
                return null

            if (handler.isAcrostic || looksLikeAnAcrostic(handler)) {
                return buildAcrostic(handler) ?: buildCrossword(handler)
            } else {
                return buildCrossword(handler)
            }
        }

        fun convertPuzzle(
            inStream : InputStream?,
            os : DataOutputStream,
            d : LocalDate?,
        ) : Boolean {
            try {
                val puz = readPuzzle(inStream)
                if (puz != null) {
                    puz.date = d
                    IO.saveNative(puz, os)
                    return true
                } else {
                    return false
                }
            } catch (e : Exception) {
                e.printStackTrace()
                LOG.severe("Unable to convert JPZ file: " + e.message)
                return false
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

                    listNotes.get(listName)?.append(
                        String.format("<p>%s: %s</p>", number, citation)
                    )
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

