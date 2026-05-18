
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlin.math.log10
import kotlin.math.min
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.Clue
import app.crossword.yourealwaysbe.forkyz.puz.ClueID
import app.crossword.yourealwaysbe.forkyz.puz.Note
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.PuzImage
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleKind
import app.crossword.yourealwaysbe.forkyz.puz.Zone
import app.crossword.yourealwaysbe.forkyz.puz.util.PuzzleUtils
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.HtmlUtil.htmlString
import app.crossword.yourealwaysbe.forkyz.util.HtmlUtil.parseHtmlColor
import app.crossword.yourealwaysbe.forkyz.util.HtmlUtil.unHtmlString
import app.crossword.yourealwaysbe.forkyz.util.JSONArray
import app.crossword.yourealwaysbe.forkyz.util.JSONArrayBuilder
import app.crossword.yourealwaysbe.forkyz.util.JSONException
import app.crossword.yourealwaysbe.forkyz.util.JSONObject
import app.crossword.yourealwaysbe.forkyz.util.JSONObjectBuilder
import app.crossword.yourealwaysbe.forkyz.util.buildJSONArray
import app.crossword.yourealwaysbe.forkyz.util.buildJSONObject
import app.crossword.yourealwaysbe.forkyz.util.parseJSONObject
import app.crossword.yourealwaysbe.forkyz.util.writeJSONObject

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
    override suspend fun parseInput(input : Source) : Puzzle? {
        return readPuzzle(input)
    }

    companion object {
        private val TAG = "ForkyzIPuzIO"

        // version 1 was not tagged in file
        private const val IO_VERSION = 3

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

        @Throws(IOException::class)
        fun readPuzzle(input: Source) : Puzzle? {
            try {
                val json = parseJSONObject(input)

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
                AppLogger.e(TAG, "Could not read IPuz file: " + e)
                return null
            } catch (e : JSONException) {
                AppLogger.e(TAG, "Could not read IPuz file: " + e)
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
            for (i in 0..<kinds.size) {
                val kind = kinds.getString(i).lowercase()

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
            builder.setTitle(puzJson.optStringEmptyNull(FIELD_TITLE))
                .setAuthor(puzJson.optStringEmptyNull(FIELD_AUTHOR))
                .setCopyright(puzJson.optStringEmptyNull(FIELD_COPYRIGHT))
                .setIntroMessage(puzJson.optStringEmptyNull(FIELD_INTRO))
                .setNotes(puzJson.optStringEmptyNull(FIELD_NOTES))
                .setCompletionMessage(
                    puzJson.optStringEmptyNull(FIELD_EXPLANATION),
                ).setSourceUrl(puzJson.optStringEmptyNull(FIELD_URL))
                .setSource(puzJson.optStringEmptyNull(FIELD_PUBLISHER))
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
            val date = puzJson.optStringEmptyNull(FIELD_DATE)
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
            val value = json.optStringEmptyNull(field)

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

            if (cells.size < boxes.size) {
                throw IPuzFormatException(
                    "Number of cell rows doesn't match boxes dimensions"
                )
            }

            val block : String = getBlockString(puzJson)
            val empty : String = getEmptyCellString(puzJson)
            val namedStyles = puzJson.optJSONObject(FIELD_NAMED_STYLES)

            for (row in boxes.indices) {
                val rowCells = cells.getJSONArray(row)

                if (rowCells.size < boxes[row].size) {
                    throw IPuzFormatException(
                        "Number of cell columns doesn't match boxes dimension"
                    )
                }

                for (col in boxes[row].indices) {
                    boxes[row][col] = getBoxFromObj(
                        rowCells.opt(col),
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
            if (cell == null) {
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
                val initVal = json.optStringEmptyNull(FIELD_VALUE)
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
                .lowercase()

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
            if (style.optBoolean(FIELD_HIGHLIGHT, false)) {
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

            var barred = style.optStringEmptyNull(barField)
            if (barred != null) {
                barred = barred.uppercase()
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
            marks[0][0] = markObj.optStringEmptyNull(FIELD_MARK_TOP_LEFT)
            marks[0][1] = markObj.optStringEmptyNull(FIELD_MARK_TOP)
            marks[0][2] = markObj.optStringEmptyNull(FIELD_MARK_TOP_RIGHT)
            marks[1][0] = markObj.optStringEmptyNull(FIELD_MARK_LEFT)
            marks[1][1] = markObj.optStringEmptyNull(FIELD_MARK_CENTER)
            marks[1][2] = markObj.optStringEmptyNull(FIELD_MARK_RIGHT)
            marks[2][0] = markObj.optStringEmptyNull(FIELD_MARK_BOTTOM_LEFT)
            marks[2][1] = markObj.optStringEmptyNull(FIELD_MARK_BOTTOM)
            marks[2][2] = markObj.optStringEmptyNull(FIELD_MARK_BOTTOM_RIGHT)

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
            if (saved != null && saved.size > 0)
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
            if (solution != null && solution.size > 0)
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
            val height = min(cells.size, boxes.size)

            for (row in 0..<height) {
                val rowCells = cells.getJSONArray(row)

                val width = min(rowCells.size, boxes[row].size)

                for (col in 0..<width) {
                    val value : String? = getCrosswordValueFromObj(
                        rowCells.opt(col),
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
            if (cell == null) {
                return null
            } else if (cell is JSONArray) {
                val values = cell
                if (values.size != 1) {
                    throw IPuzFormatException(
                        "Multiple cell values not supported: " + values,
                    )
                }
                return getCrosswordValueFromObj(values.opt(0), block, empty)
            } else if (cell is JSONObject) {
                val value = cell.optStringEmptyNull(FIELD_VALUE)
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
            if (clues == null)
                return

            val names = clues.keys
            for (listName in names) {
                addClues(
                    clues.getJSONArray(listName),
                    listName,
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

            for (i in 0..<separators.size) {
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
                    sepList.keys.forEach { pos ->
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
                if (clues != null) {
                    for (listName in clues.keys) {
                        val clueList = clues.getJSONArray(listName)
                        for (j in 0..<clueList.size) {
                            val clueObj = clueList.opt(j)
                            if (clueObj is JSONObject) {
                                val clueJson = clueObj
                                val cells = clueJson.optJSONArray(
                                    FIELD_CLUE_CELLS,
                                )
                                if (cells != null) {
                                    for (k in 0..<cells.size) {
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
         * @param separators map from clue id to separators at positions
         * read from
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
                for (i in 0..<jsonClues.size) {
                    val clueSeparators = separators?.get(ClueID(displayName, i))

                    val clueObj = jsonClues.opt(i)
                    if (clueObj != null) {
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
                if (clueArray.size != 2) {
                    throw IPuzFormatException(
                        "Unexpected clue array length: " + clueArray.size,
                    )
                }
                val clueNumObj = clueArray.opt(0)
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

                val label = clueJson.optStringEmptyNull(FIELD_CLUE_LABEL)

                // build hint, bake in additional info
                val hint = StringBuilder()

                hint.append(clueJson.getString(FIELD_CLUE_HINT))

                val conts = clueJson.optJSONArray(FIELD_CLUE_CONTINUED)
                if (conts != null && conts.size > 0) {
                    addCrossRefList(hint, conts, "cont.")
                }

                val refs = clueJson.optJSONArray(FIELD_CLUE_REFERENCES)
                if (refs != null && refs.size > 0) {
                    addCrossRefList(hint, refs, "ref.")
                }

                val enumeration = if (showEnumerations)
                    clueJson.optStringEmptyNull(FIELD_ENUMERATION)
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
                        + clueObj::class
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

            for (i in 0..<cells.size) {
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
            for (i in 0..<refs.size) {
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
         * @param enumeration null or empty if no enumeration to be
         * shown in clue
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
                "Unrecognised clue number format: " + clueNumObj::class,
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
                for (i in 0 until clueNumObj.size) {
                    val subNumObj = clueNumObj.opt(i)
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

                for (i in 0..<objs.size) {
                    if (i > 0) builder.append("/")
                    builder.append(getComplexClueNumString(objs.opt(i)))
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
            val supportUrl = puzJson.optStringEmptyNull(FIELD_EXT_SUPPORT_URL)
            if (supportUrl != null)
                builder.setSupportUrl(supportUrl)

            val shareUrl = puzJson.optStringEmptyNull(FIELD_EXT_SHARE_URL)
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
            if (playData != null)
                Companion.readPlayData(playData, builder)
        }

        private fun readImages(puzJson : JSONObject, builder : PuzzleBuilder) {
            val images = puzJson.optJSONArray(FIELD_EXT_IMAGES)
            if (images == null)
                return

            for (i in 0..<images.size) {
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

            val numRows = min(cellsJson.size, builder.height)

            for (row in 0..<numRows) {
                val rowJson = cellsJson.getJSONArray(row)

                val numCols = min(rowJson.size, builder.width)

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
         * Assumes builder already provides a puzzle with boxes and
         * clues set up.
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
                } else {
                    if (positionJson.has(FIELD_CLUEID)) {
                        val cidJson = positionJson.getJSONObject(FIELD_CLUEID)
                        builder.setCurrentClueID(decodeClueID(cidJson, builder))
                    } else {
                        builder.setCurrentClueID(null)
                    }
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

            for (i in 0..<historyJson.size) {
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

            for (i in 0..<notesJson.size) {
                val noteJson = notesJson.getJSONObject(i)
                val cndJson = noteJson.optJSONObject(FIELD_CLUE_NOTE_CLUE)
                val cid : ClueID? = decodeClueID(cndJson, builder)

                if (cid != null) {
                    val scratch = noteJson.optString(FIELD_NOTE_SCRATCH)
                    val text = unHtmlOptString(noteJson, FIELD_NOTE_TEXT)
                    val anagramSrc = noteJson.optString(FIELD_NOTE_ANAGRAM_SRC)
                    val anagramSol = noteJson.optString(FIELD_NOTE_ANAGRAM_SOL)

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

            val scratch = noteJson.optString(FIELD_NOTE_SCRATCH)
            val text = unHtmlOptString(noteJson, FIELD_NOTE_TEXT)
            val anagramSrc = noteJson.optString(FIELD_NOTE_ANAGRAM_SRC)
            val anagramSol = noteJson.optString(FIELD_NOTE_ANAGRAM_SOL)

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

            for (i in 0..<flagsJson.size) {
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
            if (cid == null)
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
         * Write puzzle to os using UTF8
         */
        @Throws(IOException::class)
        fun writePuzzle(
            puz : Puzzle,
            output : Sink,
            omitPlayState : Boolean = false,
            omitExtensions : Boolean = false,
        ) {
            val json = buildJSONObject {
                writeIPuzHeader(puz)
                writeMetaData(puz)
                writeBoxes(puz, omitPlayState)
                writeClues(puz)

                if (!omitExtensions)
                    writeExtensions(puz, omitPlayState)
            }
            writeJSONObject(json, output)
        }

        /**
         * Write IPuz version and kind
         */
        @Throws(IPuzFormatException::class)
        private fun JSONObjectBuilder.writeIPuzHeader(puz : Puzzle) {
            put(FIELD_VERSION, WRITE_VERSION)

            val kinds = buildJSONArray {
                when (puz.kind) {
                    PuzzleKind.CROSSWORD -> add(WRITE_KIND)
                    PuzzleKind.ACROSTIC -> add(WRITE_KIND_ACROSTIC)
                    else -> {
                        throw IPuzFormatException(
                            "Unrecognised puzzle kind ${puz.kind}",
                        )
                    }
                }
            }
            put(FIELD_KIND, kinds)
        }

        /**
         * Add basic puzzle metadata (title, author, etc).
         */
        private fun JSONObjectBuilder.writeMetaData(puz : Puzzle) {
            put(FIELD_TITLE, htmlUnicode(puz.title))
            put(FIELD_AUTHOR, htmlUnicode(puz.author))
            put(FIELD_COPYRIGHT, htmlUnicode(puz.copyright))
            put(FIELD_INTRO, htmlUnicode(puz.introMessage))
            put(FIELD_NOTES, htmlUnicode(puz.notes))
            put(FIELD_EXPLANATION, htmlUnicode(puz.completionMessage))
            put(FIELD_URL, puz.sourceUrl)
            put(FIELD_PUBLISHER, puz.source)

            puz.date?.let { date ->
                put(FIELD_DATE, DATE_FORMATTER.format(date))
            }
        }

        /**
         * Read all IPuz supported box information to json
         */
        private fun JSONObjectBuilder.writeBoxes(
            puz : Puzzle,
            omitPlayState : Boolean,
        ) {
            writeDimensions(puz)
            writePuzzleCells(puz)
            if (!omitPlayState)
                writeSaved(puz)
            writeSolution(puz)
        }

        /**
         * Add puzzle dimensions to json
         */
        private fun JSONObjectBuilder.writeDimensions(puz : Puzzle) {
            put(
                FIELD_DIMENSIONS,
                buildJSONObject {
                    put(FIELD_WIDTH, puz.width.toLong())
                    put(FIELD_HEIGHT, puz.height.toLong())
                },
            )
        }

        /**
         * Add the puzzle field to json
         */
        private fun JSONObjectBuilder.writePuzzleCells(puz : Puzzle) {
            val boxes : Array<Array<Box?>> = puz.boxes

            val cellsArray = buildJSONArray {
                for (row in boxes) {
                    val rowArray = buildJSONArray {
                        for (cell in row) {
                            writePuzzleCell(cell)
                        }
                    }
                    add(rowArray)
                }
            }

            put(FIELD_PUZZLE, cellsArray)
        }

        private fun JSONArrayBuilder.writePuzzleCell(box : Box?) {
            if (box == null) {
                add(DEFAULT_BLOCK)
            } else {
                val cellContents = if (isBlock(box))
                    DEFAULT_BLOCK
                else
                    box.clueNumber
                if (isCellWithStyle(box) || box.hasInitialValue()) {
                    val cellObj = buildJSONObject {
                        writeCellStyle(box)

                        // written as style label for blocks
                        if (!isBlock(box) && box.hasInitialValue()) {
                            put(FIELD_VALUE, box.initialValue)
                        }

                        if (cellContents != null) {
                            put(FIELD_CELL, cellContents)
                        } else {
                            put(FIELD_CELL, DEFAULT_EMPTY_WRITE.toLong())
                        }
                    }

                    add(cellObj)
                } else if (cellContents != null) {
                    add(cellContents)
                } else {
                    add(DEFAULT_EMPTY_WRITE.toLong())
                }
            }
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

        private fun JSONObjectBuilder.writeCellStyle(box : Box) {
            if (!isCellWithStyle(box))
                return

            val styleObj = buildJSONObject {
                val shape = SHAPE_BGS_REV.get(box.shape)
                put(FIELD_SHAPE_BG, shape)

                // i say blocks have labels rather than values
                if (isBlock(box) && box.hasInitialValue()) {
                    put(FIELD_LABEL, box.initialValue)
                }

                writeColorFields(box)
                writeBarredFields(box)
                writeMarkField(box)
            }

            put(FIELD_STYLE, styleObj)
        }

        private fun JSONObjectBuilder.writeColorFields(box : Box) {
            if (box.hasColor()) {
                put(FIELD_COLOR, colorToHex(box.color))
            }
            if (box.hasTextColor()) {
                put(FIELD_TEXT_COLOR, colorToHex(box.textColor))
            }
            if (box.hasBarColor()) {
                put(FIELD_BAR_COLOR, colorToHex(box.barColor))
            }
        }

        private fun JSONObjectBuilder.writeBarredFields(box : Box) {
            if (!box.hasBars())
                return

            writeBarredField(box, FIELD_BARRED, Box.Bar.SOLID)
            writeBarredField(box, FIELD_BARRED_DASHED, Box.Bar.DASHED)
            writeBarredField(box, FIELD_BARRED_DOTTED, Box.Bar.DOTTED)
        }

        private fun JSONObjectBuilder.writeBarredField(
            box : Box,
            barField : String,
            barStyle : Box.Bar?,
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
                put(barField, barred)
        }

        private fun JSONObjectBuilder.writeMarkField(box : Box) {
            if (box.hasMarks()) {
                val marksObj = buildJSONObject {
                    box.marks?.let { marks ->
                        put(FIELD_MARK_TOP_LEFT, marks[0][0])
                        put(FIELD_MARK_TOP, marks[0][1])
                        put(FIELD_MARK_TOP_RIGHT, marks[0][2])
                        put(FIELD_MARK_LEFT, marks[1][0])
                        put(FIELD_MARK_CENTER, marks[1][1])
                        put(FIELD_MARK_RIGHT, marks[1][2])
                        put(FIELD_MARK_BOTTOM_LEFT, marks[2][0])
                        put(FIELD_MARK_BOTTOM, marks[2][1])
                        put(FIELD_MARK_BOTTOM_RIGHT, marks[2][2])
                    }
                }

                put(FIELD_MARK, marksObj)
            }
        }

        /**
         * Add the saved field to json
         */
        private fun JSONObjectBuilder.writeSaved(puz : Puzzle) {
            val boxes : Array<Array<Box?>> = puz.boxes

            val savedArr = buildJSONArray {
                for (row in boxes) {
                    val rowArr = buildJSONArray {
                        for (box in row) {
                            if (box == null || isBlock(box))
                                add(DEFAULT_BLOCK)
                            else if (box.isBlank())
                                add(DEFAULT_EMPTY_WRITE.toLong())
                            else
                                add(box.response)
                        }
                    }
                    add(rowArr)
                }
            }

            put(FIELD_SAVED, savedArr)
        }

        /**
         * Add the solution field to json if the puzzle has one
         */
        private fun JSONObjectBuilder.writeSolution(puz : Puzzle) {
            if (!puz.hasSolution())
                return

            val boxes : Array<Array<Box?>> = puz.boxes

            val solutionArr = buildJSONArray {
                for (row in boxes) {
                    val rowArr = buildJSONArray {
                        for (box in row) {
                            if (box == null || isBlock(box)) {
                                add(DEFAULT_BLOCK)
                            } else if (box.hasSolution()) {
                                add(box.solution.toString())
                            } else {
                                addNull()
                            }
                        }
                    }
                    add(rowArr)
                }
            }

            put(FIELD_SOLUTION, solutionArr)
        }

        /**
         * Add the clues lists to the json
         */
        private fun JSONObjectBuilder.writeClues(puz : Puzzle) {
            val cluesObj = buildJSONObject {
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
                        writeClueList(fullListName, clues, zones)
                    } else {
                        // assume unrecognised fields are all Clues:xxx
                        var fullListName = listName
                        if (FIELD_CLUES_CLUES != listName)
                            fullListName = FIELD_CLUES_CLUES + ":" + listName
                        writeClueList(fullListName, clues, false)
                    }
                }
            }

            put(FIELD_CLUES, cluesObj)
        }

        /**
         * Convert a clues list into a json array and return it
         */
        @Throws(IOException::class)
        private fun JSONObjectBuilder.writeClueList(
            fieldName : String,
            clues : Iterable<Clue>,
            withZones : Boolean,
        ) {
            val cluesArr = buildJSONArray {
                for (clue in clues) {
                    val hint = clue.hint

                    if (withZones || clue.hasLabel()) {
                        val clueObj = buildJSONObject {
                            if (clue.hasClueNumber()) {
                                put(FIELD_CLUE_NUMBER, clue.clueNumber)
                            }

                            if (clue.hasLabel()) {
                                put(FIELD_CLUE_LABEL, clue.label)
                            }

                            put(FIELD_CLUE_HINT, htmlUnicode(clue.hint))

                            if (clue.hasZone()) {
                                val zoneArr = buildJSONArray {
                                    val zone = clue.zone
                                    for (pos in zone) {
                                        val posArr = buildJSONArray {
                                            add(pos.col.toLong())
                                            add(pos.row.toLong())
                                        }
                                        add(posArr)
                                    }
                                }
                                put(FIELD_CLUE_CELLS, zoneArr)
                            }
                        }
                        add(clueObj)
                    } else {
                        if (clue.hasClueNumber()) {
                            val clueArr = buildJSONArray {
                                add(clue.clueNumber)
                                add(hint)
                            }
                            add(clueArr)
                        } else {
                            add(hint)
                        }
                    }
                }
            }

            put(fieldName, cluesArr)
        }

        /**
         * Write Puzzle features not natively supported by IPuz
         */
        @Throws(IOException::class)
        private fun JSONObjectBuilder.writeExtensions(
            puz : Puzzle,
            omitPlayState : Boolean,
        ) {
            writeExtensionVolatility()

            writeClueSeparators(puz)

            put(FIELD_EXT_SUPPORT_URL, puz.supportUrl)
            put(FIELD_EXT_SHARE_URL, puz.shareUrl)

            if (puz.hasPinnedClueID) {
                puz.pinnedClueID?.let { cid ->
                    put(
                        FIELD_EXT_PINNED_CLUE_ID,
                        getClueIDJSONObject(cid),
                    )
                }
            }

            put(FIELD_EXT_IO_VERSION, IO_VERSION)

            writeImages(puz)

            if (!omitPlayState) {
                val playDataObj = buildJSONObject {
                    writeBoxExtras(puz)
                    writePosition(puz)
                    writeClueHistory(puz)
                    writeClueNotes(puz)
                    writePlayerNote(puz)
                    writeFlaggedClues(puz)

                    put(FIELD_COMPLETION_TIME, puz.time)
                    put(FIELD_PCNT_FILLED, puz.percentFilled)
                    put(FIELD_PCNT_COMPLETE, puz.percentComplete)
                    put(FIELD_RATING, puz.rating.code)
                }
                put(FIELD_EXT_PLAY_DATA, playDataObj)
            }
        }

        private fun JSONObjectBuilder.writeImages(puz : Puzzle) {
            val images = puz.images
            if (images.size > 0) {
                val imagesArr = buildJSONArray {
                    for (image in images) {
                        val imageObj = buildJSONObject {
                            put(FIELD_EXT_IMAGE_URL, image.url)
                            put(FIELD_EXT_IMAGE_ROW, image.row)
                            put(FIELD_EXT_IMAGE_COL, image.col)
                            put(FIELD_EXT_IMAGE_WIDTH, image.width)
                            put(FIELD_EXT_IMAGE_HEIGHT, image.height)
                        }
                        add(imageObj)
                    }
                }
                put(FIELD_EXT_IMAGES, imagesArr)
            }
        }

        /**
         * Add Note objects about individual clues to json
         */
        private fun JSONObjectBuilder.writeClueNotes(puz : Puzzle) {
            val clueNotesArr = buildJSONArray {
                // Sort so guaranteed output order
                for (listName in puz.clueListNames.sorted()) {
                    val clues = puz.getClues(listName)
                    if (clues == null)
                        continue

                    for (clue in clues) {
                        val cid = clue.clueID
                        val note = puz.getNote(cid)
                        if (note != null && !note.isEmpty) {
                            val noteObj = buildJSONObject {
                                put(
                                    FIELD_CLUE_NOTE_CLUE,
                                    getClueIDJSONObject(cid),
                                )
                                put(FIELD_NOTE_SCRATCH, note.compressedScratch)
                                put(FIELD_NOTE_TEXT, htmlString(note.text))
                                put(
                                    FIELD_NOTE_ANAGRAM_SRC,
                                    note.compressedAnagramSource,
                                )
                                put(
                                    FIELD_NOTE_ANAGRAM_SOL,
                                    note.compressedAnagramSolution,
                                )
                            }
                            add(noteObj)
                        }
                    }
                }
            }

            put(FIELD_CLUE_NOTES, clueNotesArr)
        }

        /**
         * Add player Note object
         */
        private fun JSONObjectBuilder.writePlayerNote(puz : Puzzle) {
            val note = puz.playerNote
            if (note != null && !note.isEmpty) {
                val noteObj = buildJSONObject {
                    put(FIELD_NOTE_SCRATCH, note.compressedScratch)
                    put(FIELD_NOTE_TEXT, htmlString(note.text))
                    put(FIELD_NOTE_ANAGRAM_SRC, note.compressedAnagramSource)
                    put(FIELD_NOTE_ANAGRAM_SOL, note.compressedAnagramSolution)
                }
                put(FIELD_PLAYER_NOTE, noteObj)
            }
        }

        private fun JSONObjectBuilder.writeFlaggedClues(puz : Puzzle) {
            val flaggedArr = buildJSONArray {
                // guarantee writing order
                val clues = puz.allClues
                    .filter { it.isFlagged }
                    .sortedWith { clue1, clue2 ->
                        clue1.clueID.compareTo(clue2.clueID)
                    }

                for (clue in clues) {
                    val cid = clue.clueID
                    val flagObj = buildJSONObject {
                        writeClueIDFields(cid)
                        if (!clue.isDefaultFlagColor) {
                            put(FIELD_COLOR, colorToHex(clue.flagColor))
                        }
                    }
                    add(flagObj)
                }
            }

            put(FIELD_FLAGGED_CLUES, flaggedArr)
        }

        /**
         * Get a ClueID as a JSONObject
         */
        private fun getClueIDJSONObject(cid : ClueID) : JSONObject {
            return buildJSONObject { writeClueIDFields(cid) }
        }

        /**
         * Write ClueID fields without wrapping in an object
         */
        private fun JSONObjectBuilder.writeClueIDFields(cid : ClueID?) {
            if (cid != null) {
                put(FIELD_CLUE_LISTNAME, cid.listName)
                put(FIELD_CLUE_INDEX, cid.index.toLong())
            }
        }

        /**
         * Write separators field to extra data
         */
        private fun JSONObjectBuilder.writeClueSeparators(puz : Puzzle) {
            if (!puz.hasClueSeparators())
                return

            val separatorsArr = buildJSONArray {
                for (listName in puz.clueListNames) {
                    for (clue in puz.getClues(listName)!!) {
                        writeClueSeparators(clue)
                    }
                }
            }

            put(FIELD_EXT_SEPARATORS, separatorsArr)
        }

        /**
         * Write clue separators to writer if has separators
         */
        private fun JSONArrayBuilder.writeClueSeparators(
            clue : Clue,
        ) {
            if (!clue.hasSeparators())
                return

            val clueSeparatorsObj = buildJSONObject {
                put(FIELD_CLUEID, getClueIDJSONObject(clue.clueID))

                val separatorsObj = buildJSONObject {
                    for (pos in clue.separatorPositions) {
                        put(pos.toString(), clue.getSeparator(pos).toString())
                    }
                }
                put(FIELD_CLUE_SEPARATORS, separatorsObj)
            }

            add(clueSeparatorsObj)
        }

        /**
         * Write clue history list
         */
        private fun JSONObjectBuilder.writeClueHistory(puz : Puzzle) {
            val history = puz.history
            if (history.isEmpty())
                return

            val historyArr = buildJSONArray {
                for (item in puz.history) {
                    add(getClueIDJSONObject(item))
                }
            }
            put(FIELD_CLUE_HISTORY, historyArr)
        }

        /**
         * Write current highlight position
         */
        private fun JSONObjectBuilder.writePosition(puz : Puzzle) {
            val pos = puz.position
            val posObj = buildJSONObject {
                put(FIELD_POSITION_ROW, pos.row.toLong())
                put(FIELD_POSITION_COL, pos.col.toLong())
                val cid = puz.currentClueID
                if (cid != null) {
                    put(FIELD_CLUEID, getClueIDJSONObject(cid))
                }
            }
            put(FIELD_POSITION, posObj)
        }

        /**
         * Write additional info about boxes (cheated, responder) if any
         */
        private fun JSONObjectBuilder.writeBoxExtras(puz : Puzzle) {
            val hasBoxExtras = (
                puz.hasCheated()
                || puz.hasResponders()
                || puz.hasBoxFlags()
            )

            if (!hasBoxExtras)
                return

            val boxes = puz.boxes

            val boxExtrasArr = buildJSONArray {
                for (row in boxes) {
                    val rowArr = buildJSONArray {
                        for (box in row) {
                            val boxObj = buildJSONObject {
                                if (box != null && !isBlock(box)) {
                                    if (box.isCheated)
                                        put(FIELD_BOX_CHEATED, true)
                                    put(FIELD_BOX_RESPONDER, box.responder)
                                    if (box.isFlagged)
                                        put(FIELD_BOX_FLAGGED, true)
                                    if (!box.isDefaultFlagColor()) {
                                        put(
                                            FIELD_BOX_FLAG_COLOR,
                                            colorToHex(box.flagColor),
                                        )
                                    }
                                }
                            }

                            add(boxObj)
                        }
                    }

                    add(rowArr)
                }
            }

            put(FIELD_BOX_EXTRAS, boxExtrasArr)
        }

        /**
         * Write volatility info about our extensions to json
         */
        private fun JSONObjectBuilder.writeExtensionVolatility() {
            val volatilityObj = buildJSONObject {
                for (field in VOLATILE_EXTENSIONS)
                    put(field, FIELD_IS_VOLATILE)
                for (field in NON_VOLATILE_EXTENSIONS)
                    put(field, FIELD_IS_NOT_VOLATILE)
            }
            put(FIELD_VOLATILE, volatilityObj)
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
            return color.toString(16).padStart(HEX_CODE_LEN, '0')
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
                        val hhhh = c.code.toString(16)
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

