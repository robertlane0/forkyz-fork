package app.crossword.yourealwaysbe.forkyz.view;

import java.util.logging.Logger;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;

public class ScrollingImageView extends FrameLayout implements OnGestureListener {
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

    private static final float BOARD_STICKY_BORDER_MM = 1.5F;

    private AuxTouchHandler aux = null;
    private ClickListener ctxListener;
    private GestureDetector gestureDetector;
    private ScaleListener scaleListener = null;
    private ScrollLocation scaleScrollLocation;
    private boolean longTouched;
    private float maxScale = 1.5f;
    private float minScale = 0.20f;
    private float runningScale = 1.0f;
    private boolean allowOverScroll = true;
    private boolean allowZoom = true;
    private int boardStickyBorder;
    private int gestureStartX;
    private int gestureStartY;
    // count of num requests to block scroll end for internal scroll
    // movements
    private int numBlockScrollEnd = 0;

    private GridLayout imageGrid;
    // the "true" size of each bitmap in the grid in child order of
    // imageGrid
    private ImageSize[] subImageSizes;
    private ImageSize fullImageSize;

    public ScrollingImageView(Context context, AttributeSet as) {
        super(context, as);

        gestureDetector = new GestureDetector(context, this);
        gestureDetector.setIsLongpressEnabled(true);
        imageGrid = new GridLayout(context);

        boardStickyBorder
            = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_MM,
                BOARD_STICKY_BORDER_MM,
                context.getResources().getDisplayMetrics()
            );

        // have some initial presence so that view can have focus from
        // the get-go
        setViewParams(imageGrid, 1, 1);
        addView(imageGrid);

