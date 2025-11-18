
package app.crossword.yourealwaysbe.forkyz.util

import android.content.Context
import androidx.preference.PreferenceManager

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.migratePreferences

class MigrationHelper(private val settings : ForkyzSettings) {
    fun applyMigrations(context : Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        migratePreferences(prefs, settings)
    }
}

