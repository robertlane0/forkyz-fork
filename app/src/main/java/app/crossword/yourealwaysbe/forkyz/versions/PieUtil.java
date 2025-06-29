package app.crossword.yourealwaysbe.forkyz.versions;

import androidx.annotation.RequiresApi;
import android.graphics.Typeface;
import android.os.Build;

@RequiresApi(api = Build.VERSION_CODES.P)
public class PieUtil extends OreoUtil {
    @Override
    public Typeface getSemiBoldTypeface() {
        return Typeface.create(Typeface.SANS_SERIF, 600, false);
    }
}
