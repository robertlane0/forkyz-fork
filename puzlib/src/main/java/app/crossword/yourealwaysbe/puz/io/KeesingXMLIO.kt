
package app.crossword.yourealwaysbe.puz.io

import java.io.DataOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.util.Collections
import java.util.logging.Logger
import javax.xml.parsers.SAXParserFactory

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.Zone
import app.crossword.yourealwaysbe.puz.util.HtmlUtil.unHtmlString

/**
 * Converts a puzzle from the XML format used by web.keesing.com
 *
 * This is not necessarily a complete implementation, but works for the
 * sources tested.
 *
 * The (supported) XML format is:
 *
 * <puzzle type="Crossword" | type="Arrowword" width="[n]" height="[n]" ...>
 * <title>
 *   <grid>
 *     <cells>
 *       <cell
 *         x="[x]"
 *         y="[y]"
 *         visible="[1/0]"
 *         content="[solutionletter]"
 *         giveaway="[1/0]" (displaysolution)
 *         fillable="[1/0]"(blockshave0)
 *       >
 *         //forarrowwords
 *         <clue arrow="arrowrightdowntop"wordindex="i">Clue</cluearrow>
 *       </cell>...
 *     </cells>
 *   </grid>
 *   <wordgroups>
 *     <wordgroup index="[n]" kind="[horizontal|vertical|??]">
 *       <header>[listname]</header>
 *       <words>
 *         <word
 *           length="[n]"
 *           content="[solution]"
 *           giveaway="[0/1]"
 *           number="0" (i think this means infer number)
 *         >
 *           <cells>
 *             <cell x="[x]" y="[y]"/>...
 *           </cells>
 *           <puzzleword>[solution]</puzzleword>
 *           <clue>[hint]</clue>
 *         </word>...
 *       </words>
 *     </wordgroup>
 *   </wordgroups>
 *   </puzzle>
 *     </title>
 *   </puzzle>
 **/
class KeesingXMLIO : PuzzleParser {
    class KeesingXMLParserException(msg : String) : Exception(msg)

    @JvmRecord
    private data class ClueIndex(
        val group : String,
        val index : String?,
    ) : Comparable<ClueIndex> {
        override fun compareTo(other : ClueIndex) : Int {
            val gdiff = compareStringNums(group, other.group)
            return if (gdiff == 0)
                compareStringNums(index, other.index)
            else
                gdiff
        }
    }

    // time to make a superclass?
    private class ClueInfo(
        var listName : String?,
        var clueNumber : String?,
        var hint : String?,
        var zone : Zone?,
    )

    private class KeesingXMLParser : DefaultHandler() {
        private var type : String? = null
        var title : String = ""
            private set
        var width : Int = 0
            private set
        var height : Int = 0
            private set
        var boxes : Array<Array<Box?>>? = null
            private set
        val clues : MutableList<ClueInfo> = mutableListOf()
        private val indexedClues : MutableMap<ClueIndex, ClueInfo>
            = mutableMapOf()
        private val charBuffer = StringBuilder()

        // sanity checks
        private var hasGridEle = false
        private var hasWordGroupsEle = false

        val isSuccessfulRead : Boolean
            /**
             * Best assessment of whether read succeeded
             */
            get() = (
                (
                    CROSSWORD_TYPE.equals(type, ignoreCase = true)
                    || ARROWWORD_TYPE.equals(type, ignoreCase = true)
                )
                && hasGridEle
                && hasWordGroupsEle
                && this.width > 0
                && this.height > 0
                && this.clues.size > 0
            )

        val isArrowWord : Boolean
            get() = ARROWWORD_TYPE == type

        fun cleanHint(hint : String) : String {
            // in arrow word \ splits clue across lines
            if (this.isArrowWord)
                return hint.replace("\\", "")
            else
                return hint
        }

