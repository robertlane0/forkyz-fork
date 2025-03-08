package app.crossword.yourealwaysbe.forkyz.versions;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import android.app.Activity;
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

public class LollipopUtil implements AndroidVersionUtils {

    private int speechReqCount = 0;

    @Override
    public void createNotificationChannels(Context context) {

    }

    @Override
    public int immutablePendingIntentFlag() {
        return 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public StaticLayout getStaticLayout(
        CharSequence text, TextPaint style, int width, Layout.Alignment align
    ) {
        return new StaticLayout(
            text,
            style, width, align, 1.0f, 0, false
        );
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
    public void finishAndRemoveTask(Activity activity) {
        activity.finishAndRemoveTask();
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

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasNetworkConnection(Context activity) {
        ConnectivityManager cm = ContextCompat.getSystemService(
            activity, ConnectivityManager.class
        );
        android.net.NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
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
    public void speak(TextToSpeech tts, CharSequence text) {
        tts.speak(
            text, TextToSpeech.QUEUE_FLUSH, null,
            "ForkyzSpeak_" + (speechReqCount++)
        );
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
    public String getFilterClueToAlphabeticRegex() {
        return "&[^;]*;|<[^>]*>|[^\\p{Alphabetic}]";
    }

    @Override
    public int foregroundServiceTypeShortService() {
        return 0;
    }
}
