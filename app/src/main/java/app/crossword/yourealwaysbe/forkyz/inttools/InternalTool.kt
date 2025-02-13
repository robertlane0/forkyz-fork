
package app.crossword.yourealwaysbe.forkyz.inttools

import androidx.appcompat.app.AppCompatActivity

/**
 * Frame work for passing interal tool tasks from view model to ui
 *
 * InternalToolData contains data needed for task (view model).
 *
 * InternalToolLauncher does the activity-side launching.
 *
 * An internal tool should provide its data class (with a builder
 * function for the view model to use). It should also extend
 * InternalToolLauncher with a visit function that takes its data and
 * does the activity launch.
 *
 * See tools in this dir for examples.
 */

/**
 * Data for external tool call
 *
 * Should provide a static builder that does all the background stuff,
 * gets from settings, threading, and so on.
 */
abstract class InternalToolData {
    abstract fun accept(launcher : InternalToolLauncher)
}

/**
 * Add a launch method for the data above
 *
 * InternalToolLauncher.launch(data : MyInternalToolData)
 *
 * Should be all main thread and just use data, not access backend
 * puzzle/settings/&c.
 */
class InternalToolLauncher(val activity : AppCompatActivity) { }

