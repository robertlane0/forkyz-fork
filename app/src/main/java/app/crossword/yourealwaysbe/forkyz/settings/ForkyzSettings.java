
package app.crossword.yourealwaysbe.forkyz.settings;

import android.content.Context;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences;
import android.os.Handler;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

import app.crossword.yourealwaysbe.forkyz.net.Downloader;
import app.crossword.yourealwaysbe.forkyz.net.Downloaders;
import app.crossword.yourealwaysbe.forkyz.tools.ExternalDictionary;
import app.crossword.yourealwaysbe.forkyz.util.files.Accessor;
import app.crossword.yourealwaysbe.puz.MovementStrategy;
import app.crossword.yourealwaysbe.puz.Playboard.DeleteCrossingMode;

/**
 * Async settings
 *
 * The get methods have a callback that is called after prefs read
 * off-thread.
 *
 * The live data methods can be used to observe certain settings.
 *
 * Use Hilt dependency injection
 */
@Singleton
public class ForkyzSettings {
    private static ForkyzSettings instance = null;

    private static final String PREF_APP_ORIENTATION_LOCK = "orientationLock";
    public static final String PREF_APP_THEME = "applicationTheme";
    private static final String PREF_APP_THEME_LEGACY_USE_DYNAMIC
        = "useDynamicColors";
    private static final String PREF_APP_DAY_NIGHT_MODE = "uiTheme";

    // public for settings page
    public static final String PREF_AUTO_DOWNLOADERS = "autoDownloaders";

    private static final String PREF_BROWSE_ALWAYS_SHOW_RATING
        = "browseAlwaysShowRating";
    private static final String PREF_BROWSE_CLEANUP_AGE = "cleanupAge";
    private static final String PREF_BROWSE_CLEANUP_AGE_ARCHIVE
        = "archiveCleanupAge";
    private static final String PREF_BROWSE_DELETE_ON_CLEANUP
        = "deleteOnCleanup";
    private static final String PREF_BROWSE_DISABLE_SWIPE = "disableSwipe";
    private static final String PREF_BROWSE_INDICATE_IF_SOLUTION
        = "browseIndicateIfSolution";
    private static final String PREF_BROWSE_LAST_DOWNLOAD = "dlLast";
    private static final String PREF_BROWSE_LAST_SEEN_VERSION
        = "lastSeenVersion";
    private static final String PREF_BROWSE_NEW_PUZZLE = "browseNewPuzzle";
    private static final String PREF_BROWSE_SHOW_PERCENTAGE_CORRECT
        = "browseShowPercentageCorrect";
    private static final String PREF_BROWSE_SORT = "sort";
    private static final String PREF_BROWSE_SWIPE_ACTION = "swipeAction";

    private static final String PREF_EXT_CHAT_GPT_API_KEY = "chatGPTAPIKey";
    private static final String PREF_EXT_CROSSWORD_SOLVER_ENABLED
        = "crosswordSolverEnabled";
    private static final String PREF_EXT_DICTIONARY = "externalDictionary";

    private static final String PREF_CLUE_LIST_CLUE_TABS_DOUBLE
        = "clueTabsDouble";
    private static final String PREF_CLUE_LIST_SHOW_WORDS
        = "showWordsInClueList";
    private static final String PREF_CLUE_LIST_SNAP_TO_CLUE = "snapClue";

    private static final String PREF_DOWNLOAD_LEGACY_BACKGROUND
        = "backgroundDownload";
    private static final String PREF_DOWNLOAD_TIMEOUT = "downloadTimeout";
    private static final String PREF_DOWNLOAD_TIMEOUT_DEFAULT = "30000";
    private static final String PREF_DOWNLOAD_UNMETERED
        = "backgroundDownloadRequireUnmetered";
    private static final String PREF_DOWNLOAD_ROAMING
        = "backgroundDownloadAllowRoaming";
    private static final String PREF_DOWNLOAD_CHARGING
        = "backgroundDownloadRequireCharging";
    private static final String PREF_DOWNLOAD_HOURLY
        = "backgroundDownloadHourly";
    private static final String PREF_DOWNLOAD_DAYS
        = "backgroundDownloadDays";
    private static final String PREF_DOWNLOAD_DAYS_TIME
        = "backgroundDownloadDaysTime";

    private static final String PREF_FILE_HANDLER_SAF_ARCHIVE
        = "safArchiveFolderUri";
    private static final String PREF_FILE_HANDLER_SAF_CROSSWORDS
        = "safCrosswordsFolderUri";
    private static final String PREF_FILE_HANDLER_SAF_ROOT
        = "safRootUri";
    private static final String PREF_FILE_HANDLER_SAF_TO_IMPORT
        = "safToImportFolderUri";
    private static final String PREF_FILE_HANDLER_SAF_TO_IMPORT_DONE
        = "safToImportDoneFolderUri";
    private static final String PREF_FILE_HANDLER_SAF_TO_IMPORT_FAILED
        = "safToImportFailedFolderUri";
    public static final String PREF_FILE_HANDLER_STORAGE_LOC
        = "storageLocation";

    private static final String PREF_KEYBOARD_COMPACT = "keyboardCompact";
    private static final String PREF_KEYBOARD_HAPTIC = "keyboardHaptic";
    private static final String PREF_KEYBOARD_HIDE_BUTTON
        = "keyboardHideButton";
    private static final String PREF_KEYBOARD_LAYOUT = "keyboardLayout";
    private static final String PREF_KEYBOARD_MODE = "keyboardShowHide";
    private static final String PREF_KEYBOARD_NATIVE = "useNativeKeyboard";
    private static final String PREF_KEYBOARD_REPEAT_DELAY
        = "keyboardRepeatDelay";
    private static final String PREF_KEYBOARD_REPEAT_INTERVAL
        = "keyboardRepeatInterval";

    private static final String PREF_PLAY_CLUE_BELOW_GRID
        = "clueBelowGrid";
    private static final String[] PREF_PLAY_CLUE_TABS_PAGES
        = { "playActivityClueTabsPage", "playActivityClueTabsPage1" };
    private static final String PREF_PLAY_DELETE_CROSSING_MODE
        = "deleteCrossingMode";
    private static final String PREF_PLAY_DOUBLE_TAP_FIT_BOARD
        = "doubleTap";
    private static final String PREF_PLAY_ENSURE_VISIBLE = "ensureVisible";
    private static final String PREF_PLAY_ENTER_CHANGES_DIRECTION
        = "enterChangesDirection";
    private static final String PREF_PLAY_FIT_TO_SCREEN = "fitToScreen";
    private static final String PREF_PLAY_FULL_SCREEN = "fullScreen";
    private static final String PREF_PLAY_GRID_RATIO_LANDSCAPE
        = "gridRatioLand";
    private static final String PREF_PLAY_GRID_RATIO_PORTRAIT = "gridRatio";
    private static final String PREF_PLAY_LEGACY_DONT_DELETE_CROSSING
        = "dontDeleteCrossing";
    private static final String PREF_PLAY_MOVEMENT_STRATEGY
        = "movementStrategy";
    private static final String PREF_PLAY_PLAY_LETTER_UNDO_ENABLED
        = "playLetterUndoEnabled";
    private static final String PREF_PLAY_PREDICT_ANAGRAM_CHARS
        = "predictAnagramChars";
    private static final String
    PREF_PLAY_PRESERVE_CORRECT_LETTERS_IN_SHOW_ERRORS
        = "preserveCorrectLettersInShowErrors";
    private static final String PREF_PLAY_RANDOM_CLUE_ON_SHAKE
        = "randomClueOnShake";
    private static final String PREF_PLAY_SCALE = "scale";
    private static final String PREF_PLAY_SCRATCH_DISPLAY = "displayScratch";
    private static final String PREF_PLAY_SCRATCH_MODE = "scratchMode";
    private static final String PREF_PLAY_SHOW_COUNT = "showCount";
    private static final String PREF_PLAY_SHOW_CLUES_TAB = "showCluesOnPlayScreen";
    private static final String PREF_PLAY_SHOW_ERRORS_CLUE = "showErrorsClue";
    private static final String PREF_PLAY_SHOW_ERRORS_CURSOR = "showErrorsCursor";
    private static final String PREF_PLAY_SHOW_ERRORS_GRID = "showErrors";
    private static final String PREF_PLAY_SHOW_TIMER = "showTimer";
    private static final String PREF_PLAY_SKIP_FILLED = "skipFilled";
    private static final String PREF_PLAY_CYCLE_UNFILLED_LEGACY
        = "cycleUnfilled";
    private static final String PREF_PLAY_CYCLE_UNFILLED_MODE
        = "cycleUnfilledMode";
    private static final String PREF_PLAY_SPACE_CHANGE_DIRECTION
        = "spaceChangesDirection";
    private static final String PREF_PLAY_SUPPRESS_HINT_HIGHLIGHTING
        = "supressHints";
    private static final String PREF_PLAY_INDICATE_SHOW_ERRORS
        = "indicateShowErrors";

