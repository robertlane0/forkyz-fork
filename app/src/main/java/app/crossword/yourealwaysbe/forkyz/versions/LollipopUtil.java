package app.crossword.yourealwaysbe.forkyz.versions;

import java.util.function.Consumer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.job.JobScheduler;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.util.BackgroundDownloadManager;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopUtil extends KitKatUtil {

    private int speechReqCount = 0;

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

    @Override
    public void finishAndRemoveTask(Activity activity) {
        activity.finishAndRemoveTask();
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

    @Override
    public void speak(TextToSpeech tts, CharSequence text) {
        tts.speak(
            text, TextToSpeech.QUEUE_FLUSH, null,
            "ForkyzSpeak_" + (speechReqCount++)
        );
    }

    @Override
    public void setDecorFitsSystemWindows(Window window, boolean fits) {
        WindowCompat.setDecorFitsSystemWindows(window, fits);
    }

    @Override
    public void setupBottomInsets(View view) {
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
                mlp.leftMargin = insets.left + origLeftMargin;
                mlp.bottomMargin = insets.bottom + origBottomMargin;
                mlp.rightMargin = insets.right + origRightMargin;
                v.setLayoutParams(mlp);

                return WindowInsetsCompat.CONSUMED;
            }
        );
    }
}
