
package app.crossword.yourealwaysbe.forkyz.versions;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;

public class MarshmallowUtil implements AndroidVersionUtils {
    @Override
    public void createNotificationChannels(Context context) {

    }

    @Override
    public int immutablePendingIntentFlag() {
        return PendingIntent.FLAG_IMMUTABLE;
    }

    @Override
    public StaticLayout getStaticLayout(
        CharSequence text, TextPaint style, int width, Layout.Alignment align
    ) {
        return StaticLayout.Builder.obtain(
            text, 0, text.length(), style, width
        ).setAlignment(align)
        .build();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setFullScreen(Window window) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
    }

    @Override
    public Typeface getSemiBoldTypeface() {
         // or fallback to bold, no semibold before P
        return Typeface.create("sans-serif", Typeface.BOLD);
    }

    @Override
    public boolean isInternalStorageFull(
        Context context, long minimumBytesFree
    ) throws IOException {
        File files = context.getFilesDir();
        return files.getFreeSpace() < minimumBytesFree;
    }

    @Override
    public boolean hasNetworkConnection(Context activity) {
        ConnectivityManager cm
            = activity.getSystemService(ConnectivityManager.class);
        return cm.getActiveNetwork() != null;
    }

    @SuppressWarnings({"deprecation","unchecked"})
    @Override
    public <T extends Serializable>
    T getSerializable(Bundle bundle, String key, Class<T> klass) {
        return (T) bundle.getSerializable(key);
    }

    @Override
    public boolean hasPostNotificationsPermission(Context context) {
        return true;
    }

    @Override
    public void requestPostNotifications(
        ActivityResultLauncher<String> launcher
    ) {
        // do nothing
    }

    @Override
    public boolean shouldShowRequestNotificationPermissionRationale(
        Activity activity
    ) {
        return false;
    }

    @Override
    public void invalidateInput(InputMethodManager imm, View view) {
        imm.restartInput(view);
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getApplicationVersionName(Context context) {
        try {
            PackageInfo info = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getApplicationUID(Context context) {
        try {
            PackageInfo info = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0);
            return info.applicationInfo.uid;
        } catch (NameNotFoundException e) {
            return -1;
        }
    }

    @Override
    public String getFilterClueToAlphabeticRegex() {
        return "&[^;]*;|<[^>]*>|[^\\p{Alphabetic}]";
    }

    @Override
    public int foregroundServiceTypeShortService() {
        return 0;
    }

    @Override
    public int foregroundServiceTypeDataSync() {
        return 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void legacySetStatusBarColor(Activity activity, int color) {
        activity.getWindow().setStatusBarColor(color);
    }
}
