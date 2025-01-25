package app.crossword.yourealwaysbe.forkyz.versions;

import java.io.IOException;
import java.io.Serializable;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.util.BackgroundDownloadManager;

public interface AndroidVersionUtils {

    @Module
    @InstallIn(SingletonComponent.class)
    public static class Factory {
        @Provides
        public static AndroidVersionUtils provideAndroidVersionUtils() {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                return new MarshmallowUtil();

            return new LollipopUtil();
        }
    }

    void createNotificationChannel(Context context);

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

    void migrateLegacyBackgroundDownloads(
        Context context,
        ForkyzSettings settings,
        BackgroundDownloadManager downloadManager
    );

    void setFullScreen(Window window);

    void finishAndRemoveTask(Activity activity);

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
     * Call right version of tts speak
     */
    void speak(TextToSpeech tts, CharSequence text);

    /**
     * Get the current application version name
     */
    String getApplicationVersionName(Context context);

    /**
     * A regex for filtering all non alphabetic chars and tags from clue text
     *
     * (Before Android 10 "Alphabetic", since "IsAlphabetic")
     */
    public String getFilterClueToAlphabeticRegex();
}
