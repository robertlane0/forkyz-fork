
package app.crossword.yourealwaysbe.forkyz.inttools

import android.os.Bundle

import app.crossword.yourealwaysbe.forkyz.view.ChooseFlagColorDialog
import app.crossword.yourealwaysbe.puz.Position

class CellFlagData(
    val position : Position,
) : InternalToolData() {
    companion object {
        fun build(position : Position) : CellFlagData = CellFlagData(position)
    }

    override fun accept(launcher : InternalToolLauncher) {
        launcher.visit(this)
    }
}

fun InternalToolLauncher.visit(data : CellFlagData) {
    val dialog = ChooseFlagColorDialog()
    val args = Bundle()
    ChooseFlagColorDialog.addBoxPositionToBundle(args, data.position)
    dialog.setArguments(args)
    dialog.show(
        this.activity.getSupportFragmentManager(),
        "ChooseFlagColorDialog"
    )
}
