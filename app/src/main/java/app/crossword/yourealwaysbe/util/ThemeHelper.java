package app.crossword.yourealwaysbe.util;

import android.app.Activity;
import android.app.Application;
import com.google.android.material.color.DynamicColors;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.settings.Theme;

public class ThemeHelper {
    private ForkyzSettings settings;

    public ThemeHelper(ForkyzSettings settings) {
        this.settings = settings;
    }

    public void themeApplication(Application app) {
        // has to be blocking: a late theme change won't get picked up
        if (settings.getThemeBlocking() == Theme.DYNAMIC)
            DynamicColors.applyToActivitiesIfAvailable(app);
    }

    public void themeActivity(Activity activity) {
        settings.getTheme(theme -> {
            if (theme == Theme.LEGACY_LIKE)
                activity.setTheme(R.style.Theme_Forkyz_LegacyLike);
        });
    }
}
