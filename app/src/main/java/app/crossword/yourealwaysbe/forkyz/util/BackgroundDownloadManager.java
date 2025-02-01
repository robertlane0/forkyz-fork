
package app.crossword.yourealwaysbe.forkyz.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;

import android.content.Context;
import androidx.annotation.MainThread;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.hilt.work.HiltWorker;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;
import com.google.common.util.concurrent.ListenableFuture;

import dagger.Module;
import dagger.Provides;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider;
import app.crossword.yourealwaysbe.forkyz.settings.BackgroundDownloadSettings;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;

/**
 * Schedule background downloads using Android WorkManager
 */
@Singleton
public class BackgroundDownloadManager {
    private static final Logger LOGGER = Logger.getLogger(
        BackgroundDownloadManager.class.getCanonicalName()
    );

    @Module
    @InstallIn(SingletonComponent.class)
    public static class WorkManagerModule {
        @Provides
        public static WorkManager provideWorkManager(
            @ApplicationContext Context applicationContext
        ) {
            return WorkManager.getInstance(applicationContext);
        }
    }

    private static final String DOWNLOAD_WORK_NAME_HOURLY
        = "backgroundDownloadHourly";
    private static final String DOWNLOAD_WORK_NAME_DAILY
        = "backgroundDownloadDaily";

    private Context applicationContext;
    private WorkManager workManager;
    private ForkyzSettings settings;

    @Inject
    BackgroundDownloadManager(
        @ApplicationContext Context applicationContext,
        WorkManager workManager,
        ForkyzSettings settings
    ) {
        this.applicationContext = applicationContext;
        this.workManager = workManager;
        this.settings = settings;
    }

    @MainThread
    public void isBackgroundDownloadEnabled(Consumer<Boolean> cb) {
        settings.getBackgroundDownloadSettings(bgSettings -> {
            cb.accept(
                bgSettings.hourly()
                || getNextDailyDownloadDelay(bgSettings) >= 0
            );
        });
    }

    @MainThread
    public void updateBackgroundDownloads() {
        cancelBackgroundDownloads();

        settings.getBackgroundDownloadSettings(bgSettings -> {
            if (bgSettings.hourly())
                scheduleHourlyDownloads(bgSettings);
            else
                scheduleNextDailyDownload(bgSettings);
        });
    }

    private void scheduleHourlyDownloads(
        BackgroundDownloadSettings bgSettings
    ) {
        PeriodicWorkRequest request
            = new PeriodicWorkRequest.Builder(
                HourlyDownloadWorker.class, 1, TimeUnit.HOURS
            ).setConstraints(getConstraints(bgSettings))
            .build();

        getWorkManager()
            .enqueueUniquePeriodicWork(
                DOWNLOAD_WORK_NAME_HOURLY,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            );
    }

    public void scheduleNextDailyDownload(
        BackgroundDownloadSettings bgSettings
    ) {
        long nextDelay = getNextDailyDownloadDelay(bgSettings);
        if (nextDelay < 0)
            return;

        OneTimeWorkRequest request
            = new OneTimeWorkRequest.Builder(DailyDownloadWorker.class)
                .setConstraints(getConstraints(bgSettings))
                .setInitialDelay(nextDelay, TimeUnit.MILLISECONDS)
                .build();

        getWorkManager()
            .enqueueUniqueWork(
                DOWNLOAD_WORK_NAME_DAILY,
                ExistingWorkPolicy.REPLACE,
                request
            );
    }

    /**
     * Set the download period to 1 hour
     */
    public void setHourlyBackgroundDownloadPeriod() {
        settings.setBackgroundDownloadHourly(true, () -> {
            updateBackgroundDownloads();
        });
    }

    /**
     * Number of millis to next daily download
     *
     * @return -1 if none to schedule
     */
    private long getNextDailyDownloadDelay(
        BackgroundDownloadSettings bgSettings
    ) {

        Set<String> days = bgSettings.days();
        int downloadTime = bgSettings.daysTime();

        long nextDownloadDelay = -1;

        for (String dayString : days) {
            int day = Integer.valueOf(dayString);
            long delay = getDelay(day, downloadTime);
            if (nextDownloadDelay < 0 || delay < nextDownloadDelay)
                nextDownloadDelay = delay;
        }

        return nextDownloadDelay;
    }

    private void cancelBackgroundDownloads() {
        getWorkManager().cancelUniqueWork(DOWNLOAD_WORK_NAME_HOURLY);
        getWorkManager().cancelUniqueWork(DOWNLOAD_WORK_NAME_DAILY);
    }

