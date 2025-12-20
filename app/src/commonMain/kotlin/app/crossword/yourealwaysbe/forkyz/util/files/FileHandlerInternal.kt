package app.crossword.yourealwaysbe.forkyz.util.files

import java.io.File
import java.io.IOException

import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils

/**
 * File storage in the default internal app storage location
 */
class FileHandlerInternal(
    utils : NativeBackendUtils,
) : FileHandlerJavaFile(
    utils,
    File(utils.getAppFilesDir(), "crosswords"),
) {
    override val isStorageMounted : Boolean = true

    override val settings : FileHandlerSettings = FileHandlerSettings(
        StorageLocation.SL_INTERNAL,
        "",
        "",
        "",
        "",
        "",
        "",
    )

    override val isStorageFull : Boolean
        get() {
            try {
                return nativeBackendUtils
                    .isInternalStorageFull(
                        FileHandler.MINIMUM_STORAGE_REQUIRED,
                    )
            } catch (e : IOException) {
                // we don't know it's not full...
                return false
            }
        }
}
