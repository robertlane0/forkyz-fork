package app.crossword.yourealwaysbe.forkyz;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;

@AndroidEntryPoint
public class PreferencesActivity
       extends ForkyzActivity
       implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    public static final String NIGHT_MODE = "nightMode";

    @Inject
    protected AndroidVersionUtils utils;

    @Inject
    protected ForkyzSettings settings;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.preferences_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        holographic();
        setupSideInsets(toolbar);
        setupBottomInsets(findViewById(R.id.content));
        setStatusBarElevation(findViewById(R.id.app_bar_layout));

        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.preferences_layout, new PreferencesFragment())
                .commit();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (fragmentManager.getBackStackEntryCount() == 0) {
            finish();
            return true;
        }

        if (fragmentManager.popBackStackImmediate()) {
            return true;
        }

        return super.onSupportNavigateUp();
    }

    // from https://developer.android.com/guide/topics/ui/settings/organize-your-settings
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller,
                                             Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment
            = getSupportFragmentManager().getFragmentFactory()
                                         .instantiate(getClassLoader(),
                                                      pref.getFragment());
        fragment.setArguments(args);

        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.preferences_layout, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }
}
