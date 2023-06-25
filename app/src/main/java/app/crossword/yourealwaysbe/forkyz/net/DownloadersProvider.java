
package app.crossword.yourealwaysbe.forkyz.net;

import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

import android.content.Context;
import androidx.core.app.NotificationManagerCompat;

import dagger.hilt.android.qualifiers.ApplicationContext;

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
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
    ForkyzSettings settings;

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
        fileHandlerProvider.get(fileHandler -> {
        settings.getDownloadersSettings(downloadersSettings -> {
            cb.accept(new Downloaders(
                applicationContext,
                utils,
                fileHandler,
                downloadersSettings,
                NotificationManagerCompat.from(applicationContext)
            ));
        });});
    }
}
