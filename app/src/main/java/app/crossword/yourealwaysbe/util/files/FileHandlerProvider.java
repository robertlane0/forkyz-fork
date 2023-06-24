
package app.crossword.yourealwaysbe.util.files;

import android.content.Context;
import android.widget.Toast;

import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation;

@Singleton
public class FileHandlerProvider {
    private Context applicationContext;
    private ForkyzSettings settings;

    private FileHandler fileHandler;

    @Inject
    public FileHandlerProvider(
        @ApplicationContext Context applicationContext,
        ForkyzSettings settings
    ) {
        this.applicationContext = applicationContext;
        this.settings = settings;
    }

    public void get(Consumer<FileHandler> cb) {
        if (fileHandler != null) {
            cb.accept(fileHandler);
        } else {
            createFileHandler(fh -> {
                fileHandler = fh;
                cb.accept(fh);
            });
        }
    }

    public void reloadFileHandler() {
        fileHandler = null;
    }

    private void createFileHandler(Consumer<FileHandler> cb) {
        settings.getFileHandlerSettings(fileHandlerSettings -> {
            FileHandler fh;

            StorageLocation loc = fileHandlerSettings.getStorageLocation();

            switch (loc) {
            case EXTERNAL_LEGACY:
                fh = new FileHandlerLegacy(applicationContext);
                break;
            case EXTERNAL_SAF:
                fh = FileHandlerSAF.readHandlerFromSettings(
                    applicationContext, fileHandlerSettings
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

            if (fh.isStorageFull()) {
                Toast t = Toast.makeText(
                    applicationContext,
                    R.string.storage_full_warning,
                    Toast.LENGTH_LONG
                );
                t.show();
            }

            cb.accept(fh);
        });
    }
}
