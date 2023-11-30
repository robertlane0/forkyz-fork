
package app.crossword.yourealwaysbe.forkyz.tools;

import androidx.appcompat.app.AppCompatActivity;

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
        ExternalCaller.launchApp(
            forkyzActivity,
            CROSSWORD_SOLVER,
            solverActivity,
            android.content.Intent.EXTRA_TEXT,
            request,
            forkyzActivity.getString(R.string.crossword_solver),
            CROSSWORD_SOLVER_WEBSITE_URL
        );
    }
}
