package app.crossword.yourealwaysbe.forkyz.util;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.content.Context;

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.net.Downloaders;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;

@Singleton
public class MigrationHelper {

    private ForkyzSettings settings;

    @Inject
    MigrationHelper(ForkyzSettings settings) {
        this.settings = settings;
    }

    public void applyMigrations(Context context) {
        AndroidVersionUtils.Factory.getInstance()
            .migrateLegacyBackgroundDownloads(settings);

        settings.migrateThemePreferences();

        settings.getDownloadersSettings(downloadersSettings -> {
            settings.migrateAutoDownloaders(
                new Downloaders(context, null, downloadersSettings)
            );
        });
    }
}
