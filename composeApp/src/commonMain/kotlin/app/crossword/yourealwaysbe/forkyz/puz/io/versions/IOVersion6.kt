
package app.crossword.yourealwaysbe.forkyz.puz.io.versions

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Note
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleMeta
import app.crossword.yourealwaysbe.forkyz.puz.io.IO.Companion.readNullTerminatedString
import app.crossword.yourealwaysbe.forkyz.puz.io.IO.Companion.writeNullTerminatedString
import app.crossword.yourealwaysbe.forkyz.puz.util.PuzzleUtils.getAcrossListName
import app.crossword.yourealwaysbe.forkyz.puz.util.PuzzleUtils.getDownListName
import app.crossword.yourealwaysbe.forkyz.util.AppLogger

// Moves clue notes out of the puz file and into meta
// i don't really think they were allowed in the puz file format
open class IOVersion6 : IOVersion5() {
    override fun applyMeta(puz : Puzzle, meta : PuzzleMeta) {
        super.applyMeta(puz, meta)
        applyNotes(puz, meta.acrossNotes, true)
        applyNotes(puz, meta.downNotes, false)
    }

    @Throws(IOException::class, CancellationException::class)
    override suspend fun readMeta(input : Source) : PuzzleMeta {
        val meta = super.readMeta(input)
        loadNotes(true, meta, input)
        loadNotes(false, meta, input)
        return meta
    }

    @Throws(IOException::class)
    override fun writeMeta(puz : Puzzle, output : Sink) {
        super.writeMeta(puz, output)
        saveNotes(output, puz, true)
        saveNotes(output, puz, false)
    }

    /**
     * First an int, is num notes
     * Format of notes (all null-term strings):
     * scratch, free-form, anagram src, anagram sol
     */
    @Throws(IOException::class)
    fun loadNotes(
        isAcross : Boolean,
        meta : PuzzleMeta,
        input : Source,
    ) {
        val numNotes = input.readInt()

        val notes = mutableListOf<Note?>()

        for (x in 0..<numNotes) {
            notes.add(readNote(input))
        }

        if (isAcross)
            meta.acrossNotes = notes
        else
            meta.downNotes = notes
    }

    @Throws(IOException::class)
    protected fun readNote(input : Source) : Note? {
        val scratch = readNullTerminatedString(input, charsetDecoder)
        val text = readNullTerminatedString(input, charsetDecoder)
        val anagramSrc = readNullTerminatedString(input, charsetDecoder)
        val anagramSol = readNullTerminatedString(input, charsetDecoder)
        if (
            scratch != null
            || text != null
            || anagramSrc != null
            || anagramSol != null
        ) {
            return Note(scratch, text, anagramSrc, anagramSol)
        }
        return null
    }

    @Throws(IOException::class)
    private fun saveNotes(
        output : Sink,
        puz : Puzzle,
        isAcross : Boolean,
    ) {
        val desiredList = if (isAcross)
            getAcrossListName(puz)
        else
            getDownListName(puz)

        var size = 0
        for (cid in puz.boardClueIDs) {
            if (desiredList == cid.listName) size += 1
        }

        // not really useful since notes should match the number of clue
        // positions
        output.writeInt(size)

        for (cid in puz.boardClueIDs) {
            if (desiredList == cid.listName) {
                val note = puz.getNote(cid)
                writeNote(note, output)
            }
        }
    }

    @Throws(IOException::class)
    protected fun writeNote(note : Note?, output : Sink) {
        var scratch : String? = null
        var text : String? = null
        var anagramSrc : String? = null
        var anagramSol : String? = null

        if (note != null) {
            scratch = note.compressedScratch
            text = note.text
            anagramSrc = note.compressedAnagramSource
            anagramSol = note.compressedAnagramSolution
        }

        writeNullTerminatedString(output, scratch, charsetEncoder)
        writeNullTerminatedString(output, text, charsetEncoder)
        writeNullTerminatedString(output, anagramSrc, charsetEncoder)
        writeNullTerminatedString(output, anagramSol, charsetEncoder)
    }

    private fun applyNotes(
        puz : Puzzle,
        notes : List<Note?>,
        isAcross : Boolean,
    ) {
        val desiredList = if (isAcross)
            getAcrossListName(puz)
        else
            getDownListName(puz)

        var idx = 0
        for (cid in puz.boardClueIDs) {
            if (desiredList == cid.listName) {
                if (idx < notes.size) {
                    val n = notes.get(idx)
                    if (n != null)
                        puz.setNote(cid, n)
                    idx += 1
                } else {
                    AppLogger.i(
                        TAG,
                        "WARNING: mismatch between number of "
                            + "clues and number of notes.",
                    )
                }
            }
        }
    }

    companion object {
        private val TAG = "ForkyzIOVersion6"
    }
}

