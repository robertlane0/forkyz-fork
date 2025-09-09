package app.crossword.yourealwaysbe.forkyz.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Base64;

import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.settings.DisplaySeparators;
import app.crossword.yourealwaysbe.forkyz.util.ColorUtils;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.forkyz.view.ScrollingImageView.Point;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.PuzImage;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;

import java.util.regex.Pattern;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * For rendering part of the board
 *
 * Caches an internal bitmap and reuses. Do not mix calls to
 * draw/drawWord/drawBoxes unless deliberately erasing previous draws.
 */
public class PlayboardRenderer {
    // for calculating max scale with no puzzle
    private static final int DEFAULT_PUZZLE_WIDTH = 15;
    private static final float BASE_BOX_SIZE_INCHES = 0.25F;
    private static final float DESCENT_FUDGE_FACTOR = 1.3F;
    private static final float SEPARATOR_FILL_PCNT = 0.85F;

    // RecordingCanvas will not draw bitmaps bigger than its own
    // constant MAX_BITMAP_SIZE, which at as of 2024/05/28 was at least
    // 100mb.
    // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/graphics/java/android/graphics/RecordingCanvas.java
    // MAX_BITMAP_SIZE is public in RecordingCanvas, but @hide-ed.
    // 100mb / 4 (4 bytes per pixel) and square rooted to get
    // the size of one dimension is just over 5000 pixels square. Use a
    // lot less here and tile instead!
    private static final int MAX_BITMAP_DIM = 1000;
    private static final float MAX_BOX_SIZE_INCHES = 1;
    private static final int MIN_PIXELS_PER_BOX = 4;

    private static final Pattern WHITESPACE_RE = Pattern.compile("\\s+");

    // set in constructor
    private final Typeface TYPEFACE_SEMI_BOLD_SANS;

    private PaintProfile profile;
    private BitmapGrid bitmapGrid;
    private Playboard board;
    private float dpi;
    private float scale = 1.0F;
    private float maxScale;
    private float minScale;
    private boolean hintHighlight;
    private int displayWidthPixels;
    private AndroidVersionUtils versionUtils;

    public static record RenderChanges(
        Collection<Position> changedPositions,
        Collection<ClueID> changedClueIDs
    ) { }

    public static class BoxSeparators {
        private String top;
        private String bottom;
        private String left;
        private String right;

        public BoxSeparators(
            String top, String bottom, String left, String right
        ) {
            this.top = top;
            this.bottom = bottom;
            this.left = left;
            this.right = right;
        }

        public BoxSeparators() {
            this(null, null, null, null);
        }

        public String getTop() { return top; }
        public String getBottom() { return bottom; }
        public String getLeft() { return left; }
        public String getRight() { return right; }

        public void setTop(String top) { this.top = top; }
        public void setBottom(String bottom) { this.bottom = bottom; }
        public void setLeft(String left) { this.left = left; }
        public void setRight(String right) { this.right = right; }

        @Override
        public boolean equals(Object other) {
            if (other == null)
                return false;
            if (other.getClass() != this.getClass())
                return false;
            BoxSeparators o = (BoxSeparators) other;
            return getTop() == o.getTop()
                && getBottom() == o.getBottom()
                && getLeft() == o.getLeft()
                && getRight() == o.getRight();
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                getTop(),
                getBottom(),
                getLeft(),
                getRight()
            );
        }

