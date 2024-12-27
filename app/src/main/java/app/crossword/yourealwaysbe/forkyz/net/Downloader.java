package app.crossword.yourealwaysbe.forkyz.net;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;

import app.crossword.yourealwaysbe.puz.Puzzle;

public interface Downloader {

    public static class DownloadResult {
        public static final DownloadResult FAILED
            = new DownloadResult(true);
        public static final DownloadResult ALREADY_EXISTS
            = new DownloadResult(false);

        Puzzle puzzle;
        String fileName;
        boolean failed;

        public DownloadResult(Puzzle puzzle, String fileName) {
            this.puzzle = puzzle;
            this.fileName = fileName;
        }

        private DownloadResult(boolean failed) {
            this.failed = failed;
        }

        public boolean isFailed() {
            return puzzle == null && fileName == null && failed;
        }

        public boolean isAlreadyExists() {
            return puzzle == null && fileName == null && !failed;
        }

        public boolean isSuccess() {
            return puzzle != null && fileName != null;
        }

        public Puzzle getPuzzle() { return puzzle; }
        public String getFileName() { return fileName; }
    }

    /**
     * Constant internal name (for e.g. storing in prefs)
     */
    String getInternalName();

    /**
     * Human readable / local configurable name
     */
    String getName();

    /**
     * Returns a URL where the user may support the crossword source
     *
     * @return null if no reasonable support/source URL (discouraged)
     */
    String getSupportUrl();

    /**
     * Download the puzzle for a given date
     *
     * Existing file names passed. Don't download if the date's puzzle
     * already exists -- downloader will not save puzzles with filenames
     * that already exist.
     */
    DownloadResult download(LocalDate date, Set<String> existingFileNames);

    boolean alwaysRun();

    /**
     * If a puzzle is currently available for the date
     */
    boolean isAvailable(LocalDate date);

    /**
     * How long since/until available
     *
     * Returns null if not known or was never available. Negative means how
     * long since.
     */
    Duration getUntilAvailable(LocalDate date);

    /**
     * Most recent puzzle date
     *
     * May return null if there is no most recent
     */
    LocalDate getLatestDate();

    /**
     * Most recent puzzle date up to until (inclusive)
     *
     * May return null if there is no most recent
     */
    LocalDate getLatestDate(LocalDate until);

    void setTimeout(int timeoutMillis);
    int getTimeout();
}
