
package app.crossword.yourealwaysbe.forkyz.view;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * A "large" bitmap made up of a grid of smaller ones
 */
public class BitmapGrid {
    /**
     * A cell in the grid
     *
     * Each cell should be the same width/height except the last row and
     * column that might be short in the relevant direction.
     *
     * Null bitmap means transparent.
     */
    public static class Cell {
        private Bitmap bitmap;
        private int width;
        private int height;

        public Cell(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }

        /**
         * Get bitmap or null if this cell doesn't have one
         */
        public Bitmap getBitmap() { return bitmap; }

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
    }

    private Cell[][] grid;
    private int numRows;
    private int numCols;
    private int width;
    private int height;

    /**
     * Build a grid structure
     *
     * Splits into cellSize x cellSize squares, except for the end
     * pieces that just hold the leftovers.
     *
     * @param width full bitmap width
     * @param height full bitmap height
     * @param cellSize split into squares of (up to) this size
     */
    public BitmapGrid(int width, int height, int cellSize) {
        this.width = width;
        this.height = height;
        this.numRows = (int) Math.ceil((double) width / cellSize);
        this.numCols = (int) Math.ceil((double) height / cellSize);

        System.out.println("FORKYZ: w " + width + " h " + height);
        System.out.println("FORKYZ: nr " + numRows + " nc " + numCols);

        this.grid = new Cell[numRows][numCols];

        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                int cellWidth = col == numCols - 1 && width != cellSize
                    ? width % cellSize
                    : cellSize;
                int cellHeight = row == numRows - 1 && height != cellSize
                    ? height % cellSize
                    : cellSize;
                System.out.println("FORKYZ: r " + row + " c " + col + " w " + cellWidth + " h " + cellHeight);
                grid[row][col] = new Cell(cellWidth, cellHeight);
            }
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getNumRows() { return numRows; }
    public int getNumCols() { return numCols; }

    public Cell getCell(int row, int col) {
        if (
            0 <= row && row < grid.length
            && 0 <= col && col < grid[row].length
        ) {
            return grid[row][col];
        } else {
            return null;
        }
    }
}