    private static final String PREF_RATINGS_DISABLE_RATINGS
        = "disableRatings";

    private static final String PREF_VOICE_ALWAYS_ANNOUNCE_BOX
        = "alwaysAnnounceBox";
    private static final String PREF_VOICE_ALWAYS_ANNOUNCE_CLUE
        = "alwaysAnnounceClue";
    private static final String PREF_VOICE_BUTTON_ACTIVATES_VOICE
        = "buttonActivatesVoice";
    private static final String PREF_VOICE_BUTTON_ANNOUNCE_CLUE
        = "buttonAnnounceClue";
    private static final String PREF_VOICE_EQUALS_ANNOUNCE_CLUE
        = "equalsAnnounceClue";
    private static final String PREF_VOICE_VOLUME_ACTIVATES_VOICE
        = "volumeActivatesVoice";

    private static final String PREF_SUPPRESS_SUMMARY_NOTIFICATIONS
        = "supressSummaryMessages";
    private static final String PREF_SUPPRESS_INDIVIDUAL_NOTIFICATIONS
        = "supressMessages";

    private static final Set<String> BACKGROUND_DOWNLOAD_PREFERENCES
        = new HashSet<>(
            Arrays.asList(
                PREF_DOWNLOAD_UNMETERED,
                PREF_DOWNLOAD_ROAMING,
                PREF_DOWNLOAD_CHARGING,
                PREF_DOWNLOAD_HOURLY,
                PREF_DOWNLOAD_DAYS,
                PREF_DOWNLOAD_DAYS_TIME
            )
        );

    private static final Map<String, Boolean> BOOL_DEFAULTS = new HashMap<>();
    private static final Map<String, Float> FLOAT_DEFAULTS = new HashMap<>();
    private static final Map<String, Integer> INT_DEFAULTS = new HashMap<>();
    private static final Map<String, Long> LONG_DEFAULTS = new HashMap<>();
    private static final Map<String, String> STRING_DEFAULTS = new HashMap<>();
    static {
        BOOL_DEFAULTS.put(PREF_BROWSE_ALWAYS_SHOW_RATING, false);
        STRING_DEFAULTS.put(PREF_BROWSE_CLEANUP_AGE, "-1");
        STRING_DEFAULTS.put(PREF_BROWSE_CLEANUP_AGE_ARCHIVE, "-1");
        BOOL_DEFAULTS.put(PREF_BROWSE_DELETE_ON_CLEANUP, false);
        BOOL_DEFAULTS.put(PREF_BROWSE_DISABLE_SWIPE, false);
        BOOL_DEFAULTS.put(PREF_BROWSE_INDICATE_IF_SOLUTION, false);
        LONG_DEFAULTS.put(PREF_BROWSE_LAST_DOWNLOAD, 0L);
        STRING_DEFAULTS.put(PREF_BROWSE_LAST_SEEN_VERSION, "");
        BOOL_DEFAULTS.put(PREF_BROWSE_NEW_PUZZLE, false);
        BOOL_DEFAULTS.put(PREF_BROWSE_SHOW_PERCENTAGE_CORRECT, false);
        INT_DEFAULTS.put(PREF_BROWSE_SORT, 0);
        BOOL_DEFAULTS.put(PREF_CLUE_LIST_SHOW_WORDS, false);
        BOOL_DEFAULTS.put(PREF_CLUE_LIST_SNAP_TO_CLUE, false);
        STRING_DEFAULTS.put(
            PREF_DOWNLOAD_TIMEOUT, PREF_DOWNLOAD_TIMEOUT_DEFAULT
        );
        STRING_DEFAULTS.put(PREF_EXT_CHAT_GPT_API_KEY, "");
        BOOL_DEFAULTS.put(PREF_EXT_CROSSWORD_SOLVER_ENABLED, true);
        BOOL_DEFAULTS.put(PREF_PLAY_CLUE_BELOW_GRID, false);
        for (int i = 0; i < PREF_PLAY_CLUE_TABS_PAGES.length; i++)
            INT_DEFAULTS.put(PREF_PLAY_CLUE_TABS_PAGES[i], i);
        BOOL_DEFAULTS.put(PREF_PLAY_DOUBLE_TAP_FIT_BOARD, false);
        BOOL_DEFAULTS.put(PREF_PLAY_ENSURE_VISIBLE, true);
        BOOL_DEFAULTS.put(PREF_PLAY_ENTER_CHANGES_DIRECTION, true);
        BOOL_DEFAULTS.put(PREF_PLAY_FIT_TO_SCREEN, false);
        BOOL_DEFAULTS.put(PREF_PLAY_FULL_SCREEN, false);
        BOOL_DEFAULTS.put(PREF_PLAY_PREDICT_ANAGRAM_CHARS, true);
        BOOL_DEFAULTS.put(
            PREF_PLAY_PRESERVE_CORRECT_LETTERS_IN_SHOW_ERRORS, false
        );
        BOOL_DEFAULTS.put(PREF_PLAY_RANDOM_CLUE_ON_SHAKE, false);
        FLOAT_DEFAULTS.put(PREF_PLAY_SCALE, 1.0F);
        BOOL_DEFAULTS.put(PREF_PLAY_SCRATCH_MODE, false);
        BOOL_DEFAULTS.put(PREF_PLAY_SCRATCH_DISPLAY, false);
        BOOL_DEFAULTS.put(PREF_PLAY_SHOW_COUNT, false);
        BOOL_DEFAULTS.put(PREF_PLAY_SHOW_CLUES_TAB, true);
        BOOL_DEFAULTS.put(PREF_PLAY_SHOW_ERRORS_CLUE, false);
        BOOL_DEFAULTS.put(PREF_PLAY_SHOW_ERRORS_CURSOR, false);
        BOOL_DEFAULTS.put(PREF_PLAY_SHOW_ERRORS_GRID, false);
        BOOL_DEFAULTS.put(PREF_PLAY_SHOW_TIMER, false);
        BOOL_DEFAULTS.put(PREF_PLAY_SKIP_FILLED, false);
        BOOL_DEFAULTS.put(PREF_PLAY_PLAY_LETTER_UNDO_ENABLED, false);
        BOOL_DEFAULTS.put(PREF_PLAY_SPACE_CHANGE_DIRECTION, true);
        BOOL_DEFAULTS.put(PREF_PLAY_SUPPRESS_HINT_HIGHLIGHTING, false);
        BOOL_DEFAULTS.put(PREF_PLAY_INDICATE_SHOW_ERRORS, true);
        BOOL_DEFAULTS.put(PREF_RATINGS_DISABLE_RATINGS, false);
        BOOL_DEFAULTS.put(PREF_VOICE_ALWAYS_ANNOUNCE_BOX, false);
        BOOL_DEFAULTS.put(PREF_VOICE_ALWAYS_ANNOUNCE_CLUE, false);
        BOOL_DEFAULTS.put(PREF_VOICE_BUTTON_ACTIVATES_VOICE, false);
        BOOL_DEFAULTS.put(PREF_VOICE_BUTTON_ANNOUNCE_CLUE, false);
        BOOL_DEFAULTS.put(PREF_VOICE_EQUALS_ANNOUNCE_CLUE, true);
        BOOL_DEFAULTS.put(PREF_VOICE_VOLUME_ACTIVATES_VOICE, false);
    }

