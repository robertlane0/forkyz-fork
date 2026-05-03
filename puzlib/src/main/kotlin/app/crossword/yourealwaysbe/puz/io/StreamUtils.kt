
package app.crossword.yourealwaysbe.puz.io

import java.io.OutputStream
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlinx.io.asSink

import no.synth.kmpzip.zip.ZipInputStream
import no.synth.kmpzip.kotlinx.SourceInputStream

object StreamUtils {
    const val DEFAULT_BUFFER_SIZE : Int = 1024

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

    /**
     * Copies the data from an input to an OutputStream object.
     *
     * @param source The input to be read.
     * @param destinationStream The output stream to be written to.
     * @return long value of the number of bytes copied.
     * @exception IOException from base io calls.
     */
    @Throws(IOException::class)
    fun copyStream(source : Source, destinationStream : OutputStream) : Long {
        return source.transferTo(destinationStream.asSink())
    }
}

