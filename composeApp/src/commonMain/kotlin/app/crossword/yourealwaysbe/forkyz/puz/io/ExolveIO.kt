
package app.crossword.yourealwaysbe.forkyz.puz.io

import java.util.logging.Logger
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlinx.io.readLine

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Clue
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.forkyz.puz.Zone
import app.crossword.yourealwaysbe.forkyz.util.HtmlUtil
import app.crossword.yourealwaysbe.forkyz.util.HtmlUtil.parseHtmlColor

/**
 * Parser for Exolve format
 *
 * Format url:
 * https://github.com/viresh-ratnakar/exolve
 *
 * Here be dragons
 * TODO: exolve-color
 */
class ExolveIO : PuzzleParser {
    class ExolveFormatException(msg : String) : Exception(msg)

    @Throws(IOException::class)
    override fun parseInput(input : Source) : Puzzle? {
        return readPuzzle(input)
    }

    private class ExolveClue(
        /**
         * Zone has an ununsual meaning
         *
         * May be null if no zone specified.
         * A zone may be only 1 box, in which case it is the start box
         * of an across/down clue.
         * Else it's the full zone.
         * Only has zone for first number, rest may need to be appended
         * later.
         */
        var zone : Zone?,
        val number : String?,
        /**
         * Any extra numbers where part of the answer lies (or null)
         */
        val extraNumbers : Array<String>?,
        val hint : String,
        val annotation : String?
    ) {
        companion object {
            // see exolve def of enums
            private val CLUE_UPTO_ENUM_RE = Regex(
                ("(\\(\\d[\\d-,'\\s.]*\\)"
                        + "|\\([^)]*(w|l)\\w*[^)]*\\)"
                        + "|\\([^)]*\\?\\))\\*?"),
                RegexOption.IGNORE_CASE
            )

            // Eg. 1, 1a, or [ONE]
            private const val ONE_NUMBER_RE = "(?:\\d+[adbu]?|\\[[^\\]]+\\])"
            private val CLUE_PARTS_RE = Regex( // zone
                ("\\s*((?:(?:\\#c\\d+r\\d+|\\#r\\d+c\\d+|\\#[a-z]\\d+)\\s*)*)"
                // number
                + "\\s*(" + ONE_NUMBER_RE + ")"
                // extra numbers (and start of hint text)
                + "(((?:\\s*[,&]\\s*" + ONE_NUMBER_RE + ")*)"
                // rest of hint (may include an initial . or spaces to
                // remove later)
                + ".*)")
            )
            private const val CLUE_PART_ZONE_GRP = 1
            private const val CLUE_PART_NUMBER_GRP = 2
            private const val CLUE_PART_EXTRA_NUMBERS_GRP = 4
            private const val CLUE_PART_HINT_GRP = 3

            private val COL_ROW_COORD_RE = Regex("\\#?c(\\d+)r(\\d+)")
            private const val COL_ROW_COORD_ROW_GRP = 2
            private const val COL_ROW_COORD_COL_GRP = 1
            private val ROW_COL_COORD_RE = Regex("\\#?r(\\d+)c(\\d+)")
            private const val ROW_COL_COORD_ROW_GRP = 1
            private const val ROW_COL_COORD_COL_GRP = 2
            private val LETTER_NUM_COORD_RE = Regex("\\#?([a-z])(\\d+)")
            private const val LETTER_NUM_COORD_ROW_GRP = 2
            private const val LETTER_NUM_COORD_COL_GRP = 1
            private const val ANNOTATION_MARKER = "[]"

            /**
             * Parse a clue line
             *
             * Puzzle height needed because rows go from the bottom
             */
            @Throws(ExolveFormatException::class)
            fun parseClueLine(
                line : String?,
                height : Int,
            ) : ExolveClue? {
                if (line == null)
                    return null

                var clue : String? = line

                // First peel off annotation before parsing clue
                var annotation : String? = null
                val hintEnd = getHintEnd(line)
                if (hintEnd >= 0) {
                    annotation = line.substring(hintEnd).trim()
                    if (annotation.startsWith(ANNOTATION_MARKER)) {
                        annotation = annotation
                            .substring(ANNOTATION_MARKER.length)
                            .trim()
                    }
                    clue = line.substring(0, hintEnd).trim()
                }

                val m = CLUE_PARTS_RE.matchEntire(clue)
                if (m != null) {
                    val zoneString = m.groupValues[CLUE_PART_ZONE_GRP]
                    val number = unwrapNumber(m.groupValues[CLUE_PART_NUMBER_GRP])
                    val extraNumbersString
                        = m.groupValues[CLUE_PART_EXTRA_NUMBERS_GRP]
                    val hint = parseHint(m.groupValues[CLUE_PART_HINT_GRP])

                    val extraNumbers = parseNumbers(extraNumbersString)
                    val zone = parseZone(zoneString, height)

                    return ExolveClue(
                        zone,
                        number,
                        extraNumbers,
                        hint,
                        annotation,
                    )
                } else {
                    // unparsable, return a "dummy" clue for display in clue
                    // list
                    return ExolveClue(null, null, null, line, null)
                }
            }

            fun parseHint(hint : String) : String {
                var hint = hint
                hint = hint.trim()
                // remove initial .
                if (hint.startsWith(".") && !hint.startsWith("..")) {
                    hint = hint.substring(1).trim()
                }
                // remove enum if to be hidden
                if (hint.endsWith("*")) {
                    val enumStart = hint.lastIndexOf("(")
                    if (enumStart >= 0) {
                        hint = hint.substring(0, enumStart).trim()
                    }
                }
                // underline bits
                return hint.replace(
                    "~\\{(\\{[^}]*\\})?([^}]*)\\}~".toRegex(),
                    "<u>$2</u>",
                )
            }

            /**
             * Parse a position string
             *
             * E.g. #r10c13 or #a11. Throws error if not parseable.
             *
             * puzzle height needed since row specified from bottom
             */
            @Throws(ExolveFormatException::class)
            fun parsePosition(
                pos : String,
                height : Int,
            ) : Position {
                var m = ROW_COL_COORD_RE.matchEntire(pos)
                if (m != null) {
                    try {
                        val row = m.groupValues[ROW_COL_COORD_ROW_GRP].toInt()
                        val col = m.groupValues[ROW_COL_COORD_COL_GRP].toInt()
                        return Position(height - row, col - 1)
                    } catch (e : NumberFormatException) {
                        throw ExolveFormatException("Bad position: " + pos)
                    }
                }

                m = COL_ROW_COORD_RE.matchEntire(pos)
                if (m != null) {
                    try {
                        val row = m.groupValues[COL_ROW_COORD_ROW_GRP].toInt()
                        val col = m.groupValues[COL_ROW_COORD_COL_GRP].toInt()
                        return Position(height - row, col - 1)
                    } catch (e : NumberFormatException) {
                        throw ExolveFormatException("Bad position: " + pos)
                    }
                }

                m = LETTER_NUM_COORD_RE.matchEntire(pos)
                if (m != null) {
                    try {
                        val row = m.groupValues[LETTER_NUM_COORD_ROW_GRP].toInt()
                        val colLetter = m.groupValues[LETTER_NUM_COORD_COL_GRP]
                        if (colLetter.length != 1) {
                            throw ExolveFormatException("Bad position: " + pos)
                        }
                        val col = colLetter.get(0).code - 'a'.code
                        return Position(height - row, col)
                    } catch (e : NumberFormatException) {
                        throw ExolveFormatException("Bad position: " + pos)
                    }
                }

                throw ExolveFormatException("Bad position: " + pos)
            }

            /**
             * Find the end of the hint in the clue list
             *
             * I.e. [] or enumeration. [] will be after the clueEnd if it
             * was used.
             *
             * @return -1 if none found
             */
            fun getHintEnd(clue : String) : Int {
                // split off annotations and possibly enum
                var clueEnd = clue.indexOf(ANNOTATION_MARKER)
                if (clueEnd >= 0)
                    return clueEnd

                clueEnd = -1

                for (m in CLUE_UPTO_ENUM_RE.findAll(clue)) {
                    clueEnd = m.range.last + 1
                }

                return clueEnd
            }

            /**
             * Parse zone of clue
             *
             * Height needed for position parsing.
             */
            @Throws(ExolveFormatException::class)
            private fun parseZone(
                zoneString : String?,
                height : Int,
            ) : Zone? {
                if (zoneString == null || zoneString.trim().isEmpty())
                    return null

                val zone = Zone()
                for (
                    cell in zoneString.split("\\s+".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                ) {
                    zone.addPosition(parsePosition(cell, height))
                }

                return zone
            }

            private fun unwrapNumber(number : String) : String {
                var number = number
                number = number.trim()
                if (number.startsWith("[") && number.endsWith("]"))
                    return number.substring(1, number.length - 1)
                else
                    return number
            }

            private fun parseNumbers(numbersString : String?) : Array<String>? {
                if (numbersString == null || numbersString.isEmpty())
                    return null

                val numbers = numbersString.split("[&,]".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                for (i in numbers.indices)
                    numbers[i] = unwrapNumber(numbers[i])
                return numbers
            }
        }
    }

    private class ExolveClueLists(
        val across : ExolveClueList,
        val down : ExolveClueList,
        val nodir : ExolveClueList,
    )

    private class ExolveClueList(
        val title : String?,
        val clues : List<ExolveClue>,
    )

    private class ExolveBoxes(
        val boxes : Array<Array<Box?>>,
        val skipNumberCells : Set<Position>,
    )

    private data class Reversal(
        var start : Position,
        var end : Position,
    )

    private class BoxRow(
        val boxes : Array<Box?>,
        val hasDiagramless : Boolean,
    )

    companion object {
        private val LOG
                = Logger.getLogger(ExolveIO::class.java.getCanonicalName())

        private const val ACROSS_LIST = "Across"
        private const val DOWN_LIST = "Down"
        private const val NODIR_LIST = "Clues"

        private val MULTILINES_SECTIONS = setOf(
            "exolve-preamble",
            "exolve-prelude",
            "exolve-postscript",
            "exolve-grid",
            "exolve-across",
            "exolve-down",
            "exolve-nodir",
            "exolve-explanations",
            "exolve-maker",
            "exolve-relabel"
        )

        private val CELL_MODIFIERS = setOf('|', '_', '+', '@', '*', '!', '~')

        private const val ESCAPE = '&'

        // b for back
        private val ACROSS_SUFFIXES = setOf("a", "ac", "aw", "b")

        // u for up
        private val DOWN_SUFFIXES = setOf("d", "dn", "u")

        @Throws(IOException::class)
        fun readPuzzle(input : Source) : Puzzle? {
            try {
                val data : Map<String, String> = extractData(input)
                return readPuzzleFromData(data)
            } catch (e : ExolveFormatException) {
                LOG.info("Failed to parse Exolve file: " + e)
                return null
            }
        }

        @Throws(IOException::class, ExolveFormatException::class)
        private fun extractData(
            input : Source,
        ) : Map<String, String> {
            val data : MutableMap<String, String> = mutableMapOf()

            chompToStart(input)

            var line = input.readLine()

            while (line != null) {
                line = line.trim()

                if (isEnd(line))
                    break

                if (isComment(line) || line.isEmpty()) {
                    line = input.readLine()
                    continue
                }

                if (!isSection(line)) {
                    throw ExolveFormatException(
                        "Line is not the start of a section: " + line
                    )
                }

                var key : String? = line
                var value = ""

                val valueStart = line.indexOf(":")
                if (valueStart >= 0) {
                    // because even the exolve examples don't always end
                    // with a colon... hope the line is entirely a key
                    key = line.substring(0, valueStart).trim()
                        .lowercase()
                    value = line.substring(valueStart + 1).trim()
                }

                if (!MULTILINES_SECTIONS.contains(key)) {
                    addKeyValue(data, key, value)
                    line = input.readLine()
                } else {
                    val fullValue = StringBuilder()
                    fullValue.append(value + "\n")

                    while (true) {
                        line = input.readLine()
                        if (line == null) break
                        line = line.trim()
                        if (isSection(line)) {
                            // leave line variable for next iteration
                            break
                        }

                        if (!isComment(line))
                            fullValue.append(line + "\n")
                    }

                    addKeyValue(data, key, fullValue.toString())
                }
            }

            return data
        }

        /**
         * Add value to the key, appending to existing sep w/ newline
         */
        private fun addKeyValue(
            data : MutableMap<String, String>,
            key : String,
            value : String,
        ) {
            if (data.containsKey(key))
                data.put(key, data.get(key) + "\n" + value)
            else
                data.put(key, value)
        }

        /**
         * Tests if a line is a section
         *
         * I.e. starts exolve-
         */
        private fun isSection(line : String?) : Boolean {
            if (line == null || line.length < 7)
                return false
            return "exolve-".equals(line.substring(0, 7), ignoreCase = true)
        }

        /*
         * Check if a line is exactly exolve-end
         */
        private fun isEnd(line : String?) : Boolean {
            return "exolve-end".equals(line, ignoreCase = true)
        }

        private fun isComment(line : String) : Boolean {
            return line.matches("#\\s.*".toRegex())
        }

        @Throws(ExolveFormatException::class, IOException::class)
        private fun chompToStart(input : Source) {
            var line : String?
            while ((input.readLine().also { line = it }) != null) {
                if ("exolve-begin".equals(line!!.trim(), ignoreCase = true))
                    return
            }
            throw ExolveFormatException("No start tag found")
        }

        @Throws(ExolveFormatException::class)
        private fun readPuzzleFromData(
            data : Map<String, String>,
        ) : Puzzle {
            val exolve3d = data.get("exolve-3d")
            if (exolve3d != null && exolve3d.length > 0) {
                throw ExolveFormatException("3D puzzles not supported.")
            }

            val boxes : ExolveBoxes = getBoxes(data)
            val builder = PuzzleBuilder(boxes.boxes)
            // may have numbers adjusted while adding clues
            builder.autoNumberBoxes(boxes.skipNumberCells)
            addMeta(data, builder)
            val clueData : ExolveClueLists = getClueData(data, builder.height)
            addClues(data, clueData, builder)
            reverseNumbers(data, builder)
            addCompletion(data, clueData, builder)
            addColors(data, clueData, builder)

            return builder.puzzle
        }

        /**
         * Add color info to grid
         *
         * Assumes clueData has expanded zones. For now, ignore color scheme
         * options like color-cell, color-circle and so on. Partly due to
         * fatigue, and partly because the color- options mainly cover
         * interface components, which are a bit out of scope. So stick to
         * the "default" color scheme.
         */
        private fun addColors(
            data : Map<String, String>,
            clueData : ExolveClueLists,
            builder : PuzzleBuilder,
        ) {
            // sigh
            var colors = data.get("exolve-color")
            val colorsU = data.get("exolve-colour")
            if (colors == null || colors.isEmpty())
                colors = colorsU
            else
                colors += "\n" + colorsU
            if (colors == null || colors.isEmpty())
                return

            val acrossZones : MutableMap<String, Zone> = mutableMapOf()
            val downZones : MutableMap<String, Zone> = mutableMapOf()

            for (clue in clueData.across.clues) {
                clue.number?.let {
                    acrossZones.put(it, clue.zone ?: Zone())
                }
            }
            for (clue in clueData.down.clues) {
                clue.number?.let {
                    downZones.put(it, clue.zone ?: Zone())
                }
            }

            for (
                colorLine in colors.split("\n".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            ) {
                val args = colorLine.split("\\s+".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()

                // find color first
                var color = -1
                for (arg in args) {
                    color = parseHtmlColor(arg)
                    if (color >= 0)
                        break
                }

                if (color >= 0) {
                    for (arg in args) {
                        if (parseHtmlColor(arg) >= 0)
                            continue

                        try {
                            val pos = ExolveClue.Companion.parsePosition(
                                arg,
                                builder.height,
                            )
                            colorPosition(pos, color, builder)
                        } catch (e : ExolveFormatException) {
                            // not a position, try a clue
                            // one of several different formats, again...
                            if (arg.startsWith("a") || arg.startsWith("A")) {
                                val number = arg.substring(1)
                                colorZone(
                                    acrossZones.get(number),
                                    color,
                                    builder,
                                )
                            } else if (arg.endsWith("a") || arg.endsWith("A")) {
                                val number = arg.substring(0, arg.length - 1)
                                colorZone(
                                    acrossZones.get(number),
                                    color,
                                    builder,
                                )
                            } else if (
                                arg.startsWith("d") || arg.startsWith("D")
                            ) {
                                val number = arg.substring(1)
                                colorZone(downZones.get(number), color, builder)
                            } else if (arg.endsWith("d") || arg.endsWith("D")) {
                                val number = arg.substring(0, arg.length - 1)
                                colorZone(downZones.get(number), color, builder)
                            }
                        }
                    }
                }
            }
        }

        private fun colorZone(
            zone : Zone?,
            color : Int,
            builder : PuzzleBuilder,
        ) {
            if (zone != null) {
                for (pos in zone)
                    colorPosition(pos, color, builder)
            }
        }

        private fun colorPosition(
            pos : Position,
            color : Int,
            builder : PuzzleBuilder,
        ) {
            val box = builder.getBox(pos)
            if (box != null)
                box.color = color
        }

        /**
         * Reverse numbers according to reversals
         */
        @Throws(ExolveFormatException::class)
        private fun reverseNumbers(
            data : Map<String, String>,
            builder : PuzzleBuilder,
        ) {
            // need to move numbers by reversal
            for (rev in getReversals(data, builder.height)) {
                val start = builder.getBox(rev.start)
                val end = builder.getBox(rev.end)
                if (start == null || end == null)
                    continue

                val numberStart = start.clueNumber
                val numberEnd = end.clueNumber

                // could try to switch middle cells too, but spec doesn't
                // say that
                start.clueNumber = numberEnd
                end.clueNumber = numberStart
            }
        }

        /**
         * Height needed for position parsing
         */
        @Throws(ExolveFormatException::class)
        private fun getClueData(
            data : Map<String, String>,
            height : Int,
        ) : ExolveClueLists {
            return ExolveClueLists(
                getClueListData(data.get("exolve-across"), height),
                getClueListData(data.get("exolve-down"), height),
                getClueListData(data.get("exolve-nodir"), height),
            )
        }

        /**
         * Height needed for position parsing
         */
        @Throws(ExolveFormatException::class)
        private fun getClueListData(
            listData : String?,
            height : Int,
        ) : ExolveClueList {
            val clues = mutableListOf<ExolveClue>()

            if (listData == null)
                return ExolveClueList(null, clues)

            val lines = listData.split("\n".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()

            // first line is blank or title
            val title = lines[0].trim()

            for (i in 1..<lines.size) {
                ExolveClue.Companion.parseClueLine(lines[i], height)?.let {
                    clues.add(it)
                }
            }

            return ExolveClueList(title, clues)
        }

        /**
         * Scans clues for annotations and puts into a completion msg
         */
        @Throws(ExolveFormatException::class)
        private fun addCompletion(
            data : Map<String, String>,
            clueData : ExolveClueLists,
            builder : PuzzleBuilder,
        ) {
            val sb = StringBuilder()

            val explanations = data.get("exolve-explanations")
            if (explanations != null)
                sb.append(explanations.trim())

            Companion.addAnnotations(clueData.across, ACROSS_LIST, sb)
            Companion.addAnnotations(clueData.down, DOWN_LIST, sb)
            Companion.addAnnotations(clueData.nodir, NODIR_LIST, sb)

            val questions = data.get("exolve-question")
            if (questions != null) {
                if (sb.length > 0)
                    sb.append("<h2>Questions</h2>")
                for (
                    question in questions.split("\n".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                ) {
                    val end = ExolveClue.Companion.getHintEnd(question)
                    if (end >= 0) {
                        sb.append("<p>")
                        sb.append(question.substring(end).trim())
                        sb.append("</p>")
                    }
                }
            }

            if (sb.length > 0)
                builder.setCompletionMessage(sb.toString())
        }

        /**
         * Add an annotation section to sb
         */
        private fun addAnnotations(
            list : ExolveClueList,
            fallbackTitle : String,
            sb : StringBuilder,
        ) {
            var title = list.title
            if (title == null || title.isEmpty())
                title = fallbackTitle

            var hasAnnotations = false
            for (clue in list.clues) {
                val annot = clue.annotation
                if (annot != null && !annot.isEmpty()) {
                    hasAnnotations = true
                    break
                }
            }

            if (hasAnnotations) {
                sb.append("<h2>" + title + "</h2>")
                for (clue in list.clues) {
                    val annot = clue.annotation
                    if (annot != null && !annot.isEmpty()) {
                        sb.append(
                            "<p>"
                            + clue.number + ". "
                            + clue.annotation
                            + "</p>",
                        )
                    }
                }
            }
        }

        @Throws(ExolveFormatException::class)
        private fun addClues(
            data : Map<String, String>,
            clueData : ExolveClueLists,
            builder : PuzzleBuilder,
        ) {
            expandZones(data, clueData, builder)
            Companion.addClueList(clueData.across, ACROSS_LIST, builder)
            Companion.addClueList(clueData.down, DOWN_LIST, builder)
            Companion.addClueList(clueData.nodir, NODIR_LIST, builder)
        }

        /**
         * Expand all zones in the clue data to the full zones
         *
         * I.e. for across, get cells for the clue.
         * For clues with multiple parts, join them all together
         * Changes clueData.
         */
        @Throws(ExolveFormatException::class)
        private fun expandZones(
            data : Map<String, String>,
            clueData : ExolveClueLists,
            builder : PuzzleBuilder,
        ) {
            // First get a map for each list with the basic zone
            // I.e. a clue that is 1 & 2 will just get the zone for number 1
            // Enter into map using all possible variations of how the clue
            // might be references elsewhere. E.g. 1 and 1a.
            // Afterwards, stitch them all together to fill clueData
            val acrossZones = mutableMapOf<String, Zone>()
            val downZones = mutableMapOf<String, Zone>()
            val nodirZones = mutableMapOf<String, Zone>()

            // reversals shift numbers.
            // E.g. an across reversal of 1.... becomes ....1. However, this
            // now means 1 down starts in a different place, but the zone
            // for 1 across should be 1.... and then reversed.
            // Keeps maps for across and down for clues that should start in
            // a different place once reversals are done.
            val newAcrossStarts = mutableMapOf<String, Position>()
            val newDownStarts = mutableMapOf<String, Position>()

            val reversals = getReversals(data, builder.height)

            for (rev in reversals) {
                // across
                if (rev.start.row == rev.end.row) {
                    builder.getBox(rev.start)?.let { start ->
                        if (start.hasClueNumber()) {
                            start.clueNumber?.let {
                                newDownStarts.put(it, rev.end)
                            }
                        }
                    }
                    builder.getBox(rev.end)?.let { end ->
                        if (end.hasClueNumber()) {
                            end.clueNumber?.let {
                                newDownStarts.put(it, rev.start)
                            }
                        }
                    }
                }
                // down
                if (rev.start.col == rev.end.col) {
                    builder.getBox(rev.start)?.let { start ->
                        if (start.hasClueNumber()) {
                            start.clueNumber?.let {
                                newAcrossStarts.put(it, rev.end)
                            }
                        }
                    }
                    builder.getBox(rev.end)?.let { end ->
                        if (end.hasClueNumber()) {
                            end.clueNumber?.let {
                                newAcrossStarts.put(it, rev.start)
                            }
                        }
                    }
                }
            }

            // bit copy-pasty
            for (clue in clueData.across.clues) {
                val number = clue.number
                var zone = clue.zone
                if (zone == null) {
                    if (newAcrossStarts.containsKey(number)) {
                        zone = builder.getAcrossZone(
                            newAcrossStarts.get(number),
                        )
                    } else {
                        zone = builder.getAcrossZone(number)
                    }
                } else if (zone.size() == 1) {
                    zone = builder.getAcrossZone(zone.getPosition(0))
                } else {
                    // leave zone as is
                }

                applyReversal(zone, reversals)?.let { zone ->
                    number?.let { number ->
                        acrossZones.put(number, zone)
                        for (suffix in ACROSS_SUFFIXES)
                            acrossZones.put(number + suffix, zone)
                    }
                }
            }

            for (clue in clueData.down.clues) {
                val number = clue.number
                var zone = clue.zone
                if (zone == null) {
                    if (newDownStarts.containsKey(number))
                        zone = builder.getDownZone(newDownStarts.get(number))
                    else
                        zone = builder.getDownZone(number)
                } else if (zone.size() == 1) {
                    zone = builder.getDownZone(zone.getPosition(0))
                } else {
                    // leave zone as is
                }

                applyReversal(zone, reversals)?.let { zone ->
                    number?.let { number ->
                        downZones.put(number, zone)
                        for (suffix in DOWN_SUFFIXES)
                            downZones.put(number + suffix, zone)
                    }
                }
            }

            for (clue in clueData.nodir.clues) {
                clue.number?.let { number ->
                    var zone = clue.zone
                    applyReversal(zone, reversals)?.let { zone ->
                        nodirZones.put(number, zone)
                    }
                }
            }

            // now fill in the data, more copy-pasty
            for (clue in clueData.across.clues) {
                val zone = Zone()
                zone.appendZone(acrossZones.get(clue.number))
                val extras = clue.extraNumbers
                if (extras != null) {
                    for (extra in extras) {
                        if (acrossZones.containsKey(extra)) {
                            zone.appendZone(acrossZones.get(extra))
                        } else if (downZones.containsKey(extra)) {
                            zone.appendZone(downZones.get(extra))
                        } else if (nodirZones.containsKey(extra)) {
                            zone.appendZone(nodirZones.get(extra))
                        }
                    }
                }
                clue.zone = zone
            }

            for (clue in clueData.down.clues) {
                val zone = Zone()
                zone.appendZone(downZones.get(clue.number))
                val extras = clue.extraNumbers
                if (extras != null) {
                    for (extra in extras) {
                        if (downZones.containsKey(extra)) {
                            zone.appendZone(downZones.get(extra))
                        } else if (acrossZones.containsKey(extra)) {
                            zone.appendZone(acrossZones.get(extra))
                        } else if (nodirZones.containsKey(extra)) {
                            zone.appendZone(nodirZones.get(extra))
                        }
                    }
                }
                clue.zone = zone
            }

            for (clue in clueData.nodir.clues) {
                val zone = Zone()
                zone.appendZone(nodirZones.get(clue.number))
                val extras = clue.extraNumbers
                if (extras != null) {
                    for (extra in extras) {
                        if (nodirZones.containsKey(extra)) {
                            zone.appendZone(nodirZones.get(extra))
                        } else if (acrossZones.containsKey(extra)) {
                            zone.appendZone(acrossZones.get(extra))
                        } else if (downZones.containsKey(extra)) {
                            zone.appendZone(downZones.get(extra))
                        }
                    }
                }
                clue.zone = zone
            }
        }

        /**
         * Get reversal zones
         *
         * height needed for position parsing
         */
        @Throws(ExolveFormatException::class)
        private fun getReversals(
            data : Map<String, String>,
            height : Int,
        ) : Set<Reversal> {
            val reversals = mutableSetOf<Reversal>()

            val reversalsData = data.get("exolve-reversals")
            if (reversalsData == null)
                return reversals

            for (
                entry in reversalsData.split(" ".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            ) {
                val positions = entry.split("-".toRegex(), limit = 2)
                    .toTypedArray()
                if (positions.size != 2) {
                    throw ExolveFormatException(
                        "Bad entry '" + entry + "' in reversals",
                    )
                }

                val start = ExolveClue.Companion.parsePosition(
                    positions[0],
                    height,
                )
                val end = ExolveClue.Companion.parsePosition(
                    positions[1],
                    height,
                )

                reversals.add(Reversal(start, end))
            }

            return reversals
        }

        /**
         * Reverse zone if in reversals else return orig
         */
        private fun applyReversal(
            zone : Zone?,
            reversals : Set<Reversal>,
        ) : Zone? {
            if (zone == null || zone.isEmpty)
                return zone

            val rev = Reversal(
                zone.getPosition(0),
                zone.getPosition(zone.size() - 1),
            )
            if (reversals.contains(rev)) {
                val reversed = Zone()
                for (i in zone.size() - 1 downTo 0) {
                    reversed.addPosition(zone.getPosition(i))
                }
                return reversed
            } else {
                return zone
            }
        }

        /**
         * Add clues from clue data
         *
         * If a numbered clue does not have the number in its starting cell,
         * the starting cell is given that number.
         *
         * @param list the clue list data from the file, assumes all zones
         * are complete
         * @param fallbackListName title to use if the list doesn't have one
         * @param builder add clues to this
         */
        private fun addClueList(
            list : ExolveClueList,
            fallbackListName : String,
            builder : PuzzleBuilder,
        ) {
            var title = if (list.title == null || list.title.isEmpty())
                fallbackListName
            else
                list.title

            for (clue in list.clues) {
                val index = builder.getNextClueIndex(title)
                val hint = clue.hint
                if ("*" != hint) {
                    val zone = clue.zone
                    val number = clue.number
                    if (
                        zone != null
                        && !zone.isEmpty
                        && number != null
                        && !number.isEmpty()
                    ) {
                        val box = builder.getBox(zone.getPosition(0))
                        if (box != null && !box.hasClueNumber())
                            box.clueNumber = number
                    }

                    builder.addClue(
                        Clue(title, index, number, null, hint, zone)
                    )
                }
            }
        }

        /**
         * Remove one of the suffixes if appears
         *
         * Else return null
         */
        private fun removeSuffixIfFound(
            s : String,
            suffixes : Set<String>,
        ) : String? {
            for (suffix in suffixes) {
                if (s.endsWith(suffix))
                    return s.substring(0, s.length - suffix.length)
            }
            return null
        }

        private fun addMeta(
            data : Map<String, String>,
            builder : PuzzleBuilder,
        ) {
            builder.setTitle(data.get("exolve-title"))
                .setAuthor(data.get("exolve-setter"))
                .setCopyright(data.get("exolve-copyright"))

            addIntro(data, builder)
            addNotes(data, builder)
        }

        private fun addIntro(
            data : Map<String, String>,
            builder : PuzzleBuilder,
        ) {
            var intro = data.get("exolve-preamble")
            if (intro == null) {
                intro = data.get("exolve-prelude")
                if (intro != null)
                    intro = intro.trim()
            } else if (data.containsKey("exolve-prelude")) {
                intro = intro.trim()
                val prelude = data.get("exolve-prelude")
                if (prelude != null)
                    intro += "\n\n" + prelude.trim()
            }
            builder.setIntroMessage(htmlString(intro))
        }

        private fun addNotes(
            data : Map<String, String>,
            builder : PuzzleBuilder,
        ) {
            val notes = StringBuilder()

            val email = data.get("exolve-email")
            if (email != null)
                notes.append(email.trim())
            val credits = data.get("exolve-credits")
            if (credits != null) {
                if (notes.length > 0)
                    notes.append("\n\n")
                notes.append(credits.trim())
            }
            val maker = data.get("exolve-maker")
            if (maker != null) {
                if (notes.length > 0)
                    notes.append("\n\n")
                notes.append(maker.trim())
            }
            val questions = data.get("exolve-question")
            if (questions != null) {
                for (
                    question in questions.split("\n".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                ) {
                    if (notes.length > 0)
                        notes.append("\n\n")
                    val end = ExolveClue.Companion.getHintEnd(question)
                    if (end < 0) {
                        notes.append(ExolveClue.Companion.parseHint(question))
                    } else {
                        notes.append(
                            ExolveClue.Companion.parseHint(
                                question.substring(0, end),
                            )
                        )
                    }
                }
            }

            if (notes.length > 0)
                builder.setNotes(htmlString(notes.toString()))
        }

        @Throws(ExolveFormatException::class)
        private fun getBoxes(data : Map<String, String>) : ExolveBoxes {
            try {
                val height = data.get("exolve-height")!!.toInt()
                val width = data.get("exolve-width")!!.toInt()
                val grid : String = data.get("exolve-grid")!!

                // note, first line should be blank (content after :)
                val rows = grid.split("\n".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (rows.size - 1 < height) {
                    throw ExolveFormatException(
                        "Not enough grid rows (expected " + height + "): "
                        + grid,
                    )
                }

                var hasDiagramless = false
                val skipNumberCells = mutableSetOf<Position>()
                val boxes = Array(height) { row ->
                    val rowData = rows[row + 1].trim()
                    val cells : List<String> = splitCells(data, rowData)
                    val br : BoxRow = getBoxRow(cells, row, skipNumberCells)
                    hasDiagramless = hasDiagramless or br.hasDiagramless
                    br.boxes
                }

                // no numbers if diagramless
                if (hasDiagramless) {
                    for (row in 0..<height) {
                        for (col in 0..<width) {
                            skipNumberCells.add(Position(row, col))
                        }
                    }
                }

                removeSolutionsIfZeros(boxes)

                return ExolveBoxes(boxes, skipNumberCells)
            } catch (e : NumberFormatException) {
                throw ExolveFormatException("Bad grid: " + e)
            }
        }

        /**
         * Remove solutions if they are all 0
         */
        private fun removeSolutionsIfZeros(boxes : Array<Array<Box?>>) {
            // exit if we find a non-0
            for (row in boxes.indices) {
                for (col in boxes[row].indices) {
                    val box = boxes[row][col]
                    if (box != null && "0" != box.solution)
                        return
                }
            }

            // all 0, remove
            for (row in boxes.indices) {
                for (col in boxes[row].indices) {
                    val box = boxes[row][col]
                    if (box != null)
                        box.solution = null
                }
            }
        }

        /**
         * Split a row of the grid into cells
         *
         * Lots of options in the spec...
         */
        @Throws(ExolveFormatException::class)
        private fun splitCells(
            data : Map<String, String>,
            rowData : String,
        ) : List<String> {
            if (splitOnSpaces(data)) {
                return rowData.split("\\s+".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toList()
            } else {
                val cells = mutableListOf<String>()
                var newCell = true
                // characters are joined to the next if the are an escaped
                // char (e.g. &*) or a unicode high/low surrogate
                var continues = false

                for (i in 0..<rowData.length) {
                    val next = rowData.get(i)

                    if (continues) {
                        val last = cells.size - 1
                        cells.set(last, cells.get(last) + next)
                        newCell = false
                        continues = false
                    } else if (
                        !continues
                        && (next == ESCAPE || Character.isHighSurrogate(next))
                    ) {
                        cells.add("" + next)
                        continues = true
                    } else if (Character.isWhitespace(next)) {
                        newCell = true
                    } else if (newCell) {
                        cells.add("" + next)
                        newCell = false
                    } else if (CELL_MODIFIERS.contains(next)) {
                        val last = cells.size - 1
                        cells.set(last, cells.get(last) + next)
                        newCell = false
                    } else {
                        cells.add("" + next)
                        newCell = false
                    }
                }

                return cells
            }
        }

        /**
         * True if grid must split on spaces
         */
        private fun splitOnSpaces(data : Map<String, String>) : Boolean {
            if (hasOption(data, "rebus-cells"))
                return true

            val lang = data.get("exolve-language")
            if (lang == null)
                return false

            val args = lang.split("\\s+".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            if (args.size >= 3) {
                try {
                    return args[2].toInt() > 1
                } catch (e : NumberFormatException) {
                    // fall through
                }
            } else if (args.size >= 2) {
                // Exolve hardcodes this one case..
                return "Devanagari".equals(args[1], ignoreCase = true)
            }

            return false
        }

        /**
         * Convert a list of cell data into boxes
         *
         * @param cells list of cell strings to parse
         * @param row the row number (0 based)
         * @param skipNumberCells add all positions marked ~ to this set
         */
        @Throws(ExolveFormatException::class)
        private fun getBoxRow(
            cells : List<String>,
            row : Int,
            skipNumberCells : MutableSet<Position>,
        ) : BoxRow {
            val boxes = arrayOfNulls<Box?>(cells.size)

            var hasDiagramless = false

            for (col in cells.indices) {
                var cell = cells.get(col)
                if ("." != cell) {
                    val box = Box()

                    // plan remove all modifiers until left with solution
                    // only

                    // ignore first it means no solution
                    cell = cell.replace("?", "")

                    // circle
                    if (hasUnescaped(cell, "@")) {
                        box.shape = Box.Shape.CIRCLE
                        cell = removeUnescaped(cell, "@")
                    }

                    // bar
                    if (hasUnescaped(cell, "\\|")) {
                        box.barRight = Box.Bar.SOLID
                        cell = removeUnescaped(cell, "\\|")
                    }
                    if (hasUnescaped(cell, "_")) {
                        box.barBottom = Box.Bar.SOLID
                        cell = removeUnescaped(cell, "_")
                    }
                    if (hasUnescaped(cell, "\\+")) {
                        box.barRight = Box.Bar.SOLID
                        box.barBottom = Box.Bar.SOLID
                        cell = removeUnescaped(cell, "\\+")
                    }

                    // skip number
                    if (hasUnescaped(cell, "~")) {
                        skipNumberCells.add(Position(row, col))
                        cell = removeUnescaped(cell, "~")
                    }

                    var diagramless = false
                    if (hasUnescaped(cell, "\\*")) {
                        diagramless = true
                        hasDiagramless = true
                        cell = removeUnescaped(cell, "\\*")
                    }

                    val prefilled : Boolean = hasUnescaped(cell, "!")
                    cell = removeUnescaped(cell, "!")

                    // remove escaping
                    cell = cell.replace(
                        (ESCAPE.toString() + "(.)").toRegex(),
                        "$1",
                    )

                    if (!cell.isEmpty()) {
                        // "diagramless" -- not really supported, but make 0*
                        // into a cell with an empty solution
                        if (("." == cell || "0" == cell) && diagramless) {
                            box.solution = Box.BLANK
                        } else {
                            box.solution = cell
                            if (prefilled) box.initialValue = cell
                        }
                    }

                    boxes[col] = box
                }
            }

            return BoxRow(boxes, hasDiagramless)
        }

        /**
         * If c is in cell without preceding &
         *
         * @param c char, escaped for regex, to look for
         */
        private fun hasUnescaped(cell : String, c : String?) : Boolean {
            return cell.matches((".*(?<!" + ESCAPE + ")" + c + ".*").toRegex())
        }

        /**
         * Remove first unescaped instance of
         *
         * @param c char, escaped for regex, to remove (e.g. "\\.")
         */
        private fun removeUnescaped(cell : String, c : String) : String {
            return cell.replaceFirst(("(?<!" + ESCAPE + ")" + c).toRegex(), "")
        }

        /**
         * True if a puzzle option is set
         */
        private fun hasOption(
            data : Map<String, String>,
            option : String,
        ) : Boolean {
            val opts = data.get("exolve-option")
            if (opts == null)
                return false
            return opts.contains(option)
        }

        private fun htmlString(s : String?) : String? {
            return HtmlUtil.htmlString(s)
        }
    }
}

