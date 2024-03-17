
package app.crossword.yourealwaysbe.forkyz.util;

import javax.inject.Inject;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.BrowseActivity;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.R;

/**
 * Class for showing an alert when broadcast done.
 *
 * Can't do in broadcast receive.
 * Sources : https://stackoverflow.com/a/18462980
 */
@AndroidEntryPoint
public class ImportedNowFinishDialogActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImportedNowFinishDialog dialog
            = new ImportedNowFinishDialog();
        Bundle args = new Bundle();
        args.putBoolean(
            BrowseActivity.INTENT_IMPORT_SUCCESS,
            getIntent().getBooleanExtra(
                BrowseActivity.INTENT_IMPORT_SUCCESS, false
            )
        );
        dialog.setArguments(args);
        FragmentManager fm = getSupportFragmentManager();
        if (!fm.isDestroyed())
            dialog.show(fm, "ImportedNowFinishDialog");
    }

    @AndroidEntryPoint
    public static class ImportedNowFinishDialog extends DialogFragment {

        @Inject
        protected ForkyzSettings settings;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();

            Bundle args = getArguments();
            boolean success = false;
            if (args != null) {
                success = args.getBoolean(
                    BrowseActivity.INTENT_IMPORT_SUCCESS, false
                );
            }

            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(activity);

            int message = success
                ? R.string.import_success_long
                : R.string.import_failure_long;

            // even if some failed, there might still be a new one
            settings.setBrowseNewPuzzle(true);

            builder.setTitle(R.string.imports_title)
                .setMessage(message)
                .setPositiveButton(
                    R.string.exit,
                    (di, i) -> { doExit(); }
                );

            return builder.create();
        }

        @Override
        public void onCancel(DialogInterface di) {
            doExit();
        }

        private void doExit() {
            // ask browse activities to close
            Activity activity = getActivity();
            Intent close = new Intent(
                BrowseActivity.BROWSER_CLOSE_ACTION
            );
            // -1 so all browse activities close
            close.putExtra(BrowseActivity.BROWSER_CLOSE_TASK_ID, -1);
            close.setPackage(activity.getPackageName());
            activity.sendBroadcast(close);
            activity.finish();
        }
    }
}
