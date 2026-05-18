
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlinx.datetime.LocalDate
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.Clue
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.forkyz.puz.Zone
import app.crossword.yourealwaysbe.forkyz.util.HtmlUtil.unHtmlString
import app.crossword.yourealwaysbe.forkyz.util.XMLElement
import app.crossword.yourealwaysbe.forkyz.util.parseXMLElement

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

    private class KeesingXMLParser(content : XMLElement) {
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

        init {
            readMetaData(content)
            readBoxes(content)
            readClues(content)
        }

        private fun readMetaData(content : XMLElement) {
            content.selectFirst("puzzle")?.let { puzzle ->
                type = puzzle.attr("type").ifEmpty { null }
            }
            content.selectFirst("title")?.let { title = it.text() }
        }

        private fun readBoxes(content : XMLElement) {
            content.selectFirst("puzzle")?.let { puzzle ->
                width = puzzle.attr("width").toIntOrNull() ?: 0
                height = puzzle.attr("height").toIntOrNull() ?: 0
                if (width > 0 && height > 0)
                    boxes = Array(height) { arrayOfNulls<Box>(width) }
            }
            if (boxes != null) {
                content.selectFirst("grid")?.let { grid ->
                    hasGridEle = true
                    for (cell in grid.select("cell")) {
                        readGridCell(cell)
                    }
                }
            }
        }

        private fun readGridCell(cell : XMLElement) {
            val x = cell.attr("x").toIntOrNull() ?: return
            val y = cell.attr("y").toIntOrNull() ?: return

            if (
                isGridCell(cell)
                && x >= 0
                && x < width
                && y >= 0
                && y < height
            ) {
                val box = Box()
                val solution = cell.attr("content")
                if (solution.isNotEmpty()) {
                    box.solution = solution
                    if ("1" == cell.attr("giveaway"))
                        box.response = solution
                }
                boxes?.let { it[y][x] = box }

                for (border in cell.select("border")) {
                    readBorder(box, border)
                }
            }

            for (clue in cell.select("clue")) {
                readGridClue(clue)
            }
        }

        private fun isGridCell(cell : XMLElement) : Boolean {
            val content = cell.attr("content")
            val fillable = cell.attr("fillable")
            return !(content.isEmpty() && "0" == fillable)
        }

        private fun readBorder(box : Box, border : XMLElement) {
            val position = border.attr("position")
            if ("top".equals(position, ignoreCase = true))
                box.barTop = Box.Bar.DASHED
            else if ("bottom".equals(position, ignoreCase = true))
                box.barBottom = Box.Bar.DASHED
            else if ("left".equals(position, ignoreCase = true))
                box.barLeft = Box.Bar.DASHED
            else if ("right".equals(position, ignoreCase = true))
                box.barRight = Box.Bar.DASHED
        }

        private fun readGridClue(clue : XMLElement) {
            val groupIndex = clue.attr("groupindex").ifEmpty { null } ?: return
            val wordIndex = clue.attr("wordindex").ifEmpty { null } ?: return
            addClue(
                groupIndex,
                wordIndex,
                ClueInfo(null, null, cleanHint(clue.text()), null),
            )
        }

        private fun readClues(content : XMLElement) {
            content.selectFirst("wordgroups")?.let { wordgroups ->
                hasWordGroupsEle = true
                for (wordgroup in wordgroups.select("wordgroup")) {
                    readWordGroup(wordgroup)
                }
            }
        }

        private fun readWordGroup(wordgroup : XMLElement) {
            val groupIndex = wordgroup.attr("index").ifEmpty { null } ?: return
            val listName = wordgroup.selectFirst("header")
                ?.let { unHtmlString(it.text()) }
                ?: return
            for (word in wordgroup.select("word")) {
                readWord(word, groupIndex, listName)
            }
        }

        private fun readWord(
            word : XMLElement,
            groupIndex : String,
            listName : String,
        ) {
            val wordIndex = word.attr("index").ifEmpty { null }
            val rawNum = word.attr("number")
            val clueNumber = if (rawNum == "0" || rawNum.isEmpty())
                null
            else
                rawNum
            val zone = Zone()
            for (cell in word.select("cell")) {
                val x = cell.attr("x").toIntOrNull() ?: continue
                val y = cell.attr("y").toIntOrNull() ?: continue
                zone.addPosition(Position(y, x))
            }
            val hint = word.selectFirst("clue")?.let { cleanHint(it.text()) }
            addClue(
                groupIndex,
                wordIndex,
                ClueInfo(listName, clueNumber, hint, zone),
            )
        }

        private fun cleanHint(hint : String) : String {
            return if (isArrowWord) hint.replace("\\", "") else hint
        }

        private fun addClue(
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
    }

    @Throws(Exception::class)
    override suspend fun parseInput(input : Source) : Puzzle? {
        return readPuzzle(input)
    }

    companion object {
        private const val CROSSWORD_TYPE = "Crossword"
        private const val ARROWWORD_TYPE = "Arrowword"

        @Throws(Exception::class)
        fun readPuzzle(input : Source) : Puzzle? {
            val content = parseXMLElement(input)
            val handler = KeesingXMLParser(content)

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
                return num1.toInt().compareTo(num2.toInt())
            } catch (e : NumberFormatException) {
                return num1.compareTo(num2)
            }
        }
    }
}
