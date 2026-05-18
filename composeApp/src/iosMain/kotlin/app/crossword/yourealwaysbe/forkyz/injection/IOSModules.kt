
package app.crossword.yourealwaysbe.forkyz.injection

import androidx.datastore.core.DataStore

import okio.FileSystem
import okio.Path.Companion.toPath

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

import app.crossword.yourealwaysbe.forkyz.IOSAppViewModel
import app.crossword.yourealwaysbe.forkyz.exttools.IOSMCPBoardManager
import app.crossword.yourealwaysbe.forkyz.exttools.MCPBoardManager
import app.crossword.yourealwaysbe.forkyz.settings.DEFAULT_SETTINGS
import app.crossword.yourealwaysbe.forkyz.settings.SETTINGS_FILE_NAME
import app.crossword.yourealwaysbe.forkyz.settings.Settings
import app.crossword.yourealwaysbe.forkyz.settings.createDataStore
import app.crossword.yourealwaysbe.forkyz.util.BackgroundDownloadManager
import app.crossword.yourealwaysbe.forkyz.util.IOS_APP_SUPPORT_DIR
import app.crossword.yourealwaysbe.forkyz.util.IOSBackgroundDownloadManager
import app.crossword.yourealwaysbe.forkyz.util.IOSNativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.IOSNotifications
import app.crossword.yourealwaysbe.forkyz.util.IOSPuzzleSaveServiceCaller
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Notifications
import app.crossword.yourealwaysbe.forkyz.util.PuzzleSaveServiceCaller
import app.crossword.yourealwaysbe.forkyz.util.files.File
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.files.IOSFileHandlerProvider

val iosModules = module {
    single<BackgroundDownloadManager> {
        IOSBackgroundDownloadManager(get(), get())
    }
    single<DataStore<Settings>> {
        createDataStore(
            fileSystem = FileSystem.SYSTEM,
            producePath = {
                File(IOS_APP_SUPPORT_DIR, SETTINGS_FILE_NAME)
                    .pathString
                    .toPath()
            },
            defaultSettings = DEFAULT_SETTINGS,
        )
    }
    single<FileHandlerProvider> { IOSFileHandlerProvider(get()) }
    single<NativeBackendUtils> { IOSNativeBackendUtils(get()) }
    single<Notifications> { IOSNotifications() }
    single<PuzzleSaveServiceCaller> { IOSPuzzleSaveServiceCaller() }
    single<MCPBoardManager> { IOSMCPBoardManager(get(), get(), get()) }
    singleOf(::IOSAppViewModel)
}
