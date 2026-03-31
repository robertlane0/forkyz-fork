
package app.crossword.yourealwaysbe.puz.io

import java.io.DataOutputStream
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import javax.xml.parsers.SAXParserFactory
import kotlinx.datetime.LocalDate

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.PuzzleBuilder.BasicClue

/**
 * Converts a puzzle from the XML format used by uclick syndicated puzzles
 * to the Across Lite .puz format.  The format is:
 *
 * <crossword>
 *   <Title v="[Title]" />
 *   <Author v="[Author]" />
 *   <Width v="[Width]" />
 *   <Height v="[Height]" />
 *   <AllAnswer v="[Grid]" />
 *   <Copyright = "[Copyright]" />
 *   <across>
 *       <a[i] a="[Answer]" c="[Clue]" n="[GridIndex]" cn="[ClueNumber]" />
 *   </across>
 *   <down>
 *       <d[j] ... />
 *   </down>
 * </crossword>
 *
 * [Grid] contains all of the letters in the solution, reading left-to-right,
 * top-to-bottom, with - for black squares. [i] is an incrementing number for
 * each across clue, starting at 1. [GridIndex] is the offset into [Grid] at
 * which the clue starts.  [Clue] text is HTML escaped.
 */
class UclickXMLIO : PuzzleParser {
    private class UclickXMLParser : DefaultHandler() {
        var title : String? = null
            private set
        var author : String? = null
            private set
        var copyright : String? = null
            private set
        var boxList : Array<Box?>? = null
            private set
        val acrossClues = mutableListOf<BasicClue>()
        val downClues = mutableListOf<BasicClue>()

        private var inAcross = false
        private var inDown = false
        private var maxClueNum = -1
        var width : Int = 0
            private set
        var height : Int = 0
            private set

        val isSuccessfulRead : Boolean
            get() = (
                boxList != null
                && acrossClues.size > 0
                && downClues.size > 0
                && maxClueNum > -1
                && width > 0
                && height > 0
            )

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
            if (inAcross) {
                val clueNum = attributes.getValue("cn")
                val clueNumInt = clueNum.toInt()
                if (clueNumInt > maxClueNum) {
                    maxClueNum = clueNumInt
                }
                acrossClues.add(
                    BasicClue(
                        clueNum,
                        decodeValue(attributes.getValue("c")),
                    )
                )
            } else if (inDown) {
                val clueNum = attributes.getValue("cn")
                val clueNumInt = clueNum.toInt()
                if (clueNumInt > maxClueNum) {
                    maxClueNum = clueNumInt
                }
                downClues.add(
                    BasicClue(
                        clueNum,
                        decodeValue(attributes.getValue("c")),
                    ),
                )
            } else if (name.equals("title", ignoreCase = true)) {
                title = decodeValue(attributes.getValue("v"))
            } else if (name.equals("author", ignoreCase = true)) {
                author = decodeValue(attributes.getValue("v"))
            } else if (name.equals("copyright", ignoreCase = true)) {
                copyright = decodeValue(attributes.getValue("v"))
            } else if (name.equals("width", ignoreCase = true)) {
                width = attributes.getValue("v").toInt()
            } else if (name.equals("height", ignoreCase = true)) {
                height = attributes.getValue("v").toInt()
            } else if (name.equals("allanswer", ignoreCase = true)) {
                val rawGrid = attributes.getValue("v")
                boxList = arrayOfNulls<Box>(height * width)
                for (i in 0..<rawGrid.length) {
                    val sol = rawGrid.get(i)
                    if (sol != '-') {
                        val box = Box()
                        box.setSolution(sol)
                        box.setBlank()
                        boxList?.set(i, box)
                    }
                }
            } else if (name.equals("across", ignoreCase = true)) {
                inAcross = true
            } else if (name.equals("down", ignoreCase = true)) {
                inDown = true
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

            if (name.equals("across", ignoreCase = true)) {
                inAcross = false
            } else if (name.equals("down", ignoreCase = true)) {
                inDown = false
            }
        }

        fun decodeValue(value : String) : String {
            try {
                return URLDecoder.decode(value, CHARSET_NAME)
            } catch (e : UnsupportedEncodingException) {
                return value
            }
        }
    }

    override fun parseInput(inStream : InputStream) : Puzzle? {
        return parsePuzzle(inStream)
    }

    companion object {
        private const val CHARSET_NAME = "utf8"
        private val ACROSS_LIST : String = DefaultListNames.ACROSS_LIST
        private val DOWN_LIST : String = DefaultListNames.DOWN_LIST

        @JvmStatic
        fun parsePuzzle(inStream : InputStream?) : Puzzle? {
            val factory = SAXParserFactory.newInstance()
            try {
                val parser = factory.newSAXParser()
                val xr = parser.getXMLReader()
                val handler = UclickXMLParser()
                xr.setContentHandler(handler)
                xr.parse(InputSource(inStream))

                if (!handler.isSuccessfulRead)
                    return null

                val boxList = handler.boxList
                if (boxList == null)
                    return null

                val builder = PuzzleBuilder(
                    boxList,
                    handler.width,
                    handler.height,
                )
                builder.autoNumberBoxes()
                    .setTitle(handler.title)
                    .setAuthor(handler.author)
                    .setCopyright(handler.copyright)
                    .setNotes("")

                for (basicClue in handler.acrossClues)
                    builder.addAcrossClue(ACROSS_LIST, basicClue)

                for (basicClue in handler.downClues)
                    builder.addDownClue(DOWN_LIST, basicClue)

                return builder.puzzle
            } catch (e : Exception) {
                return null
            }
        }

        fun convertUclickPuzzle(
            inStream : InputStream?, os : DataOutputStream?,
            copyright : String?, d : LocalDate?
        ) : Boolean {
            val puz = parsePuzzle(inStream)
            if (puz == null)
                return false

            puz.date = d
            if (copyright != null)
                puz.copyright = copyright

            try {
                IO.saveNative(puz, os)
                return true
            } catch (e : Exception) {
                e.printStackTrace()
                System.err.println("Unable to save puzzle: " + e.message)
                return false
            }
        }
    }
}

