
package app.crossword.yourealwaysbe.forkyz.tools

import android.net.Uri
import android.content.Context
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerShared
import app.crossword.yourealwaysbe.forkyz.R
import androidx.core.app.ShareCompat

class SharePuzzleData(
    val mimeType : String,
    val uri : Uri,
) : ExternalToolData() {
    companion object {
        fun build(
            context : Context,
            puz : Puzzle,
            writeBlank : Boolean,
            omitExtensions : Boolean,
            cb : (SharePuzzleData) -> Unit,
        ) {
            FileHandlerShared.getShareUri(
                context,
                puz,
                writeBlank,
                omitExtensions
            ) { puzUri ->
                val mimeType = FileHandlerShared.getShareUriMimeType()
                cb(SharePuzzleData(mimeType, puzUri))
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

