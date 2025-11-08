
package app.crossword.yourealwaysbe.forkyz.exttools

import java.util.regex.Matcher
import java.util.regex.Pattern

import android.net.Uri
import androidx.core.net.toUri

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.puz.Clue

// removing enums (of format (label=3,4-4;2) reduced IPuz form)
private val ENUMERATION_HINT_RE
    = "^(.*?)(?:\\s*\\((?:\\w+=)?[\\d\\s\\p{Punct}]*\\))?$".toRegex()
private val ENUMERATION_HINT_GRP = 1

class DuckDuckGoData(uri : Uri) : HTMLToolData(uri) {
    companion object {
        /**
         * Build DuckDuckGo search for clue
         */
        suspend fun build(
            utils : NativeBackendUtils,
            clue : Clue,
        ) : DuckDuckGoData? {
            var hint = clue?.getHint()
            if (hint == null)
                return null;

            ENUMERATION_HINT_RE.matchEntire(hint)?.let {
                hint = it.groups[ENUMERATION_HINT_GRP]?.value
            }

            // remove entities, tags, and non-alphanumeric (keep -)
            hint = hint?.replace("<[^>]*>".toRegex(), "")
            hint = hint?.replace("&[A-Za-z\\d#]+;".toRegex(), "")
            hint = hint?.replace("[^-A-Za-z\\s\\d]".toRegex(), "")

            val uri = utils
                .getString(R.string.duckduckgo_search_url, hint ?: "")
                .toUri()

            return DuckDuckGoData(uri)
        }
    }
}
