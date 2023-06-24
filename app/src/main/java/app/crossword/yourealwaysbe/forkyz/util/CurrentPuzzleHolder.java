
package app.crossword.yourealwaysbe.forkyz.util;

import javax.inject.Inject;
import javax.inject.Singleton;

import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle;
import app.crossword.yourealwaysbe.puz.Playboard;

/**
 * Holder for the current puzzle being played
 *
 * Singleton and app-wide. Persists until application destroyed
 * (property of Hilt singletons). Can be Hilt-injected into whichever
 * class needs access to get/set the current puzzle.
 */
@Singleton
public class CurrentPuzzleHolder {

    private Playboard board;
    private PuzHandle puzHandle;

    @Inject
    public CurrentPuzzleHolder() { }

    /**
     * Set the board and base file of the puzzle loaded on it
     */
    public void setBoard(Playboard board, PuzHandle puzHandle){
        this.board = board;
        this.puzHandle = puzHandle;
    }

    public void clearBoard() {
        this.board = null;
        this.puzHandle = null;
    }

    public Playboard getBoard() {
         return board;
    }

    public PuzHandle getPuzHandle() {
        return puzHandle;
    }
}
