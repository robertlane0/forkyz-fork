
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.coroutines.flow.first

import app.crossword.yourealwaysbe.forkyz.HTMLFile
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.getFileResourceData
import app.crossword.yourealwaysbe.forkyz.exttools.getLLMService

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent_voice_command_prompt

/**
 * Interpreting and dispatching voice commands
 */
class VoiceCommands {
    private val commands : MutableList<VoiceCommand> = mutableListOf()

    fun registerVoiceCommand(command : VoiceCommand) {
        commands.add(command)
    }

    /**
     * Execute the result from speech service
     *
     * Will use LLM Agent if settings say so
     *
     * @param commandAlternatives list of speech result strings, each
     * considered to be possible alternatives, ordered by likelihood.
     * Dispatch searches through them in order until it finds a match.
     */
    suspend fun dispatch(
        settings : ForkyzSettings,
        utils : NativeBackendUtils,
        commandAlternatives : List<String>,
    ) {
        val useLLM = settings.liveVoiceLLMVoiceCommands.first()
        if (useLLM)
            dispatchWithLLM(settings, utils, commandAlternatives)
        else
            dispatch(commandAlternatives)
    }

    private fun dispatch(commandAlternatives : List<String>) {
        for (command in commandAlternatives) {
            val split = command.split(
                "\\W+".toRegex(),
                limit = 2,
            ).toTypedArray()
            if (split.size < 1)
                return

            val key = split[0]
            val args = if (split.size < 2) "" else split[1]

            for (vc in commands) {
                if (vc.matches(key)) {
                    vc.execute(args)
                    return
                }
            }
        }
    }

    /**
     * Falls back to non-LLM if not configured
     */
    private suspend fun dispatchWithLLM(
        settings : ForkyzSettings,
        utils : NativeBackendUtils,
        commandAlternatives : List<String>,
    ) {
        val llmService = getLLMService(settings)
        if (llmService == null) {
            dispatch(commandAlternatives)
        } else {
            getLLMPrompt(utils, commandAlternatives)?.let { prompt ->
                llmService.makeRequest(settings, prompt)?.let { response ->
                    response.split("\n").forEach {
                        dispatch(listOf(it))
                    }
                }
            }
        }
    }

    private suspend fun getLLMPrompt(
        utils : NativeBackendUtils,
        commandAlternatives : List<String>,
    ) : String {
        val userRequest = "'" + commandAlternatives.joinToString("' / '") + "'"
        val availableCommands = commands
            .map { it.commands }
            .flatten().joinToString("; ")
        val helpDocs = getFileResourceData(
            utils,
            HTMLFile.VOICE_COMMANDS_HELP.unqualifiedFileName,
        )
        return utils.getString(
            Res.string.llm_agent_voice_command_prompt,
            userRequest,
            availableCommands,
            helpDocs,
        )
    }

    class VoiceCommand {
        // list of starter keywords
        internal val commands : MutableList<String> = mutableListOf()
        private val executeCommand : (String) -> Unit

        /**
         * Construct a VoiceCommand
         *
         * @param command the word identifying this command
         * @param execute a function that consumes the string after the
         * command word
         */
        constructor(
            command : String,
            executeCommand : (String) -> Unit,
        ) {
            this.commands.add(command)
            this.executeCommand = executeCommand
        }

        constructor(
            command : String,
            commandAlt : String,
            executeCommand : (String) -> Unit,
        ) {
            this.commands.add(command)
            this.commands.add(commandAlt)
            this.executeCommand = executeCommand
        }

        /**
         * If s matches the command trigger word
         *
         * Ignores case
         */
        fun matches(s : String) : Boolean {
            for (command in commands) {
                if (command.equals(s, ignoreCase = true))
                    return true
            }
            return false
        }

        /**
         * Execute the command on board with additional input args
         */
        fun execute(args : String) {
            executeCommand(args)
        }
    }
}
