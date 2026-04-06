package app.crossword.yourealwaysbe.puz.io

import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.util.Arrays
import java.util.Collections
import java.util.LinkedList
import java.util.Locale
import java.util.function.Consumer
import java.util.logging.Logger
import kotlin.math.log10
import kotlin.math.min
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONWriter

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Note
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.PuzImage
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.PuzzleKind
import app.crossword.yourealwaysbe.puz.Zone
import app.crossword.yourealwaysbe.puz.util.HtmlUtil.htmlString
import app.crossword.yourealwaysbe.puz.util.HtmlUtil.parseHtmlColor
import app.crossword.yourealwaysbe.puz.util.HtmlUtil.unHtmlString
import app.crossword.yourealwaysbe.puz.util.JSONParser.parse
import app.crossword.yourealwaysbe.puz.util.PuzzleUtils

/**
 * Read IPuz from a stream.
 *
 * Throws an exception if the puzzle is not in IPuz format, or is in a format
 * not supported.
 *
 * Many fields are ignored. A rough estimate has been made as to whether
 * a puzzle is playable if that field is ignored. If it may be, then the
 * puzzle will load without it. If it's probably a problem, it will
 * raise an IPuzFormatException.
 *
 * Currently checksums of solutions are not supported. The puzzle will
 * still be playable. The Puzzle class has a checksum field that is not
 * used, and corresponds to Across Lite checksums.
 *
 * Note: an error in the first version of this code means dates were
 * written in British (dd/mm/yyyy) format, not US (mm/dd/yyyy). The
 * IO_VERSION checks were added to still read old files correctly.
 *
 * Not using internationalisation for list names as these should be set
 * in file.
 *
 * http://www.ipuz.org/
 */
class IPuzIO : AbstractJSONIO() {
    class IPuzFormatException(msg : String?) : Exception(msg)

