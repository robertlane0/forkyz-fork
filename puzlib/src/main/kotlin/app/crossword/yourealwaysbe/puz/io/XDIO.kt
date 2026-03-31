
package app.crossword.yourealwaysbe.puz.io

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Collections
import java.util.logging.Logger
import java.util.regex.Pattern
import kotlin.math.max
import kotlinx.datetime.LocalDate

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder

/**
 * Parser for .xd
 *
 * Format url:
 * https://github.com/century-arcade/xd/blob/master/doc/xd-format.md
 */
class XDIO : PuzzleParser {
    class XDFormatException(msg : String) : Exception(msg)

    @Throws(IOException::class)
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return readPuzzle(inStream)
    }

    private enum class Section(private val sectionName : String) {
        META("metadata"),
        GRID("grid"),
        CLUES("clues"),
        NOTES("notes"),
        OTHER("");

        fun matches(title : String?) : Boolean {
            if (title == null)
                return false

            if (!isSectionTitle(title))
                return false

            val name = title.substring(SECTION_START.length).trim()

            return this.sectionName.equals(name, ignoreCase = true)
        }

        companion object {
            fun parseTitle(title : String?) : Section {
                for (section in entries) {
                    if (section.matches(title))
                        return section
                }
                return Section.OTHER
            }

            fun isSectionTitle(title : String?) : Boolean {
                return if (title == null)
                    false
                else
                    title.startsWith(SECTION_START)
            }

            /**
             * Peek to see if next characters are the start of a section
             */
            @Throws(IOException::class)
            fun peekIsSectionStart(reader : BufferedReader) : Boolean {
                val len : Int = SECTION_START.length
                val buf = CharArray(len)

                reader.mark(len)
                val numRead = reader.read(buf, 0, len)
                reader.reset()

                if (numRead < len)
                    return false
                else
                    return SECTION_START == String(buf)
            }
        }
    }

    private enum class Special(private val specialName : String) {
        SHADED("shaded"), CIRCLE("circle");

        companion object {
            fun parseSpecial(value : String?) : Special? {
                for (special in entries) {
                    if (special.specialName.equals(value, ignoreCase = true))
                        return special
                }
                return null
            }
        }
    }

    /**
     * Fields may be null
     */
    private class Meta(
        val title : String?,
        val author : String?,
        val copyright : String?,
        val rebus : MutableMap<Char, String>?,
        val date : LocalDate?,
        val special : Special?,
        val extraClueGroups : MutableMap<Char, String>?,
        val description : String?,
        val notes : String?,
    )

    /**
     * Information about clues
     *
     * numberInfo is whole A15 part, including the letter identifying
     * the clue list (because we don't know if it's part of the number
     * or not until we know the cluegroups from meta).
     */
    private class ClueInfo(val numberInfo : String, val hint : String)

    /**
     * match/replaces for clue markup
     */
    private class MatchReplace(val match : String, val replace : String)

    companion object {
        private val LOG
            = Logger.getLogger(XDIO::class.java.getCanonicalName())

        private val ACROSS_LIST = DefaultListNames.ACROSS_LIST
        private val DOWN_LIST = DefaultListNames.DOWN_LIST
        private val ANON_LIST = DefaultListNames.NODIR_LIST

        private val DATE_FORMATTER = LocalDate.Formats.ISO

        private val BLOCKS = setOf('#', '_', '■', '＿')

        private const val SECTION_START = "## "

        private val REF_CLUE_RE = Pattern.compile("[^.]*\\^\\w*:.*")

        private val MARKUPS = arrayOf<MatchReplace>(
            MatchReplace("\\{/([^}]*)/\\}", "<i>$1</i>"),
            MatchReplace("\\{\\*([^}]*)\\*\\}", "<b>$1</b>"),
            MatchReplace("\\{_([^}]*)_\\}", "<u>$1</u>"),
            MatchReplace("\\{-([^}]*)-\\}", "<strike>$1</strike>")
        )
        private val LINEBREAKS = MatchReplace("\\\\", "<br/>")

        @JvmStatic
        @Throws(IOException::class)
        fun readPuzzle(inStream : InputStream) : Puzzle? {
            try {
                val reader = BufferedReader(InputStreamReader(inStream))

                val line = getNextNonBlankLine(reader)

                if (Section.Companion.isSectionTitle(line))
                    return readTitledSections(line, reader)
                else
                    return readImplicitSections(line, reader)
            } catch (e : XDFormatException) {
                LOG.info("Failed to parse XD file: " + e)
                return null
            }
        }

        @Throws(IOException::class, XDFormatException::class)
        private fun readTitledSections(
            firstTitle : String?,
            reader : BufferedReader,
        ) : Puzzle? {
            var title = firstTitle

            var meta : Meta? = null
            var boxes : Array<CharArray>? = null
            var clues : MutableList<ClueInfo>? = null
            var notes : String? = null

            while (title != null) {
                when (Section.Companion.parseTitle(title)) {
                    Section.META -> meta = readMeta(null, reader)
                    Section.GRID -> boxes = readBoxes(reader, false)
                    Section.CLUES -> clues = readClueInfo(reader, false)
                    Section.NOTES -> notes = readNotes(reader, false)
                    Section.OTHER -> {}
                }

                title = getNextNonBlankLine(reader)
            }

            return boxes?.let { boxes ->
                Companion.buildPuzzle(meta, boxes, clues, notes)
            }
        }

        @Throws(IOException::class, XDFormatException::class)
        private fun readImplicitSections(
            firstLine : String?,
            reader : BufferedReader,
        ) : Puzzle {
            val meta : Meta = readMeta(firstLine, reader)
            val boxes = readBoxes(reader, true)
            val clues = readClueInfo(reader, true)
            val notes = readNotes(reader, true)
            return buildPuzzle(meta, boxes, clues, notes)
        }

        /**
         * Read meta section
         *
         * First line is either the first line of the section, or null if
         * the whole section is still in the reader (without the title)
         */
        @Throws(IOException::class, XDFormatException::class)
        private fun readMeta(
            firstLine : String?,
            reader : BufferedReader,
        ) : Meta {
            var title : String? = null
            var author : String? = null
            var copyright : String? = null
            val rebus = mutableMapOf<Char, String>()
            var date : LocalDate? = null
            var special : Special? = null
            val clueGroups : MutableMap<Char, String> = mutableMapOf()
            var description : String? = null
            var notes : String? = null

            var line = firstLine
            if (line == null)
                line = getNextDataLine(reader)

            while (line != null) {
                val keyValue = line.split(":".toRegex(), limit = 2)
                    .toTypedArray()

                if (keyValue.size < 2)
                    throw XDFormatException("Unexpected metadata line: " + line)

                val key = keyValue[0].trim()
                val value = keyValue[1].trim()

                if ("title".equals(key, ignoreCase = true)) {
                    title = parseString(value, true)
                } else if ("author".equals(key, ignoreCase = true)) {
                    author = parseString(value, true)
                } else if ("copyright".equals(key, ignoreCase = true)) {
                    copyright = parseString(value, true)
                } else if ("description".equals(key, ignoreCase = true)) {
                    description = parseString(value, true)
                } else if ("notes".equals(key, ignoreCase = true)) {
                    notes = parseString(value, true)
                } else if ("rebus".equals(key, ignoreCase = true)) {
                    parseLookupTable(value, rebus)
                } else if ("cluegroup".equals(key, ignoreCase = true)) {
                    parseLookupTable(value, clueGroups)
                } else if ("date".equals(key, ignoreCase = true)) {
                    try {
                        date = DATE_FORMATTER.parse(value)
                    } catch (e : IllegalArgumentException) {
                        throw XDFormatException(
                            "Unexpected date format: " + value,
                        )
                    }
                } else if ("special".equals(key, ignoreCase = true)) {
                    special = Special.Companion.parseSpecial(value)
                    if (special == null) {
                        throw XDFormatException(
                            "Unrecognised special value " + value,
                        )
                    }
                }

                line = getNextDataLine(reader)
            }

            return Meta(
                title,
                author,
                copyright,
                rebus,
                date,
                special,
                clueGroups,
                description,
                notes,
            )
        }

        /**
         * Parse the input string into the table
         *
         * Input of form char=value char=value. E.g.
         *
         * a=ABC d=DEF
         */
        @Throws(XDFormatException::class)
        private fun parseLookupTable(
            input : String,
            table : MutableMap<Char, String>,
        ) {
            for (
                entry in input.split("\\s+".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            ) {
                val keyValue = entry.split("=".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (keyValue.size < 2) {
                    throw XDFormatException(
                        "Unexpected table entry " + entry,
                    )
                }
                if (keyValue[0].length != 1) {
                    throw XDFormatException(
                        "Invalid table key: " + keyValue[0],
                    )
                }
                table.put(keyValue[0].get(0), keyValue[1])
            }
        }

        /**
         * Read boxes from file
         *
         * Will return a grid, throws an exception if error
         *
         * @param reader source of data
         * @param chompBlankLines whether to ignore any blank lines
         * appearing before the grid
         * @return matrix of chars for boxes (need meta data to convert to
         * boxes
         */
        @Throws(IOException::class, XDFormatException::class)
        private fun readBoxes(
            reader : BufferedReader,
            chompBlankLines : Boolean,
        ) : Array<CharArray> {
            val lines = mutableListOf<String>()

            var line : String? = if (chompBlankLines)
                getNextNonBlankLine(reader)
            else
                getNextDataLine(reader)

            if (line == null)
                throw XDFormatException("No grid!")

            var maxWidth = 0

            while (line != null) {
                line = line.trim()
                maxWidth = max(line.length, maxWidth)
                lines.add(line)
                line = getNextDataLine(reader)
            }

            val boxes = Array<CharArray>(lines.size) { row ->
                line = lines.get(row)
                CharArray(maxWidth) { col ->
                    if (col >= line.length)
                        '_'
                    else
                        line.get(col)
                }
            }

            return boxes
        }

        /**
         * Set circle/highlight if letter is special cell
         */
        private fun setSpecial(meta : Meta?, cell : Char, box : Box) {
            var special = if (meta == null) null else meta.special
            if (special == null)
                special = Special.CIRCLE

            if (Character.isLowerCase(cell)) {
                when (special) {
                    Special.CIRCLE -> box.shape = Box.Shape.CIRCLE
                    Special.SHADED -> box.color = IPuzIO.HIGHLIGHT_COLOR
                }
            }
        }

        /**
         * Read clues from file
         *
         * @param reader source of data
         * @param chompBlankLines whether to ignore any blank lines
         * appearing before the grid
         * @return List of found clues
         */
        @Throws(IOException::class, XDFormatException::class)
        private fun readClueInfo(
            reader : BufferedReader,
            chompBlankLines : Boolean,
        ) : MutableList<ClueInfo> {
            val clues = mutableListOf<ClueInfo>()

            var line : String? = if (chompBlankLines)
                getNextNonBlankLine(reader)
            else
                getNextDataLine(reader)

            if (line == null)
                return clues

            while (line != null) {
                line = line.trim()

                if (REF_CLUE_RE.matcher(line).matches()) {
                    line = getNextDataLine(reader)
                    continue
                }

                val numSplit = line.split("\\.".toRegex(), limit = 2)
                    .toTypedArray()
                if (numSplit.size < 2) {
                    throw XDFormatException(
                        "No number end found in clue: " + line,
                    )
                }

                val numberInfo = numSplit[0]
                val hintAns = numSplit[1]

                val ansStart = hintAns.lastIndexOf("~")
                val hint = parseString(
                    if (ansStart < 0)
                        hintAns.trim()
                    else
                        hintAns.substring(0, ansStart).trim(),
                    true,
                )

                clues.add(ClueInfo(numberInfo, hint))

                line = getNextDataLine(reader)
            }

            return clues
        }

        /**
         * Read notes from file
         *
         * @param reader source of data
         * @param chompBlankLines whether to ignore any blank lines
         * appearing before the grid
         * @return notes in html
         */
        @Throws(IOException::class, XDFormatException::class)
        private fun readNotes(
            reader : BufferedReader,
            chompBlankLines : Boolean,
        ) : String? {
            var line = if (chompBlankLines)
                getNextNonBlankLine(reader)
            else
                getNextDataLine(reader)

            if (line == null)
                return null

            val sb = StringBuilder()

            while (line != null) {
                sb.append(line.trim() + "<br/>")

                if (Section.Companion.peekIsSectionStart(reader))
                    line = null
                else
                    line = reader.readLine()
            }

            // remove trailing blank lines
            return sb.toString().replace("(<br/>)*$".toRegex(), "")
        }

        /**
         * Build puzzle from gathered info
         */
        @Throws(XDFormatException::class)
        private fun buildPuzzle(
            meta : Meta?,
            boxChars : Array<CharArray>,
            clues : List<ClueInfo>?,
            notes : String?,
        ) : Puzzle {
            val boxes : Array<Array<Box?>> = buildBoxes(boxChars, meta)
            val builder = PuzzleBuilder(boxes)

            try {
                builder.autoNumberBoxes()
            } catch (e : IllegalArgumentException) {
                throw XDFormatException("Could not number boxes")
            }

            if (meta != null) {
                builder.setTitle(meta.title)
                    .setAuthor(meta.author)
                    .setCopyright(meta.copyright)
                    .setDate(meta.date)
                    .setNotes(
                        buildNotes(
                            meta.description,
                            meta.notes,
                            notes,
                        ),
                    )
            }

            buildClues(clues, meta, builder)

            return builder.puzzle
        }

        /**
         * Add clue infos as clues to puzzle builder
         */
        private fun buildClues(
            clues : List<ClueInfo>?,
            meta : Meta?,
            builder : PuzzleBuilder,
        ) {
            if (clues == null)
                return

            val clueGroups = if (meta == null) null else meta.extraClueGroups

            for (clue in clues) {
                val numberInfo = clue.numberInfo
                val hint = clue.hint

                if (numberInfo.isEmpty()) {
                    val index = builder.getNextClueIndex(ANON_LIST)
                    builder.addClue(
                        Clue(ANON_LIST, index, null, null, hint, null),
                    )
                } else {
                    val listID = numberInfo.get(0)
                    val number = numberInfo.substring(1)

                    if (listID == 'A') {
                        builder.addAcrossClue(ACROSS_LIST, number, hint)
                    } else if (listID == 'D') {
                        builder.addDownClue(DOWN_LIST, number, hint)
                    } else if (
                        clueGroups != null
                        && clueGroups.containsKey(listID)
                    ) {
                        val listName = clueGroups.get(listID)!!
                        val index = builder.getNextClueIndex(listName)
                        builder.addClue(
                            Clue(listName, index, number, null, hint, null),
                        )
                    } else {
                        // use full number info as number since the first
                        // letter isn't a list
                        val index = builder.getNextClueIndex(ANON_LIST)
                        builder.addClue(
                            Clue(
                                ANON_LIST,
                                index,
                                numberInfo,
                                null,
                                hint,
                                null,
                            ),
                        )
                    }
                }
            }
        }

        /**
         * Make a single note string out of array of possible sources
         *
         * If just one non-null, then return it. Else put into paragraph
         * blocks.
         */
        private fun buildNotes(vararg notes : String?) : String {
            // set to only non null if only one
            var nonNull : String? = null
            for (note in notes) {
                if (note != null && !note.isEmpty()) {
                    if (nonNull == null) {
                        nonNull = note
                    } else {
                        nonNull = null
                        break
                    }
                }
            }

            if (nonNull != null)
                return nonNull

            // else make paragraphs
            val sb = StringBuilder()
            for (note in notes) {
                if (note != null && !note.isEmpty())
                    sb.append("<p>" + note + "</p>")
            }

            return sb.toString()
        }

        /**
         * Convert character data for boxes into boxes
         */
        private fun buildBoxes(
            boxData : Array<CharArray>,
            meta : Meta?,
        ) : Array<Array<Box?>> {
            val rebus = if (meta == null) null else meta.rebus

            val boxes = Array<Array<Box?>>(boxData.size) { row ->
                Array<Box?>(boxData[row].size) { col ->
                    val cell = boxData[row][col]
                    if (!BLOCKS.contains(cell)) {
                        val box = Box()

                        setSpecial(meta, cell, box)

                        if (rebus != null && rebus.containsKey(cell)) {
                            box.solution = rebus.get(cell)
                        } else if (cell != '.') {
                            box.setSolution(cell.uppercaseChar())
                        }

                        box
                    } else {
                        null
                    }
                }
            }
            return boxes
        }

        /**
         * Parse a markup XD string into HTML
         *
         * Puts <b/u/strike> tags around XD equiv and adds linebreaks in
         * html if you say so;
         */
        private fun parseString(s : String, doLineBreaks : Boolean) : String {
            var s = s
            for (mr in MARKUPS)
                s = s.replace(mr.match.toRegex(), mr.replace)
            if (doLineBreaks) {
                s = s.replace(
                    LINEBREAKS.match.toRegex(),
                    LINEBREAKS.replace,
                )
            }
            return s
        }

        /**
         * Reads the next non-blank line from the reader
         *
         * Returns null if the end of the file or section is reached.
         * Only chomps two blank lines at end of section, may leave further
         * blank lines.
         *
         * If finds a ## to mark a new section, does not consume the line
         * from the stream.
         */
        @Throws(IOException::class, XDFormatException::class)
        private fun getNextDataLine(reader : BufferedReader) : String? {
            if (Section.Companion.peekIsSectionStart(reader))
                return null

            var line = reader.readLine()
            if (line == null) return null

            line = line.trim()
            if (!line.isEmpty())
                return line

            if (Section.Companion.peekIsSectionStart(reader))
                return null

            line = reader.readLine()
            if (line == null)
                return null

            line = line.trim()

            return if (line.isEmpty()) null else line
        }

        /**
         * Returns null if EOF reached
         */
        @Throws(IOException::class, XDFormatException::class)
        private fun getNextNonBlankLine(reader : BufferedReader) : String? {
            while (true) {
                var line = reader.readLine()
                if (line == null)
                    return null

                line = line.trim()
                if (!line.isEmpty())
                    return line
            }
        }
    }
}

