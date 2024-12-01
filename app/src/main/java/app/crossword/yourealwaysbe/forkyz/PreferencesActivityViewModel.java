
package app.crossword.yourealwaysbe.forkyz;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
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
    private Handler handler = new Handler(Looper.getMainLooper());

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
        if (importURI != null) {
            executorService.execute(() -> {
                doImportSettings(importURI);
            });
        }
    }

    /**
     * Export settings in background thread
     */
    public void exportSettings(Uri exportURI) {
        if (exportURI != null) {
            executorService.execute(() -> {
                doExportSettings(exportURI);
            });
        }
    }

    /**
     * Import settings in current thread
     */
    private void doImportSettings(Uri importURI) {
        Application app = getApplication();
        try (
            InputStream is = app.getContentResolver()
                .openInputStream(importURI)
        ) {
            settings.importSettings(is);
            handler.post(() -> {
                Toast.makeText(
                    app,
                    app.getString(R.string.preferences_imported),
                    Toast.LENGTH_SHORT
                ).show();
            });
        } catch (IOException e) {
            handler.post(() -> {
                Toast.makeText(
                    app,
                    app.getString(R.string.preferences_import_failed),
                    Toast.LENGTH_SHORT
                ).show();
            });
        }
    }

    /**
     * Export settings in current thread
     */
    private void doExportSettings(Uri exportURI) {
        Application app = getApplication();
        try (
            OutputStream os = app.getContentResolver()
                .openOutputStream(exportURI)
        ) {
            settings.exportSettings(os);
            handler.post(() -> {
                Toast.makeText(
                    app,
                    app.getString(R.string.preferences_exported),
                    Toast.LENGTH_SHORT
                ).show();
            });
        } catch (IOException e) {
            handler.post(() -> {
                Toast.makeText(
                    app,
                    app.getString(R.string.preferences_export_failed),
                    Toast.LENGTH_SHORT
                ).show();
            });
        }
    }
}
