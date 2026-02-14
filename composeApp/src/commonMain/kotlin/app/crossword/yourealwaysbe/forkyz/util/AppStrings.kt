
package app.crossword.yourealwaysbe.forkyz.util

import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

private val CACHE_SIZE = 1000 // strings

/**
 * Cached version of KMP getString
 *
 * getString is slowish on JVM at least, so cache the last requested
 * strings.
 */
class AppStrings {
    private val cache = LRUCache<List<Any>, String>(CACHE_SIZE)

    suspend fun getString(resourceID : StringResource) : String {
        return cache.getOrPut(listOf(resourceID)) {
            org.jetbrains.compose.resources.getString(resourceID)
        }
    }

    suspend fun getString(
        resourceID : StringResource,
        vararg formatArgs: Any,
    ) : String {
        return cache.getOrPut(listOf(resourceID) + formatArgs) {
            org.jetbrains.compose.resources.getString(
                resourceID,
                *formatArgs,
            )
        }
    }

    suspend fun getPluralString(
        resourceID : PluralStringResource,
        quantity : Int,
        vararg formatArgs: Any,
    ) : String {
        return cache.getOrPut(listOf(resourceID, quantity) + formatArgs) {
            org.jetbrains.compose.resources.getPluralString(
                resourceID,
                quantity,
                *formatArgs,
            )
        }
    }
}

