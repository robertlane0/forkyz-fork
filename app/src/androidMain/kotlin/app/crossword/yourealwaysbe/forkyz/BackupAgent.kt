
package app.crossword.yourealwaysbe.forkyz

import android.app.backup.BackupAgentHelper
import android.app.backup.SharedPreferencesBackupHelper

private val PREFS = "app.crossword.yourealwaysbe.forkyz_preferences"
private val PREFS_BACKUP_KEY = "prefs"

class BackupAgent : BackupAgentHelper() {
    override fun onCreate() {
        val helper = SharedPreferencesBackupHelper(this, PREFS)
        addHelper(PREFS_BACKUP_KEY, helper)
    }
}