    private SharedPreferences prefs;
    private Handler handler;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private MutableLiveData<Accessor> liveBrowseSort;
    private MutableLiveData<ClueTabsDouble> liveClueListClueTabsDouble;
    private MutableLiveData<KeyboardSettings> liveKeyboardSettings;
    private MutableLiveData<GridRatio> livePlayGridRatioLandscape;
    private MutableLiveData<GridRatio> livePlayGridRatioPortrait;
    private MutableLiveData<DeleteCrossingMode> livePlayDeleteCrossingMode;
    private MutableLiveData<RatingsSettings> liveRatingsSettings;
    private Map<String, MutableLiveData<Boolean>> liveBooleans = new HashMap<>();

    private OnSharedPreferenceChangeListener prefChangeListener
        = (prefs, key) -> {
            if (PREF_BROWSE_SORT.equals(key)) {
                updateLiveBrowseSort();
            } else if (PREF_CLUE_LIST_CLUE_TABS_DOUBLE.equals(key)) {
                updateLiveClueListClueTabsDouble();
            } else if (
                PREF_KEYBOARD_COMPACT.equals(key)
                || PREF_KEYBOARD_HAPTIC.equals(key)
                || PREF_KEYBOARD_HIDE_BUTTON.equals(key)
                || PREF_KEYBOARD_LAYOUT.equals(key)
                || PREF_KEYBOARD_MODE.equals(key)
                || PREF_KEYBOARD_NATIVE.equals(key)
                || PREF_KEYBOARD_REPEAT_DELAY.equals(key)
                || PREF_KEYBOARD_REPEAT_INTERVAL.equals(key)
            ) {
                updateLiveKeyboardSettings();
            } else if (PREF_PLAY_DELETE_CROSSING_MODE.equals(key)) {
                updateLivePlayDeleteCrossingMode();
            } else if (PREF_PLAY_GRID_RATIO_LANDSCAPE.equals(key)) {
                updateLivePlayGridRatioLandscape();
            } else if (PREF_PLAY_GRID_RATIO_PORTRAIT.equals(key)) {
                updateLivePlayGridRatioPortrait();
            } else if (
                PREF_BROWSE_ALWAYS_SHOW_RATING.equals(key)
                || PREF_RATINGS_DISABLE_RATINGS.equals(key)
            ) {
                updateLiveRatingsSettings();
            } else if (liveBooleans.containsKey(key)) {
                updateLiveBoolean(key);
            }
        };

    @Inject
    ForkyzSettings(@ApplicationContext Context context) {
        this(
            PreferenceManager.getDefaultSharedPreferences(context),
            new Handler(context.getMainLooper())
        );
    }

    /**
     * Package level for testing only
     */
    ForkyzSettings(SharedPreferences prefs, Handler handler) {
        this.prefs = prefs;
        this.handler = handler;
        this.prefs.registerOnSharedPreferenceChangeListener(prefChangeListener);
    }

    public void getAppOrientationLock(Consumer<Orientation> cb) {
        executor.execute(() -> {
            String value = prefs.getString(
                PREF_APP_ORIENTATION_LOCK,
                Orientation.UNLOCKED.getSettingsValue()
            );
            handler.post(() -> {
                cb.accept(Orientation.getFromSettingsValue(value));
            });
        });
    }

    /**
     * Whether to use day theme, night theme, or follow system
     */
    public void getAppDayNightMode(Consumer<DayNightMode> cb) {
        executor.execute(() -> {
            String mode = prefs.getString(
                PREF_APP_DAY_NIGHT_MODE,
                DayNightMode.DAY.getSettingsValue()
            );
            handler.post(() -> {
                cb.accept(DayNightMode.getFromSettingsValue(mode));
            });
        });
    }

    public void setAppDayNightMode(DayNightMode mode, Runnable cb) {
        executor.execute(() -> {
            prefs.edit()
                .putString(PREF_APP_DAY_NIGHT_MODE, mode.getSettingsValue())
                .apply();
            handler.post(cb);
        });
    }

    /**
     * The theme selected by user
     */
    public void getAppTheme(Consumer<Theme> cb) {
        executor.execute(() -> {
            String theme = prefs.getString(
                PREF_APP_THEME, Theme.STANDARD.getSettingsValue()
            );
            handler.post(() -> {
                cb.accept(Theme.getFromSettingsValue(theme));
            });
        });
    }

    /**
     * Blocking version of getting theme
     *
     * Needed to apply dynamic colors to application
     */
    public Theme getAppThemeBlocking() {
        String theme = prefs.getString(
            PREF_APP_THEME, Theme.STANDARD.getSettingsValue()
        );
        return Theme.getFromSettingsValue(theme);
    }

    /**
     * Age of a puzzle in the crosswords list to be removed in cleanup
     */
    public void getBrowseCleanupAge(Consumer<String> cb) {
        getStringPreference(PREF_BROWSE_CLEANUP_AGE, cb);
    }

    /**
     * Age of a puzzle in the archive list to be removed in cleanup
     */
    public void getBrowseCleanupAgeArchive(Consumer<String> cb) {
        getStringPreference(PREF_BROWSE_CLEANUP_AGE_ARCHIVE, cb);
    }

