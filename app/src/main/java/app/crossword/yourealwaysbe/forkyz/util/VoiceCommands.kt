
package app.crossword.yourealwaysbe.forkyz.util

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
     * @param commandAlternatives list of speech result strings, each
     * considered to be possible alternatives, ordered by likelihood.
     * Dispatch searches through them in order until it finds a match.
     */
    fun dispatch(commandAlternatives : List<String>) {
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

    class VoiceCommand {
        // list of starter keywords
        private val commands : MutableList<String> = mutableListOf()
        private val executeCommand : (String) -> Unit

        /**
         * Construct a VoiceCommand
         *
         * @param commandId the R.string id for the word identifying
         * this command
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
