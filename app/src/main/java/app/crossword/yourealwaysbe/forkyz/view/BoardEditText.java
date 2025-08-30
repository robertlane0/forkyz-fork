package app.crossword.yourealwaysbe.forkyz.view;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import javax.inject.Inject;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import androidx.annotation.MainThread;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.settings.DisplaySeparators;
import app.crossword.yourealwaysbe.forkyz.settings.RenderSettings;
import app.crossword.yourealwaysbe.forkyz.util.BoxInputConnection;
import app.crossword.yourealwaysbe.forkyz.util.KeyboardManageableView;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.forkyz.view.PlayboardRenderer.BoxSeparators;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Position;

@AndroidEntryPoint
public class BoardEditText
        extends ScrollingImageView
        implements BoxInputConnection.BoxInputListener, KeyboardManageableView {

    @Inject
    protected AndroidVersionUtils utils;

    // for rendering
    private final ExecutorService executorService
        = Executors.newSingleThreadExecutor();
    private final Handler handler;
    // so we don't render while we're posting a bitmap
    private final Semaphore renderLock = new Semaphore(1);
    private BoxInputConnection currentInputConnection = null;
    private boolean nativeInput = false;
    private boolean forceCaps = true;
    private boolean skipFilled = false;
    private RenderSettings renderSettings = new RenderSettings();

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

    private Position selection = new Position(0, -1);
    private Box[] boxes;
    private BoxSeparators[] separators;
    private boolean[] changes;
    private PlayboardRenderer renderer;
    private String shadow;

    private BoardEditFilter filter;

    public BoardEditText(Context context) {
        super(context, null);

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

    @Override
    protected void onSizeChanged(
        int newWidth, int newHeight, int oldWidth, int oldHeight
    ) {
        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
        scrollTo(0, 0);
        int width = getContentWidth();
        int len = boxes == null ? 0 : boxes.length;
        renderer.fitWidthTo(width, len);
        render(true);
    }

    @Override
    protected void onTap(Point point) {
        BoardEditText.this.requestFocus();

        int box = findPosition(point);
        if (box >= 0) {
            if (setSelectedColNoRender(box)) {
                BoardEditText.this.render();
                updateInputConnection();
            }
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
            if (boxes[col].isBlank() && col > 0) {
                flagChanged(col);
                setSelectedColNoRender(col - 1);
            }

            col = getSelectedCol();
            char oldChar = getResponse(col);

            if (canDelete(selection)) {
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
            setSelectedColNoRender(-1);
            render();
        } else if (boxes != null) {
            if (getSelectedCol() < 0
                    || getSelectedCol() >= boxes.length) {
                setSelectedColNoRender(0);
                updateInputConnection();
                render();
            }
        }
    }

    public void setRenderSettings(RenderSettings renderSettings) {
        this.renderSettings = renderSettings;
        render();
    }

    public void setSkipFilled(boolean skipFilled) {
        this.skipFilled = skipFilled;
    }

    public void setFilter(BoardEditFilter filter) {
        this.filter = filter;
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

    /**
     * Sets separators for existing boxes using clue as a basis
     */
    public void setSeparatorsFromClue(Clue clue, boolean inferSeparators) {
        if (boxes == null || clue == null)
            return;

        BoxSeparators[] oldSeparators = separators;
        separators = new BoxSeparators[boxes.length];

        for (int i = 0; i < separators.length; i++) {
            separators[i] = renderer.getBoxSeparators(clue, inferSeparators, i);
        }

        if (
            boxes != null
            && boxes.length > 0
            && !Objects.equals(separators, oldSeparators)
        )
            render();
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
        setResponse(pos, c, true);
    }

    public void deleteResponse(int pos) {
        if (canDelete(pos)) {
            char oldChar = getResponse(pos);
            boxes[pos].setBlank();
            flagChanged(pos);
            notifyPostChange(pos, oldChar, true);
            this.render();

            if (pos == getSelectedCol())
                updateInputConnection();
        }
    }

    /**
     * Set response at pos and choose if render
     *
     * If not rendering, render() needs to be called separately when
     * done. Used to avoid multiple renders for a sequence of updates.
     */
    public void setResponse(int pos, char c, boolean render) {
        setResponseNoInputConnectionUpdate(pos, c, render);
        updateInputConnection();
    }

    /**
     * Do with render
     */
    public void setResponseNoInputConnectionUpdate(int pos, char c) {
        setResponseNoInputConnectionUpdate(pos, c, true);
    }

    /**
     * Can choose not to render if you'll do it later anyway
     */
    public void setResponseNoInputConnectionUpdate(
        int pos, char c, boolean render
    ) {
        if (boxes != null && 0 <= pos && pos < boxes.length) {
            boxes[pos].setResponse(c);
            flagChanged(pos);
            if (render)
                render();
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
        }
    }

    /**
     * See setResponse(int, char, boolean)
     */
    public void setResponse(int pos, String c, boolean render) {
        if (c != null && !c.isEmpty())
            setResponse(pos, c.charAt(0), render);
    }

    /**
     * Fill the existing boxes from the string
     *
     * Does not filter entries.
     */
    public void setFromString(String text) {
        if (text == null)
            return;

        boolean changed = false;

        for (int i = 0; i < boxes.length; i++) {
            if (i < text.length()) {
                char c = text.charAt(i);
                if (!charStringEquals(boxes[i].getResponse(), c)) {
                    boxes[i].setResponse(c);
                    flagChanged(i);
                    changed = true;
                }
            } else {
                if (!boxes[i].isBlank()) {
                    boxes[i].setBlank();
                    flagChanged(i);
                    changed = true;
                }
            }
        }

        if (changed) {
            render();
            updateInputConnection();
        }
    }

    /**
     * Fill the existing boxes from the boxes responses
     *
     * Overwrite from start to end or length of text. Runs each entry through
     * the filter.
     */
    public void enterFromBoxes(Box[] source, boolean copyBlanks) {
        if (source == null)
            return;

        int end = Math.min(source.length, boxes.length);

        for (int i = 0; i < end; i++) {
            if (copyBlanks || !source[i].isBlank()) {
                char c = filterReplacement(
                    source[i].getResponse().charAt(0), new Position(0, i)
                );
                if (c != '\0') {
                    boxes[i].setResponse(c);
                    flagChanged(i);
                }
            }
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
     * smaller chars. Does nothing if same shadow passed. Null means
     * clear shadow.
     */
    public void setShadow(String shadow) {
        if (Objects.equals(this.shadow, shadow))
            return;

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

    public String toString() {
        if (boxes == null)
            return "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < boxes.length; i++) {
            sb.append(boxes[i].getResponse());
        }

        return sb.toString();
    }

    @Override
    public boolean setNativeInput(boolean nativeInput, boolean forceCaps) {
        boolean changed = (
            this.nativeInput != nativeInput
            || this.forceCaps != forceCaps
        );
        this.nativeInput = nativeInput;
        this.forceCaps = forceCaps;
        return changed;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return isNativeInput();
    }

    // Set input type to be raw keys if a keyboard is used
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        if (onCheckIsTextEditor()) {
            currentInputConnection = new BoxInputConnection(
                this,
                utils,
                getSelectedResponse(),
                this,
                isForceCaps()
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
                    setSelectedColNoRender(col - 1);
                    this.render();
                }
                return true;
            }

        case KeyEvent.KEYCODE_DPAD_RIGHT: {
                int col = getSelectedCol();
                if (boxes != null && col < boxes.length - 1) {
                    setSelectedColNoRender(col + 1);
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

    /**
     * Set selected column, render if needed
     *
     * @return true if changed
     */
    public boolean setSelectedCol(int col) {
        boolean changed = setSelectedColNoRender(col);
        if (changed)
            render();
        return changed;
    }

    /**
     * Returns true if changed, does not rerender
     */
    private boolean setSelectedColNoRender(int col) {
        if (col == selection.getCol())
            return false;
        // allow < 0 (means no selection)
        if (boxes.length <= col)
            return false;

        flagChanged(selection.getCol(), col);
        selection.setCol(col);
        updateInputConnection();
        sendAccessibilityEvent(
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        );
        return true;
    }

    @MainThread
    private void onNewResponse(char character) {
        if (!isAcceptableCharacterResponse(character))
            return;

        final char c = filterReplacement(character, selection);

        if (c != '\0') {
            int enteredCol = getSelectedCol();
            char oldChar = getResponse(enteredCol);

            // we'll update later if the selection doesn't change
            setResponseNoInputConnectionUpdate(enteredCol, c, false);

            int col = enteredCol;
            if (col < boxes.length - 1) {
                col += 1;
                int nextPos = col;

                while (skipFilled &&
                       !boxes[col].isBlank() &&
                       col < boxes.length - 1) {
                    col += 1;
                }

                if (boxes[col].isBlank())
                    setSelectedColNoRender(col);
                else
                    setSelectedColNoRender(nextPos);
            } else {
                updateInputConnection();
            }

            notifyPostChange(enteredCol, oldChar, false);

            this.render();
        } else {
            // Needs updating else thinks there's a character in the
            // buffer, when it was refused
            updateInputConnection();
        }
    }

    @MainThread
    public void render() {
        render(false);
    }

    @MainThread
    private void render(boolean renderAll) {
        if (getWidth() == 0 || renderSettings == null)
            return;

        boolean[] renderChanges = renderAll
            ? null
            : Arrays.copyOf(changes, changes.length);

        BoxSeparators[] displaySeps
            = renderSettings.getDisplaySeparators()
                == DisplaySeparators.DS_NEVER
            ? null
            : separators;

        executorService.execute(() -> {
            try {
                lockRenderer();
                BitmapGrid bitmap = renderer.drawBoxes(
                    boxes,
                    shadow,
                    renderChanges,
                    selection,
                    null,
                    getContentWidth(),
                    displaySeps
                );
                handler.post(() -> {
                    try { setBitmap(bitmap); }
                    finally { unlockRenderer(); }
                });
            } catch (Exception e) {
                unlockRenderer();
                throw e;
            }
        });

        clearChanges();
    }

    private boolean canDelete(Position pos) {
        return canDelete(pos.getCol());
    }

    private boolean canDelete(int col) {
        if (filter == null)
            return true;

        if (boxes == null || col < 0 || col >= boxes.length)
            return false;

        char oldChar = getResponse(col);

        if (!filter.delete(oldChar, col)) {
            return false;
        }

        return true;
    }

    private char filterReplacement(char newChar, Position pos) {
        if (filter == null)
            return newChar;

        if (boxes == null || pos.getCol() < 0 || pos.getCol() >= boxes.length)
            return '\0';

        char oldChar = getResponse(pos.getCol());

        return filter.filter(oldChar, newChar, pos.getCol());
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
        if (filter != null)
            filter.postChange(pos, oldChar, isDelete);
    }

    private boolean isNativeInput() { return nativeInput; }

    private boolean isForceCaps() { return forceCaps; }

    /**
     * Use these to ensure only one thread working on render bitmap
     */
    private void lockRenderer() {
        try {
            renderLock.acquire();
        } catch (InterruptedException e) {
            // um...
        }
    }

    private void unlockRenderer() { renderLock.release(); }

    private boolean charStringEquals(String s, char c) {
        return s != null && s.length() == 1 && s.charAt(0) == c;
    }
}
