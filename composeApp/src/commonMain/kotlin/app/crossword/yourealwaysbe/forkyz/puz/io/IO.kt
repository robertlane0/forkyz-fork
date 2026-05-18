
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlin.Array
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Char
import kotlin.Int
import kotlin.IntArray
import kotlin.String
import kotlin.Throws
import kotlin.arrayOfNulls
import kotlin.code
import kotlin.coroutines.cancellation.CancellationException
import kotlin.text.String
import kotlin.text.StringBuilder
import kotlin.text.isEmpty
import kotlin.text.replace
import kotlin.text.split
import kotlin.text.toByte
import kotlin.text.toRegex
import kotlin.text.trim
import kotlin.toUShort
import kotlinx.io.Buffer
import kotlinx.io.EOFException
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.readShortLe
import kotlinx.io.writeShortLe

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.ClueList
import app.crossword.yourealwaysbe.forkyz.puz.Note
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleMeta
import app.crossword.yourealwaysbe.forkyz.puz.io.charset.CP1252Decoder
import app.crossword.yourealwaysbe.forkyz.puz.io.charset.CP1252Encoder
import app.crossword.yourealwaysbe.forkyz.puz.io.charset.CharsetDecoder
import app.crossword.yourealwaysbe.forkyz.puz.io.charset.CharsetEncoder
import app.crossword.yourealwaysbe.forkyz.puz.io.charset.UTF8Decoder
import app.crossword.yourealwaysbe.forkyz.puz.io.charset.UTF8Encoder
import app.crossword.yourealwaysbe.forkyz.puz.io.versions.IOVersion
import app.crossword.yourealwaysbe.forkyz.puz.io.versions.IOVersion1
import app.crossword.yourealwaysbe.forkyz.puz.io.versions.IOVersion2
import app.crossword.yourealwaysbe.forkyz.puz.io.versions.IOVersion3
import app.crossword.yourealwaysbe.forkyz.puz.io.versions.IOVersion4
import app.crossword.yourealwaysbe.forkyz.puz.io.versions.IOVersion5
import app.crossword.yourealwaysbe.forkyz.puz.io.versions.IOVersion6
import app.crossword.yourealwaysbe.forkyz.puz.io.versions.IOVersion7
import app.crossword.yourealwaysbe.forkyz.puz.io.versions.IOVersion8
import app.crossword.yourealwaysbe.forkyz.puz.io.versions.IOVersion9
import app.crossword.yourealwaysbe.forkyz.puz.util.PuzzleUtils.getAcrossList
import app.crossword.yourealwaysbe.forkyz.puz.util.PuzzleUtils.getAcrossListName
import app.crossword.yourealwaysbe.forkyz.puz.util.PuzzleUtils.getDownList
import app.crossword.yourealwaysbe.forkyz.puz.util.PuzzleUtils.getDownListName
import app.crossword.yourealwaysbe.forkyz.util.HtmlUtil.htmlString
import app.crossword.yourealwaysbe.forkyz.util.HtmlUtil.unHtmlString

class IO : PuzzleParser {
    @Throws(Exception::class)
    override suspend fun parseInput(input : Source) : Puzzle? {
        return loadNative(input)
    }

