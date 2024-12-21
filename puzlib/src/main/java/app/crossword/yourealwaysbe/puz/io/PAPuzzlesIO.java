
package app.crossword.yourealwaysbe.puz.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.Zone;

/**
 * Read crosswords from https://www.pa-puzzles.com
 *
 * URL e.g. https://www.pa-puzzles.com/puzzles/puzzlegroupembed.php?pid=22&cs=55
 *
 * Format is HTML, 1-based indices.
 *
 * <div id="puzzle-grid" data-x="{width}"...>
 *   <div class="row">
 *     <div id="cell-{col}-{row}" class="cell (blank)"> [Blank Cell]
 *     <div id="cell-{col}-{row}" class="cell" data-clueId="{clueId}">
 *       <span class="cell-number">{number}</span>
 *       <div class="cell-input" data-solution="{solution}"/>
 *     </div>
 *     ...
 *   </div>
 *   ...
 * </div>
 * <div class="clues">
 *   ...
 *   <div class="my-content-desktop-{across|down}">
 *       <li id="{clueId}"
 *           data-direction="{across|down}"
 *           data-cluenum="{number}"
 *           data-islink="{0/1}"
 *       ><b>{number}</b> {hint}</li>
 *   </div>
 *   ...
 * </div>
 *
 * Split clues have e.g. {direction1}-{direction2} as the direction and
 * {num1}/{num2} as the number. data-islink for the second clue will be
 * 1. Else 0.
 *
 * There are two copies of the clues my-content-desktop-* and
 * my-content-mobile-*.
 */
public class PAPuzzlesIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger(PAPuzzlesIO.class.getCanonicalName());

    private static final Pattern CELL_ID_RE
        = Pattern.compile("cell-(\\d+)-(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final int CELL_ID_COL_GRP = 1;
    private static final int CELL_ID_ROW_GRP = 2;

    private static final Pattern CLUE_HINT_RE
        = Pattern.compile("\\s*[\\d/]+\\s*(.*)");
    private static final int CLUE_HINT_GRP = 1;

    /**
     * An unfancy exception indicating error while parsing
     */
    public static class PAPuzzlesFormatException extends Exception {
        private static final long serialVersionUID = 7552858732734513L;
        public PAPuzzlesFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            Document doc = Jsoup.parse(is, null, "");

            PuzzleBuilder builder
                = new PuzzleBuilder(getBoxes(doc));

            getClues(doc, builder);

            return builder.getPuzzle();
        } catch (IOException | PAPuzzlesFormatException e) {
            LOG.info("Could not read PA Puzzles crossword: " + e);
            return null;
        }
    }

    /**
     * Read boxes from HTML
     *
     * Given a map from clueID to clue number
     */
    private static Box[][] getBoxes(Element doc)
            throws PAPuzzlesFormatException {

        Element grid = doc.selectFirst("#puzzle-grid");
        if (grid == null)
            throw new PAPuzzlesFormatException("No #puzzle-grid found.");

        int maxRow = 0;
        int maxCol = 0;

        Map<Position, Box> boxMap = new HashMap<>();

        for (Element row : grid.select(".row")) {
            for (Element cell : row.select(".cell")) {
                if (cell.hasClass("blank"))
                    continue;

                Position pos = getCellPosition(cell);
                String number = getCellNumber(cell);
                String solution = getCellSolution(cell);
                if (pos != null) {
                    Box box = new Box();
                    if (number != null)
                        box.setClueNumber(number);
                    if (solution != null)
                        box.setSolution(solution);
                    boxMap.put(pos, box);

                    maxRow = Math.max(pos.getRow(), maxRow);
                    maxCol = Math.max(pos.getCol(), maxCol);
                }
            }
        }

        Box[][] boxes = new Box[maxRow + 1][maxCol + 1];
        for (int row = 0; row <= maxRow; row++) {
            for (int col = 0; col <= maxCol; col++) {
                Position pos = new Position(row, col);
                boxes[row][col] = boxMap.get(pos);
            }
        }

        return boxes;
    }

    /**
     * Parse position from cell id
     *
     * E.g. cell-5-3 for col 4 row 2
     */
    private static Position getCellPosition(Element cell) {
        Matcher m = CELL_ID_RE.matcher(cell.id());
        if (m.matches()) {
            return new Position(
                Integer.valueOf(m.group(CELL_ID_ROW_GRP)) - 1,
                Integer.valueOf(m.group(CELL_ID_COL_GRP)) - 1
            );
        } else {
            return null;
        }
    }

    /**
     * Get clue number in cell if there is one else null
     */
    private static String getCellNumber(Element cell) {
        if (cell ==  null)
            return null;

        Element number = cell.selectFirst(".cell-number");
        return number == null ? null : number.text().trim();
    }

    /**
     * Get solution for cell if there is one
     */
    private static String getCellSolution(Element cell) {
        if (cell == null)
            return null;

        Element input = cell.selectFirst(".cell-input");
        if (input == null)
            return null;

        String solution = input.attr("data-solution");
        return solution.isEmpty() ? null : solution;
    }

    private static void getClues(Element doc, PuzzleBuilder builder) {
        getClues(doc, "clues-across", builder);
        getClues(doc, "clues-down", builder);
    }

    /**
     * Get clues from a specific list
     *
     * @param doc the doc to get from
     * @param listClass the class name identifying the list
     * @param builder add clues to this builder
     */
    private static void getClues(
        Element doc, String listClass, PuzzleBuilder builder
    ) {
        Element clueList = doc.selectFirst(".clues ." + listClass);
        if (clueList == null)
            return;

        for (Element clue : clueList.select("li")) {
            String number = clue.attr("data-cluenum");
            String[] numbers = number.split("/");
            if (numbers.length == 0)
                continue;

            String[] directions = clue.attr("data-direction").split("-");
            if (directions.length == 0)
                continue;

            String hint = getClueHint(clue);
            if (hint == null)
                continue;

            int numClues = Math.min(numbers.length, directions.length);
            Zone zone = new Zone();
            String listName = null;

            String islink = clue.attr("data-islink");
            if (!"1".equals(clue.attr("data-islink"))) {
                for (int i = 0; i < numClues; i++) {
                    if ("across".equalsIgnoreCase(directions[i])) {
                        if (i == 0)
                            listName = "Across";
                        zone.appendZone(builder.getAcrossZone(numbers[i]));
                    } else if ("down".equalsIgnoreCase(directions[i])) {
                        if (i == 0)
                            listName = "Down";
                        zone.appendZone(builder.getDownZone(numbers[i]));
                    }
                }
            }

            if (listName != null) {
                int index = builder.getNextClueIndex(listName);
                builder.addClue(new Clue(listName, index, number, hint, zone));
            }
        }
    }

    private static String getClueHint(Element clue) {
        if (clue == null)
            return null;

        // don't keep HTML since it can contain odd things like flex
        // divs
        Matcher m = CLUE_HINT_RE.matcher(clue.text());
        return m.matches() ? m.group(CLUE_HINT_GRP).trim() : null;
    }
}
