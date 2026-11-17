
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.datetime.LocalDate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.forkyz.util.AppPuzzleUtils.deriveDate
import app.crossword.yourealwaysbe.forkyz.util.AppPuzzleUtils.deriveDateFromTitle
import app.crossword.yourealwaysbe.forkyz.util.AppPuzzleUtils.deriveSource

class AppPuzzleUtilsTest {
    @Test
    fun testNull() {
        Assertions.assertNull(deriveSource(null, null, null, null))
    }

    @Test
    fun testDeriveSourceSource() {
        Assertions.assertEquals(
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
        Assertions.assertEquals(
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
        Assertions.assertEquals(
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
        Assertions.assertEquals(
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
        Assertions.assertEquals(
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
        Assertions.assertEquals(
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
        Assertions.assertEquals(
            "Title",
            deriveSource(
                "",
                null,
                "",
                "Title",
            ),
        )
    }


    @Test
    fun testDeriveDateFromAbbreviatedPuzTitle() {
        Assertions.assertEquals(
            LocalDate(2009, 11, 13),
            deriveDateFromTitle("NY Times, Fri, Nov 13, 2009"),
        )
    }

    @Test
    fun testDeriveDateFromFullPuzTitle() {
        Assertions.assertEquals(
            LocalDate(2021, 8, 9),
            deriveDateFromTitle("NY Times, Monday, August 9, 2021"),
        )
    }

    @Test
    fun testDeriveDateFromTitleWithoutDate() {
        Assertions.assertNull(deriveDateFromTitle("A crossword title"))
    }

    @Test
    fun testDeriveDateFromInvalidTitleDate() {
        Assertions.assertNull(
            deriveDateFromTitle("NY Times, Monday, February 30, 2021"),
        )
    }

    fun testDeriveDateDate() {
        val date = LocalDate(2023, 1, 2)
        Assertions.assertEquals(
            date,
            deriveDate(
                date,
                "FileSource - 20230101 - FileTitle.puz",
            ),
        )
    }

    fun testDeriveDateFileXWordDL() {
        Assertions.assertEquals(
            null,
            deriveDate(
                LocalDate(2023, 1, 2),
                "FileSource - 20230101 - FileTitle.puz",
            ),
        )
    }

    fun testDeriveDateFileForkyz() {
        Assertions.assertEquals(
            null,
            deriveDate(
                LocalDate(2023, 1, 12),
                "2023-1-12-FileSource.ipuz",
            ),
        )
    }

    fun testDeriveDateFileShort() {
        Assertions.assertEquals(
            null,
            deriveDate(
                LocalDate(2023, 1, 2),
                "FileSource - 20230101.puz",
            ),
        )
    }

    fun testDeriveDateNull() {
        Assertions.assertNull(
            deriveDate(
                null,
                "Wrong File Format.puz",
            ),
        )
    }

    fun testDeriveDateNullNull() {
        Assertions.assertNull(
            deriveDate(
                null,
                null,
            ),
        )
    }
}

