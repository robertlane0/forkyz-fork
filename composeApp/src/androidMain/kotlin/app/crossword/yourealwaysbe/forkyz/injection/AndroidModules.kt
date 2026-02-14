
package app.crossword.yourealwaysbe.forkyz.injection

import androidx.datastore.core.DataStore
import androidx.work.WorkManager

import okio.FileSystem
import okio.Path.Companion.toPath

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

import app.crossword.yourealwaysbe.forkyz.settings.SETTINGS_FILE_NAME
import app.crossword.yourealwaysbe.forkyz.settings.Settings
import app.crossword.yourealwaysbe.forkyz.settings.createDataStore
import app.crossword.yourealwaysbe.forkyz.util.AndroidBackgroundDownloadManager
import app.crossword.yourealwaysbe.forkyz.util.AndroidNativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.AndroidNotifications
import app.crossword.yourealwaysbe.forkyz.util.AndroidPuzzleSaveServiceCaller
import app.crossword.yourealwaysbe.forkyz.util.BackgroundDownloadManager
import app.crossword.yourealwaysbe.forkyz.util.DailyDownloadWorker
import app.crossword.yourealwaysbe.forkyz.util.HourlyDownloadWorker
import app.crossword.yourealwaysbe.forkyz.util.MigrationHelper
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Notifications
import app.crossword.yourealwaysbe.forkyz.util.OrientationHelper
import app.crossword.yourealwaysbe.forkyz.util.PuzzleSaveServiceCaller
import app.crossword.yourealwaysbe.forkyz.util.files.AndroidFileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

val androidModules = module {
    single<AndroidVersionUtils> {
        AndroidVersionUtils.Factory.provideAndroidVersionUtils()
    }
    single<BackgroundDownloadManager> {
        AndroidBackgroundDownloadManager(get(), get())
    }
    single<DataStore<Settings>> {
        createDataStore(
            fileSystem = FileSystem.SYSTEM,
            producePath = {
                androidContext()
                    .filesDir
                    // to match pure Android datastore
                    .resolve("datastore")
                    .resolve(SETTINGS_FILE_NAME)
                    .absolutePath
                    .toPath()
            },
        )
    }
    single<FileHandlerProvider> {
        AndroidFileHandlerProvider(
            androidContext(),
            get(),
            get(),
        )
    }
    single<NativeBackendUtils> {
        AndroidNativeBackendUtils(androidContext(), get(), get())
    }
    single<Notifications> {
        AndroidNotifications(androidContext(), get())
    }
    single<PuzzleSaveServiceCaller> {
        AndroidPuzzleSaveServiceCaller(androidContext())
    }
    single<WorkManager> { WorkManager.getInstance(androidContext()) }
    singleOf(::MigrationHelper)
    singleOf(::OrientationHelper)
    workerOf(::DailyDownloadWorker)
    workerOf(::HourlyDownloadWorker)
}

