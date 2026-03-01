
package app.crossword.yourealwaysbe.forkyz.exttools

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.Zone

private val CROSSWORD_SOLVER_MAIN = "org.billthefarmer.crossword.Main"
private val CROSSWORD_SOLVER_ANAGRAM = "org.billthefarmer.crossword.Anagram"

private val CROSSWORD_SOLVER_BLANK = '.'

enum class CrosswordSolverRequestType(val uri : String) {
    MISSING_LETTERS(CROSSWORD_SOLVER_MAIN),
    ANAGRAM(CROSSWORD_SOLVER_ANAGRAM),
}

class CrosswordSolverData(
    val requestType : CrosswordSolverRequestType,
    val request : String,
) : ExternalToolData() {
    companion object {
        private const val BLANK_CHAR = Box.BLANK[0]

        /**
         * Build a missing letters request for zone of puzzle
         */
        fun buildMissingLetters(
            puz : Puzzle,
            zone : Zone,
        ) : CrosswordSolverData {
            val request = StringBuilder()

            for (pos in zone) {
                val box = puz.checkedGetBox(pos)
                if (!Box.isBlock(box) && !box.isBlank()) {
                    request.append(box.response)
                } else {
                    request.append(".");
                }
            }

            return CrosswordSolverData(
                CrosswordSolverRequestType.MISSING_LETTERS,
                request.toString(),
            )
        }

        /**
         * Build raw missing letters request
         *
         * @param request string in crossword solver format
         * (Box.BLANK[0]) for blank)
         */
        fun buildMissingLetters(request : String?) : CrosswordSolverData
            = CrosswordSolverData(
                CrosswordSolverRequestType.MISSING_LETTERS,
                convertBlanks(request ?: ""),
            )

        /**
         * Build raw anagram request
         *
         * @param request string in crossword solver format
         * (Box.BLANK[0] for blank)
         */
        fun buildAnagram(request : String?) : CrosswordSolverData
            = CrosswordSolverData(
                CrosswordSolverRequestType.ANAGRAM,
                convertBlanks(request ?: ""),
            )

        private fun convertBlanks(s : String) : String {
            return s.replace(BLANK_CHAR, CROSSWORD_SOLVER_BLANK)
        }
    }

    override suspend fun accept(launcher : ExternalToolLauncher) {
        launcher.visit(this)
    }
}

@Throws(AppNotFoundException::class)
expect suspend fun ExternalToolLauncher.visit(data : CrosswordSolverData)

