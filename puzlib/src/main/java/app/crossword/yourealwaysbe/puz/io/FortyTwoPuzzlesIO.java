
package app.crossword.yourealwaysbe.puz.io;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.util.HtmlUtil;
import app.crossword.yourealwaysbe.puz.util.JSONParser;

/**
 * Parser for puzzles distributed by 42puzzles.com.
 *
 * The format is in JSON, and contains a bit more than just the puzzle. The
 * puzzle itself is found under the "puzzle" key, which is akin to IPuz, but
 * slightly different, so we can't use it directly.
 */
public class FortyTwoPuzzlesIO extends AbstractJSONIO {
    private JSONObject srcPuzzle;
    private int width;
    private int height;
    private String title;
    private String author;

    public FortyTwoPuzzlesIO() {
        this(null, null);
    }

    public FortyTwoPuzzlesIO(String title, String author) {
        this.title = title;
        this.author = author;
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return parsePuzzle(is).getPuzzle();
    }

    protected PuzzleBuilder parsePuzzle(InputStream is) throws Exception {
        ByteArrayInputStream copy = StreamUtils.makeByteArrayInputStream(is);
        JSONObject json;
        try {
            json = JSONParser.parse(copy);
        } catch (IOException | JSONException e) {
            return null;
        }

        srcPuzzle = json.getJSONObject("puzzle");

        width = srcPuzzle.getInt("width");
        height = srcPuzzle.getInt("height");

        Box[] boxes = parseBoxes();
        PuzzleBuilder builder = new PuzzleBuilder(boxes, width, height);
        parseClues(
            builder,
            srcPuzzle.getJSONObject("hclues"),
            DefaultListNames.ACROSS_LIST
        );
        parseClues(
            builder,
            srcPuzzle.getJSONObject("vclues"),
            DefaultListNames.DOWN_LIST
        );

        setMetadata(builder);

        return builder;
    }

    protected Box[] parseBoxes() {
        JSONArray srcValues = srcPuzzle.getJSONArray("values");
        JSONArray srcSolutions = srcPuzzle.getJSONArray("solution");
        Box[] boxes = new Box[width * height];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int index = row * width + col;
                // Might be int, might be string, so convert it explicitly to
                // string
                String value = srcValues.get(index).toString();
                String solution = srcSolutions.getString(index);
                boxes[index] = createBox(value, solution);
            }
        }

        return boxes;
    }

    protected Box createBox(String value, String solution) {
        Box box = new Box();

        // 42puzzles seems to use the # sign as a mark that boxes should be
        // highlighted. This mark is prepended to whatever else is in the value
        // string, so it needs to be cut off for further processing.
        boolean isHighlighted = false;
        if (value.startsWith("#")) {
            isHighlighted = true;
            value = value.replace("#", "");
        }

        if (value.equals("@")) {
            box.setBlock(true);
        } else if (value.equals("·")) {
            box.setBlock(true);
        } else {
            box.setClueNumber(value);
        }

        if (isHighlighted) {
            if (box.isBlock()) {
                box.setColor(HtmlUtil.parseHtmlColor("#fafa33"));
            } else {
                box.setColor(HtmlUtil.parseHtmlColor("#fffaa0"));
            }
        }

        if (!solution.equals("@")) {
            // 42games uses the Dutch "Ĳ "digram which is not supported in
            // general. It is common in crossword puzzles to use the "Y" in
            // place.
            solution = solution.replace("Ĳ", "Y");

            box.setSolution(solution);
        }

        return box;
    }

    protected void parseClues(
        PuzzleBuilder builder,
        JSONObject srcClues,
        String listName
    ) {
        // The JSONObject returns its keys in random order. Since we know that
        // all keys are numbers, we can convert them to actual numbers and sort
        // them this way.
        List<Integer> clueNums = srcClues.keySet()
            .stream()
            .map(Integer::parseInt)
            .sorted()
            .toList();
        for (Integer clueNum : clueNums) {
            String clueStr = clueNum.toString();
            PuzzleBuilder.BasicClue clue = new PuzzleBuilder.BasicClue(
                clueStr, srcClues.getString(clueStr)
            );
            if (listName.equals(DefaultListNames.ACROSS_LIST)) {
                builder.addAcrossClue(listName, clue);
            } else {
                builder.addDownClue(listName, clue);
            }
        }
    }

    protected void setMetadata(PuzzleBuilder builder) {
        builder.setTitle(title);
        builder.setAuthor(author);
        String dateStr = srcPuzzle.optString("date");
        if (dateStr != null) {
            LocalDate date = LocalDate.parse(
                dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy")
            );
            builder.setDate(date);
        }
    }
}
