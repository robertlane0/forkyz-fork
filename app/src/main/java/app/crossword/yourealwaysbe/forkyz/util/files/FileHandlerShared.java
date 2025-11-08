
package app.crossword.yourealwaysbe.forkyz.util.files;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import app.crossword.yourealwaysbe.puz.io.IPuzIO;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.forkyz.util.AppPuzzleUtils;
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils;

public class FileHandlerShared {
    private static final String TAG = "ForkyzFileHandlerShared";

    private static final String LOGCAT_FILENAME = "logcat.txt";
    private static final String LOGCAT_MIME_TYPE = "text/plain";
    // clear older than 1 hour
    private static final long CACHE_CLEAR_AGE_MILLIS = 60 * 60 * 1000L;

    private static ExecutorService executorService
        = Executors.newSingleThreadExecutor();
    private static Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Save the puzzle to cache and get Uri for sharing
     *
     * Runs off main thread to save file, calls back on main thread with
     * uriCallback.
     *
     * @param writeBlank whether to omit current play data such as
     * flagged clues and filled in letters
     * @param omitExtensions whether to omit Forkyz IPuz extensions
     */
    synchronized public static void getShareUri(
        NativeBackendUtils utils,
        Puzzle puz,
        boolean writeBlank,
        boolean omitExtensions,
        Consumer<Uri> uriCallback
    ) {
        executorService.execute(() -> {
            cleanCache(utils);
            final Uri uri = getShareUriBackground(
                utils, puz, writeBlank, omitExtensions
            );
            handler.post(() -> {
                uriCallback.accept(uri);
            });
        });
    }

    public static String getShareUriMimeType() {
        return FileHandler.MIME_TYPE_IPUZ;
    }

    /**
     * Save logcat to cache and get Uri for sharing
     *
     * Runs off main thread to save file, calls back on main thread with
     * uriCallback.
     *
     * @param lines the logcat lines
     */
    synchronized public static void getLogcatShareUri(
        NativeBackendUtils utils, List<String> logcat, Consumer<Uri> uriCallback
    ) {
        executorService.execute(() -> {
            cleanCache(utils);
            final Uri uri = getLogcatShareUriBackground(utils, logcat);
            handler.post(() -> {
                uriCallback.accept(uri);
            });
        });
    }

    public static String getLogcatShareUriMimeType() {
        return LOGCAT_MIME_TYPE;
    }

    synchronized static private Uri getShareUriBackground(
        NativeBackendUtils utils,
        Puzzle puz,
        boolean writeBlank,
        boolean omitExtensions
    ) {
        String fileName = AppPuzzleUtils.generateFileName(puz)
            + FileHandler.FILE_EXT_IPUZ;
        File shareFile = new File(utils.getShareDir(), fileName);

        try (
            OutputStream os = new BufferedOutputStream(
                new FileOutputStream(shareFile)
            )
        ) {
            IPuzIO.writePuzzle(puz, os, writeBlank, omitExtensions);
        } catch (IOException e) {
            Log.e(TAG, "Could not create file for sharing: " + e);
            return null;
        }

        return utils.getShareURIForFile(shareFile);
    }

    synchronized static private Uri getLogcatShareUriBackground(
        NativeBackendUtils utils,
        List<String> logcat
    ) {
        File shareFile = new File(utils.getShareDir(), LOGCAT_FILENAME);

        try (
            PrintStream os = new PrintStream(
                new BufferedOutputStream(
                    new FileOutputStream(shareFile)
                )
            )
        ) {
            for (String line : logcat)
                os.println(line);
        } catch (IOException e) {
            Log.e(TAG, "Could not create file for sharing: " + e);
            return null;
        }

        return utils.getShareURIForFile(shareFile);
    }

    synchronized static private void cleanCache(NativeBackendUtils utils) {
        File[] files = utils.getShareDir().listFiles();
        for (File file : files) {
            long ageMillis = System.currentTimeMillis() - file.lastModified();
            if (ageMillis > CACHE_CLEAR_AGE_MILLIS)
                file.delete();
        }
    }
}
