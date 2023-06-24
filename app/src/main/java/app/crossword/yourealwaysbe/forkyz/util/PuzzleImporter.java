package app.crossword.yourealwaysbe.forkyz.util;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.logging.Logger;

import android.content.Context;
import android.net.Uri;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.io.PuzzleStreamReader;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler;
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle;

/**
 * Takes an arbitrary URI and tries to convert to a puzzle and import
 */
public class PuzzleImporter {
    private static final Logger LOGGER
        = Logger.getLogger(PuzzleImporter.class.getCanonicalName());

    /** Import from a URI supported by resolver
     *
     * Currently does not use file extension or MIME type. Instead, use puzlib
     * that tries each known format in turn until one succeeds. Clunky, but
     * hopefully robust.
     *
     * @return new puz handle if succeeded (will return null if failed
     * or uri is null)
     */
    public static PuzHandle importUri(
        Context context,
        FileHandler fileHandler,
        Uri uri
    ) {
        if (uri == null)
            return null;

        Puzzle puz = null;

        try {
            puz = PuzzleStreamReader.parseInputStatic(
                new BufferedInputStream(
                    context.getContentResolver().openInputStream(uri)
                )
            );
        } catch (FileNotFoundException e) {
            LOGGER.info("FileNotFoundException with " + uri);
        }

        if (puz == null)
            return null;

        ensurePuzDate(puz);
        ensurePuzSource(context, puz);

        try {
            return fileHandler.saveNewPuzzle(
                puz, AppPuzzleUtils.generateFileName(puz)
            );
        } catch (IOException e) {
            LOGGER.severe("Failed to save imported puzzle: " + e);
            return null;
        }
    }

    /**
     * Try best to make sure there is some source
     *
     * Fall back to author, title, fallback
     */
    private static void ensurePuzSource(Context context, Puzzle puz) {
        String source = puz.getSource();
        if (source == null || source.isEmpty())
            puz.setSource(puz.getAuthor());
        source = puz.getSource();
        if (source == null || source.isEmpty())
            puz.setSource(puz.getTitle());
        source = puz.getSource();
        if (source == null || source.isEmpty())
            puz.setSource(context.getString(R.string.import_fallback_source));
    }

    private static void ensurePuzDate(Puzzle puz) {
        if (puz.getDate() == null)
            puz.setDate(LocalDate.now());
    }
}
