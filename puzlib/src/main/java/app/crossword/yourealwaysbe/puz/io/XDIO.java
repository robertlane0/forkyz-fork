
package app.crossword.yourealwaysbe.puz.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;

/**
 * Parser for .xd
 *
 * Format url:
 * https://github.com/century-arcade/xd/blob/master/doc/xd-format.md
 **/
public class XDIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger(XDIO.class.getCanonicalName());

    private static final String ACROSS_LIST = DefaultListNames.ACROSS_LIST;
    private static final String DOWN_LIST = DefaultListNames.DOWN_LIST;
    private static final String ANON_LIST = DefaultListNames.NODIR_LIST;

    private static final DateTimeFormatter DATE_FORMATTER
        = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);

    private static final Set<Character> BLOCKS = new HashSet<>();
    static {
        Collections.addAll(BLOCKS, '#', '_', '■', '＿');
    }

    private static final String SECTION_START = "## ";

    private static final Pattern REF_CLUE_RE
        = Pattern.compile("[^.]*\\^\\w*:.*");

    private static final MatchReplace[] MARKUPS = new MatchReplace[] {
        new MatchReplace("\\{/([^}]*)/\\}", "<i>$1</i>"),
        new MatchReplace("\\{\\*([^}]*)\\*\\}", "<b>$1</b>"),
        new MatchReplace("\\{_([^}]*)_\\}", "<u>$1</u>"),
        new MatchReplace("\\{-([^}]*)-\\}", "<strike>$1</strike>")
    };
    private static final MatchReplace LINEBREAKS
        = new MatchReplace("\\\\", "<br/>");

    public static class XDFormatException extends Exception {
        private static final long serialVersionUID = 3890271331387993661L;
        public XDFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws IOException {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is)
            );

            String line = getNextNonBlankLine(reader);

            if (Section.isSectionTitle(line))
                return readTitledSections(line, reader);
            else
                return readImplicitSections(line, reader);
        } catch (XDFormatException e) {
            LOG.info("Failed to parse XD file: " + e);
            return null;
        }
    }

    private static Puzzle readTitledSections(
        String firstTitle, BufferedReader reader
    ) throws IOException, XDFormatException {
        String title = firstTitle;

        Meta meta = null;
        char[][] boxes = null;
        List<ClueInfo> clues = null;
        String notes = null;

        while (title != null) {
            switch (Section.parseTitle(title)) {
            case META:
                meta = readMeta(null, reader);
                break;
            case GRID:
                boxes = readBoxes(reader, false);
                break;
            case CLUES:
                clues = readClueInfo(reader, false);
                break;
            case NOTES:
                notes = readNotes(reader, false);
                break;
            case OTHER:
                // skip
                break;
            }

            title = getNextNonBlankLine(reader);
        }

        return buildPuzzle(meta, boxes, clues, notes);
    }

    private static Puzzle readImplicitSections(
        String firstLine, BufferedReader reader
    ) throws IOException, XDFormatException {
        Meta meta = readMeta(firstLine, reader);
        char[][] boxes = readBoxes(reader, true);
        List<ClueInfo> clues = readClueInfo(reader, true);
        String notes = readNotes(reader, true);
        return buildPuzzle(meta, boxes, clues, notes);
    }

    /**
     * Read meta section
     *
     * First line is either the first line of the section, or null if
     * the whole section is still in the reader (without the title)
     */
    private static Meta readMeta(
        String firstLine, BufferedReader reader
    ) throws IOException, XDFormatException {
        String title = null;
        String author = null;
        String copyright = null;
        Map<Character, String> rebus = new HashMap<>();
        LocalDate date = null;
        Special special = null;
        Map<Character, String> clueGroups = new HashMap<>();
        String description = null;
        String notes = null;

        String line = firstLine;
        if (line == null)
            line = getNextDataLine(reader);

        while (line != null) {
            String[] keyValue = line.split(":", 2);

            if (keyValue.length < 2)
                throw new XDFormatException("Unexpected metadata line: " + line);

            String key = keyValue[0].trim();
            String value = keyValue[1].trim();

            if ("title".equalsIgnoreCase(key)) {
                title = parseString(value, true);
            } else if ("author".equalsIgnoreCase(key)) {
                author = parseString(value, true);
            } else if ("copyright".equalsIgnoreCase(key)) {
                copyright = parseString(value, true);
            } else if ("description".equalsIgnoreCase(key)) {
                description = parseString(value, true);
            } else if ("notes".equalsIgnoreCase(key)) {
                notes = parseString(value, true);
            } else if ("rebus".equalsIgnoreCase(key)) {
                parseLookupTable(value, rebus);
            } else if ("cluegroup".equalsIgnoreCase(key)) {
                parseLookupTable(value, clueGroups);
            } else if ("date".equalsIgnoreCase(key)) {
                try {
                    date = LocalDate.parse(value, DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    throw new XDFormatException(
                        "Unexpected date format: " + value
                    );
                }
            } else if ("special".equalsIgnoreCase(key)) {
                special = Special.parseSpecial(value);
                if (special == null) {
                    throw new XDFormatException(
                        "Unrecognised special value " + value
                    );
                }
            }

            line = getNextDataLine(reader);
        }

        return new Meta(
            title,
            author,
            copyright,
            rebus,
            date,
            special,
            clueGroups,
            description,
            notes
        );
    }

    /**
     * Parse the input string into the table
     *
     * Input of form char=value char=value. E.g.
     *
     * a=ABC d=DEF
     */
    private static void parseLookupTable(
        String input, Map<Character, String> table
    ) throws XDFormatException {
        for (String entry : input.split("\\s+")) {
            String[] keyValue = entry.split("=");
            if (keyValue.length < 2) {
                throw new XDFormatException(
                    "Unexpected table entry " + entry
                );
            }
            if (keyValue[0].length() != 1) {
                throw new XDFormatException(
                    "Invalid table key: " + keyValue[0]
                );
            }
            table.put(keyValue[0].charAt(0), keyValue[1]);
        }
    }

    /**
     * Read boxes from file
     *
     * Will return a grid, throws an exception if error
     *
     * @param reader source of data
     * @param chompBlankLines whether to ignore any blank lines
     * appearing before the grid
     * @return matrix of chars for boxes (need meta data to convert to
     * boxes
     */
    private static char[][] readBoxes(
        BufferedReader reader, boolean chompBlankLines
    ) throws IOException, XDFormatException {
        List<String> lines = new ArrayList<>();

        String line = chompBlankLines
            ? getNextNonBlankLine(reader)
            : getNextDataLine(reader);

        if (line == null)
            throw new XDFormatException("No grid!");

        int maxWidth = 0;

        while (line != null) {
            line = line.trim();
            maxWidth = Math.max(line.length(), maxWidth);
            lines.add(line);
            line = getNextDataLine(reader);
        }

        char[][] boxes = new char[lines.size()][maxWidth];
        for (int row = 0; row < lines.size(); row++) {
            line = lines.get(row);
            for (int col = 0; col < maxWidth; col++) {
                if (col >= line.length())
                    boxes[row][col] = '_';
                else
                    boxes[row][col] = line.charAt(col);
            }
        }

        return boxes;
    }

    /**
     * Set circle/highlight if letter is special cell
     */
    private static void setSpecial(Meta meta, char cell, Box box) {
        Special special = (meta == null) ? null : meta.getSpecial();
        if (special == null)
            special = Special.CIRCLE;

        if (Character.isLowerCase(cell)) {
            switch (special) {
            case CIRCLE:
                box.setShape(Box.Shape.CIRCLE);
                break;
            case SHADED:
                box.setColor(IPuzIO.HIGHLIGHT_COLOR);
                break;
            }
        }
    }

    /**
     * Read clues from file
     *
     * @param reader source of data
     * @param chompBlankLines whether to ignore any blank lines
     * appearing before the grid
     * @return List of found clues
     */
    private static List<ClueInfo> readClueInfo(
        BufferedReader reader, boolean chompBlankLines
    ) throws IOException, XDFormatException {
        List<ClueInfo> clues = new ArrayList<>();

        String line = chompBlankLines
            ? getNextNonBlankLine(reader)
            : getNextDataLine(reader);

        if (line == null)
            return clues;

        while (line != null) {
            line = line.trim();

            if (REF_CLUE_RE.matcher(line).matches()) {
                line = getNextDataLine(reader);
                continue;
            }

            String[] numSplit = line.split("\\.", 2);
            if (numSplit.length < 2) {
                throw new XDFormatException(
                    "No number end found in clue: " + line
                );
            }

            String numberInfo = numSplit[0];
            String hintAns = numSplit[1];

            int ansStart = hintAns.lastIndexOf("~");
            String hint = parseString(
                (ansStart < 0)
                    ? hintAns.trim()
                    : hintAns.substring(0, ansStart).trim(),
                true
            );

            clues.add(new ClueInfo(numberInfo, hint));

            line = getNextDataLine(reader);
        }

        return clues;
    }

    /**
     * Read notes from file
     *
     * @param reader source of data
     * @param chompBlankLines whether to ignore any blank lines
     * appearing before the grid
     * @return notes in html
     */
    private static String readNotes(
        BufferedReader reader, boolean chompBlankLines
    ) throws IOException, XDFormatException {
        String line = chompBlankLines
            ? getNextNonBlankLine(reader)
            : getNextDataLine(reader);

        if (line == null)
            return null;

        StringBuilder sb = new StringBuilder();

        while (line != null) {
            sb.append(line.trim() + "<br/>");

            if (Section.peekIsSectionStart(reader))
                line = null;
            else
                line = reader.readLine();
        }

        // remove trailing blank lines
        return sb.toString().replaceAll("(<br/>)*$", "");
    }

    /**
     * Build puzzle from gathered info
     */
    private static final Puzzle buildPuzzle(
        Meta meta,
        char[][] boxChars,
        List<ClueInfo> clues,
        String notes
    ) throws XDFormatException {
        Box[][] boxes = buildBoxes(boxChars, meta);
        PuzzleBuilder builder = new PuzzleBuilder(boxes);

        try {
            builder.autoNumberBoxes();
        } catch (IllegalArgumentException e) {
            throw new XDFormatException("Could not number boxes");
        }

        if (meta != null) {
            builder.setTitle(meta.getTitle())
                .setAuthor(meta.getAuthor())
                .setCopyright(meta.getCopyright())
                .setDate(meta.getDate())
                .setNotes(buildNotes(
                    meta.getDescription(), meta.getNotes(), notes
                ));
        }

        buildClues(clues, meta, builder);

        return builder.getPuzzle();
    }

    /**
     * Add clue infos as clues to puzzle builder
     */
    private static void buildClues(
        List<ClueInfo> clues, Meta meta, PuzzleBuilder builder
    ) {
        if (clues == null)
            return;

        Map<Character, String> clueGroups =
            (meta == null) ? null : meta.getExtraClueGroups();

        for (ClueInfo clue : clues) {
            String numberInfo = clue.getNumberInfo();
            String hint = clue.getHint();

            if (numberInfo.isEmpty()) {
                int index = builder.getNextClueIndex(ANON_LIST);
                builder.addClue(new Clue(
                    ANON_LIST, index, null, null, hint, null
                ));
            } else {
                char listID = numberInfo.charAt(0);
                String number = numberInfo.substring(1);

                if (listID == 'A') {
                    builder.addAcrossClue(ACROSS_LIST, number, hint);
                } else if (listID == 'D') {
                    builder.addDownClue(DOWN_LIST, number, hint);
                } else if (
                    clueGroups != null && clueGroups.containsKey(listID)
                ) {
                    String listName = clueGroups.get(listID);
                    int index = builder.getNextClueIndex(listName);
                    builder.addClue(new Clue(
                        listName, index, number, null, hint, null
                    ));
                } else {
                    // use full number info as number since the first
                    // letter isn't a list
                    int index = builder.getNextClueIndex(ANON_LIST);
                    builder.addClue(new Clue(
                        ANON_LIST, index, numberInfo, null, hint, null
                    ));
                }
            }
        }
    }

    /**
     * Make a single note string out of array of possible sources
     *
     * If just one non-null, then return it. Else put into paragraph
     * blocks.
     */
    private static String buildNotes(String... notes) {
        // set to only non null if only one
        String nonNull = null;
        for (String note : notes) {
            if (note != null && !note.isEmpty()) {
                if (nonNull == null) {
                    nonNull = note;
                } else {
                    nonNull = null;
                    break;
                }
            }
        }

        if (nonNull != null)
            return nonNull;

        // else make paragraphs
        StringBuilder sb = new StringBuilder();
        for (String note : notes) {
            if (note != null && !note.isEmpty())
                sb.append("<p>" + note + "</p>");
        }

        return sb.toString();
    }

    /**
     * Convert character data for boxes into boxes
     */
    private static Box[][] buildBoxes(char[][] boxData, Meta meta) {
        Map<Character, String> rebus = (meta == null) ? null : meta.getRebus();

        Box[][] boxes = new Box[boxData.length][];
        for (int row = 0; row < boxData.length; row++) {
            boxes[row] = new Box[boxData[row].length];
            for (int col = 0; col < boxData[row].length; col++) {
                char cell = boxData[row][col];
                if (!BLOCKS.contains(cell)) {
                    Box box = new Box();

                    setSpecial(meta, cell, box);

                    if (rebus != null && rebus.containsKey(cell)) {
                        box.setSolution(rebus.get(cell));
                    } else if (cell != '.') {
                        box.setSolution(Character.toUpperCase(cell));
                    }

                    boxes[row][col] = box;
                }
            }
        }
        return boxes;
    }

    /**
     * Parse a markup XD string into HTML
     *
     * Puts <i/b/u/strike> tags around XD equiv and adds linebreaks in
     * html if you say so;
     */
    private static String parseString(String s, boolean doLineBreaks) {
        for (MatchReplace mr : MARKUPS)
            s = s.replaceAll(mr.getMatch(), mr.getReplace());
        if (doLineBreaks)
            s = s.replaceAll(LINEBREAKS.getMatch(), LINEBREAKS.getReplace());
        return s;
    }

    /**
     * Reads the next non-blank line from the reader
     *
     * Returns null if the end of the file or section is reached.
     * Only chomps two blank lines at end of section, may leave further
     * blank lines.
     *
     * If finds a ## to mark a new section, does not consume the line
     * from the stream.
     */
    private static String getNextDataLine(
        BufferedReader reader
    ) throws IOException, XDFormatException {
        if (Section.peekIsSectionStart(reader))
            return null;

        String line = reader.readLine();
        if (line == null)
            return null;

        line = line.trim();
        if (!line.isEmpty())
            return line;

        if (Section.peekIsSectionStart(reader))
            return null;

        line = reader.readLine();
        if (line == null)
            return null;

        line = line.trim();

        return line.isEmpty() ? null : line;
    }

    /**
     * Returns null if EOF reached
     */
    private static String getNextNonBlankLine(
        BufferedReader reader
    ) throws IOException, XDFormatException {
        while (true) {
            String line = reader.readLine();
            if (line == null)
                return null;

            line = line.trim();
            if (!line.isEmpty())
                return line;
        }
    }

    private static enum Section {
        META("metadata"),
        GRID("grid"),
        CLUES("clues"),
        NOTES("notes"),
        OTHER("");

        private String name;

        private Section(String name) { this.name = name; }

        public boolean matches(String title) {
            if (title == null)
                return false;

            if (!isSectionTitle(title))
                return false;

            String name = title.substring(SECTION_START.length()).trim();

            return this.name.equalsIgnoreCase(name);
        }

        private static Section parseTitle(String title) {
            for (Section section : Section.values()) {
                if (section.matches(title))
                    return section;
            }
            return OTHER;
        }

        private static boolean isSectionTitle(String title) {
            return (title == null)
                ? false
                : title.startsWith(SECTION_START);
        }

        /**
         * Peek to see if next characters are the start of a section
         */
        private static boolean peekIsSectionStart(
            BufferedReader reader
        ) throws IOException {
            int len = SECTION_START.length();
            char[] buf = new char[len];

            reader.mark(len);
            int numRead = reader.read(buf, 0, len);
            reader.reset();

            if (numRead < len)
                return false;
            else
                return SECTION_START.equals(new String(buf));
        }
    }

    private static enum Special {
        SHADED("shaded"), CIRCLE("circle");

        private String name;

        private Special(String name) { this.name = name; }

        public static Special parseSpecial(String value) {
            for (Special special : Special.values()) {
                if (special.name.equalsIgnoreCase(value))
                    return special;
            }
            return null;
        }
    };

    /**
     * Fields may be null
     */
    private static class Meta {
        private String title;
        private String author;
        private String copyright;
        private Map<Character, String> rebus;
        private LocalDate date;
        private Special special;
        private Map<Character, String> extraClueGroups;
        private String description;
        private String notes;

        public Meta(
            String title,
            String author,
            String copyright,
            Map<Character, String> rebus,
            LocalDate date,
            Special special,
            Map<Character, String> extraClueGroups,
            String description,
            String notes
        ) {
            this.title = title;
            this.author = author;
            this.copyright = copyright;
            this.rebus = rebus;
            this.date = date;
            this.special = special;
            this.extraClueGroups = extraClueGroups;
            this.description = description;
            this.notes = notes;
        }

        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getCopyright() { return copyright; }
        public Map<Character, String> getRebus() { return rebus; }
        public LocalDate getDate() { return date; }
        public Special getSpecial() { return special; }
        public Map<Character, String> getExtraClueGroups() {
            return extraClueGroups;
        }
        public String getDescription() { return description; }
        public String getNotes() { return notes; }
    }

    /**
     * Information about clues
     *
     * numberInfo is whole A15 part, including the letter identifying
     * the clue list (because we don't know if it's part of the number
     * or not until we know the cluegroups from meta).
     */
    private static class ClueInfo {
        private String numberInfo;
        private String hint;

        public ClueInfo(String numberInfo, String hint) {
            this.numberInfo = numberInfo;
            this.hint = hint;
        }

        public String getNumberInfo() { return numberInfo; }
        public String getHint() { return hint; }
    }

    /**
     * match/replaces for clue markup
     */
    private static class MatchReplace {
        private String match;
        private String replace;

        public MatchReplace(String match, String replace) {
            this.match = match;
            this.replace = replace;
        }

        public String getMatch() { return match; }
        public String getReplace() { return replace; }
    }
}
