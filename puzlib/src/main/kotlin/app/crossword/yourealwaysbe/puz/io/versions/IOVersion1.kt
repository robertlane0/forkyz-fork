
package app.crossword.yourealwaysbe.puz.io.versions

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.time.Instant
import java.time.ZoneId

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleMeta
import app.crossword.yourealwaysbe.puz.io.IO.Companion.readNullTerminatedString
import app.crossword.yourealwaysbe.puz.io.IO.Companion.writeNullTerminatedString

open class IOVersion1 : IOVersion {
    override val charset = Charset.forName("Cp1252")

    @Throws(IOException::class)
    override fun read(puz : Puzzle, inStream : DataInputStream) {
        val meta = readMeta(inStream)
        applyMeta(puz, meta)
        val boxes : Array<Array<Box?>> = puz.boxes
        for (row in boxes) {
            for (b in row) {
                if (b == null || isBlock(b)) {
                    continue
                }
                b.isCheated = inStream.readBoolean()
                b.responder = readNullTerminatedString(inStream, charset)
            }
        }
        try {
            puz.time = inStream.readLong()
        } catch (ioe : IOException) {
            ioe.printStackTrace()
        }
    }

    protected open fun applyMeta(puz : Puzzle, meta : PuzzleMeta) {
        puz.source = meta.source
        puz.date = meta.date
    }

    @Throws(IOException::class)
    override fun readMeta(inStream : DataInputStream) : PuzzleMeta {
        val meta = PuzzleMeta()

        meta.author = readNullTerminatedString(inStream, charset)
        meta.source = readNullTerminatedString(inStream, charset)
        meta.title = readNullTerminatedString(inStream, charset)

        val dateEpoch = inStream.readLong()
        meta.date = if (dateEpoch == 0L)
            null
        else
            Instant.ofEpochMilli(dateEpoch)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()


        meta.percentComplete = inStream.readInt()
        meta.percentFilled = meta.percentComplete

        return meta
    }

    @Throws(IOException::class)
    override fun write(puz : Puzzle, os : DataOutputStream) {
        writeMeta(puz, os)
        val boxes : Array<Array<Box?>> = puz.boxes
        for (row in boxes) {
            for (b in row) {
                if (b == null || isBlock(b)) {
                    continue
                }
                os.writeBoolean(b.isCheated)
                writeNullTerminatedString(os, b.responder, charset)
            }
        }
        os.writeLong(puz.time)
    }

    @Throws(IOException::class)
    protected open fun writeMeta(puz : Puzzle, os : DataOutputStream) {
        writeNullTerminatedString(os, puz.author, charset)
        writeNullTerminatedString(os, puz.source, charset)
        writeNullTerminatedString(os, puz.title, charset)
        val date = puz.date
        if (date == null) {
            os.writeLong(0)
        } else {
            os.writeLong(
                date.atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            )
        }
        os.writeInt(puz.percentComplete)
    }
}

