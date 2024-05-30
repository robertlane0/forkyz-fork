package app.crossword.yourealwaysbe.forkyz.versions;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.function.Consumer;

import android.app.Activity;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.util.BackgroundDownloadManager;

public class LollipopUtil implements AndroidVersionUtils {

    private int speechReqCount = 0;

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
        settings.migrateLegacyBackgroundDownloads(legacyEnabled -> {
            if (legacyEnabled) {
                JobScheduler scheduler = (JobScheduler) context
                    .getSystemService(Context.JOB_SCHEDULER_SERVICE);
                scheduler.cancelAll();

                // start new
                downloadManager.setHourlyBackgroundDownloadPeriod();
            }
        });
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

    /**
     * SAF support for Forkyz rather than Android
     *
     * Android had it in KitKat, but didn't have the document tree access we
     * need until now.
     */
    @Override
    public boolean isSAFSupported() {
        return true;
    }

    @Override
    public ActivityResultLauncher<Uri> registerForSAFUriResult(
        Fragment fragment, Consumer<Uri> uriConsumer
    ) {
        return fragment.registerForActivityResult(
            new OpenDocumentTree(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    uriConsumer.accept(uri);
                }
            }
        );
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
    public void setDecorFitsSystemWindows(Window window, boolean fits) {
        WindowCompat.setDecorFitsSystemWindows(window, fits);
    }

    @Override
    public void setupInsets(View view, boolean sides, boolean bottom) {
        MarginLayoutParams origMlp
            = (MarginLayoutParams) view.getLayoutParams();
        // keep own copies because above object will change with view
        int origLeftMargin = origMlp.leftMargin;
        int origBottomMargin = origMlp.bottomMargin;
        int origRightMargin = origMlp.rightMargin;

        ViewCompat.setOnApplyWindowInsetsListener(
            view,
            (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime()
                );

                MarginLayoutParams mlp
                    = (MarginLayoutParams) v.getLayoutParams();
                if (sides) {
                    mlp.leftMargin = insets.left + origLeftMargin;
                    mlp.rightMargin = insets.right + origRightMargin;
                }
                if (bottom)
                    mlp.bottomMargin = insets.bottom + origBottomMargin;
                v.setLayoutParams(mlp);

                return WindowInsetsCompat.CONSUMED;
            }
        );
    }

    @Override
    public String getFilterClueToAlphabeticRegex() {
        return "&[^;]*;|<[^>]*>|[^\\p{Alphabetic}]";
    }
}
