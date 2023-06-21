package app.crossword.yourealwaysbe.forkyz;

import android.Manifest;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.DisplayMetrics;
import androidx.core.content.ContextCompat;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.preference.PreferenceManager;
import androidx.work.Configuration;

import dagger.hilt.android.HiltAndroidApp;

import app.crossword.yourealwaysbe.puz.MovementStrategy;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.settings.StorageLocation;
import app.crossword.yourealwaysbe.util.ExitActivity;
import app.crossword.yourealwaysbe.util.ThemeHelper;
import app.crossword.yourealwaysbe.util.files.FileHandlerProvider;
import app.crossword.yourealwaysbe.util.files.PuzHandle;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.inject.Inject;

@HiltAndroidApp
public class ForkyzApplication extends Application implements Configuration.Provider {
    private static final Logger LOGGER
        = Logger.getLogger(ForkyzApplication.class.getCanonicalName());

    public static final String PUZZLE_DOWNLOAD_CHANNEL_ID = "forkyz.downloads";

    @Inject
    ForkyzSettings settings;

    @Inject
    FileHandlerProvider fileHandlerProvider;

    // for background downloads
    // https://developer.android.com/reference/androidx/hilt/work/HiltWorker
    @Inject
    HiltWorkerFactory workerFactory;

    private static ForkyzApplication INSTANCE;
    private Playboard board;
    private PuzHandle puzHandle;
    private SharedPreferences prefs;

    public void exitApplication() {
        ExitActivity.exit(this);
    }

    /**
     * Set the board and base file of the puzzle loaded on it
     */
    public void setBoard(Playboard board, PuzHandle puzHandle){
        this.board = board;
        this.puzHandle = puzHandle;
    }

    public void clearBoard() {
        this.board = null;
        this.puzHandle = null;
    }

    public Playboard getBoard() {
         return board;
    }

    public PuzHandle getPuzHandle() {
        return puzHandle;
    }

    /**
     * Save the puzzle
     */
    public void saveBoard() {
        PuzHandle puzHandle = getPuzHandle();
        if (puzHandle == null) {
            LOGGER.severe("No puz handle to save puzzle to.");
            return;
        }

        Playboard board = getBoard();
        if (board == null) {
            LOGGER.severe("No board to save.");
            return;
        }

        Puzzle puz = board.getPuzzle();
        if (puz == null) {
            LOGGER.severe("No puzzle associated to the board to save.");
            return;
        }

        fileHandlerProvider.get(fileHandler -> {
            // TODO: save off main thread?
            try {
                fileHandler.save(puz, puzHandle);
            } catch (IOException e) {
                LOGGER.severe("Error saving puzzle.");
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onCreate() {
        INSTANCE = this;

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate();

        AndroidVersionUtils.Factory.getInstance().createNotificationChannel(this);

        (new ThemeHelper(settings)).themeApplication(this);
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build();
    }

    public static boolean isLandscape(DisplayMetrics metrics){
        return metrics.widthPixels > metrics.heightPixels;
    }

    public static boolean isTabletish(DisplayMetrics metrics) {
        double x = Math.pow(metrics.widthPixels / metrics.xdpi, 2);
        double y = Math.pow(metrics.heightPixels / metrics.ydpi, 2);
        double screenInches = Math.sqrt(x + y);
        if (screenInches > 9) { // look for a 9" or larger screen.
            return true;
        } else {
            return false;
        }
    }

    public static boolean isMiniTabletish(DisplayMetrics metrics) {
        return AndroidVersionUtils.Factory
            .getInstance()
            .isMiniTabletish(metrics);
    }

    public static ForkyzApplication getInstance(){
        return INSTANCE;
    }

    public void isMissingWritePermission(Consumer<Boolean> cb) {
        settings.getFileHandlerSettings(fhs -> {
            boolean isLegacy = StorageLocation.EXTERNAL_LEGACY.equals(
                fhs.getStorageLocation()
            );

            cb.accept(
                isLegacy
                && ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            );
        });
    }

    public MovementStrategy getMovementStrategy() {
        String stratName = prefs.getString(
            "movementStrategy", "MOVE_NEXT_ON_AXIS"
        );
        switch (stratName) {
        case "MOVE_NEXT_ON_AXIS":
            return MovementStrategy.MOVE_NEXT_ON_AXIS;
        case "STOP_ON_END":
            return MovementStrategy.STOP_ON_END;
        case "MOVE_NEXT_CLUE":
            return MovementStrategy.MOVE_NEXT_CLUE;
        case "MOVE_PARALLEL_WORD":
            return MovementStrategy.MOVE_PARALLEL_WORD;
        default:
            LOGGER.severe("Unknown MovementStrategy " + stratName);
            return null;
        }
    }
}
