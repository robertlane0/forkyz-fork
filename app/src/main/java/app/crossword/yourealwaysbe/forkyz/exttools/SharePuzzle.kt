
package app.crossword.yourealwaysbe.forkyz.exttools

import android.net.Uri
import androidx.core.app.ShareCompat

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerShared
import app.crossword.yourealwaysbe.puz.Puzzle

class SharePuzzleData(
    val mimeType : String,
    val uri : Uri,
) : ExternalToolData() {
    companion object {
        suspend fun build(
            utils : NativeBackendUtils,
            puz : Puzzle,
            writeBlank : Boolean,
            omitExtensions : Boolean,
            cb : (SharePuzzleData) -> Unit,
        ) {
            FileHandlerShared.getShareUri(
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

    override fun accept(launcher : ExternalToolLauncher) {
        launcher.visit(this)
    }
}

fun ExternalToolLauncher.visit(data : SharePuzzleData) {
    val title = this.activity.getString(R.string.share_puzzle_title)
    val shareIntent = ShareCompat.IntentBuilder(this.activity)
        .setChooserTitle(title)
        .setStream(data.uri)
        .setType(data.mimeType)
        .createChooserIntent();
    this.activity.startActivity(shareIntent);
}

