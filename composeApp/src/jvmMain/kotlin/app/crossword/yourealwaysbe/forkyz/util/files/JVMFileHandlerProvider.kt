
package app.crossword.yourealwaysbe.forkyz.util.files

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerInternal
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerJVMSAF
import app.crossword.yourealwaysbe.forkyz.util.stateInSubscribed
import app.crossword.yourealwaysbe.forkyz.util.getOnceNotNull

class JVMFileHandlerProvider(
    val settings : ForkyzSettings,
    val utils : NativeBackendUtils,
) : FileHandlerProvider {
    private val fileHandler = FileHandlerInternal(utils)

    // state flow to ensure only one file handler unless settings
    // change
    @OptIn(DelicateCoroutinesApi::class)
    override val liveFileHandler : StateFlow<FileHandler?>
        = settings.liveFileHandlerSettings
            .map(this::createFileHandler)
            .stateInSubscribed(GlobalScope, null)

    override fun get(cb : (FileHandler) -> Unit) {
        liveFileHandler.getOnceNotNull(cb)
    }

    private fun createFileHandler(
        fileHandlerSettings : FileHandlerSettings,
    ) : FileHandler {
        when (fileHandlerSettings.storageLocation) {
            StorageLocation.SL_EXTERNAL_SAF -> {
                return FileHandlerJVMSAF.readHandlerFromSettings(
                    utils,
                    fileHandlerSettings,
                )
            }
            else -> {
                return FileHandlerInternal(utils)
            }
        }
    }
}

