
package app.crossword.yourealwaysbe.forkyz.inttools

import android.os.Bundle
import android.content.Intent

import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.forkyz.NotesActivity

class ClueNotesData(
    val cid : ClueID? = null,
) : InternalToolData() {
    companion object {
        fun buildClueNotes(cid : ClueID?) : ClueNotesData = ClueNotesData(cid)
        fun buildPuzzleNotes() : ClueNotesData = ClueNotesData()
    }

    override fun accept(launcher : InternalToolLauncher) {
        launcher.visit(this)
    }
}

fun InternalToolLauncher.visit(data : ClueNotesData) {
    if (data.cid != null) {
        val i = Intent(activity, NotesActivity::class.java).apply {
            putExtra(NotesActivity.CLUE_NOTE_LISTNAME, data.cid.listName)
            putExtra(NotesActivity.CLUE_NOTE_INDEX, data.cid.index)
        }
        activity.startActivity(i)
    } else {
        activity.startActivity(Intent(activity, NotesActivity::class.java))
    }
}
