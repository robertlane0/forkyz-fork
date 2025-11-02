package app.crossword.yourealwaysbe.forkyz;

import android.app.Application;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;

import dagger.hilt.android.HiltAndroidApp;

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper;
import app.crossword.yourealwaysbe.forkyz.util.MigrationHelper;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;

import javax.inject.Inject;

@HiltAndroidApp
public class ForkyzApplication
        extends Application
        implements Configuration.Provider {
    @Inject
    protected AndroidVersionUtils utils;

    @Inject
    protected ForkyzSettings settings;

    @Inject
    protected ThemeHelper themeHelper;

    // for background downloads
    // https://developer.android.com/reference/androidx/hilt/work/HiltWorker
    @Inject
    HiltWorkerFactory workerFactory;

    @Inject
    protected MigrationHelper migrationHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        migrationHelper.applyMigrations(this);
        utils.createNotificationChannels(this);
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build();
    }
}
