
package app.crossword.yourealwaysbe.puz.io

import java.util.Locale
import java.util.ResourceBundle

object DefaultListNames {
    var ACROSS_LIST: String
    var DOWN_LIST: String
    var NODIR_LIST: String

    init {
        val resourceBundle = ResourceBundle.getBundle(
            "app.crossword.yourealwaysbe.puz.strings",
            Locale.getDefault(),
        )
        ACROSS_LIST = resourceBundle.getString("acrossListName")
        DOWN_LIST = resourceBundle.getString("downListName")
        NODIR_LIST = resourceBundle.getString("noDirListName")
    }
}
