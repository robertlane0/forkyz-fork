
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

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.content.FileProvider;

import app.crossword.yourealwaysbe.puz.io.IPuzIO;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.forkyz.util.AppPuzzleUtils;

public class FileHandlerShared extends FileProvider {
    private static final String TAG = "ForkyzFileHandlerShared";

    private static final String SHARE_AUTHORITY
        = "app.crossword.yourealwaysbe.forkyzfiles";
    private static final String SHARE_DIR = "shared";
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
        Context context, Puzzle puz, boolean writeBlank, boolean omitExtensions,
        Consumer<Uri> uriCallback
    ) {
        executorService.execute(() -> {
            cleanCache(context);
            final Uri uri = getShareUriBackground(
                context, puz, writeBlank, omitExtensions
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
        Context context, List<String> logcat, Consumer<Uri> uriCallback
    ) {
        executorService.execute(() -> {
            cleanCache(context);
            final Uri uri = getLogcatShareUriBackground(context, logcat);
            handler.post(() -> {
                uriCallback.accept(uri);
            });
        });
    }

    public static String getLogcatShareUriMimeType() {
        return LOGCAT_MIME_TYPE;
    }

    private static File getShareDir(Context context) {
        File shareDir = new File(context.getCacheDir(), SHARE_DIR);
        shareDir.mkdirs();
        return shareDir;
    }

    synchronized static private Uri getShareUriBackground(
        Context context,
        Puzzle puz,
        boolean writeBlank,
        boolean omitExtensions
    ) {
        String fileName = AppPuzzleUtils.generateFileName(puz)
            + FileHandler.FILE_EXT_IPUZ;
        File shareFile = new File(getShareDir(context), fileName);

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

        return getUriForFile(context, getShareAuthority(context), shareFile);
    }

    synchronized static private Uri getLogcatShareUriBackground(
        Context context,
        List<String> logcat
    ) {
        File shareFile = new File(getShareDir(context), LOGCAT_FILENAME);

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

        return getUriForFile(context, getShareAuthority(context), shareFile);
    }

    synchronized static private void cleanCache(Context context) {
        File[] files = getShareDir(context).listFiles();
        for (File file : files) {
            long ageMillis = System.currentTimeMillis() - file.lastModified();
            if (ageMillis > CACHE_CLEAR_AGE_MILLIS)
                file.delete();
        }
    }

    synchronized static private String getShareAuthority(Context context) {
        return context.getPackageName() + ".files";
    }
}
