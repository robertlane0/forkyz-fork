
package app.crossword.yourealwaysbe.forkyz.view;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import javax.inject.Inject;

import android.content.Context;
import android.os.Handler;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.settings.RenderSettings;
import app.crossword.yourealwaysbe.forkyz.theme.BoardColorScheme;
import app.crossword.yourealwaysbe.forkyz.util.BoxInputConnection;
import app.crossword.yourealwaysbe.forkyz.util.KeyboardManageableView;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.forkyz.view.PlayboardRenderer.RenderChanges;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;

/**
 * A live view of the playboard
 *
 * BoardEditText is an edit text that looks like the board.
 *
 * Renders the playboard on change and implements input connection with
 * soft-input.
 */
@AndroidEntryPoint
public abstract class BoardEditView
        extends ScrollingImageView
        implements Playboard.PlayboardListener,
                    BoxInputConnection.BoxInputListener,
                    KeyboardManageableView {
    private static final int DOUBLE_CLICK_INTERVAL = 300; // ms

    @Inject
    protected AndroidVersionUtils utils;

    private Playboard board;
    private boolean wordView;
    private PlayboardRenderer renderer;
    private Set<BoardClickListener> clickListeners = new HashSet<>();
    private long lastTap = 0;
    private BoxInputConnection currentInputConnection = null;
    private boolean nativeInput = false;
    private boolean forceCaps = true;
    private boolean scratchMode = false;
    private boolean doubleTapFit = false;
    private boolean requestFocusOnSized = false;
    private RenderSettings renderSettings = new RenderSettings();
    private BoardColorScheme boardColorScheme = null;

    // For use by implementers of render to avoid on-main-thread
    // rendering. Bitmap can be set with postBitmap in
    // ScrollingImageView
    protected final ExecutorService executorService
        = Executors.newSingleThreadExecutor();
    protected final Handler handler;
    // so we don't render while we're posting a bitmap
    private final Semaphore renderLock = new Semaphore(1);

    public interface BoardClickListener {
        default void onClick(Position position, Word previousWord) { }
        default void onLongClick(Position position) { }
    }

    public BoardEditView(Context context, AttributeSet attrs) {
        this(context, null, null);
    }

    public BoardEditView(
        Context context,
        AttributeSet attrs,
        ScrollingImageViewState state
    ) {
        super(context, attrs, state);
        handler = new Handler(context.getMainLooper());
    }

    public void setBoardColorScheme(BoardColorScheme boardColorScheme) {
        if (!Objects.equals(boardColorScheme, this.boardColorScheme)) {
            this.boardColorScheme = boardColorScheme;
            recreateRenderer(true);
        }
    }

    public void setRenderSettings(RenderSettings renderSettings) {
        if (!Objects.equals(renderSettings, this.renderSettings)) {
            this.renderSettings = renderSettings;
            render();
        }
    }

    public void setDoubleTapFitBoard(boolean doubleTapFit) {
        this.doubleTapFit = doubleTapFit;
    }

    /**
     * Request focus or delay until has dimension
     *
     * Cannot receive focus until all dimensions > 0
     */
    public void requestFocusOnSized() {
        if (getWidth() > 0 && getHeight() > 0)
            requestFocus();
        else
            requestFocusOnSized = true;
    }

    public void setBoard(Playboard board) {
        setBoard(board, false);
    }

    public void setScratchMode(boolean scratchMode) {
        this.scratchMode = scratchMode;
    }

    /**
     * Set the base board for the edit view
     *
     * Use noRender if you want to make more changes before rendering
     * Resets min/max scale as this depends on the board for some reason.
     * Set board to null to "deactivate" this view (i.e. stop it
     * drawing bitmaps on board changes).
     */
    public void setBoard(Playboard board, boolean noRender) {
        if (this.board != null)
            this.board.removeListener(this);

        if (board == null) {
            this.board =  null;
            // free up renderer and bitmap
            this.renderer = null;
        } else {
            this.board = board;
            recreateRenderer(false);
            if (!noRender)
                render();

            // TODO: needed?
            //setFocusable(true);

            // don't worry about unlistening because Playboard keeps a
            // weak set.
            board.addListener(this);
        }
    }

    /**
     * Detach view from board (and stop listening/updating)
     */
    public void detach() {
        setBoard(null);
    }

    @Override
    public void setMaxScale(float maxScale) {
        super.setMaxScale(maxScale);
        if (renderer != null)
            renderer.setMaxScale(maxScale);
    }

    @Override
    public void setMinScale(float minScale) {
        super.setMinScale(minScale);
        if (renderer != null)
            renderer.setMinScale(minScale);
    }

    /**
     * Add listener for clicks on board positions
     *
     * Does not store in a weak set, so listener will survive as long as
     * view does
     */
    public void addBoardClickListener(BoardClickListener listener) {
        clickListeners.add(listener);
    }

    public void removeBoardClickListener(BoardClickListener listener) {
        clickListeners.remove(listener);
    }

    @Override
    public float setCurrentScale(float scale) {
        return setCurrentScale(scale, false);
    }

    public float setCurrentScale(float scale, boolean noRender) {
        float oldScale = getCurrentScale();

        if (renderer != null) {
            if (scale > renderer.getMaxScale()) {
                scale = renderer.getMaxScale();
            } else if (scale < renderer.getMinScale()) {
                scale = renderer.getMinScale();
            } else if (Float.isNaN(scale)) {
                scale = 1F;
            }

            renderer.setScale(scale);
        }

        super.setCurrentScale(scale);
        if (!noRender && Float.compare(scale, oldScale) != 0) {
            render();
        }

        return scale;
    }

    public abstract Position findPosition(Point point);

    public abstract void fitToView();

    /**
     * Returns new scale
     */
    public float zoomIn() {
        if (renderer != null) {
            float newScale = renderer.zoomIn();
            setCurrentScale(newScale);
        }
        return getCurrentScale();
    }

    /**
     * Returns new scale
     */
    public float zoomInMax() {
        if (renderer != null) {
            float newScale = renderer.zoomInMax();
            setCurrentScale(newScale);
        }
        return getCurrentScale();
    }

    /**
     * Returns new scale
     */
    public float zoomOut() {
        if (renderer != null) {
            float newScale = renderer.zoomOut();
            setCurrentScale(newScale);
        }
        return getCurrentScale();
    }

    /**
     * Returns new scale
     */
    public float zoomReset() {
        if (renderer != null) {
            float newScale = renderer.zoomReset();
            setCurrentScale(newScale);
        }
        return getCurrentScale();
    }

    @Override
    public void onPlayboardChange(PlayboardChanges changes) {
        if (currentInputConnection != null)
            currentInputConnection.setResponse(getCurrentResponse());
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
    public void onNewResponse(String response) {
        Playboard board = getBoard();
        if (board == null)
            return;

        if (scratchMode) {
            if (response != null)
                board.playScratchLetter(response.charAt(0));
        } else {
            board.playLetter(response);
        }
    }

    @Override
    public void onDeleteResponse() {
        Playboard board = getBoard();
        if (board == null)
            return;

        if (scratchMode) {
            board.deleteScratchLetter();
        } else {
            board.deleteOrUndoLetter();
        }
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
                getCurrentResponse(),
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
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (requestFocusOnSized && w > 0 && h > 0) {
            requestFocus();
            requestFocusOnSized = false;
        }
        // need to rerender in case new parts of grid visible
        render(PlayboardChanges.getEmptyChanges());
    }

    @Override
    protected void onScale(float scale, Point center) {
        lastTap = System.currentTimeMillis();
        if (renderer != null) {
            int w = getImageWidth();
            int h = getImageHeight();
            scale = renderer.fitTo(w, h);
            super.onScale(scale, center);
            render();
        }
    }

    @Override
    protected void onContextMenu(Point point) {
        super.onContextMenu(point);
        Position position = findPosition(point);
        if (position != null) {
            for (BoardClickListener listener : clickListeners) {
                listener.onLongClick(position);
            }
        }
    }

    @Override
    protected void onTap(Point point) {
        super.onTap(point);

        requestFocus();

        long clickInterval = System.currentTimeMillis() - lastTap;

        if (doubleTapFit && clickInterval < DOUBLE_CLICK_INTERVAL) {
            fitToView();
            notifyScaleChange(getCurrentScale());
        } else {
            Position position = findPosition(point);
            if (position != null)
                onClick(position);
        }

        lastTap = System.currentTimeMillis();
    }

    /**
     * Click on a board position
     *
     * Note, position may not be in a word on the board, or indeed in
     * the board itself. Will be checked (by concrete implementations)
     * to see if it is an actual cell click.
     */
    abstract protected void onClick(Position position);

    protected void notifyClick(Position position, Word previousWord) {
        for (BoardClickListener listener : clickListeners) {
            listener.onClick(position, previousWord);
        }
    }

    protected RenderSettings getRenderSettings() {
        return renderSettings;
    }

    protected void render() {
        render(null);
    }

    /**
     * Render board
     *
     * @param changes playboard changes or null for render all
     */
    abstract protected void render(PlayboardChanges changes);

    protected PlayboardRenderer getRenderer() { return renderer; }
    protected Playboard getBoard() { return board; }

    /**
     * Use these to ensure only one thread working on render bitmap
     */
    protected void lockRenderer() {
        try {
            renderLock.acquire();
        } catch (InterruptedException e) {
            // um...
        }
    }

    protected void unlockRenderer() { renderLock.release(); }

    /**
     * Extract render changes object from playboard changes
     *
     * May return null if whole board changes or changes is null
     */
    protected RenderChanges getRenderChanges(PlayboardChanges changes) {
        Collection<Position> cellChanges
            = changes == null ? null : changes.getCellChanges();
        return cellChanges == null
            ? null
            : new RenderChanges(
                new HashSet<>(cellChanges),
                new HashSet<>(changes.getChangedClueIDs())
            );
    }

    private String getCurrentResponse() {
        Playboard board = getBoard();
        Box box = (board == null) ? null : board.getCurrentBox();
        return (box == null)
            ? Box.BLANK
            : box.getResponse();
    }

    private boolean isForceCaps() {
        return forceCaps;
    }

    private boolean isNativeInput() {
        return nativeInput;
    }

    /**
     * BoardWordEditView needs to override to set scale bounds
     */
    protected void recreateRenderer(boolean render) {
        Playboard board = getBoard();
        if (board == null || boardColorScheme == null)
            return;

        DisplayMetrics metrics
            = getContext().getResources().getDisplayMetrics();

        renderer = new PlayboardRenderer(
            board,
            metrics.densityDpi,
            metrics.widthPixels,
            boardColorScheme,
            utils
        );
        setMaxScale(renderer.getMaxScale());
        setMinScale(renderer.getMinScale());

        float scale = getCurrentScale();

        // reset scale in case it violates new board dims
        setCurrentScale(scale, true);

        if (render)
            render();
    }
}
