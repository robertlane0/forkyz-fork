
package app.crossword.yourealwaysbe.forkyz.exttools

import androidx.appcompat.app.AppCompatActivity

/**
 * Frame work for passing external tool tasks from view model to ui
 *
 * ExternalToolData contains data needed for task (view model).
 *
 * ExternalToolLauncher does the activity-side launching.
 *
 * An external tool should provide its data class (with a builder
 * function for the view model to use). It should also extend
 * ExternalToolLauncher with a visit function that takes its data and
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
abstract class ExternalToolData {
    abstract fun accept(launcher : ExternalToolLauncher)
}

/**
 * Add a launch method for the data above
 *
 * ExternalToolLauncher.launch(data : MyExternalToolData)
 *
 * Should be all main thread and just use data, not access backend
 * puzzle/settings/&c.
 */
class ExternalToolLauncher(val activity : AppCompatActivity) { }

