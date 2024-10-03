
package app.crossword.yourealwaysbe.forkyz.view;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;

import app.crossword.yourealwaysbe.puz.Position;

/**
 * A "large" bitmap made up of a grid of smaller ones
 */
public class BitmapGrid {
    /**
     * A tile in the grid
     *
     * Each tile should be the same width/height except the last row and
     * column that might be short in the relevant direction.
     *
     * Null bitmap means transparent.
     */
    public static class Tile {
        private Bitmap bitmap;
        private int width;
        private int height;

        public Tile(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }

        /**
         * Get bitmap or null if this tile doesn't have one
         */
        public Bitmap getBitmap() { return bitmap; }

        /**
         * If bitmap is not null
         */
        public boolean hasBitmap() { return bitmap != null; }

        /**
         * Get bitmap for drawing on, create if needed
         */
        public Bitmap getCreateBitmap() {
            if (bitmap == null) {
                bitmap = Bitmap.createBitmap(
                    width, height, Bitmap.Config.ARGB_8888
                );
                bitmap.eraseColor(Color.TRANSPARENT);
            }
            return bitmap;
        }

        public void destroyBitmap() {
            bitmap = null;
        }
    }

    private Tile[][] grid;
    private int numRows;
    private int numCols;
    private int width;
    private int height;
    private int tileSize;

    /**
     * Build a grid structure
     *
     * Splits into tileSize x tileSize squares, except for the end
     * pieces that just hold the leftovers.
     *
     * @param width full bitmap width
     * @param height full bitmap height
     * @param tileSize split into squares of (up to) this size
     */
    public BitmapGrid(int width, int height, int tileSize) {
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.numRows = height / tileSize + ((height % tileSize > 0) ? 1 : 0);
        this.numCols = width / tileSize + ((width % tileSize > 0) ? 1 : 0);

        this.grid = new Tile[numRows][numCols];

        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                int tileWidth = col == numCols - 1 && width % tileSize > 0
                    ? width % tileSize
                    : tileSize;
                int tileHeight = row == numRows - 1 && height % tileSize > 0
                    ? height % tileSize
                    : tileSize;
                grid[row][col] = new Tile(tileWidth, tileHeight);
            }
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getNumRows() { return numRows; }
    public int getNumCols() { return numCols; }
    public int getTileSize() { return tileSize; }

    public Tile getTile(int row, int col) {
        if (
            0 <= row && row < grid.length
            && 0 <= col && col < grid[row].length
        ) {
            return grid[row][col];
        } else {
            return null;
        }
    }

    /**
     * Get the tile position containing pixel (x, y) or null
     *
     * I.e. row/col of tile in tile grid (not puzzle cell grid)
     */
    public Position getPixelPosition(int x, int y) {
        if (
            0 <= x && x < getWidth()
            && 0 <= y && y < getHeight()
        ) {
            return new Position(y / tileSize, x / tileSize);
        } else {
            return null;
        }
    }

    /**
     * The rect covered by the tile
     *
     * Null if out of bounds
     */
    public Rect getTileRect(int row, int col) {
        if (
            0 <= row && row < getNumRows()
            && 0 <= col && col < getNumCols()
        ) {
            Tile tile = getTile(row, col);
            int startX = col * tileSize;
            int startY = row * tileSize;
            int endX = startX + ((tile == null) ? tileSize : tile.getWidth());
            int endY = startY + ((tile == null) ? tileSize : tile.getHeight());

            return new Rect(startX, startY, endX, endY);
        } else {
            return null;
        }
    }
}
