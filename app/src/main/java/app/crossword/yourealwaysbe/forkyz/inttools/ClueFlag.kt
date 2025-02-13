
package app.crossword.yourealwaysbe.forkyz.inttools

import android.os.Bundle

import app.crossword.yourealwaysbe.forkyz.view.ChooseFlagColorDialog
import app.crossword.yourealwaysbe.puz.ClueID

class ClueFlagData(
    val cid : ClueID,
) : InternalToolData() {
    companion object {
        fun build(cid : ClueID) : ClueFlagData = ClueFlagData(cid)
    }

    override fun accept(launcher : InternalToolLauncher) {
        launcher.visit(this)
    }
}

fun InternalToolLauncher.visit(data : ClueFlagData) {
    val dialog = ChooseFlagColorDialog()
    val args = Bundle()
    ChooseFlagColorDialog.addClueIDToBundle(args, data.cid)
    dialog.setArguments(args)
    dialog.show(
        this.activity.getSupportFragmentManager(),
        "ChooseFlagColorDialog"
    )
}
