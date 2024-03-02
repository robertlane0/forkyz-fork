package app.crossword.yourealwaysbe.forkyz.view;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import javax.inject.Inject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.util.BoxInputConnection;
import app.crossword.yourealwaysbe.forkyz.util.KeyboardManager;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Position;

@AndroidEntryPoint
public class BoardEditText
        extends ScrollingImageView
        implements BoxInputConnection.BoxInputListener,
            KeyboardManager.ManageableView {

    private static final Logger LOG
        = Logger.getLogger(BoardEditText.class.getCanonicalName());

    @Inject
    protected ForkyzSettings settings;

    @Inject
    protected AndroidVersionUtils utils;

    // for rendering
    private final ExecutorService executorService
        = Executors.newSingleThreadExecutor();
    private final Handler handler;

    private BoxInputConnection currentInputConnection = null;
    private boolean nativeInput = false;

    public interface BoardEditFilter {
        /**
         * @param oldChar the character being deleted
         * @param pos the position of the box being deleted from
         * @return true if the deletion is allowed to occur
         */
        public boolean delete(char oldChar, int pos);

        /**
         * @param oldChar the character that used to be in the box
         * @param newChar the character to replace it with
         * @param pos the position of the box
         * @return the actual character to replace the old one with or null char
         * if the replacement is not allowed
         */
        public char filter(char oldChar, char newChar, int pos);

        /**
         * @param pos the position the new char was put
         * @param oldChar the char that was overwritten
         */
        default void postChange(int pos, char oldChar, boolean isDelete) { }
    }

    public interface SelectionListener {
        /**
         * Notify when selected pos change, -1 when lose focus
         */
        void onSelect(int pos);
    }

    private Position selection = new Position(0, -1);
    private Box[] boxes;
    private boolean[] changes;
    private PlayboardRenderer renderer;
    private String shadow;
    private SelectionListener selectionListener = null;

    private BoardEditFilter[] filters;

    public BoardEditText(Context context, AttributeSet as) {
        super(context, as);

        handler = new Handler(context.getMainLooper());

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        // We are not giving the renderer a board, so be careful some
        // method calls may NPE
        this.renderer = new PlayboardRenderer(
            null,
            metrics.densityDpi,
            metrics.widthPixels,
            context,
            utils
        );
        this.renderer.setMaxScale(1.0F);
        this.renderer.setMinScale(0.6F);

        setAllowOverScroll(false);
        setAllowZoom(false);
    }

    /**
     * Get notified when selected cell changes
     */
    public void setSelectionListener(SelectionListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    @Override
    protected void onSizeChanged(
        int newWidth, int newHeight, int oldWidth, int oldHeight
    ) {
        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
        // do after layout pass (has no effect during pass)
        getViewTreeObserver().addOnPreDrawListener(
            new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    getViewTreeObserver().removeOnPreDrawListener(this);
                    scrollTo(0, 0);
                    int width = getContentWidth();
                    int len = boxes == null ? 0 : boxes.length;
                    renderer.fitWidthTo(width, len);
                    render(true);
                    return true;
                }
            }
        );
    }

    @Override
    protected void onTap(Point point) {
        BoardEditText.this.requestFocus();

        int box = findPosition(point);
        if (box >= 0) {
            setSelectedCol(box);
            BoardEditText.this.render();
            updateInputConnection();
        }

        super.onTap(point);
    }

    @Override
    public void onNewResponse(String response) {
        if (response == null || response.isEmpty())
            return;
        onNewResponse(response.charAt(0));
    }

    @Override
    public void onDeleteResponse() {
        if (boxes != null) {
            int col = getSelectedCol();
            char oldChar = getResponse(col);

            if (boxes[col].isBlank() && col > 0) {
                flagChanged(col);
                setSelectedCol(col - 1);
            }
            if (canDelete(selection)) {
                col = getSelectedCol();
                boxes[col].setBlank();
                flagChanged(col);
            }

            notifyPostChange(col, oldChar, true);

            this.render();
            updateInputConnection();
        }
    }

    @Override
    public void onFocusChanged(
        boolean gainFocus, int direction, Rect previouslyFocusedRect
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (!gainFocus) {
            setSelectedCol(-1);
            render();
        } else if (boxes != null) {
            if (getSelectedCol() < 0
                    || getSelectedCol() >= boxes.length) {
                setSelectedCol(0);
                updateInputConnection();
                render();
            }
        }
    }

    public void setFilters(BoardEditFilter[] filters) {
        this.filters = filters;
    }

    public void setLength(int len) {
        if (boxes == null || len != boxes.length) {
            Box[] newBoxes = new Box[len];
            newChanges(len);

            int overlap = 0;
            if (boxes != null) {
                overlap = Math.min(len, boxes.length);
                for (int i = 0; i < overlap; i++) {
                    newBoxes[i] = boxes[i];
                }
            }

            for (int i = overlap; i < len; ++i) {
                newBoxes[i] = new Box();
                flagChanged(i);
            }

            boxes = newBoxes;

            render();
        }
    }

    public int getLength() {
        return (boxes == null) ? 0 : boxes.length;
    }

    public Box[] getBoxes() {
        return boxes;
    }

    public boolean isBlank(int pos) {
        if (boxes != null && 0 <= pos && pos < boxes.length) {
            return boxes[pos].isBlank();
        } else {
            return true;
        }
    }

    public char getResponse(int pos) {
        if (boxes != null && 0 <= pos && pos < boxes.length) {
            String response = boxes[pos].getResponse();
            if (response == null || response.isEmpty())
                return '\0';
            else
                return response.charAt(0);
        } else {
            return '\0';
        }
    }

    public void setResponse(int pos, char c) {
        setResponseNoInputConnectionUpdate(pos, c);
        updateInputConnection();
    }

    public void setResponseNoInputConnectionUpdate(int pos, char c) {
        if (boxes != null && 0 <= pos && pos < boxes.length) {
            boxes[pos].setResponse(c);
            flagChanged(pos);
            render();
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
        }
    }

    public void setResponse(int pos, String c) {
        if (c != null && !c.isEmpty())
            setResponse(pos, c.charAt(0));
    }

    /**
     * Fill the existing boxes from the string
     *
     * Overwrite from start to end or length of text
     */
    public void setFromString(String text) {
        if (text == null)
            return;

        int end = Math.min(text.length(), boxes.length);

        for (int i = 0; i < end; i++) {
            boxes[i].setResponse(text.charAt(i));
            flagChanged(i);
        }

        render();
        updateInputConnection();
    }

    /**
     * Initialise the boxes and length from the given string
     *
     * Creates new boxes of given length
     */
    public void initialiseFromString(String text) {
        if (text == null) {
            boxes = null;
        } else {
            boxes = new Box[text.length()];
            newChanges(text.length());
            for (int i = 0; i < text.length(); i++) {
                boxes[i] = new Box();
                boxes[i].setResponse(text.charAt(i));
                flagChanged(i);
            }
        }
        render();
        updateInputConnection();
    }

    public void clear() {
        if (boxes != null) {
            for (Box box : boxes)
                box.setBlank();
            render(true);
        }
    }

    /**
     * Set a string to display in the boxes as a note
     *
     * Can be used e.g. for predictive text. Will be displayed as
     * smaller chars
     */
    public void setShadow(String shadow) {
        int oldLen = (this.shadow == null) ? 0 : this.shadow.length();
        int newLen = (shadow == null) ? 0 : shadow.length();
        int len = Math.max(oldLen, newLen);
        len = Math.min(len, boxes.length);

        // only flag changes for blank cells that would show new shadow
        for (int i = 0; i < len; i++) {
            if (isBlank(i))
                flagChanged(i);
        }

        this.shadow = shadow;

        render();
    }

    public void clearShadow() {
        setShadow(null);
    }

    public String toString() {
        return toString(false);
    }

    /**
     * Convert to string
     *
     * @param skipBlanks don't include blank boxes
     */
    public String toString(boolean skipBlanks) {
        if (boxes == null)
            return "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < boxes.length; i++) {
            if (!skipBlanks || !boxes[i].isBlank())
                sb.append(boxes[i].getResponse());
        }

        return sb.toString();
    }

    @Override
    public boolean setNativeInput(boolean nativeInput) {
        boolean changed = this.nativeInput != nativeInput;
        this.nativeInput = nativeInput;
        return changed;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return nativeInput;
    }

    // Set input type to be raw keys if a keyboard is used
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        if (onCheckIsTextEditor()) {
            currentInputConnection = new BoxInputConnection(
                this,
                utils,
                getSelectedResponse(),
                this
            );
            currentInputConnection.setOutAttrs(outAttrs);
            return currentInputConnection;
        } else {
            return null;
        }
    }

    @Override
    public InputConnection onCreateForkyzInputConnection(EditorInfo outAttrs) {
        BaseInputConnection fic = new BaseInputConnection(this, false);
        outAttrs.inputType = InputType.TYPE_NULL;
        return fic;
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
        boolean handled = isHandledKey(keyCode, event);
        if (!handled)
            return super.onKeyUp(keyCode, event);

        int cancelled = event.getFlags()
            & (KeyEvent.FLAG_CANCELED | KeyEvent.FLAG_CANCELED_LONG_PRESS);
        if (cancelled > 0)
            return true;

        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_LEFT: {
                int col = getSelectedCol();
                if (col > 0) {
                    setSelectedCol(col - 1);
                    this.render();
                }
                return true;
            }

        case KeyEvent.KEYCODE_DPAD_RIGHT: {
                int col = getSelectedCol();
                if (boxes != null && col < boxes.length - 1) {
                    setSelectedCol(col + 1);
                    this.render();
                }

                return true;
            }

        case KeyEvent.KEYCODE_DEL:
            onDeleteResponse();
            return true;

        // space handled as any char
        }

        char c = Character.toUpperCase(event.getDisplayLabel());

        if (boxes != null && isAcceptableCharacterResponse(c)) {
            onNewResponse(c);
            return true;
        }

        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        event.getText().add(toString());
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(BoardEditText.class.getName());
        if (event.getEventType() ==
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            int col = getSelectedCol();
            if (col >= 0) {
                event.setFromIndex(col);
                event.setToIndex(col + 1);
                event.setItemCount(1);
            }
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setText(toString());
    }

    public int getNumNonBlank() {
        int count = 0;
        int len = getLength();
        for (int i = 0; i < len; i++) {
            if (!isBlank(i))
                count += 1;
        }
        return count;
    }

    /**
     * If this character in the string is a blank box
     */
    public static boolean isBlank(char c) {
        return Box.BLANK.equals(String.valueOf(c));
    }

    public int getSelectedCol() {
        return selection.getCol();
    }

    public void setSelectedCol(int col) {
        flagChanged(selection.getCol(), col);
        selection.setCol(col);
        updateInputConnection();
        sendAccessibilityEvent(
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        );
        if (selectionListener != null)
            selectionListener.onSelect(col);
    }

    private void onNewResponse(char character) {
        if (!isAcceptableCharacterResponse(character))
            return;

        final char c = filterReplacement(character, selection);

        if (c != '\0') {
            settings.getPlaySkipFilled(skipCompleted -> {
                int enteredCol = getSelectedCol();
                char oldChar = getResponse(enteredCol);

                // we'll update later if the selection doesn't change
                setResponseNoInputConnectionUpdate(enteredCol, c);

                int col = enteredCol;
                if (col < boxes.length - 1) {
                    col += 1;
                    int nextPos = col;

                    while (skipCompleted &&
                           !boxes[col].isBlank() &&
                           col < boxes.length - 1) {
                        col += 1;
                    }

                    if (boxes[col].isBlank())
                        setSelectedCol(col);
                    else
                        setSelectedCol(nextPos);
                } else {
                    updateInputConnection();
                }

                notifyPostChange(enteredCol, oldChar, false);

                this.render();
            });
        } else {
            // Needs updating else thinks there's a character in the
            // buffer, when it was refused
            updateInputConnection();
        }
    }

    private void render() {
        render(false);
    }

    private void render(boolean renderAll) {
        if (getWidth() == 0)
            return;

        boolean[] renderChanges = renderAll
            ? null
            : Arrays.copyOf(changes, changes.length);

        executorService.execute(() -> {
            Bitmap bitmap = renderer.drawBoxes(
                boxes,
                shadow,
                renderChanges,
                selection,
                null,
                getContentWidth()
            );
            handler.post(() -> { setBitmap(bitmap); });
        });

        clearChanges();
    }

    private boolean canDelete(Position pos) {
        if (filters == null)
            return true;

        if (boxes == null || pos.getCol() < 0 || pos.getCol() >= boxes.length)
            return false;

        char oldChar = getResponse(pos.getCol());

        for (BoardEditFilter filter : filters) {
            if (filter != null && !filter.delete(oldChar, pos.getCol())) {
                return false;
            }
        }

        return true;
    }

    private char filterReplacement(char newChar, Position pos) {
        if (filters == null)
            return newChar;

        if (boxes == null || pos.getCol() < 0 || pos.getCol() >= boxes.length)
            return '\0';

        char oldChar = getResponse(pos.getCol());

        for (BoardEditFilter filter : filters) {
            if (filter != null) {
                newChar = filter.filter(oldChar, newChar, pos.getCol());
            }
        }

        return newChar;
    }

    private int findPosition(Point point) {
        if (boxes == null)
            return -1;

        Position position = renderer.findPosition(point);
        if (position == null)
            return -1;

        int boxesPerRow = renderer.getNumBoxesPerRow(getContentWidth());
        int box = boxesPerRow * position.getRow() + position.getCol();

        if (0 <= box && box < boxes.length)
            return box;
        else
            return -1;
    }

    private String getSelectedResponse() {
        Box box = getSelectedBox();
        return box == null ? Box.BLANK : box.getResponse();
    }

    private Box getSelectedBox() {
        int col = getSelectedCol();
        return (0 <= col && col < boxes.length) ? boxes[col] : null;
    }

    private void updateInputConnection() {
        if (currentInputConnection != null) {
            String response = getSelectedResponse();
            currentInputConnection.setResponse(response);
        }
    }

    private boolean isAcceptableCharacterResponse(char c) {
        return !Character.isISOControl(c) && !Character.isSurrogate(c);
    }

    /**
     * Width of the usable area inside the view (sans padding)
     */
    private int getContentWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private void newChanges(int len) {
        changes = new boolean[len];
    }

    private void clearChanges() {
        if (changes == null)
            return;
        Arrays.fill(changes, false);
    }

    private void flagChanged(int... positions) {
        if (changes == null)
            return;

        for (int pos : positions) {
            if (0 <= pos && pos < changes.length)
                changes[pos] = true;
        }
    }

    /**
     * Is a handled key event
     *
     * Should match onKeyUp/Down
     */
    private boolean isHandledKey(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
        case KeyEvent.KEYCODE_DEL:
        case KeyEvent.KEYCODE_SPACE:
            return true;

        default:
            char c = Character.toUpperCase(event.getDisplayLabel());
            if (boxes != null && isAcceptableCharacterResponse(c))
                return true;
        }

        return false;
    }

    private void notifyPostChange(int pos, char oldChar, boolean isDelete) {
        if (filters == null)
            return;

        for (BoardEditFilter filter : filters) {
            if (filter != null)
                filter.postChange(pos, oldChar, isDelete);
        }
    }
}
