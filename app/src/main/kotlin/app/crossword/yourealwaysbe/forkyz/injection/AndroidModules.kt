
package app.crossword.yourealwaysbe.forkyz.injection

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.work.WorkManager

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

import app.crossword.yourealwaysbe.forkyz.settings.Settings
import app.crossword.yourealwaysbe.forkyz.settings.SettingsSerializer
import app.crossword.yourealwaysbe.forkyz.util.DailyDownloadWorker
import app.crossword.yourealwaysbe.forkyz.util.HourlyDownloadWorker
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Notifications
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

private const val SETTINGS_FILE_NAME = "settings.pb"

val Context.dataStore: DataStore<Settings> by dataStore(
    fileName = SETTINGS_FILE_NAME,
    serializer = SettingsSerializer,
)

val androidModule = module {
    single<AndroidVersionUtils> {
        AndroidVersionUtils.Factory.provideAndroidVersionUtils()
    }
    single<DataStore<Settings>> { androidContext().dataStore }
    singleOf(::FileHandlerProvider)
    singleOf(::NativeBackendUtils)
    singleOf(::Notifications)
    single<WorkManager> { WorkManager.getInstance(androidContext()) }
    workerOf(::DailyDownloadWorker)
    workerOf(::HourlyDownloadWorker)
}

