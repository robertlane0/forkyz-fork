package app.crossword.yourealwaysbe.forkyz.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.DatePicker;
import android.widget.TextView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.databinding.DownloadDialogBinding;
import app.crossword.yourealwaysbe.forkyz.net.Downloader;
import app.crossword.yourealwaysbe.forkyz.net.Downloaders;
import app.crossword.yourealwaysbe.forkyz.net.DummyDownloader;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Custom dialog for choosing puzzles to download.
 */
public class DownloadPickerDialogBuilder {
    private static final Logger LOGGER = Logger.getLogger(
        DownloadPickerDialogBuilder.class.getCanonicalName()
    );

    private Activity activity;
    private DownloadDialogBinding binding;
    private List<Downloader> availableDownloaders;
    private Dialog dialog;
    private Downloaders downloaders;
    private LocalDate downloadDate;
    private int selectedItemPosition = 0;

    private OnDateChangedListener dateChangedListener
        = new DatePicker.OnDateChangedListener() {
            public void onDateChanged(
                DatePicker view, int year, int monthOfYear, int dayOfMonth
            ) {
                downloadDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth);
                updateDayOfWeek();
                updatePuzzleSelect();
            }
        };

    public DownloadPickerDialogBuilder(
        Activity a,
        final OnDownloadSelectedListener downloadButtonListener,
        int year, int monthOfYear, int dayOfMonth,
        Consumer<Consumer<Downloaders>> downloadersProvider
    ) {
        this.activity = a;
        this.downloadDate = LocalDate.of(year, monthOfYear,  dayOfMonth);
        this.binding = DownloadDialogBinding.inflate(LayoutInflater.from(activity));

        updateDayOfWeek();

        binding.datePicker.init(
            year, monthOfYear - 1, dayOfMonth, dateChangedListener
        );

        binding.puzzleSelect.setOnItemSelectedListener(
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                   selectedItemPosition = position;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    selectedItemPosition = 0;
                }
            });

        OnClickListener clickHandler = new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dateChangedListener.onDateChanged(
                        binding.datePicker,
                        binding.datePicker.getYear(),
                        binding.datePicker.getMonth(),
                        binding.datePicker.getDayOfMonth()
                    );
                    downloadButtonListener.onDownloadSelected(
                        getCurrentDate(),
                        getSelectedDownloaders()
                    );
                }
            };

        MaterialAlertDialogBuilder builder
            = new MaterialAlertDialogBuilder(activity);

        builder.setView(binding.getRoot())
            .setPositiveButton(R.string.download, clickHandler)
            .setNegativeButton(R.string.cancel, null);

        dialog = builder.create();
        dialog.setOnShowListener(new OnShowListener() {
                public void onShow(DialogInterface arg0) {
                    updatePuzzleSelect();
                }
            });

        downloadersProvider.accept(downloaders -> {
            this.downloaders = downloaders;
            updatePuzzleSelect();
        });
    }

    private boolean hasDownloaders() {
        return downloaders != null;
    }

    private void updateDayOfWeek() {
        String dayName
            = downloadDate.getDayOfWeek()
                        .getDisplayName(TextStyle.FULL, Locale.getDefault());
        binding.dayOfWeek.setText(dayName);
    }

    public Dialog getInstance() {
        return dialog;
    }

	private LocalDate getCurrentDate() {
        return downloadDate;
    }

    private List<Downloader> getSelectedDownloaders() {
        if (!hasDownloaders()) {
            return Collections.emptyList();
        } else if (selectedItemPosition == 0) {
            return downloaders.getDownloaders(getCurrentDate());
        } else {
            return Collections.singletonList(
                availableDownloaders.get(selectedItemPosition)
            );
        }
    }

    private void updatePuzzleSelect() {
        if (!hasDownloaders())
            return;

        availableDownloaders = downloaders.getDownloaders();
        availableDownloaders.add(0, new DummyDownloader());

        ArrayAdapter<Downloader> adapter = new DownloadersAdapter(
            activity,
            android.R.layout.simple_spinner_item,
            availableDownloaders
        );
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );
        binding.puzzleSelect.setAdapter(adapter);
    }

    public interface OnDownloadSelectedListener {
        void onDownloadSelected(LocalDate date, List<Downloader> downloaders);
    }

    private class DownloadersAdapter extends ArrayAdapter<Downloader> {
        public DownloadersAdapter(
            Context context, int resource, List<Downloader> downloaders
        ) {
            super(context, resource, downloaders);
        }

        @Override
        public boolean isEnabled(int position) {
            Downloader downloader = getItem(position);
            // prolly "all available"
            if (downloader == null)
                return true;

            return downloader.isAvailable(getCurrentDate());
        }

        @Override
        public View getDropDownView(
            int position, View convertView, ViewGroup parent
        ) {
            View rawView = super.getDropDownView(
                position, convertView, parent
            );

            if (!(rawView instanceof TextView))
                return rawView;

            TextView view = (TextView) rawView;

            ColorStateList colors = view.getTextColors();
            int alpha = activity.getResources().getInteger(
                isEnabled(position)
                ? R.integer.enabled_downloader_alpha
                : R.integer.disabled_downloader_alpha
            );
            view.setTextColor(colors.withAlpha(alpha));

            if (!isEnabled(position) && position > 0) {
                Downloader downloader = getItem(position);
                Duration remaining
                    = downloader.getUntilAvailable(getCurrentDate());
                long hours = remaining == null ? 0 : remaining.toHours();
                String downloaderString = downloader.toString();

                if (remaining == null) {
                    view.setText(
                        view.getContext().getString(
                            R.string.downloader_not_available,
                            downloaderString
                        )
                    );
                } else if (hours == 0) {
                    view.setText(
                        view.getContext().getString(
                            R.string.downloader_available_soon,
                            downloaderString
                        )
                    );
                } else if (hours < 24) {
                    view.setText(
                        view.getContext()
                            .getResources()
                            .getQuantityString(
                                R.plurals.downloader_available_hours,
                                (int) hours, downloaderString, hours
                            )
                    );
                } else {
                    view.setText(
                        view.getContext().getString(
                            R.string.downloader_available_future,
                            downloaderString
                        )
                    );
                }
            }

            return view;
        }
    }
}

