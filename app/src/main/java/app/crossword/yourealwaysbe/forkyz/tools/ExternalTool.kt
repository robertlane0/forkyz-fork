
package app.crossword.yourealwaysbe.forkyz.tools

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

abstract class ExternalToolData {
    abstract fun accept(launcher : ExternalToolLauncher)
}

class ExternalToolLauncher(val activity : AppCompatActivity) { }

