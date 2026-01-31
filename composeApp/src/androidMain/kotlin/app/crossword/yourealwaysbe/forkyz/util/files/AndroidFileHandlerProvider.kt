
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

import org.jetbrains.compose.resources.getString

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.storage_full_warning
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.storage_problem_falling_back_internal
import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.getOnceNotNull
import app.crossword.yourealwaysbe.forkyz.util.stateInSubscribed

class AndroidFileHandlerProvider(
    private val applicationContext : Context,
    private val utils : NativeBackendUtils,
    private val settings : ForkyzSettings,
) : FileHandlerProvider {
    // state flow to ensure only once file handler unless settings
    // change
    @OptIn(DelicateCoroutinesApi::class)
    override val liveFileHandler : StateFlow<FileHandler?>
        = settings.liveFileHandlerSettings
            .map(this::createFileHandler)
            .stateInSubscribed(GlobalScope, null)

    @MainThread
    override fun get(cb : (FileHandler) -> Unit) {
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
                    getString(Res.string.storage_problem_falling_back_internal),
                    Toast.LENGTH_LONG
                )
                t.show()
            }
        }

        if (fh.isStorageFull) {
            GlobalScope.launch(Dispatchers.Main) {
                val t = Toast.makeText(
                    applicationContext,
                    getString(Res.string.storage_full_warning),
                    Toast.LENGTH_LONG
                )
                t.show()
            }
        }

        return fh
    }
}