    /**
     * Delete on clean up crosswords list, rather than send to archive
     */
    public void getBrowseDeleteOnCleanup(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_BROWSE_DELETE_ON_CLEANUP, cb);
    }

    /**
     * Disable the swipe action in the browse list
     */
    public void getBrowseDisableSwipe(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_BROWSE_DISABLE_SWIPE, cb);
    }

    public LiveData<Boolean> liveBrowseDisableSwipe() {
        return liveBooleanPreference(PREF_BROWSE_DISABLE_SWIPE);
    }

    /**
     * Whether to indicate in browse if puzzle has answers in data
     */
    public void getBrowseIndicateIfSolution(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_BROWSE_INDICATE_IF_SOLUTION, cb);
    }

    public LiveData<Boolean> liveBrowseIndicateIfSolution() {
        return liveBooleanPreference(PREF_BROWSE_INDICATE_IF_SOLUTION);
    }

    /**
     * The last time a download was triggered when browse started
     *
     * Used to trigger automatic downloads on app start if a download
     * didn't happen recently.
     *
     * Runs sychronously, avoid usage on main thread.
     */
    public long getBrowseLastDownloadSync() {
        return getLongPreferenceSync(PREF_BROWSE_LAST_DOWNLOAD);
    }

    /**
     * Set last time download was triggered by browse
     *
     * Runs synchronously, avoid on main thread
     */
    public void setBrowseLastDownloadSync(long value) {
        setLongPreferenceSync(PREF_BROWSE_LAST_DOWNLOAD, value);
    }

    /**
     * The last version of Forkyz seen on start
     *
     * Used to display a welcome on new Forkyz versions
     */
    public void getBrowseLastSeenVersion(Consumer<String> cb) {
        getStringPreference(PREF_BROWSE_LAST_SEEN_VERSION, cb);
    }

    public void setBrowseLastSeenVersion(String value) {
        setStringPreference(PREF_BROWSE_LAST_SEEN_VERSION, value);
    }

    /**
     * BrowseActivity checks this on resume to know if to refresh list
     *
     * I.e. true if a puzzle added to puzzles without browse knowing otherwise.
     */
    public void getBrowseNewPuzzle(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_BROWSE_NEW_PUZZLE, cb);
    }

    public void setBrowseNewPuzzle(boolean pending) {
        setBooleanPreference(PREF_BROWSE_NEW_PUZZLE, pending);
    }

    public LiveData<Boolean> liveBrowseShowPercentageCorrect() {
        return liveBooleanPreference(PREF_BROWSE_SHOW_PERCENTAGE_CORRECT);
    }

    /**
     * How to order the puzzles in the browse list
     */
    public void getBrowseSort(Consumer<Accessor> cb) {
        getIntPreference(PREF_BROWSE_SORT, value -> {
            switch (value) {
            case 2: cb.accept(Accessor.SOURCE); break;
            case 1: cb.accept(Accessor.DATE_ASC); break;
            default: cb.accept(Accessor.DATE_DESC); break;
            }
        });
    }

    public LiveData<Accessor> liveBrowseSort() {
        if (liveBrowseSort == null) {
            liveBrowseSort = new MutableLiveData<Accessor>();
            updateLiveBrowseSort();
        }
        return liveBrowseSort;
    }

    public void setBrowseSort(Accessor value) {
        int intValue;
        if (value == Accessor.SOURCE)
            intValue = 2;
        else if (value == Accessor.DATE_ASC)
            intValue = 1;
        else /* DATE_DESC */
            intValue = 0;

        setIntPreference(PREF_BROWSE_SORT, intValue);
    }

    /**
     * Whether to delete or archve on swipe
     */
    public void getBrowseSwipeAction(Consumer<BrowseSwipeAction> cb) {
        executor.execute(() -> {
            String value = prefs.getString(PREF_BROWSE_SWIPE_ACTION, "DELETE");
            handler.post(() -> {
                cb.accept(BrowseSwipeAction.getFromSettingsValue(value));
            });
        });
    }

    /**
     * The settings for background downloads
     *
     * Things like whether to download every hour, or what day/time to
     * download. Plus conditions under which download should fire.
     */
    public void getBackgroundDownloadSettings(
        Consumer<BackgroundDownloadSettings> cb
    ) {
        executor.execute(() -> {
            int timeOfDay = 8;
            try {
                timeOfDay = Integer.valueOf(
                    prefs.getString(PREF_DOWNLOAD_DAYS_TIME, "8")
                );
            } catch (NumberFormatException e) {
                // ignore, keep 8
            }

            final int finalTimeOfDay = timeOfDay;

            handler.post(() -> {
                cb.accept(new BackgroundDownloadSettings(
                    prefs.getBoolean(PREF_DOWNLOAD_UNMETERED, true),
                    prefs.getBoolean(PREF_DOWNLOAD_ROAMING, false),
                    prefs.getBoolean(PREF_DOWNLOAD_CHARGING, false),
                    prefs.getBoolean(PREF_DOWNLOAD_HOURLY, false),
                    prefs.getStringSet(PREF_DOWNLOAD_DAYS, Collections.emptySet()),
                    finalTimeOfDay
                ));
            });
        });
    }

    /**
     * Set whether to background download every hour
     */
    public void setBackgroundDownloadHourly(
        boolean hourly, Runnable cb
    ) {
        setBooleanPreference(PREF_DOWNLOAD_HOURLY, hourly, cb);
    }

    /**
     * If the preference key is a background download key
     *
     * For preference activity to detect if config changes
     */
    public boolean isBackgroundDownloadConfigPref(String pref) {
        return BACKGROUND_DOWNLOAD_PREFERENCES.contains(pref);
    }

    /**
     * When to double up the clue tabs display
     */
    public void getClueListClueTabsDouble(Consumer<ClueTabsDouble> cb) {
        executor.execute(() -> {
            String value = prefs.getString(
                PREF_CLUE_LIST_CLUE_TABS_DOUBLE, "NEVER");
            handler.post(() -> {
                cb.accept(ClueTabsDouble.getFromSettingsValue(value));
            });
        });
    }

    public LiveData<ClueTabsDouble> liveClueListClueTabsDouble() {
        if (liveClueListClueTabsDouble == null) {
            liveClueListClueTabsDouble = new MutableLiveData<ClueTabsDouble>();
            updateLiveClueListClueTabsDouble();
        }
        return liveClueListClueTabsDouble;
    }

    /**
     * Whether to show the board for each clue in the clue list
     *
     * Or just the currently selected clue.
     */
    public void getClueListShowWords(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_CLUE_LIST_SHOW_WORDS, cb);
    }

    /**
     * Whether to make sure current clue always visible in clue tabs
     */
    public void getClueListSnapToClue(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_CLUE_LIST_SNAP_TO_CLUE, cb);
    }


    public LiveData<Boolean> liveClueListShowWords() {
        return liveBooleanPreference(PREF_CLUE_LIST_SHOW_WORDS);
    }

    public void setClueListShowWords(boolean value) {
        setBooleanPreference(PREF_CLUE_LIST_SHOW_WORDS, value);
    }

    /**
     * The API key for ask chat gpt for help if set
     */
    public void getExtChatGPTAPIKey(Consumer<String> cb) {
        getStringPreference(PREF_EXT_CHAT_GPT_API_KEY, cb);
    }

    public void getExtCrosswordSolverEnabled(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_EXT_CROSSWORD_SOLVER_ENABLED, cb);
    }

    public void getExtDictionary(Consumer<ExternalDictionary> cb) {
        executor.execute(() -> {
            String value = prefs.getString(PREF_EXT_DICTIONARY, "FREE");
            handler.post(() -> {
                if ("NONE".equals(value))
                    cb.accept(null);
                else if ("QUICK".equals(value))
                    cb.accept(ExternalDictionary.QUICK_DIC);
                else if ("AARD2".equals(value))
                    cb.accept(ExternalDictionary.AARD2);
                else
                    cb.accept(ExternalDictionary.FREE_DICTIONARY);
            });
        });
    }

    /**
     * Gettings the preferences for file handling
     *
     * E.g. whether to use internal or custom storage, and where the
     * custom storage is.
     */
    public void getFileHandlerSettings(Consumer<FileHandlerSettings> cb) {
        executor.execute(() -> {
            String storageLocationString = prefs.getString(
                PREF_FILE_HANDLER_STORAGE_LOC,
                StorageLocation.INTERNAL.getSettingsValue()
            );
            String safRoot = prefs.getString(
                PREF_FILE_HANDLER_SAF_ROOT, null
            );
            String safCrosswords = prefs.getString(
                PREF_FILE_HANDLER_SAF_CROSSWORDS, null
            );
            String safArchive = prefs.getString(
                PREF_FILE_HANDLER_SAF_ARCHIVE, null
            );
            String safToImport = prefs.getString(
                PREF_FILE_HANDLER_SAF_TO_IMPORT, null
            );
            String safToImportDone = prefs.getString(
                PREF_FILE_HANDLER_SAF_TO_IMPORT_DONE, null
            );
            String safToImportFailed = prefs.getString(
                PREF_FILE_HANDLER_SAF_TO_IMPORT_FAILED, null
            );

            handler.post(() -> {
                cb.accept(new FileHandlerSettings(
                    StorageLocation.fromSettingsValue(storageLocationString),
                    safRoot,
                    safCrosswords,
                    safArchive,
                    safToImport,
                    safToImportDone,
                    safToImportFailed
                ));
            });
        });
    }

    /**
     * Set file handler and call back when done
     */
    public void setFileHandlerSettings(
        FileHandlerSettings settings, Runnable cb
    ) {
        executor.execute(() -> {
            prefs.edit()
                .putString(
                    PREF_FILE_HANDLER_STORAGE_LOC,
                    settings.storageLocation().getSettingsValue()
                ).putString(
                    PREF_FILE_HANDLER_SAF_ROOT,
                    settings.safRootURI()
                ).putString(
                    PREF_FILE_HANDLER_SAF_CROSSWORDS,
                    settings.safCrosswordsURI()
                ).putString(
                    PREF_FILE_HANDLER_SAF_ARCHIVE,
                    settings.safArchiveURI()
                ).apply();
            if (cb != null)
                handler.post(() -> { cb.run(); });
        });
    }

    public void getKeyboardSettings(Consumer<KeyboardSettings> cb) {
        // use value cached in liveKeyboardSettings or create cache
        if (
            liveKeyboardSettings == null
            || !liveKeyboardSettings.isInitialized()
        ) {
            getFreshKeyboardSettings(ks -> {
                handler.post(() -> { cb.accept(ks); });
            });
            // create a cache for future if needed
            liveKeyboardSettings();
        } else {
            cb.accept(liveKeyboardSettings.getValue());
        }
    }

    /**
     * Keep a live view of the keyboard settings
     */
    public LiveData<KeyboardSettings> liveKeyboardSettings() {
        if (liveKeyboardSettings == null) {
            liveKeyboardSettings = new MutableLiveData<KeyboardSettings>();
            updateLiveKeyboardSettings();
        }
        return liveKeyboardSettings;
    }

    /**
     * Whether to display the currently selected clue below the grid
     *
     * Default is in the title bar
     */
    public void getPlayClueBelowGrid(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_CLUE_BELOW_GRID, cb);
    }

    /**
     * Whether to display the currently selected clue below the grid
     *
     * Default is in the title bar
     */
    public LiveData<Boolean> livePlayClueBelowGrid() {
        return liveBooleanPreference(PREF_PLAY_CLUE_BELOW_GRID);
    }

    /**
     * To remember which page of the clue list is selected in PlayActivity
     */
    public void getPlayClueTabsPage(int pageNum, Consumer<Integer> cb) {
        if (0 <= pageNum && pageNum < PREF_PLAY_CLUE_TABS_PAGES.length)
            getIntPreference(PREF_PLAY_CLUE_TABS_PAGES[pageNum], cb);
    }

    public void setPlayClueTabsPage(int pageNum, int value) {
        if (0 <= pageNum && pageNum < PREF_PLAY_CLUE_TABS_PAGES.length)
            setIntPreference(PREF_PLAY_CLUE_TABS_PAGES[pageNum], value);
    }

    /**
     * Whether to delete characters that may belong to a crossing word
     */
    public void getPlayDeleteCrossingMode(Consumer<DeleteCrossingMode> cb) {
        executor.execute(() -> {
            String modeString = prefs.getString(
                PREF_PLAY_DELETE_CROSSING_MODE, "DELETE"
            );
            handler.post(() -> {
                DeleteCrossingMode mode
                    = DeleteCrossingModeSetting.getFromSettingsValue(modeString)
                        .getDeleteCrossingMode();
                cb.accept(mode);
            });
        });
    }

    /**
     * Whether double tapping the screen fits the board to screen
     */
    public void getPlayDoubleTapFitBoard(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_DOUBLE_TAP_FIT_BOARD, cb);
    }

    /**
     * Whether to delete characters that may belong to a crossing word
     */
    public LiveData<DeleteCrossingMode> livePlayDeleteCrossingMode() {
        if (livePlayDeleteCrossingMode == null) {
            livePlayDeleteCrossingMode
                = new MutableLiveData<DeleteCrossingMode>();
            updateLivePlayDeleteCrossingMode();
        }
        return livePlayDeleteCrossingMode;
    }

    /**
     * Ensure the currently selected word/cell is visible on play board
     */
    public void getPlayEnsureVisible(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_ENSURE_VISIBLE, cb);
    }

    /**
     * Whether to change selection direction when enter pressed
     */
    public void getPlayEnterChangesDirection(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_ENTER_CHANGES_DIRECTION, cb);
    }

    /**
     * Whether to fit puzzle to screen on starting PlayActivity
     */
    public void getPlayFitToScreen(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_FIT_TO_SCREEN, cb);
    }

    /**
     * Whether to display the app in full screen mode
     *
     * I think this doesn't do much on modern phones, but it used to
     * hide the status bar at the top.
     */
    public void getPlayFullScreen(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_FULL_SCREEN, cb);
    }

    /**
     * Get grid ratio when clue tabs showing on play screen landscape mode
     */
    public void getPlayGridRatioLandscape(Consumer<GridRatio> cb) {
        executor.execute(() -> {
            String value = prefs.getString(
                PREF_PLAY_GRID_RATIO_LANDSCAPE,
                GridRatio.PUZZLE_SHAPE.getSettingsValue()
            );
            handler.post(() -> {
                cb.accept(GridRatio.getFromSettingsValue(value));
            });
        });
    }

    public LiveData<GridRatio> livePlayGridRatioLandscape() {
        if (livePlayGridRatioLandscape == null) {
            livePlayGridRatioLandscape = new MutableLiveData<GridRatio>();
            updateLivePlayGridRatioLandscape();
        }
        return livePlayGridRatioLandscape;
    }

    /**
     * Get grid ratio when clue tabs showing on play screen
     */
    public void getPlayGridRatioPortrait(Consumer<GridRatio> cb) {
        executor.execute(() -> {
            String value = prefs.getString(
                PREF_PLAY_GRID_RATIO_PORTRAIT,
                GridRatio.PUZZLE_SHAPE.getSettingsValue()
            );
            handler.post(() -> {
                cb.accept(GridRatio.getFromSettingsValue(value));
            });
        });
    }

    public LiveData<GridRatio> livePlayGridRatioPortrait() {
        if (livePlayGridRatioPortrait == null) {
            livePlayGridRatioPortrait = new MutableLiveData<GridRatio>();
            updateLivePlayGridRatioPortrait();
        }
        return livePlayGridRatioPortrait;
    }

    /**
     * Get the style of movement around the board
     */
    public void getPlayMovementStrategy(Consumer<MovementStrategy> cb) {
        executor.execute(() -> {
            String strategyString = prefs.getString(
                PREF_PLAY_MOVEMENT_STRATEGY, "MOVE_NEXT_ON_AXIS"
            );

            CycleUnfilledMode cycleUnfilledMode
                = CycleUnfilledMode.getFromSettingsValue(
                    prefs.getString(
                        PREF_PLAY_CYCLE_UNFILLED_MODE,
                        CycleUnfilledMode.NEVER.getSettingsValue()
                    )
                );

            boolean cycleForwards
                = cycleUnfilledMode != CycleUnfilledMode.NEVER;
            boolean cycleBackwards
                = cycleUnfilledMode == CycleUnfilledMode.ALWAYS;

            handler.post(() -> {
                MovementStrategy strategy
                    = MovementStrategySetting.getFromSettingsValue(
                            strategyString
                        ).getMovementStrategy();
                if (cycleForwards || cycleBackwards) {
                    strategy = new MovementStrategy.CycleUnfilled(
                        strategy, cycleForwards, cycleBackwards
                    );
                }

                cb.accept(strategy);
            });
        });
    }

    /**
     * Whether to jump to a random clue when device shaken
     */
    public void getPlayRandomClueOnShake(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_RANDOM_CLUE_ON_SHAKE, cb);
    }

    /**
     * Whether to use play letter undo stack on Playboard
     */
    public void getPlayPlayLetterUndoEnabled(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_PLAY_LETTER_UNDO_ENABLED, cb);
    }

    /**
     * Whether to try to guess the source chars of an anagram
     *
     * On notes page, predict by matching against clue
     */
    public void getPlayPredictAnagramChars(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_PREDICT_ANAGRAM_CHARS, cb);
    }

    /**
     * When errors are show, don't let correct cell entries be changed
     */
    public void getPlayPreserveCorrectLettersInShowErrors(
        Consumer<Boolean> cb
    ) {
        getBooleanPreference(
            PREF_PLAY_PRESERVE_CORRECT_LETTERS_IN_SHOW_ERRORS, cb
        );
    }

    public LiveData<Boolean> livePlayPreserveCorrectLettersInShowErrors() {
        return liveBooleanPreference(
            PREF_PLAY_PRESERVE_CORRECT_LETTERS_IN_SHOW_ERRORS
        );
    }

    /**
     * Remember the scale/size of the board in the PlayActivity
     */
    public void getPlayScale(Consumer<Float> cb) {
        getFloatPreference(PREF_PLAY_SCALE, cb);
    }

    public void setPlayScale(float value) {
        setFloatPreference(PREF_PLAY_SCALE, value);
    }

    /**
     * Whether to show scratch notes on the play board in grey letters
     */
    public void getPlayScratchDisplay(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_SCRATCH_DISPLAY, cb);
    }

    /**
     * Whether entered characters should go to scratch notes
     *
     * Instead of going to the main response for the cell, enter new
     * chars as scratch notes for the current clue.
     */
    public void getPlayScratchMode(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_SCRATCH_MODE, cb);
    }

    public LiveData<Boolean> livePlayScratchMode() {
        return liveBooleanPreference(PREF_PLAY_SCRATCH_MODE);
    }

    public void setPlayScratchMode(boolean value) {
        setBooleanPreference(PREF_PLAY_SCRATCH_MODE, value);
    }

    /**
     * Wether to show the clue solution length after the clue hint
     */
    public void getPlayShowCount(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_SHOW_COUNT, cb);
    }

    public LiveData<Boolean> livePlayShowCount() {
        return liveBooleanPreference(PREF_PLAY_SHOW_COUNT);
    }

    public void setPlayShowCount(boolean value) {
        setBooleanPreference(PREF_PLAY_SHOW_COUNT, value);
    }

    /**
     * Whether to highlight errors on completely filled clue entries
     */
    public void getPlayShowErrorsClue(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_SHOW_ERRORS_CLUE, cb);
    }

    public LiveData<Boolean> livePlayShowErrorsClue() {
        return liveBooleanPreference(PREF_PLAY_SHOW_ERRORS_CLUE);
    }

    public void setPlayShowErrorsClue(boolean value) {
        setBooleanPreference(PREF_PLAY_SHOW_ERRORS_CLUE, value);
    }

    /**
     * Whether to highlight errors in the currently selected cell
     */
    public void getPlayShowErrorsCursor(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_SHOW_ERRORS_CURSOR, cb);
    }

    public LiveData<Boolean> livePlayShowErrorsCursor() {
        return liveBooleanPreference(PREF_PLAY_SHOW_ERRORS_CURSOR);
    }

    public void setPlayShowErrorsCursor(boolean value) {
        setBooleanPreference(PREF_PLAY_SHOW_ERRORS_CURSOR, value);
    }

    /**
     * Whether to highlight errors anywhere on the grid
     */
    public void getPlayShowErrorsGrid(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_SHOW_ERRORS_GRID, cb);
    }

    public LiveData<Boolean> livePlayShowErrorsGrid() {
        return liveBooleanPreference(PREF_PLAY_SHOW_ERRORS_GRID);
    }

    public void setPlayShowErrorsGrid(boolean value) {
        setBooleanPreference(PREF_PLAY_SHOW_ERRORS_GRID, value);
    }

    public void getPlayShowTimer(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_SHOW_TIMER, cb);
    }

    public LiveData<Boolean> livePlayShowTimer() {
        return liveBooleanPreference(PREF_PLAY_SHOW_TIMER);
    }

    /**
     * Whether to skip filled cells when moving to the next
     */
    public void getPlaySkipFilled(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_SKIP_FILLED, cb);
    }

    /**
     * Whether to show the clue list on the PlayActivity
     */
    public void getPlayShowCluesTab(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_SHOW_CLUES_TAB, cb);
    }

    public void setPlayShowCluesTab(boolean value) {
        setBooleanPreference(PREF_PLAY_SHOW_CLUES_TAB, value);
    }

    /**
     * Whether pressing space changes the selection direction
     */
    public void getPlaySpaceChangesDirection(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_SPACE_CHANGE_DIRECTION, cb);
    }

    /**
     * Whether hinted squares should not be highlighted on board view
     */
    public void getPlaySuppressHintHighlighting(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_SUPPRESS_HINT_HIGHLIGHTING, cb);
    }

    /**
     * Whether to indicate show errors status (i.e. whether turned on)
     */
    public void getPlayIndicateShowErrors(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_INDICATE_SHOW_ERRORS, cb);
    }

    public LiveData<Boolean> livePlayIndicateShowErrors() {
        return liveBooleanPreference(PREF_PLAY_INDICATE_SHOW_ERRORS);
    }

    public LiveData<RatingsSettings> liveRatingsSettings() {
        if (liveRatingsSettings == null) {
            liveRatingsSettings = new MutableLiveData<RatingsSettings>();
            updateLiveRatingsSettings();
        }
        return liveRatingsSettings;

    }

    /**
     * Whether to always announce the current box
     */
    public void getVoiceAlwaysAnnounceBox(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_VOICE_ALWAYS_ANNOUNCE_BOX, cb);
    }

    /**
     * Whether to always announce the current clue
     */
    public void getVoiceAlwaysAnnounceClue(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_VOICE_ALWAYS_ANNOUNCE_CLUE, cb);
    }

    /**
     * Whether to show a button for activating voice commands
     */
    public void getVoiceButtonActivatesVoice(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_VOICE_BUTTON_ACTIVATES_VOICE, cb);
    }

    /**
     * Whether pressing volume down activates voice input
     */
    public LiveData<Boolean> liveVoiceVolumeActivatesVoice() {
        return liveBooleanPreference(PREF_VOICE_VOLUME_ACTIVATES_VOICE);
    }

    /**
     * Whether pressing = (?) announces the clue with voice
     */
    public LiveData<Boolean> liveVoiceEqualsAnnounceClue() {
        return liveBooleanPreference(PREF_VOICE_EQUALS_ANNOUNCE_CLUE);
    }

    /**
     * Whether to show a button for reading out clue
     */
    public void getVoiceButtonAnnounceClue(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_VOICE_BUTTON_ANNOUNCE_CLUE, cb);
    }

    /**
     * The settings for downloaders
     *
     * I.e. which ones to use, which ones are automatic, how long before
     * timeout, notifications.
     */
    public void getDownloadersSettings(Consumer<DownloadersSettings> cb) {
        executor.execute(() -> {
            DownloadersSettings settings = new DownloadersSettings(
                prefs.getBoolean("downloadDeStandaard", true),
                prefs.getBoolean("downloadDeTelegraaf", true),
                prefs.getBoolean("downloadGuardianDailyCryptic", true),
                prefs.getBoolean("downloadHamAbend", true),
                prefs.getBoolean("downloadIndependentDailyCryptic", true),
                prefs.getBoolean("downloadJonesin", true),
                prefs.getBoolean("downloadJoseph", true),
                prefs.getBoolean("download20Minutes", true),
                prefs.getBoolean("downloadLeParisienF1", true),
                prefs.getBoolean("downloadLeParisienF2", true),
                prefs.getBoolean("downloadLeParisienF3", true),
                prefs.getBoolean("downloadLeParisienF4", true),
                prefs.getBoolean("downloadMetroCryptic", true),
                prefs.getBoolean("downloadNewsday", true),
                prefs.getBoolean("downloadNewYorkTimesSyndicated", true),
                prefs.getBoolean("downloadPremier", true),
                prefs.getBoolean("downloadSheffer", true),
                prefs.getBoolean("downloadUniversal", true),
                prefs.getBoolean("downloadUSAToday", true),
                prefs.getBoolean("downloadWaPoSunday", true),
                prefs.getBoolean("downloadWsj", true),
                prefs.getBoolean("scrapeCru", false),
                prefs.getBoolean("scrapeKegler", false),
                prefs.getBoolean("scrapePrivateEye", false),
                prefs.getBoolean("scrapePrzekroj", false),
                prefs.getBoolean("downloadCustomDaily", false),
                prefs.getString("customDailyTitle", ""),
                prefs.getString("customDailyUrl", ""),
                prefs.getBoolean(
                    PREF_SUPPRESS_SUMMARY_NOTIFICATIONS, false
                ),
                prefs.getBoolean(
                    PREF_SUPPRESS_INDIVIDUAL_NOTIFICATIONS, false
                ),
                prefs.getStringSet(
                    PREF_AUTO_DOWNLOADERS, Collections.emptySet()
                ),
                Integer.valueOf(prefs.getString(
                    PREF_DOWNLOAD_TIMEOUT, PREF_DOWNLOAD_TIMEOUT_DEFAULT
                )),
                prefs.getBoolean("dlOnStartup", false)
            );
            handler.post(() -> { cb.accept(settings); });
        });
    }

    public void getDownloadTimeout(Consumer<Integer> cb) {
        getStringPreference(PREF_DOWNLOAD_TIMEOUT, value -> {
            cb.accept(Integer.valueOf(value));
        });
    }

    /**
     * Handle introduction of second selection of auto downloaders
     */
    public void migrateAutoDownloaders(Downloaders downloaders) {
        executor.execute(() -> {
            Set<String> autoDownloaders
                = prefs.getStringSet(PREF_AUTO_DOWNLOADERS, null);

            if (autoDownloaders == null) {
                autoDownloaders = new HashSet<>();

                for (Downloader downloader : downloaders.getDownloaders()) {
                    autoDownloaders.add(downloader.getInternalName());
                }

                prefs.edit()
                    .putStringSet(PREF_AUTO_DOWNLOADERS, autoDownloaders)
                    .apply();
            }
        });
    }

    /**
     * Migrate from old way of enabling hourly downloads to new
     *
     * Calls back with legacy download value -- true if legacy downloads
     * were being used.
     */
    public void migrateLegacyBackgroundDownloads(Consumer<Boolean> cb) {
        executor.execute(() -> {
            boolean legacyEnabled
                = prefs.getBoolean(PREF_DOWNLOAD_LEGACY_BACKGROUND, false);

            if (legacyEnabled) {
                // clear old
                prefs.edit()
                    .remove(PREF_DOWNLOAD_LEGACY_BACKGROUND)
                    .apply();
            }

            handler.post(() -> { cb.accept(legacyEnabled); });
        });
    }

    /**
     * Migrate from legacy theme option
     *
     * Used to be two options, now three
     */
    public void migrateThemePreferences() {
        migratePrefBooleanToString(
            PREF_APP_THEME_LEGACY_USE_DYNAMIC,
            PREF_APP_THEME,
            Theme.DYNAMIC.getSettingsValue(),
            Theme.STANDARD.getSettingsValue()
        );
    }

    /**
     * Migrate from boolean dontDeleteCrossing to DeleteCrossingMode
     */
    public void migrateDontDeleteCrossing() {
        migratePrefBooleanToString(
            PREF_PLAY_LEGACY_DONT_DELETE_CROSSING,
            PREF_PLAY_DELETE_CROSSING_MODE,
            DeleteCrossingModeSetting.PRESERVE_FILLED_CELLS.getSettingsValue(),
            DeleteCrossingModeSetting.DELETE.getSettingsValue()
        );
    }

    /**
     * Migrate from boolean cycleUnfilled to cycleUnfilledMode
     */
    public void migrateCycleUnfilled() {
        migratePrefBooleanToString(
            PREF_PLAY_CYCLE_UNFILLED_LEGACY,
            PREF_PLAY_CYCLE_UNFILLED_MODE,
            CycleUnfilledMode.ALWAYS.getSettingsValue(),
            CycleUnfilledMode.NEVER.getSettingsValue()
        );
    }

    /**
     * Suppress app notifications by settings relevant settings to false
     */
    public void disableNotifications() {
        executor.execute(() -> {
            prefs.edit()
                .putBoolean(PREF_SUPPRESS_SUMMARY_NOTIFICATIONS, true)
                .putBoolean(PREF_SUPPRESS_INDIVIDUAL_NOTIFICATIONS, true)
                .apply();
        });
    }

    private void getBooleanPreference(
        String name, Consumer<Boolean> cb
    ) {
        final Boolean defaultValue = BOOL_DEFAULTS.get(name);
        if (defaultValue == null) {
            throw new IllegalArgumentException(
                "Unrecognised Boolean preference: " + name
            );
        }

        executor.execute(() -> {
            boolean value = prefs.getBoolean(name, defaultValue);
            handler.post(() -> { cb.accept(value); });
        });
    }

    private void setBooleanPreference(String name, boolean value) {
        setBooleanPreference(name, value, null);
    }

    private void setBooleanPreference(
        String name, boolean value, Runnable cb
    ) {
        executor.execute(() -> {
            prefs.edit().putBoolean(name, value).apply();
            if (cb != null)
                handler.post(cb);
        });
    }

    private void getFloatPreference(String name, Consumer<Float> cb) {
        final Float defaultValue = FLOAT_DEFAULTS.get(name);
        if (defaultValue == null) {
            throw new IllegalArgumentException(
                "Unrecognised Float preference: " + name
            );
        }

        executor.execute(() -> {
            float value = prefs.getFloat(name, defaultValue);
            handler.post(() -> { cb.accept(value); });
        });
    }

    private void setFloatPreference(String name, float value) {
        executor.execute(() -> {
            prefs.edit().putFloat(name, value).apply();
        });
    }

    private void getIntPreference(String name, Consumer<Integer> cb) {
        final Integer defaultValue = INT_DEFAULTS.get(name);
        if (defaultValue == null) {
            throw new IllegalArgumentException(
                "Unrecognised Integer preference: " + name
            );
        }

        executor.execute(() -> {
            int value = prefs.getInt(name, defaultValue);
            handler.post(() -> { cb.accept(value); });
        });
    }

    private void setIntPreference(String name, int value) {
        executor.execute(() -> {
            prefs.edit().putInt(name, value).apply();
        });
    }

    private void getLongPreference(String name, Consumer<Long> cb) {
        final Long defaultValue = getLongDefaultValue(name);
        executor.execute(() -> {
            long value = prefs.getLong(name, defaultValue);
            handler.post(() -> { cb.accept(value); });
        });
    }

    private void setLongPreference(String name, long value) {
        executor.execute(() -> {
            setLongPreferenceSync(name, value);
        });
    }

    /**
     * Get pref synchronously, avoid on main thread
     */
    private long getLongPreferenceSync(String name) {
        return prefs.getLong(name, getLongDefaultValue(name));
    }

    /**
     * Set pref synchronously, avoid on main thread
     */
    private void setLongPreferenceSync(String name, long value) {
        prefs.edit().putLong(name, value).apply();
    }

    /**
     * Get default value for preference
     *
     * @throws IllegalArgumentException if not found
     */
    private long getLongDefaultValue(String name) {
        final Long defaultValue = LONG_DEFAULTS.get(name);
        if (defaultValue == null) {
            throw new IllegalArgumentException(
                "Unrecognised Long preference: " + name
            );
        }
        return defaultValue;
    }

    private void getStringPreference(String name, Consumer<String> cb) {
        final String defaultValue = STRING_DEFAULTS.get(name);
        if (defaultValue == null) {
            throw new IllegalArgumentException(
                "Unrecognised String preference: " + name
            );
        }

        executor.execute(() -> {
            String value = prefs.getString(name, defaultValue);
            handler.post(() -> { cb.accept(value); });
        });
    }

    private void setStringPreference(String name, String value) {
        executor.execute(() -> {
            prefs.edit().putString(name, value).apply();
        });
    }

    private void updateLiveBrowseSort() {
        if (liveBrowseSort != null)
            getBrowseSort(value -> { liveBrowseSort.setValue(value); });
    }

    private void updateLiveClueListClueTabsDouble() {
        if (liveClueListClueTabsDouble != null) {
            getClueListClueTabsDouble(value -> {
                liveClueListClueTabsDouble.setValue(value);
            });
        }
    }

    private void updateLiveKeyboardSettings() {
        if (liveKeyboardSettings != null) {
            getFreshKeyboardSettings(liveKeyboardSettings::setValue);
        }
    }

    private void updateLivePlayDeleteCrossingMode() {
        if (livePlayDeleteCrossingMode != null) {
            getPlayDeleteCrossingMode(value -> {
                livePlayDeleteCrossingMode.setValue(value);
            });
        }
    }

    private void updateLivePlayGridRatioLandscape() {
        if (livePlayGridRatioLandscape != null)
            getPlayGridRatioLandscape(
                value -> { livePlayGridRatioLandscape.setValue(value); }
            );
    }

    private void updateLivePlayGridRatioPortrait() {
        if (livePlayGridRatioPortrait != null)
            getPlayGridRatioPortrait(
                value -> { livePlayGridRatioPortrait.setValue(value); }
            );
    }

    private void updateLiveRatingsSettings() {
        if (liveRatingsSettings != null) {
            executor.execute(() -> {
                RatingsSettings settings = new RatingsSettings(
                    prefs.getBoolean(PREF_RATINGS_DISABLE_RATINGS, false),
                    prefs.getBoolean(PREF_BROWSE_ALWAYS_SHOW_RATING, false)
                );
                liveRatingsSettings.postValue(settings);
            });
        }
    }
    private LiveData<Boolean> liveBooleanPreference(String name) {
        MutableLiveData<Boolean> liveData = liveBooleans.get(name);
        if (liveData == null) {
            liveData = new MutableLiveData<Boolean>();
            liveBooleans.put(name, liveData);
            updateLiveBoolean(name);
        }
        return liveData;
    }

    private void updateLiveBoolean(String name) {
        MutableLiveData<Boolean> liveData = liveBooleans.get(name);
        if (liveData != null)
            getBooleanPreference(name, value -> { liveData.setValue(value); });
    }

    /**
     * Get a fresh copy of keyboard settings without using cached live
     * settings
     */
    private void getFreshKeyboardSettings(Consumer<KeyboardSettings> cb) {
        executor.execute(() -> {
            String value = prefs.getString(
                PREF_KEYBOARD_LAYOUT,
                KeyboardLayout.QWERTY.getSettingsValue()
            );
            String modePref = prefs.getString(
                PREF_KEYBOARD_MODE, KeyboardMode.HIDE_MANUAL.getSettingsValue()
            );

            KeyboardSettings settings = new KeyboardSettings(
                prefs.getBoolean(PREF_KEYBOARD_COMPACT, false),
                prefs.getBoolean(PREF_KEYBOARD_HAPTIC, true),
                prefs.getBoolean(PREF_KEYBOARD_HIDE_BUTTON, false),
                KeyboardLayout.getFromSettingsValue(value),
                KeyboardMode.getFromSettingsValue(modePref),
                getIntegerFromStringPref(PREF_KEYBOARD_REPEAT_DELAY, 300),
                getIntegerFromStringPref(PREF_KEYBOARD_REPEAT_INTERVAL, 75),
                prefs.getBoolean(PREF_KEYBOARD_NATIVE, false)
            );

            handler.post(() -> { cb.accept(settings); });
        });
    }

    private int getIntegerFromStringPref(String name, int fallbackValue) {
        try {
            return Integer.parseInt(
                prefs.getString(name, String.valueOf(fallbackValue))
            );
        } catch (NumberFormatException e) {
            return fallbackValue;
        }
    }

    private void migratePrefBooleanToString(
        String legacyName,
        String newName,
        String newTrueValue,
        String newFalseValue
    ) {
        executor.execute(() -> {
            if (!prefs.contains(legacyName))
                return;

            boolean legacyValue = prefs.getBoolean(legacyName, false);
            prefs.edit()
                .remove(legacyName)
                .putString(
                    newName,
                    legacyValue ? newTrueValue : newFalseValue
                ).apply();
        });
    }
}
