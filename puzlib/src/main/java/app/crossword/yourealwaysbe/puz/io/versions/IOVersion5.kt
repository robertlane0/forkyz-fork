
package app.crossword.yourealwaysbe.puz.io.versions

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.LinkedList
import java.util.logging.Logger

import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.ListNum
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleMeta
import app.crossword.yourealwaysbe.puz.io.IO
import app.crossword.yourealwaysbe.puz.util.PuzzleUtils.getAcrossListName
import app.crossword.yourealwaysbe.puz.util.PuzzleUtils.getDownListName

// Adds history list in format
// int length (int clue boolean across)*
open class IOVersion5 : IOVersion4() {
    override fun applyMeta(puz : Puzzle, meta : PuzzleMeta) {
        super.applyMeta(puz, meta)

        val cidHistory = mutableListOf<ClueID>()
        for (ln in meta.historyList) {
            val listName = ln.listName
            val number = ln.number.toString()
            val clue = puz.getClues(listName)?.getClueByNumber(number)
            if (clue != null)
                cidHistory.add(clue.clueID)
        }
        puz.history = cidHistory
    }

    @Throws(IOException::class)
    override fun readMeta(inStream : DataInputStream) : PuzzleMeta {
        val meta = super.readMeta(inStream)

        meta.historyList = mutableListOf<ListNum>()

        val length = inStream.readInt()
        for (i in 0..<length) {
            val number = inStream.readInt()
            val across = inStream.readBoolean()
            val listName = if (across) IO.ACROSS_LIST else IO.DOWN_LIST

            meta.historyList.add(ListNum(listName, number))
        }

        return meta
    }

    @Throws(IOException::class)
    override fun writeMeta(puz : Puzzle, os : DataOutputStream) {
        super.writeMeta(puz, os)

        val history = puz.history
        val acrossListName = getAcrossListName(puz)
        val downListName = getDownListName(puz)

        os.writeInt(history.size)
        for (cid in puz.history) {
            val clue = puz.getClue(cid)
            // relies on puz files only have numeric numbers
            val number = clue?.clueNumber?.toInt()
            if (number != null) {
                val listName = cid.listName
                if (acrossListName == listName) {
                    os.writeInt(number)
                    os.writeBoolean(true)
                } else if (downListName == listName) {
                    os.writeInt(number)
                    os.writeBoolean(false)
                } else {
                    // ignore
                }
            }
        }
    }

    companion object {
        private val LOG
            = Logger.getLogger(IOVersion5::class.java.getCanonicalName())
    }
}

