package app.crossword.yourealwaysbe.forkyz.versions;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;

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
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                return new PieUtil();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                return new OreoUtil();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                return new MarshmallowUtil();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                return new LollipopUtil();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                return new JellyBeanMR2Util();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                return new JellyBeanMR1Util();

            return new IceCreamSandwichUtil();
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

    boolean isMiniTabletish(DisplayMetrics metrics);

    void setFullScreen(Window window);

    ActivityResultLauncher<String> registerForUriContentsResult(
        AppCompatActivity activity, Consumer<List<Uri>> uriConsumer
    );

    void finishAndRemoveTask(Activity activity);

    Typeface getSemiBoldTypeface();

    boolean isInternalStorageFull(
        Context context, long minimumBytesFree
    ) throws IOException;

    boolean isExternalStorageDirectoryFull(
        File directory, long minimumBytesFree
    );

    boolean isSAFSupported();

    ActivityResultLauncher<Uri> registerForSAFUriResult(
        Fragment fragment, Consumer<Uri> uriConsumer
    );

    /**
     * True if the character can go into a box
     *
     * I.e. not a control char of some kind
     */
    boolean isAcceptableCharacterResponse(char c);

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
     * Calls announceForAccessibility on view if available
     *
     * And text not null
     */
    void announceForAccessibility(View view, CharSequence text);

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
     * See setupBottomInsets
     */
    public void setDecorFitsSystemWindows(Window window, boolean fits);

    /**
     * Set margins on view to avoid system bars/ime on bottom/side
     *
     * Needed with setDecorFitsSystemWindows(.., true) for app bar
     * overlap status bar. Else bottom bars/ime overlap app.
     *
     * Do nothing before API 21 since inset monitoring not supported
     * before. So use version utils setDecorFitsSystemWindows which does
     * nothing before API 21.
     */
    public void setupBottomInsets(View view);
}
