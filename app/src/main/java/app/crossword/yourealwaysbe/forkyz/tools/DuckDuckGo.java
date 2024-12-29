
package app.crossword.yourealwaysbe.forkyz.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Clue;

public class DuckDuckGo {
    // removing enums (of format (label=3,4-4;2) reduced IPuz form)
    private static final Pattern ENUMERATION_HINT_RE
        = Pattern.compile(
            "^(.*?)(?:\\s*\\((?:\\w+=)?[\\d\\s\\p{Punct}]*\\))?$"
        );
    private static final int ENUMERATION_HINT_GRP = 1;

    public static void launchSearch(Activity activity, Clue clue) {
        String hint = (clue == null) ? null : clue.getHint();
        if (hint == null)
            return;

        Matcher m = ENUMERATION_HINT_RE.matcher(hint);
        if (m.matches())
            hint = m.group(ENUMERATION_HINT_GRP).trim();

        // remove entities, tags, and non-alphanumeric (keep -)
        hint = hint.replaceAll("<[^>]*>", "");
        hint = hint.replaceAll("&[A-Za-z\\d#]+;", "");
        hint = hint.replaceAll("[^-A-Za-z\\s\\d]", "");

        System.out.println("FORKYZ: url " + Uri.parse(
            activity.getString(R.string.duckduckgo_search_url, hint)
        ));
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(
            activity.getString(R.string.duckduckgo_search_url, hint)
        ));
        activity.startActivity(i);
    }
}
