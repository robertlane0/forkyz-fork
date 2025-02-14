
package app.crossword.yourealwaysbe.forkyz.view;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.settings.DisplaySeparators;
import app.crossword.yourealwaysbe.forkyz.util.MovementStrategyUtilsKt;
import app.crossword.yourealwaysbe.forkyz.view.PlayboardRenderer.RenderChanges;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.MovementStrategy;
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Zone;

/**
 * A live view of a word on the playboard
 *
 * Renders the playboard on change and implements input connection with
 * soft-input.
 *
 * Use setWord to specify which word to draw, else current word will be
 * drawn.
 */
@AndroidEntryPoint
public class BoardWordEditView extends BoardEditView {
    private Word word;
    private boolean incognitoMode;
    private int originalHeight;
    private Set<String> suppressNotesList;
    boolean customSuppressNotesList = false;

    public BoardWordEditView(Context context) {
        this(context, null);
    }

    @Inject
    public BoardWordEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAllowOverScroll(false);
        setAllowZoom(false);
        setScaleBounds();
    }

    /**
     * Set incognito mode
     *
     * This is a bit of a hack for having a more or less invisible input
     * view at the top of the clues list if the board is being show in
     * the clues items.
     */
    public void setIncognitoMode(boolean incognitoMode) {
        if (this.incognitoMode != incognitoMode) {
            this.incognitoMode = incognitoMode;

            ViewGroup.LayoutParams params = getLayoutParams();
            if (incognitoMode) {
                setBitmap(null);
                originalHeight = params.height;
                params.height = 1;
            } else {
                params.height = originalHeight;
                renderToView();
            }

            setLayoutParams(params);
        }
    }

    @Override
    public void setBoard(Playboard board, boolean noRender) {
        super.setBoard(board, noRender);
        setScaleBounds();
    }

    /**
     * Convenience to set board and suppress notes list efficiently
     */
    public void setBoard(Playboard board, Set<String> suppressNotesList) {
        this.suppressNotesList = suppressNotesList;
        this.customSuppressNotesList = true;
        setBoard(board);
    }

    /**
     * Set word to draw or null to draw current word
     */
    public void setWord(Word word, Set<String> suppressNotesList) {
        boolean sameWord = Objects.equals(this.word, word);
        boolean sameList = customSuppressNotesList
            && Objects.equals(this.suppressNotesList, suppressNotesList);

        if (!sameWord || !sameList) {
            this.suppressNotesList = suppressNotesList;
            this.customSuppressNotesList = true;
            this.word = word;
            // need to rescale with new word
            renderToView();
        }
    }

    /**
     * Set word to draw or null to draw current word with default notes
     *
     * Default notes depends if user has them enabled
     */
    public void setWord(Word word) {
        boolean sameWord = Objects.equals(this.word, word);
        boolean sameList = !customSuppressNotesList;
        if (!sameWord || !sameList) {
            this.suppressNotesList = null;
            this.customSuppressNotesList = false;
            this.word = word;
            // need to rescale with new word
            renderToView();
        }
    }

    /**
     * Overrides to stay on same word
     *
     * Changes movement strategy to stop on end before calling to super,
     * then changes back. Stops this single word view changing words
     * when typing.
     */
    @Override
    public void onNewResponse(String response) {
        Playboard board = getBoard();
        if (board == null)
            return;

        // fix movement strategy so that we don't fall into the next
        // clue
        MovementStrategy oldStrategy = board.getMovementStrategy();
        MovementStrategyUtilsKt.getStopOnEndStrategy(
            getSettings(),
            stopOnEnd -> {
                board.setMovementStrategy(stopOnEnd);
                super.onNewResponse(response);
                board.setMovementStrategy(oldStrategy);
            }
        );
    }

    @Override
    public void onPlayboardChange(PlayboardChanges changes) {
        if (isRendering()) {
            Word currentWord = changes.getCurrentWord();
            Word previousWord = changes.getPreviousWord();
            Collection<Position> cellChanges = changes.getCellChanges();

            // if rendering current word, which is changing, refit
            // else redraw if data indicates the rendered word is
            // affected
            if (
                isRenderingCurrentWord()
                && !Objects.equals(currentWord, previousWord)
            ) {
                renderToView();
            } else if (redrawNeeded(currentWord, previousWord, changes)) {
                render(changes);
            }
        }
        super.onPlayboardChange(changes);
    }

    @Override
    public Position findPosition(Point point) {
        PlayboardRenderer renderer = getRenderer();
        if (renderer == null)
            return null;

        Zone zone = getRenderWordZone();
        if (zone == null)
            return null;

        Position pos = renderer.findPosition(point);
        if (pos == null)
            return null;

        int boxesPerRow = renderer.getNumBoxesPerRow(getContentWidth());
        int box = boxesPerRow * pos.getRow() + pos.getCol();
        return zone.getPosition(box);
    }

    @Override
    public float fitToView() {
        return fitToView(false);
    }

    @Override
    public void render(PlayboardChanges changes) {
        // don't draw until we get onSizeChanged
        if (!isRendering()) {
            return;
        }

        PlayboardRenderer renderer = getRenderer();
        if (renderer == null)
            return;

        Word renderWord = getRenderWord();

        RenderChanges renderChanges = getRenderChanges(changes);

        getSettings().getPlayRenderSettings(renderSettings -> {
            Set<String> localSuppressNotesList
                = getSuppressNotesList(renderSettings.getDisplayScratch());
            renderer.setHintHighlight(
                !renderSettings.getSuppressHintHighlighting()
            );
            executorService.execute(() -> {

                boolean displaySeparators
                    = renderSettings.getDisplaySeparators()
                        != DisplaySeparators.DS_NEVER;

                try {
                    lockRenderer();
                    BitmapGrid bitmap = renderer.drawWord(
                        renderWord,
                        renderChanges,
                        localSuppressNotesList,
                        getContentWidth(),
                        displaySeparators,
                        renderSettings.getInferSeparators()
                    );
                    handler.post(() -> {
                        try {
                            if (isRendering())
                                setBitmap(bitmap);
                        } finally {
                            unlockRenderer();
                        }
                    });
                } catch (Exception e) {
                    unlockRenderer();
                    throw e;
                }
            });
        });
    }

    @Override
    protected void onSizeChanged(
        int newWidth, int newHeight, int oldWidth, int oldHeight
    ) {
        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);

        if (!isRendering())
            return;

        renderToView();
    }

    @Override
    protected void onClick(Position newPosition) {
        if (!isRendering())
            return;

        Playboard board = getBoard();
        if (board == null)
            return;

        if (!board.isInWord(newPosition))
            return;

        Position curPosition = board.getHighlightLetter();
        ClueID curCID = board.getClueID();

        Word renderWord = getRenderWord();
        ClueID renderCID = renderWord.getClueID();

        boolean samePosition
            = Objects.equals(curPosition, newPosition)
                && Objects.equals(curCID, renderCID);

        if (samePosition) {
            // don't set highlight letter to avoid toggle of direction
            Word currentWord = board.getCurrentWord();
            notifyClick(newPosition, currentWord);
        } else {
            Word previousWord
                = board.setHighlightLetter(newPosition, renderCID);
            notifyClick(newPosition, previousWord);
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE)
            renderToView();
    }

    private Set<String> getSuppressNotesList(boolean displayScratch) {
        if (customSuppressNotesList)
            return suppressNotesList;
        else
            return displayScratch ? Collections.emptySet() : null;
    }

    private boolean isRenderingCurrentWord() {
        return word == null;
    }

    private Word getRenderWord() {
        Word renderWord = word;
        if (isRenderingCurrentWord()) {
            Playboard board = getBoard();
            renderWord = (board == null) ? null : board.getCurrentWord();
        }
        return renderWord;
    }

    private Zone getRenderWordZone() {
        Word renderWord = getRenderWord();
        return (renderWord == null) ? null : renderWord.getZone();
    }

    private float fitToView(boolean noRender) {
        PlayboardRenderer renderer = getRenderer();
        if (renderer == null)
            return -1;

        scrollTo(0, 0);

        int width = getContentWidth();
        Zone zone = getRenderWordZone();
        if (zone == null)
            return -1;

        int numBoxes = zone.size();

        float scale = renderer.fitWidthTo(width, numBoxes);

        setCurrentScale(scale, noRender);

        return scale;
    }

    /**
     * Width of the usable area inside the view (sans padding)
     */
    private int getContentWidth() {
        return getContentWidth(getMeasuredWidth());
    }

    /**
     * getContentWidth for given total view width
     */
    private int getContentWidth(int totalWidth) {
        return totalWidth - getPaddingLeft() - getPaddingRight();
    }

    /**
     * Indicates whether rendering
     *
     * Not rendering if in incognito mode, or width is zero, or not
     * visible
     */
    private boolean isRendering() {
        return !incognitoMode
            && getMeasuredWidth() != 0
            && getVisibility() == VISIBLE;
    }

    /**
     * Render after making sure fits view size
     *
     * Does a full redraw, returns true if change in size
     */
    private void renderToView() {
        // don't render during fitToView because we want to
        // guarantee rendering (and fitToView won't render
        // if the scale doesn't change).
        fitToView(true);
        render();
    }

    private boolean redrawNeeded(
        Word currentWord, Word previousWord, PlayboardChanges changes
    ) {
        Collection<Position> cellChanges
            = changes == null ? null : changes.getCellChanges();
        if (cellChanges == null)
            return true;

        Zone zone = getRenderWordZone();
        if (zone == null)
            return false;

        for (Position position : zone) {
            if (cellChanges.contains(position))
                return true;
        }

        Playboard board = getBoard();
        ClueID cid = board == null ? null : board.getClueID();
        if (cid == null)
            return false;

        if (changes.getChangedClueIDs().contains(cid))
            return true;

        return false;
    }

    private void setScaleBounds() {
        setMaxScale(1.0F);
        setMinScale(0.6F);
    }
}
