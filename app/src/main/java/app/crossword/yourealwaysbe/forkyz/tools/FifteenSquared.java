
package app.crossword.yourealwaysbe.forkyz.tools;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Puzzle;

public class FifteenSquared {
    public static void launchSearch(Activity activity, Puzzle puz) {
        if (puz == null)
            return;

        String source = puz.getSource();
        if (source == null)
            source = "";

        String title = puz.getTitle();
        if (title == null)
            title = "";

        // bodges to try to improve search hit rate
        if (source.equalsIgnoreCase(title))
            title = "";
        source = source.replaceAll("Daily Cryptic", "");
        title = title.replaceAll("\\.", "");

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(
            activity.getString(
                R.string.search_fifteen_squared_url, source, title
            )
        ));
        activity.startActivity(i);
    }
}
