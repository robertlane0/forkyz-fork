
package app.crossword.yourealwaysbe.forkyz.util.files;

import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.MainThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import dagger.hilt.android.qualifiers.ApplicationContext;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation;
import app.crossword.yourealwaysbe.forkyz.util.LiveDataUtilsKt;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;

@Singleton
public class FileHandlerProvider {
    private Context applicationContext;
    private ForkyzSettings settings;

    private AndroidVersionUtils utils;
    private LiveData<FileHandler> liveFileHandler;

    @Inject
    public FileHandlerProvider(
        @ApplicationContext Context applicationContext,
        AndroidVersionUtils utils,
        ForkyzSettings settings
    ) {
        this.applicationContext = applicationContext;
        this.utils = utils;
        this.settings = settings;
        this.liveFileHandler = Transformations.map(
            settings.getLiveFileHandlerSettings(),
            this::createFileHandler
        );
    }

    @MainThread
    public void get(Consumer<FileHandler> cb) {
        LiveDataUtilsKt.observeOnce(liveFileHandler, cb);
    }

    public LiveData<FileHandler> liveFileHandler() {
        return liveFileHandler;
    }

    private FileHandler createFileHandler(
        FileHandlerSettings fileHandlerSettings
    ) {
        FileHandler fh;

        StorageLocation loc = fileHandlerSettings.getStorageLocation();

        switch (loc) {
        case SL_EXTERNAL_SAF:
            fh = FileHandlerSAF.readHandlerFromSettings(
                applicationContext, fileHandlerSettings, settings
            );
            break;
        default:
            fh = new FileHandlerInternal(applicationContext);
            break;
        }

        if (fh == null || !fh.isStorageMounted()) {
            fh = new FileHandlerInternal(applicationContext);
            Toast t = Toast.makeText(
                applicationContext,
                R.string.storage_problem_falling_back_internal,
                Toast.LENGTH_LONG
            );
            t.show();
        }

        if (fh.isStorageFull(utils)) {
            Toast t = Toast.makeText(
                applicationContext,
                R.string.storage_full_warning,
                Toast.LENGTH_LONG
            );
            t.show();
        }

        return fh;
    }
}
