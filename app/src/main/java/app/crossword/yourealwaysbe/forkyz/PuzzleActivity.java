package app.crossword.yourealwaysbe.forkyz;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.inject.Inject;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

import org.jg.wordstonumbers.WordsToNumbersUtil;

import app.crossword.yourealwaysbe.forkyz.databinding.VoiceButtonsBinding;
import app.crossword.yourealwaysbe.forkyz.tools.ChatGPTHelp;
import app.crossword.yourealwaysbe.forkyz.tools.CrosswordSolver;
import app.crossword.yourealwaysbe.forkyz.util.ImaginaryTimer;
import app.crossword.yourealwaysbe.forkyz.util.SpeechContract;
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand;
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerShared;
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle;
import app.crossword.yourealwaysbe.forkyz.view.ChooseFlagColorDialog;
import app.crossword.yourealwaysbe.forkyz.view.ClueEditDialog;
import app.crossword.yourealwaysbe.forkyz.view.PlayboardTextRenderer;
import app.crossword.yourealwaysbe.forkyz.view.PuzzleInfoDialogs;
import app.crossword.yourealwaysbe.forkyz.view.SpecialEntryDialog;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;

@AndroidEntryPoint
public abstract class PuzzleActivity
        extends ForkyzActivity
        implements Playboard.PlayboardListener {

    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

    protected static final String HELP_RESPONSE_TEXT = "helpResponse";

    @Inject
    protected FileHandlerProvider fileHandlerProvider;

    private VoiceButtonsBinding voiceBinding = null;
    private boolean firstPlay = false;
    private ImaginaryTimer timer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private PlayboardTextRenderer textRenderer;
    private TextToSpeech ttsService = null;
    private boolean ttsReady = false;
    private boolean hasInitialValues = false;

    private LiveData<Boolean> volumeActivatesVoice;
    private LiveData<Boolean> equalsAnnounceClue;

    private Runnable updateTimeTask = new Runnable() {
        public void run() {
            PuzzleActivity.this.onTimerUpdate();
        }
    };

    private VoiceCommands voiceCommandDispatcher = new VoiceCommands();
    private ActivityResultLauncher<String> voiceInputLauncher
        = registerForActivityResult(new SpeechContract(), text -> {
            voiceCommandDispatcher.dispatch(text);
        });

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // get these early to maximise chances of it being ready by the first
        // volume down press
        volumeActivatesVoice = settings.liveVoiceVolumeActivatesVoice();
        equalsAnnounceClue = settings.liveVoiceEqualsAnnounceClue();

        startTimer();
        setupObservers();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!super.onPrepareOptionsMenu(menu))
            return false;

        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        String shareUrl = puz == null ? null : puz.getShareUrl();
        if (shareUrl == null || shareUrl.isEmpty()) {
            MenuItem open = menu.findItem(R.id.puzzle_menu_open_share_url);
            open.setVisible(false);
            open.setEnabled(false);
        }

        canRequestHelpForCurrentClue(canRequest -> {
        settings.getExtCrosswordSolverEnabled(solverEnabled -> {
        settings.getExtDictionary(dictionary -> {
            boolean extEnabled =
                canRequest
                || solverEnabled
                || dictionary != null;

            if (!extEnabled) {
                MenuItem ext = menu.findItem(R.id.puzzle_menu_external_tools);
                ext.setVisible(false);
                ext.setEnabled(false);
            }

            if (!canRequest) {
                MenuItem help = menu.findItem(R.id.puzzle_menu_ask_chat_gpt);
                help.setVisible(false);
                help.setEnabled(false);
            }

            if (!solverEnabled) {
                MenuItem solver
                    = menu.findItem(R.id.puzzle_menu_crossword_solver);
                solver.setVisible(false);
                solver.setEnabled(false);
            }

            if (dictionary == null) {
                MenuItem help
                    = menu.findItem(R.id.puzzle_menu_external_dictionary);
                help.setVisible(false);
                help.setEnabled(false);
            }
        });});});

        settings.getPlayScratchMode(scratchMode -> {
            MenuItem item = menu.findItem(R.id.puzzle_menu_scratch_mode);
            if (item != null)
                item.setChecked(scratchMode);
        });

        boolean canSolve = puz != null && puz.hasSolution();

        MenuItem showErrors = menu.findItem(R.id.puzzle_menu_show_errors);
        showErrors.setEnabled(canSolve);
        showErrors.setVisible(canSolve);

        settings.getPlayShowErrorsClue(showErrorsClue -> {
        settings.getPlayShowErrorsCursor(showErrorsCursor -> {
        settings.getPlayShowErrorsGrid(showErrorsGrid -> {
            int showErrorsTitle =
                (showErrorsGrid || showErrorsCursor || showErrorsClue)
                ? R.string.showing_errors
                : R.string.show_errors;

            showErrors.setTitle(showErrorsTitle);

            menu.findItem(R.id.puzzle_menu_show_errors_grid)
                .setChecked(showErrorsGrid);
            menu.findItem(R.id.puzzle_menu_show_errors_cursor)
                .setChecked(showErrorsCursor);
            menu.findItem(R.id.puzzle_menu_show_errors_clue)
                .setChecked(showErrorsClue);
        });});});


        MenuItem reveal = menu.findItem(R.id.puzzle_menu_reveal);
        reveal.setEnabled(canSolve);
        reveal.setVisible(canSolve);

        Box box = (board == null) ? null : board.getCurrentBox();
        boolean hasInitial = !Box.isBlock(box) && box.hasInitialValue();
        menu.findItem(R.id.puzzle_menu_reveal_initial_letter)
            .setVisible(hasInitial);

        menu.findItem(R.id.puzzle_menu_reveal_initial_letters)
            .setVisible(getHasInitialValues());

        if (puz == null || puz.getSupportUrl() == null) {
            MenuItem support = menu.findItem(R.id.puzzle_menu_support_source);
            support.setVisible(false);
            support.setEnabled(false);
        }

        return true;
    }

    private void startTimer() {
        Puzzle puz = getPuzzle();

        if (puz != null && puz.getPercentComplete() != 100) {
            ImaginaryTimer timer = new ImaginaryTimer(puz.getTime());
            setTimer(timer);
            timer.start();
        }
    }

    public ImaginaryTimer getTimer() {
        return timer;
    }

    @Override
    public void onPlayboardChange(PlayboardChanges changes) {
        handleChangeTimer();
        handleChangeAccessibility(changes);
        handleChangeChatGPT(changes);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.puzzle_menu_special_entry) {
            specialEntry();
            return true;
        } else if (id == R.id.puzzle_menu_share_clue) {
            shareClue(false);
            return true;
        } else if (id == R.id.puzzle_menu_share_clue_response) {
            shareClue(true);
            return true;
        } else if (id == R.id.puzzle_menu_share_puzzle_full) {
            sharePuzzle(false, false);
            return true;
        } else if (id == R.id.puzzle_menu_share_puzzle_without_extensions) {
            sharePuzzle(false, true);
            return true;
        } else if (id == R.id.puzzle_menu_share_puzzle_orig) {
            sharePuzzle(true, true);
            return true;
        } else if (id == R.id.puzzle_menu_open_share_url) {
            openShareUrl();
            return true;
        } else if (id == R.id.puzzle_menu_ask_chat_gpt) {
            requestHelpForCurrentClue();
            return true;
        } else if (id == R.id.clue_list_menu_edit_clue) {
            editClue();
            return true;
        } else if (id == R.id.puzzle_menu_crossword_solver) {
            callCrosswordSolver();
            return true;
        } else if (id == R.id.puzzle_menu_external_dictionary) {
            callExternalDictionary();
            return true;
        } else if (id == R.id.puzzle_menu_clue_notes) {
            launchClueNotes(getBoard().getClueID());
            return true;
        } else if (id == R.id.puzzle_menu_puzzle_notes) {
            launchPuzzleNotes();
            return true;
        } else if (id == R.id.puzzle_menu_flag_cell_toggle) {
            toggleCellFlag();
            return true;
        } else if (id == R.id.puzzle_menu_scratch_mode) {
            toggleScratchMode();
            return true;
        } else if (id == R.id.puzzle_menu_show_errors_grid) {
            settings.setPlayShowErrorsGrid(!getBoard().isShowErrorsGrid());
            return true;
        } else if (id == R.id.puzzle_menu_show_errors_clue) {
            settings.setPlayShowErrorsClue(!getBoard().isShowErrorsClue());
            return true;
        } else if (id == R.id.puzzle_menu_show_errors_cursor) {
            settings.setPlayShowErrorsCursor(
                !getBoard().isShowErrorsCursor()
            );
            return true;
        } else if (id == R.id.puzzle_menu_reveal_initial_letter) {
            getBoard().revealInitialLetter();
            return true;
        } else if (id == R.id.puzzle_menu_reveal_letter) {
            getBoard().revealLetter();
            return true;
        } else if (id == R.id.puzzle_menu_reveal_word) {
            getBoard().revealWord();
            return true;
        } if (id == R.id.puzzle_menu_reveal_initial_letters) {
            getBoard().revealInitialLetters();
            return true;
        } else if (id == R.id.puzzle_menu_reveal_errors) {
            getBoard().revealErrors();
            return true;
        } else if (id == R.id.puzzle_menu_reveal_puzzle) {
            showRevealPuzzleDialog();
            return true;
        } else if (id == R.id.puzzle_menu_info) {
            showInfoDialog();
            return true;
        } else if (id == R.id.puzzle_menu_support_source) {
            actionSupportSource();
            return true;
        } else if (id == R.id.puzzle_menu_help) {
            Intent helpIntent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("play.html"),
                this,
                HTMLActivity.class
            );
            this.startActivity(helpIntent);
            return true;
        } else if (id == R.id.puzzle_menu_settings) {
            Intent i = new Intent(this, PreferencesActivity.class);
            this.startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Puzzle puz = getPuzzle();
        ImaginaryTimer timer = getTimer();

        if ((puz != null)) {
            if ((timer != null) && (puz.getPercentComplete() != 100)) {
                timer.stop();
                puz.setTime(timer.getElapsed());
                setTimer(null);
            }

            saveBoard();
        }

        Playboard board = getBoard();
        if (board != null)
            board.removeListener(this);

        if (ttsService != null) {
            ttsService.shutdown();
            ttsService = null;
            ttsReady = false;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        ImaginaryTimer timer = getTimer();
        if (timer != null)
            timer.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Puzzle puz = getPuzzle();
        if (puz != null && puz.getPercentComplete() != 100) {
            long time = puz.getTime();
            firstPlay = (time == 0);
            ImaginaryTimer timer = new ImaginaryTimer(time);
            setTimer(timer);
            timer.start();
        }

        Playboard board = getBoard();
        if (board != null)
            board.addListener(this);

        hasInitialValues = puz != null && puz.hasInitialValueCells();

        // anything in puzzle could have changed
        invalidateOptionsMenu();
    }

    @Override
    protected void onStop() {
        super.onStop();

        ImaginaryTimer timer = getTimer();
        if (timer != null) {
            timer.stop();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_EQUALS:
            if (isAnnounceClueEquals())
                return true;
            break;
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            if (isVolumeDownActivatesVoicePref())
                return true;
            break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_EQUALS:
            if (isAnnounceClueEquals()) {
                announceClue(false);
                return true;
            }
            break;
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            if (isVolumeDownActivatesVoicePref()) {
                launchVoiceInput();
                return true;
            }
            break;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Whether the current puzzle has initial values
     */
    protected boolean getHasInitialValues() {
        return hasInitialValues;
    }

    /**
     * Override if you want to update your UI based on the timer
     *
     * But still call super. Only called if the showTimer pref is true
     */
    protected void onTimerUpdate() {
        settings.getPlayShowTimer(showTimer -> {
            if (showTimer)
                handler.postDelayed(updateTimeTask, 1000);
        });
    }

    protected Playboard getBoard(){
        return getCurrentBoard();
    }

    protected Puzzle getPuzzle() {
        Playboard board = getBoard();
        return (board == null) ? null : getBoard().getPuzzle();
    }

    protected void saveBoard() {
        PuzHandle puzHandle = getCurrentPuzHandle();
        if (puzHandle == null) {
            LOG.severe("No puz handle to save puzzle to.");
            return;
        }

        Playboard board = getCurrentBoard();
        if (board == null) {
            LOG.severe("No board to save.");
            return;
        }

        Puzzle puz = board.getPuzzle();
        if (puz == null) {
            LOG.severe("No puzzle associated to the board to save.");
            return;
        }

        fileHandlerProvider.get(fileHandler -> {
            // TODO: save off main thread?
            try {
                fileHandler.save(puz, puzHandle);
            } catch (IOException e) {
                LOG.severe("Error saving puzzle.");
                e.printStackTrace();
            }
        });
    }


    protected boolean isFirstPlay() {
        return firstPlay;
    }

    protected void setTimer(ImaginaryTimer timer) {
        this.timer = timer;
    }

    protected PuzHandle getPuzHandle() {
        return getCurrentPuzHandle();
    }

    protected void getLongClueText(Clue clue, Consumer<String> cb) {
        settings.getPlayShowCount(showCount -> {
            cb.accept(
                PlayboardTextRenderer.getLongClueText(this, clue, showCount)
            );
        });
    }

    protected void launchClueNotes(ClueID cid) {
        if (cid != null) {
            Intent i = new Intent(this, NotesActivity.class);
            i.putExtra(NotesActivity.CLUE_NOTE_LISTNAME, cid.getListName());
            i.putExtra(NotesActivity.CLUE_NOTE_INDEX, cid.getIndex());
            this.startActivity(i);
        } else {
            launchPuzzleNotes();
        }
    }

    protected void launchClueNotes(Clue clue) {
        if (clue != null)
            launchClueNotes(clue.getClueID());
        else
            launchPuzzleNotes();
    }

    protected void launchPuzzleNotes() {
        Intent i = new Intent(this, NotesActivity.class);
        this.startActivity(i);
    }

    private void showRevealPuzzleDialog() {
        DialogFragment dialog = new RevealPuzzleDialog();
        dialog.show(getSupportFragmentManager(), "RevealPuzzleDialog");
    }

    private void showInfoDialog() {
        DialogFragment dialog = new PuzzleInfoDialogs.Info();
        dialog.show(getSupportFragmentManager(), "PuzzleInfoDialgs.Info");
    }

    private void actionSupportSource() {
        Puzzle puz = getPuzzle();
        if (puz != null) {
            String supportUrl = puz.getSupportUrl();
            if (supportUrl != null) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(supportUrl));
                startActivity(i);
            }
        }
    }

    private void canRequestHelpForCurrentClue(Consumer<Boolean> cb) {
        Playboard board = getBoard();
        Clue clue = board == null ? null : board.getClue();
        if (clue == null)
            cb.accept(false);
        else
            ChatGPTHelp.isEnabled(settings, cb);
    }

    private void requestHelpForCurrentClue() {
        if (!utils.hasNetworkConnection(this)) {
            Toast t = Toast.makeText(
                this,
                R.string.help_query_but_no_active_network,
                Toast.LENGTH_LONG
            );
            t.show();
        } else {
            canRequestHelpForCurrentClue(canRequest -> {
                if (canRequest) {
                    ChatGPTHelp helper = new ChatGPTHelp(settings);
                    helper.requestHelpForCurrentClue(
                        this,
                        getBoard(),
                        (String response) -> {
                            HelpResponseDialog dialog
                                = new HelpResponseDialog();
                            Bundle args = new Bundle();
                            args.putString(HELP_RESPONSE_TEXT, response);
                            dialog.setArguments(args);
                            dialog.show(
                                getSupportFragmentManager(),
                                "HelpResponseDialog"
                            );
                        }
                    );
                }
            });
        }
    }

    private void specialEntry() {
        SpecialEntryDialog dialog = new SpecialEntryDialog();
        dialog.show(getSupportFragmentManager(), "SpecialEntryDialog");
    }

    private void shareClue(boolean withResponse) {
        Playboard board = getBoard();
        Clue clue = (board == null) ? null : board.getClue();
        if (clue == null)
            return;

        Puzzle puz = board.getPuzzle();
        String source = (puz == null) ? null : puz.getSource();
        String title = (puz == null) ? null : puz.getTitle();
        String author = (puz == null) ? null : puz.getAuthor();
        Box[] response = board.getCurrentWordBoxes();

        getShareMessage(puz, clue, response, withResponse, shareMessage -> {
            // ShareCompat from
            // https://stackoverflow.com/a/39619468/6882587
            // assume works better than the out-of-date android docs!
            Intent shareIntent = new ShareCompat.IntentBuilder(this)
                .setText(shareMessage)
                .setType("text/plain")
                .setChooserTitle(getString(R.string.share_clue_title))
                .createChooserIntent();

            startActivity(shareIntent);
        });
    }

    private void getShareMessage(
        Puzzle puz, Clue clue, Box[] response, boolean withResponse,
        Consumer<String> cb
    ) {
        getShareClueText(clue, clueText -> {
            String responseText = withResponse
                ? getShareResponseText(response)
                : null;

            String puzzleDetails = getSharePuzzleDetails(puz);

            if (withResponse) {
                cb.accept(getString(
                    R.string.share_clue_response_text,
                    clueText, responseText, puzzleDetails
                ));
            } else {
                cb.accept(getString(
                    R.string.share_clue_text,
                    clueText, puzzleDetails
                ));
            }
        });
    }

    private String getShareResponseText(Box[] boxes) {
        StringBuilder responseText = new StringBuilder();
        if (boxes != null) {
            for (Box box : boxes) {
                if (box.isBlank()) {
                    responseText.append(
                        getString(R.string.share_clue_blank_box)
                    );
                } else {
                    responseText.append(box.getResponse());
                }
            }
        }
        return responseText.toString();
    }

    protected void getShareClueText(Clue clue, Consumer<String> cb) {
        settings.getPlayShowCount(showCount -> {
            if (clue == null)
                cb.accept(getString(R.string.unknown_hint));

            int wordLen = clue.hasZone() ? clue.getZone().size() : -1;

            if (showCount && wordLen >= 0) {
                cb.accept(getString(
                    R.string.clue_format_short_no_num_no_dir_with_count,
                    clue.getHint(),
                    wordLen
                ));
            } else {
                cb.accept(clue.getHint());
            }
        });
    }

    protected void launchVoiceInput() {
        try {
            voiceInputLauncher.launch(
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            );
        } catch (ActivityNotFoundException e) {
            Toast t = Toast.makeText(
                this,
                R.string.no_speech_recognition_available,
                Toast.LENGTH_LONG
            );
            t.show();
        }
    }

    protected void registerVoiceCommand(@NonNull VoiceCommand command) {
        voiceCommandDispatcher.registerVoiceCommand(command);
    }

    /**
     * Prepared command for inputting word answers
     */
    protected void registerVoiceCommandAnswer() {
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_answer),
            getString(R.string.command_answer_alt),
            answer -> {
                Playboard board = getBoard();
                if (board == null)
                    return;

                // remove non-word as not usually entered into grids
                String prepped
                    = answer.replaceAll("\\W+", "")
                        .toUpperCase(Locale.getDefault());
                board.playAnswer(prepped);
            }
        ));
    }

    /**
     * Prepared command for inputting letters
     */
    protected void registerVoiceCommandLetter() {
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_letter),
            letter -> {
                if (letter == null || letter.isEmpty())
                    return;
                Playboard board = getBoard();
                if (board != null)
                    board.playLetter(Character.toUpperCase(letter.charAt(0)));
            }
        ));
    }

    /**
     * Prepared command for jumping to clue number
     */
    protected void registerVoiceCommandNumber() {
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_number),
            textNumber -> {
                Playboard board = getBoard();
                if (board == null)
                    return;

                String prepped
                    = WordsToNumbersUtil.convertTextualNumbersInDocument(textNumber);
                try {
                    int number = Integer.parseInt(prepped);
                    board.jumpToClue(String.valueOf(number));
                } catch (NumberFormatException e) {
                    board.jumpToClue(textNumber);
                }
            }
        ));
    }

    /**
     * Prepared command for clearing current word
     */
    protected void registerVoiceCommandClear() {
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_clear),
            args -> { getBoard().clearWord(); }
        ));
    }

    /**
     * Prepared command for announcing current clue
     */
    protected void registerVoiceCommandAnnounceClue() {
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_announce_clue),
            args -> { announceClue(false); }
        ));
    }

    /**
     * Prepared command for announcing current clue
     */
    protected void registerVoiceCommandClueHelp() {
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_current_clue_help),
            args -> { requestHelpForCurrentClue(); }
        ));
    }

    private String getSharePuzzleDetails(Puzzle puz) {
        if (puz == null)
            return "";

        String source = puz.getSource();
        String title = puz.getTitle();
        String author = puz.getAuthor();

        if (source == null)
            source = "";
        if (title == null)
            title = "";
        if (author != null) {
            // add author if not already in title or caption
            // case insensitive trick:
            // https://www.baeldung.com/java-case-insensitive-string-matching
            String quotedAuthor = Pattern.quote(author);
            boolean removeAuthor
                = author.isEmpty()
                    || title.matches("(?i).*" + quotedAuthor + ".*")
                    || source.matches("(?i).*" + quotedAuthor + ".*");

            if (removeAuthor)
                author = null;
        }

        String shareUrl = puz.getShareUrl();

        if (shareUrl == null || shareUrl.isEmpty()) {
            return (author != null)
                ? getString(
                    R.string.share_puzzle_details_author_no_url,
                    source, title, author
                ) : getString(
                    R.string.share_puzzle_details_no_author_no_url,
                    source, title
                );
        } else {
            return (author != null)
                ? getString(
                    R.string.share_puzzle_details_author_url,
                    source, title, author, shareUrl
                ) : getString(
                    R.string.share_puzzle_details_no_author_url,
                    source, title, shareUrl
                );
        }
    }

    private void sharePuzzle(boolean writeBlank, boolean omitExtensions) {
        final Puzzle puz = getPuzzle();
        if (puz == null)
            return;

        FileHandlerShared.getShareUri(
            getApplicationContext(), getPuzzle(), writeBlank, omitExtensions,
            (puzUri) -> {
                String mimeType = FileHandlerShared.getShareUriMimeType();
                String puzzleDetails = getSharePuzzleDetails(puz);

                Intent shareIntent = new ShareCompat.IntentBuilder(this)
                    .setStream(puzUri)
                    .setType(mimeType)
                    .setChooserTitle(getString(R.string.share_puzzle_title))
                    .createChooserIntent();

                startActivity(shareIntent);
            }
        );
    }

    private void openShareUrl() {
        Puzzle puz = getPuzzle();
        if (puz != null) {
            String shareUrl = puz.getShareUrl();
            if (shareUrl != null && !shareUrl.isEmpty()) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(shareUrl));
                startActivity(i);
            }
        }
    }

    /**
     * Get currently known value of volume activates voice
     *
     * Needs to be non-blocking so that onKeyUp/Down can return the
     * right value immediately, so use a live value that may be unknown
     * (defaults to false)
     */
    protected boolean isVolumeDownActivatesVoicePref() {
        Boolean value = volumeActivatesVoice.getValue();
        return value == null ? false : value;
    }

    protected void setupVoiceButtons(VoiceButtonsBinding binding) {
        voiceBinding = binding;
        if (voiceBinding == null)
            return;

        voiceBinding.voiceButton.setOnClickListener(view -> {
            launchVoiceInput();
        });
        voiceBinding.announceClueButton.setOnClickListener(view -> {
            announceClue(false);
        });
    }

    protected void setVoiceButtonVisibility() {
        if (voiceBinding == null)
            return;

        settings.getVoiceButtonActivatesVoice(buttonVoice -> {
        settings.getVoiceButtonAnnounceClue(buttonAnnounce -> {
            boolean anyButton = buttonVoice || buttonAnnounce;

            voiceBinding.voiceButtonsContainer.setVisibility(
                anyButton ? View.VISIBLE : View.GONE
            );
            voiceBinding.voiceButton.setVisibility(
                buttonVoice ? View.VISIBLE : View.GONE
            );
            voiceBinding.announceClueButton.setVisibility(
                buttonAnnounce ? View.VISIBLE : View.GONE
            );
        });});
    }

    /**
     * Announce clue
     *
     * With accessibility service if available.
     */
    protected void announceClue(boolean onlyIfAccessibilityService) {
        Playboard board = getBoard();
        if (board == null)
            return;

        settings.getPlayShowCount(showCount -> {
            CharSequence clue
                = PlayboardTextRenderer.getAccessibleCurrentClueWord(
                    this, board, showCount
                );

            if (clue != null)
                announceText(clue, onlyIfAccessibilityService);
        });
    }

    /**
     * Announce box
     *
     * With accessibility service if available.
     */
    protected void announceBox(boolean onlyIfAccessibilityService) {
        Playboard board = getBoard();
        if (board == null)
            return;

        CharSequence box =
            PlayboardTextRenderer.getAccessibleCurrentBox(this, board);

        if (box != null)
            announceText(box, onlyIfAccessibilityService);
    }

    /**
     * Add available accessibility actions to view
     *
     * Just announce clue for now
     */
    protected void addAccessibilityActions(View view) {
        ViewCompat.addAccessibilityAction(
            view,
            getString(R.string.announce_clue_label),
            (View v, AccessibilityViewCommand.CommandArguments arguments) -> {
                announceClue(true);
                return true;
            }
        );
    }

    protected boolean isAnnounceClueEquals() {
        Boolean value = equalsAnnounceClue.getValue();
        return (value == null) ? false : value;
    }

    /**
     * Handles user request to edit clue
     *
     * Calls through to editClue(cid) where cid is the id of the
     * currently selected clue on the board. May be overridden by
     * subclasses to edit a different default clue.
     */
    protected void editClue() {
        Playboard board = currentPuzzleHolder.getBoard();
        ClueID cid = (board == null) ? null : board.getClueID();
        editClue(cid);
    }

    /**
     * Open dialog to edit given clue
     */
    protected void editClue(ClueID cid) {
        if (cid == null)
            return;

        ClueEditDialog dialog = new ClueEditDialog();
        Bundle args = new Bundle();
        args.putString(ClueEditDialog.CLUE_LISTNAME, cid.getListName());
        args.putInt(ClueEditDialog.CLUE_INDEX, cid.getIndex());
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "ClueEditDialog");
    }

    private void handleChangeTimer() {
        Puzzle puz = getPuzzle();
        if (puz == null)
            return;

        ImaginaryTimer timer = getTimer();
        if (puz != null &&
            puz.getPercentComplete() == 100 &&
            timer != null) {

            timer.stop();
            puz.setTime(timer.getElapsed());
            setTimer(null);

            DialogFragment dialog = new PuzzleInfoDialogs.Finished();
            dialog.show(
                getSupportFragmentManager(),
                "PuzzleInfoDialogs.Finished"
            );
        }
    }

    private boolean isAccessibilityServiceRunning() {
        AccessibilityManager manager
            = (AccessibilityManager) getSystemService(
                Context.ACCESSIBILITY_SERVICE
            );
        return manager != null && manager.isEnabled();
    }

    private void handleChangeAccessibility(PlayboardChanges changes) {
        boolean accessibilityRunning = isAccessibilityServiceRunning();
        settings.getVoiceAlwaysAnnounceBox(announceBoxSetting -> {
        settings.getVoiceAlwaysAnnounceClue(announceClueSetting -> {
            boolean announceBox = accessibilityRunning || announceBoxSetting;
            boolean announceClue = accessibilityRunning || announceClueSetting;

            if (!announceClue && !announceBox)
                return;

            Playboard board = getBoard();
            if (board == null)
                return;

            Position newPos = board.getHighlightLetter();
            boolean isNewWord = !Objects.equals(
                changes.getPreviousWord(), changes.getCurrentWord()
            );
            boolean isNewPosition
                = !Objects.equals(changes.getPreviousPosition(), newPos);

            if (isNewWord && announceClue)
                announceClue(!announceClueSetting);
            else if (isNewPosition && announceBox)
                announceBox(!announceBoxSetting);
        });});
    }

    /**
     * Announce text with accessibility if running or tts
     */
    private void announceText(
        CharSequence text, boolean onlyIfAccessibilityService
    ) {
        if (isAccessibilityServiceRunning()) {
            View content = findViewById(android.R.id.content);
            if (content != null && text != null)
                content.announceForAccessibility(text);
        } else if (!onlyIfAccessibilityService) {
            if (ttsService == null) {
                ttsService = new TextToSpeech(
                    getApplicationContext(),
                    (int status) -> {
                        if (status == TextToSpeech.SUCCESS) {
                            ttsReady = true;
                            announceText(text, onlyIfAccessibilityService);
                        }
                    }
                );
            } else if (!ttsReady) {
                // hopefully rare occasion where tts being prepared but not
                // ready yet
                Toast t = Toast.makeText(
                    this,
                    R.string.speech_not_ready,
                    Toast.LENGTH_SHORT
                );
                t.show();
            } else {
                utils.speak(ttsService, text);
            }
        }
    }

    private void handleChangeChatGPT(PlayboardChanges changes) {
        ChatGPTHelp.isEnabled(settings, isEnabled -> {
            if (isEnabled) {
                boolean newWord = !Objects.equals(
                    changes.getPreviousWord(), changes.getCurrentWord()
                );

                if (newWord)
                    invalidateOptionsMenu();
            }
        });
    }

    public static class HelpResponseDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();

            String response = getArguments().getString(HELP_RESPONSE_TEXT);
            if (response == null)
                response = activity.getString(R.string.help_query_failed);

            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(activity);

            builder.setTitle(
                activity.getString(R.string.help_query_response_title)
            ).setMessage(response)
                .setPositiveButton(R.string.ok, null);

            return builder.create();
        }
    }

    /**
     * Call billthefarmer's crossword solver
     *
     * Protected to allow overriding by activity. Default behaviour is to send
     * the currently selected board word to the Crossword Solver app.
     *
     * Uses org.billthefarmer.crossword
     */
    protected void callCrosswordSolver() {
        Playboard board = getBoard();
        Zone zone = (board == null) ? null : board.getCurrentZone();
        if (zone == null)
            return;

        Puzzle puz = getPuzzle();
        if (puz == null)
            return;

        StringBuilder request = new StringBuilder();

        for (Position pos : zone) {
            Box box = puz.checkedGetBox(pos);
            if (!Box.isBlock(box) && !box.isBlank()) {
                String response = box.getResponse();
                request.append(response);
            } else {
                request.append(".");
            }
        }

        CrosswordSolver.launchMain(this, request.toString());
    }

    protected void callExternalDictionary() {
        Playboard board = getBoard();
        Zone zone = (board == null) ? null : board.getCurrentZone();
        if (zone == null)
            return;

        Puzzle puz = getPuzzle();
        if (puz == null)
            return;

        settings.getExtDictionary(dictionary -> {
            if (dictionary == null)
                return;

            StringBuilder request = new StringBuilder();

            for (Position pos : zone) {
                Box box = puz.checkedGetBox(pos);
                if (box != null)
                    request.append(box.getResponse());
            }

            dictionary.query(this, request.toString());
        });
    }

    /**
     * Setup status bar stuff
     *
     * Elevation and colour to match show errors
     */
    protected void setupStatusBar(AppBarLayout appBarLayout) {
        setStatusBarElevation(appBarLayout);

        Observer<Boolean> showErrorsObserver = v -> {
            setStatusBarColor(appBarLayout);
        };
        settings.livePlayIndicateShowErrors().observe(this, showErrorsObserver);
        settings.livePlayShowErrorsClue().observe(this, showErrorsObserver);
        settings.livePlayShowErrorsCursor().observe(this, showErrorsObserver);
        settings.livePlayShowErrorsGrid().observe(this, showErrorsObserver);
    }

    private void setupObservers() {
        settings.livePlayShowTimer().observe(
            this,
            showTimer -> {
                if (showTimer)
                    handler.post(updateTimeTask);
            }
        );

        settings.livePlayPreserveCorrectLettersInShowErrors().observe(
            this,
            preserveCorrect -> {
                Playboard board = getBoard();
                if (board != null)
                    board.setPreserveCorrectLettersInShowErrors(preserveCorrect);
            }
        );

        settings.livePlayDeleteCrossingMode().observe(
            this,
            deleteCrossingMode -> {
                Playboard board = getBoard();
                if (board != null)
                    board.setDeleteCrossingMode(deleteCrossingMode);
            }
        );

        settings.livePlayScratchMode().observe(
            this, value -> { invalidateOptionsMenu(); }
        );

        settings.livePlayShowErrorsGrid().observe(
            this,
            showErrorsGrid -> {
                Playboard board = getBoard();
                if (board != null && board.isShowErrorsGrid() != showErrorsGrid) {
                    board.toggleShowErrorsGrid();
                }
                invalidateOptionsMenu();
            }
        );

        settings.livePlayShowErrorsCursor().observe(
            this,
            showErrorsCursor -> {
                Playboard board = getBoard();
                if (
                    board != null &&
                    board.isShowErrorsCursor() != showErrorsCursor
                ) {
                    board.toggleShowErrorsCursor();
                }
                invalidateOptionsMenu();
            }
        );

        settings.livePlayShowErrorsClue().observe(
            this,
            showErrorsClue -> {
                Playboard board = getBoard();
                if (board != null && board.isShowErrorsClue() != showErrorsClue) {
                    board.toggleShowErrorsClue();
                }
                invalidateOptionsMenu();
            }
        );
    }

    private void toggleScratchMode() {
        settings.getPlayScratchMode(scratchMode -> {
            settings.setPlayScratchMode(!scratchMode);
        });
    }

    /**
     * Set status based on current values of show errors
     *
     * Uses LiveData.getValue, does nothing if settings not live yet
     */
    private void setStatusBarColor(AppBarLayout appBarLayout) {
        Boolean indicateShowErrors
            = settings.livePlayIndicateShowErrors().getValue();
        Boolean showErrorsClue = settings.livePlayShowErrorsClue().getValue();
        Boolean showErrorsCursor
            = settings.livePlayShowErrorsCursor().getValue();
        Boolean showErrorsGrid = settings.livePlayShowErrorsGrid().getValue();

        if (
            indicateShowErrors == null
            || showErrorsClue == null
            || showErrorsCursor == null
            || showErrorsGrid == null
        )
            return;

        Puzzle puz = getPuzzle();
        if (
            puz != null
            && puz.hasSolution()
            && indicateShowErrors
            && (
                showErrorsClue || showErrorsCursor || showErrorsGrid
            )
        ) {
            setStatusBarColor(
                appBarLayout,
                ContextCompat.getColor(this, R.color.cheatedColor)
            );
        } else {
            resetStatusBarColor(appBarLayout);
        }
    }


    /**
     * Flag currently selected cell
     */
    private void toggleCellFlag() {
        Playboard board = getBoard();
        Puzzle puz = getPuzzle();
        if (board == null || puz == null)
            return;

        Position pos = board.getHighlightLetter();
        Box box = puz.checkedGetBox(pos);
        if (pos == null || box == null)
            return;

        if (box.isFlagged()) {
            board.flagPosition(pos, false);
        } else {
            // let diagog set flag
            DialogFragment dialog = new ChooseFlagColorDialog();
            Bundle args = new Bundle();
            ChooseFlagColorDialog.addBoxPositionToBundle(args, pos);
            dialog.setArguments(args);
            dialog.show(getSupportFragmentManager(), "ChooseFlagColorDialog");
        }
    }

    public static class RevealPuzzleDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(getActivity());

            builder.setTitle(getString(R.string.reveal_puzzle))
                .setMessage(getString(R.string.are_you_sure))
                .setPositiveButton(
                    R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Playboard board
                                = ((PuzzleActivity) getActivity()).getBoard();
                            if (board != null)
                                 board.revealPuzzle();
                        }
                    }
                )
                .setNegativeButton(
                    R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }
                );

            return builder.create();
        }
    }
}
