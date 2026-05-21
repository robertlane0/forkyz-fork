
package app.crossword.yourealwaysbe.forkyz.injection


import androidx.datastore.core.DataStore

import okio.FileSystem
import okio.Path.Companion.toPath

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

import app.crossword.yourealwaysbe.forkyz.JVMAppViewModel
import app.crossword.yourealwaysbe.forkyz.exttools.JVMMCPBoardManager
import app.crossword.yourealwaysbe.forkyz.exttools.MCPBoardManager
import app.crossword.yourealwaysbe.forkyz.settings.ClueTabsDouble
import app.crossword.yourealwaysbe.forkyz.settings.DEFAULT_SETTINGS
import app.crossword.yourealwaysbe.forkyz.settings.FitToScreenMode
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardMode
import app.crossword.yourealwaysbe.forkyz.settings.SETTINGS_FILE_NAME
import app.crossword.yourealwaysbe.forkyz.settings.Settings
import app.crossword.yourealwaysbe.forkyz.settings.createDataStore
import app.crossword.yourealwaysbe.forkyz.util.BackgroundDownloadManager
import app.crossword.yourealwaysbe.forkyz.util.JVMBackgroundDownloadManager
import app.crossword.yourealwaysbe.forkyz.util.JVMNativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.JVMNotifications
import app.crossword.yourealwaysbe.forkyz.util.JVMPuzzleSaveServiceCaller
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Notifications
import app.crossword.yourealwaysbe.forkyz.util.PuzzleSaveServiceCaller
import app.crossword.yourealwaysbe.forkyz.util.XDG_DATA_DIR
import app.crossword.yourealwaysbe.forkyz.util.files.File
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.files.JVMFileHandlerProvider

val jvmModules = module {
    single<BackgroundDownloadManager> {
        JVMBackgroundDownloadManager(get(), get())
    }
    single<DataStore<Settings>> {
        createDataStore(
            fileSystem = FileSystem.SYSTEM,
            producePath = {
                File(XDG_DATA_DIR, SETTINGS_FILE_NAME)
                    .pathString
                    .toPath()
            },
            defaultSettings = DEFAULT_SETTINGS.copy(
                clueTabsDouble = ClueTabsDouble.CTD_WIDE,
                fitToScreenMode = FitToScreenMode.FTSM_LOCKED,
                keyboardShowHide = KeyboardMode.KM_NEVER_SHOW,
            )
        )
    }
    single<FileHandlerProvider> { JVMFileHandlerProvider(get(), get()) }
    single<NativeBackendUtils> { JVMNativeBackendUtils(get()) }
    single<Notifications> { JVMNotifications() }
    single<PuzzleSaveServiceCaller> { JVMPuzzleSaveServiceCaller() }
    single<MCPBoardManager> { JVMMCPBoardManager(get(), get(), get()) }
    singleOf(::JVMAppViewModel)
}

