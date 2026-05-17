
package app.crossword.yourealwaysbe.forkyz.puz.io

import org.jetbrains.compose.resources.getString

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.default_across_list_name
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.default_down_list_name
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.default_nodir_list_name
import app.crossword.yourealwaysbe.forkyz.util.AppStrings

/**
 * Access default list names for IO classes
 *
 * Not using NativeBackendUtils here for legacy reasons. Was once a JVM
 * resource lookup. A bit much to require IO classes to be injected or passed
 * NativeBackendUtils just for a KMP string lookup.
 *
 * Strings are cached for fast repeated lookup.
 */
object DefaultListNames {
    private val cache = AppStrings()

    suspend fun acrossList() : String {
        return cache.getString(Res.string.default_across_list_name)
    }

    suspend fun downList() : String {
        return cache.getString(Res.string.default_down_list_name)
    }

    suspend fun noDirList() : String {
        return getString(Res.string.default_nodir_list_name)
    }
}

