package app.crossword.yourealwaysbe.forkyz.util;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.content.Context;

import app.crossword.yourealwaysbe.forkyz.net.Downloaders;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;

@Singleton
public class MigrationHelper {

    private ForkyzSettings settings;
    private BackgroundDownloadManager downloadManager;
    private AndroidVersionUtils utils;

    @Inject
    MigrationHelper(
        AndroidVersionUtils utils,
        ForkyzSettings settings,
        BackgroundDownloadManager downloadManager
    ) {
        this.utils = utils;
        this.settings = settings;
        this.downloadManager = downloadManager;
    }

    public void applyMigrations(Context context) {
        utils.migrateLegacyBackgroundDownloads(
            context, settings, downloadManager
        );

        settings.migrateThemePreferences();

        settings.getDownloadersSettings(downloadersSettings -> {
            settings.migrateAutoDownloaders(
                new Downloaders(context, utils, null, downloadersSettings)
            );
        });
    }
}
