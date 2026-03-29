
package app.crossword.yourealwaysbe.puz.io

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream

object StreamUtils {
    const val DEFAULT_BUFFER_SIZE : Int = 1024

    /**
     * Make given input stream into a ByteArrayInputStream
     *
     * By copying into a buffer if needs be. Either you get back the original
     * input stream, or a ByteArrayInputStream that doesn't need closing.
     */
    @Throws(IOException::class)
    fun makeByteArrayInputStream(
        inStream : InputStream,
    ) : ByteArrayInputStream {
        if (inStream is ByteArrayInputStream)
            return inStream

        val baos = ByteArrayOutputStream()
        copyStream(inStream, baos)
        return ByteArrayInputStream(baos.toByteArray())
    }

    /**
     * If given input stream, returns unzipped input stream
     *
     * Returns a stream with marksSupported. Is (a copy of) original stream if
     * not zipped.
     *
     * Closing return stream has no effect (it's a byte array stream).
     */
    @Throws(IOException::class)
    fun unzipOrPassThrough(inStream : InputStream) : InputStream {
        var bis = makeByteArrayInputStream(inStream)
        try {
            ZipInputStream(bis).use { zis ->
                var entry = zis.getNextEntry()
                // using null pointer error here to break to catch to
                // resit / no zipped (bit hacky..)
                while (entry!!.isDirectory()) {
                    entry = zis.getNextEntry()
                }
                val baos = ByteArrayOutputStream()
                copyStream(zis, baos)
                bis = ByteArrayInputStream(baos.toByteArray())
            }
        } catch (e : Exception) {
            // not zipped
            bis.reset()
        }

        return bis
    }

    /**
     * Create a byte version copy of is
     *
     * Allows resetting.
     */
    @Throws(IOException::class)
    fun copyInputStream(source : InputStream) : ByteArrayInputStream {
        val outCopy = ByteArrayOutputStream()
        copyStream(source, outCopy)
        return ByteArrayInputStream(outCopy.toByteArray())
    }

    /**
     * Copies the data from an InputStream object to an OutputStream object.
     *
     * @param sourceStream The input stream to be read.
     * @param destinationStream The output stream to be written to.
     * @return int value of the number of bytes copied.
     * @exception IOException from java.io calls.
     */
    @Throws(IOException::class)
    fun copyStream(
        sourceStream : InputStream,
        destinationStream : OutputStream,
    ) : Int {
        var bytesRead = 0
        var totalBytes = 0
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

        while (bytesRead >= 0) {
            bytesRead = sourceStream.read(buffer, 0, buffer.size)

            if (bytesRead > 0) {
                destinationStream.write(buffer, 0, bytesRead)
            }

            totalBytes += bytesRead
        }

        destinationStream.flush()
        destinationStream.close()

        return totalBytes
    }

    @Throws(IOException::class)
    fun getStreamBytes(source : InputStream) : ByteArray {
        val baos = ByteArrayOutputStream()
        copyStream(source, baos)
        return baos.toByteArray()
    }
}

