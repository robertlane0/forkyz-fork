package app.crossword.yourealwaysbe.forkyz.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import android.content.Context;
import android.net.Uri;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.net.AmuseLabsStreamScraper;
import app.crossword.yourealwaysbe.forkyz.net.CrosswordNexusStreamScraper;
import app.crossword.yourealwaysbe.forkyz.net.PAPuzzlesStreamScraper;
import app.crossword.yourealwaysbe.forkyz.net.PzzlStreamScraper;
import app.crossword.yourealwaysbe.forkyz.net.RaetselZentraleSchwedenStreamScraper;
import app.crossword.yourealwaysbe.forkyz.util.files.DirHandle;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandle;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler;
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.PuzzleParser;
import app.crossword.yourealwaysbe.puz.io.PuzzleStreamReader;
import app.crossword.yourealwaysbe.puz.io.StreamUtils;

/**
 * Takes an arbitrary URI and tries to convert to a puzzle and import
 */
public class PuzzleImporter {
    private static final Logger LOGGER
        = Logger.getLogger(PuzzleImporter.class.getCanonicalName());

    private static PuzzleParser[] STREAM_SCRAPERS = new PuzzleParser[] {
        new AmuseLabsStreamScraper(),
        new CrosswordNexusStreamScraper(),
        new PAPuzzlesStreamScraper(),
        new PzzlStreamScraper(),
        new RaetselZentraleSchwedenStreamScraper()
    };

    /** Import from a URI supported by resolver
     *
     * Currently does not use file extension or MIME type. Instead, use puzlib
     * that tries each known format in turn until one succeeds. Clunky, but
     * hopefully robust.
     *
     * Will try local content resolver first, then attempt treating uri as a
     * remote URL
     *
     * @param timeout download timeout if uri is not local
     * @return new puz handle if succeeded (will return null if failed
     * or uri is null)
     */
    public static PuzHandle importUri(
        Context context, FileHandler fileHandler, Uri uri, int timeout
    ) {
        if (uri == null)
            return null;

        try (
            InputStream is = new BufferedInputStream(
                context.getContentResolver().openInputStream(uri)
            )
        ) {
            return importInputStream(
                context, fileHandler, is, getFileName(uri), timeout
            );
        } catch (IOException e) {
            try (
                InputStream is = getRemoteInputStream(uri.toString(), timeout)
            ) {
                return importInputStream(
                    context, fileHandler, is, getFileName(uri), timeout
                );
            } catch (IOException e2) {
                // we tried
                return null;
            }
        }
    }

    public static List<PuzHandle> processToImportDirectory(
        Context context, FileHandler fileHandler
    ) {
        List<PuzHandle> importedFiles = new ArrayList<>();

        DirHandle toImportDir
            = fileHandler.getToImportDirectory();

        for (FileHandle fh : fileHandler.listFiles(toImportDir)) {
            try (InputStream is = fileHandler.getBufferedInputStream(fh)) {
                PuzHandle ph = importInputStream(
                    context, fileHandler, is, fileHandler.getName(fh), 0
                );
                if (ph == null) {
                    // deliberately only get once needed to avoid
                    // unneeded dir creation in java file mode
                    // (currently SAF creates all dirs upfront)
                    DirHandle toImportFailedDir
                        = fileHandler.getToImportFailedDirectory();
                    fileHandler.moveTo(fh, toImportDir, toImportFailedDir);
                } else {
                    DirHandle toImportDoneDir
                        = fileHandler.getToImportDoneDirectory();
                    importedFiles.add(ph);
                    fileHandler.moveTo(fh, toImportDir, toImportDoneDir);
                }
            } catch (IOException e) {
                DirHandle toImportFailedDir
                    = fileHandler.getToImportFailedDirectory();
                fileHandler.moveTo(fh, toImportDir, toImportFailedDir);
            }
        }

        return importedFiles;
    }

    /**
     * Try best to make sure there is some source
     *
     * Fall back to author, title, fallback
     */
    private static void ensurePuzSource(
        Context context, Puzzle puz, String fileName
    ) {
        String source = AppPuzzleUtils.deriveSource(
            puz.getSource(),
            fileName,
            puz.getAuthor(),
            puz.getTitle()
        );

        if (source == null || source.isEmpty())
            source = context.getString(R.string.import_fallback_source);

        puz.setSource(source);
    }

    private static void ensurePuzDate(Puzzle puz, String fileName) {
        LocalDate date = AppPuzzleUtils.deriveDate(puz.getDate(), fileName);
        if (date == null)
            date = LocalDate.now();
        puz.setDate(date);
    }

    private static String getFileName(Uri uri) {
        if (uri == null)
            return null;

        String path = uri.getPath();
        if (path != null)
            return new File(path).getName();

        return null;
    }

    /**
     * Import input stream
     *
     * Try first as a local file, but then see if it's a webpage that we
     * recognise (may include further web lookups).
     */
    private static PuzHandle importInputStream(
        Context context,
        FileHandler fileHandler,
        InputStream is,
        String fileName,
        int timeout
    ) throws IOException {
        ByteArrayInputStream bis = StreamUtils.makeByteArrayInputStream(is);

        Puzzle puz = PuzzleStreamReader.parseInputStatic(bis);
        if (puz == null) {
            bis.reset();
            puz = importPuzzleFromWebPage(context, bis, timeout);
        }

        if (puz == null)
            return null;

        ensurePuzDate(puz, fileName);
        ensurePuzSource(context, puz, fileName);

        try {
            return fileHandler.saveNewPuzzle(
                puz, AppPuzzleUtils.generateFileName(puz)
            );
        } catch (IOException e) {
            LOGGER.severe("Failed to save imported puzzle: " + e);
            return null;
        }

    }

    private static BufferedInputStream getRemoteInputStream(
        String url, int timeout
    ) throws IOException {
        return NetUtils.getInputStream(url, null, timeout);
    }

    /**
     * Try to extract puzzle from recognised webpages
     *
     * These are usually those where the puzzle is a step or two away.
     * E.g. the metro website has an embedded iframe with a puzzle that can be
     * parsed.
     *
     * @param timeout the timeout to use when following urls
     */
    private static Puzzle importPuzzleFromWebPage(
        Context context, InputStream is, int timeout
    ) {
        try {
            ByteArrayInputStream bis = StreamUtils.makeByteArrayInputStream(is);
            for (PuzzleParser parser : STREAM_SCRAPERS) {
                bis.reset();
                try {
                    Puzzle puz = parser.parseInput(bis);
                    if (puz != null)
                        return puz;
                } catch (Exception e) {
                    // on to the next
                }
            }
        } catch (IOException e) {
            // we tried
        }

        return null;
    }
}
