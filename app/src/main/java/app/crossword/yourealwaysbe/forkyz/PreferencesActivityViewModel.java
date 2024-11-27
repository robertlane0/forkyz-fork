
package app.crossword.yourealwaysbe.forkyz;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

import android.app.Application;
import android.net.Uri;
import androidx.lifecycle.AndroidViewModel;

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;

@HiltViewModel
public class PreferencesActivityViewModel extends AndroidViewModel {
    private static final Logger LOGGER = Logger.getLogger(
        PreferencesActivityViewModel.class.getCanonicalName()
    );

    // important that it is single thread to avoid multiple
    // simultaneous operations
    private ExecutorService executorService
        = Executors.newSingleThreadExecutor();

    private ForkyzSettings settings;

    @Inject
    public PreferencesActivityViewModel(
        Application application,
        ForkyzSettings settings
    ) {
        super(application);
        this.settings = settings;
    }

    /**
     * Import settings in background thread
     */
    public void importSettings(Uri importURI) {
        executorService.execute(() -> {
            doImportSettings(importURI);
        });
    }

    /**
     * Export settings in background thread
     */
    public void exportSettings(Uri exportURI) {
        executorService.execute(() -> {
            doExportSettings(exportURI);
        });
    }

    /**
     * Import settings in current thread
     */
    private void doImportSettings(Uri importURI) {
        // TODO
    }

    /**
     * Export settings in current thread
     */
    private void doExportSettings(Uri exportURI) {
        // TODO
    }}
