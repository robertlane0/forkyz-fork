
package app.crossword.yourealwaysbe.forkyz.puz.io.versions

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleMeta
import app.crossword.yourealwaysbe.forkyz.puz.util.PuzzleUtils.getAcrossListName
import app.crossword.yourealwaysbe.forkyz.puz.util.PuzzleUtils.getDownListName

// Saves the current board position and clue orientation.
open class IOVersion3 : IOVersion2() {
    override fun applyMeta(puz : Puzzle, meta : PuzzleMeta) {
        super.applyMeta(puz, meta)
        meta.position?.let { puz.position = it }
        val box = puz.checkedGetBox(meta.position)
        if (box != null && !isBlock(box)) {
            val desiredList = if (meta.across)
                getAcrossListName(puz)
            else
                getDownListName(puz)
            val curCid = box.getIsPartOfClue(desiredList)
            puz.currentClueID = curCid
        }
    }

    @Throws(IOException::class)
    override fun readMeta(input : Source) : PuzzleMeta {
        val meta = super.readMeta(input)
        val x = input.readInt()
        val y = input.readInt()
        meta.position = Position(y, x)
        meta.across = input.readByte().toInt() == 1
        return meta
    }

    @Throws(IOException::class)
    override fun writeMeta(puz : Puzzle, output : Sink) {
        super.writeMeta(puz, output)
        val p = puz.position
        output.writeInt(p.col)
        output.writeInt(p.row)
        val curCid = puz.currentClueID
        val acrossList = getAcrossListName(puz)
        if (curCid == null || acrossList == curCid.listName)
            output.writeByte(1)
        else output.writeByte(-1)
    }
}

