
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.io.readString

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class StreamUtilsTest {
    @Test
    fun testUnzip() {
        SourceUtils.getResource("/hello.zip").use { input ->
            val readString = StreamUtils.unzipOrPassThrough(input)
                .readString()
            Assertions.assertEquals(readString, "Hello\n")
        }
    }
}

