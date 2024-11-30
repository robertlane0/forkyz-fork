package app.crossword.yourealwaysbe.forkyz.util;

import android.app.Activity;
import android.app.Application;
import com.google.android.material.color.DynamicColors;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.settings.Theme;

public class ThemeHelper {
    private ForkyzSettings settings;

    public ThemeHelper(ForkyzSettings settings) {
        this.settings = settings;
    }

    public void themeApplication(Application app) {
        // has to be blocking: a late theme change won't get picked up
        if (settings.getAppThemeSync() == Theme.DYNAMIC)
            DynamicColors.applyToActivitiesIfAvailable(app);
    }

    public void themeActivity(Activity activity) {
        // needs to be blocking so can be done before end of onCreate in
        // ForkyzActivity (else toolbar not themed)
        Theme theme = settings.getAppThemeSync();
        switch (theme) {
        case STANDARD:
            activity.setTheme(R.style.Theme_Forkyz_Standard);
            break;
        case LEGACY_LIKE:
            activity.setTheme(R.style.Theme_Forkyz_LegacyLike);
            break;
        default:
            // keep main theme
            break;
        }
    }
}
