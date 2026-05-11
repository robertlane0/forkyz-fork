
package app.crossword.yourealwaysbe.forkyz.util.files

import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.File
import app.crossword.yourealwaysbe.forkyz.util.toUri

/**
 * File storage in the default internal app storage location
 */
class FileHandlerJVMSAF(
    utils : NativeBackendUtils,
    rootDir : Uri,
) : FileHandlerSystemFile(utils, File(rootDir.getPath()!!)) {
    override val isStorageMounted : Boolean = true

    override val settings : FileHandlerSettings = FileHandlerSettings(
        StorageLocation.SL_EXTERNAL_SAF,
        rootDir.toString(),
        "",
        "",
        "",
        "",
        "",
    )

    override val isStorageFull : Boolean = false

    companion object {
        fun readHandlerFromSettings(
            utils : NativeBackendUtils,
            settings : FileHandlerSettings,
        ) : FileHandler {
            return FileHandlerJVMSAF(
                utils,
                settings.safRootURI.toUri(),
            )
        }
    }
}