    companion object {
        const val FILE_MAGIC : String = "ACROSS&DOWN"
        const val VERSION_STRING : String = "2.0"
        private val CHARSET_DECODER = UTF8Decoder()
        private val CHARSET_ENCODER = UTF8Encoder()
        private val CHARSET_DECODER_OLD = CP1252Decoder()
        private val CHARSET_ENCODER_OLD = CP1252Encoder()

        // Extra Section IDs and markers
        private const val GEXT_MARKER = "GEXT"
        private const val GEXT = 0
        private const val REBUS_TABLE_MARKER = "RTBL"
        private const val RTBL = 3
        private const val REBUS_BOARD_MARKER = "GRBS"
        private const val GRBS = 4
        private const val REBUS_USER_MARKER = "RUSR"
        private const val RUSR = 5

        // LEGACY: used only for reading IOVersion5 and below that dubiously
        // stored additional clue notes in the Across Lite file
        private const val ANTS_MARKER = "ANTS"
        private const val DNTS_MARKER = "DNTS"
        private const val ANTS = 1
        private const val DNTS = 2

        // GEXT section bitmasks
        private val GEXT_SQUARE_CIRCLED = 0x80.toByte()

        // NOTES CODES
        private val NOTE_SCRATCH = 0x01.toByte()
        private val NOTE_TEXT = 0x02.toByte()
        private val NOTE_ANAGRAM_SRC = 0x03.toByte()
        private val NOTE_ANAGRAM_SOL = 0x04.toByte()

        // string to use if a clue is missing
        private const val UNKNOWN_CLUE = "-"

        // X is the official char for a blank solution, i've seen D used
        // e.g. by Private Eye
        private const val EMPTY_SOL_CHAR = 'X'
        private val EMPTY_SOLS = setOf(EMPTY_SOL_CHAR.toString(), "D")

        private const val BLANK = '-'

        suspend fun acrossListName() : String {
            return DefaultListNames.acrossList()
        }

        suspend fun downListName() : String {
            return DefaultListNames.downList()
        }

        fun cksum_region(
            data : ByteArray,
            offset : Int,
            length : Int,
            cksum : Int,
        ) : Int {
            var cksum = cksum
            for (i in offset..<(offset + length)) {
                if ((cksum and 0x1) != 0) {
                    cksum = (cksum shr 1) + 0x8000
                } else {
                    cksum = cksum shr 1
                }

                cksum += (0xFF and data[i].toInt())
                cksum = cksum and 0xFFFF
            }

            return cksum
        }

        @Throws(IOException::class, CancellationException::class)
        suspend fun load(input : Source, metaInput : Source) : Puzzle? {
            val puz = loadNative(input)
            if (puz != null)
                readCustom(puz, metaInput)
            return puz
        }

        @Throws(IOException::class, CancellationException::class)
        suspend fun loadNative(input : Source) : Puzzle? {
            input.skip(0x2)

            val fileMagic = Buffer()
            input.readAtMostTo(fileMagic, FILE_MAGIC.length.toLong())

            val fileMagicStr = CHARSET_DECODER.decode(fileMagic)

            // check that this is a puz file
            if (FILE_MAGIC != fileMagicStr)
                return null

            // done in two steps to match saveNative method
            input.skip(1)
            input.skip(0xA)

            val versionBytes = Buffer()
            input.readAtMostTo(versionBytes, 3)
            val version = CHARSET_DECODER.decode(versionBytes)

            val charsetDecoder = if ("2.0".compareTo(version) > 0)
                CHARSET_DECODER_OLD
            else
                CHARSET_DECODER

            input.skip(1)

            input.skip(2)
            val solutionChecksum = input.readShortLe()

            input.skip(0xC)

            val width = 0xFFFF and input.readByte().toInt()
            val height = 0xFFFF and input.readByte().toInt()

            // read/skip number of clues
            input.readShortLe()

            input.skip(2)
            val scrambled = input.readShortLe().toInt() != 0

            val boxes = Array<Array<Box?>>(height) { arrayOfNulls<Box>(width) }

            val answerByte = Buffer()

            for (x in boxes.indices) {
                for (y in boxes[x].indices) {
                    answerByte.writeByte(input.readByte())

                    val solution = charsetDecoder.decode(answerByte).get(0)

                    if (solution != '.') {
                        boxes[x][y] = Box()
                        if (solution.code != 0 && solution != BLANK) {
                            boxes[x][y]?.setSolution(solution)
                        }
                    }
                }
            }

            for (x in boxes.indices) {
                for (y in boxes[x].indices) {
                    answerByte.writeByte(input.readByte())

                    val answer = charsetDecoder.decode(answerByte).get(0)

                    if (answer == '.') {
                        continue
                    } else if (answer == BLANK) {
                        boxes[x][y]?.setBlank()
                    } else if (!isBlock(boxes[x][y])) {
                        boxes[x][y]?.setResponse(answer)
                    } else {
                        println(
                            "Unexpected answer: "
                                + x
                                + ","
                                + y
                                + " " + answer,
                        )
                    }
                }
            }

            val builder = PuzzleBuilder(boxes)
            builder.autoNumberBoxes()

            builder.setSolutionChecksum(solutionChecksum)
            builder.setScrambled(scrambled)
            builder.setTitle(
                htmlString(readNullTerminatedString(input, charsetDecoder)),
            )
            builder.setAuthor(
                htmlString(readNullTerminatedString(input, charsetDecoder)),
            )
            builder.setCopyright(
                htmlString(readNullTerminatedString(input, charsetDecoder)),
            )

            for (x in 0..<builder.height) {
                for (y in 0..<builder.width) {
                    val box = builder.getBox(x, y)

                    if (box == null || isBlock(box)) {
                        continue
                    }

                    val clueNumber = box.clueNumber

                    if (
                        builder.isStartClue(x, y, true) && (clueNumber != null)
                    ) {
                        val value
                            = readNullTerminatedString(input, charsetDecoder)
                        htmlString(value)?.let { value ->
                            builder.addAcrossClue(
                                acrossListName(),
                                clueNumber,
                                value,
                            )
                        }
                    }

                    if (
                        builder.isStartClue(x, y, false) && (clueNumber != null)
                    ) {
                        val value
                            = readNullTerminatedString(input, charsetDecoder)
                        htmlString(value)?.let { value ->
                            builder.addDownClue(
                                downListName(),
                                clueNumber,
                                value,
                            )
                        }
                    }
                }
            }

            builder.setNotes(
                htmlString(readNullTerminatedString(input, charsetDecoder)),
            )

            var eof = false

            var rebusBoard : Array<ByteArray>? = null
            var solRebusTable : Array<String?>? = null
            var usrRebusTable : Array<String?>? = null

            while (!eof) {
                try {
                    when (readExtraSectionType(input, charsetDecoder)) {
                        GEXT -> readGextSection(input, builder)
                        ANTS -> loadNotesNative(
                            true,
                            builder,
                            input,
                            charsetDecoder,
                        )
                        DNTS -> loadNotesNative(
                            false,
                            builder,
                            input,
                            charsetDecoder,
                        )
                        GRBS -> rebusBoard = readRebusBoard(input, builder)
                        RTBL -> solRebusTable = readRebusTable(
                            input,
                            builder,
                            charsetDecoder,
                        )
                        RUSR -> usrRebusTable = readRebusTable(
                            input,
                            builder,
                            charsetDecoder,
                        )
                        else -> skipExtraSection(input)
                    }
                } catch (e : EOFException) {
                    eof = true
                }
            }

            rebusBoard?.let {
                applyRebus(it, solRebusTable, usrRebusTable, builder)
            }

            removeSolutionIfBlank(builder)

            return builder.puzzle
        }

        @Throws(IOException::class, CancellationException::class)
        suspend fun readCustom(puz : Puzzle, input : Source) {
            val version = input.readByte().toInt()
            val v = getIOVersion(version)
            v.read(puz, input)
        }

        @Throws(IOException::class)
        fun readExtraSectionType(
            input : Source,
            charsetDecoder : CharsetDecoder,
        ) : Int {
            val title = Buffer()
            input.readAtMostTo(title, 4)
            val section = charsetDecoder.decode(title)

            if (GEXT_MARKER == section) {
                return GEXT
            } else if (ANTS_MARKER == section) {
                return ANTS
            } else if (DNTS_MARKER == section) {
                return DNTS
            } else if (REBUS_BOARD_MARKER == section) {
                return GRBS
            } else if (REBUS_TABLE_MARKER == section) {
                return RTBL
            } else if (REBUS_USER_MARKER == section) {
                return RUSR
            }

            return -1
        }

        @Throws(IOException::class)
        fun readGextSection(input : Source, builder : PuzzleBuilder) {
            input.skip(4)

            for (x in 0..<builder.height) {
                for (y in 0..<builder.width) {
                    val gextInfo = input.readByte()

                    if (
                        (gextInfo.toInt() and GEXT_SQUARE_CIRCLED.toInt())
                            != 0
                    ) {
                        val box = builder.getBox(x, y)
                        if (box != null && !isBlock(box))
                            box.shape = Box.Shape.CIRCLE
                    }
                }
            }

            input.skip(1)
        }

        @Throws(IOException::class, CancellationException::class)
        suspend fun readMeta(input : Source) : PuzzleMeta? {
            val version = input.readByte().toInt()
            val v = getIOVersion(version)
            val m = v.readMeta(input)
            return m
        }

        @Throws(IOException::class)
        fun readNullTerminatedString(
            input : Source,
            charsetDecoder : CharsetDecoder,
        ) : String? {
            val baos = Buffer()

            var nextByte = input.readByte()
            while (nextByte.toInt() != 0x0) {
                if (nextByte.toInt() != 0x0) {
                    baos.writeByte(nextByte)
                }

                if (baos.size > 4096) {
                    throw IOException("Run on string!")
                }
                nextByte = input.readByte()
            }

            return if (baos.size == 0L)
                null
            else
                charsetDecoder.decode(baos)
        }

        @Throws(IOException::class)
        fun save(
            puz : Puzzle,
            puzzleOutput : Sink,
            metaOutput : Sink,
        ) {
            saveNative(puz, puzzleOutput)
            writeCustom(puz, metaOutput)
        }

        @Throws(IOException::class)
        fun saveNative(puz : Puzzle, output : Sink) {
            // We write the puzzle to a temporary output stream, with 0
            // entered for any checksums. Once we have this written out,
            // we can calculate all of the checksums and write the file
            // to the original output stream.
            val tmp = Buffer()

            tmp.writeShortLe(0)

            CHARSET_ENCODER.encode(FILE_MAGIC, tmp)
            tmp.writeByte(0)

            tmp.write(ByteArray(0xA))

            CHARSET_ENCODER.encode(VERSION_STRING, tmp)
            tmp.writeByte(0)

            tmp.write(ByteArray(2))

            tmp.writeShortLe(puz.solutionChecksum)

            tmp.write(ByteArray(0xC))

            val width = puz.width
            val height = puz.height
            val numberOfBoxes = width * height

            tmp.writeByte(width.toByte())
            tmp.writeByte(height.toByte())

            val numberOfClues : Int = getNumberOfClues(puz)

            tmp.writeShortLe(numberOfClues.toShort())

            tmp.writeShortLe(1)

            val scrambled = if (puz.isScrambled) 4.toShort() else 0.toShort()
            tmp.writeShortLe(scrambled)

            val boxes : Array<Array<Box?>> = puz.boxes

            val hasSolution = puz.hasSolution()
            for (x in boxes.indices) {
                for (y in boxes[x].indices) {
                    val box = boxes[x][y]
                    if (box == null || isBlock(box)) {
                        CHARSET_ENCODER.encode('.', tmp)
                    } else if (hasSolution) {
                        val sSol = box.solution
                        val value = if (sSol == null || sSol.isEmpty())
                            BLANK
                        else
                            sSol.get(0)
                        CHARSET_ENCODER.encode(value, tmp)
                    } else {
                        CHARSET_ENCODER.encode(EMPTY_SOL_CHAR, tmp)
                    }
                }
            }

            for (x in boxes.indices) {
                for (y in boxes[x].indices) {
                    val box = boxes[x][y]
                    if (box == null || isBlock(box)) {
                        CHARSET_ENCODER.encode('.', tmp)
                    } else {
                        val sRes = box.response
                        val value = if (sRes.isEmpty())
                            BLANK
                        else
                            sRes.get(0)

                        CHARSET_ENCODER.encode(
                            if (box.isBlank()) BLANK else value,
                            tmp,
                        )
                    }
                }
            }

            writeNullTerminatedString(
                tmp,
                unHtmlString(puz.title),
                CHARSET_ENCODER,
            )
            writeNullTerminatedString(
                tmp,
                unHtmlString(puz.author),
                CHARSET_ENCODER,
            )
            writeNullTerminatedString(
                tmp,
                unHtmlString(puz.copyright),
                CHARSET_ENCODER,
            )

            for (clue in getRawClues(puz)) {
                writeNullTerminatedString(
                    tmp,
                    unHtmlString(clue),
                    CHARSET_ENCODER,
                )
            }

            writeNullTerminatedString(
                tmp,
                unHtmlString(puz.notes),
                CHARSET_ENCODER,
            )

            writeGEXT(tmp, puz)
            writeRebus(tmp, puz, CHARSET_ENCODER)

            val bytes = tmp.readByteArray()

            // Calculate checksums and write to byte array.
            val c_cib : Int = cksum_cib(bytes, 0)
            bytes.putShortLe(0x0E, c_cib.toShort())

            val c_primary : Int = cksum_primary_board(
                bytes,
                numberOfBoxes,
                numberOfClues,
                c_cib,
            )
            bytes.putShortLe(0, c_primary.toShort())

            val c_sol : Int = cksum_solution(bytes, numberOfBoxes, 0)
            val c_grid : Int = cksum_grid(bytes, numberOfBoxes, 0)
            val c_part : Int = cksum_partial_board(
                bytes,
                numberOfBoxes,
                numberOfClues,
                0,
            )

            bytes[0x10] = (0x49 xor (c_cib and 0xFF)).toByte()
            bytes[0x11] = (0x43 xor (c_sol and 0xFF)).toByte()
            bytes[0x12] = (0x48 xor (c_grid and 0xFF)).toByte()
            bytes[0x13] = (0x45 xor (c_part and 0xFF)).toByte()
            bytes[0x14] = (0x41 xor ((c_cib and 0xFF00) shr 8)).toByte()
            bytes[0x15] = (0x54 xor ((c_sol and 0xFF00) shr 8)).toByte()
            bytes[0x16] = (0x45 xor ((c_grid and 0xFF00) shr 8)).toByte()
            bytes[0x17] = (0x44 xor ((c_part and 0xFF00) shr 8)).toByte()

            // Dump byte array to output stream.
            output.write(bytes)
        }

        @Throws(IOException::class)
        fun skipExtraSection(input : Source) {
            val numBytes = input.readShortLe()
            input.skip(2) // checksum
            input.skip(numBytes.toLong()) // data
            input.skip(1) // null terminator
        }

        /**
         * Attempts to unscramble the solution using the input key. Modifications to
         * the solution array occur in place. If true, the unscrambled solution
         * checksum is valid.
         */
        fun tryUnscramble(
            p : Puzzle,
            key_int : Int,
            solution : ByteArray,
        ) : Boolean {
            val unscrambleKey = IntArray(4)
            val unscrambleBuf = ByteArray(solution.size)
            val unscrambleTmp = ByteArray(9)

            unscrambleKey[0] = (key_int / 1000) % 10
            unscrambleKey[1] = (key_int / 100) % 10
            unscrambleKey[2] = (key_int / 10) % 10
            unscrambleKey[3] = (key_int / 1) % 10

            for (i in 3 downTo 0) {
                unscrambleString(unscrambleBuf, solution)
                arraycopy(
                    unscrambleBuf,
                    0,
                    solution,
                    0,
                    unscrambleBuf.size,
                )
                unshiftString(unscrambleTmp, solution, unscrambleKey[i])

                for (j in solution.indices) {
                    var letter = (
                        (solution[j].toInt() and 0xFF) - unscrambleKey[j % 4]
                    )

                    if (letter < 65) {
                        letter += 26
                    }

                    solution[j] = letter.toByte()
                }
            }

            if (
                p.solutionChecksum
                    == cksum_region(solution, 0, solution.size, 0).toShort()
            ) {
                var s = 0
                for (i in p.boxesList.indices) {
                    val b = p.boxesList[i]
                    if (b != null && !isBlock(b)) {
                        b.setSolution(Char(solution[s++].toUShort()))
                    }
                }
                return true
            }
            return false
        }

        @Throws(IOException::class)
        fun writeCustom(puz : Puzzle, output : Sink) {
            output.writeByte(9)
            val v = IOVersion9()
            v.write(puz, output)
        }

        fun crack(puz : Puzzle) : Boolean {
            for (a in 0..9999) {
                if (tryUnscramble(puz, a, initializeUnscrambleData(puz))) {
                    return true
                }
            }
            return false
        }

        @Throws(IOException::class)
        fun writeNullTerminatedString(
            output : Sink,
            value : String?,
            charsetEncoder : CharsetEncoder,
        ) {
            var value = value
            value = if (value == null) "" else value

            charsetEncoder.encode(value, output)
            output.writeByte(0)
        }

        fun unscrambleString(unscrambleBuf : ByteArray, str : ByteArray) {
            var oddIndex = 0
            var evenIndex = str.size / 2

            for (i in str.indices) {
                if ((i % 2) == 0) {
                    unscrambleBuf[evenIndex++] = str[i]
                } else {
                    unscrambleBuf[oddIndex++] = str[i]
                }
            }
        }

        fun unshiftString(
            unscrambleTmp : ByteArray,
            str : ByteArray,
            keynum : Int,
        ) {
            arraycopy(str, str.size - keynum, unscrambleTmp, 0, keynum)
            arraycopy(str, 0, str, keynum, str.size - keynum)
            arraycopy(unscrambleTmp, 0, str, 0, keynum)
        }

        fun initializeUnscrambleData(puz : Puzzle) : ByteArray {
            val ans = StringBuilder()
            for (x in 0..<puz.width) {
                for (y in 0..<puz.height) {
                    val box = puz.checkedGetBox(y, x)
                    if (box != null && !isBlock(box)) {
                        ans.append(box.solution)
                    }
                }
            }
            val buf = Buffer()
            CHARSET_ENCODER.encode(ans.toString(), buf)
            return buf.readByteArray()
        }

        private fun cksum_cib(puzByteArray : ByteArray, cksum : Int) : Int {
            return cksum_region(puzByteArray, 0x2C, 8, cksum)
        }

        private fun cksum_grid(
            puzByteArray : ByteArray,
            numberOfBoxes : Int,
            cksum : Int,
        ) : Int {
            return cksum_region(
                puzByteArray,
                0x34 + numberOfBoxes,
                numberOfBoxes,
                cksum,
            )
        }

        private fun cksum_partial_board(
            puzByteArray : ByteArray,
            numberOfBoxes : Int,
            numberOfClues : Int,
            cksum : Int,
        ) : Int {
            var cksum = cksum
            var offset = 0x34 + (2 * numberOfBoxes)

            for (i in 0..<(4 + numberOfClues)) {
                val startOffset = offset

                while (puzByteArray[offset].toInt() != 0) {
                    offset++
                }

                val length = offset - startOffset

                if ((i > 2) && (i < (3 + numberOfClues))) {
                    cksum = cksum_region(
                        puzByteArray,
                        startOffset,
                        length,
                        cksum,
                    )
                } else if (length > 0) {
                    cksum = cksum_region(
                        puzByteArray,
                        startOffset,
                        length + 1,
                        cksum,
                    )
                }

                offset++
            }

            return cksum
        }

        private fun cksum_primary_board(
            puzByteArray : ByteArray,
            numberOfBoxes : Int,
            numberOfClues : Int,
            cksum : Int,
        ) : Int {
            var cksum = cksum
            cksum = cksum_solution(puzByteArray, numberOfBoxes, cksum)
            cksum = cksum_grid(puzByteArray, numberOfBoxes, cksum)
            cksum = cksum_partial_board(
                puzByteArray, numberOfBoxes, numberOfClues,
                cksum
            )

            return cksum
        }

        private fun cksum_solution(
            puzByteArray : ByteArray,
            numberOfBoxes : Int,
            cksum : Int,
        ) : Int {
            return cksum_region(puzByteArray, 0x34, numberOfBoxes, cksum)
        }

        /**
         * For reading IOVersion5 and below that dubiously stored clue notes by
         * possibly abuse of the Across Lite format.
         *
         * Boxes must be set on puz first.
         *
         * Format of a note:
         * + 1st byte is number of fields in note
         * + Each field is
         * + byte identifying field
         * + null terminated string which is field value
         */
        @Throws(IOException::class, CancellationException::class)
        private suspend fun loadNotesNative(
            isAcross : Boolean,
            builder : PuzzleBuilder,
            input : Source,
            charsetDecoder : CharsetDecoder,
        ) {
            for (cid in builder.boardClueIDs) {
                val listName = cid.listName

                // assume puz files only deal with across/down list
                val desiredList = if (isAcross)
                    acrossListName()
                else
                    downListName()
                if (desiredList == listName) {
                    var scratch : String? = null
                    var text : String? = null
                    var anagramSrc : String? = null
                    var anagramSol : String? = null

                    val numFields = input.readByte()
                    for (i in 0..<numFields) {
                        val field = input.readByte()

                        val value
                            = readNullTerminatedString(input, charsetDecoder)

                        when (field) {
                            NOTE_SCRATCH -> scratch = value
                            NOTE_TEXT -> text = value
                            NOTE_ANAGRAM_SRC -> anagramSrc = value
                            NOTE_ANAGRAM_SOL -> anagramSol = value
                        }
                    }

                    if (
                        scratch != null
                        || text != null
                        || anagramSrc != null
                        || anagramSol != null
                    ) {
                        val n = Note(scratch, text, anagramSrc, anagramSol)
                        builder.setNote(cid, n)
                    }
                }
            }
        }

        @Throws(IOException::class)
        private fun getIOVersion(version : Int) : IOVersion {
            when (version) {
                1 -> return IOVersion1()
                2 -> return IOVersion2()
                3 -> return IOVersion3()
                4 -> return IOVersion4()
                5 -> return IOVersion5()
                6 -> return IOVersion6()
                7 -> return IOVersion7()
                8 -> return IOVersion8()
                9 -> return IOVersion9()
                else -> throw IOException("UnknownVersion " + version)
            }
        }

        /**
         * Return list of clues in puz format order
         *
         * That is, in numerical order, with across appearing before down
         * when same number
         */
        private fun getRawClues(puz : Puzzle) : List<String> {
            val numClues : Int = getNumberOfClues(puz)
            val rawClues = mutableListOf<String>()

            val acrossList = getAcrossListName(puz)
            val downList = getDownListName(puz)

            var acrossClues : ClueList? = null
            if (acrossList != null)
                acrossClues = puz.getClues(acrossList)

            var downClues : ClueList? = null
            if (downList != null)
                downClues = puz.getClues(downList)

            for (cid in puz.boardClueIDs) {
                val listName = cid.listName
                val index = cid.index

                // only support Across/Down in puz files
                if (acrossList == listName) {
                    val clue = acrossClues?.getClueByIndex(index)
                    if (clue != null)
                        rawClues.add(clue.hint)
                    else
                        rawClues.add(UNKNOWN_CLUE)
                } else if (downList == listName) {
                    val clue = downClues?.getClueByIndex(index)
                    if (clue != null)
                        rawClues.add(clue.hint)
                    else
                        rawClues.add(UNKNOWN_CLUE)
                }
            }

            return rawClues
        }

        /**
         * Gets number of Across/Down clues
         */
        private fun getNumberOfClues(puz : Puzzle) : Int {
            val acrossClues = getAcrossList(puz)
            val downClues = getDownList(puz)
            var count = 0

            if (acrossClues != null)
                count += acrossClues.size()

            if (downClues != null)
                count += downClues.size()

            return count
        }

        /**
         * Read rebus grid from extras
         *
         * Format is flattened grid (row by row) of bytes, with 0 if no
         * rebus at that cell position, and an index into the rebus tables
         * if there is a rebus. The index is +1 (e.g. index 0 is stored as
         * 1).
         *
         * Thanks for puz.py for the rebus encoding reference.
         * https://github.com/alexdej/puzpy
         */
        @Throws(IOException::class)
        private fun readRebusBoard(
            input : Source,
            builder : PuzzleBuilder,
        ) : Array<ByteArray> {
            val height = builder.height
            val width = builder.width
            val rebusBoard = Array<ByteArray>(height) { ByteArray(width) }

            input.skip(4)

            for (row in 0..<height)
                for (col in 0..<width)
                    rebusBoard[row][col] = input.readByte()

            input.skip(1)

            return rebusBoard
        }

        /**
         * Read a rebus table from extras
         *
         * Format is a string index:entry;index:entry;... mapping indices to
         * string entry values.
         *
         * Thanks for puz.py for the rebus encoding reference.
         * https://github.com/alexdej/puzpy
         */
        @Throws(IOException::class)
        private fun readRebusTable(
            input : Source,
            builder : PuzzleBuilder,
            charsetDecoder : CharsetDecoder,
        ) : Array<String?> {
            val height = builder.height
            val width = builder.width

            val table = arrayOfNulls<String>(width * height + 1)

            // 2 bytes len, 2 bytes checksum
            input.skip(4)

            readNullTerminatedString(input, charsetDecoder)
                ?.let { tableString ->
                    for (
                        entry in tableString.split(";".toRegex())
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    ) {
                        val items = entry.split(":".toRegex())
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                        val index = items[0].trim().toByte()
                        table[index.toInt()] = items[1]
                    }
                }

            return table
        }

        private fun applyRebus(
            rebusBoard : Array<ByteArray>?,
            solRebusTable : Array<String?>?,
            usrRebusTable : Array<String?>?,
            builder : PuzzleBuilder,
        ) {
            if (rebusBoard == null)
                return

            val height = builder.height
            val width = builder.width

            for (row in 0..<height) {
                for (col in 0..<width) {
                    val box = builder.getBox(row, col)
                    val index = rebusBoard[row][col].toInt()
                    if (index > 0 && box != null && !isBlock(box)) {
                        val truei = (index - 1).toByte().toInt()
                        if (
                            solRebusTable != null
                            && solRebusTable[truei] != null
                        ) {
                            box.solution = solRebusTable[truei]
                        }
                        usrRebusTable?.get(truei)?.let { value ->
                            box.response = value
                        }
                    }
                }
            }
        }

        private fun removeSolutionIfBlank(builder : PuzzleBuilder) {
            val height = builder.height
            val width = builder.width

            var emptySol : String? = null

            // search through for all same char being X or D.
            // if counter example found, return
            for (row in 0..<height) {
                for (col in 0..<width) {
                    val box = builder.getBox(row, col)
                    if (box != null && !isBlock(box)) {
                        if (emptySol == null) {
                            if (box.hasSolution()) {
                                emptySol = box.solution
                                if (!EMPTY_SOLS.contains(emptySol))
                                    return
                            }
                        } else if (emptySol != box.solution) {
                            return
                        }
                    }
                }
            }

            // if we made it this far then it's an empty solution
            builder.removeSolution()
        }

        @Throws(IOException::class)
        private fun writeGEXT(output : Sink, puz : Puzzle) {
            // approximate all shapes as circles
            if (!puz.hasShaped())
                return

            val width = puz.width
            val height = puz.height
            val numberOfBoxes = width * height
            val boxes : Array<Array<Box?>> = puz.boxes

            val gextSection = ByteArray(numberOfBoxes)

            for (row in boxes.indices) {
                for (col in boxes[row].indices) {
                    val box = boxes[row][col]
                    val supportedCircle = (
                        box != null && !isBlock(box) && box.hasShape()
                    )
                    if (supportedCircle) {
                        gextSection[(width * row) + col] = GEXT_SQUARE_CIRCLED
                    }
                }
            }

            writeExtraSection(output, GEXT_MARKER, gextSection)
        }

        @Throws(IOException::class)
        private fun writeRebus(
            output : Sink,
            puz : Puzzle,
            charsetEncoder : CharsetEncoder,
        ) {
            if (!hasRebus(puz))
                return

            val width = puz.width
            val height = puz.height
            val numberOfBoxes = width * height
            val boxes : Array<Array<Box?>> = puz.boxes

            val rebusBoard = ByteArray(numberOfBoxes)
            val solRebusTable = arrayOfNulls<String>(numberOfBoxes)
            val usrRebusTable = arrayOfNulls<String>(numberOfBoxes)

            var rebusIndex = 0

            for (row in boxes.indices) {
                for (col in boxes[row].indices) {
                    val box = boxes[row][col]
                    if (box != null && isRebusBox(box)) {
                        val solution = box.solution
                        val response = box.response

                        val tableIndex = width * row + col

                        rebusBoard[tableIndex] = (rebusIndex + 1).toByte()
                        solRebusTable[rebusIndex] = solution ?: ""
                        usrRebusTable[rebusIndex] = response

                        rebusIndex += 1
                    }
                }
            }

            writeExtraSection(output, REBUS_BOARD_MARKER, rebusBoard)

            val encodedSolTable
                = encodeRebusTable(solRebusTable, charsetEncoder)
            writeExtraSection(output, REBUS_TABLE_MARKER, encodedSolTable)

            val encodedUsrTable
                = encodeRebusTable(usrRebusTable, charsetEncoder)
            writeExtraSection(output, REBUS_USER_MARKER, encodedUsrTable)
        }

        private fun hasRebus(puz : Puzzle) : Boolean {
            val boxes : Array<Array<Box?>> = puz.boxes

            for (row in boxes.indices) {
                for (col in boxes[row].indices) {
                    val box = boxes[row][col]
                    if (box != null && isRebusBox(box))
                        return true
                }
            }

            return false
        }

        private fun isRebusBox(box : Box) : Boolean {
            if (isBlock(box))
                return false

            val solution = box.solution
            val response = box.response

            val solLen = solution?.length ?: 0
            val resLen = response.length

            return solLen > 1 || resLen > 1
        }

        @Throws(IOException::class)
        private fun writeExtraSection(
            output : Sink,
            marker : String,
            data : ByteArray,
        ) {
            CHARSET_ENCODER.encode(marker, output)
            output.writeShortLe(data.size.toShort())
            val chksum = cksum_region(data, 0, data.size, 0)
            output.writeShortLe(chksum.toShort())
            output.write(data)
            output.writeByte(0)
        }

        private fun encodeRebusTable(
            table : Array<String?>,
            charsetEncoder : CharsetEncoder,
        ) : ByteArray {
            val sb = StringBuilder()
            for (index in table.indices) {
                val entry = table[index]
                if (entry != null) {
                    // delete special characters
                    val cleanEntry = entry.replace(";", "").replace(":", "")
                    sb.append(index.toString())
                    sb.append(":")
                    sb.append(cleanEntry)
                    sb.append(";")
                }
            }

            val buffer = Buffer()
            charsetEncoder.encode(sb.toString(), buffer)
            return buffer.readByteArray()
        }

        /**
         * Put short value in little endian at index
         */
        private fun ByteArray.putShortLe(
            index : Int,
            value : Short,
        ) {
            val v = value.toInt() and 0xFFFF
            this[index] = (v and 0xFF).toByte()
            this[index + 1] = ((v shr 8) and 0xFF).toByte()
        }

        /**
         * Simple implementation of arraycopy to avoid JVM
         */
        private fun arraycopy(
            src : ByteArray,
            srcPos : Int,
            dest : ByteArray,
            destPos : Int,
            len : Int,
        ) {
            if (srcPos >= destPos) {
                for (i in 0 until len) {
                    dest[destPos + i] = src[srcPos + i]
                }
            } else {
                for (i in (len - 1) downTo 0) {
                    dest[destPos + i] = src[srcPos + i]
                }
            }
        }

    }
}

