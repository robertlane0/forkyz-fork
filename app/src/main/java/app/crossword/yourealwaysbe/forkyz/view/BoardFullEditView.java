
package app.crossword.yourealwaysbe.forkyz.view;

import java.util.Collections;
import java.util.Set;
import javax.inject.Inject;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.view.PlayboardRenderer.RenderChanges;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * A live view of the full playboard
 *
 * Renders the playboard on change and implements input connection with
 * soft-input.
 */
@AndroidEntryPoint
public class BoardFullEditView extends BoardEditView {
    // how far off screen in inches to still draw grid
    private static final int SCREEN_OVERDRAW = 2;

    // screen overdraw in pixels
    private int screenOverdrawPx;

    @Inject
    public BoardFullEditView(Context context, AttributeSet attrs) {
        super(context, attrs);

        DisplayMetrics metrics
            = getContext().getResources().getDisplayMetrics();
        screenOverdrawPx = metrics.densityDpi * SCREEN_OVERDRAW;
    }

    @Override
    public Position findPosition(Point point) {
        PlayboardRenderer renderer = getRenderer();
        if (renderer == null)
            return null;
        return renderer.findPosition(point);
    }

    @Override
    public float fitToView() {
        PlayboardRenderer renderer = getRenderer();
        if (renderer == null)
            return -1;

        scrollTo(0, 0);
        int w = getWidth();
        int h = getHeight();
        float scale = renderer.fitTo(w, h);
        setCurrentScale(scale);
        return scale;
    }

    @Override
    public void onPlayboardChange(PlayboardChanges changes) {
        render(changes);
        ensureVisible(changes.getPreviousWord());
        super.onPlayboardChange(changes);
    }

    @Override
    public void render(PlayboardChanges changes, boolean rescale) {
        PlayboardRenderer renderer = getRenderer();
        Playboard board = getBoard();
        if (renderer == null || board == null)
            return;

        RenderChanges renderChanges = getRenderChanges(changes);

        getSettings().getPlayScratchDisplay(displayScratch -> {
        getSettings().getPlaySuppressHintHighlighting(suppressHints -> {
            renderer.setHintHighlight(!suppressHints);

            Set<String> suppressNotesList = displayScratch
                ? Collections.emptySet()
                : null;

            executorService.execute(() -> {
                try {
                    lockRenderer();
                    BitmapGrid bitmap = renderer.draw(
                        renderChanges, suppressNotesList, getRenderRect()
                    );
                    handler.post(() -> {
                        try { setBitmap(bitmap, rescale); }
                        finally { unlockRenderer(); }
                    });
                } catch (Exception e) {
                    unlockRenderer();
                    throw e;
                }
            });
        });});
    }

    @Override
    protected void onClick(Position position) {
        Playboard board = getBoard();
        if (board == null)
            return;

        if (board.isInWord(position)) {
            Word previousWord = board.setHighlightLetter(position);
            notifyClick(position, previousWord);
        } else {
            PlayboardRenderer renderer = getRenderer();
            if (renderer == null)
                return;

            Position cellPos = renderer.getUnpinnedPosition(position);
            if (cellPos != null) {
                Puzzle puz = board.getPuzzle();
                if (puz == null)
                    return;

                ClueID pinnedCID = puz.getPinnedClueID();
                Word previousWord
                    = board.setHighlightLetter(cellPos, pinnedCID);
                notifyClick(position, previousWord);
            }
        }
    }

    @Override
    protected void scrollEnd() {
        super.scrollEnd();
        render(PlayboardChanges.getEmptyChanges());
    }

    /**
     * The area of grid to draw during render
     *
     * Will be visible rect, plus a buffer
     */
    private Rect getRenderRect() {
        Rect rect = getVisibleRect();
        rect.inset(-screenOverdrawPx, -screenOverdrawPx);
        return rect;
    }

    private void ensureVisible(Word previousWord) {
        PlayboardRenderer renderer = getRenderer();
        Playboard board = getBoard();
        if (renderer == null || board == null)
            return;

        /*
         * If we jumped to a new word, ensure the first letter is visible.
         * Otherwise, insure that the current letter is visible. Only necessary
         * if the cursor is currently off screen.
         */
        getSettings().getPlayEnsureVisible(ensureVisible -> {
            if (ensureVisible) {
                Word currentWord = board.getCurrentWord();
                Position cursorPos = board.getHighlightLetter();

                Point topLeft;
                Point bottomRight;
                Point cursorTopLeft;
                Point cursorBottomRight;

                cursorTopLeft = renderer.findPointTopLeft(cursorPos);
                cursorBottomRight = renderer.findPointBottomRight(cursorPos);

                if (
                    (previousWord != null)
                    && previousWord.equals(currentWord)
                ) {
                    topLeft = cursorTopLeft;
                    bottomRight = cursorBottomRight;
                } else {
                    topLeft = renderer.findPointTopLeft(currentWord);
                    bottomRight = renderer.findPointBottomRight(currentWord);
                }

                ensureVisible(bottomRight);
                ensureVisible(topLeft);

                // ensure the cursor is always on the screen.
                ensureVisible(cursorBottomRight);
                ensureVisible(cursorTopLeft);
            }
        });
    }
}
