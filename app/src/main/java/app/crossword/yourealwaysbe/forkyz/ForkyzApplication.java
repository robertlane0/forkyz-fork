package app.crossword.yourealwaysbe.forkyz;

import android.app.Application;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;

import dagger.hilt.android.HiltAndroidApp;

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.util.ThemeHelper;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;

import java.util.logging.Logger;
import javax.inject.Inject;

@HiltAndroidApp
public class ForkyzApplication
        extends Application
        implements Configuration.Provider {
    private static final Logger LOGGER
        = Logger.getLogger(ForkyzApplication.class.getCanonicalName());

    public static final String PUZZLE_DOWNLOAD_CHANNEL_ID = "forkyz.downloads";

    @Inject
    protected AndroidVersionUtils utils;

    @Inject
    protected ForkyzSettings settings;

    // for background downloads
    // https://developer.android.com/reference/androidx/hilt/work/HiltWorker
    @Inject
    HiltWorkerFactory workerFactory;

    @Override
    public void onCreate() {
        super.onCreate();

        utils.createNotificationChannel(this);

        (new ThemeHelper(settings)).themeApplication(this);
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build();
    }
}
