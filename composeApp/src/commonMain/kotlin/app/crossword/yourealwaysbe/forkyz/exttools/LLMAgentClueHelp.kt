
package app.crossword.yourealwaysbe.forkyz.exttools

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_no_clue_to_query
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_query_failed
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_query_just_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_query_just_response
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_query_just_solution
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_query_solution_and_response
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_clue_blank_box
import app.crossword.yourealwaysbe.forkyz.settings.ExternalToolSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.puz.Playboard

/**
 * Makes request and returns response data
 *
 * @return text response for OK dialog
 */
suspend fun askLLMAgentForCurrentClue(
    utils : NativeBackendUtils,
    settings : ForkyzSettings,
    board : Playboard
) : String {
    val requester = LLMClueHelpRequester(utils, settings)
    return requester.askForHelp(board)
}

fun isLLMAgentEnabled(settings : ExternalToolSettings) : Boolean {
    return settings.llmAgentSettings.isEnabled
}

/**
 * Requests help for the current clue using an LLM service
 */
class LLMClueHelpRequester(
    private val utils : NativeBackendUtils,
    private val settings : ForkyzSettings,
) {
    /**
     * Asks the LLM service for help with the current clue
     *
     * @param board the current playboard
     * @return a string response suitable for displaying in a dialog
     */
    suspend fun askForHelp(board : Playboard) : String {
        val response = getLLMService(settings)?.let { llmService ->
            val query = makeQuery(board)
            if (query == null) {
                return utils.getString(Res.string.help_no_clue_to_query)
            }
            llmService.makeRequest(settings, query)
        }
        return response ?: utils.getString(Res.string.help_query_failed)
    }

    private suspend fun makeQuery(board : Playboard) : String? {
        val blank = utils.getString(Res.string.share_clue_blank_box)

        val puz = board.getPuzzle() ?: return null
        val clue = board.getClue() ?: return null

        val hint = clue.getHint()
        var response : String? = null
        var solution : String? = null

        val zone = clue.getZone()
        if (zone != null) {
            var hasResponse = false
            val responseBuilder = StringBuilder()
            var hasSolution = false
            val solutionBuilder = StringBuilder()

            for (pos in zone) {
                val box = puz.checkedGetBox(pos)
                if (box != null) {
                    if (!box.isBlank()) {
                        hasResponse = true
                        responseBuilder.append(box.getResponse())
                    } else {
                        responseBuilder.append(blank)
                    }

                    if (box.hasSolution()) {
                        hasSolution = true
                        solutionBuilder.append(box.getSolution())
                    } else {
                        solutionBuilder.append(blank)
                    }
                }
            }

            if (hasSolution)
                solution = solutionBuilder.toString()
            if (hasResponse)
                response = responseBuilder.toString()
        }

        return when {
            response != null && solution != null -> {
                utils.getString(
                    Res.string.help_query_solution_and_response,
                    hint, response, solution
                )
            }
            response != null -> {
                utils.getString(
                    Res.string.help_query_just_response,
                    hint, response
                )
            }
            solution != null -> {
                utils.getString(
                    Res.string.help_query_just_solution,
                    hint, solution
                )
            }
            else -> {
                utils.getString(Res.string.help_query_just_clue, clue)
            }
        }
    }
}

