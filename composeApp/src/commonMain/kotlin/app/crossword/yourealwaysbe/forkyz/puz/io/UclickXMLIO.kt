
package app.crossword.yourealwaysbe.forkyz.puz.io

import io.ktor.http.decodeURLQueryComponent
import kotlinx.datetime.LocalDate
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder.BasicClue
import app.crossword.yourealwaysbe.forkyz.puz.util.XMLElement
import app.crossword.yourealwaysbe.forkyz.puz.util.parseXMLElement

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
    private class UclickXMLParser(content : XMLElement) {
        var title : String? = null
            private set
        var author : String? = null
            private set
        var copyright : String? = null
            private set
        var boxList : Array<Box?>? = null
            private set
        val acrossClues : MutableList<BasicClue> = mutableListOf()
        val downClues : MutableList<BasicClue> = mutableListOf()
        var width : Int = 0
            private set
        var height : Int = 0
            private set
        var maxClueNum : Int = -1
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

        init {
            readMetadata(content)
            readBoxes(content)
            readClues(content)
        }

        private fun readMetadata(content : XMLElement) {
            content.selectFirst("Title")?.attr("v")
                ?.let { title = decodeValue(it) }
            content.selectFirst("Author")?.attr("v")
                ?.let { author = decodeValue(it) }
            content.selectFirst("Copyright")?.attr("v")
                ?.let { copyright = decodeValue(it) }
            content.selectFirst("Width")?.attr("v")?.toIntOrNull()
                ?.let { width = it }
            content.selectFirst("Height")?.attr("v")?.toIntOrNull()
                ?.let { height = it }
        }

        private fun readBoxes(content : XMLElement) {
            content.selectFirst("AllAnswer")?.attr("v")?.let { rawGrid ->
                val list = arrayOfNulls<Box>(height * width)
                for (i in rawGrid.indices) {
                    if (i >= height * width) break
                    val sol = rawGrid[i]
                    if (sol != '-') {
                        list[i] = Box().apply {
                            setSolution(sol)
                            setBlank()
                        }
                    }
                }
                boxList = list
            }
        }

        private fun readClue(
            clue : XMLElement,
            clues : MutableList<BasicClue>,
        ) {
            val cn = clue.attr("cn")
            cn.toIntOrNull()
                ?.let { if (it > maxClueNum) maxClueNum = it }
            clues.add(BasicClue(cn, decodeValue(clue.attr("c"))))
        }

        private fun readClues(content : XMLElement) {
            content.selectFirst("across")?.let { across ->
                for (clue in across.select("[cn]"))
                    readClue(clue, acrossClues)
            }
            content.selectFirst("down")?.let { down ->
                for (clue in down.select("[cn]"))
                    readClue(clue, downClues)
            }
        }

        companion object {
            fun decodeValue(value : String) : String {
                return try {
                    value.decodeURLQueryComponent()
                } catch (e : Exception) {
                    value
                }
            }
        }
    }

    override fun parseInput(input : Source) : Puzzle? {
        return parsePuzzle(input)
    }

    companion object {
        private val ACROSS_LIST : String = DefaultListNames.ACROSS_LIST
        private val DOWN_LIST : String = DefaultListNames.DOWN_LIST

        fun parsePuzzle(input : Source) : Puzzle? {
            try {
                val content = parseXMLElement(input)
                val handler = UclickXMLParser(content)

                if (!handler.isSuccessfulRead)
                    return null

                val boxList = handler.boxList ?: return null

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
    }
}
