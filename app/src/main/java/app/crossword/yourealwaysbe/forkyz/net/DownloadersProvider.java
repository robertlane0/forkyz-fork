
package app.crossword.yourealwaysbe.forkyz.net;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

import android.content.Context;
import androidx.core.app.NotificationManagerCompat;

import dagger.hilt.android.qualifiers.ApplicationContext;

import app.crossword.yourealwaysbe.forkyz.settings.DownloadersSettings;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.util.LiveDataUtilsKt;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;

/**
 * Convenience for constructing Downloaders objects
 *
 * Since most arguments are injectable, no need to fetch them in all
 * places.
 */
@Singleton
public class DownloadersProvider {
    private Context applicationContext;
    private AndroidVersionUtils utils;
    private FileHandlerProvider fileHandlerProvider;
    private ForkyzSettings settings;
    private MediatorLiveData<Downloaders> liveDownloadersMediator;

    @Inject
    public DownloadersProvider(
        @ApplicationContext Context applicationContext,
        AndroidVersionUtils utils,
        ForkyzSettings settings,
        FileHandlerProvider fileHandlerProvider
    ) {
        this.applicationContext = applicationContext;
        this.utils = utils;
        this.settings = settings;
        this.fileHandlerProvider = fileHandlerProvider;
    }

    public void get(Consumer<Downloaders> cb) {
        LiveDataUtilsKt.observeOnce(liveDownloaders(), cb);
    }

    public LiveData<Downloaders> liveDownloaders() {
        if (liveDownloadersMediator != null)
            return liveDownloadersMediator;

        liveDownloadersMediator = new MediatorLiveData<>();

        LiveData<FileHandler> liveFileHandler
            = fileHandlerProvider.liveFileHandler();
        LiveData<DownloadersSettings> liveDownloadersSettings
            = settings.liveDownloadersSettings();

        liveDownloadersMediator.addSource(
            liveFileHandler,
            fileHandler -> {
                DownloadersSettings downloadersSettings
                    = liveDownloadersSettings.getValue();
                if (downloadersSettings != null && fileHandler != null) {
                    liveDownloadersMediator.setValue(
                        constructFromSettings(
                            downloadersSettings,
                            fileHandler
                        )
                    );
                }
            }
        );

        liveDownloadersMediator.addSource(
            liveDownloadersSettings,
            downloadersSettings -> {
                FileHandler fileHandler = liveFileHandler.getValue();
                if (downloadersSettings != null && fileHandler != null) {
                    liveDownloadersMediator.setValue(
                        constructFromSettings(
                            downloadersSettings,
                            fileHandler
                        )
                    );
                }
            }
        );

        return liveDownloadersMediator;
    }

    private Downloaders constructFromSettings(
        DownloadersSettings downloadersSettings, FileHandler fileHandler
    ) {
        return new Downloaders(
            applicationContext,
            utils,
            fileHandler,
            downloadersSettings,
            NotificationManagerCompat.from(applicationContext)
        );
    }
}