    @Throws(Exception::class)
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return readPuzzle(inStream)
    }

    /**
     * Extend JSONWriter with a write method to add custom formatting
     */
    private class FormatableJSONWriter(
        writer : Appendable,
    ) : JSONWriter(writer) {
        /**
         * Writes the field if it is not null with trailing new line
         * @return self for chaining
         */
        @Throws(IOException::class)
        fun keyValueNonNull(
            field : String,
            value : Any?,
        ) : FormatableJSONWriter {
            keyValueNonNull(0, field, value)
            return this
        }

        /**
         * Writes the field if not null with trailing new line and indent
         * @return self for chaining
         */
        @Throws(IOException::class)
        fun keyValueNonNull(
            indentSteps : Int,
            field : String,
            value : Any?,
        ) : FormatableJSONWriter {
            if (value != null) {
                indent(indentSteps)
                key(field)
                value(value)
                newLine()
            }
            return this
        }

        @Throws(IOException::class)
        fun newLine() : FormatableJSONWriter {
            writer.append("\n")
            return this
        }

        @Throws(IOException::class)
        fun indent(count : Int) : FormatableJSONWriter {
            for (i in 0..<count)
                writer.append("\t")
            return this
        }
    }

    companion object {
        private val LOG
            = Logger.getLogger(IPuzIO::class.java.getCanonicalName())

        // version 1 was not tagged in file
        private const val IO_VERSION = 3

        private val WRITE_CHARSET : Charset = Charset.forName("UTF-8")

        private const val FIELD_VERSION = "version"
        private const val FIELD_KIND = "kind"

        private const val FIELD_AUTHOR = "author"
        private const val FIELD_COPYRIGHT = "copyright"
        private const val FIELD_DATE = "date"
        private const val FIELD_INTRO = "intro"
        private const val FIELD_EXPLANATION = "explanation"
        private const val FIELD_NOTES = "notes"
        private const val FIELD_PUBLISHER = "publisher"
        private const val FIELD_TITLE = "title"
        private const val FIELD_URL = "url"

        private const val FIELD_DIMENSIONS = "dimensions"
        private const val FIELD_WIDTH = "width"
        private const val FIELD_HEIGHT = "height"

        private const val FIELD_PUZZLE = "puzzle"
        private const val FIELD_SAVED = "saved"
        private const val FIELD_SOLUTION = "solution"
        private const val FIELD_SHOW_ENUMERATIONS = "showenumerations"
        private const val FIELD_CLUES = "clues"
        private const val FIELD_NAMED_STYLES = "styles"

        private const val FIELD_CELL = "cell"
        private const val FIELD_VALUE = "value"
        private const val FIELD_STYLE = "style"
        private const val FIELD_SHAPE_BG = "shapebg"
        private const val FIELD_COLOR = "color"
        private const val FIELD_TEXT_COLOR = "colortext"
        private const val FIELD_BAR_COLOR = "colorbar"
        private const val HEX_CODE_LEN = 6
        private val HEX_COLOR_FORMAT = "%0" + HEX_CODE_LEN + "X"
        private const val FIELD_BARRED = "barred"
        private const val FIELD_BARRED_DASHED = "dashed"
        private const val FIELD_BARRED_DOTTED = "dotted"
        private const val FIELD_HIGHLIGHT = "highlight"

        private val SHAPE_BGS = mapOf(
            "circle" to Box.Shape.CIRCLE,
            "arrowleft" to Box.Shape.ARROW_LEFT,
            "arrowright" to Box.Shape.ARROW_RIGHT,
            "arrowup" to Box.Shape.ARROW_UP,
            "arrowdown" to Box.Shape.ARROW_DOWN,
            "triangleleft" to Box.Shape.TRIANGLE_LEFT,
            "triangleright" to Box.Shape.TRIANGLE_RIGHT,
            "triangleup" to Box.Shape.TRIANGLE_UP,
            "triangledown" to Box.Shape.TRIANGLE_DOWN,
            "diamond" to Box.Shape.DIAMOND,
            "club" to Box.Shape.CLUB,
            "heart" to Box.Shape.HEART,
            "spade" to Box.Shape.SPADE,
            "star" to Box.Shape.STAR,
            "square" to Box.Shape.SQUARE,
            "rhombus" to Box.Shape.RHOMBUS,
            "/" to Box.Shape.FORWARD_SLASH,
            "\\" to Box.Shape.BACK_SLASH,
            "x" to Box.Shape.X,
        )

        private val SHAPE_BGS_REV = mapOf(
            Box.Shape.CIRCLE to "circle",
            Box.Shape.ARROW_LEFT to "arrow-left",
            Box.Shape.ARROW_RIGHT to "arrow-right",
            Box.Shape.ARROW_UP to "arrow-up",
            Box.Shape.ARROW_DOWN to "arrow-down",
            Box.Shape.TRIANGLE_LEFT to "triangle-left",
            Box.Shape.TRIANGLE_RIGHT to "triangle-right",
            Box.Shape.TRIANGLE_UP to "triangle-up",
            Box.Shape.TRIANGLE_DOWN to "triangle-down",
            Box.Shape.DIAMOND to "diamond",
            Box.Shape.CLUB to "club",
            Box.Shape.HEART to "heart",
            Box.Shape.SPADE to "spade",
            Box.Shape.STAR to "star",
            Box.Shape.SQUARE to "square",
            Box.Shape.RHOMBUS to "rhombus",
            Box.Shape.FORWARD_SLASH to "/",
            Box.Shape.BACK_SLASH to "\\",
            Box.Shape.X to "X",
        )

        private const val BARRED_TOP = 'T'
        private const val BARRED_BOTTOM = 'B'
        private const val BARRED_LEFT = 'L'
        private const val BARRED_RIGHT = 'R'
        private const val FIELD_MARK = "mark"
        private const val FIELD_MARK_TOP_LEFT = "TL"
        private const val FIELD_MARK_TOP = "T"
        private const val FIELD_MARK_TOP_RIGHT = "TR"
        private const val FIELD_MARK_LEFT = "L"
        private const val FIELD_MARK_CENTER = "C"
        private const val FIELD_MARK_RIGHT = "R"
        private const val FIELD_MARK_BOTTOM_LEFT = "BL"
        private const val FIELD_MARK_BOTTOM = "B"
        private const val FIELD_MARK_BOTTOM_RIGHT = "BR"
        private const val FIELD_LABEL = "label"

        private const val FIELD_CLUES_ACROSS = "Across"
        private const val FIELD_CLUES_DOWN = "Down"

        private const val FIELD_CLUE_NUMBER = "number"
        private const val FIELD_CLUE_LABEL = "label"
        private const val FIELD_CLUE_INDEX = "index"
        private const val FIELD_CLUE_NUMBERS = "numbers"
        private const val FIELD_CLUE_HINT = "clue"
        private const val FIELD_CLUE_CONTINUED = "continued"
        private const val FIELD_CLUE_REFERENCES = "references"
        private const val FIELD_CLUE_DIRECTION = "direction"
        private const val FIELD_CLUE_CELLS = "cells"

        private const val FIELD_ENUMERATION = "enumeration"

        private const val FIELD_BLOCK = "block"
        private const val FIELD_EMPTY = "empty"

        private const val DEFAULT_BLOCK = "#"
        private const val DEFAULT_EMPTY_READ = "0"
        private const val DEFAULT_EMPTY_WRITE = 0

        private const val WRITE_VERSION = "http://ipuz.org/v2"
        private const val WRITE_KIND = "http://ipuz.org/crossword#1"
        private const val WRITE_KIND_ACROSTIC = "http://ipuz.org/acrostic#1"
        private const val ACROSTIC_PREFIX = "http://ipuz.org/acrostic"

        private val SUPPORTED_VERSIONS = arrayOf(
            "http://ipuz.org/v1",
            WRITE_VERSION,
        )
        private val SUPPORTED_KIND_PREFIXES = arrayOf(
            WRITE_KIND,
            "http://ipuz.org/crossword",
            ACROSTIC_PREFIX,
        )

        private const val EXT_NAMESPACE = "app.crossword.yourealwaysbe"

        private val FIELD_EXT_SUPPORT_URL
            = getQualifiedExtensionName("supporturl")
        private val FIELD_EXT_SHARE_URL
            = getQualifiedExtensionName("shareurl")
        private val FIELD_EXT_PLAY_DATA
            = getQualifiedExtensionName("playdata")
        private val FIELD_EXT_IO_VERSION
            = getQualifiedExtensionName("ioversion")
        private val FIELD_EXT_IMAGES
            = getQualifiedExtensionName("images")
        private val FIELD_EXT_IMAGE_URL
            = getQualifiedExtensionName("url")
        private val FIELD_EXT_IMAGE_ROW
            = getQualifiedExtensionName("row")
        private val FIELD_EXT_IMAGE_COL
            = getQualifiedExtensionName("col")
        private val FIELD_EXT_IMAGE_WIDTH
            = getQualifiedExtensionName("width")
        private val FIELD_EXT_IMAGE_HEIGHT
            = getQualifiedExtensionName("height")
        private val FIELD_EXT_PINNED_CLUE_ID
            = getQualifiedExtensionName("pinnedClueID")
        private val FIELD_EXT_SEPARATORS
            = getQualifiedExtensionName("separators")

        private const val FIELD_VOLATILE = "volatile"
        private const val FIELD_IS_VOLATILE = "*"
        private const val FIELD_IS_NOT_VOLATILE = ""

        private val VOLATILE_EXTENSIONS = arrayOf(FIELD_EXT_PLAY_DATA)

        private val NON_VOLATILE_EXTENSIONS = arrayOf(
            FIELD_EXT_SUPPORT_URL,
            FIELD_EXT_SHARE_URL,
            FIELD_EXT_IO_VERSION,
            FIELD_EXT_IMAGES,
            FIELD_EXT_SEPARATORS,
        )

        private const val FIELD_BOX_EXTRAS = "boxextras"
        private const val FIELD_BOX_CHEATED = "cheated"
        private const val FIELD_BOX_FLAGGED = "flagged"
        private const val FIELD_BOX_FLAG_COLOR = "flagcolor"
        private const val FIELD_BOX_RESPONDER = "responder"
        private const val FIELD_COMPLETION_TIME = "completiontime"
        private const val FIELD_PCNT_COMPLETE = "percentcomplete"
        private const val FIELD_PCNT_FILLED = "percentfilled"
        private const val FIELD_POSITION = "position"
        private const val FIELD_POSITION_ROW = "row"
        private const val FIELD_POSITION_COL = "col"
        private const val FIELD_POSITION_ACROSS = "across"
        private const val FIELD_CLUEID = "clueid"
        private const val FIELD_CLUE_HISTORY = "cluehistory"
        private const val FIELD_CLUE_ACROSS = "across"
        private const val FIELD_CLUE_LISTNAME = "listname"
        private const val FIELD_CLUE_NOTES = "cluenotes"
        private const val FIELD_PLAYER_NOTE = "playernote"
        private const val FIELD_CLUE_NOTE_CLUE = "clue"
        private const val FIELD_NOTE_SCRATCH = "scratch"
        private const val FIELD_NOTE_TEXT = "text"
        private const val FIELD_NOTE_ANAGRAM_SRC = "anagramsource"
        private const val FIELD_NOTE_ANAGRAM_SOL = "anagramsolution"
        private const val FIELD_FLAGGED_CLUES = "flaggedclues"
        private const val FIELD_RATING = "rating"
        private const val FIELD_CLUE_SEPARATORS = "separators"

        private val DATE_FORMATTER_V1 = LocalDate.Format {
            day(Padding.ZERO)
            char('/')
            monthNumber(Padding.ZERO)
            char('/')
            year(Padding.ZERO)
        }
        private val DATE_FORMATTER = LocalDate.Format {
            monthNumber(Padding.ZERO)
            char('/')
            day(Padding.ZERO)
            char('/')
            year(Padding.ZERO)
        }

        // for puzzles that forget to double digits
        private val DATE_FORMATTER_SINGLES = LocalDate.Format {
            monthNumber(Padding.NONE)
            char('/')
            day(Padding.NONE)
            char('/')
            year(Padding.ZERO)
        }

        private const val FIELD_CLUES_CLUES = "Clues"
        private const val FIELD_CLUES_ZONES = "Zones"
        private val FIELD_CLUES_DIRECTIONS = setOf(
            "Across",
            "Down",
            "Diagonal",
            "Diagonal Up",
            "Diagonal Down Left",
            "Diagonal Up Left",
            FIELD_CLUES_ZONES,
            FIELD_CLUES_CLUES,
        )

        private const val NULL_CLUE = "-"

        // before we started supporting any list name, we used across/down
        // and a boolean to tell the difference.
        private const val OLD_ACROSS_LIST_NAME = "Across"
        private const val OLD_DOWN_LIST_NAME = "Down"

        private const val ACROSTIC_BOARD_CLUE_LIST = "Quote"
        private const val ACROSTIC_BOARD_CLUE_HINT = "Quote"

        private val DEFINED_COLORS = intArrayOf(
            parseHtmlColor("#000000"),  // 0 - black
            parseHtmlColor("#d9d1f1"),  // 1 - light purple
            parseHtmlColor("#afe1af"),  // 2 - light green
            parseHtmlColor("#fffaa0"),  // 3 - light yellow
            parseHtmlColor("#add8e6"),  // 4 - light blue
            parseHtmlColor("#faa0a0"),  // 5 - light red
            parseHtmlColor("#bdffff"),  // 6 - light cyan
            parseHtmlColor("#f5f5dc"),  // 7 - off white
            parseHtmlColor("#cccccc"),  // 8 - light black
            parseHtmlColor("#cf9fff"),  // 9 - dark purple
            parseHtmlColor("#50c878"),  // 10 - dark green
            parseHtmlColor("#fafa33"),  // 11 - dark yellow
            parseHtmlColor("#4169e1"),  // 12 - dark blue
            parseHtmlColor("#fa8072"),  // 13 - dark red
            parseHtmlColor("#00ffff"),  // 14 - cyan
            parseHtmlColor("#ebca9a"),  // 15 - dark off white
        )
        private val DEFINED_COLOURS_MAX_DIGITS
            = (log10(DEFINED_COLORS.size.toDouble()).toInt()) + 1
        val HIGHLIGHT_COLOR : Int = DEFINED_COLORS[3]

        @JvmStatic
        @Throws(IOException::class)
        fun readPuzzle(inStream : InputStream) : Puzzle? {
            try {
                val json = parse(inStream)

                checkIPuzVersion(json)
                val kind : PuzzleKind? = getPuzKind(json)

                val builder = PuzzleBuilder(readBoxes(json))

                if (kind != null)
                    builder.setKind(kind)

                readMetaData(json, builder)
                readClues(json, builder)
                readExtensions(json, builder)

                if (PuzzleKind.ACROSTIC == kind)
                    ensureBoardClue(builder)

                return builder.puzzle
            } catch (e : IPuzFormatException) {
                LOG.severe("Could not read IPuz file: " + e)
                return null
            } catch (e : JSONException) {
                LOG.severe("Could not read IPuz file: " + e)
                return null
            }
        }

        @Throws(IPuzFormatException::class)
        private fun checkIPuzVersion(puzJson : JSONObject) {
            val version = puzJson.getString(FIELD_VERSION)
            for (supportedVersion in SUPPORTED_VERSIONS) {
                if (supportedVersion.equals(version, ignoreCase = true))
                    return
            }
            throw IPuzFormatException("Unsupported IPuz version: " + version)
        }

        /**
         * Checks recognized ipuz kind
         *
         * @return PuzzleKind the ipuz kind matches
         */
        @Throws(IPuzFormatException::class)
        private fun getPuzKind(puzJson : JSONObject) : PuzzleKind {
            var puzKind : PuzzleKind? = null

            // Either acrostic or crossword. If we find acrostic, we're
            // done. Otherwise, assume crossword unless acrostic comes up
            // later
            val kinds = puzJson.getJSONArray(FIELD_KIND)
            for (i in 0..<kinds.length()) {
                val kind = kinds.getString(i).lowercase(Locale.getDefault())

                if (kind.startsWith(ACROSTIC_PREFIX))
                    return PuzzleKind.ACROSTIC

                for (supportedKindPrefix in SUPPORTED_KIND_PREFIXES) {
                    if (kind.startsWith(supportedKindPrefix)) {
                        puzKind = PuzzleKind.CROSSWORD
                    }
                }
            }

            if (puzKind == null)
                throw IPuzFormatException("No supported IPuz kind: " + kinds)

            return puzKind
        }

        /**
         * Read puzzle info from puzJson into puz
         *
         * Meta-data stuff, like title, copyright, etc.
         */
        @Throws(IPuzFormatException::class)
        private fun readMetaData(
            puzJson : JSONObject,
            builder : PuzzleBuilder,
        ) {
            builder.setTitle(optStringNull(puzJson, FIELD_TITLE))
                .setAuthor(optStringNull(puzJson, FIELD_AUTHOR))
                .setCopyright(optStringNull(puzJson, FIELD_COPYRIGHT))
                .setIntroMessage(optStringNull(puzJson, FIELD_INTRO))
                .setNotes(optStringNull(puzJson, FIELD_NOTES))
                .setCompletionMessage(optStringNull(puzJson, FIELD_EXPLANATION))
                .setSourceUrl(optStringNull(puzJson, FIELD_URL))
                .setSource(optStringNull(puzJson, FIELD_PUBLISHER))
                .setDate(parseDate(puzJson))
        }

        /**
         * Parse date field (depends on IO version!)
         *
         * I put day/month the wrong way around in initial version :/
         *
         * @return null if no date or date doesn't match known format
         */
        @Throws(IPuzFormatException::class)
        private fun parseDate(puzJson : JSONObject) : LocalDate? {
            val date = optStringNull(puzJson, FIELD_DATE)
            if (date == null)
                return null

            try {
                if (getIOVersion(puzJson) == 1)
                    return DATE_FORMATTER_V1.parse(date)
                else
                    return DATE_FORMATTER.parse(date)
            } catch (e : IllegalArgumentException) {
                try {
                    return DATE_FORMATTER_SINGLES.parse(date)
                } catch (e2 : IllegalArgumentException) {
                    return null
                }
            }
        }

        /**
         * The Forkyz IO version
         *
         * 0 if no Forkyz data specified, otherwise 1 or above
         */
        private fun getIOVersion(puzJson : JSONObject) : Int {
            if (puzJson.has(FIELD_EXT_IO_VERSION))
                return puzJson.getInt(FIELD_EXT_IO_VERSION)
            else
                return if (puzJson.has(FIELD_EXT_PLAY_DATA)) 1 else 0
        }

        /**
         * Get optional field from JSON
         *
         * Strips any HTML elements from it.
         */
        private fun unHtmlOptString(
            json : JSONObject,
            field : String,
        ) : String? {
            val value = optStringNull(json, field)

            if (value == null)
                return null

            return unHtmlString(value)
        }

        /**
         * Read fully populated Box objects from JSON
         *
         * Will always return some boxes, or throw a JSON exception
         */
        @Throws(IPuzFormatException::class)
        private fun readBoxes(puzJson : JSONObject) : Array<Array<Box?>> {
            val dimensions = puzJson.getJSONObject(FIELD_DIMENSIONS)

            val width = dimensions.getInt(FIELD_WIDTH)
            val height = dimensions.getInt(FIELD_HEIGHT)

            val boxes = Array<Array<Box?>>(height) { arrayOfNulls<Box>(width) }

            readPuzzleCells(puzJson, boxes)
            readSaved(puzJson, boxes)
            readSolution(puzJson, boxes)

            return boxes
        }

        /**
         * Populate boxes array following cells in JSON
         *
         * I.e. no box, block, empty, or clue number, possibly with styles
         * etc.
         */
        @Throws(IPuzFormatException::class)
        private fun readPuzzleCells(
            puzJson : JSONObject,
            boxes : Array<Array<Box?>>,
        ) {
            val cells = puzJson.getJSONArray(FIELD_PUZZLE)

            if (cells.length() < boxes.size) {
                throw IPuzFormatException(
                    "Number of cell rows doesn't match boxes dimensions"
                )
            }

            val block : String = getBlockString(puzJson)
            val empty : String = getEmptyCellString(puzJson)
            val namedStyles = puzJson.optJSONObject(FIELD_NAMED_STYLES)

            for (row in boxes.indices) {
                val rowCells = cells.getJSONArray(row)

                if (rowCells.length() < boxes[row].size) {
                    throw IPuzFormatException(
                        "Number of cell columns doesn't match boxes dimension"
                    )
                }

                for (col in boxes[row].indices) {
                    boxes[row][col] = getBoxFromObj(
                        rowCells.get(col),
                        block,
                        empty,
                        namedStyles,
                    )
                }
            }
        }

        private fun getBlockString(puzJson : JSONObject) : String {
            return puzJson.optString(FIELD_BLOCK, DEFAULT_BLOCK)
        }

        private fun getEmptyCellString(puzJson : JSONObject) : String {
            return puzJson.optString(FIELD_EMPTY, DEFAULT_EMPTY_READ)
        }

        /**
         * Turn the (JSON) object into a box
         *
         * If null or block value, then blank. If empty then empty box, else
         * box with clue number and maybe decoration.
         *
         * @param cell the object in the JSON for the cell (could be a
         * number, string, or JSON object
         * @param block the string for a block
         * @param empty the string for an empty cell
         * @param namedStyles the JSON object containing named style specs
         * used as shortcuts (the "styles" field of the IPuz)
         */
        @Throws(IPuzFormatException::class)
        private fun getBoxFromObj(
            cell : Any?,
            block : String,
            empty : String,
            namedStyles : JSONObject?,
        ) : Box? {
            if (isJSONNull(cell)) {
                return null
            } else if (cell is JSONObject) {
                val json = cell

                // unsure if ipuz allows cell field to be missing, but
                // reasonable to assume empty cell if so
                var cellObj = json.opt(FIELD_CELL)
                if (cellObj == null)
                    cellObj = empty

                // to track whether we need to return a null block or an
                // object block with styles &c.
                var createdBlock = false
                var hasData = false

                var box = getBoxFromObj(cellObj, block, empty, namedStyles)
                if (box == null) {
                    // create a box for now, in case it has styles
                    box = Box()
                    box.isBlock = true
                    createdBlock = true
                }

                var style = json.optJSONObject(FIELD_STYLE)
                if (style == null && namedStyles != null) {
                    val styleName = json.optString(FIELD_STYLE)
                    if (styleName != null && !styleName.isEmpty())
                        style = namedStyles.optJSONObject(styleName)
                }

                if (style != null) {
                    val label = style.optString(FIELD_LABEL)
                    if (label != null && !label.isEmpty()) {
                        box.initialValue = label
                        box.response = label
                        hasData = true
                    }

                    hasData = hasData or getShapeFromStyleObj(style, box)
                    hasData = hasData or getColorsFromStyleObj(style, box)
                    hasData = hasData or getBarredFromStyleObj(style, box)
                    hasData = hasData or getMarksFromStyleObj(style, box)
                }

                // overwrites label if there is one (not supporting both)
                val initVal = optStringNull(json, FIELD_VALUE)
                if (initVal != null) {
                    box.initialValue = initVal
                    box.response = initVal
                    hasData = true
                }

                return if (createdBlock && !hasData) null else box
            } else if (cell.toString() == block) {
                return null
            } else if (cell.toString() == empty) {
                return Box()
            } else {
                try {
                    val box = Box()
                    box.clueNumber = cell.toString()
                    return box
                } catch (e : NumberFormatException) {
                    throw IPuzFormatException(
                        "Unrecognised cell in puzzle: " + cell,
                    )
                }
            }
        }

        /**
         * Read the shape field and set in box
         *
         * Returns true if a recognised shape was found
         */
        private fun getShapeFromStyleObj(
            style : JSONObject,
            box : Box,
        ) : Boolean {
            val shape = style.optString(FIELD_SHAPE_BG)
            val boxShape : Box.Shape? = getShape(shape)
            if (boxShape != null) {
                box.shape = boxShape
                return true
            } else {
                return false
            }
        }

        /**
         * Get Box.Shape from text description
         *
         * Uses known IPuz values, with a bit of leniency in punctuation
         *
         * @return null if not recognised
         */
        fun getShape(name : String?) : Box.Shape? {
            var name = name
            if (name == null)
                return null

            // make lenient by removing all non alpha characters except /
            // and \ which are defined shapes
            name = name.replace("[^\\w/\\\\]".toRegex(), "")
                .lowercase(Locale.getDefault())

            return SHAPE_BGS.get(name)
        }

        /**
         * Reads various colours from style and set in box
         *
         * Returns true if something set
         */
        private fun getColorsFromStyleObj(
            style : JSONObject,
            box : Box,
        ) : Boolean {
            var hasData = false
            if (style.optBoolean(FIELD_HIGHLIGHT)) {
                box.color = HIGHLIGHT_COLOR
                hasData = true
            }

            val color : Int? = readColor(style.optString(FIELD_COLOR))
            if (color != null) {
                box.color = color
                hasData = true
            }

            val textColor : Int? = readColor(style.optString(FIELD_TEXT_COLOR))
            if (textColor != null) {
                box.textColor = textColor
                hasData = true
            }

            val barColor : Int? = readColor(style.optString(FIELD_BAR_COLOR))
            if (barColor != null) {
                box.barColor = barColor
                hasData = true
            }

            return hasData
        }

        /**
         * Gets stuff about bars from style
         *
         * Fills in box directly, returns true if it found some bars
         */
        private fun getBarredFromStyleObj(
            style : JSONObject,
            box : Box,
        ) : Boolean {
            var hasBars = false

            hasBars = hasBars or getBarredFromStyleObj(
                style,
                FIELD_BARRED_DOTTED,
                Box.Bar.DOTTED,
                box,
            )
            hasBars = hasBars or getBarredFromStyleObj(
                style,
                FIELD_BARRED_DASHED,
                Box.Bar.DASHED,
                box,
            )
            hasBars = hasBars or getBarredFromStyleObj(
                style,
                FIELD_BARRED,
                Box.Bar.SOLID,
                box,
            )
            return hasBars
        }

        /**
         * Look for bars of a particular kind in style object
         */
        private fun getBarredFromStyleObj(
            style : JSONObject,
            barField : String,
            barStyle : Box.Bar,
            box : Box,
        ) : Boolean {
            var hasBars = false

            var barred = optStringNull(style, barField)
            if (barred != null) {
                barred = barred.uppercase(Locale.getDefault())
                for (i in 0..<barred.length) {
                    val c = barred.get(i)
                    when (c) {
                        BARRED_TOP -> {
                            box.barTop = barStyle
                            hasBars = true
                        }

                        BARRED_BOTTOM -> {
                            box.barBottom = barStyle
                            hasBars = true
                        }

                        BARRED_LEFT -> {
                            box.barLeft = barStyle
                            hasBars = true
                        }

                        BARRED_RIGHT -> {
                            box.barRight = barStyle
                            hasBars = true
                        }

                        else -> {}
                    }
                }
            }

            return hasBars
        }

        /**
         * Fills in marks data for box
         *
         * Returns true if found some
         */
        private fun getMarksFromStyleObj(
            style : JSONObject,
            box : Box,
        ) : Boolean {
            val markObj = style.optJSONObject(FIELD_MARK)
            if (markObj == null)
                return false

            val marks = Array<Array<String?>>(3) { arrayOfNulls<String>(3) }
            marks[0][0] = optStringNull(markObj, FIELD_MARK_TOP_LEFT)
            marks[0][1] = optStringNull(markObj, FIELD_MARK_TOP)
            marks[0][2] = optStringNull(markObj, FIELD_MARK_TOP_RIGHT)
            marks[1][0] = optStringNull(markObj, FIELD_MARK_LEFT)
            marks[1][1] = optStringNull(markObj, FIELD_MARK_CENTER)
            marks[1][2] = optStringNull(markObj, FIELD_MARK_RIGHT)
            marks[2][0] = optStringNull(markObj, FIELD_MARK_BOTTOM_LEFT)
            marks[2][1] = optStringNull(markObj, FIELD_MARK_BOTTOM)
            marks[2][2] = optStringNull(markObj, FIELD_MARK_BOTTOM_RIGHT)

            box.setMarks(marks)

            return true
        }

        /**
         * Populate boxes array with response data from JSON
         */
        @Throws(IPuzFormatException::class)
        private fun readSaved(
            puzJson : JSONObject,
            boxes : Array<Array<Box?>>,
        ) {
            val block : String = getBlockString(puzJson)
            val empty : String = getEmptyCellString(puzJson)
            val saved = puzJson.optJSONArray(FIELD_SAVED)
            if (saved != null && saved.length() > 0)
                readValues(saved, boxes, false, block, empty)
        }

        /**
         * Populate boxes array with puzzle solution data from JSON
         */
        @Throws(IPuzFormatException::class)
        private fun readSolution(
            puzJson : JSONObject,
            boxes : Array<Array<Box?>>,
        ) {
            val block : String = getBlockString(puzJson)
            val empty : String = getEmptyCellString(puzJson)
            val solution = puzJson.optJSONArray(FIELD_SOLUTION)
            if (solution != null && solution.length() > 0)
                readValues(solution, boxes, true, block, empty)
        }

        /**
         * Reads a value array and loads into boxes saved/solution
         *
         * @param cells the array of arrays of CrosswordValues
         * @param boxes the boxes to read data into
         * @param isSolution whether puzzles solution is being read, else
         * saved user responses will be read
         */
        @Throws(IPuzFormatException::class)
        private fun readValues(
            cells : JSONArray,
            boxes : Array<Array<Box?>>,
            isSolution : Boolean,
            block : String,
            empty : String,
        ) {
            val height = min(cells.length(), boxes.size)

            for (row in 0..<height) {
                val rowCells = cells.getJSONArray(row)

                val width = min(rowCells.length(), boxes[row].size)

                for (col in 0..<width) {
                    val value : String? = getCrosswordValueFromObj(
                        rowCells.get(col),
                        block,
                        empty,
                    )

                    if (value != null && !value.isEmpty()) {
                        if (boxes[row][col] == null) {
                            boxes[row][col] = Box()
                            boxes[row][col]?.let { it.isBlock = true }
                        }

                        if (isSolution)
                            boxes[row][col]?.let{ it.solution = value }
                        else
                            boxes[row][col]?.let { it.response = value }
                    }
                }
            }
        }

        /**
         * Fill in the saved data for the box from the object in the JSON
         *
         * @param cell the object in the JSON saved array
         * @param block the representation for a block
         * @param empty the representation of an empty cell
         * @return value of response if given, Box.BLANK if empty, null if
         * block or omitted
         */
        @Throws(IPuzFormatException::class)
        private fun getCrosswordValueFromObj(
            cell : Any?,
            block : String,
            empty : String,
        ) : String? {
            if (isJSONNull(cell)) {
                return null
            } else if (cell is JSONArray) {
                val values = cell
                if (values.length() != 1) {
                    throw IPuzFormatException(
                        "Multiple cell values not supported: " + values,
                    )
                }
                return getCrosswordValueFromObj(values.get(0), block, empty)
            } else if (cell is JSONObject) {
                val json = cell
                val value = optStringNull(json, FIELD_VALUE)
                if (value == null)
                    return null
                else if (value.isEmpty())
                    return Box.BLANK
                else
                    return value
            } else {
                val value = cell.toString()
                if (block == value)
                    return null
                else if (empty == value || value.isEmpty())
                    return Box.BLANK
                else
                    return value
            }
        }

        /**
         * Read clues into puz
         */
        @Throws(IPuzFormatException::class)
        private fun readClues(puzJson : JSONObject, builder : PuzzleBuilder) {
            // default to true as it is safest
            val showEnumerations = puzJson.optBoolean(
                FIELD_SHOW_ENUMERATIONS,
                true,
            )

            val coordBase = getCoordBase(puzJson)
            val separators = readSeparators(puzJson)

            val clues = puzJson.optJSONObject(FIELD_CLUES)
            if (isJSONNull(clues))
                return

            val names = clues?.names()
            if (names == null)
                return

            for (i in 0..<names.length()) {
                val listName = names.getString(i)
                addClues(
                    clues.getJSONArray(listName),
                    names.getString(i),
                    showEnumerations,
                    coordBase,
                    separators,
                    builder,
                )
            }
        }

        /**
         * Reads separators for clues from extensions
         *
         * @return null if no separators, else a map from clue id to
         * separators map
         */
        @Throws(JSONException::class)
        private fun readSeparators(
            puzJson : JSONObject,
        ) : Map<ClueID, Map<Int, String>>? {
            val separators = puzJson.optJSONArray(FIELD_EXT_SEPARATORS)
            if (separators == null)
                return null

            val sepMap = mutableMapOf<ClueID, MutableMap<Int, String>>()

            for (i in 0..<separators.length()) {
                try {
                    val clueSeparators = separators.getJSONObject(i)

                    // passing null for the builder as decode should not need it
                    // and it is not ready for decoding yet anyway
                    val cid : ClueID? = decodeClueID(
                        clueSeparators.getJSONObject(FIELD_CLUEID),
                        null,
                    )

                    val clueSepMap = mutableMapOf<Int, String>()
                    val sepList = clueSeparators.getJSONObject(
                        FIELD_CLUE_SEPARATORS,
                    )
                    sepList.keys().forEachRemaining { pos ->
                        try {
                            val sep = sepList.getString(pos)
                            if (!sep.isEmpty()) {
                                clueSepMap.put(pos.toInt(), sep)
                            }
                        } catch (e : NumberFormatException) {
                            // ignore key
                        }
                    }

                    if (cid != null)
                        sepMap.put(cid, clueSepMap)
                } catch (e : IPuzFormatException) {
                    // just ignore separator if can't be read
                } catch (e : JSONException) {
                }
            }

            return sepMap
        }

        /**
         * Determine if cell indexes 0- or 1-based
         *
         * 0-based matches Puzzaz implementation and first crossword in
         * ipuz. 1-based matches the examples in the ipuz spec (but crashes
         * Puzzaz).
         *
         * Use 0-based by default. 1-based if on Forkyz ipuz io 2 or below,
         * or if some coordinate is too high using 0-based numbering.
         *
         * @param puzJson json object of puzzle
         */
        private fun getCoordBase(puzJson : JSONObject) : Int {
            val ioVersion : Int = getIOVersion(puzJson)
            if (ioVersion > 0 && ioVersion <= 2) {
                return 1
            } else if (ioVersion >= 3) {
                return 0
            } else { // search cells
                val dimensions = puzJson.getJSONObject(FIELD_DIMENSIONS)
                val width = dimensions.getInt(FIELD_WIDTH)
                val height = dimensions.getInt(FIELD_HEIGHT)

                val clues = puzJson.optJSONObject(FIELD_CLUES)
                if (clues != null && !isJSONNull(clues)) {
                    val names = clues.names()
                    for (i in 0..<names.length()) {
                        val listName = names.getString(i)
                        val clueList = clues.getJSONArray(listName)
                        for (j in 0..<clueList.length()) {
                            val clueObj = clueList.get(j)
                            if (clueObj is JSONObject) {
                                val clueJson = clueObj
                                val cells = clueJson.optJSONArray(
                                    FIELD_CLUE_CELLS,
                                )
                                if (cells != null) {
                                    for (k in 0..<cells.length()) {
                                        val cell = cells.getJSONArray(k)
                                        val row = cell.getInt(1)
                                        if (row >= height)
                                            return 1
                                        val col = cell.getInt(0)
                                        if (col >= width)
                                            return 1
                                    }
                                }
                            }
                        }
                    }
                }
                return 0
            }
        }

        /**
         * Transfer clues from json to puzzle
         *
         * Adds enumeration text to hint if showEnumerations is true
         *
         * @param coordBase 0 if 0-based, 1 if 1-based
         * @param separators map from clue id to separators at positions read from
         * ipuz extensions, or null if no separators
         */
        @Throws(IPuzFormatException::class)
        private fun addClues(
            jsonClues : JSONArray,
            listName : String,
            showEnumerations : Boolean,
            coordBase : Int,
            separators : Map<ClueID, Map<Int, String>>?,
            builder : PuzzleBuilder,
        ) {
            val splitName = listName.split(":".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val dirName = splitName[0]
            val displayName = if (splitName.size > 1) splitName[1] else dirName

            try {
                for (i in 0..<jsonClues.length()) {
                    val clueSeparators = separators?.get(ClueID(displayName, i))

                    val clueObj = jsonClues.get(i)
                    // all clues not across until proven otherwise by list name
                    addClue(
                        dirName,
                        displayName,
                        clueObj,
                        showEnumerations,
                        coordBase,
                        clueSeparators,
                        builder,
                    )
                }
            } catch (e : IllegalArgumentException) {
                throw IPuzFormatException(e.message)
            }
        }

        /**
         * Convert a JSON object clue into a Clue and add to builder
         *
         * Adds enumeration to hint if showEnumerations is true
         *
         * @param coordBase 0 if 0-based indexing, 1 if 1-based
         * @param dirName the name of the direction of the clue (see ipuz
         * directions)
         * @param separators that apply to this clue or null if none
         */
        @Throws(IPuzFormatException::class)
        private fun addClue(
            dirName : String?,
            listName : String,
            clueObj : Any,
            showEnumerations : Boolean,
            coordBase : Int,
            separators : Map<Int, String>?,
            builder : PuzzleBuilder,
        ) {
            if (clueObj is String) {
                val hint = clueObj
                buildClue(
                    listName,
                    null,
                    null,
                    hint,
                    null,
                    null,
                    separators,
                    builder,
                )
            } else if (clueObj is JSONArray) {
                val clueArray = clueObj
                if (clueArray.length() != 2) {
                    throw IPuzFormatException(
                        "Unexpected clue array length: " + clueArray.length(),
                    )
                }
                val clueNumObj = clueArray.get(0)
                val hint = clueArray.getString(1)
                val zone = getDirZone(
                    getClueNumber(clueNumObj),
                    dirName,
                    builder,
                )

                buildClue(
                    listName,
                    clueNumObj,
                    null,
                    hint,
                    null,
                    zone,
                    separators,
                    builder,
                )
            } else if (clueObj is JSONObject) {
                val clueJson = clueObj

                // get clue number, sometimes nested
                var clueNumObj = clueJson.opt(FIELD_CLUE_NUMBER)
                if (getClueNumber(clueNumObj) == null)
                    clueNumObj = clueJson.opt(FIELD_CLUE_NUMBERS)

                val label = optStringNull(clueJson, FIELD_CLUE_LABEL)

                // build hint, bake in additional info
                val hint = StringBuilder()

                hint.append(clueJson.getString(FIELD_CLUE_HINT))

                val conts = clueJson.optJSONArray(FIELD_CLUE_CONTINUED)
                if (conts != null && conts.length() > 0) {
                    addCrossRefList(hint, conts, "cont.")
                }

                val refs = clueJson.optJSONArray(FIELD_CLUE_REFERENCES)
                if (refs != null && refs.length() > 0) {
                    addCrossRefList(hint, refs, "ref.")
                }

                val enumeration = if (showEnumerations)
                    optStringNull(clueJson, FIELD_ENUMERATION)
                else
                    null

                var zone = getClueZone(clueJson, coordBase)
                if (zone == null) {
                    zone = getDirZone(
                        getClueNumber(clueNumObj),
                        dirName,
                        builder,
                    )
                }

                buildClue(
                    listName,
                    clueNumObj,
                    label,
                    hint.toString(),
                    enumeration,
                    zone,
                    separators,
                    builder,
                )
            } else {
                throw IPuzFormatException(
                    "Unsupported clue format "
                        + clueObj.javaClass
                        + ": "
                        + clueObj,
                )
            }
        }

        /**
         * Read zone info from clue json
         *
         * @param coordBase 0 if 0-based, 1 if 1-based
         * @return null if no info
         */
        private fun getClueZone(
            clueJson : JSONObject?,
            coordBase : Int,
        ) : Zone? {
            if (clueJson == null)
                return null

            val cells = clueJson.optJSONArray(FIELD_CLUE_CELLS)
            if (cells == null)
                return null

            val zone = Zone()

            for (i in 0..<cells.length()) {
                val cell = cells.getJSONArray(i)
                zone.addPosition(
                    Position(
                        cell.getInt(1) - coordBase,
                        cell.getInt(0) - coordBase,
                    ),
                )
            }

            return zone
        }

        /**
         * Get zone for direction/number
         *
         * Returns null if not found
         */
        private fun getDirZone(
            number : String?,
            dirName : String?,
            builder : PuzzleBuilder,
        ) : Zone? {
            // TODO: support zoning of more than just across/down
            if (FIELD_CLUES_ACROSS == dirName)
                return builder.getAcrossZone(number)
            else if (FIELD_CLUES_DOWN == dirName)
                return builder.getDownZone(number)
            else return null
        }

        /**
         * Add cross references to hint
         *
         * @param hint hint being built
         * @param refs list of references
         * @param description text to identify what reference list is (e.g.
         * "cont." or "ref.")
         */
        private fun addCrossRefList(
            hint : StringBuilder,
            refs : JSONArray,
            description : String?,
        ) {
            hint.append(" (")
            hint.append(description)
            hint.append(" ")
            for (i in 0..<refs.length()) {
                if (i > 0) hint.append("/")
                val ref = refs.getJSONObject(i)
                hint.append(
                    getComplexClueNumString(ref.get(FIELD_CLUE_NUMBER)),
                )
                hint.append(" ")

                hint.append(ref.getString(FIELD_CLUE_DIRECTION))
            }
            hint.append(")")
        }

        /**
         * Build a Clue object from info and add to builder
         *
         * In particular handles the different clue number formats
         *
         * @param clueNumObj the ClueNum object in JSON
         * @param listName the name of the list the clue is in
         * @param label a label to display instead of clue number (null if
         * not used)
         * @param hint the clue hint
         * @param enumeration null or empty if no enumeration to be shown in clue
         * @param zone the zone of the clue or null
         * @param separators separators to add to clue or null if none
         * @param builder the builder to add the clue to
         */
        @Throws(IPuzFormatException::class)
        private fun buildClue(
            listName : String,
            clueNumObj : Any?,
            label : String?,
            hint : String,
            enumeration : String?,
            zone : Zone?,
            separators : Map<Int, String>?,
            builder : PuzzleBuilder,
        ) {
            var hint = hint
            val number = getClueNumber(clueNumObj)

            if (isComplexClueNumber(clueNumObj)) {
                val numString = getComplexClueNumString(clueNumObj)
                if (numString != null && numString.length > 0)
                    hint += " (clues " + numString + ")"
            }

            if (enumeration != null && enumeration.length > 0) {
                hint += " (" + enumeration + ")"
            }

            val index = builder.getNextClueIndex(listName)
            val clue = Clue(
                listName,
                index,
                number,
                label,
                hint,
                zone,
                separators,
            )

            builder.addClue(clue)
        }

        /**
         * Check if clueNumObj is something other than a simple number
         *
         * Will accept a JSONArray of ClueNums not just a single ClueNum
         */
        @Throws(IPuzFormatException::class)
        private fun isComplexClueNumber(clueNumObj : Any?) : Boolean {
            if (clueNumObj == null)
                return false

            if (clueNumObj is Number)
                return false

            if (clueNumObj is String)
                return false

            if (clueNumObj is JSONArray)
                return true

            throw IPuzFormatException(
                "Unrecognised clue number format: " + clueNumObj.javaClass,
            )
        }

        /**
         * Return basic number from clueNumObj
         *
         * If a number, return its int value. If a string, return first
         * integer in the string. If a JSONArray return the first item in
         * the array from which a number can be extracted.
         *
         * Else return null (including if null passed).
         */
        private fun getClueNumber(clueNumObj : Any?) : String? {
            if (clueNumObj is Number)
                return clueNumObj.toInt().toString()

            if (clueNumObj is String)
                return clueNumObj

            if (clueNumObj is JSONArray) {
                val clueNums = clueNumObj

                for (subNumObj in clueNums) {
                    val subNum = getClueNumber(subNumObj)
                    if (subNum != null)
                        return subNum
                }
            }

            return null
        }

        /**
         * Extract string from a complex clue number representation
         *
         * E.g. "1/2" from ["1", "2"]
         *
         * Returns null if nothing useful could be extracted.
         */
        private fun getComplexClueNumString(clueNumObj : Any?) : String? {
            if (clueNumObj is Number)
                return clueNumObj.toInt().toString()

            if (clueNumObj is String)
                return clueNumObj

            if (clueNumObj is JSONArray) {
                val objs = clueNumObj

                val builder = StringBuilder()

                for (i in 0..<objs.length()) {
                    if (i > 0) builder.append("/")
                    builder.append(getComplexClueNumString(objs.get(i)))
                }

                return builder.toString()
            }

            return null
        }

        /**
         * Read non-standard IPuz fields into puzzle
         *
         * Does not read clue separators as these need to be read during clue
         * reading
         */
        @Throws(IPuzFormatException::class)
        private fun readExtensions(
            puzJson : JSONObject,
            builder : PuzzleBuilder,
        ) {
            val supportUrl = optStringNull(puzJson, FIELD_EXT_SUPPORT_URL)
            if (supportUrl != null)
                builder.setSupportUrl(supportUrl)

            val shareUrl = optStringNull(puzJson, FIELD_EXT_SHARE_URL)
            if (shareUrl != null)
                builder.setShareUrl(shareUrl)

            readImages(puzJson, builder)

            val pinnedClueID : ClueID? = decodeClueID(
                puzJson.optJSONObject(FIELD_EXT_PINNED_CLUE_ID),
                builder,
            )
            if (pinnedClueID != null)
                builder.setPinnedClueID(pinnedClueID)

            val playData = puzJson.optJSONObject(FIELD_EXT_PLAY_DATA)
            if (playData != null && !isJSONNull(playData))
                Companion.readPlayData(playData, builder)
        }

        private fun readImages(puzJson : JSONObject, builder : PuzzleBuilder) {
            val images = puzJson.optJSONArray(FIELD_EXT_IMAGES)
            if (images == null)
                return

            for (i in 0..<images.length()) {
                val image = images.getJSONObject(i)
                val url = image.getString(FIELD_EXT_IMAGE_URL)
                val row = image.getInt(FIELD_EXT_IMAGE_ROW)
                val col = image.getInt(FIELD_EXT_IMAGE_COL)
                val width = image.getInt(FIELD_EXT_IMAGE_WIDTH)
                val height = image.getInt(FIELD_EXT_IMAGE_HEIGHT)

                builder.addImage(PuzImage(url, row, col, width, height))
            }
        }

        /**
         * Read play data extension from playData object to puz
         *
         * @param playData the playData field of the puz json
         */
        @Throws(IPuzFormatException::class)
        private fun readPlayData(
            playData : JSONObject,
            builder : PuzzleBuilder,
        ) {
            readBoxExtras(playData, builder)
            readPosition(playData, builder)
            readClueHistory(playData, builder)
            readClueNotes(playData, builder)
            readPlayerNote(playData, builder)
            readFlaggedClues(playData, builder)

            if (playData.has(FIELD_COMPLETION_TIME))
                builder.setTime(playData.getLong(FIELD_COMPLETION_TIME))

            if (playData.has(FIELD_RATING))
                builder.setRating(playData.getInt(FIELD_RATING).toChar())
        }

        /**
         * Read non-standard info about boxes (e.g. is cheated)
         *
         * Assumes boxes have been set on puz
         */
        private fun readBoxExtras(
            playData : JSONObject,
            builder : PuzzleBuilder,
        ) {
            if (!playData.has(FIELD_BOX_EXTRAS))
                return

            val cellsJson = playData.getJSONArray(FIELD_BOX_EXTRAS)

            val numRows = min(cellsJson.length(), builder.height)

            for (row in 0..<numRows) {
                val rowJson = cellsJson.getJSONArray(row)

                val numCols = min(rowJson.length(), builder.width)

                for (col in 0..<numCols) {
                    val box = builder.getBox(row, col)
                    if (box != null && !isBlock(box)) {
                        val boxJson = rowJson.getJSONObject(col)

                        if (boxJson.has(FIELD_BOX_CHEATED)) {
                            box.isCheated = boxJson.getBoolean(
                                FIELD_BOX_CHEATED,
                            )
                        }
                        if (boxJson.has(FIELD_BOX_RESPONDER)) {
                            box.responder = boxJson.getString(
                                FIELD_BOX_RESPONDER,
                            )
                        }
                        if (boxJson.has(FIELD_BOX_FLAGGED)) {
                            box.isFlagged = boxJson.getBoolean(
                                FIELD_BOX_FLAGGED,
                            )
                        }
                        if (boxJson.has(FIELD_BOX_FLAG_COLOR)) {
                            readColor(
                                boxJson.optString(FIELD_BOX_FLAG_COLOR),
                            )?.let {
                                box.flagColor = it
                            }
                        }
                    }
                }
            }
        }

        /**
         * Read the position from playData
         *
         * Assumes builder already provides a puzzle with boxes and clues set up.
         */
        @Throws(IPuzFormatException::class)
        private fun readPosition(
            playData : JSONObject,
            builder : PuzzleBuilder,
        ) {
            if (!playData.has(FIELD_POSITION))
                return

            val positionJson = playData.getJSONObject(FIELD_POSITION)

            if (
                positionJson.has(FIELD_POSITION_ROW)
                && positionJson.has(FIELD_POSITION_COL)
            ) {
                val row = positionJson.optInt(FIELD_POSITION_ROW, -1)
                val col = positionJson.optInt(FIELD_POSITION_COL, -1)
                val pos = Position(row, col)

                if (
                    0 <= row
                    && row <= builder.height
                    && 0 <= col
                    && col <= builder.width
                ) {
                    builder.setPosition(pos)
                }

                // old style: has an across boolean for across/down only
                // new style: has current clue id
                if (positionJson.has(FIELD_POSITION_ACROSS)) {
                    // old style
                    val list
                        = if (positionJson.getBoolean(FIELD_POSITION_ACROSS))
                            OLD_ACROSS_LIST_NAME
                        else
                            OLD_DOWN_LIST_NAME
                    val box = builder.getBox(pos)
                    val cid = if (box == null || isBlock(box))
                        null
                    else
                        box.getIsPartOfClue(list)
                    builder.setCurrentClueID(cid)
                } else if (positionJson.has(FIELD_CLUEID)) {
                    val cidJson = positionJson.getJSONObject(FIELD_CLUEID)
                    builder.setCurrentClueID(decodeClueID(cidJson, builder))
                }
            }
        }

        /**
         * Reads clue history from playData
         *
         * Assumes builder returns a puzzle with clue lists set up.
         */
        @Throws(IPuzFormatException::class)
        private fun readClueHistory(
            playData : JSONObject,
            builder : PuzzleBuilder,
        ) {
            if (!playData.has(FIELD_CLUE_HISTORY))
                return

            val historyJson = playData.getJSONArray(FIELD_CLUE_HISTORY)

            val history = mutableListOf<ClueID>()

            for (i in 0..<historyJson.length()) {
                val itemJson = historyJson.getJSONObject(i)
                val cid = decodeClueID(itemJson, builder)
                if (cid != null)
                    history.add(cid)
            }

            builder.setHistory(history)
        }

        /**
         * Read notes from playData
         *
         * Assumes builder returns a puzzle with clue lists set up
         */
        @Throws(IPuzFormatException::class)
        private fun readClueNotes(
            playData : JSONObject,
            builder : PuzzleBuilder,
        ) {
            if (!playData.has(FIELD_CLUE_NOTES))
                return

            val notesJson = playData.getJSONArray(FIELD_CLUE_NOTES)

            for (i in 0..<notesJson.length()) {
                val noteJson = notesJson.getJSONObject(i)
                val cndJson = noteJson.optJSONObject(FIELD_CLUE_NOTE_CLUE)
                val cid : ClueID? = decodeClueID(cndJson, builder)

                if (cid != null) {
                    val scratch = noteJson.optString(FIELD_NOTE_SCRATCH, null)
                    val text = unHtmlOptString(noteJson, FIELD_NOTE_TEXT)
                    val anagramSrc
                        = noteJson.optString(FIELD_NOTE_ANAGRAM_SRC, null)
                    val anagramSol
                        = noteJson.optString(FIELD_NOTE_ANAGRAM_SOL, null)

                    if (
                        scratch != null
                        || text != null
                        || anagramSrc != null
                        || anagramSol != null
                    ) {
                        builder.setNote(
                            cid,
                            Note(scratch, text, anagramSrc, anagramSol),
                        )
                    }
                }
            }
        }

        /**
         * Read player note
         */
        private fun readPlayerNote(
            playData : JSONObject,
            builder : PuzzleBuilder,
        ) {
            if (!playData.has(FIELD_PLAYER_NOTE))
                return

            val noteJson = playData.getJSONObject(FIELD_PLAYER_NOTE)

            val scratch = noteJson.optString(FIELD_NOTE_SCRATCH, null)
            val text = unHtmlOptString(noteJson, FIELD_NOTE_TEXT)
            val anagramSrc = noteJson.optString(FIELD_NOTE_ANAGRAM_SRC, null)
            val anagramSol = noteJson.optString(FIELD_NOTE_ANAGRAM_SOL, null)

            if (
                scratch != null
                || text != null
                || anagramSrc != null
                || anagramSol != null
            ) {
                builder.setPlayerNote(
                    Note(scratch, text, anagramSrc, anagramSol),
                )
            }
        }

        /**
         * Read which clues are flagged
         *
         * Assumes builder returns a puzzle whose clue lists have been set
         * up
         */
        @Throws(IPuzFormatException::class)
        private fun readFlaggedClues(
            playData : JSONObject,
            builder : PuzzleBuilder,
        ) {
            if (!playData.has(FIELD_FLAGGED_CLUES))
                return

            val flagsJson = playData.getJSONArray(FIELD_FLAGGED_CLUES)

            for (i in 0..<flagsJson.length()) {
                val flagJson = flagsJson.getJSONObject(i)
                val cid = decodeClueID(flagJson, builder)
                if (cid != null) {
                    val color = readColor(flagJson.optString(FIELD_COLOR))
                    if (color != null)
                        builder.flagClue(cid, true, color)
                    else
                        builder.flagClue(cid, true)
                }
            }
        }

        /**
         * Read a JSON representation of ClueID to ClueID
         *
         * Assumes (for legacy clue ids) that builder already returns a
         * puzzle with clue lists set up.
         *
         * ClueIDs version 1: list name and across/down boolean
         * ClueIDs version 2: list name and clue number
         * ClueIDs version 3: list name and index in clue list
         *
         * Version 2 was created to handle clue lists apart from
         * across/down. Extended to Version 3 to better handle unnumbered clues.
         *
         * @param builder the builder object, can be null if version 3
         * or above is guaranteed
         * @return null if builder needed but no builder given
         */
        @Throws(IPuzFormatException::class)
        private fun decodeClueID(
            cid : JSONObject?,
            builder : PuzzleBuilder?,
        ) : ClueID? {
            if (cid == null || isJSONNull(cid))
                return null

            // Version 3
            if (cid.has(FIELD_CLUE_LISTNAME) && cid.has(FIELD_CLUE_INDEX)) {
                val listName = cid.getString(FIELD_CLUE_LISTNAME)
                val index = cid.getInt(FIELD_CLUE_INDEX)
                return ClueID(listName, index)
                // Version 2
            } else if (
                cid.has(FIELD_CLUE_LISTNAME) && cid.has(FIELD_CLUE_NUMBER)
            ) {
                var number = cid.optString(FIELD_CLUE_NUMBER)
                if (number == null)
                    number = cid.getInt(FIELD_CLUE_NUMBER).toString()
                val listName = cid.getString(FIELD_CLUE_LISTNAME)
                if (builder == null)
                    return null
                return getClueIDFromListNum(listName, number, builder)
                // Version 1
            } else if (
                cid.has(FIELD_CLUE_NUMBER) && cid.has(FIELD_CLUE_ACROSS)
            ) {
                val across = cid.getBoolean(FIELD_CLUE_ACROSS)
                val listName : String = if (across)
                    OLD_ACROSS_LIST_NAME
                else
                    OLD_DOWN_LIST_NAME
                var number = cid.optString(FIELD_CLUE_NUMBER)
                if (number == null)
                    number = cid.getInt(FIELD_CLUE_NUMBER).toString()
                if (builder == null)
                    return null
                return getClueIDFromListNum(listName, number, builder)
            } else {
                throw IPuzFormatException(
                    "Could not decode ClueID from " + cid,
                )
            }
        }

        /**
         * Find the clue ID from listname and clue number
         *
         * Requires buidler to return a puzzle with clue lists already set
         * up.
         */
        @Throws(IPuzFormatException::class)
        private fun getClueIDFromListNum(
            listName : String,
            number : String,
            builder : PuzzleBuilder,
        ) : ClueID {
            val puz = builder.puzzle

            val clues = puz.getClues(listName)
            if (clues == null) {
                throw IPuzFormatException(
                    "Clue ID with non-existent list name: " + listName,
                )
            }

            val index = clues.getClueIndex(number)
            if (index < 0) {
                throw IPuzFormatException(
                    "Clue ID with non-existent number "
                        + number
                        + " in list "
                        + listName,
                )
            }

            return ClueID(listName, index)
        }

        /**
         * For acrostics, ensure there's a clue for selecting whole board
         *
         * Else the user has to pick box by box, which is annoying. Add one
         * if needed.
         */
        private fun ensureBoardClue(builder : PuzzleBuilder) {
            val puz = builder.puzzle

            val boardZone: Zone = getCluedBoardZone(puz)
            if (!hasClueCoveringZone(puz, boardZone)) {
                val index = builder.getNextClueIndex(ACROSTIC_BOARD_CLUE_LIST)
                builder.addClue(
                    Clue(
                        ACROSTIC_BOARD_CLUE_LIST,
                        index,
                        null,
                        ACROSTIC_BOARD_CLUE_HINT,
                        boardZone,
                    ),
                )
            }
        }

        /**
         * True if there's a clue in puz with all position in zone
         */
        private fun hasClueCoveringZone(puz : Puzzle, zone : Zone?) : Boolean {
            for (clue in puz.allClues) {
                if (zoneCoversZone(clue.zone, zone))
                    return true
            }
            return false
        }

        /**
         * True if all positions of coveredZone are in coveringZone
         *
         * Assumes coveredZone doesn't contain duplicates. Positions don't
         * have to be in same order.
         */
        private fun zoneCoversZone(
            coveringZone : Zone?,
            coveredZone : Zone?,
        ) : Boolean {
            if (coveringZone === coveredZone)
                return true
            if (coveredZone == null)
                return true
            if (coveringZone == null)
                return coveredZone.isEmpty

            // this optimisation won't work if coveredZone contain duplicate
            // elements... (never in the way we use this)
            if (coveringZone.size() < coveredZone.size())
                return false

            val coveringPositions = mutableSetOf<Position>()
            for (pos in coveringZone)
                coveringPositions.add(pos)

            for (pos in coveredZone) {
                if (!coveringPositions.contains(pos))
                    return false
            }

            return true
        }

        /**
         * Make a zone containing all puzzle cells appearing in a clue
         */
        private fun getCluedBoardZone(puz : Puzzle) : Zone {
            val cluedPositions = mutableSetOf<Position>()
            for (clue in puz.allClues) {
                for (pos in clue.zone)
                    cluedPositions.add(pos)
            }

            val boardZone = Zone()
            for (row in 0..<puz.height) {
                for (col in 0..<puz.width) {
                    val box = puz.checkedGetBox(row, col)
                    if (!isBlock(box)) {
                        val pos = Position(row, col)
                        if (cluedPositions.contains(pos))
                            boardZone.addPosition(pos)
                    }
                }
            }

            return boardZone
        }

        /**
         * Write puzzle to os using WRITE_CHARSET
         */
        @JvmOverloads
        @Throws(IOException::class)
        fun writePuzzle(
            puz : Puzzle,
            os : OutputStream,
            omitPlayState : Boolean = false,
            omitExtensions : Boolean = false,
        ) {
            var writer : BufferedWriter? = null
            try {
                writer = BufferedWriter(
                    OutputStreamWriter(os, WRITE_CHARSET),
                )

                val jsonWriter = FormatableJSONWriter(writer)

                jsonWriter.`object`()
                jsonWriter.newLine()

                writeIPuzHeader(puz, jsonWriter)
                writeMetaData(puz, jsonWriter)
                writeBoxes(puz, jsonWriter, omitPlayState)
                writeClues(puz, jsonWriter)

                if (!omitExtensions)
                    writeExtensions(puz, jsonWriter, omitPlayState)

                jsonWriter.endObject()
                jsonWriter.newLine()
            } finally {
                // don't close original output stream, it's the caller's job
                if (writer != null)
                    writer.flush()
            }
        }

        /**
         * Write IPuz version and kind
         */
        @Throws(IOException::class,IPuzFormatException::class)
        private fun writeIPuzHeader(
            puz : Puzzle,
            writer : FormatableJSONWriter,
        ) {
            writer.keyValueNonNull(FIELD_VERSION, WRITE_VERSION)
                .key(FIELD_KIND)
                .array()

            when (puz.kind) {
                PuzzleKind.CROSSWORD -> writer.value(WRITE_KIND)
                PuzzleKind.ACROSTIC -> writer.value(WRITE_KIND_ACROSTIC)
                else -> {
                    throw IPuzFormatException(
                        "Unrecognised puzzle kind ${puz.kind}",
                    )
                }
            }

            writer.endArray()
            writer.newLine()
        }

        /**
         * Add basic puzzle metadata (title, author, etc).
         */
        @Throws(IOException::class)
        private fun writeMetaData(puz : Puzzle, writer : FormatableJSONWriter) {
            writer
                .keyValueNonNull(FIELD_TITLE, htmlUnicode(puz.title))
                .keyValueNonNull(FIELD_AUTHOR, htmlUnicode(puz.author))
                .keyValueNonNull(FIELD_COPYRIGHT, htmlUnicode(puz.copyright))
                .keyValueNonNull(FIELD_INTRO, htmlUnicode(puz.introMessage))
                .keyValueNonNull(FIELD_NOTES, htmlUnicode(puz.notes))
                .keyValueNonNull(
                    FIELD_EXPLANATION, htmlUnicode(puz.completionMessage),
                ).keyValueNonNull(FIELD_URL, puz.sourceUrl)
                .keyValueNonNull(FIELD_PUBLISHER, puz.source)

            val date = puz.date
            if (date != null)
                writer.keyValueNonNull(FIELD_DATE, DATE_FORMATTER.format(date))
        }

        /**
         * Read all IPuz supported box information to json
         */
        @Throws(IOException::class)
        private fun writeBoxes(
            puz : Puzzle,
            writer : FormatableJSONWriter,
            omitPlayState : Boolean,
        ) {
            writeDimensions(puz, writer)
            writePuzzleCells(puz, writer)
            if (!omitPlayState)
                writeSaved(puz, writer)
            writeSolution(puz, writer)
        }

        /**
         * Add puzzle dimensions to json
         */
        @Throws(IOException::class)
        private fun writeDimensions(
            puz : Puzzle,
            writer : FormatableJSONWriter,
        ) {
            writer.key(FIELD_DIMENSIONS)
                .`object`()
                .key(FIELD_WIDTH).value(puz.width.toLong())
                .key(FIELD_HEIGHT).value(puz.height.toLong())
                .endObject()
            writer.newLine()
        }

        /**
         * Add the puzzle field to json
         */
        @Throws(IOException::class)
        private fun writePuzzleCells(
            puz : Puzzle,
            writer : FormatableJSONWriter,
        ) {
            writer.key(FIELD_PUZZLE)
                .array()
            writer.newLine()

            val boxes : Array<Array<Box?>> = puz.boxes

            for (row in boxes.indices) {
                writer.indent(1)
                    .array()

                for (col in boxes[row].indices) {
                    val box = boxes[row][col]

                    if (box == null) {
                        writer.value(DEFAULT_BLOCK)
                    } else {
                        val cellContents = if (isBlock(box))
                            DEFAULT_BLOCK
                        else
                            box.clueNumber
                        if (isCellWithStyle(box) || box.hasInitialValue()) {
                            writer.`object`()

                            writeCellStyle(box, writer)

                            // written as style label for blocks
                            if (!isBlock(box) && box.hasInitialValue()) {
                                writer.key(FIELD_VALUE)
                                    .value(box.initialValue)
                            }

                            if (cellContents != null) {
                                writer.key(FIELD_CELL).value(cellContents)
                            } else {
                                writer.key(FIELD_CELL)
                                    .value(DEFAULT_EMPTY_WRITE.toLong())
                            }

                            writer.endObject()
                        } else if (cellContents != null) {
                            writer.value(cellContents)
                        } else {
                            writer.value(DEFAULT_EMPTY_WRITE.toLong())
                        }
                    }
                }

                writer.endArray()
                writer.newLine()
            }

            writer.endArray()
            writer.newLine()
        }

        private fun isCellWithStyle(box : Box) : Boolean {
            return box.hasShape()
                || box.hasBars()
                || box.hasColor()
                || box.hasTextColor()
                || box.hasBarColor()
                || box.hasMarks()
                || (isBlock(box) && box.hasInitialValue())
        }

        private fun writeCellStyle(
            box : Box,
            writer : FormatableJSONWriter,
        ) {
            if (!isCellWithStyle(box))
                return

            writer.key(FIELD_STYLE)
                .`object`()

            val shape = SHAPE_BGS_REV.get(box.shape)
            if (shape != null) {
                writer.key(FIELD_SHAPE_BG).value(shape)
            }

            // i say blocks have labels rather than values
            if (isBlock(box) && box.hasInitialValue()) {
                writer.key(FIELD_LABEL).value(box.initialValue)
            }

            writeColorFields(box, writer)
            writeBarredFields(box, writer)
            writeMarkField(box, writer)

            writer.endObject()
        }

        private fun writeColorFields(box : Box, writer : FormatableJSONWriter) {
            if (box.hasColor()) {
                writer.key(FIELD_COLOR)
                    .value(colorToHex(box.color))
            }
            if (box.hasTextColor()) {
                writer.key(FIELD_TEXT_COLOR)
                    .value(colorToHex(box.textColor))
            }
            if (box.hasBarColor()) {
                writer.key(FIELD_BAR_COLOR)
                    .value(colorToHex(box.barColor))
            }
        }

        private fun writeBarredFields(
            box : Box,
            writer : FormatableJSONWriter,
        ) {
            if (!box.hasBars())
                return

            writeBarredField(box, FIELD_BARRED, Box.Bar.SOLID, writer)
            writeBarredField(box, FIELD_BARRED_DASHED, Box.Bar.DASHED, writer)
            writeBarredField(box, FIELD_BARRED_DOTTED, Box.Bar.DOTTED, writer)
        }

        private fun writeBarredField(
            box : Box,
            barField : String?,
            barStyle : Box.Bar?,
            writer : FormatableJSONWriter,
        ) {
            var barred = ""

            if (box.barTop == barStyle)
                barred += BARRED_TOP
            if (box.barRight == barStyle)
                barred += BARRED_RIGHT
            if (box.barBottom == barStyle)
                barred += BARRED_BOTTOM
            if (box.barLeft == barStyle)
                barred += BARRED_LEFT

            if (!barred.isEmpty())
                writer.key(barField).value(barred)
        }

        private fun writeMarkField(box : Box, writer : FormatableJSONWriter) {
            if (box.hasMarks()) {
                writer.key(FIELD_MARK)
                    .`object`()

                box.marks?.let { marks ->
                    writeMark(FIELD_MARK_TOP_LEFT, marks[0][0], writer)
                    writeMark(FIELD_MARK_TOP, marks[0][1], writer)
                    writeMark(FIELD_MARK_TOP_RIGHT, marks[0][2], writer)
                    writeMark(FIELD_MARK_LEFT, marks[1][0], writer)
                    writeMark(FIELD_MARK_CENTER, marks[1][1], writer)
                    writeMark(FIELD_MARK_RIGHT, marks[1][2], writer)
                    writeMark(FIELD_MARK_BOTTOM_LEFT, marks[2][0], writer)
                    writeMark(FIELD_MARK_BOTTOM, marks[2][1], writer)
                    writeMark(FIELD_MARK_BOTTOM_RIGHT, marks[2][2], writer)
                }

                writer.endObject()
            }
        }

        private fun writeMark(
            markField : String?,
            text : String?,
            writer : FormatableJSONWriter,
        ) {
            if (text == null)
                return
            writer.key(markField).value(text)
        }

        /**
         * Add the saved field to json
         */
        @Throws(IOException::class)
        private fun writeSaved(puz : Puzzle, writer : FormatableJSONWriter) {
            writer.key(FIELD_SAVED)
                .array()
            writer.newLine()

            val boxes : Array<Array<Box?>> = puz.boxes

            for (row in boxes.indices) {
                writer.indent(1)
                    .array()

                for (col in boxes[row].indices) {
                    val box = boxes[row][col]

                    if (box == null || isBlock(box))
                        writer.value(DEFAULT_BLOCK)
                    else if (box.isBlank())
                        writer.value(DEFAULT_EMPTY_WRITE.toLong())
                    else
                        writer.value(box.response)
                }

                writer.endArray()
                writer.newLine()
            }

            writer.endArray()
            writer.newLine()
        }

        /**
         * Add the solution field to json if the puzzle has one
         */
        @Throws(IOException::class)
        private fun writeSolution(puz : Puzzle, writer : FormatableJSONWriter) {
            if (!puz.hasSolution())
                return

            writer.key(FIELD_SOLUTION)
                .array()
            writer.newLine()

            val boxes : Array<Array<Box?>> = puz.boxes

            for (row in boxes.indices) {
                writer.indent(1)
                    .array()

                for (col in boxes[row].indices) {
                    val box = boxes[row][col]

                    if (box == null || isBlock(box)) {
                        writer.value(DEFAULT_BLOCK)
                    } else if (box.hasSolution()) {
                        writer.value(box.solution.toString())
                    } else {
                        writer.value(JSONObject.NULL)
                    }
                }

                writer.endArray()
                writer.newLine()
            }

            writer.endArray()
            writer.newLine()
        }

        /**
         * Add the clues lists to the json
         */
        @Throws(IOException::class)
        private fun writeClues(puz : Puzzle, writer : FormatableJSONWriter) {
            writer.key(FIELD_CLUES)
                .`object`()
            writer.newLine()

            // Sort so guaranteed output order
            for (listName in puz.clueListNames.sorted()) {
                val clues = puz.getClues(listName)
                if (clues == null)
                    continue

                var direction : String? = null

                // check if a standard direction or "zones", else default to
                // "clues" list
                if (PuzzleUtils.isAcrossList(puz, clues)) {
                    direction = FIELD_CLUES_ACROSS
                } else if (PuzzleUtils.isDownList(puz, clues)) {
                    direction = FIELD_CLUES_DOWN
                } else if (PuzzleUtils.isZonesList(clues)) {
                    direction = FIELD_CLUES_ZONES
                }

                // TODO: other directions
                if (direction != null) {
                    var fullListName = listName
                    if (direction != fullListName)
                        fullListName = direction + ":" + listName
                    val zones = FIELD_CLUES_ZONES == direction
                    Companion.writeClueList(fullListName, clues, zones, writer)
                } else {
                    // assume unrecognised fields are all Clues:xxx
                    var fullListName = listName
                    if (FIELD_CLUES_CLUES != listName)
                        fullListName = FIELD_CLUES_CLUES + ":" + listName
                    Companion.writeClueList(fullListName, clues, false, writer)
                }
            }

            writer.endObject()
            writer.newLine()
        }

        /**
         * Convert a clues list into a json array and return it
         */
        @Throws(IOException::class)
        private fun writeClueList(
            fieldName : String?,
            clues : Iterable<Clue>,
            withZones : Boolean,
            writer : FormatableJSONWriter,
        ) {
            writer.indent(1)
                .key(fieldName)
                .array()
            writer.newLine()

            for (clue in clues) {
                val hint = clue.hint

                writer.indent(2)

                if (withZones || clue.hasLabel()) {
                    writer.`object`()
                    writer.newLine()

                    if (clue.hasClueNumber()) {
                        writer.indent(3)
                            .key(FIELD_CLUE_NUMBER)
                            .value(clue.clueNumber)
                        writer.newLine()
                    }

                    if (clue.hasLabel()) {
                        writer.indent(3)
                            .key(FIELD_CLUE_LABEL)
                            .value(clue.label)
                        writer.newLine()
                    }

                    writer.indent(3)
                        .key(FIELD_CLUE_HINT)
                        .value(htmlUnicode(clue.hint))
                    writer.newLine()

                    writer.newLine()

                    if (clue.hasZone()) {
                        writer.indent(3)
                            .key(FIELD_CLUE_CELLS)
                            .array()
                        val zone = clue.zone
                        for (pos in zone) {
                            writer.array()
                                .value(pos.col.toLong())
                                .value(pos.row.toLong())
                                .endArray()
                        }
                        writer.endArray()
                        writer.newLine()
                    }
                    writer.indent(2)
                        .endObject()
                } else {
                    if (clue.hasClueNumber()) {
                        writer.array()
                            .value(clue.clueNumber)
                            .value(hint)
                            .endArray()
                    } else {
                        writer.value(hint)
                    }
                }
                writer.newLine()
            }

            writer.indent(1)
                .endArray()
            writer.newLine()
        }

        /**
         * Write Puzzle features not natively supported by IPuz
         */
        @Throws(IOException::class)
        private fun writeExtensions(
            puz : Puzzle,
            writer : FormatableJSONWriter,
            omitPlayState : Boolean,
        ) {
            writeExtensionVolatility(writer)

            writeClueSeparators(puz, writer)

            writer.keyValueNonNull(FIELD_EXT_SUPPORT_URL, puz.supportUrl)
            writer.keyValueNonNull(FIELD_EXT_SHARE_URL, puz.shareUrl)

            if (puz.hasPinnedClueID) {
                writer.key(FIELD_EXT_PINNED_CLUE_ID)
                writeClueID(puz.pinnedClueID, writer)
                writer.newLine()
            }

            writer.keyValueNonNull(FIELD_EXT_IO_VERSION, IO_VERSION)

            writeImages(puz, writer)

            if (!omitPlayState) {
                writer.key(FIELD_EXT_PLAY_DATA)
                    .`object`()
                writer.newLine()

                writeBoxExtras(puz, writer)
                writePosition(puz, writer)
                writeClueHistory(puz, writer)
                writeClueNotes(puz, writer)
                writePlayerNote(puz, writer)
                writeFlaggedClues(puz, writer)

                writer.keyValueNonNull(1, FIELD_COMPLETION_TIME, puz.time)
                    .keyValueNonNull(1, FIELD_PCNT_FILLED, puz.percentFilled)
                    .keyValueNonNull(
                        1,
                        FIELD_PCNT_COMPLETE,
                        puz.percentComplete,
                    ).keyValueNonNull(1, FIELD_RATING, puz.rating.code)

                writer.endObject()
                writer.newLine()
            }
        }

        @Throws(IOException::class)
        private fun writeImages(
            puz : Puzzle,
            writer : FormatableJSONWriter,
        ) {
            val images = puz.images
            if (images.size > 0) {
                writer.key(FIELD_EXT_IMAGES)
                    .array()
                writer.newLine()

                for (image in images) {
                    writer.`object`()
                    writer.keyValueNonNull(1, FIELD_EXT_IMAGE_URL, image.url)
                        .keyValueNonNull(1, FIELD_EXT_IMAGE_ROW, image.row)
                        .keyValueNonNull(1, FIELD_EXT_IMAGE_COL, image.col)
                        .keyValueNonNull(1, FIELD_EXT_IMAGE_WIDTH, image.width)
                        .keyValueNonNull(
                            1,
                            FIELD_EXT_IMAGE_HEIGHT,
                            image.height,
                        )
                        .endObject()
                    writer.newLine()
                }

                writer.endArray()
                writer.newLine()
            }
        }

        /**
         * Add Note objects about individual clues to json
         */
        @Throws(IOException::class)
        private fun writeClueNotes(
            puz : Puzzle,
            writer : FormatableJSONWriter,
        ) {
            writer.indent(1)
                .key(FIELD_CLUE_NOTES)
                .array()
            writer.newLine()

            // Sort so guaranteed output order
            for (listName in puz.clueListNames.sorted()) {
                val clues = puz.getClues(listName)
                if (clues == null)
                    continue

                for (clue in clues) {
                    val cid = clue.clueID
                    val note = puz.getNote(cid)
                    if (note != null && !note.isEmpty) {
                        writer.indent(2)
                            .`object`()
                        writer.newLine()
                            .indent(3)
                            .key(FIELD_CLUE_NOTE_CLUE)
                        writeClueID(cid, writer)
                        writer.newLine()
                            .keyValueNonNull(
                                3,
                                FIELD_NOTE_SCRATCH,
                                note.compressedScratch
                            ).keyValueNonNull(
                                3,
                                FIELD_NOTE_TEXT,
                                htmlString(note.text)
                            ).keyValueNonNull(
                                3,
                                FIELD_NOTE_ANAGRAM_SRC,
                                note.compressedAnagramSource
                            ).keyValueNonNull(
                                3,
                                FIELD_NOTE_ANAGRAM_SOL,
                                note.compressedAnagramSolution
                            )
                        writer.indent(2)
                            .endObject()
                        writer.newLine()
                    }
                }
            }

            writer.indent(1)
                .endArray()
            writer.newLine()
        }

        /**
         * Add player Note object
         */
        @Throws(IOException::class)
        private fun writePlayerNote(
            puz : Puzzle,
            writer : FormatableJSONWriter,
        ) {
            val note = puz.playerNote
            if (note != null && !note.isEmpty) {
                writer.indent(1)
                    .key(FIELD_PLAYER_NOTE)
                    .`object`()
                writer.newLine()
                    .keyValueNonNull(
                        2,
                        FIELD_NOTE_SCRATCH,
                        note.compressedScratch,
                    ).keyValueNonNull(
                        2,
                        FIELD_NOTE_TEXT,
                        htmlString(note.text),
                    ).keyValueNonNull(
                        2,
                        FIELD_NOTE_ANAGRAM_SRC,
                        note.compressedAnagramSource,
                    ).keyValueNonNull(
                        2,
                        FIELD_NOTE_ANAGRAM_SOL,
                        note.compressedAnagramSolution,
                    )
                writer.indent(1)
                    .endObject()
                writer.newLine()
            }
        }

        @Throws(IOException::class)
        private fun writeFlaggedClues(
            puz : Puzzle,
            writer : FormatableJSONWriter,
        ) {
            writer.indent(1)
                .key(FIELD_FLAGGED_CLUES)
                .array()
            writer.newLine()

            // guarantee writing order
            val clues = puz.allClues
                .filter { it.isFlagged }
                .sortedWith { clue1, clue2 ->
                    clue1.clueID.compareTo(clue2.clueID)
                }

            for (clue in clues) {
                val cid = clue.clueID
                writer.indent(2)

                writer.`object`()
                writeClueIDFields(cid, writer)
                if (!clue.isDefaultFlagColor) {
                    writer.key(FIELD_COLOR).value(colorToHex(clue.flagColor))
                }
                writer.endObject()

                writer.newLine()
            }

            writer.indent(1)
                .endArray()
            writer.newLine()
        }

        /**
         * Write a ClueID on one line as a JSONObject
         */
        @Throws(IOException::class)
        private fun writeClueID(
            cid : ClueID?,
            writer : FormatableJSONWriter,
        ) {
            if (cid != null) {
                writer.`object`()
                writeClueIDFields(cid, writer)
                writer.endObject()
            }
        }

        /**
         * Write ClueID fields without wrapping in an object
         */
        @Throws(IOException::class)
        private fun writeClueIDFields(
            cid : ClueID?,
            writer : FormatableJSONWriter,
        ) {
            if (cid != null) {
                writer.key(FIELD_CLUE_LISTNAME).value(cid.listName)
                    .key(FIELD_CLUE_INDEX).value(cid.index.toLong())
            }
        }

        /**
         * Write separators field to extra data
         */
        @Throws(IOException::class)
        private fun writeClueSeparators(
            puz : Puzzle,
            writer : FormatableJSONWriter,
        ) {
            if (!puz.hasClueSeparators())
                return

            writer.key(FIELD_EXT_SEPARATORS)
                .array()
            writer.newLine()

            for (listName in puz.clueListNames) {
                for (clue in puz.getClues(listName)!!) {
                    writeClueSeparators(1, clue, writer)
                }
            }

            writer.endArray()
            writer.newLine()
        }

        /**
         * Write clue separators to writer if has separators
         */
        @Throws(IOException::class)
        private fun writeClueSeparators(
            indent : Int,
            clue : Clue,
            writer : FormatableJSONWriter,
        ) {
            if (!clue.hasSeparators())
                return

            writer.indent(indent)
                .`object`()
            writer.newLine()

            writer.indent(indent + 1)
                .key(FIELD_CLUEID)
            writeClueID(clue.clueID, writer)
            writer.newLine()

            writer.indent(indent + 1)
                .key(FIELD_CLUE_SEPARATORS)
                .`object`()
            writer.newLine()

            for (pos in clue.separatorPositions) {
                writer.indent(indent + 2)
                    .key(pos.toString())
                    .value(clue.getSeparator(pos).toString())
                writer.newLine()
            }

            writer.indent(indent + 1)
                .endObject()
            writer.newLine()

            writer.indent(indent)
                .endObject()
            writer.newLine()
        }

        /**
         * Write clue history list
         */
        @Throws(IOException::class)
        private fun writeClueHistory(
            puz : Puzzle,
            writer : FormatableJSONWriter,
        ) {
            val history = puz.history
            if (history.isEmpty())
                return

            writer.indent(1)
                .key(FIELD_CLUE_HISTORY)
                .array()
            writer.newLine()

            for (item in puz.history) {
                writer.indent(2)
                writeClueID(item, writer)
                writer.newLine()
            }

            writer.indent(1)
                .endArray()
            writer.newLine()
        }

        /**
         * Write current highlight position
         */
        @Throws(IOException::class)
        private fun writePosition(puz : Puzzle, writer : FormatableJSONWriter) {
            val pos = puz.position

            writer.indent(1)
                .key(FIELD_POSITION)
                .`object`()
                .key(FIELD_POSITION_ROW).value(pos.row.toLong())
                .key(FIELD_POSITION_COL).value(pos.col.toLong())
            val cid = puz.currentClueID
            if (cid != null) {
                writer.key(FIELD_CLUEID)
                writeClueID(cid, writer)
            }
            writer.endObject()
            writer.newLine()
        }

        /**
         * Write additional info about boxes (cheated, responder) if any
         */
        @Throws(IOException::class)
        private fun writeBoxExtras(
            puz : Puzzle,
            writer : FormatableJSONWriter,
        ) {
            val hasBoxExtras = (
                puz.hasCheated()
                || puz.hasResponders()
                || puz.hasBoxFlags()
            )

            if (!hasBoxExtras)
                return

            val boxes = puz.boxes

            writer.indent(1)
                .key(FIELD_BOX_EXTRAS)
                .array()
            writer.newLine()

            for (row in boxes.indices) {
                writer.indent(2)
                    .array()
                for (col in boxes[row].indices) {
                    writer.`object`()

                    val box = boxes[row][col]
                    if (box != null && !isBlock(box)) {
                        if (box.isCheated)
                            writer.key(FIELD_BOX_CHEATED).value(true)
                        val responder = box.responder
                        if (responder != null)
                            writer.key(FIELD_BOX_RESPONDER).value(responder)
                        if (box.isFlagged)
                            writer.key(FIELD_BOX_FLAGGED).value(true)
                        if (!box.isDefaultFlagColor()) {
                            writer.key(FIELD_BOX_FLAG_COLOR)
                                .value(colorToHex(box.flagColor))
                        }
                    }

                    writer.endObject()
                }

                writer.endArray()
                writer.newLine()
            }

            writer.indent(1)
                .endArray()
            writer.newLine()
        }

        /**
         * Write volatility info about our extensions to json
         */
        @Throws(IOException::class)
        private fun writeExtensionVolatility(writer : FormatableJSONWriter) {
            writer.key(FIELD_VOLATILE)
                .`object`()
            writer.newLine()

            for (field in VOLATILE_EXTENSIONS)
                writer.keyValueNonNull(1, field, FIELD_IS_VOLATILE)

            for (field in NON_VOLATILE_EXTENSIONS) {
                writer.keyValueNonNull(
                    1,
                    field,
                    FIELD_IS_NOT_VOLATILE,
                )
            }

            writer.endObject()
            writer.newLine()
        }

        /**
         * Returns fully qualified name of an extension field
         */
        private fun getQualifiedExtensionName(fieldName : String?) : String {
            return EXT_NAMESPACE + ":" + fieldName
        }

        /**
         * Read colour
         *
         * Should be a number (DEFINED_COLORS are what we support) or a
         * 6-digit hex bbbbbb of form 0x00rrggbb.
         *
         * Allows some flex. Assumes DEFINED_COLOURS_MAX_DIGITS chars or
         * less is a number or is a hex code. As a backup, falls back to
         * HTML format.
         *
         * Returns null if cannot
         */
        private fun readColor(color : String?) : Int? {
            if (color == null || color.isEmpty())
                return null

            try {
                if (color.length <= DEFINED_COLOURS_MAX_DIGITS) {
                    val colorNum = color.toInt()
                    if (0 <= colorNum && colorNum < DEFINED_COLORS.size)
                        return DEFINED_COLORS[colorNum]
                    // else fall through to backup
                } else {
                    return color.toInt(16)
                }
            } catch (e : NumberFormatException) {
                // fall through to HTML backup
            }
            val iColor = parseHtmlColor(color)
            return if (iColor < 0) null else iColor
        }

        /**
         * Convert a color to dddddd
         */
        private fun colorToHex(color : Int) : String {
            // ignore alpha channel
            return String.format(
                HEX_COLOR_FORMAT,
                color and 0x00ffffff,
            )
        }

        /**
         * Convert json \\u characters into
         *
         * org.json converts certain characters into \\uxxxx notation, which
         * is rejected by Puzzazz. Convert to these into html entity.
         */
        private fun htmlUnicode(s : String?) : String? {
            if (s != null && hasJSONUChar(s)) {
                val sb = StringBuilder()
                for (i in 0..<s.length) {
                    val c = s.get(i)
                    if (isJSONUChar(c)) {
                        // adapted from org.json
                        sb.append("&#x")
                        val hhhh = Integer.toHexString(c.code)
                        sb.append("0000", 0, 4 - hhhh.length)
                        sb.append(hhhh)
                        sb.append(";")
                    } else {
                        sb.append(c)
                    }
                }
                return sb.toString()
            } else {
                return s
            }
        }

        /**
         * If some char in the string would be escaped by org.json
         */
        private fun hasJSONUChar(s : String?) : Boolean {
            if (s == null)
                return false

            for (i in 0..<s.length) {
                if (isJSONUChar(s.get(i)))
                    return true
            }
            return false
        }

        /*
         * If c is a char org.json would escape as \\uxxxx
         *
         * Condition taken from JSONObject implementation
         */
        private fun isJSONUChar(c : Char) : Boolean {
            return (
                c < ' '
                || (c >= '\u0080' && c < '\u00a0')
                || (c >= '\u2000' && c < '\u2100')
            )
        }
    }
}

