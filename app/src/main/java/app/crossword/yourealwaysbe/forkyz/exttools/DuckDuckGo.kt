
package app.crossword.yourealwaysbe.forkyz.exttools

import java.util.regex.Matcher
import java.util.regex.Pattern

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.puz.Clue

// removing enums (of format (label=3,4-4;2) reduced IPuz form)
private val ENUMERATION_HINT_RE
    = "^(.*?)(?:\\s*\\((?:\\w+=)?[\\d\\s\\p{Punct}]*\\))?$".toRegex()
private val ENUMERATION_HINT_GRP = 1


class DuckDuckGoData(uri : Uri) : HTMLToolData(uri) {
    companion object {
        /**
         * Build DuckDuckGo search for clue
         *
         * Needs context for getString
         */
        fun build(context : Context, clue : Clue) : DuckDuckGoData? {
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

            val uri = Uri.parse(
                context.getResources()
                    .getString(R.string.duckduckgo_search_url, hint)
            )

            return DuckDuckGoData(uri)
        }
    }
}
