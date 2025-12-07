
package app.crossword.yourealwaysbe.forkyz

import android.app.Application

import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

import app.crossword.yourealwaysbe.forkyz.injection.androidModule
import app.crossword.yourealwaysbe.forkyz.injection.sharedModule
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.MigrationHelper
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

class ForkyzApplication() : Application() {
    private val utils : AndroidVersionUtils by inject()
    private val themeHelper : ThemeHelper by inject()
    private val migrationHelper : MigrationHelper by inject()
    private val settings : ForkyzSettings by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@ForkyzApplication)
            modules(androidModule, sharedModule)
            workManagerFactory()
        }

        migrationHelper.applyMigrations(this)
        utils.createNotificationChannels(this)
    }
}
