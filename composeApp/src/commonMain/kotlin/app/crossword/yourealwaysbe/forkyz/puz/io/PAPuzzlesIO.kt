
package app.crossword.yourealwaysbe.forkyz.puz.io

import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min
import kotlin.text.RegexOption.IGNORE_CASE
import kotlinx.io.IOException
import kotlinx.io.Source

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.parseSource

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Clue
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.forkyz.puz.Zone

/**
 * Read crosswords from https://www.pa-puzzles.com
 *
 * URL e.g. https://www.pa-puzzles.com/puzzles/puzzlegroupembed.php?pid=22&cs=55
 *
 * Format is HTML, 1-based indices.
 *
 * <div id="puzzle-grid" data-x="{width}"...>
 *   <div class="row">
 *     <div id="cell-{col}-{row}" class="cell (blank)"> [Blank Cell]
 *     <div id="cell-{col}-{row}" class="cell" data-clueId="{clueId}">
 *       <span class="cell-number">{number}</span>
 *       <div class="cell-input" data-solution="{solution}"/>
 *     </div>
 *     ...
 *   </div>
 *   ...
 * </div>
 * <div class="clues">
 *   <h2>list name</h2>
 *   ...
 *   <div class="clues-{across|down}">
 *       <li id="{clueId}"
 *           data-direction="{across|down}"
 *           data-cluenum="{number}"
 *           data-islink="{0/1}"
 *       ><b>{number}</b> {hint}</li>
 *   </div>
 *   ...
 * </div>
 *
 * Split clues have e.g. {direction1}-{direction2} as the direction and
 * {num1}/{num2} as the number. data-islink for the second clue will be
 * 1. Else 0.
 *
 * There are sometimes two copies of the clues my-content-desktop-* and
 * my-content-mobile-*.
 */
class PAPuzzlesIO : PuzzleParser {
    /**
     * An unfancy exception indicating error while parsing
     */
    class PAPuzzlesFormatException(msg : String?) : Exception(msg)

    @Throws(Exception::class)
    override fun parseInput(input : Source) : Puzzle? {
        return readPuzzle(input)
    }

