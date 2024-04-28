package app.crossword.yourealwaysbe.forkyz.versions;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.Q)
public class QUtil extends PieUtil {
    @Override
    public String getFilterClueToAlphabeticRegex() {
        return "&[^;]*;|<[^>]*>|[^\\p{IsAlphabetic}]";
    }
}
