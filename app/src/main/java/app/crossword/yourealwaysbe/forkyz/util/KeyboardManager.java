/**
 * Manage on screen keyboard for Play/Notes/ClueList activity (and others)
 */

package app.crossword.yourealwaysbe.forkyz.util;

import java.util.function.Consumer;
import java.util.logging.Logger;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardMode;
import app.crossword.yourealwaysbe.forkyz.view.ForkyzKeyboard;

public class KeyboardManager {
    private static final Logger LOG = Logger.getLogger(KeyboardManager.class.getCanonicalName());

    private Activity activity;
    private ForkyzSettings settings;
    private ForkyzKeyboard keyboardView;
    private int blockHideDepth = 0;

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
         * @return true if this is a change from previous mode
         */
        boolean setNativeInput(boolean nativeInput);

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
     *
     * @param activity the activity the keyboard is for
     * @param settings the settings instance for the app
     * @param keyboardView the keyboard view of the activity
     * @param initialView the view that should have focus immediately if
     * keyboard always show
     */
    public KeyboardManager(
        Activity activity,
        ForkyzSettings settings,
        ForkyzKeyboard keyboardView,
        ManageableView initialView
    ) {
        this.activity = activity;
        this.settings = settings;
        this.keyboardView = keyboardView;

        settings.getKeyboardMode(mode -> {
            if (mode == KeyboardMode.ALWAYS_SHOW) {
                showKeyboard(initialView);
            } else {
                hideKeyboard();
            }
        });
    }

    /**
     * Call this from the activities onResume method
     */
    public void onResume() {
        setHideRowVisibility();

        settings.getKeyboardNative(nativeKeyboard -> {
            if (nativeKeyboard)
                keyboardView.setVisibility(View.GONE);
        });

        setSoftInputLayout();
    }

    /**
     * Call this when the activity receives an onPause
     */
    public void onPause() {
        keyboardView.onPause();
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
        if (manageableView == null)
            return;

        View view = manageableView.getView();
        if (view == null)
            return;

        settings.getKeyboardNative(nativeKeyboard -> {
            boolean nativeChanged
                = manageableView.setNativeInput(nativeKeyboard);

            settings.getKeyboardMode(mode -> {
                if (mode != KeyboardMode.NEVER_SHOW && view.requestFocus()) {
                    if (nativeKeyboard) {
                        InputMethodManager imm = getInputMethodManager();
                        if (nativeChanged)
                            imm.restartInput(view);
                        imm.showSoftInput(view, 0);
                        keyboardView.setVisibility(View.GONE);
                    } else {
                        keyboardView.setVisibility(View.VISIBLE);
                        attachForkyzKeyboardToView(manageableView);
                    }
                }
            });
        });
    }

    /**
     * Attach the keyboard to a view without changing visibilty
     */
    public void attachKeyboardToView(ManageableView view) {
        settings.getKeyboardNative(nativeKeyboard -> {
            if (!nativeKeyboard)
                attachForkyzKeyboardToView(view);
        });
    }

    public void hideKeyboard() { hideKeyboard(false); }

    public void hideKeyboard(boolean force) { hideKeyboard(force, null); }

    /**
     * Hide the keyboard unless the user always wants it
     *
     * Will not hide if the user is currently pressing a key
     *
     * @param force force hide the keyboard, even if user has set always
     * show
     * @param cb accept true if the hide request was not blocked by settings or
     * pushBlockHide or null if don't care
     */
    public void hideKeyboard(boolean force, Consumer<Boolean> cb) {
        settings.getKeyboardMode(mode -> {
        settings.getKeyboardNative(nativeKeyboard -> {
            boolean prefHide =
                mode != KeyboardMode.ALWAYS_SHOW
                    && mode != KeyboardMode.HIDE_MANUAL;
            boolean softHide =
                prefHide && !keyboardView.hasKeysDown() && !isBlockHide();
            boolean doHide = force || softHide;

            if (doHide) {
                if (nativeKeyboard) {
                    View focus = activity.getCurrentFocus();
                    if (focus != null) {
                        // turn off native input if can
                        if (focus instanceof ManageableView)
                            ((ManageableView) focus).setNativeInput(false);
                        InputMethodManager imm = getInputMethodManager();
                        imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                    }
                } else {
                    keyboardView.setVisibility(View.GONE);
                }
            }

            if (cb != null)
                cb.accept(doHide);
        });});
    }

    /**
     * Call when a native view (e.g. TextEdit) gets focus
     *
     * Will hide the inapp/native keyboard if needed
     */
    public void onFocusNativeView(View view, boolean gainFocus) {
        settings.getKeyboardNative(nativeKeyboard -> {
            if (!nativeKeyboard) {
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
     * Handle back key
     *
     * Hides keyboard if mode allows it.
     *
     * @param cb accept true if key press was consumed, false if it
     * should be passed on
     */
    public void handleBackKey(Consumer<Boolean> cb) {
        settings.getKeyboardMode(mode -> {
        settings.getKeyboardHideButton(hideButton -> {
            boolean force = mode != KeyboardMode.ALWAYS_SHOW;
            boolean toHide =
                keyboardView.getVisibility() == View.VISIBLE
                    && !hideButton;
            hideKeyboard(
                force,
                hidden -> { cb.accept(toHide && hidden); }
            );
        });});
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
        settings.getKeyboardHideButton(hideButton -> {
            if (hideButton) {
                settings.getKeyboardMode(mode -> {
                    keyboardView.setShowHideButton(
                        mode == KeyboardMode.HIDE_MANUAL
                            || mode == KeyboardMode.SHOW_SPARINGLY
                    );
                });
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
        settings.getKeyboardNative(nativeKeyboard -> {
            if (nativeKeyboard) {
                settings.getKeyboardMode(mode -> {
                    if (mode == KeyboardMode.ALWAYS_SHOW) {
                        activity.getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                        );
                    } else if (mode == KeyboardMode.NEVER_SHOW) {
                        activity.getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                        );
                    }
                });
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
}
