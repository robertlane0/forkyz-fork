
package app.crossword.yourealwaysbe.forkyz.exttools

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.puz.Puzzle

class FifteenSquaredData(uri : Uri) : HTMLToolData(uri) {
    companion object {
        /**
         * Build a request for Fifteen Squared
         *
         * Context needed for getString
         */
        fun build(context : Context, puz : Puzzle) : FifteenSquaredData {
            var source = puz.getSource()
            if (source == null)
                source = ""

            var title = puz.getTitle()
            if (title == null)
                title = ""

            // bodges to try to improve search hit rate
            if (source.equals(title, ignoreCase = true))
                title = ""
            source = source.replace("Daily Cryptic", "")
            title = title.replace(".", "")

            val uri = context.getResources().getString(
                R.string.search_fifteen_squared_url, source, title
            ).toUri()

            return FifteenSquaredData(uri)
        }
    }
}

