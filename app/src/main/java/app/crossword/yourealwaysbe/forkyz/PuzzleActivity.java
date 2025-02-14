package app.crossword.yourealwaysbe.forkyz;

import java.util.function.Consumer;
import java.util.logging.Logger;

import android.content.ActivityNotFoundException;
import android.content.Context;
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
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.appbar.AppBarLayout;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.databinding.VoiceButtonsBinding;
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolLauncher;
import app.crossword.yourealwaysbe.forkyz.inttools.InternalToolLauncher;
import app.crossword.yourealwaysbe.forkyz.util.ImaginaryTimer;
import app.crossword.yourealwaysbe.forkyz.util.SpeechContract;
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand;
import app.crossword.yourealwaysbe.forkyz.view.PlayboardTextRenderer;
import app.crossword.yourealwaysbe.forkyz.view.PuzzleInfoDialogs;
import app.crossword.yourealwaysbe.forkyz.view.RevealPuzzleDialog;
import app.crossword.yourealwaysbe.forkyz.view.SpecialEntryDialog;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;

@AndroidEntryPoint
public abstract class PuzzleActivity
        extends ForkyzActivity
        implements Playboard.PlayboardListener {

    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

    private VoiceButtonsBinding voiceBinding = null;
    private Handler handler = new Handler(Looper.getMainLooper());
    private TextToSpeech ttsService = null;
    private boolean ttsReady = false;
    private boolean updatingTimer = false;

    private Runnable updateTimeTask = new Runnable() {
        public void run() {
            PuzzleActivity.this.onTimerUpdate();
        }
    };

    private PuzzleActivityViewModel viewModel;
    private AppBarLayout appBarLayout = null;

    private ActivityResultLauncher<String> voiceInputLauncher
        = registerForActivityResult(new SpeechContract(), text -> {
            viewModel.dispatchVoiceCommand(text);
        });

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        viewModel = new ViewModelProvider(this)
            .get(PuzzleActivityViewModel.class);

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

    @Override
    public void onPlayboardChange(PlayboardChanges changes) {
        // for override only
        // remove when everyone has a view model
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
            Playboard board = getBoard();
            if (board != null)
                launchClueNotes(board.getClueID());
            return true;
        } else if (id == R.id.puzzle_menu_puzzle_notes) {
            launchPuzzleNotes();
            return true;
        } else if (id == R.id.puzzle_menu_flag_clue_toggle) {
            viewModel.toggleClueFlag();
            return true;
        } else if (id == R.id.puzzle_menu_flag_cell_toggle) {
            viewModel.toggleCellFlag();
            return true;
        } else if (id == R.id.puzzle_menu_scratch_mode) {
            viewModel.toggleScratchMode();
            return true;
        } else if (id == R.id.puzzle_menu_show_errors_grid) {
            viewModel.toggleShowErrorsGrid();
            return true;
        } else if (id == R.id.puzzle_menu_show_errors_clue) {
            viewModel.toggleShowErrorsClue();
            return true;
        } else if (id == R.id.puzzle_menu_show_errors_cursor) {
            viewModel.toggleShowErrorsCursor();
            return true;
        } else if (id == R.id.puzzle_menu_reveal_initial_letter) {
            viewModel.revealInitialLetter();
            return true;
        } else if (id == R.id.puzzle_menu_reveal_letter) {
            viewModel.revealLetter();
            return true;
        } else if (id == R.id.puzzle_menu_reveal_word) {
            viewModel.revealWord();
            return true;
        } if (id == R.id.puzzle_menu_reveal_initial_letters) {
            viewModel.revealInitialLetters();
            return true;
        } else if (id == R.id.puzzle_menu_reveal_errors) {
            viewModel.revealErrors();
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

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        viewModel.stopTimer();
        viewModel.saveBoard();

        viewModel.unlistenBoard();
        // remove when everyone has a view model
        Playboard board = getBoard();
        if (board != null)
            board.removeListener(this);

        if (ttsService != null) {
            ttsService.shutdown();
            ttsService = null;
            ttsReady = false;
        }

        // start again on resume if updating
        handler.removeCallbacks(updateTimeTask);
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

        viewModel.startTimer();
        // start updating again
        if (updatingTimer)
            handler.postDelayed(updateTimeTask, 1000);

        viewModel.listenBoard();
        // remove when everyone has a view model
        board.addListener(this);

        // anything in puzzle could have changed
        invalidateOptionsMenu();
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
                viewModel.announceClue(false);
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

    // remove when all activities can talk to their view models
    public ImaginaryTimer getTimer() {
        return viewModel.getTimer();
    }

    protected void registerVoiceCommand(@NonNull VoiceCommand command) {
        viewModel.registerVoiceCommand(command);
    }

    /**
     * Whether the current puzzle has initial values
     */
    protected boolean getHasInitialValues() {
        return viewModel.getPuzzleHasInitialValues();
    }

    /**
     * Override if you want to update your UI based on the timer
     *
     * But still call super. Only called if the showTimer pref is true
     */
    protected void onTimerUpdate() {
        if (updatingTimer)
            handler.postDelayed(updateTimeTask, 1000);
    }

    protected Playboard getBoard(){
        return viewModel.getBoard();
    }

    protected Puzzle getPuzzle() {
        return viewModel.getPuzzle();
    }

    protected boolean isFirstPlay() {
        return viewModel.isFirstPlay();
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
        String url = viewModel.getSupportURL();
        if (url != null) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
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
        VoiceState state = viewModel.getVoiceState().getValue();
        return state != null && state.getVolumeActivatesVoice();
    }

    protected void setupVoiceButtons(VoiceButtonsBinding binding) {
        voiceBinding = binding;
        if (voiceBinding == null)
            return;

        voiceBinding.voiceButton.setOnClickListener(view -> {
            launchVoiceInput();
        });
        voiceBinding.announceClueButton.setOnClickListener(view -> {
            viewModel.announceClue(false);
        });
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
                viewModel.announceClue(true);
                return true;
            }
        );
    }

    protected boolean isAnnounceClueEquals() {
        VoiceState state = viewModel.getVoiceState().getValue();
        return state != null && state.getEqualsAnnounceClue();
    }

    // remove when all activities talk to view model
    // notes activity currently overrides this for its own behaviour
    // so menu should call this not go to view model directly
    protected void editClue() {
        viewModel.editClue();
    }

    // remove when all activities talk to view model
    protected void editClue(ClueID cid) {
        viewModel.editClue(cid);
    }

    private boolean isAccessibilityServiceRunning() {
        AccessibilityManager manager
            = (AccessibilityManager) getSystemService(
                Context.ACCESSIBILITY_SERVICE
            );
        return manager != null && manager.isEnabled();
    }

    /**
     * Announce text with accessibility if running or tts
     */
    private void announce(AnnounceData data) {
        CharSequence text = data.getMessage();
        if (isAccessibilityServiceRunning()) {
            View content = findViewById(android.R.id.content);
            if (content != null && text != null)
                content.announceForAccessibility(text);
        } else {
            if (ttsService == null) {
                ttsService = new TextToSpeech(
                    getApplicationContext(),
                    (int status) -> {
                        if (status == TextToSpeech.SUCCESS) {
                            ttsReady = true;
                            announce(data);
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
        this.appBarLayout = appBarLayout;
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
        viewModel.getAnnounceEvent().observe(this, this::announce);

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

        viewModel.getInternalToolEvent().observe(
            this,
            data -> { data.accept(new InternalToolLauncher(this)); }
        );

        viewModel.getMenuState().observe(
            this,
            data -> { invalidateOptionsMenu(); }
        );

        viewModel.getVoiceState().observe(
            this,
            state -> {
                if (voiceBinding != null) {
                    boolean buttonVoice = state.getShowVoiceButton();
                    boolean buttonAnnounce = state.getShowAnnounceButton();
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
                }
            }
        );

        viewModel.getPuzzleFinishedEvent().observe(
            this,
            (finished) -> {
                if (finished) {
                    DialogFragment dialog = new PuzzleInfoDialogs.Finished();
                    dialog.show(
                        PuzzleActivity.this.getSupportFragmentManager(),
                        "PuzzleInfoDialogs.Finished"
                    );
                }
            }
        );

        viewModel.getUiState().observe(
            this,
            state -> {
                updateShowTimer(state);
                setStatusBarColor(state);
            }
        );
    }

    private void updateShowTimer(PuzzleActivityUIState state) {
        if (state.getShowTimer() && !updatingTimer) {
            handler.postDelayed(updateTimeTask, 1000);
        }
        updatingTimer = state.getShowTimer();
    }

    private void setStatusBarColor(PuzzleActivityUIState state) {
        if (appBarLayout == null)
            return;

        if (state.getIndicateShowErrors()) {
            setStatusBarColor(
                appBarLayout,
                ContextCompat.getColor(this, R.color.cheatedColor)
            );
        } else {
            resetStatusBarColor(appBarLayout);
        }
    }
}
