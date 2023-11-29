
package app.crossword.yourealwaysbe.forkyz.tools;

import java.util.Locale;

import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;

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
}
