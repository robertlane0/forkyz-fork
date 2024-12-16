package app.crossword.yourealwaysbe.forkyz;

import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import com.google.android.material.color.DynamicColors;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.net.Downloader;
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.util.BackgroundDownloadManager;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;

import java.time.format.DateTimeFormatter;
import javax.inject.Inject;

public class PreferencesSubPages {

    public static class SourcesFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_sources, rootKey);
        }
    }

    @AndroidEntryPoint
    public static class DailyFragment
            extends PreferencesBaseFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Inject
        protected ForkyzSettings settings;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_daily, rootKey);
        }

        @Override
        public void onResume() {
            PreferenceManager
                .getDefaultSharedPreferences(getActivity().getApplicationContext())
                .registerOnSharedPreferenceChangeListener(this);
            setCustomError();
            super.onResume();
        }

        @Override
        public void onPause() {
            PreferenceManager
                .getDefaultSharedPreferences(getActivity().getApplicationContext())
                .unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String pref
        ) {
            if (settings.isDownloadCustomDaily(pref))
                setCustomError();
        }

        private void setCustomError() {
            Preference errorMsg = findPreference(
                ForkyzSettings.PREF_DOWNLOAD_CUSTOM_DAILY_URL
            );
            if (errorMsg == null)
                return;

            errorMsg.setSummary(
                getResources().getString(R.string.custom_url_desc)
            );

            settings.getDownloadCustomDaily(downloadCustom -> {
                if (downloadCustom) {
                    settings.getDownloadCustomDailyURL(url -> {
                        try {
                            DateTimeFormatter.ofPattern(url);
                        } catch (IllegalArgumentException e) {
                            errorMsg.setSummary(
                                getResources().getString(
                                    R.string.custom_url_desc_with_error,
                                    e.getMessage()
                                )
                            );
                            errorMsg.setVisible(true);
                        }
                    });
                }
            });
        }
    }

    public static class WeeklyFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_weekly, rootKey);
        }
    }

    public static class ScraperFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.preferences_scrapers, rootKey);

            findPreference("aboutScrapes")
                    .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference arg0) {
                        Intent i = new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("scrapes.html"),
                            getActivity(),
                            HTMLActivity.class
                        );
                        getActivity().startActivity(i);
                        return true;
                    }
                });
        }
    }

    @AndroidEntryPoint
    public static class DownloadFragment extends PreferencesBaseFragment {

        @Inject
        protected AndroidVersionUtils utils;

        @Inject
        protected DownloadersProvider downloadersProvider;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_download, rootKey);
            setAvailableDownloaders();
        }

        private void setAvailableDownloaders() {
            downloadersProvider.get(dls -> {
                List<Downloader> downloaders = dls.getDownloaders();

                int len = downloaders.size();
                CharSequence[] values = new CharSequence[len];
                CharSequence[] labels = new CharSequence[len];

                // done this way because i know the downloaders list is a linked
                // list
                int index = 0;
                for (Downloader downloader : downloaders) {
                    values[index] = downloader.getInternalName();
                    labels[index] = downloader.getName();
                    index += 1;
                }

                MultiSelectListPreference available = findPreference(
                    ForkyzSettings.PREF_AUTO_DOWNLOADERS
                );
                available.setEntries(labels);
                available.setEntryValues(values);
            });
        }
    }

    @AndroidEntryPoint
    public static class DownloadBackgroundFragment
            extends PreferencesBaseFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Inject
        protected ForkyzSettings settings;

        @Inject
        BackgroundDownloadManager downloadManager;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(
                R.xml.preferences_download_background, rootKey
            );
        }

        @Override
        public void onResume() {
            PreferenceManager
                .getDefaultSharedPreferences(getActivity().getApplicationContext())
                .registerOnSharedPreferenceChangeListener(this);
            super.onResume();
        }

        @Override
        public void onPause() {
            PreferenceManager
                .getDefaultSharedPreferences(getActivity().getApplicationContext())
                .unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String pref
        ) {
            if (settings.isBackgroundDownloadConfigPref(pref))
                downloadManager.updateBackgroundDownloads();
        }
    }

    public static class BrowserFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_browser, rootKey);
        }
    }

    public static class KeyboardFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_keyboard, rootKey);
        }
    }

    public static class InteractionFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_interaction, rootKey);
        }
    }

    public static class VoiceFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_voice, rootKey);

            findPreference("aboutVoiceCommands")
                    .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference arg0) {
                        Intent i = new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("voice_commands.html"),
                            getActivity(),
                            HTMLActivity.class
                        );
                        getActivity().startActivity(i);
                        return true;
                    }
                });
        }
    }

    public static class DisplayFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_display, rootKey);
            if (!DynamicColors.isDynamicColorAvailable()) {

                ListPreference themePref
                    = findPreference(ForkyzSettings.PREF_APP_THEME);

                Resources res = getResources();
                themePref.setEntries(
                    res.getStringArray(R.array.themeTypeLabelsNoDynamic)
                );
                themePref.setEntryValues(
                    res.getStringArray(R.array.themeTypeValuesNoDynamic)
                );
            }
        }
    }

    public static class ExternalToolsFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_external, rootKey);
        }
    }

    @AndroidEntryPoint
    public static class ExportImportFragment extends PreferencesBaseFragment {
        private static final String MIME_TYPE = "application/json";
        private static final String DEFAULT_FILENAME = "forkyz_settings.json";

        private PreferencesActivityViewModel model;

        // set in onCreate
        ActivityResultLauncher<String> getImportURI = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null)
                        model.importSettings(uri);
                }
            }
        );

        ActivityResultLauncher<String> getExportURI = registerForActivityResult(
            new ActivityResultContracts.CreateDocument(MIME_TYPE),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null)
                        model.exportSettings(uri);
                }
            }
        );

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(
                R.xml.preferences_export_import, rootKey
            );

            model = new ViewModelProvider(requireActivity()).get(
                PreferencesActivityViewModel.class
            );

            findPreference("preferencesImport").setOnPreferenceClickListener(
                pref -> {
                    getImportURI.launch(MIME_TYPE);
                    return true;
                }
            );

            findPreference("preferencesExport").setOnPreferenceClickListener(
                pref -> {
                    getExportURI.launch(DEFAULT_FILENAME);
                    return true;
                }
            );
        }
    }
}
