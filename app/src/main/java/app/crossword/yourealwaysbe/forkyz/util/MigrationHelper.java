package app.crossword.yourealwaysbe.forkyz.util;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.settings.MigratePreferencesKt;

@Singleton
public class MigrationHelper {
    private ForkyzSettings settings;

    @Inject
    MigrationHelper(ForkyzSettings settings) {
        this.settings = settings;
    }

    public void applyMigrations(Context context) {
        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(context);
        MigratePreferencesKt.migratePreferences(prefs, settings);
    }
}
