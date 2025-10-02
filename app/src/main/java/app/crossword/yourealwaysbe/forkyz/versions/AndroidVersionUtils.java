package app.crossword.yourealwaysbe.forkyz.versions;

import java.io.IOException;
import java.io.Serializable;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.result.ActivityResultLauncher;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

public interface AndroidVersionUtils {

    @Module
    @InstallIn(SingletonComponent.class)
    public static class Factory {
        @Provides
        public static AndroidVersionUtils provideAndroidVersionUtils() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
                return new VanillaIceCreamUtil();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                return new UpsideDownCakeUtil();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                return new TiramisuUtil();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                return new RUtil();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                return new QUtil();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                return new PieUtil();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                return new OreoUtil();

            return new MarshmallowUtil();
        }
    }

    void createNotificationChannels(Context context);

    int immutablePendingIntentFlag();

    StaticLayout getStaticLayout(
        CharSequence text, TextPaint style, int width, Layout.Alignment align
    );

    default StaticLayout getStaticLayout(
        CharSequence text, TextPaint style, int width
    ) {
        return getStaticLayout(
            text, style, width, Layout.Alignment.ALIGN_NORMAL
        );
    }

    void setFullScreen(Window window);

    Typeface getSemiBoldTypeface();

    boolean isInternalStorageFull(
        Context context, long minimumBytesFree
    ) throws IOException;

    boolean hasNetworkConnection(Context context);

    /**
     * Convenience method for type-safe Bundle.getSerializalbe
     *
     * Should end up in a BundleCompat one day, i hope.
     */
    <T extends Serializable> T
    getSerializable(Bundle bundle, String key, Class<T> klass);

    /**
     * Has POST_NOTIFICATIONS permission or is not on an API that needs it
     */
    boolean hasPostNotificationsPermission(Context context);

    /**
     * Request POST_NOTIFICATIONS on devices that have it
     *
     * Else, ignore
     */
    void requestPostNotifications(ActivityResultLauncher<String> launcher);

    /**
     * If rationale is needed when requesting notification permission
     */
    boolean shouldShowRequestNotificationPermissionRationale(Activity activity);

    /**
     * Call invalidateInput or restartInput as available
     */
    void invalidateInput(InputMethodManager imm, View view);

    /**
     * Get the current application version name
     */
    String getApplicationVersionName(Context context);

    /**
     * Get the current application uid or -1
     */
    int getApplicationUID(Context context);

    /**
     * A regex for filtering all non alphabetic chars and tags from clue text
     *
     * (Before Android 10 "Alphabetic", since "IsAlphabetic")
     */
    public String getFilterClueToAlphabeticRegex();

    /**
     * Constant ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
     */
    public int foregroundServiceTypeShortService();

    /**
     * Constant ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
     */
    public int foregroundServiceTypeDataSync();

    /**
     * Set status bar color using deprecated API methods if available
     */
    public void legacySetStatusBarColor(Activity activity, int color);
}
