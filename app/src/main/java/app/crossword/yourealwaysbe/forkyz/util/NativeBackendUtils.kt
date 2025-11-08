
package app.crossword.yourealwaysbe.forkyz.util

import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

import android.content.Context
import android.net.Uri

import dagger.hilt.android.qualifiers.ApplicationContext

import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

/**
 * Collects functions that viewmodels &c. need that depend on context
 */
@Singleton
class NativeBackendUtils @Inject constructor(
    @param:ApplicationContext private val applicationContext : Context,
    private val utils : AndroidVersionUtils,
) {
    suspend fun getString(resourceID : Int) : String {
        return applicationContext.getString(resourceID)
    }

    suspend fun getString(resourceID : Int, vararg formatArgs: Any) : String {
        return applicationContext.getString(resourceID, *formatArgs)
    }

    fun hasNetworkConnection() : Boolean {
        return utils.hasNetworkConnection(applicationContext)
    }

    fun getApplicationVersionName() : String {
        return utils.getApplicationVersionName(applicationContext)
    }

    @Throws(IOException::class)
    fun openBufferedInputStream(uri : Uri) : InputStream {
        return BufferedInputStream(
            applicationContext.getContentResolver().openInputStream(uri)
        )
    }
}

