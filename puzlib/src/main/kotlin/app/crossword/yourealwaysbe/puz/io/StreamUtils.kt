
package app.crossword.yourealwaysbe.puz.io

import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.Source

import no.synth.kmpzip.zip.ZipInputStream
import no.synth.kmpzip.kotlinx.SourceInputStream

object StreamUtils {
    /**
     * If given input, returns unzipped input
     */
    @Throws(IOException::class)
    fun unzipOrPassThrough(input : Source) : Source {
        var copy = input.peek()
        try {
            ZipInputStream(SourceInputStream(copy)).use { zis ->
                var entry = zis.nextEntry
                // using null pointer error here to break to catch to
                // resit / no zipped (bit hacky..)
                while (entry!!.isDirectory) {
                    entry = zis.nextEntry
                }
                copy = Buffer()
                copy.write(zis.readBytes())
            }
        } catch (e : Exception) {
            // not zipped
            copy = input
        }

        return copy
    }

    /**
     * Create an in-memory copy of input that does not need closing
     *
     * Consumes input
     */
    @Throws(IOException::class)
    fun makeCopy(input : Source) : Source {
        val copy = Buffer()
        input.transferTo(copy)
        return copy
    }
}