        aux = new MultitouchHandler();
        aux.init(this);
    }

    public int getImageWidth() {
        return imageGrid.getMeasuredWidth();
    }

    public int getImageHeight() {
        return imageGrid.getMeasuredHeight();
    }

    /**
     * Set the bitmap
     *
     * Replaces current image display with the new one. Will resize all
     * views if needed.
     */
    public void setBitmap(BitmapGrid bitmap) {
        presetImageSize(bitmap);
        int numRows = bitmap == null ? 0 : bitmap.getNumRows();
        int numCols = bitmap == null ? 0 : bitmap.getNumCols();
        if (numRows > 0 && numCols > 0) {
            for (int row = 0; row < numRows; row++) {
                for (int col = 0; col < numCols; col++) {
                    BitmapGrid.Tile tile = bitmap.getTile(row, col);
                    if (tile == null)
                        continue;

                    int viewIdx = row * numCols + col;
                    ImageView imageView
                        = (ImageView) imageGrid.getChildAt(viewIdx);
                    if (imageView != null)
                        imageView.setImageBitmap(tile.getBitmap());
                }
            }
        }
    }

    // This method is a hack needed in PlayActivity when clue tabs are
    // shown. The constrain of the board to 1:1 seems to have no effect
    // until render() is called.
    public void relayout() {
        setViewParams(imageGrid, getImageWidth(), getImageHeight(), true);
    }

    public void setContextMenuListener(ClickListener l) {
        this.ctxListener = l;
    }

    public void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    public float getMaxScale() {
        return maxScale;
    }

    public void setMinScale(float minScale) {
        this.minScale = minScale;
    }

    public float getMinScale() {
        return minScale;
    }

    public void setScaleListener(ScaleListener scaleListener) {
        this.scaleListener = scaleListener;
    }

    private float currentScale = 1.0f;

    /**
     * Sets current scale
     *
     * May adjust if necessary, returns final scale
     */
    public float setCurrentScale(float scale){
        this.currentScale = scale;
        return this.currentScale;
    }

    public float getCurrentScale() {
        return currentScale;
    }

    public boolean isVisible(Point p) {
        int currentMinX = this.getScrollX();
        int currentMaxX = this.getMeasuredWidth() + this.getScrollX();
        int currentMinY = this.getScrollY();
        int currentMaxY = this.getMeasuredHeight() + this.getScrollY();

        return (p.x >= currentMinX) && (p.x <= currentMaxX)
            && (p.y >= currentMinY) && (p.y <= currentMaxY);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
            onTouchStart();
            break;
        case MotionEvent.ACTION_UP:
            onTouchEnd();
            break;
        case MotionEvent.ACTION_CANCEL:
            onTouchCancel();
            break;
        }

        if (aux != null)
            aux.onTouchEvent(ev);

        gestureDetector.onTouchEvent(ev);

        return true;
    }

    public void ensureVisible(Point p) {
        // do not scroll before we have dimensions
        if (this.getMeasuredWidth() == 0 || this.getMeasuredHeight() == 0)
            return;

        int x = p.x;
        int y = p.y;

        int currentX = this.getScrollX();
        int currentMaxX = this.getMeasuredWidth() + this.getScrollX();
        int currentY = this.getScrollY();
        int currentMaxY = this.getMeasuredHeight() + this.getScrollY();

        boolean scrolled = false;

        pushBlockScrollEnd();

        if (x < currentX) {
            this.scrollTo(x, currentY);
            scrolled = true;
        } else if (x > currentMaxX) {
            this.scrollTo(x - this.getMeasuredWidth(), currentY);
            scrolled = true;
        }

        if (y < currentY) {
            this.scrollTo(currentX, y);
            scrolled = true;
        } else if (y > currentMaxY) {
            this.scrollTo(currentX, y - this.getMeasuredHeight());
            scrolled = true;
        }

        popBlockScrollEnd();

        if (scrolled)
            scrollEnd();
    }

    public boolean onDown(MotionEvent e) {
        // this should always return true
        return true;
    }

    public boolean onFling(
        MotionEvent e1, MotionEvent e2, float velocityX, float velocityY
    ) {
        return false;
    }

    public void onLongPress(MotionEvent e) {
        if ((aux != null) && aux.inProgress())
            return;

        final Point p = this.resolveToImagePoint(e.getX(), e.getY());

        onContextMenu(p);
    }

    public boolean onScroll(
        MotionEvent e1, MotionEvent e2, float distanceX, float distanceY
    ) {
        if ((aux != null) && aux.inProgress())
            return true;

        this.longTouched = false;

        this.scrollBy((int) distanceX, (int) distanceY);

        return true;
    }

    public void onShowPress(MotionEvent e) {
    }

    public boolean onSingleTapUp(MotionEvent e) {
        if ((aux != null) && aux.inProgress())
            return true;

        Point p = this.resolveToImagePoint(e.getX(), e.getY());

        if (this.longTouched) {
            this.longTouched = false;
        } else {
            onTap(p);
        }

        return true;
    }

    public Point resolveToImagePoint(float x, float y) {
        return this.resolveToImagePoint((int) x, (int) y);
    }

    public Point resolveToImagePoint(int x, int y) {
        return new Point(x + this.getScrollX(), y + this.getScrollY());
    }

    /**
     * Set whether overscroll is allowed
     *
     * Overscroll is when the board is dragged so that an edge of the
     * board pulls away from the corresponding edge of the view. E.g. a
     * gap between the left view edge and the left board edge. Default
     * is yes.
     */
    public void setAllowOverScroll(boolean allowOverScroll) {
        this.allowOverScroll = allowOverScroll;
    }

    public void setAllowZoom(boolean allowZoom) {
        this.allowZoom = allowZoom;
    }

    @Override
    public void scrollBy(int x, int y) {
        int curX = this.getScrollX();
        int curY = this.getScrollY();
        int newX = curX + x;
        int newY = curY + y;

        int screenWidth = this.getMeasuredWidth();
        int screenHeight = this.getMeasuredHeight();
        int boardWidth = this.getImageWidth();
        int boardHeight= this.getImageHeight();

        // don't allow space between right/bot edge of screen and
        // board (careful of negatives, since co-ords are neg)
        // only adjust if we're scrolling up and just stay put if there
        // was already a gap
        int newRight = newX - boardWidth;
        if (x > 0 &&
            -newRight < screenWidth &&
            (!allowOverScroll || -newRight > screenWidth - boardStickyBorder))
            newX = Math.max(-(screenWidth - boardWidth), curX);

        if (x < 0 &&
            -newRight > screenWidth &&
            (!allowOverScroll || -newRight < screenWidth + boardStickyBorder))
            newX = Math.max(-(screenWidth - boardWidth), curX);

        int newBot = newY - boardHeight;
        if (y > 0 &&
            -newBot < screenHeight &&
            (!allowOverScroll || -newBot > screenHeight - boardStickyBorder))
            newY = Math.max(-(screenHeight - boardHeight), curY);

        if (y < 0 &&
            -newBot > screenHeight &&
            (!allowOverScroll || -newBot < screenHeight + boardStickyBorder))
            newY = Math.max(-(screenHeight - boardHeight), curY);

        // don't allow space between left/top edge of screen and board
        // by doing second this is prioritised over bot/right
        // fix even if scrolling down to stop flipping from one edge to
        // the other (i.e. never allow a gap top/left, but sometime
        // allow bot/right if needed)
        if (newX < 0 &&
            (!allowOverScroll || newX > -boardStickyBorder))
            newX = 0;
        if (newY < 0 &&
            (!allowOverScroll || newY > -boardStickyBorder))
            newY = 0;

        // as above but for scrolling top/left off screen
        if (newX > 0 &&
            (!allowOverScroll || newX < boardStickyBorder))
            newX = 0;
        if (newY > 0 &&
            (!allowOverScroll || newY < boardStickyBorder))
            newY = 0;

        scrollTo(newX, newY);
    }

    @Override
    public void scrollTo(int x, int y) {
        if (x != getScrollX() || y != getScrollY()) {
            super.scrollTo(x, y);

            if (!isBlockScrollEnd())
                scrollEnd();
        }
    }

    public void zoom(float scale, int x, int y) {
        if (!allowZoom)
            return;

        if (this.scaleScrollLocation == null) {
            this.scaleScrollLocation = new ScrollLocation(
                this.resolveToImagePoint(x, y), this.imageGrid
            );
        }

        if ((runningScale * scale) < minScale) {
            scale = 1.0F;
        }

        if ((runningScale * scale) > maxScale) {
            scale = 1.0F;
        }

        if(scale * this.currentScale > maxScale ){
            return;
        }
        if(scale * this.currentScale < minScale){
            return;
        }

        int h;
        int w;

        for (int i = 0; i < imageGrid.getChildCount(); i++) {
            View imageView = imageGrid.getChildAt(i);
            if (
                subImageSizes != null
                && i < subImageSizes.length
                && subImageSizes[i] != null
            ) {
                h = subImageSizes[i].height();
                w = subImageSizes[i].width();
                h *= runningScale;
                w *= runningScale;
            } else {
                h = imageView.getMeasuredHeight();
                w = imageView.getMeasuredWidth();
                h *= scale;
                w *= scale;
            }
            setViewParams(imageView, w, h);
        }

        if (fullImageSize != null) {
            h = fullImageSize.height();
            w = fullImageSize.width();
            h *= runningScale;
            w *= runningScale;
        } else {
            h = imageGrid.getMeasuredHeight();
            w = imageGrid.getMeasuredWidth();
            h *= scale;
            w *= scale;
        }
        setViewParams(imageGrid, w, h);

        this.scaleScrollLocation.fixScroll(w, h);

        runningScale *= scale;
        currentScale *= scale;
    }

    public void zoomEnd() {
        if (this.scaleScrollLocation != null) {
            onScale(
                runningScale,
                this.scaleScrollLocation.findNewPoint(
                    getImageWidth(), getImageHeight()
                )
            );
            this.scaleScrollLocation.fixScroll(
                getImageWidth(), getImageHeight()
            );
        }

        this.scaleScrollLocation = null;
        runningScale = 1.0f;
    }

    /**
     * Set size of image view
     *
     * Just uses shape of bitmap, doesn't show any of the data.
     */
    private void presetImageSize(BitmapGrid bitmap) {
        int numRows = bitmap == null ? 0 : bitmap.getNumRows();
        int numCols = bitmap == null ? 0 : bitmap.getNumCols();
        if (numRows == 0 || numCols == 0) {
            imageGrid.removeAllViews();
            setViewParams(imageGrid, 1, 1);
            subImageSizes = null;
            fullImageSize = null;
        } else {
            // rescale if we had to change grid layout
            boolean newTileDims = subImageSizes == null
                || imageGrid.getRowCount() != numRows
                || imageGrid.getColumnCount() != bitmap.getNumCols();

            if (newTileDims) {
                // would be nicer to reuse all views, but a naive
                // implementation would cause crashes when Android
                // noticed the child views didn't agree with the parent
                // on row/col counts
                imageGrid.removeAllViews();

                // set image grid to correct grid size and child count
                imageGrid.setRowCount(numRows);
                imageGrid.setColumnCount(bitmap.getNumCols());

                subImageSizes = new ImageSize[numRows * numCols];
                for (int row = 0; row < numRows; row++) {
                    for (int col = 0; col < numCols; col++) {
                        imageGrid.addView(new ImageView(getContext()));
                    }
                }
            }

            // set bitmaps and dimensions
            for (int row = 0; row < numRows; row++) {
                for (int col = 0; col < numCols; col++) {
                    BitmapGrid.Tile tile = bitmap.getTile(row, col);
                    if (tile == null)
                        continue;

                    int viewIdx = row * numCols + col;
                    ImageView imageView
                        = (ImageView) imageGrid.getChildAt(viewIdx);

                    int tileWidth = tile.getWidth();
                    int tileHeight = tile.getHeight();

                    boolean newSubSize = subImageSizes[viewIdx] == null
                        || subImageSizes[viewIdx].width() != tileWidth
                        || subImageSizes[viewIdx].height() != tileHeight;

                    if (newSubSize) {
                        subImageSizes[viewIdx] = new ImageSize(
                            tileWidth, tileHeight
                        );
                        setViewParams(imageView, tileWidth, tileHeight);
                    }
                }
            }

            boolean newSize = fullImageSize == null
                || fullImageSize.width() != bitmap.getWidth()
                || fullImageSize.height() != bitmap.getHeight();

            if (newSize) {
                setViewParams(imageGrid, bitmap.getWidth(), bitmap.getHeight());
                fullImageSize = new ImageSize(
                    bitmap.getWidth(), bitmap.getHeight()
                );
            }
        }
    }

    /**
     * Bookkeeping for gesture start
     *
     * Stores scroll location so it can be restored on cancel
     */
    private void onTouchStart() {
        gestureStartX = getScrollX();
        gestureStartY = getScrollY();
        pushBlockScrollEnd();
    }

    /**
     * Bookkeeping for gesture end
     *
     * Currently does nothing
     */
    private void onTouchEnd() {
        popBlockScrollEnd();
        if (gestureStartX != getScrollX() || gestureStartY != getScrollY()) {
            if (!isBlockScrollEnd())
                scrollEnd();
        }
    }

    /**
     * Bookkeeping for gesture cancel
     *
     * Undoes scroll actions if the scroll was ultimately cancelled
     */
    private void onTouchCancel() {
        scrollTo(gestureStartX, gestureStartY);
        popBlockScrollEnd();
    }

    /**
     * Called when a scrolling gesture has finished, or scroll jumped
     */
    protected void scrollEnd() {
        // Do nothing, for overriding
    }

    public interface AuxTouchHandler {
        boolean inProgress();
        void init(ScrollingImageView view);
        boolean onTouchEvent(MotionEvent ev);
    }

    public interface ClickListener {
        default void onContextMenu(Point e) { }
        default void onTap(Point e) { }
    }

    public interface ScaleListener {
        void onScale(float scale);
    }

    /**
     * The area visible on screen
     *
     * Not limited to the size of the bitmap. Takes scroll position into
     * account. Does not go below 0. E.g. if the x range was 100-600
     * then those 500 pixels of the bitmap could be visible, if the
     * bitmap were wide enough.
     */
    public Rect getVisibleRect() {
        Point topLeft = resolveToImagePoint(0, 0);
        Point bottomRight = resolveToImagePoint(
            getMeasuredWidth(), getMeasuredHeight()
        );
        Rect rect = new Rect(
            Math.max(0, topLeft.x),
            Math.max(0, topLeft.y),
            Math.max(0, bottomRight.x),
            Math.max(0, bottomRight.y)
        );
        return rect;
    }

    /**
     * Called when scale changes
     *
     * Always call super.onScale, so that listeners can also be
     * notified.
     */
    protected void onScale(float scale, Point center) {
        notifyScaleChange(scale);
    }

    protected void notifyScaleChange(float scale) {
        if (scaleListener != null)
            scaleListener.onScale(scale);
    }

    /**
     * Called on long press on point p
     *
     * Always call super.onContextMenu so that listeners can be
     * notified.
     */
    protected void onContextMenu(Point p) {
        if (ScrollingImageView.this.ctxListener != null) {
            ScrollingImageView.this.ctxListener.onContextMenu(p);
            ScrollingImageView.this.longTouched = true;
        }
    }

    /**
     * Called on tap on point p
     *
     * Always call super.onTap so that listeners can be notified.
     */
    protected void onTap(Point p) {
        if (this.ctxListener != null) {
            this.ctxListener.onTap(p);
        }
    }

    /**
     * Change view params, return if change made
     */
    private boolean setViewParams(View view, int width, int height) {
        return setViewParams(view, width, height, false);
    }

    /**
     * Change image size, add to view if needed
     *
     * Ignores if view has not been attached / has not current layout
     * params, unless force set.
     */
    private boolean setViewParams(
        View view, int width, int height, boolean force
    ) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp != null) {
            boolean change = lp.width != width || lp.height != height;
            if (force || change) {
                lp.width = width;
                lp.height = height;
                view.setLayoutParams(lp);
                return true;
            }
        }
        return false;
    }

    private void pushBlockScrollEnd() {
        numBlockScrollEnd += 1;
    }

    private void popBlockScrollEnd() {
        if (numBlockScrollEnd > 0)
            numBlockScrollEnd -= 1;
    }

    private boolean isBlockScrollEnd() {
        return numBlockScrollEnd > 0;
    }

    public static class Point {
        int x;
        int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Point() {
        }

        public int distance(Point p) {
            double d = Math.sqrt(
                ((double) this.x - (double) p.x)
                    + ((double) this.y - (double) p.y)
            );

            return (int) Math.round(d);
        }

        @Override
        public String toString(){
            return "["+x+", "+y+"]";
        }
    }

    private class ScrollLocation {
        private final double percentAcrossImage;
        private final double percentDownImage;
        private final int absoluteX;
        private final int absoluteY;

        public ScrollLocation(Point p, View view) {
            this.percentAcrossImage
                = (double) p.x / (double) view.getMeasuredWidth();
            this.percentDownImage
                = (double) p.y / (double) view.getMeasuredHeight();
            this.absoluteX = p.x - ScrollingImageView.this.getScrollX();
            this.absoluteY = p.y - ScrollingImageView.this.getScrollY();
        }

        public Point findNewPoint(int newWidth, int newHeight) {
            int newX = (int) Math.round(
                (double) newWidth * this.percentAcrossImage
            );
            int newY = (int) Math.round(
                (double) newHeight * this.percentDownImage
            );
            return new Point(newX, newY);
        }

        public void fixScroll(int newWidth, int newHeight) {
            Point newPoint = this.findNewPoint(newWidth, newHeight);

            int newScrollX = newPoint.x - this.absoluteX;
            int newScrollY = newPoint.y - this.absoluteY;

            pushBlockScrollEnd();
            scrollTo(newScrollX, newScrollY);
            popBlockScrollEnd();
        }
    }

    /**
     * Used to record original size of an image in the grid
     */
    private record ImageSize(
        int width,
        int height
    ) { }
}
