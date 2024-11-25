package app.crossword.yourealwaysbe.forkyz.util;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.content.Context;

import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;

@Singleton
public class MigrationHelper {

    private ForkyzSettings settings;
    private BackgroundDownloadManager downloadManager;
    private AndroidVersionUtils utils;
    private DownloadersProvider downloadersProvider;

    @Inject
    MigrationHelper(
        AndroidVersionUtils utils,
        ForkyzSettings settings,
        BackgroundDownloadManager downloadManager,
        DownloadersProvider downloadersProvider
    ) {
        this.utils = utils;
        this.settings = settings;
        this.downloadManager = downloadManager;
        this.downloadersProvider = downloadersProvider;
    }

    public void applyMigrations(Context context) {
        utils.migrateLegacyBackgroundDownloads(
            context, settings, downloadManager
        );
        settings.migrateThemePreferences();
        downloadersProvider.get(settings::migrateAutoDownloaders);
        settings.migrateDontDeleteCrossing();
        settings.migrateCycleUnfilled();
    }
}
