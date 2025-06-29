package app.crossword.yourealwaysbe.forkyz.versions;

import androidx.annotation.RequiresApi;
import android.os.Build;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class QUtil extends PieUtil {
    @Override
    public String getFilterClueToAlphabeticRegex() {
        return "&[^;]*;|<[^>]*>|[^\\p{IsAlphabetic}]";
    }
}
