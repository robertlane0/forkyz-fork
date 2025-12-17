
package app.crossword.yourealwaysbe.forkyz.util.files

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerInternal

class JVMFileHandlerProvider(
    val utils : NativeBackendUtils,
) : FileHandlerProvider {
    private val fileHandler = FileHandlerInternal(utils)

    override val liveFileHandler : StateFlow<FileHandler?>
        = MutableStateFlow<FileHandler?>(fileHandler)

    override fun get(cb : (FileHandler) -> Unit) {
        cb(fileHandler)
    }
}

