
package app.crossword.yourealwaysbe.puz.io.versions

import java.nio.charset.Charset
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleMeta
import app.crossword.yourealwaysbe.puz.io.IO.Companion.readNullTerminatedString
import app.crossword.yourealwaysbe.puz.io.IO.Companion.writeNullTerminatedString

open class IOVersion1 : IOVersion {
    override val charset = Charset.forName("Cp1252")

    @Throws(IOException::class)
    override fun read(puz : Puzzle, input : Source) {
        val meta = readMeta(input)
        applyMeta(puz, meta)
        val boxes : Array<Array<Box?>> = puz.boxes
        for (row in boxes) {
            for (b in row) {
                if (b == null || isBlock(b)) {
                    continue
                }
                b.isCheated = input.readByte().toInt() != 0
                b.responder = readNullTerminatedString(input, charset)
            }
        }
        try {
            puz.time = input.readLong()
        } catch (ioe : IOException) {
            ioe.printStackTrace()
        }
    }

    protected open fun applyMeta(puz : Puzzle, meta : PuzzleMeta) {
        puz.source = meta.source
        puz.date = meta.date
    }

    @Throws(IOException::class)
    override fun readMeta(input : Source) : PuzzleMeta {
        val meta = PuzzleMeta()

        meta.author = readNullTerminatedString(input, charset)
        meta.source = readNullTerminatedString(input, charset)
        meta.title = readNullTerminatedString(input, charset)

        val dateEpoch = input.readLong()
        meta.date = if (dateEpoch == 0L)
            null
        else
            Instant.fromEpochMilliseconds(dateEpoch)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date

        meta.percentComplete = input.readInt()
        meta.percentFilled = meta.percentComplete

        return meta
    }

    @Throws(IOException::class)
    override fun write(puz : Puzzle, output : Sink) {
        writeMeta(puz, output)
        val boxes : Array<Array<Box?>> = puz.boxes
        for (row in boxes) {
            for (b in row) {
                if (b == null || isBlock(b)) {
                    continue
                }
                output.writeByte(if (b.isCheated) 1 else 0)
                writeNullTerminatedString(output, b.responder, charset)
            }
        }
        output.writeLong(puz.time)
    }

    @Throws(IOException::class)
    protected open fun writeMeta(puz : Puzzle, output : Sink) {
        writeNullTerminatedString(output, puz.author, charset)
        writeNullTerminatedString(output, puz.source, charset)
        writeNullTerminatedString(output, puz.title, charset)
        val date = puz.date
        if (date == null) {
            output.writeLong(0)
        } else {
            output.writeLong(
                date.atStartOfDayIn(TimeZone.currentSystemDefault())
                    .toEpochMilliseconds(),
            )
        }
        output.writeInt(puz.percentComplete)
    }
}

