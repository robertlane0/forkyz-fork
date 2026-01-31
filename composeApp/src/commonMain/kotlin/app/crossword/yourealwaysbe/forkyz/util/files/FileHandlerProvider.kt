
package app.crossword.yourealwaysbe.forkyz.util.files

import kotlinx.coroutines.flow.StateFlow

interface FileHandlerProvider {
    val liveFileHandler : StateFlow<FileHandler?>
    fun get(cb : (FileHandler) -> Unit)
}
