package app.crossword.yourealwaysbe.forkyz.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spanned;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import app.crossword.yourealwaysbe.forkyz.PuzzleActivity;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.databinding.CompletedBinding;
import app.crossword.yourealwaysbe.forkyz.databinding.CompletionInfoBinding;
import app.crossword.yourealwaysbe.forkyz.databinding.PuzzleInfoDialogBinding;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder;
import app.crossword.yourealwaysbe.forkyz.util.ImaginaryTimer;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider;
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

public class PuzzleInfoDialogs {
    @AndroidEntryPoint
    public static class Finished extends DialogFragment {
        private static final long SECONDS = 1000;
        private static final long MINUTES = SECONDS * 60;
        private static final long HOURS = MINUTES * 60;

        @Inject
        protected CurrentPuzzleHolder currentPuzzleHolder;

        @Inject
        protected ForkyzSettings settings;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();

            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(activity);

            CompletedBinding binding
                = CompletedBinding.inflate(activity.getLayoutInflater());

            builder.setView(binding.getRoot())
                .setTitle(activity.getString(R.string.puzzle_finished_title));

            Puzzle puz = getPuzzle(currentPuzzleHolder);
            if (puz == null)
                return builder.create();

            populateFinishedInfo(puz, binding.completionInfo);
            setupRating(puz, binding.puzzleRating);

            String shareMessage = getShareMessage(puz);

            // with apologies to the Material guidelines..
            builder.setNegativeButton(
                R.string.share,
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                        Intent sendIntent = new Intent(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                        sendIntent.setType("text/plain");
                        activity.startActivity(Intent.createChooser(
                            sendIntent, activity.getString(R.string.share_your_time)
                        ));
                    }
                }
            );

            builder.setPositiveButton(R.string.done, null);

