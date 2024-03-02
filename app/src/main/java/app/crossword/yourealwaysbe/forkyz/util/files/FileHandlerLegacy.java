
package app.crossword.yourealwaysbe.forkyz.util.files;

import java.util.logging.Logger;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;

/**
 * Implementation of original Shortyz file access directly working with
 * external SD card directory.
 */
@SuppressWarnings("deprecation")
public class FileHandlerLegacy extends FileHandlerJavaFile {
    private static final Logger LOGGER
        = Logger.getLogger(FileHandlerLegacy.class.getCanonicalName());

    @SuppressWarnings("deprecation")
    public FileHandlerLegacy(Context applicationContext) {
        super(applicationContext, Environment.getExternalStorageDirectory());
    }

    @Override
    public boolean needsWriteExternalStoragePermission() { return true; }

    @Override
    public boolean isStorageMounted() {
        return Environment.MEDIA_MOUNTED.equals(
            Environment.getExternalStorageState()
        );
    }

    @Override
    public boolean isStorageFull(AndroidVersionUtils utils) {
        StatFs stats = new StatFs(
            Environment.getExternalStorageDirectory().getAbsolutePath()
        );
        return (
            stats.getAvailableBlocksLong() * stats.getBlockSizeLong()
                < MINIMUM_STORAGE_REQUIRED
        );
    }
}
