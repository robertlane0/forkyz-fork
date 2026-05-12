
package app.crossword.yourealwaysbe.forkyz.puz.io

import java.util.logging.Logger
import kotlinx.datetime.LocalDate
import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.forkyz.puz.util.JSONException
import app.crossword.yourealwaysbe.forkyz.puz.util.JSONObject
import app.crossword.yourealwaysbe.forkyz.puz.util.PuzzleUtils
import app.crossword.yourealwaysbe.forkyz.puz.util.parseJSONObject

/**
 * Puzzle format used by Smart Games
 *
 * "data": {
 *   "attributes": {
 *     "author": "..",
 *     "publicationDate": "yyyy-mm-dd",
 *     "config": {
 *       "board": "AA#A\r\nA#AA\r\n#AAA...", // # block \r\n splits rows
 *       "entries": {
 *         "across": {
 *         "1": {
 *         "col": x, // 0-indexed
 *         "row": y, // 0-indexed
 *         "clue": "hint",
 *         },
 *         ..
 *       },
 *       "down": {
 *       ..
 *       },
 *       "highlighted_cells": [] // dunno format, ignored for now
 *     },
 *   }
 * }
 */
class SmartGamesJSONIO : AbstractJSONIO() {
    class SmartGamesJSONFormatException(msg : String) : Exception(msg)

    @Throws(Exception::class)
    override fun parseInput(input : Source) : Puzzle? {
        return readPuzzle(input)
    }

    companion object {
        private val LOG = Logger.getLogger(
            SmartGamesJSONIO::class.java.getCanonicalName(),
        )

        private val DATE_FORMATTER = LocalDate.Formats.ISO

        private val ACROSS_LIST : String = DefaultListNames.ACROSS_LIST
        private val DOWN_LIST : String = DefaultListNames.DOWN_LIST

        @Throws(IOException::class)
        fun readPuzzle(input : Source) : Puzzle? {
            try {
                val json = parseJSONObject(input)
                return readPuzzleFromJSON(json)
            } catch (e : SmartGamesJSONFormatException) {
                LOG.severe("Could not read SmartGames JSON: " + e)
                return null
            } catch (e : JSONException) {
                LOG.severe("Could not read SmartGames JSON: " + e)
                return null
            }
        }

        fun readPuzzle(jsonString : String) : Puzzle? {
            try {
                val json = parseJSONObject(jsonString)
                return readPuzzleFromJSON(json)
            } catch (e : SmartGamesJSONFormatException) {
                LOG.severe("Could not read SmartGames JSON: " + e)
                return null
            } catch (e : JSONException) {
                LOG.severe("Could not read SmartGames JSON: " + e)
                return null
            }
        }

        @Throws(JSONException::class, SmartGamesJSONFormatException::class)
        private fun readPuzzleFromJSON(json : JSONObject) : Puzzle {
            try {
                var data = json.optJSONObject("data")
                if (data == null)
                    data = json
                val attributes = data.optJSONObject("attributes")
                if (attributes != null)
                    data = attributes

                val builder = PuzzleBuilder(getBoxes(data))
                addMetaData(data, builder)
                addClues(data, builder)

                return builder.puzzle
            } catch (e : IllegalArgumentException) {
                throw SmartGamesJSONFormatException(
                    "Could not set grid boxes from data file: " + e.message,
                )
            }
        }

        @Throws(JSONException::class, SmartGamesJSONFormatException::class)
        private fun getBoxes(json : JSONObject) : Array<Array<Box?>> {
            val config = json.getJSONObject("config")
            val cells : Array<String> = config
                .getString("board")
                .split("\r\n".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()

            val numRows = cells.size
            val numCols = cells
                .map { it.length }
                .maxOrNull() ?: 0

            if (numCols == 0) {
                throw SmartGamesJSONFormatException(
                    "Grid has a bad shape: "
                    + numRows
                    + " rows and "
                    + numCols
                    + " cols.",
                )
            }

            val boxes = Array(numRows) { arrayOfNulls<Box?>(numCols) }
            for (row in 0..<numRows) {
                val len = cells[row].length
                for (col in 0..<len) {
                    cells[row].get(col).let { cell ->
                        if (Character.isAlphabetic(cell.code)) {
                            boxes[row][col] = Box()
                            boxes[row][col]?.setSolution(cell)
                        }
                    }
                }
            }

            return boxes
        }

        @Throws(JSONException::class)
        private fun addMetaData(json : JSONObject?, builder : PuzzleBuilder) {
            if (json != null) {
                builder.setAuthor(json.optStringEmptyNull("author"))
                try {
                    val date = json.optStringEmptyNull("publicationDate")
                    if (date != null)
                        builder.setDate(DATE_FORMATTER.parse(date))
                } catch (e : IllegalArgumentException) {
                    // oh well
                }
            }
        }

        @Throws(JSONException::class)
        private fun addClues(json : JSONObject, builder : PuzzleBuilder) {
            val config = json.getJSONObject("config")
            val clues = config.getJSONObject("entries")
            addClueList(clues.optJSONObject("across"), true, builder)
            addClueList(clues.optJSONObject("down"), false, builder)
        }

        @Throws(JSONException::class)
        private fun addClueList(
            clues : JSONObject?,
            across : Boolean,
            builder : PuzzleBuilder,
        ) {
            if (clues == null)
                return

            clues.keys
                .sortedWith(PuzzleUtils::compareStringNumbers)
                .forEach { number ->
                    val clue = clues.getJSONObject(number)
                    val hint = clue.getString("clue")
                    val row = clue.getInt("row")
                    val col = clue.getInt("col")

                    builder.setBoxClueNumber(row, col, number)
                    if (across) {
                        builder.addAcrossClue(ACROSS_LIST, number, hint)
                    } else {
                        builder.addDownClue(DOWN_LIST, number, hint)
                    }
                }
        }
    }
}