            return builder.create();

        }

        protected void populateFinishedInfo(
            Puzzle puz, CompletionInfoBinding binding
        ) {
            Activity activity = getActivity();

            addCompletedMsg(puz, binding.puzzleCompletedMsg);

            long elapsed = puz.getTime();
            long finishedTime = elapsed;

            long hours = elapsed / HOURS;
            elapsed = elapsed % HOURS;

            long minutes = elapsed / MINUTES;
            elapsed = elapsed % MINUTES;

            long seconds = elapsed / SECONDS;

            String elapsedString;
            if (hours > 0) {
                elapsedString = activity.getString(
                    R.string.completed_time_format_with_hours,
                    hours, minutes, seconds
                );
            } else {
                elapsedString = activity.getString(
                    R.string.completed_time_format_no_hours,
                    minutes, seconds
                );
            }

            int totalClues = puz.getNumberOfClues();
            int totalBoxes = getNumBoxes(puz);;
            int cheatedBoxes = getNumCheatedBoxes(puz);;

            int cheatLevel = cheatedBoxes * 100 / totalBoxes;
            if(cheatLevel == 0 && cheatedBoxes > 0){
                cheatLevel = 1;
            }
            String cheatedString = activity.getString(
                R.string.num_hinted_boxes, cheatedBoxes, cheatLevel
            );

            binding.elapsedTime.setText(elapsedString);
            binding.totalClues.setText(String.format(
                Locale.getDefault(), "%d", totalClues)
            );
            binding.totalBoxes.setText(String.format(
                Locale.getDefault(), "%d", totalBoxes
            ));
            binding.cheatedBoxes.setText(cheatedString);
        }

        protected void setupRating(Puzzle puz, RatingBar ratingBar) {
            int maxRating = ratingBar.getResources()
                .getInteger(R.integer.max_rating);
            ratingBar.setRating(Math.min(puz.getRating(), maxRating));
            ratingBar.setOnRatingBarChangeListener(
                (rb, rating, fu) -> {
                    puz.setRating((char) Math.min(rating, maxRating));
                }
            );

            settings.getLiveRatingsSettings().observe(
                this,
                ratingsSettings -> {
                    ratingBar.setVisibility(
                        ratingsSettings.getDisableRatings()
                        ? View.GONE
                        : View.VISIBLE
                    );
                }
            );
        }

        private String getShareMessage(Puzzle puz) {
            Activity activity = getActivity();

            String source = puz.getSource();
            if (source == null)
                source = puz.getTitle();
            if (source == null)
                source = "";

            int cheatedBoxes = getNumCheatedBoxes(puz);

            if (puz.getDate() != null) {
                DateTimeFormatter dateFormat
                    = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);

                return activity.getResources().getQuantityString(
                    R.plurals.share_message_with_date,
                    cheatedBoxes,
                    source, dateFormat.format(puz.getDate()), cheatedBoxes
                );
            } else {
                return activity.getResources().getQuantityString(
                    R.plurals.share_message_no_date,
                    cheatedBoxes,
                    source, cheatedBoxes
                );
            }
        }

        private int getNumBoxes(Puzzle puz) {
            int totalBoxes = 0;
            for(Box b : puz.getBoxesList()){
                if(!Box.isBlock(b))
                    totalBoxes++;
            }
            return totalBoxes;
        }

        private int getNumCheatedBoxes(Puzzle puz) {
            int cheatedBoxes = 0;
            for(Box b : puz.getBoxesList()){
                if(!Box.isBlock(b) && b.isCheated())
                    cheatedBoxes++;
            }
            return cheatedBoxes;
        }

        private void addCompletedMsg(Puzzle puz, TextView view) {
            String msg = puz.getCompletionMessage();
            if (msg == null || msg.isEmpty()) {
                view.setVisibility(View.GONE);
            } else {
                view.setText(HtmlCompat.fromHtml(msg, 0));
                view.setVisibility(View.VISIBLE);
            }
        }
    }

    // kind of a weird extends, but they share finished info
    @AndroidEntryPoint
    public static class Info extends Finished {
        @Inject
        protected FileHandlerProvider fileHandlerProvider;

        @Inject
        protected CurrentPuzzleHolder currentPuzzleHolder;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();

            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(getActivity());

            PuzzleInfoDialogBinding binding = PuzzleInfoDialogBinding.inflate(
                activity.getLayoutInflater()
            );

            Puzzle puz = getPuzzle(currentPuzzleHolder);
            if (puz != null) {
                binding.puzzleInfoTitle.setText(smartHtml(puz.getTitle()));
                binding.puzzleInfoAuthor.setText(puz.getAuthor());
                binding.puzzleInfoCopyright.setText(
                    smartHtml(puz.getCopyright())
                );

                ImaginaryTimer timer = getTimer();
                if (timer != null) {
                    binding.puzzleInfoTime.setText(getString(
                        R.string.elapsed_time, timer.time()
                    ));
                } else {
                    binding.puzzleInfoTime.setText(getString(
                        R.string.elapsed_time,
                        new ImaginaryTimer(puz.getTime()).time()
                    ));
                }

                binding.puzzleInfoProgress
                    .setProgress(puz.getPercentComplete());

                fileHandlerProvider.get(fileHandler -> {
                    PuzHandle handle = getPuzHandle();
                    if (handle != null) {
                        binding.puzzleInfoFilename.setText(
                            fileHandler.getUri(handle).toString()
                        );
                    }
                });

                addIntro(binding);
                addNotes(binding);
                addCompletedInfo(binding);
                setupRating(puz, binding.puzzleInfoRating);
            }

            builder.setView(binding.getRoot());

            return builder.create();
        }

        private ImaginaryTimer getTimer() {
            Activity activity = getActivity();
            if (activity instanceof PuzzleActivity)
                return ((PuzzleActivity) activity).getTimer();
            else
                return null;
        }

        private PuzHandle getPuzHandle() {
            return currentPuzzleHolder.getPuzHandle();
        }

        private void addIntro(PuzzleInfoDialogBinding binding) {
            Puzzle puz = getPuzzle(currentPuzzleHolder);
            if (puz == null)
                return;

            String intro = puz.getIntroMessage();
            if (intro == null || intro.isEmpty()) {
                binding.puzzleInfoIntroTitle.setVisibility(View.GONE);
                binding.puzzleInfoIntro.setVisibility(View.GONE);
            } else {
                binding.puzzleInfoIntroTitle.setVisibility(View.VISIBLE);
                binding.puzzleInfoIntro.setText(smartHtml(intro));
                binding.puzzleInfoIntro.setVisibility(View.VISIBLE);
            }
        }

        private void addNotes(PuzzleInfoDialogBinding binding) {
            Puzzle puz = getPuzzle(currentPuzzleHolder);
            if (puz == null || !puz.hasNotes()) {
                binding.puzzleInfoNotesTitle.setVisibility(View.GONE);
                binding.puzzleInfoNotes.setVisibility(View.GONE);
                return;
            }

            binding.puzzleInfoNotesTitle.setVisibility(View.VISIBLE);
            binding.puzzleInfoNotes.setVisibility(View.VISIBLE);

            String puzNotes = puz.getNotes();

            final String notes = puzNotes;

            String[] split = notes.split(
                "(?i:(?m:"
                    + "^\\s*Across:?\\s*$|^.*>Across<.*|"
                    + "^\\s*Down:?\\s*$|^.*>Down<.*|"
                    + "^\\s*\\d))", 2
            );

            final String text = split[0].trim();
            final boolean hasMore = split.length > 1;

            if (!hasMore) {
                binding.puzzleInfoNotes.setText(smartHtml(text));
            } else {
                if (text.length() > 0) {
                    binding.puzzleInfoNotes.setText(smartHtml(getString(
                        R.string.tap_to_show_full_notes_with_text, text
                    )));
                } else {
                    binding.puzzleInfoNotes.setText(getString(
                        R.string.tap_to_show_full_notes_no_text
                    ));
                }

                binding.puzzleInfoNotes.setOnClickListener(
                    new View.OnClickListener() {
                        private boolean showAll = true;

                        public void onClick(View view) {
                            TextView tv = (TextView) view;

                            if (showAll) {
                                if (notes == null || notes.length() == 0) {
                                    tv.setText(getString(
                                        R.string.tap_to_hide_full_notes_no_text
                                    ));
                                } else {
                                    tv.setText(smartHtml(getString(
                                        R.string.tap_to_hide_full_notes_with_text,
                                        notes
                                    )));
                                }
                            } else {
                                if (text == null || text.length() == 0) {
                                    tv.setText(getString(
                                        R.string.tap_to_show_full_notes_no_text
                                    ));
                                } else {
                                    tv.setText(smartHtml(getString(
                                        R.string.tap_to_show_full_notes_with_text,
                                        text
                                    )));
                                }
                            }

                            showAll = !showAll;
                        }
                    }
                );
            }
        }

        private void addCompletedInfo(PuzzleInfoDialogBinding binding) {
            Puzzle puz = getPuzzle(currentPuzzleHolder);
            if (puz == null || puz.getPercentComplete() < 100) {
                binding.puzzleInfoCompletedTitle.setVisibility(View.GONE);
                binding.puzzleInfoCompletedPadding.setVisibility(View.GONE);
                binding.completionInfo
                    .puzzleCompletedMsg.setVisibility(View.GONE);
                binding.completionInfo.statsTable.setVisibility(View.GONE);
            } else {
                String msg = puz.getCompletionMessage();
                if (msg == null || msg.isEmpty()) {
                    binding.puzzleInfoCompletedTitle.setVisibility(View.GONE);
                    binding.puzzleInfoCompletedPadding
                        .setVisibility(View.VISIBLE);
                    binding.completionInfo
                        .puzzleCompletedMsg.setVisibility(View.GONE);
                } else {
                    binding.puzzleInfoCompletedTitle
                        .setVisibility(View.VISIBLE);
                    binding.puzzleInfoCompletedPadding
                        .setVisibility(View.GONE);
                    binding.completionInfo
                        .puzzleCompletedMsg.setVisibility(View.VISIBLE);
                }
                binding.completionInfo.statsTable.setVisibility(View.VISIBLE);
                populateFinishedInfo(puz, binding.completionInfo);
            }
        }
    }

    @AndroidEntryPoint
    public static class Intro extends DialogFragment {
        @Inject
        protected CurrentPuzzleHolder currentPuzzleHolder;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(getActivity());

            Puzzle puz = getPuzzle(currentPuzzleHolder);
            if (puz != null && puz.hasIntroMessage()) {
                builder.setTitle(getString(R.string.introduction))
                    .setMessage(smartHtml(puz.getIntroMessage()))
                    .setPositiveButton(R.string.ok, null);
            }

            return builder.create();
        }

    }

    private static Spanned smartHtml(String text) {
        return text == null ? null : HtmlCompat.fromHtml(text, 0);
    }

    private static Puzzle getPuzzle(CurrentPuzzleHolder currentPuzzleHolder) {
        Playboard board = currentPuzzleHolder.getBoard();
        return board == null ? null : board.getPuzzle();
    }
}
