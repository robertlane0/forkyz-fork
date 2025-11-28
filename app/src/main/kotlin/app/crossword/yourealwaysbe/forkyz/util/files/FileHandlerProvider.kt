
package app.crossword.yourealwaysbe.forkyz.util.files

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

import android.content.Context
import android.widget.Toast
import androidx.annotation.MainThread

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.getOnceNotNull
import app.crossword.yourealwaysbe.forkyz.util.stateInSubscribed

class FileHandlerProvider(
    private val applicationContext : Context,
    private val utils : NativeBackendUtils,
    private val settings : ForkyzSettings,
) {
    // state flow to ensure only once file handler unless settings
    // change
    @OptIn(DelicateCoroutinesApi::class)
    val liveFileHandler : StateFlow<FileHandler?>
        = settings.liveFileHandlerSettings
            .map(this::createFileHandler)
            .stateInSubscribed(GlobalScope, null)

    @MainThread
    fun get(cb : (FileHandler) -> Unit) {
        liveFileHandler.getOnceNotNull(cb)
    }

    // so can launch toasts
    @OptIn(DelicateCoroutinesApi::class)
    private fun createFileHandler(
        fileHandlerSettings : FileHandlerSettings,
    ) : FileHandler {
        var fh : FileHandler?

        val loc = fileHandlerSettings.storageLocation

        when (loc) {
            StorageLocation.SL_EXTERNAL_SAF -> {
                fh = FileHandlerSAF.readHandlerFromSettings(
                    applicationContext, utils, fileHandlerSettings, settings
                )
            }
            else -> {
                fh = FileHandlerInternal(utils)
            }
        }

        if (fh == null || !fh.isStorageMounted) {
            fh = FileHandlerInternal(utils)
            GlobalScope.launch(Dispatchers.Main) {
                val t = Toast.makeText(
                    applicationContext,
                    R.string.storage_problem_falling_back_internal,
                    Toast.LENGTH_LONG
                )
                t.show()
            }
        }

        if (fh.isStorageFull) {
            GlobalScope.launch(Dispatchers.Main) {
                val t = Toast.makeText(
                    applicationContext,
                    R.string.storage_full_warning,
                    Toast.LENGTH_LONG
                )
                t.show()
            }
        }

        return fh
    }
}
