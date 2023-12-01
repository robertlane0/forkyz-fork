
package app.crossword.yourealwaysbe.forkyz.tools;

import java.util.Locale;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;

import app.crossword.yourealwaysbe.forkyz.R;

public abstract class ExternalDictionary {

    /**
     * Look up the given word in the dictionary
     *
     * May display a dialog if the dictionary is not installed
     */
    public abstract void query(AppCompatActivity activity, String word);

    public static final ExternalDictionary FREE_DICTIONARY
        = new ExternalDictionary() {
            private static final String URL_FORMAT
                = "https://www.thefreedictionary.com/%s";

            @Override
            public void query(AppCompatActivity activity, String word) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(
                    String.format(Locale.getDefault(), URL_FORMAT, word)
                ));
                activity.startActivity(intent);
            }
        };

    public static final ExternalDictionary QUICK_DIC
        = new ExternalDictionary() {
            @Override
            public void query(AppCompatActivity activity, String word) {
                ExternalCaller.launchIntent(
                    activity,
                    "com.hughes.action.ACTION_SEARCH_DICT",
                    SearchManager.QUERY,
                    word,
                    activity.getString(R.string.quick_dic),
                    "https://github.com/rdoeffinger/Dictionary/releases"
                );
            }
        };

    public static final ExternalDictionary AARD2
        = new ExternalDictionary() {
            @Override
            public void query(AppCompatActivity activity, String word) {
                ExternalCaller.launchIntent(
                    activity,
                    "aard2.lookup",
                    SearchManager.QUERY,
                    word,
                    activity.getString(R.string.aard2),
                    "https://aarddict.org/"
                );
            }
        };
}
