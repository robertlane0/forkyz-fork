
package app.crossword.yourealwaysbe.forkyz.exttools

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.search_fifteen_squared_url
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.toUri

class FifteenSquaredData(uri : Uri) : HTMLToolData(uri) {
    companion object {
        /**
         * Build a request for Fifteen Squared
         */
        suspend fun build(
            utils : NativeBackendUtils,
            puz : Puzzle,
        ) : FifteenSquaredData {
            var source = puz.source
            if (source == null)
                source = ""

            var title = puz.title
            if (title == null)
                title = ""

            // bodges to try to improve search hit rate
            if (source.equals(title, ignoreCase = true))
                title = ""
            source = source.replace("Daily Cryptic", "")
            title = title.replace(".", "")

            val uri = utils.getString(
                Res.string.search_fifteen_squared_url, source, title
            ).toUri()

            return FifteenSquaredData(uri)
        }
    }
}

