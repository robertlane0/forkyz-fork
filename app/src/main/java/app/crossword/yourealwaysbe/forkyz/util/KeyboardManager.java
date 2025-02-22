/**
 * Manage on screen keyboard for Play/Notes/ClueList activity (and others)
 */

package app.crossword.yourealwaysbe.forkyz.util;

import java.util.logging.Logger;

import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardMode;
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardSettings;
import app.crossword.yourealwaysbe.forkyz.view.ForkyzKeyboard;

public class KeyboardManager {
    private static final Logger LOG = Logger.getLogger(KeyboardManager.class.getCanonicalName());

    private AppCompatActivity activity;
    private ForkyzSettings settings;
    private ForkyzKeyboard keyboardView;
    private int blockHideDepth = 0;

    // only enabled when non-native keyboard is showing and back will cause it
    // to hide
    private OnBackPressedCallback backCallback
        = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                hideKeyboard(true);
            }
        };

    /**
     * A view that can be set to take native input
     *
     * If false, assumed to just handle keypresses from ForkyzKeyboard.
     * Needs getView method to get access to the actual view.
     */
    public interface ManageableView {
        /**
         * Indicates view should be in native input mode
         *
         * @param nativeInput if native input
         * @param forceCaps whether to convert all native input into uppercase
         * @return true if this is a change from previous mode
         */
        boolean setNativeInput(boolean nativeInput, boolean forceCaps);

        /**
         * The view that is manageable (usually this)
         */
        View getView();

        /**
         * Create an input connection for the built-in Forkyz keyboard
         */
        InputConnection onCreateForkyzInputConnection(EditorInfo ei);
    }

    /**
     * Create a new manager to handle the keyboard
     *
     * To use, pass on calls to the implemented methods below.
     * Adds a back key handler to activity for when keyboard will take the back
     * key.
     *
     * @param activity the activity the keyboard is for
     * @param settings the settings instance for the app
     * @param keyboardView the keyboard view of the activity
     * @param initialView the view that should have focus immediately if
     * keyboard always show
     */
    public KeyboardManager(
        AppCompatActivity activity,
        ForkyzSettings settings,
        ForkyzKeyboard keyboardView,
        ManageableView initialView
    ) {
        this.activity = activity;
        this.settings = settings;
        this.keyboardView = keyboardView;

        settings.getKeyboardSettings(ks -> {
            if (ks.getMode() == KeyboardMode.KM_ALWAYS_SHOW) {
                showKeyboard(ks, initialView);
            } else {
                hideKeyboard(ks, false);
            }
        });

        activity.getOnBackPressedDispatcher()
            .addCallback(activity, backCallback);
    }

    /**
     * Call this from the activities onResume method
     */
    public void onResume() {
        setHideRowVisibility();

        settings.getKeyboardSettings(ks-> {
            if (ks.getUseNative())
                keyboardView.setVisibility(View.GONE);
        });

        setSoftInputLayout();
    }

    /**
     * Call this when the activity receives an onPause
     */
    public void onPause() {

    }

    /**
     * Call this when the activity receives an onStop
     */
    public void onStop() { }

    /**
     * Call this when the activity receives an onDestroy
     */
    public void onDestroy() { }

    /**
     * Show the keyboard -- must be called after UI drawn
     *
     * @param view the view the keyboard should work for, will request
     * focus
     */
    public void showKeyboard(ManageableView manageableView) {
        settings.getKeyboardSettings(
            ks -> { showKeyboard(ks, manageableView); }
        );
    }

    /**
     * Attach the keyboard to a view without changing visibilty
     */
    public void attachKeyboardToView(ManageableView view) {
        settings.getKeyboardSettings(ks -> {
            if (!ks.getUseNative())
                attachForkyzKeyboardToView(view);
        });
    }

    public void hideKeyboard() { hideKeyboard(false); }

    /**
     * Hide the keyboard unless the user always wants it
     *
     * Will not hide if the user is currently pressing a key
     *
     * @param force force hide the keyboard, even if user has set always
     * show
     */
    public void hideKeyboard(boolean force) {
        settings.getKeyboardSettings(ks -> { hideKeyboard(ks, force); });
    }

    /**
     * Call when a native view (e.g. TextEdit) gets focus
     *
     * Will hide the inapp/native keyboard if needed
     */
    public void onFocusNativeView(View view, boolean gainFocus) {
        settings.getKeyboardSettings(ks -> {
            if (!ks.getUseNative()) {
                if (gainFocus) {
                    hideKeyboard(true);
                } else {
                    InputMethodManager imm = getInputMethodManager();
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
            }
        });
    }

    /**
     * Handle a simulated back action
     *
     * Hides keyboard if mode allows it.
     *
     * @returns true if action was consumed, false if it
     * should be passed on
     */
    public boolean doBackAction() {
        if (backCallback.isEnabled()) {
            backCallback.handleOnBackPressed();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Add a block hide request
     *
     * hideKeyboard will only have an effect if there are no block hide
     * requests (or force was passed to hideKeyboard)
     */
    public void pushBlockHide() { blockHideDepth++; }

    /**
     * Remove a block hide request
     */
    public void popBlockHide() { blockHideDepth--; }

    private boolean isBlockHide() { return blockHideDepth > 0; }

    private void setHideRowVisibility() {
        settings.getKeyboardSettings(ks -> {
            if (ks.getHideButton()) {
                keyboardView.setShowHideButton(
                    ks.getMode() == KeyboardMode.KM_HIDE_MANUAL
                        || ks.getMode() == KeyboardMode.KM_SHOW_SPARINGLY
                    ,
                    () -> { keyboardView.setVisibility(View.GONE); }
                );
            } else {
                keyboardView.setShowHideButton(false);
            }
        });
    }

    /**
     * Sets window-level soft input mode
     *
     * E.g. always show or always hide when native
     */
    private void setSoftInputLayout() {
        settings.getKeyboardSettings(ks -> {
            if (ks.getUseNative()) {
                if (ks.getMode() == KeyboardMode.KM_ALWAYS_SHOW) {
                    activity.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams
                            .SOFT_INPUT_STATE_ALWAYS_VISIBLE
                    );
                } else if (ks.getMode() == KeyboardMode.KM_NEVER_SHOW) {
                    activity.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams
                            .SOFT_INPUT_STATE_ALWAYS_HIDDEN
                    );
                }
            }
        });
    }

    private void attachForkyzKeyboardToView(ManageableView view) {
        keyboardView.setInputConnection(
            view.onCreateForkyzInputConnection(
                keyboardView.getEditorInfo()
            )
        );
    }

    private InputMethodManager getInputMethodManager() {
        return (InputMethodManager)
            activity.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    /**
     * Hides keyboard according to settings
     *
     * @return true if keyboard hide was not blocked by settings
     */
    private boolean hideKeyboard(KeyboardSettings ks, boolean force) {
        if (ks == null)
            return false;

        boolean prefHide =
            ks.getMode() != KeyboardMode.KM_ALWAYS_SHOW
                && ks.getMode() != KeyboardMode.KM_HIDE_MANUAL;
        boolean softHide =
            prefHide && !keyboardView.hasKeysDown() && !isBlockHide();
        boolean doHide = force || softHide;

        if (doHide) {
            if (ks.getUseNative()) {
                View focus = activity.getCurrentFocus();
                if (focus != null) {
                    // turn off native input if can
                    if (focus instanceof ManageableView)
                        ((ManageableView) focus).setNativeInput(false, false);
                    InputMethodManager imm = getInputMethodManager();
                    imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                }
            } else {
                keyboardView.setVisibility(View.GONE);
                backCallback.setEnabled(false);
            }
        }

        return doHide;
    }

    private void showKeyboard(
        KeyboardSettings ks, ManageableView manageableView
    ) {
        if (manageableView == null)
            return;

        View view = manageableView.getView();
        if (view == null)
            return;

       boolean nativeChanged
            = manageableView.setNativeInput(
                ks.getUseNative(), ks.getForceCaps()
            );

        if (
            ks.getMode() != KeyboardMode.KM_NEVER_SHOW
            && view.requestFocus()
        ) {
            if (ks.getUseNative()) {
                InputMethodManager imm = getInputMethodManager();
                if (nativeChanged)
                    imm.restartInput(view);
                imm.showSoftInput(view, 0);
                keyboardView.setVisibility(View.GONE);
            } else {
                keyboardView.setVisibility(View.VISIBLE);
                attachForkyzKeyboardToView(manageableView);
                backCallback.setEnabled(
                    !ks.getHideButton()
                    && ks.getMode() != KeyboardMode.KM_ALWAYS_SHOW
                );
            }
        }
    }
}
