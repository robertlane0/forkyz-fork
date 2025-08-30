
package app.crossword.yourealwaysbe.forkyz.util.files

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

import android.content.Context
import android.widget.Toast
import androidx.annotation.MainThread

import dagger.hilt.android.qualifiers.ApplicationContext

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.util.defaultScope
import app.crossword.yourealwaysbe.forkyz.util.getOnceNotNull
import app.crossword.yourealwaysbe.forkyz.util.stateInSubscribed
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

@Singleton
class FileHandlerProvider @Inject constructor(
     @ApplicationContext private val applicationContext : Context,
    private val utils : AndroidVersionUtils,
    private val settings : ForkyzSettings,
) {
    // state flow to ensure only once file handler unless settings
    // change
    val liveFileHandler : StateFlow<FileHandler?>
        = settings.liveFileHandlerSettings
            .map(this::createFileHandler)
            .stateInSubscribed(defaultScope, null)

    @MainThread
    fun get(cb : (FileHandler) -> Unit) {
        liveFileHandler.getOnceNotNull(cb)
    }

    private fun createFileHandler(
        fileHandlerSettings : FileHandlerSettings,
    ) : FileHandler {
        var fh : FileHandler?

        val loc = fileHandlerSettings.storageLocation

        when (loc) {
            StorageLocation.SL_EXTERNAL_SAF -> {
                fh = FileHandlerSAF.readHandlerFromSettings(
                    applicationContext, fileHandlerSettings, settings
                )
            }
            else -> {
                fh = FileHandlerInternal(applicationContext)
            }
        }

        if (fh == null || !fh.isStorageMounted()) {
            fh = FileHandlerInternal(applicationContext)
            val t = Toast.makeText(
                applicationContext,
                R.string.storage_problem_falling_back_internal,
                Toast.LENGTH_LONG
            )
            t.show()
        }

        if (fh.isStorageFull(utils)) {
            val t = Toast.makeText(
                applicationContext,
                R.string.storage_full_warning,
                Toast.LENGTH_LONG
            )
            t.show()
        }

        return fh
    }
}
