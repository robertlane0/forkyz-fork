
package app.crossword.yourealwaysbe.settings;

import android.content.Context;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences;
import android.os.Handler;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

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

import app.crossword.yourealwaysbe.net.Downloader;
import app.crossword.yourealwaysbe.net.Downloaders;
import app.crossword.yourealwaysbe.util.files.Accessor;

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

    public static final int BROWSE_SWIPE_DELETE = 0;
    public static final int BROWSE_SWIPE_ARCHIVE = 1;

    // TODO: convert to record class when possible!
    // https://issuetracker.google.com/issues/197081367
    public static class DownloadersSettings {
        private boolean downloadDeStandaard;
        private boolean downloadDeTelegraaf;
        private boolean downloadGuardianDailyCryptic;
        private boolean downloadHamAbend;
        private boolean downloadIndependentDailyCryptic;
        private boolean downloadJonesin;
        private boolean downloadJoseph;
        private boolean download20Minutes;
        private boolean downloadLeParisienF1;
        private boolean downloadLeParisienF2;
        private boolean downloadLeParisienF3;
        private boolean downloadLeParisienF4;
        private boolean downloadNewsday;
        private boolean downloadPremier;
        private boolean downloadSheffer;
        private boolean downloadUniversal;
        private boolean downloadUSAToday;
        private boolean downloadWaPoSunday;
        private boolean downloadWsj;
        private boolean scrapeCru;
        private boolean scrapeKegler;
        private boolean scrapePrivateEye;
        private boolean scrapePrzekroj;
        private boolean downloadCustomDaily;
        private String customDailyTitle;
        private String customDailyUrl;
        private boolean suppressSummaryNotifications;
        private boolean suppressIndividualNotifications;
        private Set<String> autoDownloaders;
        private int downloadTimeout;
        private boolean downloadOnStartup;

        public DownloadersSettings(
            boolean downloadDeStandaard,
            boolean downloadDeTelegraaf,
            boolean downloadGuardianDailyCryptic,
            boolean downloadHamAbend,
            boolean downloadIndependentDailyCryptic,
            boolean downloadJonesin,
            boolean downloadJoseph,
            boolean download20Minutes,
            boolean downloadLeParisienF1,
            boolean downloadLeParisienF2,
            boolean downloadLeParisienF3,
            boolean downloadLeParisienF4,
            boolean downloadNewsday,
            boolean downloadPremier,
            boolean downloadSheffer,
            boolean downloadUniversal,
            boolean downloadUSAToday,
            boolean downloadWaPoSunday,
            boolean downloadWsj,
            boolean scrapeCru,
            boolean scrapeKegler,
            boolean scrapePrivateEye,
            boolean scrapePrzekroj,
            boolean downloadCustomDaily,
            String customDailyTitle,
            String customDailyUrl,
            boolean suppressSummaryNotifications,
            boolean suppressIndividualNotifications,
            Set<String> autoDownloaders,
            int downloadTimeout,
            boolean downloadOnStartup
        ) {
            this.downloadDeStandaard = downloadDeStandaard;
            this.downloadDeTelegraaf = downloadDeTelegraaf;
            this.downloadGuardianDailyCryptic = downloadGuardianDailyCryptic;
            this.downloadHamAbend = downloadHamAbend;
            this.downloadIndependentDailyCryptic
                = downloadIndependentDailyCryptic;
            this.downloadJonesin = downloadJonesin;
            this.downloadJoseph = downloadJoseph;
            this.download20Minutes = download20Minutes;
            this.downloadLeParisienF1 = downloadLeParisienF1;
            this.downloadLeParisienF2 = downloadLeParisienF2;
            this.downloadLeParisienF3 = downloadLeParisienF3;
            this.downloadLeParisienF4 = downloadLeParisienF4;
            this.downloadNewsday = downloadNewsday;
            this.downloadPremier = downloadPremier;
            this.downloadSheffer = downloadSheffer;
            this.downloadUniversal = downloadUniversal;
            this.downloadUSAToday = downloadUSAToday;
            this.downloadWaPoSunday = downloadWaPoSunday;
            this.downloadWsj = downloadWsj;
            this.scrapeCru = scrapeCru;
            this.scrapeKegler = scrapeKegler;
            this.scrapePrivateEye = scrapePrivateEye;
            this.scrapePrzekroj = scrapePrzekroj;
            this.downloadCustomDaily = downloadCustomDaily;
            this.customDailyTitle = customDailyTitle;
            this.customDailyUrl = customDailyUrl;
            this.suppressSummaryNotifications = suppressSummaryNotifications;
            this.suppressIndividualNotifications
                = suppressIndividualNotifications;
            this.autoDownloaders = autoDownloaders;
            this.downloadTimeout = downloadTimeout;
            this.downloadOnStartup = downloadOnStartup;
        }

        public boolean getDownloadDeStandaard() { return downloadDeStandaard; }
        public boolean getDownloadDeTelegraaf() { return downloadDeTelegraaf; }
        public boolean getDownloadGuardianDailyCryptic() {
            return downloadGuardianDailyCryptic;
        }
        public boolean getDownloadHamAbend() { return downloadHamAbend; }
        public boolean getDownloadIndependentDailyCryptic() {
            return downloadIndependentDailyCryptic;
        }
        public boolean getDownloadJonesin() { return downloadJonesin; }
        public boolean getDownloadJoseph() { return downloadJoseph; }
        public boolean getDownload20Minutes() { return download20Minutes; }
        public boolean getDownloadLeParisienF1() {
            return downloadLeParisienF1;
        }
        public boolean getDownloadLeParisienF2() {
            return downloadLeParisienF2;
        }
        public boolean getDownloadLeParisienF3() {
            return downloadLeParisienF3;
        }
        public boolean getDownloadLeParisienF4() {
            return downloadLeParisienF4;
        }
        public boolean getDownloadNewsday() { return downloadNewsday; }
        public boolean getDownloadPremier() { return downloadPremier; }
        public boolean getDownloadSheffer() { return downloadSheffer; }
        public boolean getDownloadUniversal() { return downloadUniversal; }
        public boolean getDownloadUSAToday() { return downloadUSAToday; }
        public boolean getDownloadWaPoSunday() { return downloadWaPoSunday; }
        public boolean getDownloadWsj() { return downloadWsj; }
        public boolean getScrapeCru() { return scrapeCru; }
        public boolean getScrapeKegler() { return scrapeKegler; }
        public boolean getScrapePrivateEye() { return scrapePrivateEye; }
        public boolean getScrapePrzekroj() { return scrapePrzekroj; }
        public boolean getDownloadCustomDaily() { return downloadCustomDaily; }
        public String getCustomDailyTitle() { return customDailyTitle; }
        public String getCustomDailyUrl() { return customDailyUrl; }
        public boolean getSuppressSummaryNotifications() {
            return suppressSummaryNotifications;
        }
        public boolean getSuppressIndividualNotifications() {
            return suppressIndividualNotifications;
        }
        public Set<String> getAutoDownloaders() { return autoDownloaders; }
        public int getDownloadTimeout() { return downloadTimeout; }
        public boolean getDownloadOnStartup() { return downloadOnStartup; }

        public boolean isNotificationPermissionNeeded() {
            return !(
                getSuppressSummaryNotifications()
                && getSuppressIndividualNotifications()
            );
        }
    }

    // public for settings page
    public static final String PREF_AUTO_DOWNLOADERS = "autoDownloaders";
    private static final String PREF_BROWSE_CLEANUP_AGE = "cleanupAge";
    private static final String PREF_BROWSE_CLEANUP_AGE_ARCHIVE
        = "archiveCleanupAge";
    private static final String PREF_BROWSE_DELETE_ON_CLEANUP
        = "deleteOnCleanup";
    private static final String PREF_BROWSE_DISABLE_SWIPE = "disableSwipe";
    private static final String PREF_BROWSE_LAST_DOWNLOAD = "dlLast";
    private static final String PREF_BROWSE_LAST_SEEN_VERSION
        = "lastSeenVersion";
    private static final String PREF_BROWSE_SORT = "sort";
    private static final String PREF_BROWSE_SWIPE_ACTION = "swipeAction";

    private static final String PREF_PLAY_DONT_DELETE_CROSSING
        = "dontDeleteCrossing";
    private static final String
    PREF_PLAY_PRESERVE_CORRECT_LETTERS_IN_SHOW_ERRORS
        = "preserveCorrectLettersInShowErrors";
    private static final String PREF_PLAY_SHOW_COUNT = "showCount";


    private static final String PREF_SUPPRESS_SUMMARY_NOTIFICATIONS
        = "supressSummaryMessages";
    private static final String PREF_SUPPRESS_INDIVIDUAL_NOTIFICATIONS
        = "supressMessages";

    private static final Map<String, Boolean> BOOL_DEFAULTS = new HashMap<>();
    private static final Map<String, Integer> INT_DEFAULTS = new HashMap<>();
    private static final Map<String, Long> LONG_DEFAULTS = new HashMap<>();
    private static final Map<String, String> STRING_DEFAULTS = new HashMap<>();
    static {
        STRING_DEFAULTS.put(PREF_BROWSE_CLEANUP_AGE, "-1");
        STRING_DEFAULTS.put(PREF_BROWSE_CLEANUP_AGE_ARCHIVE, "-1");
        BOOL_DEFAULTS.put(PREF_BROWSE_DELETE_ON_CLEANUP, false);
        BOOL_DEFAULTS.put(PREF_BROWSE_DISABLE_SWIPE, false);
        LONG_DEFAULTS.put(PREF_BROWSE_LAST_DOWNLOAD, 0L);
        STRING_DEFAULTS.put(PREF_BROWSE_LAST_SEEN_VERSION, "");
        INT_DEFAULTS.put(PREF_BROWSE_SORT, 0);
        STRING_DEFAULTS.put(PREF_BROWSE_SWIPE_ACTION, "DELETE");
        BOOL_DEFAULTS.put(PREF_PLAY_DONT_DELETE_CROSSING, false);
        BOOL_DEFAULTS.put(
            PREF_PLAY_PRESERVE_CORRECT_LETTERS_IN_SHOW_ERRORS, false
        );
        BOOL_DEFAULTS.put(PREF_PLAY_SHOW_COUNT, false);
    }

    private SharedPreferences prefs;
    private Handler handler;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private MutableLiveData<Accessor> liveBrowseSort;
    private Map<String, MutableLiveData<Boolean>> liveBooleans = new HashMap<>();

    private OnSharedPreferenceChangeListener prefChangeListener
        = (prefs, key) -> {
            if (PREF_BROWSE_SORT.equals(key)) {
                updateLiveBrowseSort();
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

    public void getBrowseCleanupAge(Consumer<String> cb) {
        getStringPreference(PREF_BROWSE_CLEANUP_AGE, cb);
    }

    public void getBrowseCleanupAgeArchive(Consumer<String> cb) {
        getStringPreference(PREF_BROWSE_CLEANUP_AGE_ARCHIVE, cb);
    }

    public void getBrowseDeleteOnCleanup(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_BROWSE_DELETE_ON_CLEANUP, cb);
    }

    public void getBrowseDisableSwipe(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_BROWSE_DISABLE_SWIPE, cb);
    }

    public LiveData<Boolean> liveBrowseDisableSwipe() {
        return getLiveBoolean(PREF_BROWSE_DISABLE_SWIPE);
    }

    public void getBrowseLastDownload(Consumer<Long> cb) {
        getLongPreference(PREF_BROWSE_LAST_DOWNLOAD, cb);
    }

    public void setBrowseLastDownload(long value) {
        setLongPreference(PREF_BROWSE_LAST_DOWNLOAD, value);
    }

    public void getBrowseLastSeenVersion(Consumer<String> cb) {
        getStringPreference(PREF_BROWSE_LAST_SEEN_VERSION, cb);
    }

    public void setBrowseLastSeenVersion(String value) {
        setStringPreference(PREF_BROWSE_LAST_SEEN_VERSION, value);
    }

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

    public void getBrowseSwipeAction(Consumer<Integer> cb) {
        getStringPreference(PREF_BROWSE_SWIPE_ACTION, value -> {
            cb.accept("DELETE".equals(value)
                ? BROWSE_SWIPE_DELETE
                : BROWSE_SWIPE_ARCHIVE
            );
        });
    }

    public void getPlayDontDeleteCrossing(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_DONT_DELETE_CROSSING, cb);
    }

    public void getPlayPreserveCorrectLettersInShowErrors(
        Consumer<Boolean> cb
    ) {
        getBooleanPreference(
            PREF_PLAY_PRESERVE_CORRECT_LETTERS_IN_SHOW_ERRORS, cb
        );
    }

    public void getPlayShowCount(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_PLAY_SHOW_COUNT, cb);
    }

    public void setPlayShowCount(boolean value) {
        setBooleanPreference(PREF_PLAY_SHOW_COUNT, value);
    }

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
                prefs.getBoolean("downloadNewsday", true),
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
                Integer.valueOf(prefs.getString("downloadTimeout", "30000")),
                prefs.getBoolean("dlOnStartup", false)
            );
            handler.post(() -> { cb.accept(settings); });
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
        executor.execute(() -> {
            prefs.edit().putBoolean(name, value).apply();
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
        final Long defaultValue = LONG_DEFAULTS.get(name);
        if (defaultValue == null) {
            throw new IllegalArgumentException(
                "Unrecognised Long preference: " + name
            );
        }

        executor.execute(() -> {
            long value = prefs.getLong(name, defaultValue);
            handler.post(() -> { cb.accept(value); });
        });
    }

    private void setLongPreference(String name, long value) {
        executor.execute(() -> {
            prefs.edit().putLong(name, value).apply();
        });
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

    private LiveData<Boolean> getLiveBoolean(String name) {
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
}
