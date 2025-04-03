
package app.crossword.yourealwaysbe.forkyz.inttools

import android.os.Bundle
import android.content.Intent

import app.crossword.yourealwaysbe.forkyz.ClueListActivity

// trivial inttool, useful for putting clue list launch into the
// framework
class ClueListData() : InternalToolData() {
    companion object {
        fun build() : ClueListData = ClueListData()
    }

    override fun accept(launcher : InternalToolLauncher) {
        launcher.visit(this)
    }
}

fun InternalToolLauncher.visit(data : ClueListData) {
    activity.startActivity(Intent(activity, ClueListActivity::class.java))
}
