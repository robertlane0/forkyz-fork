package app.crossword.yourealwaysbe.forkyz;

import java.io.IOException;
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
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.databinding.VoiceButtonsBinding;
import app.crossword.yourealwaysbe.forkyz.tools.ChatGPTHelpData;
import app.crossword.yourealwaysbe.forkyz.tools.ExternalToolLauncher;
import app.crossword.yourealwaysbe.forkyz.util.ImaginaryTimer;
import app.crossword.yourealwaysbe.forkyz.util.SpeechContract;
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand;
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

    private PuzzleActivityViewModel viewModel;

    private ActivityResultLauncher<String> voiceInputLauncher
        = registerForActivityResult(new SpeechContract(), text -> {
            viewModel.dispatchVoiceCommand(text);
        });

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        viewModel = new ViewModelProvider(this)
            .get(PuzzleActivityViewModel.class);

        // get these early to maximise chances of it being ready by the first
        // volume down press
        volumeActivatesVoice = settings.getLiveVoiceVolumeActivatesVoice();
        equalsAnnounceClue = settings.getLiveVoiceEqualsAnnounceClue();

        startTimer();
        setupObservers();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!super.onPrepareOptionsMenu(menu))
            return false;

        MenuState state = viewModel.getMenuState().getValue();
        if (state == null)
            return true;

        if (!state.getHasShareURL()) {
            MenuItem open = menu.findItem(R.id.puzzle_menu_open_share_url);
            open.setVisible(false);
            open.setEnabled(false);
        }

        if (!state.getHasSupportURL()) {
            MenuItem support = menu.findItem(R.id.puzzle_menu_support_source);
            support.setVisible(false);
            support.setEnabled(false);
        }

        ExternalToolsMenuState ext = state.getExternalToolsState();

        if (!ext.getHasExternal()) {
            MenuItem extTools = menu.findItem(R.id.puzzle_menu_external_tools);
            extTools.setVisible(false);
            extTools.setEnabled(false);
        }

        if (!ext.getHasChatGPT()) {
            MenuItem help = menu.findItem(R.id.puzzle_menu_ask_chat_gpt);
            help.setVisible(false);
            help.setEnabled(false);
        }

        if (!ext.getHasCrosswordSolver()) {
            MenuItem solver = menu.findItem(R.id.puzzle_menu_crossword_solver);
            solver.setVisible(false);
            solver.setEnabled(false);
        }

        if (!ext.getHasDuckDuckGo()) {
            MenuItem item = menu.findItem(R.id.puzzle_menu_duckduckgo);
            item.setVisible(false);
            item.setEnabled(false);
        }

        if (!ext.getHasFifteenSquared()) {
            MenuItem item = menu.findItem(R.id.puzzle_menu_fifteen_squared);
            item.setVisible(false);
            item.setEnabled(false);
        }

        if (!ext.getHasExternalDictionary()) {
            MenuItem help = menu.findItem(R.id.puzzle_menu_external_dictionary);
            help.setVisible(false);
            help.setEnabled(false);
        }

        MenuItem item = menu.findItem(R.id.puzzle_menu_scratch_mode);
        if (item != null)
            item.setChecked(state.isScratchMode());

        ShowErrorsMenuState errState = state.getShowErrorsState();
        boolean showErrorsClue = errState.isShowingErrorsClue();
        boolean showErrorsCursor = errState.isShowingErrorsCursor();
        boolean showErrorsGrid = errState.isShowingErrorsGrid();

        MenuItem showErrors = menu.findItem(R.id.puzzle_menu_show_errors);
        showErrors.setEnabled(errState.getEnabled());
        showErrors.setVisible(errState.getEnabled());

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

        boolean canReveal = state.getCanReveal();
        MenuItem reveal = menu.findItem(R.id.puzzle_menu_reveal);
        reveal.setEnabled(canReveal);
        reveal.setVisible(canReveal);

        menu.findItem(R.id.puzzle_menu_reveal_initial_letter)
            .setVisible(state.getCanRevealInitialBoxLetter());

        menu.findItem(R.id.puzzle_menu_reveal_initial_letters)
            .setVisible(state.getCanRevealInitialLetters());

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
        Playboard board = getBoard();

        if (board != null) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                finish();
                return true;
            } else if (id == R.id.puzzle_menu_special_entry) {
                specialEntry();
                return true;
            } else if (id == R.id.puzzle_menu_share_clue) {
                viewModel.shareClue(false);
                return true;
            } else if (id == R.id.puzzle_menu_share_clue_response) {
                viewModel.shareClue(true);
                return true;
            } else if (id == R.id.puzzle_menu_share_puzzle_full) {
                viewModel.sharePuzzle(false, false);
                return true;
            } else if (id == R.id.puzzle_menu_share_puzzle_without_extensions) {
                viewModel.sharePuzzle(false, true);
                return true;
            } else if (id == R.id.puzzle_menu_share_puzzle_orig) {
                viewModel.sharePuzzle(true, true);
                return true;
            } else if (id == R.id.puzzle_menu_open_share_url) {
                openShareUrl();
                return true;
            } else if (id == R.id.puzzle_menu_ask_chat_gpt) {
                viewModel.requestHelpForCurrentClue();
                return true;
            } else if (id == R.id.clue_list_menu_edit_clue) {
                editClue();
                return true;
            } else if (id == R.id.puzzle_menu_crossword_solver) {
                callCrosswordSolver();
                return true;
            } else if (id == R.id.puzzle_menu_duckduckgo) {
                viewModel.searchDuckDuckGo();
                return true;
            } else if (id == R.id.puzzle_menu_external_dictionary) {
                callExternalDictionary();
                return true;
            } else if (id == R.id.puzzle_menu_fifteen_squared) {
                viewModel.searchFifteenSquared();
                return true;
            } else if (id == R.id.puzzle_menu_clue_notes) {
                launchClueNotes(board.getClueID());
                return true;
            } else if (id == R.id.puzzle_menu_puzzle_notes) {
                launchPuzzleNotes();
                return true;
            } else if (id == R.id.puzzle_menu_flag_clue_toggle) {
                toggleClueFlag();
                return true;
            } else if (id == R.id.puzzle_menu_flag_cell_toggle) {
                toggleCellFlag();
                return true;
            } else if (id == R.id.puzzle_menu_scratch_mode) {
                toggleScratchMode();
                return true;
            } else if (id == R.id.puzzle_menu_show_errors_grid) {
                settings.setPlayShowErrorsGrid(!board.isShowErrorsGrid());
                return true;
            } else if (id == R.id.puzzle_menu_show_errors_clue) {
                settings.setPlayShowErrorsClue(!board.isShowErrorsClue());
                return true;
            } else if (id == R.id.puzzle_menu_show_errors_cursor) {
                settings.setPlayShowErrorsCursor(
                    !board.isShowErrorsCursor()
                );
                return true;
            } else if (id == R.id.puzzle_menu_reveal_initial_letter) {
                board.revealInitialLetter();
                return true;
            } else if (id == R.id.puzzle_menu_reveal_letter) {
                board.revealLetter();
                return true;
            } else if (id == R.id.puzzle_menu_reveal_word) {
                board.revealWord();
                return true;
            } if (id == R.id.puzzle_menu_reveal_initial_letters) {
                board.revealInitialLetters();
                return true;
            } else if (id == R.id.puzzle_menu_reveal_errors) {
                board.revealErrors();
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
                Intent i = new Intent(this, SettingsActivity.class);
                this.startActivity(i);
                return true;
            }
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

        Playboard board = getBoard();
        Puzzle puz = getPuzzle();
        if (board == null || puz == null) {
            LOG.info("No puzzle board, puzzle activity finishing.");
            finish();
            return;
        }

        if (puz.getPercentComplete() != 100) {
            long time = puz.getTime();
            firstPlay = (time == 0);
            ImaginaryTimer timer = new ImaginaryTimer(time);
            setTimer(timer);
            timer.start();
        }

        board.addListener(this);

        hasInitialValues = puz.hasInitialValueCells();

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

    protected void registerVoiceCommand(@NonNull VoiceCommand command) {
        viewModel.registerVoiceCommand(command);
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
        return viewModel.getBoard();
    }

    protected Puzzle getPuzzle() {
        return viewModel.getPuzzle();
    }

    protected void saveBoard() {
        viewModel.saveBoard();
    }

    protected boolean isFirstPlay() {
        return firstPlay;
    }

    protected void setTimer(ImaginaryTimer timer) {
        this.timer = timer;
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

    private void specialEntry() {
        SpecialEntryDialog dialog = new SpecialEntryDialog();
        dialog.show(getSupportFragmentManager(), "SpecialEntryDialog");
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
        Playboard board = getBoard();
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
        ChatGPTHelpData.isEnabled(settings, isEnabled -> {
            if (isEnabled) {
                boolean newWord = !Objects.equals(
                    changes.getPreviousWord(), changes.getCurrentWord()
                );

                if (newWord)
                    invalidateOptionsMenu();
            }
        });
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
        viewModel.callCrosswordSolver();
    }

    /**
     * Call external dictionary
     *
     * Protected to allow activity overriding.
     */
    protected void callExternalDictionary() {
        viewModel.callExternalDictionary();
    }

    /**
     * Setup status bar stuff
     *
     * Elevation and colour to match show errors
     */
    protected void setupStatusBar(AppBarLayout appBarLayout) {
        super.setupStatusBar(appBarLayout);

        Observer<Boolean> showErrorsObserver = v -> {
            setStatusBarColor(appBarLayout);
        };
        settings.getLivePlayIndicateShowErrors().observe(this, showErrorsObserver);
        settings.getLivePlayShowErrorsClue().observe(this, showErrorsObserver);
        settings.getLivePlayShowErrorsCursor().observe(this, showErrorsObserver);
        settings.getLivePlayShowErrorsGrid().observe(this, showErrorsObserver);
    }

    // TODO: delete these register commands and register in viewModel
    // instead
    /**
     * Prepared command for inputting word answers
     */
    protected void registerVoiceCommandAnswer() {
        viewModel.registerVoiceCommandAnswer();
    }

    /**
     * Prepared command for inputting letters
     */
    protected void registerVoiceCommandLetter() {
        viewModel.registerVoiceCommandLetter();
    }

    /**
     * Prepared command for jumping to clue number
     */
    protected void registerVoiceCommandNumber() {
        viewModel.registerVoiceCommandNumber();
    }

    /**
     * Prepared command for clearing current word
     */
    protected void registerVoiceCommandClear() {
        viewModel.registerVoiceCommandClear();
    }

    /**
     * Prepared command for announcing current clue
     */
    protected void registerVoiceCommandAnnounceClue() {
        viewModel.registerVoiceCommandAnnounceClue();
    }

    /**
     * Prepared command for announcing current clue
     */
    protected void registerVoiceCommandClueHelp() {
        viewModel.registerVoiceCommandClueHelp();
    }

    private void setupObservers() {
        settings.getLivePlayShowTimer().observe(
            this,
            showTimer -> {
                if (showTimer)
                    handler.post(updateTimeTask);
            }
        );

        viewModel.getShareStreamEvent().observe(
            this,
            shareStream -> {
                Intent shareIntent = new ShareCompat.IntentBuilder(this)
                    .setChooserTitle(shareStream.getTitle())
                    .setStream(shareStream.getUri())
                    .setType(shareStream.getMimeType())
                    .createChooserIntent();
                startActivity(shareIntent);
            }
        );

        viewModel.getAnnounceClueEvent().observe(
            this,
            (v) -> { announceClue(false); }
        );

        viewModel.getSendToastEvent().observe(
            this,
            toast -> {
                Toast.makeText(this, toast.getText(), Toast.LENGTH_LONG).show();
            }
        );

        viewModel.getExternalToolEvent().observe(
            this,
            data -> { data.accept(new ExternalToolLauncher(this)); }
        );

        viewModel.getMenuState().observe(
            this,
            data -> { invalidateOptionsMenu(); }
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
            = settings.getLivePlayIndicateShowErrors().getValue();
        Boolean showErrorsClue = settings.getLivePlayShowErrorsClue().getValue();
        Boolean showErrorsCursor
            = settings.getLivePlayShowErrorsCursor().getValue();
        Boolean showErrorsGrid = settings.getLivePlayShowErrorsGrid().getValue();

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
     * Flag currently selected clue and choose colour
     */
    private void toggleClueFlag() {
        Playboard board = getBoard();
        if (board == null)
            return;

        Clue clue = board.getClue();
        if (clue == null)
            return;

        if (clue.isFlagged()) {
            board.flagClue(clue, false);
        } else {
            DialogFragment dialog = new ChooseFlagColorDialog();
            Bundle args = new Bundle();
            ChooseFlagColorDialog.addClueToBundle(args, clue);
            dialog.setArguments(args);
            dialog.show(getSupportFragmentManager(), "ChooseFlagColorDialog");
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