    companion object {
        private val LOG
            = Logger.getLogger(PAPuzzlesIO::class.java.getCanonicalName())

        private val CELL_ID_RE = "cell-(\\d+)-(\\d+)".toRegex(IGNORE_CASE)
        private const val CELL_ID_COL_GRP = 1
        private const val CELL_ID_ROW_GRP = 2

        private val CLUE_HINT_RE = "\\s*[\\d/]+\\s*(.*)".toRegex()
        private const val CLUE_HINT_GRP = 1

        @Throws(IOException::class)
        fun readPuzzle(input : Source) : Puzzle? {
            try {
                val doc = Ksoup.parseSource(input)
                val builder = PuzzleBuilder(getBoxes(doc))
                getClues(doc, builder)
                return builder.puzzle
            } catch (e : IOException) {
                LOG.info("Could not read PA Puzzles crossword: " + e)
                return null
            } catch (e : PAPuzzlesFormatException) {
                LOG.info("Could not read PA Puzzles crossword: " + e)
                return null
            }
        }

        /**
         * Read boxes from HTML
         *
         * Given a map from clueID to clue number
         */
        @Throws(PAPuzzlesFormatException::class)
        private fun getBoxes(doc : Element) : Array<Array<Box?>> {
            val grid = doc.selectFirst("#puzzle-grid")
            if (grid == null)
                throw PAPuzzlesFormatException("No #puzzle-grid found.")

            var maxRow = 0
            var maxCol = 0

            val boxMap : MutableMap<Position, Box> = mutableMapOf()

            for (row in grid.select(".row")) {
                for (cell in row.select(".cell")) {
                    if (cell.hasClass("blank"))
                        continue

                    val pos = getCellPosition(cell)
                    val number = getCellNumber(cell)
                    val solution = getCellSolution(cell)
                    if (pos != null) {
                        val box = Box()
                        if (number != null)
                            box.clueNumber = number
                        if (solution != null)
                            box.solution = solution
                        boxMap.put(pos, box)

                        maxRow = max(pos.row, maxRow)
                        maxCol = max(pos.col, maxCol)
                    }
                }
            }

            val boxes = Array<Array<Box?>>(maxRow + 1) {
                arrayOfNulls<Box>(maxCol + 1)
            }
            for (row in 0..maxRow) {
                for (col in 0..maxCol) {
                    val pos = Position(row, col)
                    boxes[row][col] = boxMap.get(pos)
                }
            }

            return boxes
        }

        /**
         * Parse position from cell id
         *
         * E.g. cell-5-3 for col 4 row 2
         */
        private fun getCellPosition(cell : Element) : Position? {
            val m = CELL_ID_RE.matchEntire(cell.id())
            val row = m?.groups[CELL_ID_ROW_GRP]?.value?.toInt()
            val col = m?.groups[CELL_ID_COL_GRP]?.value?.toInt()
            if (row != null && col != null)
                return Position(row - 1, col - 1)
            else
                return null
        }

        /**
         * Get clue number in cell if there is one else null
         */
        private fun getCellNumber(cell : Element?) : String? {
            if (cell == null)
                return null

            val number = cell.selectFirst(".cell-number")
            return number?.text()?.trim()
        }

        /**
         * Get solution for cell if there is one
         */
        private fun getCellSolution(cell : Element?) : String? {
            if (cell == null)
                return null

            val input = cell.selectFirst(".cell-input")
            if (input == null)
                return null

            val solution = input.attr("data-solution")
            return if (solution.isEmpty()) null else solution
        }

        private fun getClues(doc : Element, builder : PuzzleBuilder) {
            getClues(doc, "clues-across", builder)
            getClues(doc, "clues-down", builder)
        }

        /**
         * Get clues from a specific list
         *
         * @param doc the doc to get from
         * @param listClass the class name identifying the list
         * @param builder add clues to this builder
         */
        private fun getClues(
            doc : Element,
            listClass : String?,
            builder : PuzzleBuilder,
        ) {
            val clueList = doc.selectFirst(".clues ." + listClass)
            if (clueList == null)
                return

            var listName = findListName(clueList)

            for (clue in clueList.select("li")) {
                val number = clue.attr("data-cluenum")
                val numbers : Array<String> = number.split("/".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (numbers.size == 0)
                    continue

                val directions : Array<String> = clue.attr("data-direction")
                    .split("-".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (directions.size == 0)
                    continue

                val hint : String? = getClueHint(clue)
                if (hint == null)
                    continue

                val numClues = min(numbers.size, directions.size)
                val zone = Zone()

                val islink = clue.attr("data-islink")
                if ("1" != clue.attr("data-islink")) {
                    for (i in 0..<numClues) {
                        if ("across".equals(directions[i], ignoreCase = true)) {
                            if (i == 0 && listName == null)
                                listName = DefaultListNames.ACROSS_LIST
                            zone.appendZone(builder.getAcrossZone(numbers[i]))
                        } else if (
                            "down".equals(directions[i], ignoreCase = true)
                        ) {
                            if (i == 0 && listName == null)
                                listName = DefaultListNames.DOWN_LIST
                            zone.appendZone(builder.getDownZone(numbers[i]))
                        }
                    }
                }

                if (listName != null) {
                    val index = builder.getNextClueIndex(listName)
                    builder.addClue(Clue(listName, index, number, hint, zone))
                }
            }
        }

        /**
         * Search previous siblings of parent looking for <h2> element with
         * title.
         *
         * Because clue list ul is inside clue div after h2 with title.
         */
        private fun findListName(clueList : Element) : String? {
            var cur = clueList.parentElement()
            while (cur != null) {
                if ("h2".equals(cur.tagName(), ignoreCase = true))
                    return cur.text()
                cur = cur.previousElementSibling()
            }
            return null
        }

        private fun getClueHint(clue : Element?) : String? {
            if (clue == null)
                return null

            // don't keep HTML since it can contain odd things like flex
            // divs
            return CLUE_HINT_RE.matchEntire(clue.text())
                ?.groups[CLUE_HINT_GRP]
                ?.value
                ?.trim()
        }
    }
}

