
package app.crossword.yourealwaysbe.puz;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A clue on the board
 *
 * Must have a listName and index, number is optional.
 */
public class Clue implements Serializable {
    private static final long serialVersionUID = 2973710091235898521L;

    public static final int DEFAULT_FLAG_COLOR = -1;

    // enumeration is e.g. (4, 5) at end of clue. Ignores IPuz word
    // length endings e.g. ;4, except `;4 where `; should be literal ;
    // also ignore cue words e.g. long= at start of clue. Only allows
    // digits and recognised enumeration characters to avoid
    // over-inferring.
    private static final Pattern ENUMERATION_HINT_RE
        = Pattern.compile(
            "^.*\\((?:\\w+=)?"
            + "([\\d\\s\\p{Punct}]*?)"
            + "(?:(?<!`);\\d+)?\\)\s*$"
        );
    private static int ENUMERATION_HINT_GRP = 1;
    private static final Pattern ENUMERATION_WHITESPACE_RE
        = Pattern.compile("^[\\s,]+$");
    // character that mean special things in IPuz enumerations
    private static final Set<Character> ENUMERATION_SPECIAL
        = Set.of('*', '^', '+');

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
    private SortedMap<Integer, String> separators;
    private SortedMap<Integer, String> inferredSeparators;

    public Clue(
        String listName,
        int index, String number, String label,
        String hint,
        Zone zone,
        Map<Integer, String> separators
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
     * @param pos the position to get separator for
     * @param inferSeparators whether to infer separators from hint
     * @return the separating char or null char if no separator
     */
    public String getSeparator(int pos, boolean inferSeparators) {
        String separator = separators.get(pos);
        if (separator == null && inferSeparators) {
            if (inferredSeparators == null)
                inferSeparators();
            separator = inferredSeparators.get(pos);
        }
        return separator;
    }

    /**
     * Get separator at pos (not inferred)
     */
    public String getSeparator(int pos) {
        return getSeparator(pos, false);
    }

    /**
     * Whether clue has any separators
     *
     * @param inferSeparators whether to include inferred separators
     * when answering
     */
    public boolean hasSeparators(boolean inferSeparators) {
        if (separators.size() > 0)
            return true;

        if (inferSeparators) {
            if (inferredSeparators == null)
                inferSeparators();
            return inferredSeparators.size() > 0;
        } else {
            return false;
        }
    }

    /**
     * Whether has separators (not inferred)
     */
    public boolean hasSeparators() {
        return hasSeparators(false);
    }

    /**
     * Set of positions with separators (not inferred)
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
        // erase previously inferred separators
        inferredSeparators = null;
    }

    /**
     * Estimate separators from hint
     *
     * Looks for (enumeration) at end of clue. Estimation algorithm
     * based on IPuz enumeration specification, with modifications. E.g.
     * comma is interpreted as whitespace (common in most crosswords).
     */
    private void inferSeparators() {
        inferredSeparators = new TreeMap<>();

        Matcher m = ENUMERATION_HINT_RE.matcher(getHint());
        if (!m.matches())
            return;

        String enumeration = m.group(ENUMERATION_HINT_GRP);

        int len = 0;
        int sepPos = 0;
        String separator = "";

        for (int i = 0; i < enumeration.length(); i++) {
            char c = enumeration.charAt(i);
            if (Character.isDigit(c)) {
                int digit = Character.digit(c, 10);
                // abandon after first word of unspecified length
                if (len == 0 && digit == 0)
                    return;
                len = 10 * len + digit;
            } else if (ENUMERATION_SPECIAL.contains(c)) {
                // ignore
            } else {
                // backticks mean take next char literally
                if (c == '`' && i < enumeration.length() - 1) {
                    c = enumeration.charAt(i + 1);
                    i += 1;
                }

                sepPos = sepPos + len;

                String nextSep = String.valueOf(c);
                String curSep = inferredSeparators.get(sepPos);
                if (curSep == null)
                    inferredSeparators.put(sepPos, nextSep);
                else
                    inferredSeparators.put(sepPos, curSep + nextSep);

                len = 0;
            }
        }

        // replace whitespace with " "
        Set<Integer> spaces = new HashSet<>();
        for (Map.Entry<Integer, String> entry : inferredSeparators.entrySet()) {
            if (ENUMERATION_WHITESPACE_RE.matcher(entry.getValue()).matches())
                spaces.add(entry.getKey());
        }
        for (int pos : spaces)
            inferredSeparators.put(pos, " ");
    }
}

