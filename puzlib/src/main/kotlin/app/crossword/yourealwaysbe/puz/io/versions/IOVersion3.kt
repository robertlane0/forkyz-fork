
package app.crossword.yourealwaysbe.puz.io.versions

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.logging.Logger

import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleMeta
import app.crossword.yourealwaysbe.puz.util.PuzzleUtils.getAcrossListName
import app.crossword.yourealwaysbe.puz.util.PuzzleUtils.getDownListName

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
    override fun readMeta(inStream : DataInputStream) : PuzzleMeta {
        val meta = super.readMeta(inStream)
        val x = inStream.readInt()
        val y = inStream.readInt()
        meta.position = Position(y, x)
        meta.across = inStream.read() == 1
        return meta
    }

    @Throws(IOException::class)
    override fun writeMeta(puz : Puzzle, os : DataOutputStream) {
        super.writeMeta(puz, os)
        val p = puz.position
        os.writeInt(p.col)
        os.writeInt(p.row)
        val curCid = puz.currentClueID
        val acrossList = getAcrossListName(puz)
        if (curCid == null || acrossList == curCid.listName)
            os.write(1)
        else os.write(-1)
    }

    companion object {
        private val LOG
            = Logger.getLogger(IOVersion3::class.java.getCanonicalName())
    }
}

