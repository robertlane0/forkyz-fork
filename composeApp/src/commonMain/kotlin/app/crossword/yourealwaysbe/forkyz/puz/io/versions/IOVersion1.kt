
package app.crossword.yourealwaysbe.forkyz.puz.io.versions

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleMeta
import app.crossword.yourealwaysbe.forkyz.puz.io.IO.Companion.readNullTerminatedString
import app.crossword.yourealwaysbe.forkyz.puz.io.IO.Companion.writeNullTerminatedString
import app.crossword.yourealwaysbe.forkyz.puz.io.charset.CP1252Decoder
import app.crossword.yourealwaysbe.forkyz.puz.io.charset.CP1252Encoder
import app.crossword.yourealwaysbe.forkyz.puz.io.charset.CharsetDecoder
import app.crossword.yourealwaysbe.forkyz.puz.io.charset.CharsetEncoder

open class IOVersion1 : IOVersion {
    override val charsetDecoder : CharsetDecoder = CP1252Decoder()
    override val charsetEncoder : CharsetEncoder = CP1252Encoder()

    @Throws(IOException::class, CancellationException::class)
    override suspend fun read(puz : Puzzle, input : Source) {
        val meta = readMeta(input)
        applyMeta(puz, meta)
        val boxes : Array<Array<Box?>> = puz.boxes
        for (row in boxes) {
            for (b in row) {
                if (b == null || isBlock(b)) {
                    continue
                }
                b.isCheated = input.readByte().toInt() != 0
                b.responder = readNullTerminatedString(input, charsetDecoder)
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

    @Throws(IOException::class, CancellationException::class)
    override suspend fun readMeta(input : Source) : PuzzleMeta {
        val meta = PuzzleMeta()

        meta.author = readNullTerminatedString(input, charsetDecoder)
        meta.source = readNullTerminatedString(input, charsetDecoder)
        meta.title = readNullTerminatedString(input, charsetDecoder)

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
                writeNullTerminatedString(output, b.responder, charsetEncoder)
            }
        }
        output.writeLong(puz.time)
    }

    @Throws(IOException::class)
    protected open fun writeMeta(puz : Puzzle, output : Sink) {
        writeNullTerminatedString(output, puz.author, charsetEncoder)
        writeNullTerminatedString(output, puz.source, charsetEncoder)
        writeNullTerminatedString(output, puz.title, charsetEncoder)
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

