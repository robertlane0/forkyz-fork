
package app.crossword.yourealwaysbe.forkyz.util.files;

import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import dagger.hilt.android.qualifiers.ApplicationContext;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation;
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
            settings.liveFileHandlerSettings(),
            this::createFileHandler
        );
    }

    public void get(Consumer<FileHandler> cb) {
        liveFileHandler.observeForever(new Observer<FileHandler>() {
            @Override
            public void onChanged(FileHandler fh) {
                liveFileHandler.removeObserver(this);
                cb.accept(fh);
            }
        });
    }

    public LiveData<FileHandler> liveFileHandler() {
        return liveFileHandler;
    }

    /**
     * Returns true if WRITE_EXTERNAL_STORAGE permission is needed/missing
     */
    public void isMissingWritePermission(Consumer<Boolean> cb) {
        get(fileHandler -> {
            boolean needsPerm
                = fileHandler.needsWriteExternalStoragePermission();

            cb.accept(
                needsPerm
                && ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            );
        });
    }

    private FileHandler createFileHandler(
        FileHandlerSettings fileHandlerSettings
    ) {
        FileHandler fh;

        StorageLocation loc = fileHandlerSettings.storageLocation();

        switch (loc) {
        case EXTERNAL_LEGACY:
            fh = new FileHandlerLegacy(applicationContext);
            break;
        case EXTERNAL_SAF:
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
