package app.crossword.yourealwaysbe.forkyz;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.squareup.seismic.ShakeDetector;

import app.crossword.yourealwaysbe.forkyz.databinding.PlayBinding;
import app.crossword.yourealwaysbe.forkyz.settings.GridRatio;
import app.crossword.yourealwaysbe.forkyz.util.ImaginaryTimer;
import app.crossword.yourealwaysbe.forkyz.util.KeyboardManager;
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand;
import app.crossword.yourealwaysbe.forkyz.view.BoardEditView.BoardClickListener;
import app.crossword.yourealwaysbe.forkyz.view.ClueTabs;
import app.crossword.yourealwaysbe.forkyz.view.ForkyzKeyboard;
import app.crossword.yourealwaysbe.forkyz.view.PuzzleInfoDialogs;
import app.crossword.yourealwaysbe.forkyz.view.ScrollingImageView.ScaleListener;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Logger;

public class PlayActivity extends PuzzleActivity
                          implements Playboard.PlayboardListener,
                                     ClueTabs.ClueTabsListener,
                                     ShakeDetector.Listener {
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");
    private static final float ACROSTIC_BOARD_HEIGHT_RATIO_MIN = 0.25F;
    private static final float ACROSTIC_CLUE_TABS_WORD_SCALE = 0.7F;
    private static final float ACROSTIC_CLUE_TABS_HEIGHT_RATIO_MIN = 0.3F;

    private PlayBinding binding;
    private Handler handler = new Handler(Looper.getMainLooper());
    private KeyboardManager keyboardManager;
    private boolean hasInitialValues = false;
    private ShakeDetector shakeDetector = null;

    // Proper value is loaded before this is used anyway
    private GridRatio currentGridRatio = GridRatio.ONE_TO_ONE;

    private Runnable fitToScreenTask = new Runnable() {
        @Override
        public void run() {
            PlayActivity.this.fitBoardToScreen();
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidthInInches = (metrics.widthPixels > metrics.heightPixels ? metrics.widthPixels : metrics.heightPixels) / Math.round(160 * metrics.density);
        LOG.info("Configuration Changed "+screenWidthInInches+" ");
        if(screenWidthInInches >= 7){
            this.handler.post(this.fitToScreenTask);
        }
    }

    /**
     * Create the activity
     *
     * This only sets up the UI widgets. The set up for the current
     * puzzle/board is done in onResume as these are held by the
     * application and may change while paused!
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // board is loaded by BrowseActivity and put into the
        // Application, onResume sets up PlayActivity for current board
        // as it may change!
        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        if (board == null || puz == null) {
            LOG.info("PlayActivity started but no Puzzle selected, finishing.");
            finish();
            return;
        }

        binding = PlayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        holographic();
        finishOnHomeButton();
        setupBottomInsets(binding.constraintLayout);

        setDefaultKeyMode(Activity.DEFAULT_KEYS_DISABLE);

        setFullScreenMode();

        binding.clueTabs.setOnClueLongClickDescription(
            getString(R.string.open_clue_notes)
        );
        binding.clueTabs.setOnClueClickDescription(
            getString(R.string.select_clue)
        );
        binding.clueTabs.setOnBarLongClickDescription(
            getString(R.string.toggle_clue_tabs)
        );

        binding.keyboardView.setSpecialKeyListener(
            new ForkyzKeyboard.SpecialKeyListener() {
                @Override
                public void onKeyDown(@ForkyzKeyboard.SpecialKey int key) {
                    // ignore
                }

                @Override
                public void onKeyUp(@ForkyzKeyboard.SpecialKey int key) {
                    // ignore
                    switch (key) {
                    case ForkyzKeyboard.KEY_CHANGE_CLUE_DIRECTION:
                        getBoard().toggleSelection();
                        return;
                    case ForkyzKeyboard.KEY_NEXT_CLUE:
                        getBoard().nextWord();
                        return;
                    case ForkyzKeyboard.KEY_PREVIOUS_CLUE:
                        getBoard().previousWord();
                        return;
                    default:
                        // ignore
                    }
                }
            }
        );

        keyboardManager = new KeyboardManager(
            this, settings, binding.keyboardView, binding.boardView
        );

        settings.getPlaySkipFilled(board::setSkipCompletedLetters);

        createClueLine(binding.clueHolder.clueLine);
        createClueLine(binding.clueLineBelowGrid.clueText);

        this.registerForContextMenu(binding.boardView);
        binding.boardView.addBoardClickListener(new BoardClickListener() {
            @Override
            public void onClick(Position position, Word previousWord) {
                displayKeyboard(previousWord);
            }

            @Override
            public void onLongClick(Position position) {
                Word w = board.setHighlightLetter(position);
                launchClueNotes(board.getClueID());
            }
        });
        ViewCompat.replaceAccessibilityAction(
            binding.boardView,
            AccessibilityActionCompat.ACTION_LONG_CLICK,
            getText(R.string.open_clue_notes),
            null
        );

        // constrain to 1:1 if clueTabs is showing
        // or half of screen if acrostic
        binding.boardView.addOnLayoutChangeListener(
            new View.OnLayoutChangeListener() {
                public void onLayoutChange(View v,
                  int left, int top, int right, int bottom,
                  int leftWas, int topWas, int rightWas, int bottomWas
                ) {
                    boolean showCluesTab
                        = binding.clueTabs.getVisibility() != View.GONE;
                    boolean constrainedDims = false;

                    ConstraintSet set = new ConstraintSet();
                    set.clone(binding.constraintLayout);

                    if (showCluesTab) {
                        int height = bottom - top;
                        int width = right - left;

                        int orientation
                            = PlayActivity.this
                                .getResources()
                                .getConfiguration()
                                .orientation;

                        boolean portrait = (
                            orientation
                                == Configuration.ORIENTATION_PORTRAIT
                        );

                        if (portrait) {
                            int maxHeight = 0;
                            DisplayMetrics metrics
                                = getResources().getDisplayMetrics();
                            if (isAcrostic()) {
                                maxHeight = (int)(
                                    ACROSTIC_BOARD_HEIGHT_RATIO_MIN
                                        * metrics.heightPixels
                                );
                                Puzzle puz = getPuzzle();
                                if (puz != null) {
                                    int proportionalHeight = (int)(
                                        ((float) puz.getHeight())
                                            / puz.getWidth()
                                            * metrics.widthPixels
                                    );
                                    maxHeight = Math.max(
                                        maxHeight, proportionalHeight
                                    );
                                }
                            } else {
                                switch (currentGridRatio) {
                                case ONE_TO_ONE:
                                    maxHeight = width;
                                    break;
                                case THIRTY_PCNT:
                                    maxHeight = (int)(
                                        0.3 * metrics.heightPixels
                                    );
                                    break;
                                case FORTY_PCNT:
                                    maxHeight = (int)(
                                        0.4 * metrics.heightPixels
                                    );
                                    break;
                                case FIFTY_PCNT:
                                    maxHeight = (int)(
                                        0.5 * metrics.heightPixels
                                    );
                                    break;
                                case SIXTY_PCNT:
                                    maxHeight = (int)(
                                        0.6 * metrics.heightPixels
                                    );
                                    break;
                                }
                            }

                            if (height > maxHeight) {
                                constrainedDims = true;
                                set.constrainMaxHeight(
                                    binding.boardView.getId(), maxHeight
                                );
                            }
                        }
                    } else {
                        set.constrainMaxHeight(
                            binding.boardView.getId(), 0
                        );
                    }

                    set.applyTo(binding.constraintLayout);

                    // if the view changed size, then rescale the view
                    // cannot change layout during a layout change, so
                    // use a predraw listener that requests a new layout
                    // and returns false to cancel the current draw
                    if (constrainedDims ||
                        left != leftWas || right != rightWas ||
                        top != topWas || bottom != bottomWas) {
                        binding.boardView
                            .getViewTreeObserver()
                            .addOnPreDrawListener(
                                new ViewTreeObserver.OnPreDrawListener() {
                                    public boolean onPreDraw() {
                                        binding.boardView.relayout();
                                        binding.boardView
                                            .getViewTreeObserver()
                                            .removeOnPreDrawListener(this);
                                        return false;
                                    }
                                }
                            );
                    }
                }
            }
        );

        binding.boardView.setScaleListener(new ScaleListener() {
            public void onScale(float newScale) {
                settings.setPlayScale(newScale);
            }
        });


        binding.clueLineBelowGrid.prevClueButton.setOnClickListener(
            view -> {
                Playboard b = getBoard();
                if (b != null)
                    b.previousWord();
            }
        );
        binding.clueLineBelowGrid.nextClueButton.setOnClickListener(
            view -> {
                Playboard b = getBoard();
                if (b != null)
                    b.nextWord();
            }
        );

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        settings.getPlayFitToScreen(fitToScreen -> {
            if (fitToScreen || (
                    isLandscape(metrics)
                    && (isTabletish(metrics) || isMiniTabletish(metrics))
                )
            ) {
                this.handler.postDelayed(fitToScreenTask, 100);
            }
        });

        setupVoiceButtons(binding.voiceButtonsInclude);
        setupVoiceCommands();

        addAccessibilityActions(binding.boardView);

        setupObservers();
    }

    private void fitBoardToScreen() {
        float newScale = binding.boardView.fitToView();
        settings.setPlayScale(newScale);
    }

    private static String neverNull(String val) {
        return val == null ? "" : val.trim();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.play_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        if (puz == null || puz.getSupportUrl() == null) {
            MenuItem support = menu.findItem(R.id.play_menu_support_source);
            support.setVisible(false);
            support.setEnabled(false);
        }

        settings.getPlayScratchMode(scratchMode -> {
            menu.findItem(R.id.play_menu_scratch_mode).setChecked(scratchMode);
        });

        boolean canSolve = puz != null && puz.hasSolution();

        MenuItem showErrors = menu.findItem(R.id.play_menu_show_errors);
        MenuItem reveal = menu.findItem(R.id.play_menu_reveal);
        showErrors.setEnabled(canSolve);
        showErrors.setVisible(canSolve);
        reveal.setEnabled(canSolve);
        reveal.setVisible(canSolve);

        settings.getPlayShowErrorsClue(showErrorsClue -> {
        settings.getPlayShowErrorsCursor(showErrorsCursor -> {
        settings.getPlayShowErrorsGrid(showErrorsGrid -> {
            int showErrorsTitle =
                (showErrorsGrid || showErrorsCursor || showErrorsClue)
                ? R.string.showing_errors
                : R.string.show_errors;

            showErrors.setTitle(showErrorsTitle);

            menu.findItem(R.id.play_menu_show_errors_grid)
                .setChecked(showErrorsGrid);
            menu.findItem(R.id.play_menu_show_errors_cursor)
                .setChecked(showErrorsCursor);
            menu.findItem(R.id.play_menu_show_errors_clue)
                .setChecked(showErrorsClue);
        });});});

        Box box = (board == null) ? null : board.getCurrentBox();
        boolean hasInitial = !Box.isBlock(box) && box.hasInitialValue();
        menu.findItem(R.id.play_menu_reveal_initial_letter)
            .setVisible(hasInitial);

        menu.findItem(R.id.play_menu_reveal_initial_letters)
            .setVisible(hasInitialValues);

        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = isHandledKey(keyCode, event);
        if (!handled)
            return super.onKeyDown(keyCode, event);
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        int flags = event.getFlags();

        boolean handled = isHandledKey(keyCode, event);
        if (!handled)
            return super.onKeyUp(keyCode, event);

        int cancelled = event.getFlags()
            & (KeyEvent.FLAG_CANCELED | KeyEvent.FLAG_CANCELED_LONG_PRESS);
        if (cancelled > 0)
            return true;

        // handle back separately as it we shouldn't block a keyboard
        // hide because of it
        if (keyCode == KeyEvent.KEYCODE_BACK
                || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            keyboardManager.handleBackKey(keyHandled -> {
                if (!keyHandled)
                    this.finish();
            });
        }

        keyboardManager.pushBlockHide();

        if (getBoard() != null) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_SEARCH:
                    getBoard().nextWord();
                    break;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    onDownKey();
                    break;

                case KeyEvent.KEYCODE_DPAD_UP:
                    onUpKey();
                    break;

                case KeyEvent.KEYCODE_DPAD_LEFT:
                    onLeftKey();
                    break;

                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    onRightKey();
                    break;

                case KeyEvent.KEYCODE_DPAD_CENTER:
                    getBoard().toggleSelection();
                    break;

                case KeyEvent.KEYCODE_SPACE:
                    settings.getPlaySpaceChangesDirection(changesDir -> {
                        if (changesDir)
                            getBoard().toggleSelection();
                        else
                            playLetter(' ');
                    });
                    break;

                case KeyEvent.KEYCODE_ENTER:
                    settings.getPlayEnterChangesDirection(enterChanges -> {
                        if (enterChanges)
                            getBoard().toggleSelection();
                        else
                            getBoard().nextWord();
                    });
                    break;

                case KeyEvent.KEYCODE_DEL:
                    onDeleteKey();
                    break;
            }

            char c = Character.toUpperCase(event.getDisplayLabel());

            if (Character.isLetterOrDigit(c))
                playLetter(c);
        }

        keyboardManager.popBlockHide();

        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        if (getBoard() != null) {
            if (id == R.id.play_menu_reveal_initial_letter) {
                getBoard().revealInitialLetter();
                return true;
            } if (id == R.id.play_menu_reveal_letter) {
                getBoard().revealLetter();
                return true;
            } else if (id == R.id.play_menu_reveal_word) {
                getBoard().revealWord();
                return true;
            } if (id == R.id.play_menu_reveal_initial_letters) {
                getBoard().revealInitialLetters();
                return true;
            } else if (id == R.id.play_menu_reveal_errors) {
                getBoard().revealErrors();
                return true;
            } else if (id == R.id.play_menu_reveal_puzzle) {
                showRevealPuzzleDialog();
                return true;
            } else if (id == R.id.play_menu_show_errors_grid) {
                settings.setPlayShowErrorsGrid(!getBoard().isShowErrorsGrid());
                return true;
            } else if (id == R.id.play_menu_show_errors_clue) {
                settings.setPlayShowErrorsClue(!getBoard().isShowErrorsClue());
                return true;
            } else if (id == R.id.play_menu_show_errors_cursor) {
                settings.setPlayShowErrorsCursor(
                    !getBoard().isShowErrorsCursor()
                );
                return true;
            } else if (id == R.id.play_menu_scratch_mode) {
                toggleScratchMode();
                return true;
            } else if (id == R.id.play_menu_settings) {
                Intent i = new Intent(this, PreferencesActivity.class);
                this.startActivity(i);
                return true;
            } else if (id == R.id.play_menu_zoom_in) {
                float newScale = binding.boardView.zoomIn();
                settings.setPlayScale(newScale);
                return true;
            } else if (id == R.id.play_menu_zoom_in_max) {
                float newScale = binding.boardView.zoomInMax();
                settings.setPlayScale(newScale);
                return true;
            } else if (id == R.id.play_menu_zoom_out) {
                float newScale = binding.boardView.zoomOut();
                settings.setPlayScale(newScale);
                return true;
            } else if (id == R.id.play_menu_zoom_fit) {
                fitBoardToScreen();
                return true;
            } else if (id == R.id.play_menu_zoom_reset) {
                float newScale = binding.boardView.zoomReset();
                settings.setPlayScale(newScale);
                return true;
            } else if (id == R.id.play_menu_info) {
                showInfoDialog();
                return true;
            } else if (id == R.id.play_menu_clues) {
                PlayActivity.this.launchClueList();
                return true;
            } else if (id == R.id.play_menu_clue_notes) {
                launchClueNotes(getBoard().getClueID());
                return true;
            } else if (id == R.id.play_menu_player_notes) {
                launchPuzzleNotes();
                return true;
            } else if (id == R.id.play_menu_help) {
                Intent helpIntent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("playscreen.html"),
                    this,
                    HTMLActivity.class
                );
                this.startActivity(helpIntent);
                return true;
            } else if (id == R.id.play_menu_support_source) {
                actionSupportSource();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClueTabsClick(Clue clue, ClueTabs view) {
        Playboard board = getBoard();
        if (board == null)
            return;
        onClueTabsClickGeneral(clue, board.getCurrentWord());
    }

    @Override
    public void onClueTabsBoardClick(
        Clue clue, Word previousWord, ClueTabs view
    ) {
        onClueTabsClickGeneral(clue, previousWord);
    }

    @Override
    public void onClueTabsLongClick(Clue clue, ClueTabs view) {
        Playboard board = getBoard();
        if (board == null)
            return;
        board.jumpToClue(clue);
        launchClueNotes(clue);
    }

    @Override
    public void onClueTabsBarSwipeDown(ClueTabs view) {
        hideClueTabs();
    }

    @Override
    public void onClueTabsBarLongclick(ClueTabs view) {
        hideClueTabs();
    }

    @Override
    public void onClueTabsPageChange(ClueTabs view, int pageNumber) {
        settings.setPlayClueTabsPage(pageNumber);
    }

    public void onPlayboardChange(PlayboardChanges changes) {
        super.onPlayboardChange(changes);

        Word previousWord = changes.getPreviousWord();

        Position newPos = getBoard().getHighlightLetter();

        boolean isNewWord = (previousWord == null) ||
            !previousWord.checkInWord(newPos);

        if (isNewWord) {
            // hide keyboard when moving to a new word
            keyboardManager.hideKeyboard();
        }

        setClueText();

        // changed cells could mean change in reveal letters options
        if (hasInitialValues)
            invalidateOptionsMenu();
    }

    @Override
    protected void onTimerUpdate() {
        super.onTimerUpdate();

        Puzzle puz = getPuzzle();
        ImaginaryTimer timer = getTimer();

        if (puz != null && timer != null) {
            getWindow().setTitle(timer.time());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        keyboardManager.onPause();

        Playboard board = getBoard();
        if (board != null)
            board.removeListener(this);

        binding.clueTabs.removeListener(this);

        pauseShakeDetection();
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.onConfigurationChanged(getBaseContext().getResources()
                                                    .getConfiguration());

        setVoiceButtonVisibility();
        registerBoard();

        if (keyboardManager != null)
            keyboardManager.onResume();

        handleFirstPlay();
        resumeShakeDetection();
    }

    private void registerBoard() {
        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        if (board == null || puz == null) {
            LOG.info("PlayActivity resumed but no Puzzle selected, finishing.");
            finish();
            return;
        }

        setActivityTitle();
        binding.boardView.setBoard(board);

        settings.getPlayScale(scale-> {
            scale = binding.boardView.setCurrentScale(scale);
            settings.setPlayScale(scale);
        });

        binding.clueTabs.setBoard(board);
        binding.clueTabs.setMaxWordScale(ACROSTIC_CLUE_TABS_WORD_SCALE);
        binding.clueTabs.setShowWords(isAcrostic());
        settings.getPlayClueTabsPage(page -> {
            binding.clueTabs.setPage(page);
            binding.clueTabs.addListener(this);
        });

        settings.getPlaySkipFilled(board::setSkipCompletedLetters);
        settings.getPlayMovementStrategy(movementStrategy -> {
            board.setMovementStrategy(movementStrategy);
        });
        board.addListener(this);

        keyboardManager.attachKeyboardToView(binding.boardView);

        setClueText();

        hasInitialValues = puz.hasInitialValueCells();
        // always invalidate as anything in puzzle could have changed
        invalidateOptionsMenu();
    }

    @Override
    public void hearShake() {
        settings.getPlayRandomClueOnShake(randomClueOnShake -> {
            if (randomClueOnShake)
                pickRandomUnfilledClue();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (keyboardManager != null)
            keyboardManager.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (keyboardManager != null)
            keyboardManager.onDestroy();
    }

    /**
     * Change keyboard display if the same word has been selected twice
     */
    private void displayKeyboard(Word previous) {
        // only show keyboard if double click a word
        // hide if it's a new word
        Playboard board = getBoard();
        if (board != null) {
            Position newPos = board.getHighlightLetter();
            if ((previous != null) &&
                previous.checkInWord(newPos.getRow(), newPos.getCol())) {
                keyboardManager.showKeyboard(binding.boardView);
            } else {
                keyboardManager.hideKeyboard();
            }
        }
    }

    private void setupObservers() {
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

        settings.livePlayGridRatio().observe(
            this,
            gridRatio -> {
                if (currentGridRatio != gridRatio)
                    makeClueTabsInvisible();
                currentGridRatio = gridRatio;
                settings.getPlayShowCluesTab(showCluesTab -> {
                    if (showCluesTab) {
                        showClueTabs();
                    } else {
                        hideClueTabs();
                    }
                });
            }
        );

        settings.livePlayClueBelowGrid().observe(
            this,
            belowGrid -> {
                binding.clueLineBelowGrid.layout.setVisibility(
                    belowGrid ? View.VISIBLE : View.GONE
                );

                if (!belowGrid)
                    setClueLineTextSize(binding.clueHolder.clueLine);

                setClueText();
                setActivityTitle();
            }
        );
    }

    private void setClueText() {
        Playboard board = getBoard();
        if (board == null)
            return;

        Clue c = board.getClue();
        getLongClueText(c, text -> {
        settings.getPlayClueBelowGrid(belowGrid -> {
            Spanned htmlText = smartHtml(text);
            if (belowGrid) {
                binding.clueLineBelowGrid.clueText.setText(
                    htmlText
                );
            } else {
                binding.clueHolder.clueLine.setText(htmlText);
            }
        });});
    }

    private void launchClueList() {
        Intent i = new Intent(this, ClueListActivity.class);
        PlayActivity.this.startActivity(i);
    }

    /**
     * Changes the constraints on clue tabs to show.
     *
     * Updates shared prefs.
     */
    private void showClueTabs() {
        int clueTabsId = binding.clueTabs.getId();

        ConstraintSet set = new ConstraintSet();
        set.clone(binding.constraintLayout);
        set.setVisibility(clueTabsId, ConstraintSet.VISIBLE);
        if (isAcrostic()) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int minHeight = (int)(
                ACROSTIC_CLUE_TABS_HEIGHT_RATIO_MIN * metrics.heightPixels
            );
            set.constrainMinHeight(clueTabsId, minHeight);
        }
        set.applyTo(binding.constraintLayout);

        settings.setPlayShowCluesTab(true);
    }

    /**
     * Changes the constraints on clue tabs to hide.
     *
     * Updates shared prefs.
     */
    private void hideClueTabs() {
        makeClueTabsInvisible();
        settings.setPlayShowCluesTab(false);
    }

    /**
     * Hides clue tabs but does not change show clue tabs setting
     *
     * Normally use hideClueTabs. This is also used when gridRatio
     * changes as setting the grid max height cannot make the grid
     * larger.
     */
    private void makeClueTabsInvisible() {
        ConstraintSet set = new ConstraintSet();
        set.clone(binding.constraintLayout);
        set.setVisibility(binding.clueTabs.getId(), ConstraintSet.GONE);
        set.applyTo(binding.constraintLayout);
    }

    private void showInfoDialog() {
        DialogFragment dialog = new PuzzleInfoDialogs.Info();
        dialog.show(getSupportFragmentManager(), "PuzzleInfoDialgs.Info");
    }

    private void showRevealPuzzleDialog() {
        DialogFragment dialog = new RevealPuzzleDialog();
        dialog.show(getSupportFragmentManager(), "RevealPuzzleDialog");
    }

    private void setFullScreenMode() {
        settings.getPlayFullScreen(fullScreen -> {
            if (fullScreen)
                utils.setFullScreen(getWindow());
        });
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

    private void toggleScratchMode() {
        settings.getPlayScratchMode(scratchMode -> {
            settings.setPlayScratchMode(!scratchMode);
        });
    }

    private void setupVoiceCommands() {
        registerVoiceCommandAnswer();
        registerVoiceCommandLetter();
        registerVoiceCommandNumber();
        registerVoiceCommandClear();
        registerVoiceCommandAnnounceClue();
        registerVoiceCommandClueHelp();

        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_delete),
            args -> { onDeleteKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_toggle),
            args -> {
                Playboard board = getBoard();
                if (board != null)
                    board.toggleSelection();
            }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_next),
            args -> {
                Playboard board = getBoard();
                if (board != null)
                    board.nextWord();
            }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_previous),
            args -> {
                Playboard board = getBoard();
                if (board != null)
                    board.previousWord();
            }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_left),
            args -> { onLeftKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_right),
            args -> { onRightKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_up),
            args -> { onUpKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_down),
            args -> { onDownKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_back),
            args -> { onBackKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_clues),
            args -> { launchClueList(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_notes),
            args -> {
                Playboard board = getBoard();
                if (board != null)
                    launchClueNotes(board.getClueID());
            }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_jump_random),
            args -> { pickRandomUnfilledClue(); }
        ));
    }

    private void onLeftKey() {
        Playboard board = getBoard();
        if (board != null)
            board.moveLeft();
    }

    private void onRightKey() {
        Playboard board = getBoard();
        if (board != null)
            board.moveRight();
    }

    private void onDownKey() {
        Playboard board = getBoard();
        if (board != null)
            board.moveDown();
    }

    private void onUpKey() {
        Playboard board = getBoard();
        if (board != null)
            board.moveUp();
    }

    private void onBackKey() {
        keyboardManager.handleBackKey(keyHandled -> {
            if (!keyHandled)
                this.finish();
        });
    }

    private void onDeleteKey() {
        Playboard board = getBoard();
        if (board == null)
            return;

        settings.getPlayScratchMode(scratchMode -> {
            if (scratchMode)
                board.deleteScratchLetter();
            else
                board.deleteLetter();
        });
    }

    private void playLetter(char c) {
        settings.getPlayScratchMode(scratchMode -> {
            if (scratchMode)
                getBoard().playScratchLetter(c);
            else
                getBoard().playLetter(c);
        });
    }

    private boolean isAcrostic() {
        Puzzle puz = getPuzzle();
        return puz == null
            ? false
            : Puzzle.Kind.ACROSTIC.equals(puz.getKind());
    }

    /**
     * Handle a click on the clue tabs
     *
     * @param clue the clue clicked
     * @param the previously selected word since last board update (a
     * clue tabs board click might have changed the word)
     */
    private void onClueTabsClickGeneral(Clue clue, Word previousWord) {
        Playboard board = getBoard();
        if (board == null)
            return;

        if (clue.hasZone()) {
            if (!Objects.equals(clue.getClueID(), board.getClueID()))
                board.jumpToClue(clue);
            displayKeyboard(previousWord);
        }
    }

    private void handleFirstPlay() {
        if (!isFirstPlay())
            return;

        Puzzle puz = getPuzzle();
        if (puz == null || !puz.hasIntroMessage())
            return;

        DialogFragment dialog = new PuzzleInfoDialogs.Intro();
        dialog.show(getSupportFragmentManager(), "PuzzleInfoDialogs.Intro");
    }

    private void resumeShakeDetection() {
        settings.getPlayRandomClueOnShake(randomClueOnShake -> {
            if (randomClueOnShake) {
                if (shakeDetector == null)
                    shakeDetector = new ShakeDetector(this);
                shakeDetector.start(
                    (SensorManager) getSystemService(SENSOR_SERVICE),
                    SensorManager.SENSOR_DELAY_GAME
                );
            }
        });
    }

    private void pauseShakeDetection() {
        if (shakeDetector != null)
            shakeDetector.stop();
    }

    private void pickRandomUnfilledClue() {
        Playboard board = getBoard();
        Puzzle puz = getPuzzle();
        if (board == null || puz == null)
            return;

        ClueID currentID = board.getClueID();

        List<Clue> unfilledClues = new ArrayList<>();
        for (Clue clue : puz.getAllClues()) {
            ClueID cid = clue.getClueID();
            boolean current = Objects.equals(currentID, cid);

            if (!current && !board.isFilledClueID(clue.getClueID()))
                unfilledClues.add(clue);
        }

        if (unfilledClues.size() > 0) {
            // bit inefficient, but saves a field
            Random rand = new Random();
            int idx = rand.nextInt(unfilledClues.size());
            board.jumpToClue(unfilledClues.get(idx));
        }
    }

    /**
     * Is a key we'll handle
     *
     * Should match onKeyUp and onKeyDown
     */
    private boolean isHandledKey(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_ESCAPE:
        case KeyEvent.KEYCODE_SEARCH:
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_DPAD_DOWN:
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_SPACE:
        case KeyEvent.KEYCODE_ENTER:
        case KeyEvent.KEYCODE_DEL:
            return true;
        }

        char c = Character.toUpperCase(event.getDisplayLabel());
        if (Character.isLetterOrDigit(c))
            return true;

        return false;
    }

    private static boolean isLandscape(DisplayMetrics metrics){
        return metrics.widthPixels > metrics.heightPixels;
    }

    private static boolean isTabletish(DisplayMetrics metrics) {
        double x = Math.pow(metrics.widthPixels / metrics.xdpi, 2);
        double y = Math.pow(metrics.heightPixels / metrics.ydpi, 2);
        double screenInches = Math.sqrt(x + y);
        if (screenInches > 9) { // look for a 9" or larger screen.
            return true;
        } else {
            return false;
        }
    }

    private boolean isMiniTabletish(DisplayMetrics metrics) {
        return utils.isMiniTabletish(metrics);
    }

    /**
     * Setup the clue line click listeners &c.
     *
     * Move into a method since there are two clue lines to set up: the one in
     * the title and the one under the board, depending on what is configured
     * to be used.
     */
    private void createClueLine(TextView clueLine) {
        clueLine.setClickable(true);
        clueLine.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                settings.getPlayShowCluesTab(show -> {
                    if (show)
                        PlayActivity.this.hideClueTabs();
                    else
                        PlayActivity.this.showClueTabs();
                });
            }
        });
        ViewCompat.replaceAccessibilityAction(
            clueLine,
            AccessibilityActionCompat.ACTION_CLICK,
            getText(R.string.toggle_clue_tabs),
            null
        );
        clueLine.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View arg0) {
                PlayActivity.this.launchClueList();
                return true;
            }
        });
        ViewCompat.replaceAccessibilityAction(
            clueLine,
            AccessibilityActionCompat.ACTION_LONG_CLICK,
            getText(R.string.open_clue_list),
            null
        );

        setClueLineTextSize(clueLine);
    }

    private void setClueLineTextSize(TextView clueLine) {
        int clueTextSize
            = getResources().getInteger(R.integer.clue_text_size);
        int minClueTextSize
            = getResources().getInteger(R.integer.min_clue_text_size);

        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            clueLine,
            minClueTextSize,
            clueTextSize,
            1,
            TypedValue.COMPLEX_UNIT_SP
        );
    }

    private String getPuzzleTitle() {
        Puzzle puz = getPuzzle();
        if (puz == null) {
            return getString(R.string.app_name);
        } else {
            String title = puz.getTitle();
            if (title != null)
                return title;
            title = puz.getSource();
            if (title != null)
                return title;
            return getString(R.string.app_name);
        }
    }

    private void setActivityTitle() {
        String title = getPuzzleTitle();
        setTitle(title);
        settings.getPlayClueBelowGrid(belowGrid -> {
            if (belowGrid) {
                binding.clueHolder.clueLine.setText(title);
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                    binding.clueHolder.clueLine,
                    getResources().getInteger(R.integer.min_clue_text_size),
                    getResources().getInteger(R.integer.title_text_size),
                    1,
                    TypedValue.COMPLEX_UNIT_SP
                );
            }
        });
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
                                = ((PlayActivity) getActivity()).getBoard();
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
