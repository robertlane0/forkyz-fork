
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.datetime.LocalDate

import org.junit.Assert
import org.junit.Test

import app.crossword.yourealwaysbe.forkyz.util.AppPuzzleUtils.deriveDate
import app.crossword.yourealwaysbe.forkyz.util.AppPuzzleUtils.deriveSource

class AppPuzzleUtilsTest {
    @Test
    fun testNull() {
        Assert.assertNull(deriveSource(null, null, null, null))
    }

    @Test
    fun testDeriveSourceSource() {
        Assert.assertEquals(
            "Source",
            deriveSource(
                "Source",
                "FileSource - 20230101 - FileTitle.puz",
                "Author",
                "Title",
            ),
        )
    }

    @Test
    fun testDeriveSourceFileXWordDL() {
        Assert.assertEquals(
            "FileSource",
            deriveSource(
                null,
                "FileSource - 20230101 - FileTitle.puz",
                "Author",
                "Title"
            )
        )
    }

    @Test
    fun testDeriveSourceFileForkyz() {
        Assert.assertEquals(
            "FileSource",
            deriveSource(
                null,
                "2023-1-15-FileSource.ipuz",
                "Author",
                "Title",
            ),
        )
    }

    @Test
    fun testDeriveSourceFileForkyzUUID() {
        Assert.assertEquals(
            "FileSource",
            deriveSource(
                null,
                "2023-1-15-FileSource-a1b2c3d4.ipuz",
                "Author",
                "Title",
            ),
        )
    }

    @Test
    fun testDeriveSourceShort() {
        Assert.assertEquals(
            "FileSource",
            deriveSource(
                null,
                "FileSource - 20230101.puz",
                "Author",
                "Title",
            ),
        )
    }

    @Test
    fun testDeriveSourceAuthor() {
        Assert.assertEquals(
            "Author",
            deriveSource(
                null,
                "Wrong filename format",
                "Author",
                "Title",
            ),
        )
    }

    @Test
    fun testDeriveSourceTitle() {
        Assert.assertEquals(
            "Title",
            deriveSource(
                "",
                null,
                "",
                "Title",
            ),
        )
    }

    fun testDeriveDateDate() {
        val date = LocalDate(2023, 1, 2)
        Assert.assertEquals(
            date,
            deriveDate(
                date,
                "FileSource - 20230101 - FileTitle.puz",
            ),
        )
    }

    fun testDeriveDateFileXWordDL() {
        Assert.assertEquals(
            null,
            deriveDate(
                LocalDate(2023, 1, 2),
                "FileSource - 20230101 - FileTitle.puz",
            ),
        )
    }

    fun testDeriveDateFileForkyz() {
        Assert.assertEquals(
            null,
            deriveDate(
                LocalDate(2023, 1, 12),
                "2023-1-12-FileSource.ipuz",
            ),
        )
    }

    fun testDeriveDateFileShort() {
        Assert.assertEquals(
            null,
            deriveDate(
                LocalDate(2023, 1, 2),
                "FileSource - 20230101.puz",
            ),
        )
    }

    fun testDeriveDateNull() {
        Assert.assertNull(
            deriveDate(
                null,
                "Wrong File Format.puz",
            ),
        )
    }

    fun testDeriveDateNullNull() {
        Assert.assertNull(
            deriveDate(
                null,
                null,
            ),
        )
    }
}

