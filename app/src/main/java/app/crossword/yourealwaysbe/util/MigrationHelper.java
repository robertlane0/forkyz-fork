package app.crossword.yourealwaysbe.util;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.content.Context;
import android.content.SharedPreferences;

import app.crossword.yourealwaysbe.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.net.Downloaders;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;

@Singleton
public class MigrationHelper {

    private ForkyzSettings settings;

    @Inject
    MigrationHelper(ForkyzSettings settings) {
        this.settings = settings;
    }

    public void applyMigrations(Context context, SharedPreferences prefs) {
        AndroidVersionUtils.Factory.getInstance()
            .migrateLegacyBackgroundDownloads(prefs);

        settings.migrateThemePreferences();

        settings.getDownloadersSettings(downloadersSettings -> {
            settings.migrateAutoDownloaders(
                new Downloaders(context, null, downloadersSettings)
            );
        });
    }
}
