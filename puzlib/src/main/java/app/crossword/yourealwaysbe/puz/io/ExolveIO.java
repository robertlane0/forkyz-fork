
package app.crossword.yourealwaysbe.puz.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.Zone;
import app.crossword.yourealwaysbe.puz.util.HtmlUtil;

/**
 * Parser for Exolve format
 *
 * Format url:
 * https://github.com/viresh-ratnakar/exolve
 *
 * Here be dragons
 * TODO: exolve-color
 **/
public class ExolveIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger(ExolveIO.class.getCanonicalName());

    private static final String ACROSS_LIST = "Across";
    private static final String DOWN_LIST = "Down";
    private static final String NODIR_LIST = "Clues";

    private static final Set<String> MULTILINES_SECTIONS = new HashSet<>();
    static {
        Collections.addAll(
            MULTILINES_SECTIONS,
            "exolve-preamble", "exolve-prelude", "exolve-postscript",
            "exolve-grid",
            "exolve-across", "exolve-down", "exolve-nodir",
            "exolve-explanations",
            "exolve-maker",
            "exolve-relabel"
        );
    }

    private static final Set<Character> CELL_MODIFIERS = new HashSet<>();
    static {
        Collections.addAll(CELL_MODIFIERS, '|', '_', '+', '@', '*', '!', '~');
    }

    private static final Character ESCAPE = '&';

    private static final Set<String> ACROSS_SUFFIXES = new HashSet<>();
    static {
        // b is "back" for when there is a reversal
        Collections.addAll(ACROSS_SUFFIXES, "a", "ac", "aw", "b");
    }

    private static final Set<String> DOWN_SUFFIXES = new HashSet<>();
    static {
        // u is "up" for when there is a reversal
        Collections.addAll(DOWN_SUFFIXES, "d", "dn", "u");
    }

    public static class ExolveFormatException extends Exception {
        private static final long serialVersionUID = 5393371441393213661L;
        public ExolveFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws IOException {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            Map<String, String> data = extractData(is);
            if (data == null)
                return null;
            return readPuzzleFromData(data);
        } catch (ExolveFormatException e) {
            LOG.info("Failed to parse Exolve file: " + e);
            return null;
        }
    }

    private static Map<String, String> extractData(
        InputStream is
    ) throws IOException, ExolveFormatException {
        Map<String, String> data = new HashMap<>();

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(is)
        );

        chompToStart(reader);

        String line = reader.readLine();

        while (line != null) {
            line = line.trim();

            if (isEnd(line))
                break;

            if (isComment(line) || line.isEmpty()) {
                line = reader.readLine();
                continue;
            }

            if (!isSection(line)) {
                throw new ExolveFormatException(
                    "Line is not the start of a section: " + line
                );
            }

            String key = line;
            String value = "";

            int valueStart = line.indexOf(":");
            if (valueStart >= 0) {
                // because even the exolve examples don't always end
                // with a colon... hope the line is entirely a key
                key = line.substring(0, valueStart).trim().toLowerCase();
                value = line.substring(valueStart + 1).trim();
            }

            if (!MULTILINES_SECTIONS.contains(key)) {
                addKeyValue(data, key, value);
                line = reader.readLine();
            } else {
                StringBuilder fullValue = new StringBuilder();
                fullValue.append(value + "\n");

                while (true) {
                    line = reader.readLine();
                    if (line == null)
                        break;
                    line = line.trim();
                    if (isSection(line)) {
                        // leave line variable for next iteration
                        break;
                    }

                    if (!isComment(line))
                        fullValue.append(line + "\n");
                }

                addKeyValue(data, key, fullValue.toString());
            }
        }

        return data;
    }

    /**
     * Add value to the key, appending to existing sep w/ newline
     */
    private static void addKeyValue(
        Map<String, String> data, String key, String value
    ) {
        if (data.containsKey(key))
            data.put(key, data.get(key) + "\n" + value);
        else
            data.put(key, value);
    }

    /**
     * Tests if a line is a section
     *
     * I.e. starts exolve-
     */
    private static boolean isSection(String line) {
        if (line == null || line.length() < 7)
            return false;
        return "exolve-".equalsIgnoreCase(line.substring(0, 7));
    }

    /*
     * Check if a line is exactly exolve-end
     */
    private static boolean isEnd(String line) {
        return "exolve-end".equalsIgnoreCase(line);
    }

    private static boolean isComment(String line) {
        return line.matches("#\\s.*");
    }

    private static void chompToStart(
        BufferedReader reader
    ) throws ExolveFormatException, IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if ("exolve-begin".equalsIgnoreCase(line.trim()))
                return;
        }
        throw new ExolveFormatException("No start tag found");
    }

    private static Puzzle readPuzzleFromData(
        Map<String, String> data
    ) throws ExolveFormatException {
        String exolve3d = data.get("exolve-3d");
        if (exolve3d != null && exolve3d.length() > 0) {
            throw new ExolveFormatException("3D puzzles not supported.");
        }

        ExolveBoxes boxes = getBoxes(data);
        PuzzleBuilder builder = new PuzzleBuilder(boxes.getBoxes());
        // may have numbers adjusted while adding clues
        builder.autoNumberBoxes(boxes.getSkipNumberCells());
        addMeta(data, builder);
        ExolveClueLists clueData = getClueData(data, builder.getHeight());
        addClues(data, clueData, builder);
        reverseNumbers(data, builder);
        addCompletion(data, clueData, builder);
        addColors(data, clueData, builder);

        return builder.puzzle;
    }

    /**
     * Add color info to grid
     *
     * Assumes clueData has expanded zones. For now, ignore color scheme
     * options like color-cell, color-circle and so on. Partly due to
     * fatigue, and partly because the color- options mainly cover
     * interface components, which are a bit out of scope. So stick to
     * the "default" color scheme.
     */
    private static void addColors(
        Map<String, String> data,
        ExolveClueLists clueData,
        PuzzleBuilder builder
    ) {
        // sigh
        String colors = data.get("exolve-color");
        String colorsU = data.get("exolve-colour");
        if (colors == null || colors.isEmpty())
            colors = colorsU;
        else
            colors += "\n" + colorsU;
        if (colors == null || colors.isEmpty())
            return;

        Map<String, Zone> acrossZones = new HashMap<>();
        Map<String, Zone> downZones = new HashMap<>();

        for (ExolveClue clue : clueData.getAcross().getClues())
            acrossZones.put(clue.getNumber(), clue.getZone());
        for (ExolveClue clue : clueData.getDown().getClues())
            downZones.put(clue.getNumber(), clue.getZone());

        for (String colorLine : colors.split("\n")) {
            String[] args = colorLine.split("\\s+");

            // find color first
            int color = -1;
            for (String arg : args) {
                color = HtmlUtil.parseHtmlColor(arg);
                if (color >= 0)
                    break;
            }

            if (color >= 0) {
                for (String arg : args) {
                    if (HtmlUtil.parseHtmlColor(arg) >= 0)
                        continue;

                    try {
                        Position pos = ExolveClue.parsePosition(
                            arg, builder.getHeight()
                        );
                        colorPosition(pos, color, builder);
                    } catch (ExolveFormatException e) {
                        // not a position, try a clue
                        // one of several different formats, again...
                        if (arg.startsWith("a") || arg.startsWith("A")) {
                            String number = arg.substring(1);
                            colorZone(acrossZones.get(number), color, builder);
                        } else if (arg.endsWith("a") || arg.endsWith("A")) {
                            String number = arg.substring(0, arg.length() - 1);
                            colorZone(acrossZones.get(number), color, builder);
                        } else if (arg.startsWith("d") || arg.startsWith("D")) {
                            String number = arg.substring(1);
                            colorZone(downZones.get(number), color, builder);
                        } else if (arg.endsWith("d") || arg.endsWith("D")) {
                            String number = arg.substring(0, arg.length() - 1);
                            colorZone(downZones.get(number), color, builder);
                        }
                    }
                }
            }
        }
    }

    private static void colorZone(
        Zone zone, int color, PuzzleBuilder builder
    ) {
        if (zone != null) {
            for (Position pos : zone)
                colorPosition(pos, color, builder);
        }
    }

    private static void colorPosition(
        Position pos, int color, PuzzleBuilder builder
    ) {
        Box box = builder.getBox(pos);
        if (box != null)
            box.setColor(color);
    }

    /**
     * Reverse numbers according to reversals
     */
    private static void reverseNumbers(
        Map<String, String> data,
        PuzzleBuilder builder
    ) throws ExolveFormatException {
        // need to move numbers by reversal
        for (Reversal rev : getReversals(data, builder.getHeight())) {
            Box start = builder.getBox(rev.getStart());
            Box end = builder.getBox(rev.getEnd());

            String numberStart = start.getClueNumber();
            String numberEnd = end.getClueNumber();

            // could try to switch middle cells too, but spec doesn't
            // say that
            start.setClueNumber(numberEnd);
            end.setClueNumber(numberStart);
        }
    }

    /**
     * Height needed for position parsing
     */
    private static ExolveClueLists getClueData(
        Map<String, String> data, int height
    ) throws ExolveFormatException {
        return new ExolveClueLists(
            getClueListData(data.get("exolve-across"), height),
            getClueListData(data.get("exolve-down"), height),
            getClueListData(data.get("exolve-nodir"), height)
        );
    }

    /**
     * Height needed for position parsing
     */
    private static ExolveClueList getClueListData(
        String listData, int height
    ) throws ExolveFormatException {
        List<ExolveClue> clues= new ArrayList<>();

        if (listData == null)
            return new ExolveClueList(null, clues);

        String[] lines = listData.split("\n");

        // first line is blank or title
        String title = lines[0].trim();

        for (int i = 1; i < lines.length; i++) {
            clues.add(ExolveClue.parseClueLine(lines[i], height));
        }

        return new ExolveClueList(title, clues);
    }

    /**
     * Scans clues for annotations and puts into a completion msg
     */
    private static void addCompletion(
        Map<String, String> data,
        ExolveClueLists clueData,
        PuzzleBuilder builder
    ) throws ExolveFormatException {
        StringBuilder sb = new StringBuilder();

        String explanations = data.get("exolve-explanations");
        if (explanations != null)
            sb.append(explanations.trim());

        addAnnotations(clueData.getAcross(), ACROSS_LIST, sb);
        addAnnotations(clueData.getDown(), DOWN_LIST, sb);
        addAnnotations(clueData.getNodir(), NODIR_LIST, sb);

        String questions = data.get("exolve-question");
        if (questions != null) {
            if (sb.length() > 0)
                sb.append("<h2>Questions</h2>");
            for (String question : questions.split("\n")) {
                int end = ExolveClue.getHintEnd(question);
                if (end >= 0) {
                    sb.append("<p>");
                    sb.append(question.substring(end).trim());
                    sb.append("</p>");
                }
            }
        }

        if (sb.length() > 0)
            builder.setCompletionMessage(sb.toString());
    }

    /**
     * Add an annotation section to sb
     */
    private static void addAnnotations(
        ExolveClueList list, String fallbackTitle, StringBuilder sb
    ) {
        String title = list.getTitle();
        if (title == null || title.isEmpty())
            title = fallbackTitle;

        boolean hasAnnotations = false;
        for (ExolveClue clue : list.getClues()) {
            String annot = clue.getAnnotation();
            if (annot != null && !annot.isEmpty()) {
                hasAnnotations = true;
                break;
            }
        }

        if (hasAnnotations) {
            sb.append("<h2>" + title + "</h2>");
            for (ExolveClue clue : list.getClues()) {
                String annot = clue.getAnnotation();
                if (annot != null && !annot.isEmpty()) {
                    sb.append(
                        "<p>"
                        + clue.getNumber() + ". "
                        + clue.getAnnotation()
                        + "</p>"
                    );
                }
            }
        }
    }

    private static void addClues(
        Map<String, String> data,
        ExolveClueLists clueData,
        PuzzleBuilder builder
    ) throws ExolveFormatException {
        expandZones(data, clueData, builder);
        addClueList(clueData.getAcross(), ACROSS_LIST, builder);
        addClueList(clueData.getDown(), DOWN_LIST, builder);
        addClueList(clueData.getNodir(), NODIR_LIST, builder);
    }

    /**
     * Expand all zones in the clue data to the full zones
     *
     * I.e. for across, get cells for the clue.
     * For clues with multiple parts, join them all together
     * Changes clueData.
     */
    private static void expandZones(
        Map<String, String> data,
        ExolveClueLists clueData,
        PuzzleBuilder builder
    ) throws ExolveFormatException {
        // First get a map for each list with the basic zone
        // I.e. a clue that is 1 & 2 will just get the zone for number 1
        // Enter into map using all possible variations of how the clue
        // might be references elsewhere. E.g. 1 and 1a.
        // Afterwards, stitch them all together to fill clueData
        Map<String, Zone> acrossZones = new HashMap<>();
        Map<String, Zone> downZones = new HashMap<>();
        Map<String, Zone> nodirZones = new HashMap<>();

        // reversals shift numbers.
        // E.g. an across reversal of 1.... becomes ....1. However, this
        // now means 1 down starts in a different place, but the zone
        // for 1 across should be 1.... and then reversed.
        // Keeps maps for across and down for clues that should start in
        // a different place once reversals are done.
        Map<String, Position> newAcrossStarts = new HashMap<>();
        Map<String, Position> newDownStarts = new HashMap<>();

        Set<Reversal> reversals = getReversals(data, builder.getHeight());

        for (Reversal rev : reversals) {
            // across
            if (rev.getStart().getRow() == rev.getEnd().getRow()) {
                Box start = builder.getBox(rev.getStart());
                if (start.hasClueNumber())
                    newDownStarts.put(start.getClueNumber(), rev.getEnd());
                Box end = builder.getBox(rev.getEnd());
                if (end.hasClueNumber())
                    newDownStarts.put(end.getClueNumber(), rev.getStart());
            }
            // down
            if (rev.getStart().getCol() == rev.getEnd().getCol()) {
                Box start = builder.getBox(rev.getStart());
                if (start.hasClueNumber())
                    newAcrossStarts.put(start.getClueNumber(), rev.getEnd());
                Box end = builder.getBox(rev.getEnd());
                if (end.hasClueNumber())
                    newAcrossStarts.put(end.getClueNumber(), rev.getStart());
            }
        }

        // bit copy-pasty
        for (ExolveClue clue : clueData.getAcross().getClues()) {
            String number = clue.getNumber();
            Zone zone = clue.getZone();
            if (zone == null) {
                if (newAcrossStarts.containsKey(number))
                    zone = builder.getAcrossZone(newAcrossStarts.get(number));
                else
                    zone = builder.getAcrossZone(number);
            } else if (zone.size() == 1) {
                zone = builder.getAcrossZone(zone.getPosition(0));
            } else {
                // leave zone as is
            }

            zone = applyReversal(zone, reversals);

            acrossZones.put(number, zone);
            for (String suffix : ACROSS_SUFFIXES)
                acrossZones.put(number + suffix, zone);
        }

        for (ExolveClue clue : clueData.getDown().getClues()) {
            String number = clue.getNumber();
            Zone zone = clue.getZone();
            if (zone == null) {
                if (newDownStarts.containsKey(number))
                    zone = builder.getDownZone(newDownStarts.get(number));
                else
                    zone = builder.getDownZone(number);
            } else if (zone.size() == 1) {
                zone = builder.getDownZone(zone.getPosition(0));
            } else {
                // leave zone as is
            }

            zone = applyReversal(zone, reversals);

            downZones.put(number, zone);
            for (String suffix : DOWN_SUFFIXES)
                downZones.put(number + suffix, zone);
        }

        for (ExolveClue clue : clueData.getNodir().getClues()) {
            String number = clue.getNumber();
            Zone zone = clue.getZone();
            zone = applyReversal(zone, reversals);
            nodirZones.put(number, zone);
        }

        // now fill in the data, more copy-pasty
        for (ExolveClue clue : clueData.getAcross().getClues()) {
            Zone zone = new Zone();
            zone.appendZone(acrossZones.get(clue.getNumber()));
            String[] extras = clue.getExtraNumbers();
            if (extras != null) {
                for (String extra : extras) {
                    if (acrossZones.containsKey(extra)) {
                        zone.appendZone(acrossZones.get(extra));
                    } else if (downZones.containsKey(extra)) {
                        zone.appendZone(downZones.get(extra));
                    } else if (nodirZones.containsKey(extra)) {
                        zone.appendZone(nodirZones.get(extra));
                    }
                }
            }
            clue.setZone(zone);
        }

        for (ExolveClue clue : clueData.getDown().getClues()) {
            Zone zone = new Zone();
            zone.appendZone(downZones.get(clue.getNumber()));
            String[] extras = clue.getExtraNumbers();
            if (extras != null) {
                for (String extra : extras) {
                    if (downZones.containsKey(extra)) {
                        zone.appendZone(downZones.get(extra));
                    } else if (acrossZones.containsKey(extra)) {
                        zone.appendZone(acrossZones.get(extra));
                    } else if (nodirZones.containsKey(extra)) {
                        zone.appendZone(nodirZones.get(extra));
                    }
                }
            }
            clue.setZone(zone);
        }

        for (ExolveClue clue : clueData.getNodir().getClues()) {
            Zone zone = new Zone();
            zone.appendZone(nodirZones.get(clue.getNumber()));
            String[] extras = clue.getExtraNumbers();
            if (extras != null) {
                for (String extra : extras) {
                    if (nodirZones.containsKey(extra)) {
                        zone.appendZone(nodirZones.get(extra));
                    } else if (acrossZones.containsKey(extra)) {
                        zone.appendZone(acrossZones.get(extra));
                    } else if (downZones.containsKey(extra)) {
                        zone.appendZone(downZones.get(extra));
                    }
                }
            }
            clue.setZone(zone);
        }
    }

    /**
     * Get reversal zones
     *
     * height needed for position parsing
     */
    private static Set<Reversal> getReversals(
        Map<String, String> data, int height
    ) throws ExolveFormatException {
        Set<Reversal> reversals = new HashSet<>();

        String reversalsData = data.get("exolve-reversals");
        if (reversalsData == null)
            return reversals;

        for (String entry : reversalsData.split(" ")) {
            String[] positions = entry.split("-", 2);
            if (positions.length != 2) {
                throw new ExolveFormatException(
                    "Bad entry '" + entry + "' in reversals"
                );
            }

            Position start = ExolveClue.parsePosition(positions[0], height);
            Position end = ExolveClue.parsePosition(positions[1], height);

            reversals.add(new Reversal(start, end));
        }

        return reversals;
    }

    /**
     * Reverse zone if in reversals else return orig
     */
    private static Zone applyReversal(
        Zone zone, Set<Reversal> reversals
    ) {
        if (zone == null || zone.isEmpty())
            return zone;

        Reversal rev = new Reversal(
            zone.getPosition(0), zone.getPosition(zone.size() - 1)
        );
        if (reversals.contains(rev)) {
            Zone reversed = new Zone();
            for (int i = zone.size() - 1; i >= 0; i--) {
                reversed.addPosition(zone.getPosition(i));
            }
            return reversed;
        } else {
            return zone;
        }
    }

    /**
     * Add clues from clue data
     *
     * If a numbered clue does not have the number in its starting cell,
     * the starting cell is given that number.
     *
     * @param list the clue list data from the file, assumes all zones
     * are complete
     * @param fallbackListName title to use if the list doesn't have one
     * @param builder add clues to this
     */
    private static void addClueList(
        ExolveClueList list, String fallbackListName, PuzzleBuilder builder
    ) {
        String title = list.getTitle();
        if (title == null || title.isEmpty())
            title = fallbackListName;

        for (ExolveClue clue : list.getClues()) {
            int index = builder.getNextClueIndex(title);
            String hint = clue.getHint();
            if (!"*".equals(hint)) {
                Zone zone = clue.getZone();
                String number = clue.getNumber();
                if (
                    zone != null && !zone.isEmpty()
                    && number != null && !number.isEmpty()
                ) {
                    Box box = builder.getBox(zone.getPosition(0));
                    if (box != null && !box.hasClueNumber())
                        box.setClueNumber(number);
                }

                builder.addClue(new Clue(
                    title, index, number, null, clue.getHint(), zone
                ));
            }
        }
    }

    /**
     * Remove one of the suffixes if appears
     *
     * Else return null
     */
    private static String removeSuffixIfFound(String s, Set<String> suffixes) {
        for (String suffix : suffixes) {
            if (s.endsWith(suffix))
                return s.substring(0, s.length() - suffix.length());
        }
        return null;
    }

    private static void addMeta(
        Map<String, String> data, PuzzleBuilder builder
    ) {
        builder.setTitle(data.get("exolve-title"))
            .setAuthor(data.get("exolve-setter"))
            .setCopyright(data.get("exolve-copyright"));

        addIntro(data, builder);
        addNotes(data, builder);
    }

    private static void addIntro(
        Map<String, String> data, PuzzleBuilder builder
    ) {
        String intro = data.get("exolve-preamble");
        if (intro == null) {
            intro = data.get("exolve-prelude");
            if (intro != null)
                intro = intro.trim();
        } else if (data.containsKey("exolve-prelude")) {
            intro = intro.trim();
            String prelude = data.get("exolve-prelude");
            if (prelude != null)
                intro += "\n\n" + prelude.trim();
        }
        builder.setIntroMessage(htmlString(intro));
    }

    private static void addNotes(
        Map<String, String> data, PuzzleBuilder builder
    ) {
        StringBuilder notes = new StringBuilder();

        String email = data.get("exolve-email");
        if (email != null)
            notes.append(email.trim());
        String credits = data.get("exolve-credits");
        if (credits != null) {
            if (notes.length() > 0)
                notes.append("\n\n");
            notes.append(credits.trim());
        }
        String maker = data.get("exolve-maker");
        if (maker != null) {
            if (notes.length() > 0)
                notes.append("\n\n");
            notes.append(maker.trim());
        }
        String questions = data.get("exolve-question");
        if (questions != null) {
            for (String question : questions.split("\n")) {
                if (notes.length() > 0)
                    notes.append("\n\n");
                int end = ExolveClue.getHintEnd(question);
                if (end < 0) {
                    notes.append(ExolveClue.parseHint(question));
                } else {
                    notes.append(ExolveClue.parseHint(
                        question.substring(0, end)
                    ));
                }
            }
        }

        if (notes.length() > 0)
            builder.setNotes(htmlString(notes.toString()));
    }

    private static ExolveBoxes getBoxes(
        Map<String, String> data
    ) throws ExolveFormatException {
        try {
            int height = Integer.valueOf(data.get("exolve-height"));
            int width = Integer.valueOf(data.get("exolve-width"));
            String grid = data.get("exolve-grid");
            if (grid == null)
                throw new ExolveFormatException("Missing grid field");

            // note, first line should be blank (content after :)
            String[] rows = grid.split("\n");
            if (rows.length - 1 < height) {
                throw new ExolveFormatException(
                    "Not enough grid rows (expected " + height + "): " + grid
                );
            }

            boolean hasDiagramless = false;
            Box[][] boxes = new Box[height][];
            Set<Position> skipNumberCells = new HashSet<>();
            for (int row = 0; row < height; row++) {
                String rowData = rows[row + 1].trim();
                List<String> cells = splitCells(data, rowData);
                BoxRow br = getBoxRow(cells, row, skipNumberCells);
                boxes[row] = br.getBoxes();
                hasDiagramless |= br.getHasDiagramless();
            }

            // no numbers if diagramless
            if (hasDiagramless) {
                for (int row = 0; row < height; row++)
                    for (int col = 0; col < width; col++)
                        skipNumberCells.add(new Position(row, col));
            }

            removeSolutionsIfZeros(boxes);

            return new ExolveBoxes(boxes, skipNumberCells);
        } catch (NumberFormatException e) {
            throw new ExolveFormatException("Bad width or height: " + e);
        }
    }

    /**
     * Remove solutions if they are all 0
     */
    private static void removeSolutionsIfZeros(Box[][] boxes) {
        // exit if we find a non-0
        for (int row = 0; row < boxes.length; row++) {
            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];
                if (box != null && !"0".equals(box.getSolution()))
                    return;
            }
        }

        // all 0, remove
        for (int row = 0; row < boxes.length; row++) {
            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];
                if (box != null)
                    box.setSolution(null);
            }
        }
    }

    /**
     * Split a row of the grid into cells
     *
     * Lots of options in the spec...
     */
    private static List<String> splitCells(
        Map<String, String> data, String rowData
    ) throws ExolveFormatException {
        if (splitOnSpaces(data)) {
            return Arrays.asList(rowData.split("\\s+"));
        } else {
            List<String> cells = new ArrayList<>();
            boolean newCell = true;
            // characters are joined to the next if the are an escaped
            // char (e.g. &*) or a unicode high/low surrogate
            boolean continues = false;

            for (int i = 0; i < rowData.length(); i++) {
                char next = rowData.charAt(i);

                if (continues) {
                    int last = cells.size() - 1;
                    cells.set(last, cells.get(last) + next);
                    newCell = false;
                    continues = false;
                } else if (
                    !continues
                    && (next == ESCAPE || Character.isHighSurrogate(next))
                ) {
                    cells.add("" + next);
                    continues = true;
                } else if (Character.isWhitespace(next)) {
                    newCell = true;
                } else if (newCell) {
                    cells.add("" + next);
                    newCell = false;
                } else if (CELL_MODIFIERS.contains(next)) {
                    int last = cells.size() - 1;
                    cells.set(last, cells.get(last) + next);
                    newCell = false;
                } else {
                    cells.add("" + next);
                    newCell = false;
                }
            }

            return cells;
        }
    }

    /**
     * True if grid must split on spaces
     */
    private static boolean splitOnSpaces(Map<String, String> data) {
        if (hasOption(data, "rebus-cells"))
            return true;

        String lang = data.get("exolve-language");
        if (lang == null)
            return false;

        String[] args = lang.split("\\s+");
        if (args.length >= 3) {
            try {
                return Integer.valueOf(args[2]) > 1;
            } catch (NumberFormatException e) {
                // fall through
            }
        } else if (args.length >= 2) {
            // Exolve hardcodes this one case..
            return "Devanagari".equalsIgnoreCase(args[1]);
        }

        return false;
    }

    /**
     * Convert a list of cell data into boxes
     *
     * @param cells list of cell strings to parse
     * @param row the row number (0 based)
     * @param skipNumberCells add all positions marked ~ to this set
     */
    private static BoxRow getBoxRow(
        List<String> cells,
        int row,
        Set<Position> skipNumberCells
    ) throws ExolveFormatException {
        Box[] boxes = new Box[cells.size()];

        boolean hasDiagramless = false;

        for (int col = 0; col < cells.size(); col++) {
            String cell = cells.get(col);
            if (!".".equals(cell)) {
                Box box = new Box();

                // plan remove all modifiers until left with solution
                // only

                // ignore first it means no solution
                cell = cell.replace("?", "");

                // circle
                if (hasUnescaped(cell, "@")) {
                    box.setShape(Box.Shape.CIRCLE);
                    cell = removeUnescaped(cell, "@");
                }

                // bar
                if (hasUnescaped(cell, "\\|")) {
                    box.setBarRight(Box.Bar.SOLID);
                    cell = removeUnescaped(cell, "\\|");
                }
                if (hasUnescaped(cell, "_")) {
                    box.setBarBottom(Box.Bar.SOLID);
                    cell = removeUnescaped(cell, "_");
                }
                if (hasUnescaped(cell, "\\+")) {
                    box.setBarRight(Box.Bar.SOLID);
                    box.setBarBottom(Box.Bar.SOLID);
                    cell = removeUnescaped(cell, "\\+");
                }

                // skip number
                if (hasUnescaped(cell, "~")) {
                    skipNumberCells.add(new Position(row, col));
                    cell = removeUnescaped(cell, "~");
                }

                boolean diagramless = false;
                if (hasUnescaped(cell, "\\*")) {
                    diagramless = true;
                    hasDiagramless = true;
                    cell = removeUnescaped(cell, "\\*");
                }

                boolean prefilled = hasUnescaped(cell, "!");
                cell = removeUnescaped(cell, "!");

                // remove escaping
                cell = cell.replaceAll(ESCAPE + "(.)", "$1");

                if (!cell.isEmpty()) {
                    // "diagramless" -- not really supported, but make 0*
                    // into a cell with an empty solution
                    if ((".".equals(cell) || "0".equals(cell)) && diagramless) {
                        box.setSolution(Box.BLANK);
                    } else {
                        box.setSolution(cell);
                        if (prefilled)
                            box.setInitialValue(cell);
                    }
                }

                boxes[col] = box;
            }
        }

        return new BoxRow(boxes, hasDiagramless);
    }

    /**
     * If c is in cell without preceding &
     *
     * @param c char, escaped for regex, to look for
     */
    private static boolean hasUnescaped(String cell, String c) {
        return cell.matches(".*(?<!" + ESCAPE + ")" + c + ".*");
    }

    /**
     * Remove first unescaped instance of
     *
     * @param c char, escaped for regex, to remove (e.g. "\\.")
     */
    private static String removeUnescaped(String cell, String c) {
        return cell.replaceFirst("(?<!" + ESCAPE + ")" + c, "");
    }

    /**
     * True if a puzzle option is set
     */
    private static boolean hasOption(
        Map<String, String> data, String option
    ) {
        String opts = data.get("exolve-option");
        if (opts == null)
            return false;
        return opts.contains(option);
    }

    private static String htmlString(String s) {
        return HtmlUtil.htmlString(s);
    }

    private static class ExolveClue {
        // see exolve def of enums
        private static final Pattern CLUE_UPTO_ENUM_RE
            = Pattern.compile(
                "(\\(\\d[\\d-,'\\s.]*\\)"
                    + "|\\([^)]*(w|l)\\w*[^)]*\\)"
                    + "|\\([^)]*\\?\\))\\*?",
                Pattern.CASE_INSENSITIVE
            );

        // Eg. 1, 1a, or [ONE]
        private static final String ONE_NUMBER_RE
            = "(?:\\d+[adbu]?|\\[[^\\]]+\\])";
        private static final Pattern CLUE_PARTS_RE
            = Pattern.compile(
                // zone
                "\\s*((?:(?:\\#c\\d+r\\d+|\\#r\\d+c\\d+|\\#[a-z]\\d+)\\s*)*)"
                // number
                + "\\s*(" + ONE_NUMBER_RE + ")"
                // extra numbers (and start of hint text)
                + "(((?:\\s*[,&]\\s*" + ONE_NUMBER_RE + ")*)"
                // rest of hint (may include an initial . or spaces to
                // remove later)
                + ".*)"
            );
        private static final int CLUE_PART_ZONE_GRP = 1;
        private static final int CLUE_PART_NUMBER_GRP = 2;
        private static final int CLUE_PART_EXTRA_NUMBERS_GRP = 4;
        private static final int CLUE_PART_HINT_GRP = 3;

        private static final Pattern COL_ROW_COORD_RE
            = Pattern.compile("\\#?c(\\d+)r(\\d+)");
        private static final int COL_ROW_COORD_ROW_GRP = 2;
        private static final int COL_ROW_COORD_COL_GRP = 1;
        private static final Pattern ROW_COL_COORD_RE
            = Pattern.compile("\\#?r(\\d+)c(\\d+)");
        private static final int ROW_COL_COORD_ROW_GRP = 1;
        private static final int ROW_COL_COORD_COL_GRP = 2;
        private static final Pattern LETTER_NUM_COORD_RE
            = Pattern.compile("\\#?([a-z])(\\d+)");
        private static final int LETTER_NUM_COORD_ROW_GRP = 2;
        private static final int LETTER_NUM_COORD_COL_GRP = 1;
        private static final String ANNOTATION_MARKER = "[]";

        private Zone zone;
        private String number;
        private String[] extraNumbers;
        private String hint;
        private String annotation;

        public ExolveClue(
            Zone zone,
            String number,
            String[] extraNumbers,
            String hint,
            String annotation
        ) {
            this.zone = zone;
            this.number = number;
            this.extraNumbers = extraNumbers;
            this.hint = hint;
            this.annotation = annotation;
        }

        /**
         * Zone has an ununsual meaning
         *
         * May be null if no zone specified.
         * A zone may be only 1 box, in which case it is the start box
         * of an across/down clue.
         * Else it's the full zone.
         * Only has zone for first number, rest may need to be appended
         * later.
         */
        public Zone getZone() { return zone; }
        public void setZone(Zone zone) { this.zone = zone; }
        public String getNumber() { return number; }
        /**
         * Any extra numbers where part of the answer lies (or null)
         */
        public String[] getExtraNumbers() { return extraNumbers; }
        public String getHint() { return hint; }
        public String getAnnotation() { return annotation; }

        /**
         * Parse a clue line
         *
         * Puzzle height needed because rows go from the bottom
         */
        public static ExolveClue parseClueLine(
            String line, int height
        ) throws ExolveFormatException{
            if (line == null)
                return null;

            String clue = line;

            // First peel off annotation before parsing clue
            String annotation = null;
            int hintEnd = getHintEnd(line);
            if (hintEnd >= 0) {
                annotation = line.substring(hintEnd).trim();
                if (annotation.startsWith(ANNOTATION_MARKER)) {
                    annotation = annotation
                        .substring(ANNOTATION_MARKER.length())
                        .trim();
                }
                clue = line.substring(0, hintEnd).trim();
            }

            Matcher m = CLUE_PARTS_RE.matcher(clue);
            if (m.matches()) {
                String zoneString = m.group(CLUE_PART_ZONE_GRP);
                String number = unwrapNumber(m.group(CLUE_PART_NUMBER_GRP));
                String extraNumbersString
                    = m.group(CLUE_PART_EXTRA_NUMBERS_GRP);
                String hint = parseHint(m.group(CLUE_PART_HINT_GRP));

                String[] extraNumbers = parseNumbers(extraNumbersString);
                Zone zone = parseZone(zoneString, height);

                return new ExolveClue(
                    zone, number, extraNumbers, hint, annotation
                );
            } else {
                // unparsable, return a "dummy" clue for display in clue
                // list
                return new ExolveClue(
                    null, null, null, line, null
                );
            }
        }

        private static String parseHint(String hint) {
            hint = hint.trim();
            // remove initial .
            if (hint.startsWith(".") && !hint.startsWith("..")) {
                hint = hint.substring(1).trim();
            }
            // remove enum if to be hidden
            if (hint.endsWith("*")) {
                int enumStart = hint.lastIndexOf("(");
                if (enumStart >= 0) {
                    hint = hint.substring(0, enumStart).trim();
                }
            }
            // underline bits
            return hint.replaceAll(
                "~\\{(\\{[^}]*\\})?([^}]*)\\}~",
                "<u>$2</u>"
            );
        }

        /**
         * Parse zone of clue
         *
         * Height needed for position parsing.
         */
        private static Zone parseZone(
            String zoneString, int height
        ) throws ExolveFormatException {
            if (zoneString == null || zoneString.trim().isEmpty())
                return null;

            Zone zone = new Zone();
            for (String cell : zoneString.split("\\s+")) {
                zone.addPosition(parsePosition(cell, height));
            }

            return zone;
        }

        /**
         * Parse a position string
         *
         * E.g. #r10c13 or #a11. Throws error if not parseable.
         *
         * puzzle height needed since row specified from bottom
         */
        private static Position parsePosition(
            String pos, int height
        ) throws ExolveFormatException {
            Matcher m = ROW_COL_COORD_RE.matcher(pos);
            if (m.matches()) {
                try {
                    int row = Integer.valueOf(m.group(ROW_COL_COORD_ROW_GRP));
                    int col = Integer.valueOf(m.group(ROW_COL_COORD_COL_GRP));
                    return new Position(height - row, col - 1);
                } catch (NumberFormatException e) {
                    throw new ExolveFormatException("Bad position: " + pos);
                }
            }

            m = COL_ROW_COORD_RE.matcher(pos);
            if (m.matches()) {
                try {
                    int row = Integer.valueOf(m.group(COL_ROW_COORD_ROW_GRP));
                    int col = Integer.valueOf(m.group(COL_ROW_COORD_COL_GRP));
                    return new Position(height - row, col - 1);
                } catch (NumberFormatException e) {
                    throw new ExolveFormatException("Bad position: " + pos);
                }
            }

            m = LETTER_NUM_COORD_RE.matcher(pos);
            if (m.matches()) {
                try {
                    int row = Integer.valueOf(
                        m.group(LETTER_NUM_COORD_ROW_GRP)
                    );
                    String colLetter = m.group(LETTER_NUM_COORD_COL_GRP);
                    if (colLetter.length() != 1) {
                        throw new ExolveFormatException("Bad position: " + pos);
                    }
                    int col = colLetter.charAt(0) - 'a';
                    return new Position(height - row, col);
                } catch (NumberFormatException e) {
                    throw new ExolveFormatException("Bad position: " + pos);
                }
            }

            throw new ExolveFormatException("Bad position: " + pos);
        }

        private static String unwrapNumber(String number) {
            number = number.trim();
            if (number.startsWith("[") && number.endsWith("]"))
                return number.substring(1, number.length() - 1);
            else
                return number;
        }

        private static String[] parseNumbers(String numbersString) {
            if (numbersString == null || numbersString.isEmpty())
                return null;

            String[] numbers = numbersString.split("[&,]");
            for (int i = 0; i < numbers.length; i++)
                numbers[i] = unwrapNumber(numbers[i]);
            return numbers;
        }

        /**
         * Find the end of the hint in the clue list
         *
         * I.e. [] or enumeration. [] will be after the clueEnd if it
         * was used.
         *
         * @return -1 if none found
         */
        private static int getHintEnd(String clue) {
            // split off annotations and possibly enum
            int clueEnd = clue.indexOf(ANNOTATION_MARKER);
            if (clueEnd >= 0)
                return clueEnd;

            clueEnd = -1;

            Matcher m = CLUE_UPTO_ENUM_RE.matcher(clue);
            while (m.find()) {
                clueEnd = m.end();
            }

            return clueEnd;
        }
    }

    private static class ExolveClueLists {
        private ExolveClueList across;
        private ExolveClueList down;
        private ExolveClueList nodir;

        public ExolveClueLists(
            ExolveClueList across,
            ExolveClueList down,
            ExolveClueList nodir
        ) {
            this.across = across;
            this.down = down;
            this.nodir = nodir;
        }

        public ExolveClueList getAcross() { return across; }
        public ExolveClueList getDown() { return down; }
        public ExolveClueList getNodir() { return nodir; }
    }

    private static class ExolveClueList {
        private String title;
        private List<ExolveClue> clues;

        public ExolveClueList(String title, List<ExolveClue> clues) {
            this.title = title;
            this.clues = clues;
        }

        public String getTitle() { return title; }
        public List<ExolveClue> getClues() { return clues; }
    }

    private static class ExolveBoxes {
        private Box[][] boxes;
        private Set<Position> skipNumberCells;

        public ExolveBoxes(Box[][] boxes, Set<Position> skipNumberCells) {
            this.boxes = boxes;
            this.skipNumberCells = skipNumberCells;
        }

        public Box[][] getBoxes() { return boxes; }
        public Set<Position> getSkipNumberCells() { return skipNumberCells; }
    }

    private static class Reversal {
        Position start;
        Position end;

        public Reversal(Position start, Position end) {
            this.start = start;
            this.end = end;
        }

        public Position getStart() { return start; }
        public Position getEnd() { return end; }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Reversal))
                return false;

            Reversal o = (Reversal) other;
            return Objects.equals(start, o.start)
                && Objects.equals(end, o.end);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }

    private static class BoxRow {
        private Box[] boxes;
        private boolean hasDiagramless;

        public BoxRow(Box[] boxes, boolean hasDiagramless) {
            this.boxes = boxes;
            this.hasDiagramless = hasDiagramless;
        }

        public Box[] getBoxes() { return boxes; }
        public boolean getHasDiagramless() { return hasDiagramless; }
    }
}
