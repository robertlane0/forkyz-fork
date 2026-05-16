
package app.crossword.yourealwaysbe.forkyz.puz.io.charset

import kotlinx.io.Sink

class CP1252Encoder : CharsetEncoder {
    override fun encode(value : String, sink : Sink) {
        for (c in value)
            encode(c, sink)
    }

    override fun encode(value : Char, sink : Sink) {
        val ucode = value.code
        if (ucode > 0xFF)
            sink.writeByte(UNKNOWN_BYTE)
        else if (value in CP1252_0X80_TO_0X9F)
            sink.writeByte(CP1252_0X80_TO_0X9F[value]!!)
        else
            sink.writeByte(ucode.toByte())
    }

    companion object {
        private val UNKNOWN_BYTE = 0x3F.toByte()
        private val CP1252_0X80_TO_0X9F = mapOf(
            '\u20AC' to 0x80.toByte(),
            '\u201A' to 0x82.toByte(),
            '\u0192' to 0x83.toByte(),
            '\u201E' to 0x84.toByte(),
            '\u2026' to 0x85.toByte(),
            '\u2020' to 0x86.toByte(),
            '\u2021' to 0x87.toByte(),
            '\u02C6' to 0x88.toByte(),
            '\u2030' to 0x89.toByte(),
            '\u0160' to 0x8a.toByte(),
            '\u2039' to 0x8b.toByte(),
            '\u0152' to 0x8c.toByte(),
            '\u017D' to 0x8e.toByte(),
            '\u2018' to 0x91.toByte(),
            '\u2019' to 0x92.toByte(),
            '\u201C' to 0x93.toByte(),
            '\u201D' to 0x94.toByte(),
            '\u2022' to 0x95.toByte(),
            '\u2013' to 0x96.toByte(),
            '\u2014' to 0x97.toByte(),
            '\u02DC' to 0x98.toByte(),
            '\u2122' to 0x99.toByte(),
            '\u0161' to 0x9a.toByte(),
            '\u203A' to 0x9b.toByte(),
            '\u0153' to 0x9c.toByte(),
            '\u017E' to 0x9e.toByte(),
            '\u0178' to 0x9f.toByte(),
        )
    }
}

