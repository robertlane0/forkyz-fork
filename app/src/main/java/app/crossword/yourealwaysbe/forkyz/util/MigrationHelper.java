package app.crossword.yourealwaysbe.forkyz.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.settings.MigratePreferencesKt;

public class MigrationHelper {
    private ForkyzSettings settings;

    public MigrationHelper(ForkyzSettings settings) {
        this.settings = settings;
    }

    public void applyMigrations(Context context) {
        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(context);
        MigratePreferencesKt.migratePreferences(prefs, settings);
    }
}