        public String toString() {
            return top + " / " + bottom + " / " + left + " / " + right;
        }
    }

    // colors are gotten from context
    public PlayboardRenderer(
        Playboard board,
        float dpi, int displayWidthPixels,
        Context context, AndroidVersionUtils versionUtils
    ) {
        TYPEFACE_SEMI_BOLD_SANS = versionUtils.getSemiBoldTypeface();

        this.dpi = dpi;
        this.displayWidthPixels = displayWidthPixels;
        this.board = board;
        this.hintHighlight = false;
        this.maxScale = getDeviceMaxScale();
        this.minScale = getDeviceMinScale();
        this.profile = new PaintProfile(context, scale);
        this.versionUtils = versionUtils;
    }

    public void setHintHighlight(boolean hintHighlight) {
        this.hintHighlight = hintHighlight;
    }

    public float getMaxScale() {
        return maxScale;
    }

    public float getMinScale() {
        return minScale;
    }

    public void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    public void setMinScale(float minScale) {
        this.minScale = minScale;
    }

    synchronized public void setScale(float scale) {
        scale = boundScale(scale);

        this.bitmapGrid = null;
        this.scale = scale;

        profile.setScale(scale);
    }

    public float getScale() {
        return this.scale;
    }

    /**
     * Draw the board or just refresh it
     *
     * Refreshes current word and reset word if not null
     *
     * @param changes the changes or null for redraw all
     * @param suppressNotesLists as in drawBox
     * @param displaySeparators the mode for showing cell separators
     * @param inferSeparators whether to draw inferred separators
     * @param visibleRect area that will be visible (can be used to
     * avoid drawing outside of it). Null means draw all.
     */
    synchronized public BitmapGrid draw(
        RenderChanges changes,
        Set<String> suppressNotesLists,
        DisplaySeparators displaySeparators,
        boolean inferSeparators,
        Rect visibleRect
    ) {
        try {
            boolean newBitmap = initialiseBitmap(
                getFullWidth(), getFullHeight()
            );

            // if new bitmap, rerender all
            if (newBitmap)
                changes = null;

            TileCanvas[][] canvases = makeBitmapGridCanvases(visibleRect);
            if (canvases == null)
                return bitmapGrid;

            drawBoardBoxes(
                canvases,
                changes,
                suppressNotesLists,
                displaySeparators,
                inferSeparators
            );
            drawPinnedClue(
                canvases,
                changes,
                suppressNotesLists,
                displaySeparators,
                inferSeparators
            );

            drawImages(canvases, changes == null);
            return bitmapGrid;
        } catch (OutOfMemoryError e) {
            return bitmapGrid;
        }
    }

    /**
     * getNumBoxesPerRow at current scale
     */
    public int getNumBoxesPerRow(int wrapWidth) {
        return getNumBoxesPerRow(wrapWidth, getScale());
    }

    public int getNumBoxesPerRow(int wrapWidth, float scale) {
        int boxSize = calcBoxSize(scale);
        return wrapWidth / boxSize;
    }

    /**
     * Size of image that would be drawn for word
     *
     * @return row in height in pixels, col is width in pixels
     */
    public Position getWordPixelsForWidth(Word word, int wrapWidth) {
        Zone zone = word == null ? null : word.getZone();
        int len = zone == null ? 0 : zone.size();
        Position dims = getBoxesDimsForWidth(wrapWidth, len, getScale());
        int boxSize = getBoxSize();
        return new Position(dims.getRow() * boxSize, dims.getCol() * boxSize);
    }

    /**
     * Draw given word
     *
     * @param word word to draw
     * @param changes changes to render or null for render all
     * @param suppressNotesLists as in drawBox
     * @param wrapWidth if non-zero, wrap word after width (in pixels)
     * exceeded
     * @param displaySeparators whether to show the word separators
     * @param inferSeparators whether to draw inferred separators
     */
    synchronized public BitmapGrid drawWord(
        Word word,
        RenderChanges changes,
        Set<String> suppressNotesLists,
        int wrapWidth,
        boolean displaySeparators,
        boolean inferSeparators
    ) {
        Box[] boxes = board.getWordBoxes(word);
        Zone zone = word.getZone();
        int boxSize = getBoxSize();
        Position highlight = board.getHighlightLetter();

        Position dims = getBoxesDimsForWidth(
            wrapWidth, boxes.length, getScale()
        );
        int height = dims.getRow();
        int width = dims.getCol();

        if (initialiseBitmap(width, height))
            changes = null;

        TileCanvas[][] canvases = makeBitmapGridCanvases();

        for (int i = 0; i < boxes.length; i++) {
            Position pos = zone.getPosition(i);

            if (!isRenderPos(pos, changes))
                continue;

            int x = (i % width) * boxSize;
            int y = (i / width) * boxSize;

            TileCanvas tileCanvas = getCanvasByPixel(canvases, x, y);
            if (tileCanvas != null) {
                BoxSeparators separators = displaySeparators
                    ? getBoxSeparators(word.getClueID(), inferSeparators, i)
                    : null;

                this.drawBox(
                    canvases,
                    x, y,
                    pos.getRow(), pos.getCol(),
                    boxes[i],
                    null,
                    null, highlight,
                    suppressNotesLists,
                    false,
                    separators
                );
            }
        }

        // draw highlight outline again as it will have been overpainted
        if (highlight != null) {
            PaintProfile profile = getProfile();
            int idx = zone.indexOf(highlight);
            if (idx > -1) {
                int x = idx * boxSize;
                drawBoxOutline(
                    canvases, x, 0, profile.getCurrentLetterBox()
                );
            }
        }

        return bitmapGrid;
    }

    /**
     * Draw the boxes
     *
     * @param boxes boxes to draw
     * @param shadowString string of characters to display as "notes",
     * one for each box or null if none, shown is blank and has no
     * puzzle notes to show
     * @param changes array of positions that have changed (should be
     * rendered). Can be null to draw all.
     * @param highlight the position in the box list to highlight
     * @param suppressNotesLists as in drawBox
     * @param wrapWidth if non-zero, wrap after this number of pixels
     * @param separators the separators to draw on the boxes (or null)
     */
    synchronized public BitmapGrid drawBoxes(
        Box[] boxes,
        String shadowString,
        boolean[] changes,
        Position highlight,
        Set<String> suppressNotesLists,
        int wrapWidth,
        BoxSeparators[] separators
    ) {
        if (boxes == null || boxes.length == 0) {
            return null;
        }

        int boxSize = getBoxSize();

        Position dims = getBoxesDimsForWidth(
            wrapWidth, boxes.length, getScale()
        );
        int height = dims.getRow();
        int width = dims.getCol();

        if (initialiseBitmap(width, height))
            changes = null;

        if (width == 0 || height == 0)
            return bitmapGrid;

        TileCanvas[][] canvases = makeBitmapGridCanvases();

        for (int i = 0; i < boxes.length; i++) {
            if (changes != null && !changes[i])
                continue;

            String shadow = null;
            if (shadowString != null && i < shadowString.length())
                shadow = Character.toString(shadowString.charAt(i));

            int x = (i % width) * boxSize;
            int y = (i / width) * boxSize;

            BoxSeparators boxSeparators = null;
            if (separators != null && i < separators.length)
                boxSeparators = separators[i];

            TileCanvas tileCanvas = getCanvasByPixel(canvases, x, y);
            if (tileCanvas != null) {
                this.drawBox(
                    canvases,
                    x, y,
                    0, i,
                    boxes[i],
                    shadow,
                    null,
                    highlight,
                    suppressNotesLists,
                    false,
                    boxSeparators
                );
            }
        }

        if (highlight != null) {
            PaintProfile profile = getProfile();
            int col = highlight.getCol();
            if (col >= 0 && col < boxes.length) {
                int x = col * boxSize;
                int y = 0;
                drawBoxOutline(
                    canvases, x, y, profile.getCurrentLetterBox()
                );
            }
        }

        return bitmapGrid;
    }

    /**
     * Board position of the point
     *
     * Not checked if in bounds, if outsize main board, check with
     * getUnpinnedPosition to see if it's a position in the pinned
     * display
     */
    public Position findPosition(Point p) {
        int boxSize = getBoxSize();

        int col = p.x / boxSize;
        int row = p.y / boxSize;

        return new Position(row, col);
    }

    /**
     * Convert a position to true position on board
     *
     * If position not on board but in the display of the pinned clue,
     * return the box on the main board corresponding to this box on
     * the pinned clue.
     *
     * Else return null;
     */
    public Position getUnpinnedPosition(Position pos) {
        Zone pinnedZone = getPinnedZone();
        if (pinnedZone == null)
            return null;

        if (pos.getRow() != getPinnedRow())
            return null;

        int col = pos.getCol() - getPinnedCol();

        if (col >= 0 && col < pinnedZone.size())
            return pinnedZone.getPosition(col);
        else
            return null;
    }

    public int findBoxNoScale(Point p) {
        int boxSize =  (int) (BASE_BOX_SIZE_INCHES * dpi);
        return p.x / boxSize;
    }

    public Point findPointBottomRight(Position p) {
        int boxSize = getBoxSize();
        int x = (p.getCol() * boxSize) + boxSize;
        int y = (p.getRow() * boxSize) + boxSize;

        return new Point(x, y);
    }

    public Point findPointBottomRight(Word word) {
        Zone zone = word.getZone();

        if (zone == null || zone.isEmpty())
            return null;

        // for now assume that last box is bottom right
        Position p = zone.getPosition(zone.size() - 1);

        int boxSize = getBoxSize();
        int x = (p.getCol() * boxSize) + boxSize;
        int y = (p.getRow() * boxSize) + boxSize;

        return new Point(x, y);
    }

    public Point findPointTopLeft(Position p) {
        int boxSize = getBoxSize();
        int x = p.getCol() * boxSize;
        int y = p.getRow() * boxSize;

        return new Point(x, y);
    }

    public Point findPointTopLeft(Word word) {
        // for now, assume first zone position is top left
        Zone zone = word.getZone();
        if (zone == null || zone.isEmpty())
            return null;
        return findPointTopLeft(zone.getPosition(0));
    }

    synchronized public float fitTo(int width, int height) {
        return fitTo(width, height, getFullWidth(), getFullHeight());
    }

    synchronized public float fitTo(
        int width, int height, int numBoxesWidth, int numBoxesHeight
    ) {
        this.bitmapGrid = null;
        float newScaleWidth = calculateScale(width, numBoxesWidth);
        float newScaleHeight = calculateScale(height, numBoxesHeight);
        setScale(Math.min(newScaleWidth, newScaleHeight));
        return getScale();
    }

    /**
     * Calc needed scale to fix boxes to width and bound to min/max
     */
    public float calculateScale(int numPixels, int numBoxes) {
        double density = (double) dpi * (double) BASE_BOX_SIZE_INCHES;
        float newScale = (float) (
            (double) numPixels / (double) numBoxes / density
        );
        return boundScale(newScale);
    }

    synchronized public float fitWidthTo(int width, int numBoxes) {
        this.bitmapGrid = null;
        setScale(calculateScale(width, numBoxes));
        return getScale();
    }

    synchronized public float zoomIn() {
        this.bitmapGrid = null;
        this.scale = scale * 1.25F;
        if(scale > this.getMaxScale()){
            this.scale = this.getMaxScale();
        }
        return scale;
    }

    synchronized public float zoomOut() {
        this.bitmapGrid = null;
        this.scale = scale / 1.25F;
        if(scale < this.getMinScale()){
            scale = this.getMinScale();
        }
        return scale;
    }

    synchronized public float zoomReset() {
        this.bitmapGrid = null;
        this.scale = 1.0F;
        return scale;
    }

    synchronized public float zoomInMax() {
        this.bitmapGrid = null;
        this.scale = getMaxScale();

        return scale;
    }

    /**
     * Get the separators for the box displaying word at offset
     *
     * For when cid is being show in a horizontal row
     *
     * @param clue the clue to get separators from
     * @param inferSeparators whether to infer separators
     * @param offset the position in the clue zone of the box
     * @return may return null if no separators
     */
    public BoxSeparators getBoxSeparators(
        Clue clue, boolean inferSeparators, int offset
    ) {
        // draw separator from offset + 1 on the right
        String right = clue.getSeparator(offset + 1, inferSeparators);
        // and first one if needed
        String left = offset == 0
            ? clue.getSeparator(0, inferSeparators)
            : null;

        return new BoxSeparators(null, null, left, right);
    }

    public BoxSeparators getBoxSeparators(
        ClueID cid, boolean inferSeparators, int offset
    ) {
        Puzzle puz = board == null ? null : board.getPuzzle();
        if (puz == null)
            return null;

        Clue clue = puz.getClue(cid);
        if (clue == null)
            return null;

        return getBoxSeparators(clue, inferSeparators, offset);
    }

    /**
     * Get the separators for the box on the full board
     *
     * May return null if no separators
     */
    private BoxSeparators getBoxSeparators(
        DisplaySeparators displaySeparators,
        boolean inferSeparators,
        Position position,
        Word currentWord
    ) {
        if (displaySeparators == DisplaySeparators.DS_NEVER)
            return null;

        Puzzle puz = board == null ? null : board.getPuzzle();
        if (puz == null)
            return null;

        Box box = puz.checkedGetBox(position);
        if (box == null)
            return null;

        BoxSeparators separators = new BoxSeparators();

        // do other clues first if they should be there
        if (displaySeparators == DisplaySeparators.DS_ALWAYS) {
            for (ClueID cid : box.getIsPartOfClues()) {
                addBoxSeparators(
                    puz.getClue(cid), inferSeparators, position, separators
                );
            }
        }

        // current clue takes precedence
        ClueID currentCID = (currentWord == null)
            ? null
            : currentWord.getClueID();
        if (box.isPartOf(currentCID)) {
            addBoxSeparators(
                puz.getClue(currentCID), inferSeparators, position, separators
            );
        }

        return separators;
    }

    /**
     * Add all separators for clue for box
     */
    private void addBoxSeparators(
        Clue clue,
        boolean inferSeparators,
        Position position,
        BoxSeparators separators
    ) {
        if (clue != null && clue.hasSeparators(inferSeparators)) {
            int offset = clue.getZone().indexOf(position);
            addBoxSeparator(clue, inferSeparators, offset, separators);
            // add start sep if needed
            if (offset == 0)
                addBoxSeparator(clue, inferSeparators, -1, separators);
        }
    }

    /**
     * Set separator for box and clue at particular offset
     *
     * Takes from offset+1. It looks cleaner on the board to have the
     * separator on e.g. the right instead of left, as it doesn't clash
     * with numbers so much.
     */
    private void addBoxSeparator(
        Clue clue, boolean inferSeparators, int offset, BoxSeparators separators
    ) {
        if (clue == null)
            return;

        String separator = clue.getSeparator(offset + 1, inferSeparators);
        if (separator != null) {
            ClueID cid = clue.getClueID();
            Zone zone = clue.getZone();
            if (zone != null && !zone.isEmpty()) {
                // try to figure out where to put the separator, default
                // to right
                Zone.Direction dir = zone.getDirection(offset);
                if (dir == Zone.Direction.INCONCLUSIVE)
                    dir = zone.getDirection(offset - 1);
                if (dir == Zone.Direction.INCONCLUSIVE)
                    dir = Zone.Direction.RIGHT;

                // on opposite site if coming from start (-1)
                if (offset < 0)
                    dir = dir.reverse();

                switch (dir) {
                case UP: separators.setTop(separator); break;
                case DOWN: separators.setBottom(separator); break;
                case LEFT: separators.setLeft(separator); break;
                case RIGHT: separators.setRight(separator); break;
                }
            }
        }
    }

    /**
     * Get max scale for device
     *
     * This aims for enough to make boxes MAX_BOX_SIZE_INCHES but allows
     * larger if needed to fit screen
     */
    private float getDeviceMaxScale(){
        Puzzle puz = (board == null) ? null : board.getPuzzle();
        int width = (puz == null) ? DEFAULT_PUZZLE_WIDTH : puz.getWidth();

        float puzzleBaseSizeInPixels = width * BASE_BOX_SIZE_INCHES * dpi;
        float fitToScreen =  displayWidthPixels / puzzleBaseSizeInPixels;
        float maxBoxSize = MAX_BOX_SIZE_INCHES / BASE_BOX_SIZE_INCHES;

        return Math.max(maxBoxSize, fitToScreen);
    }

    /**
     * Get min scale
     *
     * To have min pixels per box
     */
    private float getDeviceMinScale(){
        return MIN_PIXELS_PER_BOX / (BASE_BOX_SIZE_INCHES * dpi);
    }

    /**
     * Bound scale to set min/max
     */
    private float boundScale(float scale) {
        float maxScale = getMaxScale();
        float minScale = getMinScale();

        if (scale > maxScale) {
            return maxScale;
        } else if (scale < minScale) {
            return minScale;
        } else if (Float.isNaN(scale)) {
            return 1.0f;
        } else {
            return scale;
        }
    }

    /**
     * The number of rows/cols when rendering numBoxes at wrapWidth
     *
     * In drawBoxes mode
     */
    private Position getBoxesDimsForWidth(
        int wrapWidth, int numBoxes, float scale
    ) {
        int rows;
        int cols;

        if (wrapWidth > 0) {
            int boxesPerRow = getNumBoxesPerRow(wrapWidth, scale);
            rows = (int) Math.ceil(numBoxes / (float) boxesPerRow);
            cols = Math.min(boxesPerRow, numBoxes);
        } else {
            rows = 1;
            cols = numBoxes;
        }

        return new Position(rows, cols);
    }

    /**
     * Draw an individual box
     *
     * @param shadow a character to display if the box is blank and has no
     * other notes to show. Ignored if null.
     * @param fullBoard whether to draw details that only make sense when the
     * full board can be seen.
     * @param suppressNotesLists set of lists to not draw notes from.
     * Empty set means draw notes from all lists, null means don't draw
     * any notes.
     * @param displaySeparators true if separators are to be shown
     * @param separatorCID if not null, show separators for this clue
     * only
     */
    private void drawBox(
        TileCanvas[][] canvases,
        int x, int y,
        int row, int col,
        Box box,
        String shadow,
        Word currentWord,
        Position highlight,
        Set<String> suppressNotesLists,
        boolean fullBoard,
        BoxSeparators separators
    ) {
        PaintProfile profile = getProfile();
        int boxSize = profile.getBoxSize();

        boolean isHighlighted
            = highlight.getCol() == col
                && highlight.getRow() == row;

        Paint outlineColor = isHighlighted
            ? profile.getCurrentLetterBox()
            : profile.getOutline(box);
        drawBoxOutline(canvases, x, y, outlineColor);

        // now only draw on one canvas as tiles are lined up by box
        // contents
        TileCanvas tileCanvas = getCanvasByPixel(canvases, x, y);
        if (tileCanvas == null)
            return;
        Canvas canvas = tileCanvas.canvas();

        Rect r = new Rect(x + 1, y + 1, (x + boxSize) - 1, (y + boxSize) - 1);

        if (box == null) {
            canvas.drawRect(r, profile.getBoxColor(box));
        } else {
            if (highlightError(box, isHighlighted))
                box.setCheated(true);

            boolean inCurrentWord =
                (currentWord != null) && currentWord.checkInWord(row, col);

            drawBoxBackground(
                canvas, box, row, col, r, highlight, inCurrentWord
            );

            drawBoxShape(canvas, x, y, box, inCurrentWord);

            // Bars before clue numbers to avoid obfuscating
            if (fullBoard)
                drawBoxBars(canvas, x, y, box);

            drawBoxMarks(canvas, x, y, box, inCurrentWord);
            drawBoxFlags(canvas, x, y, box);
            drawBoxSeparators(
                canvas,
                x, y, box,
                inCurrentWord,
                separators
            );

            if (box.isBlank()) {
                if (suppressNotesLists != null || shadow != null) {
                    drawBoxNotes(
                        canvas, x, y, box,
                        shadow, inCurrentWord, suppressNotesLists
                    );
                }
            } else {
                drawBoxLetter(
                    canvas, x, y,
                    box, row, col,
                    isHighlighted, inCurrentWord
                );
            }
        }
    }

    private void drawBoxOutline(
        TileCanvas[][] canvases, int x, int y, Paint color
    ) {
        int boxSize = getBoxSize();
        int endx = x + boxSize;
        int endy = y + boxSize;
        int offset = (int) Math.ceil(color.getStrokeWidth() / 2);
        forTiles(
            canvases,
            x - offset,
            y - offset,
            endx + offset,
            endy + offset,
            tileCanvas -> {
                if (tileCanvas != null)
                    drawBoxOutline(tileCanvas.canvas(), x, y, color);
            }
        );
    }

    private void drawBoxOutline(
        Canvas canvas, int x, int y, Paint color
    ) {
        int boxSize = getBoxSize();
        int endx = x + boxSize;
        int endy = y + boxSize;
        // Draw left, top, right, bottom
        canvas.drawLine(x, y, x, y + boxSize, color);
        canvas.drawLine(x, y, x + boxSize, y, color);
        canvas.drawLine(x + boxSize, y, x + boxSize, y + boxSize, color);
        canvas.drawLine(x, y + boxSize, x + boxSize, y + boxSize, color);
    }

    private void drawBoxBackground(
        Canvas canvas, Box box, int row, int col,
        Rect boxRect, Position highlight, boolean inCurrentWord
    ) {
        PaintProfile profile = getProfile();

        // doesn't depend on current word (for BoxEditText)
        boolean isHighlighted
            = highlight.getCol() == col
                && highlight.getRow() == row;
        boolean highlightError = highlightError(box, isHighlighted);

        if (isHighlighted && !highlightError) {
            canvas.drawRect(boxRect, profile.getCurrentLetterHighlight());
        } else if (isHighlighted && highlightError) {
            canvas.drawRect(boxRect, profile.getErrorHighlight());
        } else if (inCurrentWord) {
            canvas.drawRect(boxRect, profile.getCurrentWordHighlight());
        } else if (highlightError) {
            canvas.drawRect(boxRect, profile.getError());
        } else if (this.hintHighlight && box.isCheated()) {
            canvas.drawRect(boxRect, profile.getCheated());
        } else {
            canvas.drawRect(boxRect, profile.getBoxColor(box));
        }
    }

    private void drawBoxBars(
        Canvas canvas, int x, int y, Box box
    ) {
        PaintProfile profile = getProfile();
        int boxSize = profile.getBoxSize();
        int barSize = profile.getBarSize();
        int offset = barSize / 2;

        if (box.isBarredLeft()) {
            int barx = x + offset;
            drawBar(
                canvas,
                barx, y, barx, y + boxSize,
                box, box.getBarLeft()
            );
        }

        if (box.isBarredTop()) {
            int bary = y + offset;
            drawBar(
                canvas,
                x, bary, x + boxSize, bary,
                box, box.getBarTop()
            );
        }

        if (box.isBarredRight()) {
            int barx = x + boxSize - offset;
            drawBar(
                canvas,
                barx, y, barx, y + boxSize,
                box, box.getBarRight()
            );
        }

        if (box.isBarredBottom()) {
            int bary = y + boxSize - offset;
            drawBar(
                canvas,
                x, bary, x + boxSize, bary,
                box, box.getBarBottom()
            );
        }
    }

    private void drawBar(
        Canvas canvas,
        int xstart, int ystart, int xend, int yend,
        Box box, Box.Bar barStyle
    ) {
        PaintProfile profile = getProfile();
        Paint barColor = profile.getBarColor(box, barStyle);
        Path path = new Path();
        path.moveTo(xstart, ystart);
        path.lineTo(xend, yend);
        canvas.drawPath(path, barColor);
    }

    private void drawBoxMarks(
        Canvas canvas, int x, int y, Box box, boolean inCurrentWord
    ) {
        PaintProfile profile = getProfile();
        int boxSize = profile.getBoxSize();
        int numberOffset = profile.getNumberOffset();
        TextPaint numberText = profile.getNumberText(box, inCurrentWord);

        if (box.hasClueNumber()) {
            String clueNumber = box.getClueNumber();
            drawHtmlText(
                canvas,
                clueNumber,
                x + numberOffset,
                y + numberOffset / 2,
                boxSize,
                numberText
            );
        }

        if (box.hasMarks()) {
            int markHeight = getTotalHeight(numberText);

            // 3x3 by guarantee of Box
            String[][] marks = box.getMarks();
            for (int row = 0; row < 3; row++) {
                int markY;
                switch (row) {
                case 1: // middle
                    markY = boxSize / 2 - markHeight / 2 - numberOffset / 2;
                    break;
                case 2: // bottom
                    markY = boxSize - numberOffset - markHeight;
                    break;
                default: // top
                    markY = numberOffset / 2;
                }

                for (int col = 0; col < 3; col++) {
                    if (marks[row][col] != null) {
                        int fullWidth = boxSize - 2 * numberOffset;
                        Layout.Alignment align;
                        switch (col) {
                        case 1: // centre
                            align = Layout.Alignment.ALIGN_CENTER;
                            break;
                        case 2: // right
                            align = Layout.Alignment.ALIGN_OPPOSITE;
                            break;
                        default: // left
                            align = Layout.Alignment.ALIGN_NORMAL;
                        }

                        drawHtmlText(
                            canvas,
                            marks[row][col],
                            x + numberOffset,
                            y + markY,
                            fullWidth,
                            align,
                            numberText
                        );
                    }
                }
            }
        }
    }

    private void drawBoxFlags(Canvas canvas, int x, int y, Box box) {
        PaintProfile profile = getProfile();
        int boxSize = profile.getBoxSize();
        int barSize = profile.getBarSize();
        int numberOffset = profile.getNumberOffset();
        int numberTextSize = profile.getNumberTextSize();

        Puzzle puz = board == null ? null : board.getPuzzle();
        if (puz == null)
            return;

        Clue flagAcross = null;
        Clue flagDown = null;

        for (ClueID cid : box.getIsPartOfClues()) {
            if (box.isStartOf(cid)) {
                Clue clue = puz.getClue(cid);
                if (clue != null && clue.isFlagged()) {
                    if (isClueProbablyAcross(cid))
                        flagAcross = clue;
                    else
                        flagDown = clue;
                }
            }
        }

        if (flagDown != null) {
            String clueNumber = box.getClueNumber();
            int numDigits = clueNumber == null ? 0 : clueNumber.length();
            int numWidth = numDigits * numberTextSize / 2;
            Rect bar = new Rect(
                x + numberOffset + numWidth + barSize,
                y + 1 * barSize,
                x + boxSize - barSize,
                y + 2 * barSize
            );
            canvas.drawRect(bar, profile.getFlag(flagDown));
        }

        if (flagAcross != null) {
            Rect bar = new Rect(
                x + 1 * barSize,
                y + barSize + numberOffset + numberTextSize,
                x + 2 * barSize,
                y + boxSize - barSize
            );
            canvas.drawRect(bar, profile.getFlag(flagAcross));
        }

        // flag on box
        if (box.isFlagged()) {
            Rect bar = new Rect(
                x + boxSize - 1 * barSize,
                y + barSize + numberOffset + numberTextSize,
                x + boxSize - 2 * barSize,
                y + boxSize - barSize
            );
            canvas.drawRect(bar, profile.getFlag(box));
        }

    }

    /**
     * Draw separators for box
     *
     * Draw separators for separatorCID if not null, else draws for all
     * clues, preferring the clue of the current word if there are
     * multiple clues with separators on the same side of the box.
     */
    private void drawBoxSeparators(
        Canvas canvas,
        int x,
        int y,
        Box box,
        boolean inCurrentWord,
        BoxSeparators separators
    ) {
        if (separators == null)
            return;

        PaintProfile profile = getProfile();
        int boxSize = profile.getBoxSize();
        int barSize = profile.getBarSize();
        int barOffset = barSize / 2;
        TextPaint text = profile.getSeparatorText();
        int height = getTotalHeight(text);
        int dashSize = profile.getDashSeparatorSize();

        String top = separators.getTop();
        if (isWhitespace(top)) {
            int bary = y + barOffset;
            drawBar(
                canvas,
                x, bary, x + boxSize, bary,
                box, Box.Bar.DOTTED
            );
        } else if ("-".equals(top)) {
            int barx = x + boxSize / 2;
            drawBar(
                canvas,
                barx, y, barx, y + dashSize,
                box, Box.Bar.SOLID
            );
        } else if (top != null) {
            // draw like marks
            drawSeparator(
                canvas,
                top,
                x,
                y + height / 2,
                boxSize,
                Layout.Alignment.ALIGN_CENTER,
                text
            );
        }

        String bottom = separators.getBottom();
        if (isWhitespace(bottom)) {
            // dashed bar
            int bary = y + boxSize - barOffset;
            drawBar(
                canvas,
                x, bary, x + boxSize, bary,
                box, Box.Bar.DOTTED
            );
        } else if ("-".equals(bottom)) {
            int barx = x + boxSize / 2;
            int bary = y + boxSize;
            drawBar(
                canvas,
                barx, bary - dashSize, barx, bary,
                box, Box.Bar.SOLID
            );
        } else if (bottom != null) {
            drawSeparator(
                canvas,
                bottom,
                x,
                y + boxSize - height / 2,
                boxSize,
                Layout.Alignment.ALIGN_CENTER,
                text
            );
        }

        String left = separators.getLeft();
        if (isWhitespace(left)) {
            // dashed bar
            int barx = x + barOffset;
            drawBar(
                canvas,
                barx, y, barx, y + boxSize,
                box, Box.Bar.DOTTED
            );
        } else if ("-".equals(left)) {
            int bary = y + boxSize / 2;
            drawBar(
                canvas,
                x, bary, x + dashSize, bary,
                box, Box.Bar.SOLID
            );
        } else if (left != null) {
            drawSeparator(
                canvas,
                String.valueOf(left),
                x,
                y + boxSize / 2,
                boxSize,
                Layout.Alignment.ALIGN_NORMAL,
                text
            );
        }

        String right = separators.getRight();
        if (isWhitespace(right)) {
            // dashed bar
            int barx = x + boxSize - barOffset;
            drawBar(
                canvas,
                barx, y, barx, y + boxSize,
                box, Box.Bar.DOTTED
            );
        } else if ("-".equals(right)) {
            int barx = x + boxSize;
            int bary = y + boxSize / 2;
            drawBar(
                canvas,
                barx - dashSize, bary, barx, bary,
                box, Box.Bar.SOLID
            );
        } else if (right != null) {
            drawSeparator(
                canvas,
                right,
                x,
                y + boxSize / 2,
                boxSize,
                Layout.Alignment.ALIGN_OPPOSITE,
                text
            );
        }
    }

    private void drawBoxShape(
        Canvas canvas, int x, int y, Box box, boolean inCurrentWord
    ) {
        if (!box.hasShape())
            return;

        int boxSize = getBoxSize();
        PaintProfile profile = getProfile();
        Paint paint = profile.getShape(box, inCurrentWord);
        int off = profile.getShapeStrokeWidth();

        Path path;

        switch(box.getShape()) {
        case CIRCLE:
            canvas.drawCircle(
                x + boxSize / 2, y + boxSize / 2, boxSize / 2 - off, paint
            );
            break;
        case ARROW_LEFT:
            path = new Path();
            path.moveTo(x + boxSize - off, y + boxSize / 2);
            path.lineTo(x + off, y + boxSize / 2);
            path.lineTo(x + boxSize / 4, y + boxSize / 4);
            path.moveTo(x + off, y + boxSize / 2);
            path.lineTo(x + boxSize / 4, y + 3 * boxSize / 4);
            canvas.drawPath(path, paint);
            break;
        case ARROW_RIGHT:
            path = new Path();
            path.moveTo(x + off, y + boxSize / 2);
            path.lineTo(x + boxSize - off, y + boxSize / 2);
            path.lineTo(x + 3 * boxSize / 4, y + boxSize / 4);
            path.moveTo(x + boxSize - off, y + boxSize / 2);
            path.lineTo(x + 3 * boxSize / 4, y + 3 * boxSize / 4);
            canvas.drawPath(path, paint);
            break;
        case ARROW_UP:
            path = new Path();
            path.moveTo(x + boxSize / 2, y + boxSize - off);
            path.lineTo(x + boxSize / 2, y + off);
            path.lineTo(x + boxSize / 4, y + boxSize / 4);
            path.moveTo(x + boxSize / 2, y + off);
            path.lineTo(x + 3 * boxSize / 4, y + boxSize / 4);
            canvas.drawPath(path, paint);
            break;
        case ARROW_DOWN:
            path = new Path();
            path.moveTo(x + boxSize / 2, y + off);
            path.lineTo(x + boxSize / 2, y + boxSize - off);
            path.lineTo(x + boxSize / 4, y + 3 * boxSize / 4);
            path.moveTo(x + boxSize / 2, y + boxSize - off);
            path.lineTo(x + 3 * boxSize / 4, y + 3 * boxSize / 4);
            canvas.drawPath(path, paint);
            break;
        case TRIANGLE_LEFT:
            path = new Path();
            path.moveTo(x + boxSize - off, y + boxSize / 2);
            path.lineTo(x + off, y + off);
            path.lineTo(x + off, y + boxSize - off);
            path.close();
            canvas.drawPath(path, paint);
            break;
        case TRIANGLE_RIGHT:
            path = new Path();
            path.moveTo(x + off, y + boxSize / 2);
            path.lineTo(x + boxSize - off, y + off);
            path.lineTo(x + boxSize - off, y + boxSize - off);
            path.close();
            canvas.drawPath(path, paint);
            break;
        case TRIANGLE_UP:
            path = new Path();
            path.moveTo(x + boxSize / 2, y + boxSize - off);
            path.lineTo(x + off, y + off);
            path.lineTo(x + boxSize - off, y + off);
            path.close();
            canvas.drawPath(path, paint);
            break;
        case TRIANGLE_DOWN:
            path = new Path();
            path.moveTo(x + boxSize / 2, y + off);
            path.lineTo(x + off, y + boxSize - off);
            path.lineTo(x + boxSize - off, y + boxSize - off);
            path.close();
            canvas.drawPath(path, paint);
            break;
        case DIAMOND:
            path = new Path();
            path.moveTo(x + boxSize / 2, y + off);
            path.lineTo(x + boxSize - off, y + boxSize / 2);
            path.lineTo(x + boxSize / 2, y + boxSize - off);
            path.lineTo(x + off, y + boxSize / 2);
            path.close();
            canvas.drawPath(path, paint);
            break;
        case CLUB:
            path = new Path();
            path.moveTo(x + boxSize / 2, y + 3 * boxSize / 5);
            path.quadTo(
                x + off, y + boxSize - off,
                x + off, y + boxSize / 2
            );
            path.quadTo(
                x + off, y + boxSize / 5,
                x + 2 * boxSize / 5, y + 2 * boxSize / 5
            );
            path.quadTo(
                x + off, y + off,
                x + boxSize / 2, y + off
            );
            path.quadTo(
                x + boxSize - off, y + off,
                x + 3 * boxSize / 5, y + 2 * boxSize / 5
            );
            path.quadTo(
                x + boxSize - off, y + boxSize / 5,
                x + boxSize - off, y + boxSize / 2
            );
            path.quadTo(
                x + boxSize - off, y + boxSize - off,
                x + boxSize / 2, y + 3 * boxSize / 5
            );
            path.quadTo(
                x + boxSize / 2, y + 4 * boxSize / 5,
                x + 3 * boxSize / 4, y + boxSize - off
            );
            path.lineTo(
                x + boxSize / 4, y + boxSize - off
            );
            path.quadTo(
                x + boxSize / 2, y + 4 * boxSize / 5,
                x + boxSize / 2, y + 3 * boxSize / 5
            );
            canvas.drawPath(path, paint);
            break;
        case HEART:
            path = new Path();
            path.moveTo(x + boxSize / 2, y + boxSize / 4);
            path.cubicTo(
                x + boxSize / 2, y + off,
                x + off, y + off,
                x + off, y + boxSize / 3
            );
            path.cubicTo(
                x + off, y + boxSize / 2,
                x + boxSize / 2, y + 2 * boxSize / 3,
                x + boxSize / 2, y + boxSize - off
            );
            path.cubicTo(
                x + boxSize / 2, y + 2 * boxSize / 3,
                x + boxSize - off, y + boxSize / 2,
                x + boxSize - off, y + boxSize / 3
            );
            path.cubicTo(
                x + boxSize - off, y + off,
                x + boxSize / 2, y + off,
                x + boxSize / 2, y + boxSize / 4
            );
            canvas.drawPath(path, paint);
            break;
        case SPADE:
            path = new Path();
            path.moveTo(x + boxSize / 2, y + 3 * boxSize / 5);
            path.cubicTo(
                x + boxSize / 2, y + 4 * boxSize / 5,
                x + off, y + 4 * boxSize / 5,
                x + off, y + 3 * boxSize / 5
            );
            path.cubicTo(
                x + off, y + boxSize / 2,
                x + boxSize / 2, y + boxSize / 3,
                x + boxSize / 2, y + off
            );
            path.cubicTo(
                x + boxSize / 2, y + boxSize / 3,
                x + boxSize - off, y + boxSize / 2,
                x + boxSize - off, y + 3 * boxSize / 5
            );
            path.cubicTo(
                x + boxSize - off, y + 4 * boxSize / 5,
                x + boxSize / 2, y + 4 * boxSize / 5,
                x + boxSize / 2, y + 3 * boxSize / 5
            );
            path.quadTo(
                x + boxSize / 2, y + 4 * boxSize / 5,
                x + 2 * boxSize / 3, y + boxSize - off
            );
            path.lineTo(x + boxSize / 3, y + boxSize - off);
            path.quadTo(
                x + boxSize / 2, y + 4 * boxSize / 5,
                x + boxSize / 2, y + 3 * boxSize / 5
            );
            canvas.drawPath(path, paint);
            break;
        case STAR:
            path = new Path();
            path.moveTo(x + off, y + 2 * boxSize / 5);
            path.lineTo(x + 2 * boxSize / 5, y + 2 * boxSize / 5);
            path.lineTo(x + boxSize / 2, y + off);
            path.lineTo(x + 3 * boxSize / 5, y + 2 * boxSize / 5);
            path.lineTo(x + boxSize - off, y + 2 * boxSize / 5);
            path.lineTo(x + 7 * boxSize / 10, y + 3 * boxSize / 5);
            path.lineTo(x + 4 * boxSize / 5, y + boxSize - off);
            path.lineTo(x + boxSize / 2, y + 7 * boxSize / 10);
            path.lineTo(x + boxSize / 5, y + boxSize - off);
            path.lineTo(x + 3 * boxSize / 10, y + 3 * boxSize / 5);
            path.close();
            canvas.drawPath(path, paint);
            break;
        case SQUARE:
            canvas.drawRect(
                x + off, y + off, x + boxSize - off, y + boxSize - off, paint
            );
            break;
        case RHOMBUS:
            path = new Path();
            path.moveTo(x + 2 * boxSize / 5, y + off);
            path.lineTo(x + boxSize - off, y + off);
            path.lineTo(x + 3 * boxSize / 5, y + boxSize - off);
            path.lineTo(x + off, y + boxSize - off);
            path.close();
            canvas.drawPath(path, paint);
            break;
        case FORWARD_SLASH:
            canvas.drawLine(
                x + off, y + off, x + boxSize - off, y + boxSize - off, paint
            );
            break;
        case BACK_SLASH:
            canvas.drawLine(
                x + off, y + boxSize - off, x + boxSize - off, y + off, paint
            );
            break;
        case X:
            canvas.drawLine(
                x + off, y + off, x + boxSize - off, y + boxSize - off, paint
            );
            canvas.drawLine(
                x + off, y + boxSize - off, x + boxSize - off, y + off, paint
            );
            break;
        }
    }

    private void drawBoxLetter(
        Canvas canvas, int x, int y,
        Box box, int row, int col,
        boolean isHighlighted, boolean inCurrentWord
    ) {
        PaintProfile profile = getProfile();
        int boxSize = profile.getBoxSize();
        int textOffset = profile.getTextOffset();
        TextPaint thisLetter = profile.getLetterText(box, inCurrentWord);

        String letterString = box.isBlank()
            ? null
            : box.getResponse();

        if (letterString == null)
            return;

        if (highlightError(box, isHighlighted)) {
            if (isHighlighted) {
                thisLetter = profile.getColor();
            } else if (inCurrentWord) {
                thisLetter = profile.getErrorHighlight();
            }
        }

        if (letterString.length() > 1) {
            thisLetter = getIdealTextSize(letterString, thisLetter, boxSize);
        }

        int yoffset
            = boxSize - textOffset - getTotalHeight(thisLetter);
        drawText(
            canvas,
            letterString,
            x + (boxSize / 2),
            y + yoffset,
            boxSize,
            thisLetter
        );
    }

    /** Draw the notes for a box
     *
     * @param shadow a character to display as a note if there is nothing
     * else in the box or null if none
     */
    private void drawBoxNotes(
        Canvas canvas, int x, int y, Box box,
        String shadow, boolean inCurrentWord, Set<String> suppressNotesLists
    ) {
        PaintProfile profile = getProfile();
        int boxSize = profile.getBoxSize();
        int textOffset = profile.getTextOffset();
        TextPaint noteText = profile.getNoteText(box, inCurrentWord);
        TextPaint miniNoteText = profile.getMiniNoteText(box, inCurrentWord);
        TextPaint letterText = profile.getLetterText(box, inCurrentWord);

        String noteStringAcross = null;
        String noteStringDown = null;

        for (ClueID cid : box.getIsPartOfClues()) {
            if (suppressNotesLists.contains(cid.getListName()))
                continue;

            Note note = board.getPuzzle().getNote(cid);
            if (note == null)
                continue;

            String scratch = note.getScratch();
            if (scratch == null)
                continue;

            int pos = box.getCluePosition(cid);
            if (pos < 0 || pos >= scratch.length())
                continue;

            char noteChar = scratch.charAt(pos);
            if (noteChar == ' ')
                continue;

            if (isClueProbablyAcross(cid)) {
                noteStringAcross =
                    Character.toString(noteChar);
            } else {
                noteStringDown =
                    Character.toString(noteChar);
            }
        }

        if (noteStringAcross == null && noteStringDown == null) {
            noteStringAcross = shadow;
        }

        float[] mWidth = new float[1];
        letterText.getTextWidths("M", mWidth);
        float letterTextHalfWidth = mWidth[0] / 2;

        if (noteStringAcross != null && noteStringDown != null) {
            if (noteStringAcross.equals(noteStringDown)) {
                // Same scratch letter in both directions
                // Align letter with across and down answers
                int noteTextHeight = getTotalHeight(noteText);
                drawText(
                    canvas,
                    noteStringAcross,
                    x + (int)(boxSize - letterTextHalfWidth),
                    y + boxSize - noteTextHeight - textOffset,
                    boxSize,
                    noteText
                );
            } else {
                // Conflicting scratch letters
                // Display both letters side by side
                int noteTextHeight = getTotalHeight(miniNoteText);
                drawText(
                    canvas,
                    noteStringAcross,
                    x + (int)(boxSize * 0.05 + letterTextHalfWidth),
                    y + boxSize - noteTextHeight - textOffset,
                    boxSize,
                    miniNoteText
                );
                int yoffset =
                    boxSize
                    - noteTextHeight
                    + (int) miniNoteText.ascent();
                drawText(
                    canvas,
                    noteStringDown,
                    x + (int)(boxSize - letterTextHalfWidth),
                    y + yoffset,
                    boxSize,
                    miniNoteText
                );
            }
        } else if (noteStringAcross != null) {
            // Across scratch letter only - display in bottom left
            int noteTextHeight = getTotalHeight(noteText);
            drawText(
                canvas,
                noteStringAcross,
                x + (boxSize / 2),
                y + boxSize - noteTextHeight - textOffset,
                boxSize,
                noteText
            );
        } else if (noteStringDown != null) {
            // Down scratch letter only - display in bottom left
            int noteTextHeight = getTotalHeight(noteText);
            drawText(
                canvas,
                noteStringDown,
                x + (int)(boxSize - letterTextHalfWidth),
                y + boxSize - noteTextHeight - textOffset,
                boxSize,
                noteText
            );
        }
    }

    /**
     * Estimate general direction of clue
     *
     * Bias towards across if unsure
     */
    private boolean isClueProbablyAcross(ClueID cid) {
        Puzzle puz = board == null ? null : board.getPuzzle();
        if (puz == null)
            return true;

        Clue clue = puz.getClue(cid);
        Zone zone = (clue == null) ? null : clue.getZone();
        if (zone == null || zone.size() <= 1)
            return true;

        Position pos0 = zone.getPosition(0);
        Position pos1 = zone.getPosition(1);

        return pos1.getCol() > pos0.getCol();
    }

    private boolean highlightError(Box box, boolean hasCursor) {
        if (board == null)
            return false;

        if (box.isBlank() || !box.hasSolution())
            return false;

        boolean correct = Objects.equals(box.getSolution(), box.getResponse());
        if (correct)
            return false;

        // it's wrong, so when do we highlight?
        if (board.isShowErrorsGrid()) {
            return true;
        } else if (board.isShowErrorsCursor() && hasCursor) {
            return true;
        } else if (board.isShowErrorsClue()) {
            ClueID cid = board.getClueID();
            return box.getIsPartOfClues().contains(cid)
                && board.isFilledClueID(cid);
        } else {
            return false;
        }
    }

    private boolean isPartOfCurrentClue(Position pos) {
        ClueID cid = board == null ? null : board.getClueID();
        return cid != null && isPartOfClue(pos, cid);
    }

    private boolean isPartOfClue(Position pos, ClueID cid) {
        if (pos == null || board == null || cid == null)
            return false;

        Box box = board.getPuzzle().checkedGetBox(pos);
        if (box == null)
            return false;

        return box.getIsPartOfClues().contains(cid);
    }

    private void drawText(
        Canvas canvas,
        CharSequence text,
        int x, int  y, int width,
        TextPaint style
    ) {
        drawText(
            canvas, text, x, y, width, Layout.Alignment.ALIGN_NORMAL, style
        );
    }

    private void drawText(
        Canvas canvas,
        CharSequence text,
        int x, int  y, int width, Layout.Alignment align,
        TextPaint style
    ) {
        // with some help from:
        // https://stackoverflow.com/a/41870464
        StaticLayout staticLayout
            = versionUtils.getStaticLayout(text, style, width, align);
        canvas.save();
        canvas.translate(x, y);
        staticLayout.draw(canvas);
        canvas.restore();
    }

    /**
     * Draw a separator at x, y
     *
     * Scales to fill full height of separator text style and tries to
     * center glyph vertically
     */
    private void drawSeparator(
        Canvas canvas,
        String text,
        int x, int y, int width, Layout.Alignment align,
        TextPaint style
    ) {
        float targetHeight = -style.ascent();

        // work out bounds to scale
        Rect rect = new Rect();
        style.getTextBounds(text, 0, text.length(), rect);
        int baseHeight = rect.height();
        // from baseline
        int centerOffset = (rect.top + rect.bottom) / 2;

        float scale = SEPARATOR_FILL_PCNT * targetHeight / baseHeight;

        int fullSize = (int) (scale * style.getTextSize());
        TextPaint fullStyle = new TextPaint(style);
        fullStyle.setTextSize(fullSize);
        int fullCenterOffset = (int) (scale * centerOffset);

        StaticLayout staticLayout
            = versionUtils.getStaticLayout(text, fullStyle, width, align);

        canvas.save();
        canvas.translate(
            x,
            // this is a fudge :/
            y - fullCenterOffset + fullStyle.ascent() - fullStyle.descent() / 2
        );
        staticLayout.draw(canvas);
        canvas.restore();
    }

    /**
     * Calculate text size to avoid overflow
     *
     * See how much space it would be, recommend a smaller version if
     * needed.
     *
     * Returns the text paint with the right size, may or may not be the
     * original style passed
     */
    private static TextPaint getIdealTextSize(
        CharSequence text, TextPaint style, int width
    ) {
        float desiredWidth = StaticLayout.getDesiredWidth(text, style);
        float styleSize = style.getTextSize();
        if (desiredWidth > width) {
            // -1 needed else on rare occasions overruns lines
            int newSize = (int) ((width / desiredWidth) * styleSize) - 1;
            TextPaint newStyle = new TextPaint(style);
            newStyle.setTextSize(newSize);
            return newStyle;
        } else {
            return style;
        }
    }

    private void drawHtmlText(
        Canvas canvas, String text, int x, int y, int width, TextPaint style
    ) {
        drawText(canvas, HtmlCompat.fromHtml(text, 0), x, y, width, style);
    }

    private void drawHtmlText(
        Canvas canvas, String text,
        int x, int y, int width, Layout.Alignment align,
        TextPaint style
    ) {
        drawText(
            canvas, HtmlCompat.fromHtml(text, 0), x, y, width, align, style
        );
    }

    private int getTotalHeight(TextPaint style) {
        return (int) Math.ceil(
            - style.ascent()
            + DESCENT_FUDGE_FACTOR * style.descent()
        );
    }

    /**
     * Draws the images
     *
     * @param canvases as drawBoardBoxes
     * @param drawAll if complete redraw
     */
    private void drawImages(TileCanvas[][] canvases, boolean drawAll) {
        Puzzle puz = (board == null) ? null : board.getPuzzle();
        if (puz == null || bitmapGrid == null)
            return;

        int boxSize = getBoxSize();

        for (PuzImage image : puz.getImages()) {
            Object tag = image.getTag();
            if (tag == null || !(tag instanceof Bitmap))
                tagImageWithBitmap(image);

            tag = image.getTag();
            if (tag instanceof Bitmap) {
                Bitmap bmp = (Bitmap) tag;
                int startx = image.getCol() * boxSize;
                int starty = image.getRow() * boxSize;
                int endx = startx + image.getWidth() * boxSize;
                int endy = starty + image.getHeight() * boxSize;

                // draw bitmap on each tile it covers
                forTiles(canvases, startx, starty, endx, endy, tileCanvas -> {
                    if (
                        tileCanvas != null
                        && (drawAll || tileCanvas.isNew())
                    ) {
                        tileCanvas.canvas().drawBitmap(
                            bmp,
                            null,
                            new Rect(startx, starty, endx, endy),
                            null
                        );
                    }
                });
            }
        }
    }

    /**
     * Call callback for each tile in pixel rect
     *
     * Passed back TileCanvas may be null
     */
    private void forTiles(
        TileCanvas[][] canvases,
        int startx, int starty, int endx, int endy,
        Consumer<TileCanvas> cb
    ) {
        Position tileStart
            = bitmapGrid.getPixelPosition(startx, starty);
        Position tileEnd
            = bitmapGrid.getPixelPosition(endx, endy);

        int startRow = tileStart == null ? 0 : tileStart.getRow();
        int startCol = tileStart == null ? 0 : tileStart.getCol();
        int endRow = tileEnd == null
            ? bitmapGrid.getNumRows()
            : tileEnd.getRow();
        int endCol = tileEnd == null
            ? bitmapGrid.getNumCols()
            : tileEnd.getCol();

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                cb.accept(getCanvasByTilePosition(canvases, row, col));
            }
        }

    }

    private void tagImageWithBitmap(PuzImage image) {
        String url = image.getURL();
        if (url == null || url.length() < 5)
            return;

        if (url.substring(0, 5).equalsIgnoreCase("data:")) {
            int start = url.indexOf(",") + 1;
            if (start > 0) {
                byte[] data = Base64.decode(
                    url.substring(start), Base64.DEFAULT
                );
                Bitmap imgBmp
                    = BitmapFactory.decodeByteArray(data, 0, data.length);
                image.setTag(imgBmp);
            }
        }
    }

    /**
     * Refresh the pinned clue (or draw)
     *
     * Refresh parts in current or reset word, unless renderAll.
     *
     * @param canvases canvas matrix matching bitmapGrid tiles with a
     * non-null TileCanvas if the tile should be drawn. New canvases
     * drawn in full, else just changes.
     * @param changes the positions that have changed (can be null for
     * all)
     * @param suppressNotesLists as in drawBox
     */
    private void drawPinnedClue(
        TileCanvas[][] canvases,
        RenderChanges changes,
        Set<String> suppressNotesLists,
        DisplaySeparators displaySeparators,
        boolean inferSeparators
    ) {
        Puzzle puz = this.board.getPuzzle();
        Box[][] boxes = this.board.getBoxes();
        int boxSize = getBoxSize();
        Position highlight = board.getHighlightLetter();

        if (!puz.hasPinnedClueID())
            return;

        Zone pinnedZone = getPinnedZone();
        if (pinnedZone == null)
            return;

        Word currentWord = this.board.getCurrentWord();

        int pinnedRow = getPinnedRow();
        int pinnedCol = getPinnedCol();

        int y =  pinnedRow * boxSize;

        for (int i = 0; i < pinnedZone.size(); i++) {
            int x = (pinnedCol + i) * boxSize;

            TileCanvas tileCanvas = getCanvasByPixel(canvases, x, y);
            if (tileCanvas == null)
                continue;

            Position pos = pinnedZone.getPosition(i);
            if (!tileCanvas.isNew() && !isRenderPos(pos, changes))
                continue;

            BoxSeparators separators = getBoxSeparators(
                puz.getPinnedClueID(), inferSeparators, i
            );

            int row = pos.getRow();
            int col = pos.getCol();

            this.drawBox(
                canvases,
                x, y, row, col,
                boxes[row][col],
                null, currentWord, highlight,
                suppressNotesLists,
                true,
                separators
            );
        }

        // draw highlight outline again as it will have been overpainted
        if (highlight != null) {
            PaintProfile profile = getProfile();
            int idx = pinnedZone.indexOf(highlight);
            if (idx > -1) {
                int x = (pinnedCol + idx) * boxSize;
                TileCanvas tileCanvas = getCanvasByPixel(canvases, x, y);
                if (tileCanvas != null) {
                    drawBoxOutline(
                        tileCanvas.canvas(),
                        x, y,
                        profile.getCurrentLetterBox()
                    );
                }
            }
        }
    }

    /**
     * Find the canvas to use for the given pixel
     *
     * Assuming canvases is the same shape tile-wise as bitmapGrid.
     *
     * Returns null if not found, no bitmapGrid, &c.
     */
    private TileCanvas getCanvasByPixel(TileCanvas[][] canvases, int x, int y) {
        if (bitmapGrid == null || canvases == null)
            return null;

        Position tilePos = bitmapGrid.getPixelPosition(x, y);
        if (tilePos == null)
            return null;

        int row = tilePos.getRow();
        int col = tilePos.getCol();

        if (row < 0 || row >= canvases.length)
            return null;

        if (col < 0 || col >= canvases[row].length)
            return null;

        return getCanvasByTilePosition(canvases, row, col);
    }

    /**
     * Get the canvas by row/col in tile grid
     */
    private TileCanvas getCanvasByTilePosition(
        TileCanvas[][] canvases, int row, int col
    ) {
        if (canvases == null)
            return null;

        if (row < 0 || row >= canvases.length)
            return null;

        if (col < 0 || col >= canvases[row].length)
            return null;

        return canvases[row][col];
    }
    /**
     * Row on which pinned word is rendered
     *
     * Or -1 if nothing pinned
     */
    private int getPinnedRow() {
        Puzzle puz = this.board.getPuzzle();
        return puz.hasPinnedClueID() ? puz.getHeight() + 1 : -1;
    }

    /**
     * Col of first box of pinned word
     *
     * Or -1 if nothing pinned
     */
    private int getPinnedCol() {
        Zone pinnedZone = getPinnedZone();
        return pinnedZone == null
            ? -1
            : (getFullWidth() - pinnedZone.size()) / 2;
    }

    /**
     * Make sure bitmap field has a bitmap
     *
     * @return true if a new (blank) bitmap created, else old one used
     */
    private boolean initialiseBitmap(int width, int height) {
        if (bitmapGrid != null)
            return false;

        int boxSize = getBoxSize();
        int fullWidth = width * boxSize;
        int fullHeight = height * boxSize;

        int boxesPerTile = Math.max(1, MAX_BITMAP_DIM / boxSize);
        int tileSize = boxesPerTile * boxSize;

        bitmapGrid = new BitmapGrid(fullWidth, fullHeight, tileSize);

        return true;
    }

    private int getFullWidth() {
        Puzzle puz = this.board.getPuzzle();
        int width = puz.getWidth();

        if (puz.hasPinnedClueID()) {
            Zone pinnedZone = getPinnedZone();
            if (pinnedZone != null)
                width = Math.max(width, pinnedZone.size());
        }

        return width;
    }

    private int getFullHeight() {
        Puzzle puz = this.board.getPuzzle();
        int height = puz.getHeight();

        if (puz.hasPinnedClueID())
            height += 2;

        return height;
    }

    /**
     * Refresh board on canvas or draw all
     *
     * @param canvases canvas matrix matching bitmapGrid tiles with a
     * non-null TileCanvas if the tile should be drawn. New canvases
     * drawn in full, else just changes.
     * @param changes the changes or null for redraw all
     * @param suppressNotesLists the notes lists not to draw (null means
     * draw none, empty means draw all)
     * @param displaySeparators whether to draw the separators
     * @param inferSeparators whether to infer separators
     */
    private void drawBoardBoxes(
        TileCanvas[][] canvases,
        RenderChanges changes,
        Set<String> suppressNotesLists,
        DisplaySeparators displaySeparators,
        boolean inferSeparators
    ) {
        Puzzle puz = board.getPuzzle();
        Box[][] boxes = board.getBoxes();
        int boxSize = getBoxSize();
        int width = puz.getWidth();
        int height = puz.getHeight();
        Position highlight = board.getHighlightLetter();
        Word currentWord = this.board.getCurrentWord();

        // just have one object for some efficiency
        Position pos = new Position(0, 0);

        boolean showErrorsClue = board.isShowErrorsClue();

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int x = col * boxSize;
                int y = row * boxSize;

                TileCanvas tileCanvas = getCanvasByPixel(canvases, x, y);
                if (tileCanvas == null)
                    continue;

                pos.setRow(row);
                pos.setCol(col);
                if (!tileCanvas.isNew() && !isRenderPos(pos, changes))
                    continue;

                BoxSeparators separators = getBoxSeparators(
                    displaySeparators, inferSeparators, pos, currentWord
                );

                this.drawBox(
                    canvases,
                    x, y, row, col,
                    boxes[row][col],
                    null, currentWord, highlight,
                    suppressNotesLists,
                    true,
                    separators
                );
            }
        }

        // draw highlight outline again as it will have been overpainted
        if (highlight != null) {
            PaintProfile profile = getProfile();
            int curX = highlight.getCol() * boxSize;
            int curY = highlight.getRow() * boxSize;
            drawBoxOutline(
                canvases, curX, curY, profile.getCurrentLetterBox()
            );
        }
    }

    private Zone getPinnedZone() {
        Puzzle puz = this.board.getPuzzle();
        Clue pinnedClue = puz.getClue(puz.getPinnedClueID());
        return pinnedClue == null
            ? null
            : pinnedClue.getZone();
    }

    /**
     * The size of a box in pixels according to current scale
     */
    private int getBoxSize() {
        return getProfile().getBoxSize();
    }

    /**
     * Metrics and paint objects for drawing
     */
    private PaintProfile getProfile() {
        return profile;
    }

    /**
     * True if the position needs rendering
     *
     * Either because it is directly listed as a change, or a change implies
     * this one may have changed (e.g. because of show errors clue, the
     * completion of the clue can cause a change).
     */
    private boolean isRenderPos(Position pos, RenderChanges changes) {
        if (board == null)
            return false;

        if (changes == null)
            return true;

        if (changes.changedPositions() == null)
            return true;

        if (changes.changedPositions().contains(pos))
            return true;

        if (board.isShowErrorsClue() && isPartOfCurrentClue(pos))
            return true;

        for (ClueID cid : changes.changedClueIDs()) {
            if (isPartOfClue(pos, cid))
                return true;
        }

        return false;
    }

    /**
     * Convencience for call without a visible rect (draw all)
     */
    private TileCanvas[][] makeBitmapGridCanvases() {
        return makeBitmapGridCanvases(null);
    }

    /**
     * Setup matrix of canvases for drawing on tiles of the bitmapGrid
     *
     * Each tile will contain an exact number of puzzle cells. Tiles
     * outside of the visible rect will be set to null so they are not
     * drawn. Tiles will be marked as new if they were not already in
     * the bitmapGrid.
     *
     * bitmapGrid will also be updated -- new bitmaps will be created if
     * the tile is newly visible. Old bitmaps will be deleted if they
     * are now out of view.
     */
    private TileCanvas[][] makeBitmapGridCanvases(Rect visibleRect) {
        if (bitmapGrid == null)
            return null;

        int numRows = bitmapGrid.getNumRows();
        int numCols = bitmapGrid.getNumCols();

        TileCanvas[][] canvases = new TileCanvas[numRows][numCols];

        int offsetX = 0;
        int offsetY = 0;

        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                BitmapGrid.Tile tile = bitmapGrid.getTile(row, col);
                Rect tileRect = visibleRect != null
                    ? bitmapGrid.getTileRect(row, col)
                    : null;

                // never null
                if (tile != null) {
                    boolean showTile = visibleRect == null
                        || tileRect == null
                        || Rect.intersects(visibleRect, tileRect);

                    if (showTile) {
                        boolean newBitmap = !tile.hasBitmap();
                        Canvas canvas = new Canvas(tile.getCreateBitmap());
                        canvas.translate(-offsetX, -offsetY);
                        canvases[row][col] = new TileCanvas(canvas, newBitmap);
                    } else {
                        canvases[row][col] = null;
                        tile.destroyBitmap();
                    }

                    offsetX += tile.getWidth();
                    if (col == numCols - 1)
                        offsetY += tile.getHeight();
                }
            }
            offsetX = 0;
        }

        return canvases;
    }

    /**
     * Box size at given scale
     */
    private int calcBoxSize(float scale) {
        int boxSize = (int) (BASE_BOX_SIZE_INCHES * dpi * scale);
        if (boxSize == 0) {
            boxSize = (int) (BASE_BOX_SIZE_INCHES * dpi * 0.25F);
        }
        return boxSize;
    }

    /**
     * Is not null and not whitespace
     */
    private boolean isWhitespace(String s) {
        return (s == null) ? false : WHITESPACE_RE.matcher(s).matches();
    }

    private class PaintProfile {
        // box colours also can be used for text for "transparent" text
        // (e.g. highlighted error cells)
        private final TextPaint cellBox = new TextPaint();
        private final TextPaint blockBox = new TextPaint();
        private final Paint shape = new Paint();
        private final Paint blockShape = new Paint();
        private final Paint outline = new Paint();
        private final Paint bar = new Paint();
        private final Paint barDashed = new Paint();
        private final Paint barDotted = new Paint();
        private final Paint cheated = new Paint();
        private final Paint currentLetterBox = new Paint();
        private final Paint currentLetterHighlight = new Paint();
        private final Paint currentWordHighlight = new Paint();
        private final TextPaint letterText = new TextPaint();
        private final TextPaint blockLetterText = new TextPaint();
        private final TextPaint numberText = new TextPaint();
        private final TextPaint blockNumberText = new TextPaint();
        private final TextPaint separatorText = new TextPaint();
        private final TextPaint noteText = new TextPaint();
        private final TextPaint blockNoteText = new TextPaint();
        private final TextPaint miniNoteText = new TextPaint();
        private final TextPaint blockMiniNoteText = new TextPaint();
        private final Paint error = new Paint();
        private final TextPaint errorHighlight = new TextPaint();
        private final Paint flag = new Paint();
        private final TextPaint onBlock = new TextPaint();

        private int boxSize;
        private int numberTextSize = boxSize / 4;
        private int separatorTextSize = boxSize / 4;
        private int miniNoteTextSize = boxSize / 2;
        private int noteTextSize = Math.round(boxSize * 0.6F);
        private int letterTextSize = Math.round(boxSize * 0.7F);
        private int barSize = boxSize / 12;
        private int numberOffset = barSize;
        private int textOffset = boxSize / 30;
        private int shapeStrokeWidth = Math.max(1, boxSize / 15);

        public PaintProfile(Context context, float scale) {
            int blockColor
                = ContextCompat.getColor(context, R.color.blockColor);
            int cellColor = ContextCompat.getColor(context, R.color.cellColor);
            int currentWordHighlightColor = ContextCompat.getColor(
                context, R.color.currentWordHighlightColor
            );
            int currentLetterHighlightColor = ContextCompat.getColor(
                context, R.color.currentLetterHighlightColor
            );
            int errorColor
                = ContextCompat.getColor(context, R.color.errorColor);
            int errorHighlightColor
                = ContextCompat.getColor(context, R.color.errorHighlightColor);
            int cheatedColor
                = ContextCompat.getColor(context, R.color.cheatedColor);
            int boardLetterColor
                = ContextCompat.getColor(context, R.color.boardLetterColor);
            int boardNoteColor
                = ContextCompat.getColor(context, R.color.boardNoteColor);
            int flagColor = ContextCompat.getColor(context, R.color.flagColor);
            int onBlockColor
                = ContextCompat.getColor(context, R.color.onBlockColor);
            int boardShapeColor
                = ContextCompat.getColor(context, R.color.boardShapeColor);
            int blockShapeColor
                = ContextCompat.getColor(context, R.color.blockShapeColor);

            outline.setColor(blockColor);
            outline.setStrokeWidth(2.0F);

            // line styles set in scale
            bar.setColor(blockColor);
            bar.setStyle(Style.STROKE);
            barDashed.setColor(blockColor);
            barDashed.setStyle(Style.STROKE);
            barDotted.setColor(blockColor);
            barDotted.setStyle(Style.STROKE);

            numberText.setTextAlign(Align.LEFT);
            numberText.setColor(boardLetterColor);
            numberText.setAntiAlias(true);
            numberText.setTypeface(Typeface.MONOSPACE);

            blockNumberText.setTextAlign(Align.LEFT);
            blockNumberText.setColor(onBlockColor);
            blockNumberText.setAntiAlias(true);
            blockNumberText.setTypeface(Typeface.MONOSPACE);

            separatorText.setTextAlign(Align.LEFT);
            separatorText.setColor(blockColor);
            separatorText.setAntiAlias(true);
            separatorText.setTypeface(Typeface.MONOSPACE);
            separatorText.setFakeBoldText(true);

            noteText.setTextAlign(Align.CENTER);
            noteText.setColor(boardNoteColor);
            noteText.setAntiAlias(true);
            noteText.setTypeface(TYPEFACE_SEMI_BOLD_SANS);

            blockNoteText.setTextAlign(Align.CENTER);
            blockNoteText.setColor(onBlockColor);
            blockNoteText.setAntiAlias(true);
            blockNoteText.setTypeface(TYPEFACE_SEMI_BOLD_SANS);

            miniNoteText.setTextAlign(Align.CENTER);
            miniNoteText.setColor(boardNoteColor);
            miniNoteText.setAntiAlias(true);
            miniNoteText.setTypeface(TYPEFACE_SEMI_BOLD_SANS);

            blockMiniNoteText.setTextAlign(Align.CENTER);
            blockMiniNoteText.setColor(onBlockColor);
            blockMiniNoteText.setAntiAlias(true);
            blockMiniNoteText.setTypeface(TYPEFACE_SEMI_BOLD_SANS);

            letterText.setTextAlign(Align.CENTER);
            letterText.setColor(boardLetterColor);
            letterText.setAntiAlias(true);
            letterText.setTypeface(Typeface.SANS_SERIF);

            blockLetterText.setTextAlign(Align.CENTER);
            blockLetterText.setColor(onBlockColor);
            blockLetterText.setAntiAlias(true);
            blockLetterText.setTypeface(Typeface.SANS_SERIF);

            shape.setColor(boardShapeColor);
            shape.setAntiAlias(true);
            shape.setStyle(Style.STROKE);
            shape.setStrokeJoin(Paint.Join.ROUND);

            blockShape.setColor(blockShapeColor);
            blockShape.setAntiAlias(true);
            blockShape.setStyle(Style.STROKE);
            blockShape.setStrokeJoin(Paint.Join.ROUND);

            currentWordHighlight.setColor(currentWordHighlightColor);
            currentLetterHighlight.setColor(currentLetterHighlightColor);
            currentLetterBox.setColor(cellColor);
            currentLetterBox.setStrokeWidth(2.0F);

            error.setTextAlign(Align.CENTER);
            error.setColor(errorColor);
            error.setAntiAlias(true);
            error.setTypeface(Typeface.SANS_SERIF);

            errorHighlight.setTextAlign(Align.CENTER);
            errorHighlight.setColor(errorHighlightColor);
            errorHighlight.setAntiAlias(true);
            errorHighlight.setTypeface(Typeface.SANS_SERIF);

            blockBox.setColor(blockColor);
            blockBox.setTextAlign(Align.CENTER);
            blockBox.setAntiAlias(true);
            blockBox.setTypeface(Typeface.SANS_SERIF);

            cellBox.setColor(cellColor);
            cellBox.setTextAlign(Align.CENTER);
            cellBox.setAntiAlias(true);
            cellBox.setTypeface(Typeface.SANS_SERIF);

            cheated.setColor(cheatedColor);
            flag.setColor(flagColor);

            setScale(scale);
        }

        public TextPaint getBoxColor(Box box) {
            if (box == null || !box.hasColor())
               return Box.isBlock(box) ? blockBox : cellBox;
            else
               return getRelativePaint(cellBox, box.getColor());
        }

        public TextPaint getColor() {
            return cellBox;
        }

        public Paint getShape(Box box, boolean inCurrentWord) {
            Paint base = Box.isBlock(box) ? blockShape : shape;
            if (box == null || !box.hasTextColor() || inCurrentWord) {
               return base;
            } else {
                Paint mixedPaint = new Paint(base);
                Paint cell = getColor();
                mixedPaint.setColor(getRelativeColor(
                    cell.getColor(), box.getTextColor()
                ));
                return mixedPaint;
            }
        }

        public Paint getOutline(Box box) {
            return outline;
        }

        public Paint getBarColor(Box box, Box.Bar barStyle) {
            Paint base = bar;
            if (barStyle == Box.Bar.DOTTED)
                base = barDotted;
            else if (barStyle == Box.Bar.DASHED)
                base = barDashed;

            if (box == null || !box.hasBarColor()) {
               return base;
            } else {
                Paint mixedPaint = new Paint(base);
                Paint cell = getColor();
                mixedPaint.setColor(getRelativeColor(
                    cell.getColor(), box.getBarColor()
                ));
                return mixedPaint;
            }
        }

        public Paint getCheated() {
            return cheated;
        }

        public Paint getCurrentLetterBox() {
            return currentLetterBox;
        }

        public Paint getCurrentLetterHighlight() {
            return currentLetterHighlight;
        }

        public Paint getCurrentWordHighlight() {
            return currentWordHighlight;
        }

        public TextPaint getLetterText(Box box, boolean inCurrentWord) {
            return mixTextPaint(
                box, letterText, blockLetterText, inCurrentWord
            );
        }

        public TextPaint getNumberText(Box box, boolean inCurrentWord) {
            return mixTextPaint(
                box, numberText, blockNumberText, inCurrentWord
            );
        }

        public TextPaint getSeparatorText() {
            return separatorText;
        }

        public TextPaint getNoteText(Box box, boolean inCurrentWord) {
            return mixTextPaint(box, noteText, blockNoteText, inCurrentWord);
        }

        public TextPaint getMiniNoteText(Box box, boolean inCurrentWord) {
            return mixTextPaint(
                box, miniNoteText, blockMiniNoteText, inCurrentWord
            );
        }

        public Paint getError() {
            return error;
        }

        public TextPaint getErrorHighlight() {
            return errorHighlight;
        }

        public Paint getFlag(Clue clue) {
            if (clue.isDefaultFlagColor()) {
                return flag;
            } else {
                Paint customFlag = new Paint();
                // clue colors have a 0 alpha channel, so fix
                customFlag.setColor(ColorUtils.addAlpha(clue.getFlagColor()));
                return customFlag;
            }
        }

        public Paint getFlag(Box box) {
            if (box.isDefaultFlagColor()) {
                return flag;
            } else {
                Paint customFlag = new Paint();
                // clue colors have a 0 alpha channel, so fix
                customFlag.setColor(ColorUtils.addAlpha(box.getFlagColor()));
                return customFlag;
            }
        }

        public int getBoxSize() { return boxSize; }
        public int getNumberTextSize() { return numberTextSize; }
        public int getSeparatorTextSize() { return separatorTextSize; }
        public int getMiniNoteTextSize() { return miniNoteTextSize; }
        public int getNoteTextSize() { return noteTextSize; }
        public int getLetterTextSize() { return letterTextSize; }
        public int getBarSize() { return barSize; }
        public int getNumberOffset() { return numberOffset; }
        public int getTextOffset() { return textOffset; }
        public int getShapeStrokeWidth() { return shapeStrokeWidth; }
        public int getDashSeparatorSize() { return boxSize / 5; }

        public void setScale(float scale) {
            boxSize = calcBoxSize(scale);
            numberTextSize = boxSize / 4;
            separatorTextSize = boxSize / 4;
            miniNoteTextSize = boxSize / 2;
            noteTextSize = Math.round(boxSize * 0.6F);
            letterTextSize = Math.round(boxSize * 0.7F);
            barSize = boxSize / 12;
            numberOffset = barSize;
            textOffset = boxSize / 30;
            shapeStrokeWidth = Math.max(1, boxSize / 15);

            numberText.setTextSize(numberTextSize);
            blockNumberText.setTextSize(numberTextSize);
            separatorText.setTextSize(separatorTextSize);
            letterText.setTextSize(letterTextSize);
            blockLetterText.setTextSize(letterTextSize);
            cellBox.setTextSize(letterTextSize);
            blockBox.setTextSize(letterTextSize);
            error.setTextSize(letterTextSize);
            errorHighlight.setTextSize(letterTextSize);
            noteText.setTextSize(noteTextSize);
            blockNoteText.setTextSize(noteTextSize);
            miniNoteText.setTextSize(miniNoteTextSize);
            blockMiniNoteText.setTextSize(miniNoteTextSize);

            shape.setStrokeWidth(shapeStrokeWidth);
            blockShape.setStrokeWidth(shapeStrokeWidth);

            bar.setStrokeWidth(barSize);
            barDashed.setStrokeWidth(barSize);
            float dashSize = boxSize / 9.0F;
            barDashed.setPathEffect(new DashPathEffect(
                new float[] { 2 * dashSize, dashSize }, dashSize
            ));
            barDotted.setStrokeWidth(barSize);
            barDotted.setPathEffect(new DashPathEffect(
                new float[] { barSize, barSize}, (barSize / 2)
            ));
        }

        private TextPaint mixTextPaint(
            Box box, TextPaint cellBase, TextPaint blockBase,
            boolean inCurrentWord
        ) {
            TextPaint base = Box.isBlock(box) ? blockBase : cellBase;
            if (box == null || !box.hasTextColor() || inCurrentWord) {
               return base;
            } else {
                TextPaint mixedPaint = new TextPaint(base);
                Paint cell = getColor();
                mixedPaint.setColor(getRelativeColor(
                    cell.getColor(), box.getTextColor()
                ));
                return mixedPaint;
            }
        }

        /**
         * Return a new paint based on color
         *
         * For use when "inverting" a color to appear on the board. Relative
         * vs. a pure boxColor background is the pure color. Vs. a pure black
         * background in the inverted color. Somewhere in between is
         * somewhere in between.
         *
         * @param base the standard background color
         * @param color 24-bit 0x00rrggbb "pure" color
         */
        private Paint getRelativePaint(Paint base, int pureColor) {
            Paint mixedPaint = new Paint(base);
            mixedPaint.setColor(getRelativeColor(base.getColor(), pureColor));
            return mixedPaint;
        }

        /**
         * Return a new text paint based on color
         */
        private TextPaint getRelativePaint(TextPaint base, int pureColor) {
            TextPaint mixedPaint = new TextPaint(base);
            mixedPaint.setColor(getRelativeColor(base.getColor(), pureColor));
            return mixedPaint;
        }

        private int getRelativeColor(int baseColor, int pureColor) {
            int mixedR = mixColors(Color.red(baseColor), Color.red(pureColor));
            int mixedG = mixColors(Color.green(baseColor), Color.green(pureColor));
            int mixedB = mixColors(Color.blue(baseColor), Color.blue(pureColor));

            return Color.rgb(mixedR, mixedG, mixedB);
        }

        /**
         * Tint a 0-255 pure color against a base
         *
         * See getRelativePaint
         */
        private int mixColors(int base, int pure) {
            double baseBias = base / 255.0;
            return (int)(
                (baseBias * pure) + ((1- baseBias) * (255 - pure))
            );
        }
    }

    /**
     * Holds a canvas for drawing
     *
     * @param canvas the canvas to draw on, never null
     * @param isNew whether the canvas is for a tile that wasn't drawn
     * before
     */
    private record TileCanvas(
        Canvas canvas,
        boolean isNew
    ) { }
}

