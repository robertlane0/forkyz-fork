
package app.crossword.yourealwaysbe.puz;

import java.io.Serializable;
import java.util.Objects;

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

    public Clue(
        String listName,
        int index, String number, String label,
        String hint,
        Zone zone
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
     * 0x00rrggbb or DEFAULT_FLAG_COLOR
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
            && isFlagged() == other.isFlagged();
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

