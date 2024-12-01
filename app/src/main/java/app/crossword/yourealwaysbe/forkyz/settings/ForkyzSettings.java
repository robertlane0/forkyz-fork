
package app.crossword.yourealwaysbe.forkyz.settings;

import android.content.Context;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences;
import android.os.Handler;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import app.crossword.yourealwaysbe.forkyz.net.Downloader;
import app.crossword.yourealwaysbe.forkyz.net.Downloaders;
import app.crossword.yourealwaysbe.forkyz.tools.ExternalDictionary;
import app.crossword.yourealwaysbe.forkyz.util.JSONUtils;
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
    private static final String PREF_PLAY_TOGGLE_BEFORE_MOVE
        = "toggleBeforeMove";
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
            List.of(
                PREF_DOWNLOAD_UNMETERED,
                PREF_DOWNLOAD_ROAMING,
                PREF_DOWNLOAD_CHARGING,
                PREF_DOWNLOAD_HOURLY,
                PREF_DOWNLOAD_DAYS,
                PREF_DOWNLOAD_DAYS_TIME
            )
        );

    private static final Charset WRITE_CHARSET = Charset.forName("UTF-8");

    public static class BooleanSetting extends Setting<Boolean> {
        public BooleanSetting(String key, boolean defaultValue) {
            super(key, defaultValue);
        }

        @Override
        protected Boolean getValueBackUnchecked(SharedPreferences prefs) {
            return prefs.getBoolean(getKey(), getDefaultValue());
        }

        @Override
        protected void setValueBackUnchecked(
            SharedPreferences prefs, Boolean value
        ) {
            prefs.edit().putBoolean(getKey(), value).apply();
        }

        @Override
        protected void getFromJSONUnchecked(
            SharedPreferences prefs, JSONObject json
        ) throws JSONException {
            setValue(prefs, json.getBoolean(getKey()));
        }
    }

    public static class FloatSetting extends Setting<Float> {
        public FloatSetting(String key, float defaultValue) {
            super(key, defaultValue);
        }

        public FloatSetting(
            String key,
            float defaultValue,
            Function<Float, Boolean> validator
        ) {
            super(key, defaultValue, validator);
        }

        @Override
        protected Float getValueBackUnchecked(SharedPreferences prefs) {
            return prefs.getFloat(getKey(), getDefaultValue());
        }

        @Override
        protected void setValueBackUnchecked(
            SharedPreferences prefs, Float value
        ) {
            prefs.edit().putFloat(getKey(), value).apply();
        }

        @Override
        protected void getFromJSONUnchecked(
            SharedPreferences prefs, JSONObject json
        ) throws JSONException {
            setValue(prefs, (float) json.getDouble(getKey()));
        }
    }

    public static class IntegerSetting extends Setting<Integer> {
        public IntegerSetting(String key, int defaultValue) {
            super(key, defaultValue);
        }

        public IntegerSetting(
            String key,
            int defaultValue,
            Function<Integer, Boolean> validator
        ) {
            super(key, defaultValue, validator);
        }

        @Override
        protected Integer getValueBackUnchecked(SharedPreferences prefs) {
            return prefs.getInt(getKey(), getDefaultValue());
        }

        @Override
        protected void setValueBackUnchecked(
            SharedPreferences prefs, Integer value
        ) {
            prefs.edit().putInt(getKey(), value).apply();
        }

        @Override
        protected void getFromJSONUnchecked(
            SharedPreferences prefs, JSONObject json
        ) throws JSONException {
            setValue(prefs, json.getInt(getKey()));
        }
    }

    public static class LongSetting extends Setting<Long> {
        public LongSetting(String key, long defaultValue) {
            super(key, defaultValue);
        }

        public LongSetting(
            String key,
            long defaultValue,
            Function<Long, Boolean> validator
        ) {
            super(key, defaultValue, validator);
        }

        @Override
        protected Long getValueBackUnchecked(SharedPreferences prefs) {
            return prefs.getLong(getKey(), getDefaultValue());
        }

        @Override
        protected void setValueBackUnchecked(
            SharedPreferences prefs, Long value
        ) {
            prefs.edit().putLong(getKey(), value).apply();
        }

        @Override
        protected void getFromJSONUnchecked(
            SharedPreferences prefs, JSONObject json
        ) throws JSONException {
            setValue(prefs, json.getLong(getKey()));
        }
    }

    public static class StringSetting extends Setting<String> {
        public StringSetting(String key, String defaultValue) {
            super(key, defaultValue);
        }

        public StringSetting(
            String key,
            String defaultValue,
            Function<String, Boolean> validator
        ) {
            super(key, defaultValue, validator);
        }

        @Override
        protected String getValueBackUnchecked(SharedPreferences prefs) {
            return prefs.getString(getKey(), getDefaultValue());
        }

        @Override
        protected void setValueBackUnchecked(
            SharedPreferences prefs, String value
        ) {
            prefs.edit().putString(getKey(), value).apply();
        }

        @Override
        protected void getFromJSONUnchecked(
            SharedPreferences prefs, JSONObject json
        ) throws JSONException {
            setValue(prefs, json.getString(getKey()));
        }
    }

    public static class StringSetSetting extends Setting<Set<String>> {
        public StringSetSetting(String key, Set<String> defaultValue) {
            super(key, defaultValue);
        }

        @Override
        protected Set<String> getValueBackUnchecked(SharedPreferences prefs) {
            return prefs.getStringSet(getKey(), getDefaultValue());
        }

        @Override
        protected void setValueBackUnchecked(
            SharedPreferences prefs, Set<String> value
        ) {
            prefs.edit().putStringSet(getKey(), value).apply();
        }

        /**
         * Add to JSON if not a null value
         */
        public void addToJSON(
            SharedPreferences prefs, JSONObject json
        ) throws JSONException {
            JSONArray array = new JSONArray();
            Set<String> strings = getValue(prefs);
            if (strings != null) {
                for (String v : getValue(prefs))
                    array.put(v);
                json.put(getKey(), array);
            }
        }

        @Override
        protected void getFromJSONUnchecked(
            SharedPreferences prefs, JSONObject json
        ) throws JSONException {
            JSONArray array = json.getJSONArray(getKey());
            Set<String> strings = new HashSet<>();
            for (int i = 0; i < array.length(); i++)
                strings.add(array.getString(i));

            setValue(prefs, strings);
        }
   }

    public static class FSEnumSetting<T extends Enum<T> & EnumSetting>
            extends BaseSetting<T, String> {
        private Class<T> type;

        public FSEnumSetting(
            String key,
            Class<T> type,
            T defaultValue
        ) {
            super(
                key,
                defaultValue,
                value -> {
                    return EnumSetting.getFromSettingsValueNull(
                        value, type
                    ) != null;
                }
            );
            this.type = type;
        }

        @Override
        protected String getValueBackUnchecked(SharedPreferences prefs) {
            return prefs.getString(
                getKey(),
                getDefaultValue().getSettingsValue()
            );
        }

        @Override
        public void setValue(SharedPreferences prefs, T value) {
            setValueBack(prefs, value.getSettingsValue());
        }

        @Override
        protected T getValueUnchecked(SharedPreferences prefs) {
            String value = getValueBackUnchecked(prefs);
            return EnumSetting.getFromSettingsValueNonNull(
                value, type, getDefaultValue()
            );
        }

        @Override
        protected void setValueBackUnchecked(
            SharedPreferences prefs, String value
        ) {
            prefs.edit().putString(getKey(), value).apply();
        }

        @Override
        protected void getFromJSONUnchecked(
            SharedPreferences prefs, JSONObject json
        ) throws JSONException {
            setValueBack(prefs, json.getString(getKey()));
        }
    }

    FSEnumSetting<Orientation> orientationSetting
        = new FSEnumSetting<Orientation>(
            PREF_APP_ORIENTATION_LOCK,
            Orientation.class,
            Orientation.UNLOCKED
        );
    FSEnumSetting<Theme> appThemeSetting
        = new FSEnumSetting<Theme>(
            PREF_APP_THEME,
            Theme.class,
            Theme.STANDARD
        );
    FSEnumSetting<DayNightMode> appDayNightModeSetting
        = new FSEnumSetting<DayNightMode>(
            PREF_APP_DAY_NIGHT_MODE,
            DayNightMode.class,
            DayNightMode.DAY
        );
    FSEnumSetting<BrowseSwipeAction> browseSwipeActionSetting
        = new FSEnumSetting<BrowseSwipeAction>(
            PREF_BROWSE_SWIPE_ACTION,
            BrowseSwipeAction.class,
            BrowseSwipeAction.DELETE
        );
    FSEnumSetting<ClueTabsDouble> clueTabsDoubleSetting
        = new FSEnumSetting<ClueTabsDouble>(
            PREF_CLUE_LIST_CLUE_TABS_DOUBLE,
            ClueTabsDouble.class,
            ClueTabsDouble.NEVER
        );
    FSEnumSetting<StorageLocation> storageLocationSetting
        = new FSEnumSetting<StorageLocation>(
            PREF_FILE_HANDLER_STORAGE_LOC,
            StorageLocation.class,
            StorageLocation.INTERNAL
        );
    FSEnumSetting<KeyboardLayout> keyboardLayoutSetting
        = new FSEnumSetting<KeyboardLayout>(
            PREF_KEYBOARD_LAYOUT,
            KeyboardLayout.class,
            KeyboardLayout.QWERTY
        );
    FSEnumSetting<KeyboardMode> keyboardModeSetting
        = new FSEnumSetting<KeyboardMode>(
            PREF_KEYBOARD_MODE,
            KeyboardMode.class,
            KeyboardMode.HIDE_MANUAL
        );
    FSEnumSetting<DeleteCrossingModeSetting> deleteCrossingModeSetting
        = new FSEnumSetting<DeleteCrossingModeSetting>(
            PREF_PLAY_DELETE_CROSSING_MODE,
            DeleteCrossingModeSetting.class,
            DeleteCrossingModeSetting.DELETE
        );
    FSEnumSetting<GridRatio> landscapeGridRatioSetting
        = new FSEnumSetting<GridRatio>(
            PREF_PLAY_GRID_RATIO_LANDSCAPE,
            GridRatio.class,
            GridRatio.PUZZLE_SHAPE
        );
    FSEnumSetting<GridRatio> portraitGridRatioSetting
        = new FSEnumSetting<GridRatio>(
            PREF_PLAY_GRID_RATIO_PORTRAIT,
            GridRatio.class,
            GridRatio.PUZZLE_SHAPE
        );
    FSEnumSetting<MovementStrategySetting> movementStrategySetting
        = new FSEnumSetting<MovementStrategySetting>(
            PREF_PLAY_MOVEMENT_STRATEGY,
            MovementStrategySetting.class,
            MovementStrategySetting.MOVE_NEXT_ON_AXIS
        );
    FSEnumSetting<CycleUnfilledMode> cycleUnfilledSetting
        = new FSEnumSetting<CycleUnfilledMode>(
            PREF_PLAY_CYCLE_UNFILLED_MODE,
            CycleUnfilledMode.class,
            CycleUnfilledMode.NEVER
        );

    private HashMap<String, BooleanSetting> booleanSettings = new HashMap<>();
    private HashMap<String, FloatSetting> floatSettings = new HashMap<>();
    private HashMap<String, IntegerSetting> integerSettings = new HashMap<>();
    private HashMap<String, LongSetting> longSettings = new HashMap<>();
    private HashMap<String, StringSetting> stringSettings = new HashMap<>();
    private HashMap<String, StringSetSetting> stringSetSettings
        = new HashMap<>();
    private HashMap<String, FSEnumSetting<?>> enumSettings = new HashMap<>();
    {
        BooleanSetting[] booleans = {
            new BooleanSetting(PREF_BROWSE_ALWAYS_SHOW_RATING, false),
            new BooleanSetting(PREF_BROWSE_DELETE_ON_CLEANUP, false),
            new BooleanSetting(PREF_BROWSE_DISABLE_SWIPE, false),
            new BooleanSetting(PREF_BROWSE_INDICATE_IF_SOLUTION, false),
            new BooleanSetting(PREF_BROWSE_NEW_PUZZLE, false),
            new BooleanSetting(PREF_BROWSE_SHOW_PERCENTAGE_CORRECT, false),
            new BooleanSetting(PREF_EXT_CROSSWORD_SOLVER_ENABLED, true),
            new BooleanSetting(PREF_CLUE_LIST_SHOW_WORDS, false),
            new BooleanSetting(PREF_CLUE_LIST_SNAP_TO_CLUE, false),
            new BooleanSetting(PREF_DOWNLOAD_UNMETERED, true),
            new BooleanSetting(PREF_DOWNLOAD_ROAMING, false),
            new BooleanSetting(PREF_DOWNLOAD_CHARGING, false),
            new BooleanSetting(PREF_DOWNLOAD_HOURLY, false),
            new BooleanSetting(PREF_KEYBOARD_COMPACT, false),
            new BooleanSetting(PREF_KEYBOARD_HAPTIC, true),
            new BooleanSetting(PREF_KEYBOARD_HIDE_BUTTON, false),
            new BooleanSetting(PREF_KEYBOARD_NATIVE, false),
            new BooleanSetting(PREF_PLAY_CLUE_BELOW_GRID, false),
            new BooleanSetting(PREF_PLAY_DOUBLE_TAP_FIT_BOARD, false),
            new BooleanSetting(PREF_PLAY_ENSURE_VISIBLE, true),
            new BooleanSetting(PREF_PLAY_ENTER_CHANGES_DIRECTION, true),
            new BooleanSetting(PREF_PLAY_FIT_TO_SCREEN, false),
            new BooleanSetting(PREF_PLAY_FULL_SCREEN, false),
            new BooleanSetting(PREF_PLAY_PLAY_LETTER_UNDO_ENABLED, false),
            new BooleanSetting(PREF_PLAY_PREDICT_ANAGRAM_CHARS, true),
            new BooleanSetting(
                PREF_PLAY_PRESERVE_CORRECT_LETTERS_IN_SHOW_ERRORS, false
            ),
            new BooleanSetting(PREF_PLAY_RANDOM_CLUE_ON_SHAKE, false),
            new BooleanSetting(PREF_PLAY_SCRATCH_DISPLAY, false),
            new BooleanSetting(PREF_PLAY_SCRATCH_MODE, false),
            new BooleanSetting(PREF_PLAY_SHOW_COUNT, false),
            new BooleanSetting(PREF_PLAY_SHOW_CLUES_TAB, true),
            new BooleanSetting(PREF_PLAY_SHOW_ERRORS_CLUE, false),
            new BooleanSetting(PREF_PLAY_SHOW_ERRORS_CURSOR, false),
            new BooleanSetting(PREF_PLAY_SHOW_ERRORS_GRID, false),
            new BooleanSetting(PREF_PLAY_SHOW_TIMER, false),
            new BooleanSetting(PREF_PLAY_SKIP_FILLED, false),
            new BooleanSetting(PREF_PLAY_TOGGLE_BEFORE_MOVE, false),
            new BooleanSetting(PREF_PLAY_SPACE_CHANGE_DIRECTION, true),
            new BooleanSetting(PREF_PLAY_SUPPRESS_HINT_HIGHLIGHTING, false),
            new BooleanSetting(PREF_PLAY_INDICATE_SHOW_ERRORS, true),
            new BooleanSetting(PREF_RATINGS_DISABLE_RATINGS, false),
            new BooleanSetting(PREF_VOICE_ALWAYS_ANNOUNCE_BOX, false),
            new BooleanSetting(PREF_VOICE_ALWAYS_ANNOUNCE_CLUE, false),
            new BooleanSetting(PREF_VOICE_BUTTON_ACTIVATES_VOICE, false),
            new BooleanSetting(PREF_VOICE_BUTTON_ANNOUNCE_CLUE, false),
            new BooleanSetting(PREF_VOICE_EQUALS_ANNOUNCE_CLUE, true),
            new BooleanSetting(PREF_VOICE_VOLUME_ACTIVATES_VOICE, false),
            new BooleanSetting(PREF_SUPPRESS_SUMMARY_NOTIFICATIONS, false),
            new BooleanSetting(PREF_SUPPRESS_INDIVIDUAL_NOTIFICATIONS, false),
            // DOWNLOADERS
            new BooleanSetting("downloadDeStandaard", true),
            new BooleanSetting("downloadDeTelegraaf", true),
            new BooleanSetting("downloadGuardianDailyCryptic", true),
            new BooleanSetting("downloadHamAbend", true),
            new BooleanSetting("downloadIndependentDailyCryptic", true),
            new BooleanSetting("downloadJonesin", true),
            new BooleanSetting("downloadJoseph", true),
            new BooleanSetting("download20Minutes", true),
            new BooleanSetting("downloadLeParisienF1", true),
            new BooleanSetting("downloadLeParisienF2", true),
            new BooleanSetting("downloadLeParisienF3", true),
            new BooleanSetting("downloadLeParisienF4", true),
            new BooleanSetting("downloadMetroCryptic", true),
            new BooleanSetting("downloadNewsday", true),
            new BooleanSetting("downloadNewYorkTimesSyndicated", true),
            new BooleanSetting("downloadPremier", true),
            new BooleanSetting("downloadSheffer", true),
            new BooleanSetting("downloadUniversal", true),
            new BooleanSetting("downloadUSAToday", true),
            new BooleanSetting("downloadWaPoSunday", true),
            new BooleanSetting("downloadWsj", true),
            new BooleanSetting("scrapeCru", false),
            new BooleanSetting("scrapeKegler", false),
            new BooleanSetting("scrapePrivateEye", false),
            new BooleanSetting("scrapePrzekroj", false),
            new BooleanSetting("downloadCustomDaily", false),
            new BooleanSetting("dlOnStartup", false)
        };
        for (BooleanSetting s : booleans)
            booleanSettings.put(s.getKey(), s);

        FloatSetting[] floats = {
            new FloatSetting(
                PREF_PLAY_SCALE,
                1.0F,
                value -> { return 0 <= value; }
            ),
        };
        for (FloatSetting s : floats)
            floatSettings.put(s.getKey(), s);

        IntegerSetting[] integers = {
            new IntegerSetting(
                PREF_BROWSE_SORT,
                0,
                value -> { return 0 <= value && value <= 2; }
            ),
            new IntegerSetting(
                PREF_PLAY_CLUE_TABS_PAGES[0],
                0,
                value -> { return value >= 0; }
            ),
            new IntegerSetting(
                PREF_PLAY_CLUE_TABS_PAGES[1],
                1,
                value -> { return value >= 0; }
            ),
        };
        for (IntegerSetting s : integers)
            integerSettings.put(s.getKey(), s);

        LongSetting[] longs = {
            new LongSetting(
                PREF_BROWSE_LAST_DOWNLOAD,
                0L,
                value -> {
                    return value >= 0 && value <= System.currentTimeMillis();
                }
            ),
        };
        for (LongSetting s : longs)
            longSettings.put(s.getKey(), s);

        StringSetting[] strings = {
            new StringSetting(PREF_BROWSE_CLEANUP_AGE, "-1"),
            new StringSetting(PREF_BROWSE_CLEANUP_AGE_ARCHIVE, "-1"),
            new StringSetting(PREF_BROWSE_LAST_SEEN_VERSION, ""),
            new StringSetting(PREF_EXT_CHAT_GPT_API_KEY, ""),
            new StringSetting(PREF_EXT_DICTIONARY, "FREE"),
            new StringSetting(
                PREF_DOWNLOAD_TIMEOUT, PREF_DOWNLOAD_TIMEOUT_DEFAULT
            ),
            new StringSetting(PREF_DOWNLOAD_DAYS_TIME, "8"),
            new StringSetting(PREF_FILE_HANDLER_SAF_ARCHIVE, null),
            new StringSetting(PREF_FILE_HANDLER_SAF_CROSSWORDS, null),
            new StringSetting(PREF_FILE_HANDLER_SAF_ROOT, null),
            new StringSetting(PREF_FILE_HANDLER_SAF_TO_IMPORT, null),
            new StringSetting(PREF_FILE_HANDLER_SAF_TO_IMPORT_DONE, null),
            new StringSetting(PREF_FILE_HANDLER_SAF_TO_IMPORT_FAILED, null),
            new StringSetting(PREF_KEYBOARD_REPEAT_DELAY, "300"),
            new StringSetting(PREF_KEYBOARD_REPEAT_INTERVAL, "75"),
            // DOWNLOADERS
            new StringSetting("customDailyTitle", ""),
            new StringSetting("customDailyUrl", ""),
        };
        for (StringSetting s : strings)
            stringSettings.put(s.getKey(), s);

        StringSetSetting[] stringSets = {
            new StringSetSetting(PREF_AUTO_DOWNLOADERS, Collections.emptySet()),
            new StringSetSetting(PREF_DOWNLOAD_DAYS, Collections.emptySet()),
        };
        for (StringSetSetting s : stringSets)
            stringSetSettings.put(s.getKey(), s);

        FSEnumSetting<?>[] enums = {
            orientationSetting,
            appThemeSetting,
            appDayNightModeSetting,
            browseSwipeActionSetting,
            clueTabsDoubleSetting,
            storageLocationSetting,
            keyboardLayoutSetting,
            keyboardModeSetting,
            deleteCrossingModeSetting,
            landscapeGridRatioSetting,
            portraitGridRatioSetting,
            movementStrategySetting,
            cycleUnfilledSetting
        };
        for (FSEnumSetting<?> s : enums)
            enumSettings.put(s.getKey(), s);
    };
    List<Map<String, ? extends BaseSetting<?, ?>>> allSettings = List.of(
        booleanSettings,
        enumSettings,
        floatSettings,
        longSettings,
        stringSettings,
        stringSetSettings
    );

    private SharedPreferences prefs;
    private Handler handler;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private MutableLiveData<Accessor> liveBrowseSort;
    private MutableLiveData<DeleteCrossingMode> livePlayDeleteCrossingMode;
    private MutableLiveData<KeyboardSettings> liveKeyboardSettings;
    private MutableLiveData<RatingsSettings> liveRatingsSettings;

    private OnSharedPreferenceChangeListener prefChangeListener
        = (prefs, key) -> {
            if (
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
            } else if (
                PREF_BROWSE_ALWAYS_SHOW_RATING.equals(key)
                || PREF_RATINGS_DISABLE_RATINGS.equals(key)
            ) {
                updateLiveRatingsSettings();
            } else if (PREF_BROWSE_SORT.equals(key)) {
                updateLiveBrowseSort();
            } else if (PREF_PLAY_DELETE_CROSSING_MODE.equals(key)) {
                updateLivePlayDeleteCrossingMode();
            } else {
                BaseSetting<?, ?> setting = getSetting(key);
                if (setting != null)
                    setting.updateLiveData(prefs);
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
        getFSEnumSetting(orientationSetting, cb);
    }

    /**
     * Whether to use day theme, night theme, or follow system
     */
    public void getAppDayNightMode(Consumer<DayNightMode> cb) {
        getFSEnumSetting(appDayNightModeSetting, cb);
    }

    public void setAppDayNightMode(DayNightMode mode, Runnable cb) {
        setFSEnumSetting(appDayNightModeSetting, mode, cb);
    }

    /**
     * The theme selected by user
     */
    public void getAppTheme(Consumer<Theme> cb) {
        getFSEnumSetting(appThemeSetting, cb);
    }

    /**
     * Blocking version of getting theme
     *
     * Needed to apply dynamic colors to application
     */
    public Theme getAppThemeSync() {
        return getFSEnumSettingSync(appThemeSetting);
    }

    /**
     * Age of a puzzle in the crosswords list to be removed in cleanup
     */
    public void getBrowseCleanupAge(Consumer<String> cb) {
        getStringSetting(PREF_BROWSE_CLEANUP_AGE, cb);
    }

    /**
     * Age of a puzzle in the archive list to be removed in cleanup
     */
    public void getBrowseCleanupAgeArchive(Consumer<String> cb) {
        getStringSetting(PREF_BROWSE_CLEANUP_AGE_ARCHIVE, cb);
    }

    /**
     * Delete on clean up crosswords list, rather than send to archive
     */
    public void getBrowseDeleteOnCleanup(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_BROWSE_DELETE_ON_CLEANUP, cb);
    }

    /**
     * Disable the swipe action in the browse list
     */
    public void getBrowseDisableSwipe(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_BROWSE_DISABLE_SWIPE, cb);
    }

    public LiveData<Boolean> liveBrowseDisableSwipe() {
        return liveBooleanSetting(PREF_BROWSE_DISABLE_SWIPE);
    }

    /**
     * Whether to indicate in browse if puzzle has answers in data
     */
    public void getBrowseIndicateIfSolution(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_BROWSE_INDICATE_IF_SOLUTION, cb);
    }

    public LiveData<Boolean> liveBrowseIndicateIfSolution() {
        return liveBooleanSetting(PREF_BROWSE_INDICATE_IF_SOLUTION);
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
        return getLongSettingSync(PREF_BROWSE_LAST_DOWNLOAD);
    }

    /**
     * Set last time download was triggered by browse
     *
     * Runs synchronously, avoid on main thread
     */
    public void setBrowseLastDownloadSync(long value) {
        setLongSettingSync(PREF_BROWSE_LAST_DOWNLOAD, value);
    }

    /**
     * The last version of Forkyz seen on start
     *
     * Used to display a welcome on new Forkyz versions
     */
    public void getBrowseLastSeenVersion(Consumer<String> cb) {
        getStringSetting(PREF_BROWSE_LAST_SEEN_VERSION, cb);
    }

    public void setBrowseLastSeenVersion(String value) {
        setStringSetting(PREF_BROWSE_LAST_SEEN_VERSION, value);
    }

    /**
     * BrowseActivity checks this on resume to know if to refresh list
     *
     * I.e. true if a puzzle added to puzzles without browse knowing otherwise.
     */
    public void getBrowseNewPuzzle(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_BROWSE_NEW_PUZZLE, cb);
    }

    public void setBrowseNewPuzzle(boolean pending) {
        setBooleanSetting(PREF_BROWSE_NEW_PUZZLE, pending);
    }

    public LiveData<Boolean> liveBrowseShowPercentageCorrect() {
        return liveBooleanSetting(PREF_BROWSE_SHOW_PERCENTAGE_CORRECT);
    }

    /**
     * How to order the puzzles in the browse list
     */
    public void getBrowseSort(Consumer<Accessor> cb) {
        getIntegerSetting(PREF_BROWSE_SORT, value -> {
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

        setIntegerSetting(PREF_BROWSE_SORT, intValue);
    }

    /**
     * Whether to delete or archve on swipe
     */
    public void getBrowseSwipeAction(Consumer<BrowseSwipeAction> cb) {
        getFSEnumSetting(browseSwipeActionSetting, cb);
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
                    getStringSettingSync(PREF_DOWNLOAD_DAYS_TIME)
                );
            } catch (NumberFormatException e) {
                // ignore, keep 8
            }

            final int finalTimeOfDay = timeOfDay;

            handler.post(() -> {
                cb.accept(new BackgroundDownloadSettings(
                    getBooleanSettingSync(PREF_DOWNLOAD_UNMETERED),
                    getBooleanSettingSync(PREF_DOWNLOAD_ROAMING),
                    getBooleanSettingSync(PREF_DOWNLOAD_CHARGING),
                    getBooleanSettingSync(PREF_DOWNLOAD_HOURLY),
                    getStringSetSettingSync(PREF_DOWNLOAD_DAYS),
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
        setBooleanSetting(PREF_DOWNLOAD_HOURLY, hourly, cb);
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
        getFSEnumSetting(clueTabsDoubleSetting, cb);
    }

    public LiveData<ClueTabsDouble> liveClueListClueTabsDouble() {
        return liveFSEnumSetting(clueTabsDoubleSetting);
    }

    /**
     * Whether to show the board for each clue in the clue list
     *
     * Or just the currently selected clue.
     */
    public void getClueListShowWords(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_CLUE_LIST_SHOW_WORDS, cb);
    }

    /**
     * Whether to make sure current clue always visible in clue tabs
     */
    public void getClueListSnapToClue(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_CLUE_LIST_SNAP_TO_CLUE, cb);
    }


    public LiveData<Boolean> liveClueListShowWords() {
        return liveBooleanSetting(PREF_CLUE_LIST_SHOW_WORDS);
    }

    public void setClueListShowWords(boolean value) {
        setBooleanSetting(PREF_CLUE_LIST_SHOW_WORDS, value);
    }

    /**
     * The API key for ask chat gpt for help if set
     */
    public void getExtChatGPTAPIKey(Consumer<String> cb) {
        getStringSetting(PREF_EXT_CHAT_GPT_API_KEY, cb);
    }

    public void getExtCrosswordSolverEnabled(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_EXT_CROSSWORD_SOLVER_ENABLED, cb);
    }

    public void getExtDictionary(Consumer<ExternalDictionary> cb) {
        executor.execute(() -> {
            String value = getStringSettingSync(PREF_EXT_DICTIONARY);
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
            StorageLocation storageLocation = getFSEnumSettingSync(
                storageLocationSetting
            );
            String safRoot = getStringSettingSync(PREF_FILE_HANDLER_SAF_ROOT);
            String safCrosswords = getStringSettingSync(
                PREF_FILE_HANDLER_SAF_CROSSWORDS
            );
            String safArchive = getStringSettingSync(
                PREF_FILE_HANDLER_SAF_ARCHIVE
            );
            String safToImport = getStringSettingSync(
                PREF_FILE_HANDLER_SAF_TO_IMPORT
            );
            String safToImportDone = getStringSettingSync(
                PREF_FILE_HANDLER_SAF_TO_IMPORT_DONE
            );
            String safToImportFailed = getStringSettingSync(
                PREF_FILE_HANDLER_SAF_TO_IMPORT_FAILED
            );

            handler.post(() -> {
                cb.accept(new FileHandlerSettings(
                    storageLocation,
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
            setFSEnumSettingSync(
                storageLocationSetting,
                settings.storageLocation()
            );
            setStringSettingSync(
                PREF_FILE_HANDLER_SAF_ROOT,
                settings.safRootURI()
            );
            setStringSettingSync(
                PREF_FILE_HANDLER_SAF_CROSSWORDS,
                settings.safCrosswordsURI()
            );
            setStringSettingSync(
                PREF_FILE_HANDLER_SAF_ARCHIVE,
                settings.safArchiveURI()
            );
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
        getBooleanSetting(PREF_PLAY_CLUE_BELOW_GRID, cb);
    }

    /**
     * Whether to display the currently selected clue below the grid
     *
     * Default is in the title bar
     */
    public LiveData<Boolean> livePlayClueBelowGrid() {
        return liveBooleanSetting(PREF_PLAY_CLUE_BELOW_GRID);
    }

    /**
     * To remember which page of the clue list is selected in PlayActivity
     */
    public void getPlayClueTabsPage(int pageNum, Consumer<Integer> cb) {
        if (0 <= pageNum && pageNum < PREF_PLAY_CLUE_TABS_PAGES.length)
            getIntegerSetting(PREF_PLAY_CLUE_TABS_PAGES[pageNum], cb);
    }

    public void setPlayClueTabsPage(int pageNum, int value) {
        if (0 <= pageNum && pageNum < PREF_PLAY_CLUE_TABS_PAGES.length)
            setIntegerSetting(PREF_PLAY_CLUE_TABS_PAGES[pageNum], value);
    }

    /**
     * Whether to delete characters that may belong to a crossing word
     */
    public void getPlayDeleteCrossingMode(Consumer<DeleteCrossingMode> cb) {
        getFSEnumSetting(
            deleteCrossingModeSetting,
            value -> { cb.accept(value.getDeleteCrossingMode()); }
        );
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
     * Whether double tapping the screen fits the board to screen
     */
    public void getPlayDoubleTapFitBoard(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_DOUBLE_TAP_FIT_BOARD, cb);
    }

    /**
     * Ensure the currently selected word/cell is visible on play board
     */
    public void getPlayEnsureVisible(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_ENSURE_VISIBLE, cb);
    }

    /**
     * Whether to change selection direction when enter pressed
     */
    public void getPlayEnterChangesDirection(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_ENTER_CHANGES_DIRECTION, cb);
    }

    /**
     * Whether to fit puzzle to screen on starting PlayActivity
     */
    public void getPlayFitToScreen(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_FIT_TO_SCREEN, cb);
    }

    /**
     * Whether to display the app in full screen mode
     *
     * I think this doesn't do much on modern phones, but it used to
     * hide the status bar at the top.
     */
    public void getPlayFullScreen(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_FULL_SCREEN, cb);
    }

    /**
     * Get grid ratio when clue tabs showing on play screen landscape mode
     */
    public void getPlayGridRatioLandscape(Consumer<GridRatio> cb) {
        getFSEnumSetting(landscapeGridRatioSetting, cb);
    }

    public LiveData<GridRatio> livePlayGridRatioLandscape() {
        return liveFSEnumSetting(landscapeGridRatioSetting);
    }

    /**
     * Get grid ratio when clue tabs showing on play screen
     */
    public void getPlayGridRatioPortrait(Consumer<GridRatio> cb) {
        getFSEnumSetting(portraitGridRatioSetting, cb);
    }

    public LiveData<GridRatio> livePlayGridRatioPortrait() {
        return liveFSEnumSetting(portraitGridRatioSetting);
    }

    /**
     * Get the style of movement around the board
     */
    public void getPlayMovementStrategy(Consumer<MovementStrategy> cb) {
        executor.execute(() -> {
            MovementStrategySetting strategySetting
                = getFSEnumSettingSync(movementStrategySetting);
            CycleUnfilledMode cycleUnfilledMode
                = getFSEnumSettingSync(cycleUnfilledSetting);

            boolean cycleForwards
                = cycleUnfilledMode != CycleUnfilledMode.NEVER;
            boolean cycleBackwards
                = cycleUnfilledMode == CycleUnfilledMode.ALWAYS;

            handler.post(() -> {
                MovementStrategy strategy
                    = strategySetting.getMovementStrategy();
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
        getBooleanSetting(PREF_PLAY_RANDOM_CLUE_ON_SHAKE, cb);
    }

    /**
     * Whether to use play letter undo stack on Playboard
     */
    public void getPlayPlayLetterUndoEnabled(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_PLAY_LETTER_UNDO_ENABLED, cb);
    }

    /**
     * Whether to try to guess the source chars of an anagram
     *
     * On notes page, predict by matching against clue
     */
    public void getPlayPredictAnagramChars(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_PREDICT_ANAGRAM_CHARS, cb);
    }

    /**
     * When errors are show, don't let correct cell entries be changed
     */
    public void getPlayPreserveCorrectLettersInShowErrors(
        Consumer<Boolean> cb
    ) {
        getBooleanSetting(
            PREF_PLAY_PRESERVE_CORRECT_LETTERS_IN_SHOW_ERRORS, cb
        );
    }

    public LiveData<Boolean> livePlayPreserveCorrectLettersInShowErrors() {
        return liveBooleanSetting(
            PREF_PLAY_PRESERVE_CORRECT_LETTERS_IN_SHOW_ERRORS
        );
    }

    /**
     * Remember the scale/size of the board in the PlayActivity
     */
    public void getPlayScale(Consumer<Float> cb) {
        getFloatSetting(PREF_PLAY_SCALE, cb);
    }

    public void setPlayScale(float value) {
        setFloatSetting(PREF_PLAY_SCALE, value);
    }

    /**
     * Whether to show scratch notes on the play board in grey letters
     */
    public void getPlayScratchDisplay(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_SCRATCH_DISPLAY, cb);
    }

    /**
     * Whether entered characters should go to scratch notes
     *
     * Instead of going to the main response for the cell, enter new
     * chars as scratch notes for the current clue.
     */
    public void getPlayScratchMode(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_SCRATCH_MODE, cb);
    }

    public LiveData<Boolean> livePlayScratchMode() {
        return liveBooleanSetting(PREF_PLAY_SCRATCH_MODE);
    }

    public void setPlayScratchMode(boolean value) {
        setBooleanSetting(PREF_PLAY_SCRATCH_MODE, value);
    }

    /**
     * Wether to show the clue solution length after the clue hint
     */
    public void getPlayShowCount(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_SHOW_COUNT, cb);
    }

    public LiveData<Boolean> livePlayShowCount() {
        return liveBooleanSetting(PREF_PLAY_SHOW_COUNT);
    }

    public void setPlayShowCount(boolean value) {
        setBooleanSetting(PREF_PLAY_SHOW_COUNT, value);
    }

    /**
     * Whether to highlight errors on completely filled clue entries
     */
    public void getPlayShowErrorsClue(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_SHOW_ERRORS_CLUE, cb);
    }

    public LiveData<Boolean> livePlayShowErrorsClue() {
        return liveBooleanSetting(PREF_PLAY_SHOW_ERRORS_CLUE);
    }

    public void setPlayShowErrorsClue(boolean value) {
        setBooleanSetting(PREF_PLAY_SHOW_ERRORS_CLUE, value);
    }

    /**
     * Whether to highlight errors in the currently selected cell
     */
    public void getPlayShowErrorsCursor(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_SHOW_ERRORS_CURSOR, cb);
    }

    public LiveData<Boolean> livePlayShowErrorsCursor() {
        return liveBooleanSetting(PREF_PLAY_SHOW_ERRORS_CURSOR);
    }

    public void setPlayShowErrorsCursor(boolean value) {
        setBooleanSetting(PREF_PLAY_SHOW_ERRORS_CURSOR, value);
    }

    /**
     * Whether to highlight errors anywhere on the grid
     */
    public void getPlayShowErrorsGrid(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_SHOW_ERRORS_GRID, cb);
    }

    public LiveData<Boolean> livePlayShowErrorsGrid() {
        return liveBooleanSetting(PREF_PLAY_SHOW_ERRORS_GRID);
    }

    public void setPlayShowErrorsGrid(boolean value) {
        setBooleanSetting(PREF_PLAY_SHOW_ERRORS_GRID, value);
    }

    public void getPlayShowTimer(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_SHOW_TIMER, cb);
    }

    public LiveData<Boolean> livePlayShowTimer() {
        return liveBooleanSetting(PREF_PLAY_SHOW_TIMER);
    }

    /**
     * Whether to skip filled cells when moving to the next
     */
    public void getPlaySkipFilled(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_SKIP_FILLED, cb);
    }

    /**
     * Whether to show the clue list on the PlayActivity
     */
    public void getPlayShowCluesTab(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_SHOW_CLUES_TAB, cb);
    }

    public void setPlayShowCluesTab(boolean value) {
        setBooleanSetting(PREF_PLAY_SHOW_CLUES_TAB, value);
    }

    /**
     * Whether pressing space changes the selection direction
     */
    public void getPlaySpaceChangesDirection(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_SPACE_CHANGE_DIRECTION, cb);
    }

    /**
     * Whether hinted squares should not be highlighted on board view
     */
    public void getPlaySuppressHintHighlighting(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_SUPPRESS_HINT_HIGHLIGHTING, cb);
    }

    /**
     * D-pad/arrow-key behaviour on clue change
     *
     * True means change clue on first press, do movement on second. False
     * means do both in one.
     */
    public void getPlayToggleBeforeMove(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_TOGGLE_BEFORE_MOVE, cb);
    }

    public LiveData<Boolean> livePlayToggleBeforeMove() {
        return liveBooleanSetting(PREF_PLAY_TOGGLE_BEFORE_MOVE);
    }

    /**
     * Whether to indicate show errors status (i.e. whether turned on)
     */
    public void getPlayIndicateShowErrors(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_PLAY_INDICATE_SHOW_ERRORS, cb);
    }

    public LiveData<Boolean> livePlayIndicateShowErrors() {
        return liveBooleanSetting(PREF_PLAY_INDICATE_SHOW_ERRORS);
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
        getBooleanSetting(PREF_VOICE_ALWAYS_ANNOUNCE_BOX, cb);
    }

    /**
     * Whether to always announce the current clue
     */
    public void getVoiceAlwaysAnnounceClue(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_VOICE_ALWAYS_ANNOUNCE_CLUE, cb);
    }

    /**
     * Whether to show a button for activating voice commands
     */
    public void getVoiceButtonActivatesVoice(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_VOICE_BUTTON_ACTIVATES_VOICE, cb);
    }

    /**
     * Whether pressing volume down activates voice input
     */
    public LiveData<Boolean> liveVoiceVolumeActivatesVoice() {
        return liveBooleanSetting(PREF_VOICE_VOLUME_ACTIVATES_VOICE);
    }

    /**
     * Whether pressing = (?) announces the clue with voice
     */
    public LiveData<Boolean> liveVoiceEqualsAnnounceClue() {
        return liveBooleanSetting(PREF_VOICE_EQUALS_ANNOUNCE_CLUE);
    }

    /**
     * Whether to show a button for reading out clue
     */
    public void getVoiceButtonAnnounceClue(Consumer<Boolean> cb) {
        getBooleanSetting(PREF_VOICE_BUTTON_ANNOUNCE_CLUE, cb);
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
                getBooleanSettingSync("downloadDeStandaard"),
                getBooleanSettingSync("downloadDeTelegraaf"),
                getBooleanSettingSync("downloadGuardianDailyCryptic"),
                getBooleanSettingSync("downloadHamAbend"),
                getBooleanSettingSync("downloadIndependentDailyCryptic"),
                getBooleanSettingSync("downloadJonesin"),
                getBooleanSettingSync("downloadJoseph"),
                getBooleanSettingSync("download20Minutes"),
                getBooleanSettingSync("downloadLeParisienF1"),
                getBooleanSettingSync("downloadLeParisienF2"),
                getBooleanSettingSync("downloadLeParisienF3"),
                getBooleanSettingSync("downloadLeParisienF4"),
                getBooleanSettingSync("downloadMetroCryptic"),
                getBooleanSettingSync("downloadNewsday"),
                getBooleanSettingSync("downloadNewYorkTimesSyndicated"),
                getBooleanSettingSync("downloadPremier"),
                getBooleanSettingSync("downloadSheffer"),
                getBooleanSettingSync("downloadUniversal"),
                getBooleanSettingSync("downloadUSAToday"),
                getBooleanSettingSync("downloadWaPoSunday"),
                getBooleanSettingSync("downloadWsj"),
                getBooleanSettingSync("scrapeCru"),
                getBooleanSettingSync("scrapeKegler"),
                getBooleanSettingSync("scrapePrivateEye"),
                getBooleanSettingSync("scrapePrzekroj"),
                getBooleanSettingSync("downloadCustomDaily"),
                getStringSettingSync("customDailyTitle"),
                getStringSettingSync("customDailyUrl"),
                getBooleanSettingSync(PREF_SUPPRESS_SUMMARY_NOTIFICATIONS),
                getBooleanSettingSync(PREF_SUPPRESS_INDIVIDUAL_NOTIFICATIONS),
                getStringSetSettingSync(PREF_AUTO_DOWNLOADERS),
                Integer.valueOf(getStringSettingSync(PREF_DOWNLOAD_TIMEOUT)),
                getBooleanSettingSync("dlOnStartup")
            );
            handler.post(() -> { cb.accept(settings); });
        });
    }

    public void getDownloadTimeout(Consumer<Integer> cb) {
        getStringSetting(PREF_DOWNLOAD_TIMEOUT, value -> {
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
            setBooleanSettingSync(PREF_SUPPRESS_SUMMARY_NOTIFICATIONS, true);
            setBooleanSettingSync(PREF_SUPPRESS_INDIVIDUAL_NOTIFICATIONS, true);
        });
    }

    public void importSettings(InputStream is) throws IOException {
        try {
            JSONObject json = JSONUtils.streamToJSON(is);

            var i = iterateSettings();
            while (i.hasNext()) {
                i.next().getFromJSON(prefs, json);
            }
        } catch (JSONException e) {
            throw new IOException("JSON Error importing settings", e);
        }
    }

    public void exportSettings(OutputStream os) throws IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(
                new BufferedWriter(
                    new OutputStreamWriter(os, WRITE_CHARSET)
                )
            );

            JSONObject json = new JSONObject();
            var i = iterateSettings();
            while (i.hasNext()) {
                i.next().addToJSON(prefs, json);
            }

            writer.print(json.toString());
        } catch (JSONException e) {
            throw new IOException("JSON Error exporting settings", e);
        } finally {
            // don't close original output stream, it's the caller's job
            if (writer != null)
                writer.flush();
        };
    }

    /**
     * Wait for get/set executor to finish
     *
     * For testing, cannot use after
     */
    void shutdownWaitAll() {
        try {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // oh well
        }
    }

    private void getBooleanSetting(
        String key, Consumer<Boolean> cb
    ) {
        executor.execute(() -> {
            boolean value = booleanSettings.get(key).getValue(prefs);
            handler.post(() -> { cb.accept(value); });
        });
    }

    private boolean getBooleanSettingSync(String key) {
        return booleanSettings.get(key).getValue(prefs);
    }

    private void setBooleanSetting(String key, boolean value) {
        setBooleanSetting(key, value, null);
    }

    private void setBooleanSetting(
        String key, boolean value, Runnable cb
    ) {
        executor.execute(() -> {
            setBooleanSettingSync(key, value);
            if (cb != null)
                handler.post(cb);
        });
    }

    private void setBooleanSettingSync(String key, boolean value) {
        booleanSettings.get(key).setValue(prefs, value);
    }

    private void getFloatSetting(String key, Consumer<Float> cb) {
        executor.execute(() -> {
            float value = floatSettings.get(key).getValue(prefs);
            handler.post(() -> { cb.accept(value); });
        });
    }

    private void setFloatSetting(String name, float value) {
        executor.execute(() -> {
            prefs.edit().putFloat(name, value).apply();
        });
    }

    private void getIntegerSetting(String key, Consumer<Integer> cb) {
        executor.execute(() -> {
            int value = integerSettings.get(key).getValue(prefs);
            handler.post(() -> { cb.accept(value); });
        });
    }

    private void setIntegerSetting(String key, int value) {
        executor.execute(() -> {
            integerSettings.get(key).setValue(prefs, value);
        });
    }

    private void getLongSetting(String key, Consumer<Long> cb) {
        executor.execute(() -> {
            long value = getLongSettingSync(key);
            handler.post(() -> { cb.accept(value); });
        });
    }

    private void setLongSetting(String key, long value) {
        executor.execute(() -> {
            setLongSettingSync(key, value);
        });
    }

    /**
     * Get pref synchronously, avoid on main thread
     */
    private long getLongSettingSync(String key) {
        return longSettings.get(key).getValue(prefs);
    }

    /**
     * Set pref synchronously, avoid on main thread
     */
    private void setLongSettingSync(String key, long value) {
        longSettings.get(key).setValue(prefs, value);
    }

    private void getStringSetting(String key, Consumer<String> cb) {
        executor.execute(() -> {
            String value = stringSettings.get(key).getValue(prefs);
            handler.post(() -> { cb.accept(value); });
        });
    }

    private String getStringSettingSync(String key) {
        return stringSettings.get(key).getValue(prefs);
    }

    private void setStringSetting(String key, String value) {
        executor.execute(() -> {
            setStringSettingSync(key, value);
        });
    }

    private void setStringSettingSync(String key, String value) {
        stringSettings.get(key).setValue(prefs, value);
    }
    private Set<String> getStringSetSettingSync(String key) {
        return stringSetSettings.get(key).getValue(prefs);
    }

    private void setStringSetSettingSync(String key, Set<String> value) {
        stringSetSettings.get(key).setValue(prefs, value);
    }

    private void updateLiveBrowseSort() {
        if (liveBrowseSort != null)
            getBrowseSort(value -> { liveBrowseSort.setValue(value); });
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

    private LiveData<Boolean> liveBooleanSetting(String key) {
        return booleanSettings.get(key).getCreateLiveData(prefs);
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
                getBooleanSettingSync(PREF_KEYBOARD_COMPACT),
                getBooleanSettingSync(PREF_KEYBOARD_HAPTIC),
                getBooleanSettingSync(PREF_KEYBOARD_HIDE_BUTTON),
                getFSEnumSettingSync(keyboardLayoutSetting),
                getFSEnumSettingSync(keyboardModeSetting),
                getIntegerFromStringSettingSync(
                    PREF_KEYBOARD_REPEAT_DELAY, 300
                ),
                getIntegerFromStringSettingSync(
                    PREF_KEYBOARD_REPEAT_INTERVAL, 75
                ),
                getBooleanSettingSync(PREF_KEYBOARD_NATIVE)
            );

            handler.post(() -> { cb.accept(settings); });
        });
    }

    private int getIntegerFromStringSettingSync(
        String name, int fallbackValue
    ) {
        try {
            return Integer.parseInt(getStringSettingSync(name));
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

    private <T extends Enum<T> & EnumSetting> void getFSEnumSetting(
        FSEnumSetting<T> setting, Consumer<T> cb
    ) {
        executor.execute(() -> {
            T value = getFSEnumSettingSync(setting);
            handler.post(() -> { cb.accept(value); });
        });
    }

    private <T extends Enum<T> & EnumSetting>
    T getFSEnumSettingSync(FSEnumSetting<T> setting) {
        return setting.getValue(prefs);
    }

    private <T extends Enum<T> & EnumSetting>
    MutableLiveData<T> liveFSEnumSetting(FSEnumSetting<T> setting) {
        return setting.getCreateLiveData(prefs);
    }

    private <T extends Enum<T> & EnumSetting>
    void setFSEnumSetting(FSEnumSetting<T> setting, T value) {
        setFSEnumSetting(setting, value, null);
    }

    private <T extends Enum<T> & EnumSetting>
    void setFSEnumSetting(FSEnumSetting<T> setting, T value, Runnable cb) {
        executor.execute(() -> {
            setFSEnumSettingSync(setting, value);
            if (cb != null)
                handler.post(cb);
        });
    }

    private <T extends Enum<T> & EnumSetting>
    void setFSEnumSettingSync(FSEnumSetting<T> setting, T value) {
        setting.setValue(prefs, value);
    }

    private Iterator<? extends BaseSetting<?, ?>> iterateSettings() {
        return allSettings.stream()
            .flatMap(c -> c.values().stream())
            .iterator();
    }

    private BaseSetting<?, ?> getSetting(String key) {
        for (var settings : allSettings) {
            BaseSetting<?, ?> s = settings.get(key);
            if (s != null)
                return s;
        }
        return null;
    }

    /**
     * Generic setting
     *
     * TFront - the "front end" type of the setting
     * TBack - the type actually written/read from shared prefs
     *
     * TBack only used for settings import/export
     */
    private static abstract class BaseSetting<TFront, TBack> {
        private String key;
        private TFront defaultValue;
        private Function<TBack, Boolean> validator;

        private MutableLiveData<TFront> liveData;

        public BaseSetting(String key, TFront defaultValue) {
            this(key, defaultValue, null);
        }

        /**
         * Construct a setting
         *
         * @param key the name of the setting
         * @param defaultValue the default value
         * @param validator validate a (back-end) value before entering
         */
        public BaseSetting(
            String key,
            TFront defaultValue,
            Function<TBack, Boolean> validator
        ) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.validator = validator;
        }

        public String getKey() { return key; }
        public TFront getDefaultValue() { return defaultValue; }

        /**
         * Returns value or default if error or null
         */
        public TFront getValue(SharedPreferences prefs) {
            try {
                TFront value = getValueUnchecked(prefs);
                return value == null ? getDefaultValue() : value;
            } catch (ClassCastException e) {
                return defaultValue;
            }
        }

        public abstract void setValue(SharedPreferences prefs, TFront value);

        public MutableLiveData<TFront> getCreateLiveData(
            SharedPreferences prefs
        ) {
            if (liveData == null) {
                liveData = new MutableLiveData<>();
                updateLiveData(prefs);
            }
            return liveData;
        }

        public void updateLiveData(SharedPreferences prefs) {
            if (liveData != null) {
                liveData.setValue(getValue(prefs));
            }
        }

        /**
         * Add to JSON if not a null value
         */
        public void addToJSON(
            SharedPreferences prefs, JSONObject json
        ) throws JSONException {
            TBack value = getValueBackUnchecked(prefs);
            if (value != null)
                json.put(getKey(), value);
        }

        /**
         * Get value from JSON object
         *
         * Do nothing if not there, wrong type, fails validation, or
         * other error
         */
        public void getFromJSON(
            SharedPreferences prefs, JSONObject json
        ) {
            try {
                if (json.has(getKey()))
                    getFromJSONUnchecked(prefs, json);
            } catch (JSONException e) {
                // pass
            }
        }

        /**
         * Get value from settings, don't check for cast errors
         */
        protected abstract TFront getValueUnchecked(SharedPreferences prefs);

        /**
         * Get value from settings, don't check for cast errors
         */
        protected abstract TBack getValueBackUnchecked(
            SharedPreferences prefs
        );

        protected void setValueBack(SharedPreferences prefs, TBack value) {
            if (validator == null || validator.apply(value))
                setValueBackUnchecked(prefs, value);
        }

        /**
         * Set backend value without checking validation
         */
        protected abstract void setValueBackUnchecked(
            SharedPreferences prefs, TBack value
        );

        /**
         * Get value from JSON object don't check errors
         */
        protected abstract void getFromJSONUnchecked(
            SharedPreferences prefs, JSONObject json
        ) throws JSONException;
    }

    /**
     * Setting where front/backend type are the same
     *
     * Most settings are these
     */
    private static abstract class Setting<T> extends BaseSetting<T, T> {
        public Setting(String key, T defaultValue) {
            this(key, defaultValue, null);
        }

        public Setting(
            String key,
            T defaultValue,
            Function<T, Boolean> validator
        ) {
            super(key, defaultValue, validator);
        }

        @Override
        public void setValue(SharedPreferences prefs, T value) {
            setValueBack(prefs, value);
        }

        @Override
        protected T getValueUnchecked(SharedPreferences prefs) {
            return getValueBackUnchecked(prefs);
        }
    }
}
