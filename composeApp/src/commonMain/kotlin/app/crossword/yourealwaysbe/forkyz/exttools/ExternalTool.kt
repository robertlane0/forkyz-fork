
package app.crossword.yourealwaysbe.forkyz.exttools

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
 * Thrown when app can't be launched
 */
class AppNotFoundException(
    val appName : String,
    val appURL : String,
) : Exception()

/**
 * Data for external tool call
 *
 * Should provide a static builder that does all the background stuff,
 * gets from settings, threading, and so on.
 */
abstract class ExternalToolData {
    abstract suspend fun accept(launcher : ExternalToolLauncher)
}

/**
 * Add a launch method for the data above
 *
 * ExternalToolLauncher.launch(data : MyExternalToolData)
 *
 * Should be all main thread and just use data, not access backend
 * puzzle/settings/&c.
 *
 * Response passed by callback should be displayed in an OK dialog to
 * the user.
 *
 * Expect class used to platforms can provide their own implementation
 * that stores natively required data (e.g. activity)
 */
expect class ExternalToolLauncher { }

