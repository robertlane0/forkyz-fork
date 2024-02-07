
package app.crossword.yourealwaysbe.puz.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StreamUtils {
    public static final int DEFAULT_BUFFER_SIZE = 1024;

    /**
     * Make given input stream into a ByteArrayInputStream
     *
     * By copying into a buffer if needs be. Either you get back the original
     * input stream, or a ByteArrayInputStream that doesn't need closing.
     */
    public static ByteArrayInputStream makeByteArrayInputStream(
        InputStream is
    ) throws IOException {
        if (is instanceof ByteArrayInputStream)
            return (ByteArrayInputStream) is;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copyStream(is, baos);
        return  new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * If given input stream, returns unzipped input stream
     *
     * Returns a stream with marksSupported. Is (a copy of) original stream if
     * not zipped.
     *
     * Closing return stream has no effect (it's a byte array stream).
     */
    public static InputStream unzipOrPassThrough(InputStream is)
            throws IOException {

        ByteArrayInputStream bis = makeByteArrayInputStream(is);
        try (
            ZipInputStream zis = new ZipInputStream(bis)
        ) {
            ZipEntry entry = zis.getNextEntry();
            while (entry.isDirectory()) {
                entry = zis.getNextEntry();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            copyStream(zis, baos);
            bis = new ByteArrayInputStream(baos.toByteArray());
        } catch (Exception e) {
            // not zipped
            bis.reset();
        }

        return bis;
    }

    /**
     * Create a byte version copy of is
     *
     * Allows resetting.
     */
    public static ByteArrayInputStream copyInputStream(InputStream source)
            throws IOException {
        ByteArrayOutputStream outCopy = new ByteArrayOutputStream();
        copyStream(source, outCopy);
        return new ByteArrayInputStream(outCopy.toByteArray());
    }

    /**
     * Copies the data from an InputStream object to an OutputStream object.
     *
     * @param sourceStream
     *            The input stream to be read.
     * @param destinationStream
     *            The output stream to be written to.
     * @return int value of the number of bytes copied.
     * @exception IOException
     *                from java.io calls.
     */
    public static int copyStream(InputStream sourceStream, OutputStream destinationStream)
            throws IOException {
        int bytesRead = 0;
        int totalBytes = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        while (bytesRead >= 0) {
            bytesRead = sourceStream.read(buffer, 0, buffer.length);

            if (bytesRead > 0) {
                destinationStream.write(buffer, 0, bytesRead);
            }

            totalBytes += bytesRead;
        }

        destinationStream.flush();
        destinationStream.close();

        return totalBytes;
    }

    public static byte[] getStreamBytes(InputStream source)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copyStream(source, baos);
        return baos.toByteArray();
    }
}
