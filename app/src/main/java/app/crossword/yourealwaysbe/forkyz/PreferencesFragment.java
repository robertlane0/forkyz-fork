package app.crossword.yourealwaysbe.forkyz;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.Preference;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;

import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation;
import app.crossword.yourealwaysbe.forkyz.util.ExitActivity;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerSAF;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;

@AndroidEntryPoint
public class PreferencesFragment extends PreferencesBaseFragment {

    @Inject
    protected AndroidVersionUtils utils;

    @Inject
    protected ForkyzSettings settings;

    @Inject
    protected FileHandlerProvider fileHandlerProvider;

    private Handler handler;

    // set in onCreate
    ActivityResultLauncher<Uri> getSAFURI;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSAFURI = utils.registerForSAFUriResult(this, (uri) -> {
            onNewExternalStorageSAFURI(uri);
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        handler = new Handler(context.getMainLooper());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        handler = null;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);

        findPreference(ForkyzSettings.PREF_FILE_HANDLER_STORAGE_LOC)
            .setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(
                        Preference preference, Object newValue
                    ) {
                        onStorageLocationChange((String) newValue);
                        return false;
                    }
                }
            );

        setStorageOptions();

        findPreference("releaseNotes")
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference arg0) {
                    Activity activity = getActivity();
                    if (activity == null)
                        return false;

                    Intent i = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("release.html"),
                        activity,
                        HTMLActivity.class
                    );
                    activity.startActivity(i);

                    return true;
                }
            });

        findPreference("license")
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference arg0) {
                    Activity activity = getActivity();
                    if (activity == null)
                        return false;

                    Intent i = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("license.html"),
                        activity,
                        HTMLActivity.class
                    );
                    activity.startActivity(i);
                    return true;
                }
            });
    }

    @Override
    public void onResume() {
        setStorageOptions();
        super.onResume();
    }

    /**
     * Called when the use selects a storage location
     *
     * If they selected external storage (Storage Access Framework),
     * then they may be prompted to select a directory. This happens if
     * they have not already set the directory up, they are
     * reselecting the same option, suggesting they want to change it,
     * or there is a problem with the current one.
     *
     * @return true if change should be committed to prefs
     */
    private void onStorageLocationChange(String newValue) {
        settings.getFileHandlerSettings(fileHandlerSettings -> {
            Activity activity = getActivity();
            if (activity == null)
                return;

            StorageLocation newLocation
                = StorageLocation.fromSettingsValue(newValue);

            boolean isSAF = StorageLocation.EXTERNAL_SAF.equals(newLocation);

            if (isSAF) {
                FileHandlerSAF fileHandler
                    = FileHandlerSAF.readHandlerFromSettings(
                        activity.getApplicationContext(), fileHandlerSettings
                    );

                if (fileHandler == null) {
                    if (handler != null) {
                        handler.post(() -> {
                            Toast t = Toast.makeText(
                                activity,
                                R.string.storage_select_saf_info,
                                Toast.LENGTH_LONG
                            );
                            t.show();
                            if (getSAFURI != null)
                                getSAFURI.launch(null);
                        });
                    }
                } else {
                    settings.setFileHandlerSettings(
                        fileHandler.getSettings(),
                        () -> { newFileHandlerAppExit(); }
                    );
                }
            } else {
                // keep old URIs so they can be reselected in future
                settings.setFileHandlerSettings(
                    new FileHandlerSettings(
                        newLocation,
                        fileHandlerSettings.getSAFRootURI(),
                        fileHandlerSettings.getSAFCrosswordsURI(),
                        fileHandlerSettings.getSAFArchiveURI(),
                        fileHandlerSettings.getSAFToImportURI(),
                        fileHandlerSettings.getSAFToImportDoneURI(),
                        fileHandlerSettings.getSAFToImportFailedURI()
                    ),
                    () -> { newFileHandlerAppExit(); }
                );
            }
        });
    }

    private void onNewExternalStorageSAFURI(Uri uri) {
        if (uri == null)
            return;

        Activity activity = getActivity();
        if (activity == null)
            return;

        FileHandlerSAF fileHandler = FileHandlerSAF.initialiseSAFForRoot(
            activity.getApplicationContext(), uri
        );

        if (fileHandler != null) {
            settings.setFileHandlerSettings(
                fileHandler.getSettings(),
                () -> { newFileHandlerAppExit(); }
            );
        } else {
            if (handler != null) {
                handler.post(() -> {
                    Toast t = Toast.makeText(
                        activity,
                        R.string.failed_to_initialise_saf,
                        Toast.LENGTH_LONG
                    );
                    t.show();
                });
            }
        }
    }

    private void newFileHandlerAppExit() {
        fileHandlerProvider.reloadFileHandler();
        if (handler != null) {
            StorageChangedDialog dialog = new StorageChangedDialog();
            dialog.show(
                getParentFragmentManager(), "StorageChangedDialog"
            );
        }
    }

    private void setStorageOptions() {
        settings.getFileHandlerSettings(fhSettings -> {
            CharSequence[] entries = new CharSequence[2];
            CharSequence[] values = new CharSequence[2];

            entries[0] = getString(R.string.internal_storage);
            values[0] = StorageLocation.INTERNAL.getSettingsValue();

            if (FileHandlerSAF.isSAFSupported(utils)) {
                String storageLocationSAFURI = fhSettings.getSAFRootURI();
                if (storageLocationSAFURI == null) {
                    storageLocationSAFURI =
                        getString(R.string.external_storage_saf_none_selected);
                }

                entries[1] =
                    getString(R.string.external_storage_saf) + " "
                        + getString(
                            R.string.external_storage_saf_current_uri,
                            storageLocationSAFURI
                        );
                values[1] = StorageLocation.EXTERNAL_SAF.getSettingsValue();
            } else {
                entries[1] = getString(R.string.external_storage_legacy);
                values[1] = StorageLocation.EXTERNAL_LEGACY.getSettingsValue();
            }

            ListPreference storageOptions
                = findPreference(ForkyzSettings.PREF_FILE_HANDLER_STORAGE_LOC);

            storageOptions.setEntries(entries);
            storageOptions.setEntryValues(values);
        });
    }

    @AndroidEntryPoint
    public static class StorageChangedDialog extends DialogFragment {

        @Inject
        @ApplicationContext
        protected Context applicationContext;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(getActivity());

            builder.setTitle(R.string.storage_changed_title)
                .setMessage(R.string.storage_changed_please_restart)
                .setPositiveButton(
                    R.string.exit,
                    (di, i) -> { exitApplication(); }
                );

            return builder.create();
        }

        @Override
        public void onCancel(DialogInterface di) {
            exitApplication();
        }

        private void exitApplication() {
            ExitActivity.exit(applicationContext);
        }
    }
}
