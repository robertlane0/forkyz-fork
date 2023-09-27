
package app.crossword.yourealwaysbe.forkyz.util;

import java.time.LocalDate;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AppPuzzleUtilsTest {

    @Test
    public void testNull() {
        assertNull(AppPuzzleUtils.deriveSource(null, null, null, null));
    }

    @Test
    public void testDeriveSourceSource() {
        assertEquals(
            "Source",
            AppPuzzleUtils.deriveSource(
                "Source",
                "FileSource - 20230101 - FileTitle.puz",
                "Author",
                "Title"
            )
        );
    }

    @Test
    public void testDeriveSourceFileXWordDL() {
        assertEquals(
            "FileSource",
            AppPuzzleUtils.deriveSource(
                null,
                "FileSource - 20230101 - FileTitle.puz",
                "Author",
                "Title"
            )
        );
    }

    @Test
    public void testDeriveSourceFileForkyz() {
        assertEquals(
            "FileSource",
            AppPuzzleUtils.deriveSource(
                null,
                "2023-1-15-FileSource.ipuz",
                "Author",
                "Title"
            )
        );
    }

    @Test
    public void testDeriveSourceFileForkyzUUID() {
        assertEquals(
            "FileSource",
            AppPuzzleUtils.deriveSource(
                null,
                "2023-1-15-FileSource-a1b2c3d4.ipuz",
                "Author",
                "Title"
            )
        );
    }

    @Test
    public void testDeriveSourceShort() {
        assertEquals(
            "FileSource",
            AppPuzzleUtils.deriveSource(
                null,
                "FileSource - 20230101.puz",
                "Author",
                "Title"
            )
        );
    }

    @Test
    public void testDeriveSourceAuthor() {
        assertEquals(
            "Author",
            AppPuzzleUtils.deriveSource(
                null,
                "Wrong filename format",
                "Author",
                "Title"
            )
        );
    }

    @Test
    public void testDeriveSourceTitle() {
        assertEquals(
            "Title",
            AppPuzzleUtils.deriveSource(
                "",
                null,
                "",
                "Title"
            )
        );
    }

    public void testDeriveDateDate() {
        LocalDate date = LocalDate.of(2023, 01, 02);
        assertEquals(
            date,
            AppPuzzleUtils.deriveDate(
                date,
                "FileSource - 20230101 - FileTitle.puz"
            )
        );

    }

    public void testDeriveDateFileXWordDL() {
        assertEquals(
            null,
            AppPuzzleUtils.deriveDate(
                LocalDate.of(2023, 01, 02),
                "FileSource - 20230101 - FileTitle.puz"
            )
        );
    }

    public void testDeriveDateFileForkyz() {
        assertEquals(
            null,
            AppPuzzleUtils.deriveDate(
                LocalDate.of(2023, 01, 12),
                "2023-1-12-FileSource.ipuz"
            )
        );

    }

    public void testDeriveDateFileShort() {
        assertEquals(
            null,
            AppPuzzleUtils.deriveDate(
                LocalDate.of(2023, 01, 02),
                "FileSource - 20230101.puz"
            )
        );
    }

    public void testDeriveDateNull() {
        assertNull(
            AppPuzzleUtils.deriveDate(
                null,
                "Wrong File Format.puz"
            )
        );
    }

    public void testDeriveDateNullNull() {
        assertNull(
            AppPuzzleUtils.deriveDate(
                null,
                null
            )
        );
    }
}

