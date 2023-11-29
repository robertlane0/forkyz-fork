
package app.crossword.yourealwaysbe.forkyz.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import app.crossword.yourealwaysbe.forkyz.R;

/**
 * Call out to billthefarmer's crossword solver
 */
public class CrosswordSolver {

    private static final String CROSSWORD_SOLVER
        = "org.billthefarmer.crossword";
    protected static final String CROSSWORD_SOLVER_MAIN
        = "org.billthefarmer.crossword.Main";
    protected static final String CROSSWORD_SOLVER_ANAGRAM
        = "org.billthefarmer.crossword.Anagram";
    private static final String CROSSWORD_SOLVER_WEBSITE_URL
        = "https://billthefarmer.github.io/crossword";

    /**
     * Launch the main solver app
     *
     * Shows a dialog with weblink if app not installed
     *
     * @param activity the activity to call from
     * @param request a string with letters and . for blank
     */
    public static void launchMain(
        AppCompatActivity activity, String request
    ) {
        launchCrosswordSolver(activity, CROSSWORD_SOLVER_MAIN, request);
    }

    /**
     * Launch the anagram solver
     *
     * Shows a dialog with weblink if app not installed
     *
     * @param activity the activity to call from
     * @param request string of letters to use
     */
    public static void launchAnagram(
        AppCompatActivity activity, String request
    ) {
        launchCrosswordSolver(activity, CROSSWORD_SOLVER_ANAGRAM, request);
    }

    /**
     * Launch billthefarmer's crossword solver
     *
     * Shows a dialog with weblink if app not installed
     *
     * @param forkyzActivity the activity calling from
     * @param solverActivity either CROSSWORD_SOLVER_MAIN (missing letters) or
     * CROSSWORD_SOLVER_ANAGRAM (anagrams)
     * @param request the request text, use . for blanks, just the letters for
     * anagrams
     */
    private static void launchCrosswordSolver(
        AppCompatActivity forkyzActivity, String solverActivity, String request
    ) {
        try {
            Intent i = new Intent();
            i.setComponent(
                new ComponentName(CROSSWORD_SOLVER, solverActivity)
            );
            i.putExtra(android.content.Intent.EXTRA_TEXT, request);
            forkyzActivity.startActivity(i);
        } catch (ActivityNotFoundException e) {
            DialogFragment dialog = new NoCrosswordSolverDialog();
            dialog.show(
                forkyzActivity.getSupportFragmentManager(),
                "NoCrosswordSolverDialog"
            );
        }
    }

    public static class NoCrosswordSolverDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();

            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(activity);

            builder.setTitle(R.string.crossword_solver_not_installed)
                .setMessage(R.string.crossword_solver_install_msg)
                .setNegativeButton(
                    R.string.website,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                            DialogInterface dialogInterface, int i
                        ) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(
                                Uri.parse(CROSSWORD_SOLVER_WEBSITE_URL)
                            );
                            activity.startActivity(intent);
                        }
                    }
                ).setPositiveButton(android.R.string.ok, null);

            return builder.create();
       }
    }
}