        /**
         * Add clue to clues
         *
         * If index info not use will be used to update existing clues
         */
        fun addClue(
            groupIndex : String,
            wordIndex : String?,
            info : ClueInfo,
        ) {
            val clueIndex = wordIndex?.let { ClueIndex(groupIndex, it) }
            val existing = clueIndex?.let { indexedClues.get(it) }
            if (existing == null) {
                clues.add(info)
                if (clueIndex != null)
                    indexedClues.put(clueIndex, info)
            } else {

                if (info.listName != null)
                    existing.listName = info.listName
                if (info.clueNumber != null)
                    existing.clueNumber = info.clueNumber
                if (info.hint != null)
                    existing.hint = info.hint
                if (info.zone != null)
                    existing.zone = info.zone
            }
        }

        // Use several handlers to maintain three different modes:
        // outerXML, inGrid, and inClues
        private val outerXML : DefaultHandler = object : DefaultHandler() {
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

                if (name.equals("puzzle", ignoreCase = true)) {
                    charBuffer.delete(0, charBuffer.length)

                    type = attributes.getValue("type")
                    width = attributes.getValue("width").toInt()
                    height = attributes.getValue("height").toInt()
                    boxes = Array<Array<Box?>>(height) {
                        arrayOfNulls<Box>(width)
                    }
                } else if (name.equals("title", ignoreCase = true)) {
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
                } else {
                    charBuffer.append("</" + tagName + ">")
                }
            }
        }

        private val inGrid : DefaultHandler = object : DefaultHandler() {
            private var inClueGroupIndex : String? = null
            private var inClueWordIndex : String? = null
            private val charBuffer = StringBuilder()
            private var inBox : Box? = null

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

                if (name.equals("cell", ignoreCase = true)) {
                    parseCell(attributes)
                } else if (name.equals("clue", ignoreCase = true)) {
                    inClueGroupIndex = attributes.getValue("groupindex")
                    inClueWordIndex = attributes.getValue("wordindex")
                    charBuffer.delete(0, charBuffer.length)
                } else if (
                    name.equals("border", ignoreCase = true)
                ) {
                    inBox?.let { inBox ->
                        val position = attributes.getValue("position")
                        if ("top".equals(position, ignoreCase = true))
                            inBox.barTop = Box.Bar.DASHED
                        else if ("bottom".equals(position, ignoreCase = true))
                            inBox.barBottom = Box.Bar.DASHED
                        else if ("left".equals(position, ignoreCase = true))
                            inBox.barLeft = Box.Bar.DASHED
                        else if ("right".equals(position, ignoreCase = true))
                            inBox.barRight = Box.Bar.DASHED
                    }
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

                if (name.equals("clue", ignoreCase = true)) {
                    endClue()
                } else if (name.equals("cell", ignoreCase = true)) {
                    inBox = null
                }
            }

            fun endClue() {
                val hint = cleanHint(charBuffer.toString())
                inClueGroupIndex?.let { inClueGroupIndex ->
                    inClueWordIndex?.let { inClueWordIndex ->
                        addClue(
                            inClueGroupIndex,
                            inClueWordIndex,
                            ClueInfo(null, null, hint, null),
                        )
                    }
                }
                inClueGroupIndex = null
                inClueWordIndex = null
            }

            fun parseCell(attributes : Attributes) {
                val x = attributes.getValue("x").toInt()
                val y = attributes.getValue("y").toInt()

                if (
                    isCell(attributes)
                    && 0 <= x
                    && x < width
                    && 0 <= y
                    && y < height
                ) {
                    val box = Box()

                    val solution = attributes.getValue("content")
                    if (solution != null && solution.length > 0) {
                        box.solution = solution
                        if ("1" == attributes.getValue("giveaway"))
                            box.response = solution
                    }

                    boxes?.let { boxes ->
                        boxes[y][x] = box
                    }
                    inBox = box
                }
            }

            fun isCell(attributes : Attributes) : Boolean {
                val content = attributes.getValue("content")
                val fillable = attributes.getValue("fillable")

                val block = (
                    (content == null || content.isEmpty())
                    && "0" == fillable
                )

                return !block
            }
        }

