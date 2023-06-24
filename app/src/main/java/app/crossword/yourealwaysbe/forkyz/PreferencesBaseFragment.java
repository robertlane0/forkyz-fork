
package app.crossword.yourealwaysbe.forkyz;

import android.app.Activity;
import androidx.preference.PreferenceFragmentCompat;

public abstract class PreferencesBaseFragment extends PreferenceFragmentCompat {
    @Override
    public void onResume() {
        Activity activity = getActivity();
        CharSequence title = getPreferenceScreen().getTitle();
        if (title == null) {
            title = activity
                .getResources()
                .getString(R.string.settings_label);
        }
        activity.setTitle(title);
        super.onResume();
    }
}
