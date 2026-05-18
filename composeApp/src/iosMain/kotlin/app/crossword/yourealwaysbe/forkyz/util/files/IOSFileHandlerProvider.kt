
package app.crossword.yourealwaysbe.forkyz.util.files

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.getOnceNotNull

class IOSFileHandlerProvider(
    val utils : NativeBackendUtils,
) : FileHandlerProvider {
    private val fileHandler = FileHandlerInternal(utils)

    @OptIn(DelicateCoroutinesApi::class)
    override val liveFileHandler : StateFlow<FileHandler?>
        = MutableStateFlow(fileHandler)

    override fun get(cb : (FileHandler) -> Unit) {
        liveFileHandler.getOnceNotNull(cb)
    }
}
