package app.crossword.yourealwaysbe.forkyz.versions;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.function.Consumer;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Uri;
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
import androidx.fragment.app.Fragment;

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.util.BackgroundDownloadManager;

public class KitKatUtil implements AndroidVersionUtils {
    @Override
    public void createNotificationChannel(Context context) {

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

    @Override
    public void migrateLegacyBackgroundDownloads(
        Context context,
        ForkyzSettings settings,
        BackgroundDownloadManager downloadManager
    ) {
        // do nothing: legacy background download needed lollipop
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
        activity.finish();
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
    public boolean isSAFSupported() {
        return false;
    }

    @Override
    public ActivityResultLauncher<Uri> registerForSAFUriResult(
        Fragment fragment, Consumer<Uri> uriConsumer
    ) {
        // do nothing, not supported
        return null;
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
    @SuppressWarnings("deprecation")
    public void speak(TextToSpeech tts, CharSequence text) {
        tts.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, null);
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
    public void setDecorFitsSystemWindows(Window window, boolean fits) {
        // do nothing since we can't avoid overlapping system bars / ime
        // in early Android
    }

    @Override
    public void setupBottomInsets(View view) {
        // do nothing
    }

    @Override
    public String getFilterClueToAlphabeticRegex() {
        return "<[^>]*>|[^\\p{Alphabetic}]";
    }
}
