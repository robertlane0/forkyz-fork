
package app.crossword.yourealwaysbe.forkyz.inttools

import android.os.Bundle

import app.crossword.yourealwaysbe.forkyz.view.ClueEditDialog
import app.crossword.yourealwaysbe.puz.ClueID

class EditClueData(
    val cid : ClueID,
) : InternalToolData() {
    companion object {
        fun build(cid : ClueID) : EditClueData = EditClueData(cid)
    }

    override fun accept(launcher : InternalToolLauncher) {
        launcher.visit(this)
    }
}

fun InternalToolLauncher.visit(data : EditClueData) {
    val dialog = ClueEditDialog()
    val args = Bundle()
    args.putString(ClueEditDialog.CLUE_LISTNAME, data.cid.getListName())
    args.putInt(ClueEditDialog.CLUE_INDEX, data.cid.getIndex())
    dialog.setArguments(args)
    dialog.show(
        this.activity.getSupportFragmentManager(),
        "ClueEditDialog"
    )
}
