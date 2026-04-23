
package app.crossword.yourealwaysbe.puz.io

import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.logging.Logger

import org.jsoup.Jsoup

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.PuzImage
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.util.JSONException
import app.crossword.yourealwaysbe.puz.util.JSONObject
import app.crossword.yourealwaysbe.puz.util.parseJSONObject


/**
 * Read crosswords from https://przekroj.pl
 *
 * First interpret io stream as JSON, if that fails, try it as an HTML file and
 * try to extract JSON from it.
 *
 * Does not set title/author/date (not in JSON) unless gets something from HTML
 */
class PrzekrojIO : PuzzleParser {
    /**
     * An unfancy exception indicating error while parsing
     */
    class PrzFormatException(msg : String) : Exception(msg)

    @Throws(Exception::class)
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return readPuzzle(inStream)
    }

    companion object {
        private val LOG
            = Logger.getLogger(PrzekrojIO::class.java.getCanonicalName())

        // Not internationalised, Polish crosswords
        private const val ACROSS_LIST = "Poziomo"
        private const val DOWN_LIST = "Pionowo"

        private const val DEFAULT_SOURCE = "Przekroj"

        @JvmStatic
        @Throws(IOException::class)
        fun readPuzzle(inStream : InputStream) : Puzzle? {
            try {
                val copy = StreamUtils.copyInputStream(inStream)

                var puz : Puzzle? = null
                try {
                    puz = readPuzzleFromJSON(parseJSONObject(copy))
                } catch (e : JSONException) {
                    // fall through
                }

                if (puz == null) {
                    copy.reset()
                    val doc = Jsoup.parse(copy, null, "")
                    val cwJson = doc.select(".crossword")
                        .attr("data-json")
                        .replace("&quot;", "\"")
                    var title : String?
                        = doc.select("article").attr("data-title")
                    if (title == null || title.isEmpty()) {
                        val titleEle = doc.selectFirst("title")
                        if (titleEle != null)
                            title = titleEle.text().trim()
                    }

                    puz = readPuzzleFromJSON(parseJSONObject(cwJson))
                    if (title != null) {
                        puz.let { it.title = title }
                    }
                }

                val source = puz.source
                if (source == null || source.isEmpty())
                    puz.source = DEFAULT_SOURCE

                return puz
            } catch (e : IOException) {
                LOG.info("Could not read Przekroj crossword: " + e)
                return null
            } catch (e : JSONException) {
                LOG.info("Could not read Przekroj crossword: " + e)
                return null
            } catch (e : PrzFormatException) {
                LOG.info("Could not read Przekroj crossword: " + e)
                return null
            }
        }

        @Throws(PrzFormatException::class)
        private fun readPuzzleFromJSON(json : JSONObject) : Puzzle {
            val boxes : Array<Array<Box?>> = readBoxes(json)
            val builder = PuzzleBuilder(boxes)
            addClues(json, builder)
            addImages(json, builder)
            return builder.puzzle
        }

        @Throws(PrzFormatException::class)
        private fun readBoxes(json : JSONObject) : Array<Array<Box?>> {
            val width = json.getInt("x")
            val height = json.getInt("y")

            val boxes = Array<Array<Box?>>(height) {
                arrayOfNulls<Box>(width)
            }

            val questions = json.getJSONArray("questions")
            for (i in 0..<questions.size) {
                val question = questions.getJSONObject(i)
                var row = question.getInt("y") - 1
                var col = question.getInt("x") - 1
                val number = question.getString("n")

                if (boxes[row][col] == null) {
                    boxes[row][col] = Box()
                } else {
                    boxes[row][col]?.let { box ->
                        if (box.hasClueNumber()) {
                            if (number != box.clueNumber) {
                                throw PrzFormatException(
                                    "Clue number mismatch at position "
                                    + row
                                    + " "
                                    + col,
                                )
                            }
                        }
                    }
                }

                boxes[row][col]?.let { it.clueNumber = number }

                val dir = question.getString("d")
                val across = "horizontal" == dir.lowercase(Locale.getDefault())
                val drow = if (across) 0 else 1
                val dcol = if (across) 1 else 0

                val answer = question.getString("a").uppercase(Locale.getDefault())

                for (j in 0..<answer.length) {
                    if (boxes[row][col] == null)
                        boxes[row][col] = Box()
                    boxes[row][col]?.let {
                        it.solution = answer.get(j).toString()
                    }

                    row += drow
                    col += dcol
                }
            }

            return boxes
        }

        private fun addClues(json : JSONObject, builder : PuzzleBuilder) {
            val questions = json.getJSONArray("questions")
            for (i in 0..<questions.size) {
                val question = questions.getJSONObject(i)
                val number = question.getString("n")
                val hint = question.getString("q")
                val dir = question.getString("d")
                val across = "horizontal" == dir.lowercase(Locale.getDefault())

                if (across)
                    builder.addAcrossClue(ACROSS_LIST, number, hint)
                else
                    builder.addDownClue(DOWN_LIST, number, hint)
            }
        }

        private fun addImages(json : JSONObject, builder : PuzzleBuilder) {
            val images = json.optJSONArray("images")
            if (images == null)
                return

            for (i in 0..<images.size) {
                val image = images.getJSONObject(i)
                val url = image.getString("src")
                val row = image.getInt("y") - 1
                val col = image.getInt("x") - 1
                val width = image.getInt("w")
                val height = image.getInt("h")

                if (!url.isEmpty())
                    builder.addImage(PuzImage(url, row, col, width, height))
            }
        }
    }
}

