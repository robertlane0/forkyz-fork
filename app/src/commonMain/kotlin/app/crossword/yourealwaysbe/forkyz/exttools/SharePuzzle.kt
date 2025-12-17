
package app.crossword.yourealwaysbe.forkyz.exttools

import kotlinx.coroutines.CoroutineScope

import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerShared
import app.crossword.yourealwaysbe.puz.Puzzle

class SharePuzzleData(
    val mimeType : String,
    val uri : Uri,
) : ExternalToolData() {
    companion object {
        suspend fun build(
            scope : CoroutineScope,
            utils : NativeBackendUtils,
            puz : Puzzle,
            writeBlank : Boolean,
            omitExtensions : Boolean,
            cb : (SharePuzzleData) -> Unit,
        ) {
            FileHandlerShared.getShareUri(
                scope,
                utils,
                puz,
                writeBlank,
                omitExtensions
            ) { puzUri ->
                val mimeType = FileHandlerShared.SHARE_URI_MIME_TYPE
                puzUri?.let { cb(SharePuzzleData(mimeType, it)) }
            }
        }
    }

    override suspend fun accept(launcher : ExternalToolLauncher) {
        launcher.visit(this)
    }
}

@Throws(AppNotFoundException::class)
expect suspend fun ExternalToolLauncher.visit(data : SharePuzzleData)

