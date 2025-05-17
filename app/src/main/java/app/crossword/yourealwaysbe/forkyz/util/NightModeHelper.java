package app.crossword.yourealwaysbe.forkyz.util;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import app.crossword.yourealwaysbe.forkyz.settings.DayNightMode;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;

/**
 * Heavily adapted from sources below, description no longer relevant so
 * removed.
 *
 * Original source: https://gist.github.com/slightfoot/c508cdc8828a478572e0
 * Adapted for Shortyz by Simon Lightfoot <simon@demondevelopers.com>
  */
public class NightModeHelper {

    private WeakReference<AppCompatActivity> mActivity;
    private ForkyzSettings settings;

    public static NightModeHelper bind(
        AppCompatActivity activity,
        ForkyzSettings settings
    ) {
        NightModeHelper helper = new NightModeHelper(activity, settings);
        helper.restoreNightMode();

        // This may seem pointless but it forces the Theme to be reloaded
        // with new styles that would change due to new Configuration.
        int activityTheme = getTheme(activity);
        activity.setTheme(activityTheme);

        return helper;
    }

    private NightModeHelper(
        AppCompatActivity activity, ForkyzSettings settings
    ) {
        this.mActivity = new WeakReference<>(activity);
        this.settings = settings;
    }

    public void restoreNightMode() {
        AppCompatActivity activity = mActivity.get();
        if (activity != null) {
            getCurrentMode(currentMode -> {
                switch (currentMode) {
                case DNM_DAY:
                    AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_NO
                    );
                    break;
                case DNM_NIGHT:
                    AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_YES
                    );
                    break;
                case DNM_SYSTEM:
                    AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    );
                    break;
                }
            });
        }
    }

    public void getCurrentMode(Consumer<DayNightMode> cb) {
        settings.getAppDayNightMode(cb);
    }

    /**
     * Helper method to get the theme resource id. Warning, accessing
     * non-public methods is a no-no and there is no guarantee this will
     * work.
     *
     * @param context the context you want to extract the theme-resource-id from
     * @return The themeId associated w/ the context
     */
    @Deprecated
    private static int getTheme(Context context) {
        try {
            Class<?> wrapper = Context.class;
            Method method = wrapper.getMethod("getThemeResId");
            method.setAccessible(true);
            return (Integer) method.invoke(context);
        } catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.WARNING, "Exception getting theme", e);
        }
        return 0;
    }
}