        private val inClues : DefaultHandler = object : DefaultHandler() {
            private var inListName : String? = null
            private var inClueNumber : String? = null
            private var inClueHint : String? = null
            private var inClueZone : Zone? = null
            private var inGroupIndex : String? = null
            private var inWordIndex : String? = null

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
                    if (name.equals("wordgroup", ignoreCase = true)) {
                        charBuffer.delete(0, charBuffer.length)
                        inGroupIndex = attributes.getValue("index")
                    } else if (name.equals("header", ignoreCase = true)) {
                        charBuffer.delete(0, charBuffer.length)
                    } else if (name.equals("word", ignoreCase = true)) {
                        charBuffer.delete(0, charBuffer.length)

                        inWordIndex = attributes.getValue("index")

                        inClueNumber = attributes.getValue("number")
                        if ("0" == inClueNumber)
                            inClueNumber = null

                        inClueZone = Zone()
                    } else if (name.equals("cell", ignoreCase = true)) {
                        inClueZone?.let { inClueZone ->
                            val x = attributes.getValue("x").toInt()
                            val y = attributes.getValue("y").toInt()
                            inClueZone.addPosition(Position(y, x))
                        }
                    } else if (name.equals("clue", ignoreCase = true)) {
                        charBuffer.delete(0, charBuffer.length)
                    } else {
                        charBuffer.append("<" + tagName + ">")
                    }
                } catch (e : NumberFormatException) {
                    LOG.severe("Could not read Keesing XML cell data: " + e)
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

                if (name.equals("wordgroup", ignoreCase = true)) {
                    inWordIndex = null
                } else if (name.equals("header", ignoreCase = true)) {
                    inListName = unHtmlString(charBuffer.toString())
                } else if (name.equals("clue", ignoreCase = true)) {
                    inClueHint = cleanHint(charBuffer.toString())
                } else if (name.equals("word", ignoreCase = true)) {
                    inGroupIndex?.let { inGroupIndex ->
                        inListName?.let { inListName ->
                            addClue(
                                inGroupIndex,
                                inWordIndex,
                                ClueInfo(
                                    inListName,
                                    inClueNumber,
                                    inClueHint,
                                    inClueZone,
                                )
                            )
                        }
                    }
                    inWordIndex = null
                    inClueNumber = null
                    inClueZone = null
                } else if (name.equals("wordgroup", ignoreCase = true)) {
                    inListName = null
                    inGroupIndex = null
                } else {
                    charBuffer.append("</" + tagName + ">")
                }
            }
        }

        private var state : DefaultHandler = outerXML

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

            if (name.equals("grid", ignoreCase = true)) {
                hasGridEle = true
                state = inGrid
            } else if (name.equals("wordgroups", ignoreCase = true)) {
                hasWordGroupsEle = true
                state = inClues
            }

            state.startElement(nsURI, name, tagName, attributes)
        }

        @Throws(SAXException::class)
        override fun characters(ch : CharArray, start : Int, length : Int) {
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
            } else if (name.equals("wordgroups", ignoreCase = true)) {
                state = outerXML
            }
        }
    }

    @Throws(Exception::class)
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return readPuzzle(inStream)
    }

    companion object {
        private val LOG = Logger.getLogger(
            KeesingXMLIO::class.java.getCanonicalName(),
        )

        private const val CROSSWORD_TYPE = "Crossword"
        private const val ARROWWORD_TYPE = "Arrowword"

        @JvmStatic
        @Throws(Exception::class)
        fun readPuzzle(inStream : InputStream) : Puzzle? {
            val factory = SAXParserFactory.newInstance()
            val parser = factory.newSAXParser()
            val xr = parser.getXMLReader()
            val handler = KeesingXMLParser()
            xr.setContentHandler(handler)
            xr.parse(InputSource(inStream))

            val gottenBoxes = handler.boxes
            val gottenClues = handler.clues

            if (!handler.isSuccessfulRead || gottenBoxes == null)
                return null

            val builder = PuzzleBuilder(gottenBoxes)
            builder.setTitle(handler.title)

            numberBoxes(gottenClues, builder)

            val clues = getNumberedClues(gottenClues, builder.puzzle)
            for (entry in clues.entries) {
                val listName = entry.key
                val clues = entry.value
                for (clue in clues) {
                    val index = builder.getNextClueIndex(listName)
                    clue.hint?.let { hint ->
                        builder.addClue(
                            Clue(
                                listName,
                                index,
                                clue.clueNumber,
                                null,
                                hint,
                                clue.zone,
                            ),
                        )
                    }
                }
            }

            return builder.puzzle
        }

        fun convertPuzzle(
            inStream : InputStream,
            os : DataOutputStream,
            d : LocalDate,
        ) : Boolean {
            try {
                val puz : Puzzle? = readPuzzle(inStream)
                if (puz == null) {
                    return false
                } else {
                    puz.date = d
                    IO.saveNative(puz, os)
                }
                return true
            } catch (e : Exception) {
                LOG.severe("Unable to convert Keesing XML file: " + e.message)
                return false
            }
        }

        /**
         * Auto number boxes by clue starts
         */
        private fun numberBoxes(
            clues : List<ClueInfo>,
            builder : PuzzleBuilder,
        ) {
            val clueStarts : MutableSet<Position> = mutableSetOf()
            for (info in clues) {
                val zone = info.zone
                val start = if (zone == null || zone.isEmpty)
                    null
                else
                    zone.getPosition(0)
                if (start != null)
                    clueStarts.add(start)
            }

            var clueCount = 1

            for (row in 0..<builder.height) {
                for (col in 0..<builder.width) {
                    val pos = Position(row, col)
                    val box = builder.getBox(pos)

                    if (box == null || isBlock(box)) {
                        continue
                    }

                    val isStart = clueStarts.contains(pos)

                    if (isStart) {
                        val autoNumber = clueCount.toString()
                        box.clueNumber = autoNumber
                        clueCount++
                    }
                }
            }
        }

        /**
         * Fill in clue numbers and split ClueInfo into lists
         *
         * @return map from list name to clue info with clue number
         */
        @Throws(KeesingXMLParserException::class)
        private fun getNumberedClues(
            clues : List<ClueInfo>,
            puz : Puzzle,
        ) : Map<String, List<ClueInfo>> {
            val sortedClues : MutableMap<String, MutableList<ClueInfo>>
                = mutableMapOf()

            for (clue in clues) {
                var listName = clue.listName
                if (listName == null)
                    listName = "No list"

                val clueList : MutableList<ClueInfo>
                    = sortedClues.get(listName) ?: {
                        val newList = mutableListOf<ClueInfo>()
                        sortedClues.put(listName, newList)
                        newList
                    }()

                if (clue.clueNumber == null) {
                    val zone = clue.zone
                    if (zone != null && !zone.isEmpty) {
                        val start = puz.checkedGetBox(zone.getPosition(0))
                        if (start == null || isBlock(start)) {
                            throw KeesingXMLParserException(
                                "Clue starts at block box " + zone.getPosition(0)
                            )
                        }
                        clue.clueNumber = start.clueNumber
                    }
                }

                clueList.add(clue)
            }

            for (list in sortedClues.values) {
                Companion.sortClueList(list)
            }

            return sortedClues
        }

        private fun sortClueList(clues : MutableList<ClueInfo>) {
            clues.sortWith { ci1, ci2 ->
                compareStringNums(ci1.clueNumber, ci2.clueNumber)
            }
        }

        private fun compareStringNums(num1 : String?, num2 : String?) : Int {
            if (num1 == null && num2 == null)
                return 0
            else if (num1 == null)
                return -1
            else if (num2 == null)
                return 1

            try {
                return Integer.compare(num1.toInt(), num2.toInt())
            } catch (e : NumberFormatException) {
                return num1.compareTo(num2)
            }
        }
    }
}

