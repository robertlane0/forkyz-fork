
package app.crossword.yourealwaysbe.forkyz.util

import javax.inject.Inject
import javax.inject.Singleton

import android.content.Context

import dagger.hilt.android.qualifiers.ApplicationContext

import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

/**
 * Collects functions that viewmodels &c. need that depend on context
 */
@Singleton
class NativeBackendUtils @Inject constructor(
    @param:ApplicationContext private val applicationContext : Context,
    private val utils : AndroidVersionUtils,
) {
    // TODO: will need to become suspend fun
    fun getString(resourceID : Int) : String {
        return applicationContext.getString(resourceID)
    }

    fun getString(resourceID : Int, vararg formatArgs: Any) : String {
        return applicationContext.getString(resourceID, formatArgs)
    }

    fun hasNetworkConnection() : Boolean {
        return utils.hasNetworkConnection(applicationContext)
    }
}

