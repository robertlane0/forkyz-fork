
package app.crossword.yourealwaysbe.forkyz.util.files;

import java.io.IOException;
import java.util.logging.Logger;

import android.content.Context;

import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;

/**
 * File storage in the default internal app storage location
 */
public class FileHandlerInternal extends FileHandlerJavaFile {
    private static final Logger LOGGER
        = Logger.getLogger(FileHandlerInternal.class.getCanonicalName());

    public FileHandlerInternal(Context applicationContext) {
        super(applicationContext, applicationContext.getFilesDir());
    }

    @Override
    public boolean needsWriteExternalStoragePermission() { return false; }

    @Override
    public boolean isStorageMounted() {
        return true;
    }

    @Override
    public boolean isStorageFull(AndroidVersionUtils utils) {
        try {
            return utils.isInternalStorageFull(
                getApplicationContext(), MINIMUM_STORAGE_REQUIRED
            );
        } catch (IOException e) {
            // we don't know it's not full...
            return false;
        }
    }
}
