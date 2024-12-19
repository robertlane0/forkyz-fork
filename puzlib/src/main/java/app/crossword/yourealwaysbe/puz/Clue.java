
package app.crossword.yourealwaysbe.puz;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A clue on the board
 *
 * Must have a listName and index, number is optional.
 */
public class Clue implements Serializable {
    private static final long serialVersionUID = 2973710091235898521L;

    public static final int DEFAULT_FLAG_COLOR = -1;

    private ClueID clueID;
    // because sometimes numbers are alphabetic
    private String number;
    // as per ipuz, number corresponds to cell number, but label might
    // be displayed instead.
    private String label;
    private String hint;
    private Zone zone;
    private boolean flagged = false;
    // 0x00rrggbb or DEFAULT_FLAG_COLOR
    private int flagColor = DEFAULT_FLAG_COLOR;
    // separating characters between the n-1 and n cell in answer
    private SortedMap<Integer, Character> separators;

    public Clue(
        String listName,
        int index, String number, String label,
        String hint,
        Zone zone,
        Map<Integer, Character> separators
    ) {
        this.clueID = new ClueID(listName, index);
        this.number = number;
        this.label = label;
        if (listName == null || index < 0) {
            throw new IllegalArgumentException(
                "Clues must have a list name and index in the list"
            );
        }
        this.hint = hint;
        this.zone = (zone == null) ? new Zone() : zone;
        this.separators = (separators == null)
            ? new TreeMap<>()
            : new TreeMap<>(separators);
    }

    public Clue(
        String listName,
        int index, String number, String label,
        String hint,
        Zone zone
    ) {
        this(listName, index, number, label, hint, zone, null);
    }

    public Clue(
        String listName,
        int index, String number,
        String hint,
        Zone zone
    ) {
        this(listName, index, number, null, hint, zone);
    }

    /**
     * Construct a numberless clue with no zone
     */
    public Clue(String listName, int index, String hint) {
        this(listName, index, null, hint, null);
    }

    public ClueID getClueID() { return clueID; }
    public boolean hasClueNumber() { return number != null; }
    public String getClueNumber() { return number; }
    public boolean hasLabel() { return label != null; }
    public String getLabel() { return label; }
    public String getHint() { return hint; }
    public boolean hasZone() { return zone.size() != 0; }
    public Zone getZone() { return zone; }
    public boolean isFlagged() { return flagged; }

    /**
     * Flag clue
     *
     * During play, call Playboard.flagClue to make sure listeners
     * updated.
     */
    public void setFlagged(boolean flagged) { this.flagged = flagged; }

    /**
     * 0x00rrggbb or DEFAULT_FLAG_COLOR
     */
    public int getFlagColor() { return flagColor; }

    /**
     * If no custom flag color has been set
     */
    public boolean isDefaultFlagColor() {
        return flagColor == DEFAULT_FLAG_COLOR;
    }

    /**
     * Set flag color
     *
     * 0x00rrggbb or DEFAULT_FLAG_COLOR
     *
     * During play, call Playboard.setFlagColor to make sure listeners
     * updated.
     */
    public void setFlagColor(int flagColor) {
        this.flagColor = flagColor;
    }

    /**
     * The number that should be displayed with the clue
     *
     * This is label if set, else clue number if set, else null
     */
    public String getDisplayNumber() {
        if (hasLabel())
            return getLabel();
        else if (hasClueNumber())
            return getClueNumber();
        else
            return null;
    }

    /**
     * Get the separating character between the pos-1th and pos-th cell
     *
     * 0 means start of word. 1 means between 0th and 1st character.
     *
     * @return the separating char or null char if no separator
     */
    public char getSeparator(int pos) {
        Character c = separators.get(pos);
        return (c == null) ? '\0' : c;
    }

    public boolean hasSeparators() {
        return separators.size() > 0;
    }

    /**
     * Set of positions with separators
     *
     * Will iterate in ascending order
     */
    public Set<Integer> getSeparatorPositions() {
        return separators.keySet();
    }

    @Override
    public String toString() {
        return getClueID() + " / " + getClueNumber() + " / "  + getHint();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Clue))
            return false;

        Clue other = (Clue) o;

        return getClueID().equals(other.getClueID())
            && Objects.equals(getClueNumber(), other.getClueNumber())
            && Objects.equals(getLabel(), other.getLabel())
            && Objects.equals(getHint(), other.getHint())
            && Objects.equals(getZone(), other.getZone())
            && isFlagged() == other.isFlagged()
            && Objects.equals(separators, other.separators);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getClueID(), getClueNumber(), getLabel(), getHint(), getZone()
        );
    }

    void setClueNumber(String number) {
        this.number = number;
    }

    void setHint(String hint) {
        this.hint = (hint == null) ? "" : hint;
    }
}