    private Constraints getConstraints(
        BackgroundDownloadSettings bgSettings
    ) {
        boolean requireUnmetered = bgSettings.requireUnmetered();
        boolean allowRoaming = bgSettings.allowRoaming();
        boolean requireCharging = bgSettings.requireCharging();

        Constraints.Builder constraintsBuilder = new Constraints.Builder();

        if (requireUnmetered)
            constraintsBuilder.setRequiredNetworkType(NetworkType.UNMETERED);
        else if (!allowRoaming)
            constraintsBuilder.setRequiredNetworkType(NetworkType.NOT_ROAMING);
        else
            constraintsBuilder.setRequiredNetworkType(NetworkType.CONNECTED);

        constraintsBuilder.setRequiresCharging(requireCharging);

        return constraintsBuilder.build();
    }

    /**
     * Get num millis to next day/time
     *
     * @param dayOfWeek 1-7 like java DayOfWeek
     * @param hourOfDay 0-23
     */
    private long getDelay(int dayOfWeek, int hourOfDay) {
        // start from now and adjust
        LocalDateTime now = LocalDateTime.now();
        int nowDayOfWeek = now.getDayOfWeek().getValue();

        LocalDateTime nextDownload
            = now.plusDays(dayOfWeek - nowDayOfWeek)
                .withHour(hourOfDay)
                .truncatedTo(ChronoUnit.HOURS);

        if (!nextDownload.isAfter(LocalDateTime.now()))
            nextDownload = nextDownload.plusDays(7);

        return ChronoUnit.MILLIS.between(now, nextDownload);
    }

    private WorkManager getWorkManager() { return workManager; }

    private abstract static class BaseDownloadWorker extends ListenableWorker {
        private static ExecutorService downloadExecutorService
            = Executors.newCachedThreadPool();

        ForkyzSettings settings;
        AndroidVersionUtils utils;
        FileHandlerProvider fileHandlerProvider;
        DownloadersProvider downloadersProvider;

        public BaseDownloadWorker(
            Context context,
            WorkerParameters params,
            AndroidVersionUtils utils,
            ForkyzSettings settings,
            FileHandlerProvider fileHandlerProvider,
            DownloadersProvider downloadersProvider
        ) {
            super(context, params);
            this.utils = utils;
            this.settings = settings;
            this.fileHandlerProvider = fileHandlerProvider;
            this.downloadersProvider = downloadersProvider;
        }

        protected void doDownload(
            CallbackToFutureAdapter.Completer<ListenableWorker.Result> completer
        ) {
            System.out.println("FORKYZ: do download");
            fileHandlerProvider.isMissingWritePermission(missing -> {
                if (missing) {
                    LOGGER.info("Skipping download, no write permission");
                    return;
                }

                LOGGER.info("Downloading most recent puzzles");

                System.out.println("FORKYZ: get dls");
                downloadersProvider.get(dls -> {
                downloadExecutorService.execute(() -> {
                    System.out.println("FORKYZ: um ok");
                    try {
                        LocalDate now = LocalDate.now();
                        dls.downloadLatestInRange(
                            now, now, dls.getAutoDownloaders()
                        );

                        completer.set(ListenableWorker.Result.success());

                        // This is used to tell BrowseActivity that
                        // puzzles may have been updated while paused.
                        settings.setBrowseNewPuzzle(true);
                    } catch (Exception e) {
                        completer.setException(e);
                    }
                });});
            });
        }
    }

    @HiltWorker
    public static class HourlyDownloadWorker extends BaseDownloadWorker {
        @AssistedInject
        public HourlyDownloadWorker(
            @Assisted Context context,
            @Assisted WorkerParameters params,
            AndroidVersionUtils utils,
            ForkyzSettings settings,
            FileHandlerProvider fileHandlerProvider,
            DownloadersProvider downloadersProvider
        ) {
            super(
                context, params,
                utils, settings, fileHandlerProvider, downloadersProvider
            );
        }

        @Override
        public ListenableFuture<ListenableWorker.Result> startWork() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                doDownload(completer);
                return "BaseDownloadWorker.doDownload";
            });
        }
    }

    @HiltWorker
    public static class DailyDownloadWorker extends BaseDownloadWorker {
        BackgroundDownloadManager manager;
        @AssistedInject
        public DailyDownloadWorker(
            @Assisted Context context,
            @Assisted WorkerParameters params,
            AndroidVersionUtils utils,
            ForkyzSettings settings,
            FileHandlerProvider fileHandlerProvider,
            BackgroundDownloadManager manager,
            DownloadersProvider downloadersProvider
        ) {
            super(
                context, params,
                utils, settings, fileHandlerProvider, downloadersProvider
            );
            this.manager = manager;
        }

        @Override
        public ListenableFuture<ListenableWorker.Result> startWork() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                settings.getBackgroundDownloadSettings(bgSettings -> {
                    manager.scheduleNextDailyDownload(bgSettings);
                });
                doDownload(completer);
                return "BaseDownloadWorker.doDownload";
            });
        }
    }
}
