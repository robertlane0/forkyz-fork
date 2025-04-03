/**
 * Deliberately not using Keyboard or KeyboardView as they are deprecated.
 * First principles approach preferred instead.
 */

package app.crossword.yourealwaysbe.forkyz.view;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.inject.Inject;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.IntDef;
import androidx.core.view.LayoutInflaterCompat;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import com.google.android.material.button.MaterialButton;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardSettings;
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardLayout;

@AndroidEntryPoint
public class ForkyzKeyboard
    extends LinearLayout
    implements View.OnTouchListener, View.OnClickListener {

    private static final String FORKYZ_TEXT_KEY = "ForkyzTextKey";
    private static final String FORKYZ_IMAGE_KEY = "ForkyzImageKey";
    private static final EnumMap<KeyboardLayout, Integer> KEYBOARD_LAYOUTS;
    static {
        KEYBOARD_LAYOUTS = new EnumMap<>(KeyboardLayout.class);
        KEYBOARD_LAYOUTS.put(
            KeyboardLayout.KL_QWERTY, R.layout.forkyz_keyboard_qwerty
        );
        KEYBOARD_LAYOUTS.put(
            KeyboardLayout.KL_QWERTZ, R.layout.forkyz_keyboard_qwertz
        );
        KEYBOARD_LAYOUTS.put(
            KeyboardLayout.KL_DVORAK, R.layout.forkyz_keyboard_dvorak
        );
        KEYBOARD_LAYOUTS.put(
            KeyboardLayout.KL_COLEMAK, R.layout.forkyz_keyboard_colemak
        );
    };

    public enum SpecialKey {
        KEY_CHANGE_CLUE_DIRECTION(0),
        KEY_NEXT_CLUE(1),
        KEY_PREVIOUS_CLUE(2);

        private int keyCode;

        private SpecialKey(int keyCode) {
            this.keyCode = keyCode;
        }

        public static SpecialKey valueOf(int value) {
            for (SpecialKey key : SpecialKey.values()) {
                if (key.keyCode == value)
                    return key;
            }
            throw new IllegalArgumentException(
                "Unknown special key code " + value
            );
        }
    }

    public interface SpecialKeyListener {
        public void onKeyUp(SpecialKey key);
        public void onKeyDown(SpecialKey key);
    }

    /**
     * Encodes a particular key's action on up/down
     *
     * Different implementations for normal keys and special keys
     */
    private interface KeyActor {
        public void fireKeyUp();
        public void fireKeyDown();
    }

    @Inject
    protected ForkyzSettings settings;

    private Handler handler = new Handler(Looper.getMainLooper());

    private SparseArray<KeyActor> keyActors = new SparseArray<>();
    private SparseArray<Timer> keyTimers = new SparseArray<>();
    private InputConnection inputConnection;
    private int countKeysDown = 0;
    private List<Integer> specialKeyViewIds = new ArrayList<>();
    private boolean showSpecialKeys = false;
    private SpecialKeyListener specialKeyListener = null;
    private boolean showHideButton = true;
    private Runnable onHideButtonPressed = null;

    public ForkyzKeyboard(Context context) {
        this(context, null, 0);
    }

    public ForkyzKeyboard(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ForkyzKeyboard(
        Context context, AttributeSet attrs, int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        setFocusable(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // need to do this here: there's no life cycle owner until attached
        settings.getLiveKeyboardSettings().observe(
            ViewTreeLifecycleOwner.get(this),
            ks -> { createView(getContext(), ks); }
        );
    }

    @Override
    public synchronized void onVisibilityChanged(View view, int visibility) {
        super.onVisibilityChanged(view, visibility);
        if (visibility != View.VISIBLE) {
            cancelKeyTimers();
        }
    }

    @Override
    public synchronized boolean onTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
            onKeyDown(view.getId());
            view.setPressed(true);
            settings.getKeyboardSettings(ks -> {
                if (ks.getHaptic()) {
                    view.performHapticFeedback(
                        HapticFeedbackConstants.KEYBOARD_TAP
                    );
                }
            });
            return true;
        case MotionEvent.ACTION_UP:
            view.setPressed(false);
            onKeyUp(view.getId());
            return true;
        case MotionEvent.ACTION_CANCEL:
            view.setPressed(false);
            onKeyCancel(view.getId());
            return true;
        case MotionEvent.ACTION_MOVE:
        case MotionEvent.ACTION_POINTER_UP:
        case MotionEvent.ACTION_POINTER_DOWN:
            // ignore these mid-gesture movements, but consume them as
            // they are part of the gesture we're tracking
            return true;
        }
        return false;
    }

    @Override
    public synchronized void onClick(View view) {
        int id = view.getId();
        onKeyDown(id);
        onKeyUp(id);
    }

    /**
     * Attach the keyboard to send events to the view
     */
    public synchronized void setInputConnection(
        InputConnection inputConnection
    ) {
        this.inputConnection = inputConnection;
    }

    public EditorInfo getEditorInfo() {
        return new EditorInfo();
    }

    /**
     * Check if at least one key is currently pressed
     */
    public synchronized boolean hasKeysDown() { return countKeysDown > 0; }

    public void setShowSpecialKeys(boolean showSpecialKeys) {
        this.showSpecialKeys = showSpecialKeys;
    }

    /**
     * Set a listener for and display special keys
     */
    public synchronized void setSpecialKeyListener(
        SpecialKeyListener specialKeyListener
    ) {
        this.specialKeyListener = specialKeyListener;
    }

    public synchronized void setShowHideButton(boolean show) {
        setShowHideButton(show, onHideButtonPressed);
    }

    /**
     * Whether to show or hide the keyboard hide button row
     */
    public synchronized void setShowHideButton(
        boolean show, Runnable onHideButtonPressed
    ) {
        showHideButton = show;
        this.onHideButtonPressed = onHideButtonPressed;
        View hideRow = findViewById(R.id.hide_row);
        if (hideRow != null)
            hideRow.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Make special keys visible, reactive if listener available
     */
    private synchronized void setupSpecialKeys() {
        if (showSpecialKeys) {
            setSpecialKeyVisibilty(View.VISIBLE);
            for (int viewId : specialKeyViewIds) {
                View keyView = findViewById(viewId);
                keyView.setOnTouchListener(this);
                keyView.setOnClickListener(this);
            }
        } else {
            setSpecialKeyVisibilty(View.INVISIBLE);
            for (int viewId : specialKeyViewIds) {
                View keyView = findViewById(viewId);
                keyView.setOnTouchListener(null);
                keyView.setOnClickListener(null);
            }
        }
    }

    private synchronized void countKeyDown() { countKeysDown++; }
    private synchronized void countKeyUp() { countKeysDown--; }

    private synchronized void createView(
        Context context, KeyboardSettings keyboardSettings
    ) {
        // clear any old state (if config change)
        cancelKeyTimers();
        removeAllViews();
        keyActors.clear();
        keyTimers.clear();
        specialKeyViewIds.clear();

        // inflate new state
        LayoutInflater inflater = (LayoutInflater)
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater = inflater.cloneInContext(context);
        LayoutInflaterCompat.setFactory2(
            inflater,
            new FKFactory(keyboardSettings)
        );

        inflater.inflate(
            KEYBOARD_LAYOUTS.get(keyboardSettings.getLayout()),
            this,
            true
        );

        // initially hide unless special key listener is set
        setupSpecialKeys();

        Button hideButton = (Button) findViewById(R.id.key_hide);
        hideButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (onHideButtonPressed != null)
                    onHideButtonPressed.run();
            }
        });

        setShowHideButton(showHideButton);
    }

    private synchronized void setSpecialKeyVisibilty(int visibility) {
        for (int viewId : specialKeyViewIds) {
            findViewById(viewId).setVisibility(visibility);
        }
    }

    private synchronized void addKeyCode(int keyId, int keyCode) {
        keyActors.put(keyId, new KeyActor() {
            @Override
            public synchronized void fireKeyUp() {
                if (inputConnection != null)
                    fireKey(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
            }

            @Override
            public synchronized void fireKeyDown() {
                if (inputConnection != null)
                     fireKey(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            }

            private void fireKey(KeyEvent event) {
                int flags = KeyEvent.FLAG_KEEP_TOUCH_MODE
                    | KeyEvent.FLAG_SOFT_KEYBOARD;
                KeyEvent flagged = KeyEvent.changeFlags(event, flags);
                inputConnection.sendKeyEvent(flagged);
            }
        });
    }

    private void addSpecialKeyCode(int keyId, SpecialKey keyCode) {
        specialKeyViewIds.add(keyId);
        keyActors.put(keyId, new KeyActor() {
            @Override
            public synchronized void fireKeyUp() {
                if (specialKeyListener != null) {
                    specialKeyListener.onKeyUp(keyCode);
                }
            }

            @Override
            public synchronized void fireKeyDown() {
                if (specialKeyListener != null) {
                    specialKeyListener.onKeyDown(keyCode);
                }
            }
        });
    }

    private synchronized void onKeyUp(int keyId) {
        countKeyUp();
        sendKeyUp(keyId);
        cancelKeyTimer(keyId);
    }

    private synchronized void onKeyCancel(int keyId) {
        countKeyUp();
        cancelKeyTimer(keyId);
    }

    private synchronized void onKeyDown(final int keyId) {
        countKeyDown();
        sendKeyDown(keyId);

        settings.getKeyboardSettings(ks -> {
            int delay = ks.getRepeatDelay();
            int interval = ks.getRepeatInterval();

            if (delay == 0 || interval == 0)
                return;

            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    // post key events on main thread
                    handler.post(() -> {
                        sendKeyUp(keyId);
                        sendKeyDown(keyId);
                    });
                }
            }, delay, interval);
            setKeyTimer(keyId, timer);
        });
    }

    private synchronized void setKeyTimer(int keyId, Timer timer) {
        cancelKeyTimer(keyId);
        keyTimers.put(keyId, timer);
    }

    private synchronized void cancelKeyTimer(int keyId) {
        if (keyTimers == null)
            return;

        Timer timer = keyTimers.get(keyId);
        if (timer != null) {
            timer.cancel();
            // no point keeping references to expired timers
            keyTimers.put(keyId, null);
        }
    }

    private synchronized void cancelKeyTimers() {
        if (keyTimers == null)
            return;

        for (int i = 0; i < keyTimers.size(); i++) {
            Timer timer = keyTimers.valueAt(i);
            if (timer != null)
                timer.cancel();
        }
    }

    /**
     * Send a key up event for the button with the view id keyId
     *
     * Only if an actor was initialised
     */
    private synchronized void sendKeyUp(int keyId) {
        KeyActor actor = keyActors.get(keyId);
        if (actor != null)
            actor.fireKeyUp();
    }

    /**
     * Send a key down event for the button with the view id keyId
     *
     * Only if an actor was initialised
     */
    private synchronized void sendKeyDown(int keyId) {
        KeyActor actor = keyActors.get(keyId);
        if (actor != null)
            actor.fireKeyDown();
    }

    private class FKFactory implements LayoutInflater.Factory2 {
        private KeyboardSettings keyboardSettings;

        public FKFactory(KeyboardSettings keyboardSettings) {
            this.keyboardSettings = keyboardSettings;
        }

        @Override
        public View onCreateView(
            View parent, String tag, Context context, AttributeSet attrs
        ) {
            if (FORKYZ_TEXT_KEY.equals(tag)) {
                TextView view = new MaterialButton(context, attrs);
                setButtonPadding(view, context, attrs);
                setupButton(view, context, attrs);
                return view;
            } else if (FORKYZ_IMAGE_KEY.equals(tag)) {
                View view = new MaterialButton(context, attrs);
                setButtonPadding(view, context, attrs);
                setupButton(view, context, attrs);
                return view;
            } else {
                return null;
            }
        }

        public View onCreateView(
            String tag, Context context, AttributeSet attrs
        ) {
            return onCreateView(null, tag, context, attrs);
        }

        private void setupButton(
            View view, Context context, AttributeSet attrs
        ) {
            view.setFocusable(false);
            TypedArray ta = context.obtainStyledAttributes(
                attrs, R.styleable.ForkyzKey, 0, 0
            );
            try {
                int keyCode = ta.getInt(R.styleable.ForkyzKey_keyCode, -1);
                int specialKeyCode = ta.getInt(
                    R.styleable.ForkyzKey_specialKeyCode, -1
                );

                if (keyCode > -1) {
                    ForkyzKeyboard.this.addKeyCode(view.getId(), keyCode);
                }

                if (specialKeyCode > -1) {
                    ForkyzKeyboard.this.addSpecialKeyCode(
                        view.getId(), SpecialKey.valueOf(specialKeyCode)
                    );
                }

                if (keyCode > -1 || specialKeyCode > -1) {
                    view.setOnTouchListener(ForkyzKeyboard.this);
                    view.setOnClickListener(ForkyzKeyboard.this);
                }
            } finally {
                ta.recycle();
            }
        }

        private void setButtonPadding(
            View view, Context context, AttributeSet attrs
        ) {
            int paddingTop = view.getPaddingTop();
            int paddingBottom = view.getPaddingBottom();
            int paddingLeft = view.getPaddingLeft();
            int paddingRight = view.getPaddingRight();

            if (!hasAttribute(android.R.attr.padding, context, attrs) &&
                !hasAttribute(android.R.attr.paddingTop, context, attrs) &&
                !hasAttribute(android.R.attr.paddingBottom, context, attrs)) {

                int btnPaddingPcnt
                    = context
                        .getResources()
                        .getInteger(
                            keyboardSettings.getCompact()
                                ? R.integer.keyboardButtonPaddingPcntCompact
                                : R.integer.keyboardButtonPaddingPcnt
                        );
                int dispHght
                    = context.getResources().getDisplayMetrics().heightPixels;
                int paddingTopBot = (int) ((btnPaddingPcnt / 100.0) * dispHght);

                paddingTop = paddingTopBot;
                paddingBottom = paddingTopBot;
            }

            view.setPadding(
                paddingLeft, paddingTop, paddingRight, paddingBottom
            );
        }

        private boolean hasAttribute(
            int id, Context context, AttributeSet attrs
        ) {
            boolean hasAttribute = false;
            TypedArray ta = context.obtainStyledAttributes(
                attrs, new int[] { id }
            );
            try {
                hasAttribute = ta.getString(0) != null;
            } finally {
                ta.recycle();
            }
            return hasAttribute;
        }

        private int dpToPx(Context context, int dp) {
            final float scale
                = context.getResources().getDisplayMetrics().density;
            return (int) (dp * scale + 0.5f);
        }
    }
}
