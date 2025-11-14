package app.crossword.yourealwaysbe.puz.io;

import java.io.InputStream;
import java.util.ArrayList;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;

/**
 * Parser specifically for the "In Het Midden" crossword of the NRC newspaper.
 *
 * The difference with a normal crossword is that there is a special horizontal word which has to be
 * found (the "middenwoord"). The affected cells are highlighted in the input (using a "#" sign).
 * This class injects the middenwoord as an additional clue that can be filled out. This is not
 * strictly necessary as it is made up from the letters of other (vertical) words, but it is more
 * satisfactory for puzzlers who figure the middenwoord out from partial information when they can
 * actually fill it out.
 * The middenwoord might be broken up in multiple words, separated by a highlighted block. In this
 * case, all middenwoord words are added as separate clues.
 */
public class NRCInHetMiddenIO extends FortyTwoPuzzlesIO {
    private boolean inMiddenWoord = false;
    private ArrayList<Box> middenWoordStartBoxes = new ArrayList<>();

    public String middenwoordHint = "MIDDENWOORD";

    public NRCInHetMiddenIO() {
        super("In het midden", "NRC");
    }

    @Override
    protected PuzzleBuilder parsePuzzle(InputStream is) throws Exception {
        PuzzleBuilder builder = super.parsePuzzle(is);

        // If there's just one middenwoord woord, relabel the clue from "m1" to just "m".
        if (middenWoordStartBoxes.size() == 1 && middenWoordStartBoxes.get(0).getClueNumber().equals("m1")) {
            Position pos = builder.getNumberPositions().get("m1");
            builder.setBoxClueNumber(pos.getRow(), pos.getCol(), "m");
        }

        // Inject the middenwoord clues
        for (Box box: middenWoordStartBoxes) {
            PuzzleBuilder.BasicClue clue = new PuzzleBuilder.BasicClue(box.getClueNumber(), middenwoordHint);
            builder.addAcrossClue(DefaultListNames.ACROSS_LIST, clue);
        }

        return builder;
    }

    @Override
    protected Box createBox(String value, String solution) {
        boolean isMiddenWoord = value.startsWith("#"); // 42games marks the "middenwoord" as highlighted boxes, using a # sign

        Box box = super.createBox(value, solution);

        if (inMiddenWoord && box.isBlock()) {
            // We encountered a block inside the middenwoord, which means we're in a situation where
            // the middenwoord consists of multiple words, and we just left one of them. So mark
            // that we're not in the middenwoord (anymore), so the next cycle detects that we
            // entered the middenwoord.
            isMiddenWoord = false;
            inMiddenWoord = false;
        }

        if (isMiddenWoord) {
            // If this is the first cell of the middenwoord and there is no clue number assigned
            // from a vertical word, we have to assign it a special clue "number", which we'll
            // label "m" plus the word number.
            if (!inMiddenWoord) {
                middenWoordStartBoxes.add(box);
                if (box.getClueNumber().isEmpty()) {
                    box.setClueNumber("m" + middenWoordStartBoxes.size());
                }
            }
            inMiddenWoord = true;
        }

        return box;
    }
}
