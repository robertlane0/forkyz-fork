
package app.crossword.yourealwaysbe.puz;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Representation of a word on the grid
 *
 * Normally where an answer to a clue goes, a sequence of boxes, usually
 * in a line across or down, but can be any sequence.
 */
public class Zone implements Iterable<Position> {
    private static final long serialVersionUID = 1883316891012999786L;

    public static enum Direction {
        UP, DOWN, LEFT, RIGHT;

        public Direction reverse() {
            switch (this) {
            case UP: return DOWN;
            case DOWN: return UP;
            case LEFT: return RIGHT;
            case RIGHT: return LEFT;
            }
            // never here, but don't return null
            return LEFT;
        }
    }

    private final ArrayList<Position> positions = new ArrayList<>();

    public boolean hasPosition(Position pos) {
        return positions.contains(pos);
    }

    public int size() {
        return positions.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Get position at index, or null
     */
    public Position getPosition(int idx) {
        if (idx >= 0 && idx < positions.size())
            return positions.get(idx);
        else
            return null;
    }

    /**
     * Index of position or -1
     */
    public int indexOf(Position pos) {
        return positions.indexOf(pos);
    }

    public void addPosition(Position pos) {
        if (pos != null)
            positions.add(pos);
    }

    public void appendZone(Zone zone) {
        if (zone == null)
            return;

        for (Position pos : zone)
            addPosition(pos);
    }

    @Override
    public Iterator<Position> iterator() {
        return positions.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Zone))
            return false;

        Zone other = (Zone) o;

        return positions.equals(other.positions);
    }

    @Override
    public int hashCode() {
        return positions.hashCode();
    }

    @Override
    public String toString() {
        return positions.toString();
    }

    /**
     * Get the direction of travel for zone from position to position + 1
     */
    public Direction getDirection(int position) {
        // just a default
        if (size() <= 1)
            return position <= 0 ? Direction.LEFT : Direction.RIGHT;

        if (position < 0)
            position = 0;

        // use last two if at the end
        if (position >= size() - 1)
            position = size() - 2;

        Position posStart = getPosition(position);
        Position posEnd = getPosition(position + 1);

        if (posStart.getCol() < posEnd.getCol())
            return Direction.RIGHT;
        else if (posStart.getCol() > posEnd.getCol())
            return Direction.LEFT;
        else if (posStart.getRow() < posEnd.getRow())
            return Direction.DOWN;
        else if (posStart.getRow() > posEnd.getRow())
            return Direction.UP;
        else // same position, who knows!
            return Direction.LEFT;
    }
}
