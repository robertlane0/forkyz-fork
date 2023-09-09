
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
    public void testDeriveSourceFile() {
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

    public void testDeriveDateFile() {
        assertEquals(
            null,
            AppPuzzleUtils.deriveDate(
                LocalDate.of(2023, 01, 02),
                "FileSource - 20230101 - FileTitle.puz"
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

