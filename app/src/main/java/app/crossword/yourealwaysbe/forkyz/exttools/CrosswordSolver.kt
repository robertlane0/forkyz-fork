
package app.crossword.yourealwaysbe.forkyz.exttools

import android.content.ActivityNotFoundException

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.Zone

val CROSSWORD_SOLVER_BLANK = '.'

private val CROSSWORD_SOLVER = "org.billthefarmer.crossword"
private val CROSSWORD_SOLVER_MAIN = "org.billthefarmer.crossword.Main"
private val CROSSWORD_SOLVER_ANAGRAM = "org.billthefarmer.crossword.Anagram"
private val CROSSWORD_SOLVER_WEBSITE_URL
    = "https://billthefarmer.github.io/crossword";

enum class CrosswordSolverRequestType(val uri : String) {
    MISSING_LETTERS(CROSSWORD_SOLVER_MAIN),
    ANAGRAM(CROSSWORD_SOLVER_ANAGRAM),
}

class CrosswordSolverData(
    val requestType : CrosswordSolverRequestType,
    val request : String,
) : ExternalToolData() {
    companion object {
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
                    request.append(box.getResponse())
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
         * @param request string in crossword solver format (. for
         * blank)
         */
        @JvmStatic // remove when all Kotlin
        fun buildMissingLetters(request : String?) : CrosswordSolverData
            = CrosswordSolverData(
                CrosswordSolverRequestType.MISSING_LETTERS,
                request ?: "",
            )

        /**
         * Build raw anagram request
         *
         * @param request string in crossword solver format (. for
         * blank)
         */
        @JvmStatic // remove when all Kotlin
        fun buildAnagram(request : String?) : CrosswordSolverData
            = CrosswordSolverData(
                CrosswordSolverRequestType.ANAGRAM,
                request ?: "",
            )
    }

    override fun accept(launcher : ExternalToolLauncher) {
        launcher.visit(this)
    }
}

@Throws(AppNotFoundException::class)
fun ExternalToolLauncher.visit(data : CrosswordSolverData) {
    ExternalCaller.launchApp(
        this.activity,
        CROSSWORD_SOLVER,
        data.requestType.uri,
        android.content.Intent.EXTRA_TEXT,
        data.request,
        this.activity.getString(R.string.crossword_solver),
        CROSSWORD_SOLVER_WEBSITE_URL,
    )
}

