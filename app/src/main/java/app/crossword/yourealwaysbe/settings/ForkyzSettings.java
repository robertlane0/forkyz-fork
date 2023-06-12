
package app.crossword.yourealwaysbe.settings;

import android.content.Context;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.util.files.Accessor;

/**
 * Async settings
 *
 * The get methods have a callback that is called after prefs read
 * off-thread.
 *
 * The live data methods can be used to observe certain settings.
 *
 * Use getInstance (singleton pattern)
 */
public class ForkyzSettings {
    private static ForkyzSettings instance = null;

    /**
     * Get settings instance
     */
    public static ForkyzSettings getSettings() {
        if (instance != null)
            return instance;
        Context context = ForkyzApplication.getInstance();
        instance = new ForkyzSettings(
            PreferenceManager.getDefaultSharedPreferences(context),
            new Handler(context.getMainLooper())
        );
        return instance;
    }

    private static final String PREF_BROWSE_SORT = "sort";
    private static final String PREF_SHOW_COUNT = "showCount";

    private static final Map<String, Boolean> BOOL_DEFAULTS = new HashMap<>();
    private static final Map<String, Integer> INT_DEFAULTS = new HashMap<>();
    static {
        BOOL_DEFAULTS.put(PREF_SHOW_COUNT, false);
        INT_DEFAULTS.put(PREF_BROWSE_SORT, 0);
    }

    private SharedPreferences prefs;
    private Handler handler;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private MutableLiveData<Accessor> liveBrowseSort;

    private OnSharedPreferenceChangeListener prefChangeListener
        = (prefs, key) -> {
            if (PREF_BROWSE_SORT.equals(key)) {
                updateLiveBrowseSort();
            }
        };

    /**
     * Package level for testing only
     */
    ForkyzSettings(SharedPreferences prefs, Handler handler) {
        this.prefs = prefs;
        this.handler = handler;
        this.prefs.registerOnSharedPreferenceChangeListener(prefChangeListener);
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

    public void getShowCount(Consumer<Boolean> cb) {
        getBooleanPreference(PREF_SHOW_COUNT, cb);
    }

    public void setShowCount(boolean value) {
        setBooleanPreference(PREF_SHOW_COUNT, value);
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

    private void updateLiveBrowseSort() {
        if (liveBrowseSort != null)
            getBrowseSort(value -> { liveBrowseSort.setValue(value); });
    }
}
