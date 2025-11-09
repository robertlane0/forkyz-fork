
package app.crossword.yourealwaysbe.forkyz.util.files;

import java.io.IOException;

import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils;
import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings;
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation;

/**
 * File storage in the default internal app storage location
 */
public class FileHandlerInternal extends FileHandlerJavaFile {
    public FileHandlerInternal(NativeBackendUtils utils) {
        super(utils, utils.getAppFilesDir());
    }

    @Override
    public boolean isStorageMounted() {
        return true;
    }

    @Override
    public FileHandlerSettings getSettings() {
        return new FileHandlerSettings(
            StorageLocation.SL_INTERNAL,
            "",
            "",
            "",
            "",
            "",
            ""
        );
    }

    @Override
    public boolean isStorageFull() {
        try {
            return getNativeBackendUtils()
                .isInternalStorageFull(MINIMUM_STORAGE_REQUIRED);
        } catch (IOException e) {
            // we don't know it's not full...
            return false;
        }
    }
}
