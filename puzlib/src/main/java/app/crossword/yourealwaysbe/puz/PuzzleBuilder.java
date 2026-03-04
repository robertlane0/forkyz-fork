
package app.crossword.yourealwaysbe.puz;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.iharder.Base64;

import app.crossword.yourealwaysbe.puz.io.StreamUtils;
import app.crossword.yourealwaysbe.puz.util.PuzzleUtils;

public class PuzzleBuilder {
    private final Puzzle puzzle = new Puzzle();
    private Map<String, Position> numberPositions;

    /**
     * Convenience class for partial clues
     */
    public static class BasicClue {
        private String num, hint;

        public BasicClue(String num, String hint) {
            this.num = num;
            this.hint = hint;
        }

        public String getNum() { return num; }
        public String getHint() { return hint; }
    }

    public PuzzleBuilder(Box[][] boxes) {
        puzzle.setBoxes(boxes);
    }

    public PuzzleBuilder(Box[] boxesList, int width, int height) {
        int i = 0;
        Box[][] boxes = new Box[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boxes[y][x] = boxesList[i++];
            }
        }
        puzzle.setBoxes(boxes);
    }

    /**
     * Provides access to puzzle methods directly
     */
    public Puzzle getPuzzle() { return puzzle; }
    public int getWidth() { return puzzle.getWidth(); }
    public int getHeight() { return puzzle.getHeight(); }
    public Iterable<ClueID> getBoardClueIDs() {
        return puzzle.getBoardClueIDs();
    }

    public Box getBox(int row, int col) {
        return puzzle.checkedGetBox(row, col);
    }

    public Box getBox(Position position) {
        return puzzle.checkedGetBox(position);
    }

    public PuzzleBuilder setBoxClueNumber(int row, int col, String number) {
        Box box = puzzle.checkedGetBox(row, col);
        if (!Box.isBlock(box)) {
            box.setClueNumber(number);
            invalidateNumberPosition();
        }
        return this;
    }

    /**
     * Add a clue to the puzzle
     *
     * Will check the zone matches the boxes and delete any positions
     * in the zone that it finds are not valid (e.g. blocks can't be
     * part of clue positions.
     */
    public PuzzleBuilder addClue(Clue clue) {
        Clue cleanClue = sanitizeClueZone(clue);
        puzzle.addClue(cleanClue);
        return this;
    }

    /**
     * Removed bad positions from clue if needed
     *
     * Returns original clue object if all good, else a clone with bad
     * zone positions removed.
     */
    private Clue sanitizeClueZone(Clue clue) {
        Set<Position> badPositions = new HashSet<>();
        for (Position pos : clue.getZone()) {
            if (Box.isBlock(getBox(pos)))
                badPositions.add(pos);
        }

        if (badPositions.isEmpty()) {
            return clue;
        } else {
            Zone newZone = new Zone();
            for (Position pos : clue.getZone()) {
                if (!badPositions.contains(pos))
                    newZone.addPosition(pos);
            }
            return new Clue(
                clue.clueID.getListName(),
                clue.clueID.getIndex(),
                clue.getClueNumber(),
                clue.getLabel(),
                clue.getHint(),
                newZone
            );
        }
    }

    public PuzzleBuilder setSolutionChecksum(short checkSum) {
        puzzle.setSolutionChecksum(checkSum);
        return this;
    }

    public PuzzleBuilder setScrambled(boolean scrambled) {
        puzzle.setScrambled(scrambled);
        return this;
    }

    public PuzzleBuilder setTitle(String title) {
        puzzle.setTitle(title);
        return this;
    }

    public PuzzleBuilder setAuthor(String author) {
        puzzle.setAuthor(author);
        return this;
    }

    public PuzzleBuilder setCopyright(String copyright) {
        puzzle.setCopyright(copyright);
        return this;
    }

    public PuzzleBuilder setNotes(String notes) {
        puzzle.setNotes(notes);
        return this;
    }

    public PuzzleBuilder setIntroMessage(String introMessage) {
        puzzle.setIntroMessage(introMessage);
        return this;
    }

    public PuzzleBuilder setCompletionMessage(String completionMessage) {
        puzzle.setCompletionMessage(completionMessage);
        return this;
    }

    public PuzzleBuilder setRating(char rating) {
        puzzle.setRating(rating);
        return this;
    }

    public PuzzleBuilder setPlayerNote(Note playerNote) {
        puzzle.setPlayerNote(playerNote);
        return this;
    }

    public PuzzleBuilder setNote(ClueID cid, Note note) {
        puzzle.setNote(cid, note);
        return this;
    }

    public PuzzleBuilder setDate(LocalDate date) {
        puzzle.setDate(date);
        return this;
    }

    public PuzzleBuilder setSource(String source) {
        puzzle.setSource(source);
        return this;
    }

    public PuzzleBuilder setSourceUrl(String sourceUrl) {
        puzzle.setSourceUrl(sourceUrl);
        return this;
    }

    public PuzzleBuilder setSupportUrl(String supportUrl) {
        puzzle.setSupportUrl(supportUrl);
        return this;
    }

    public PuzzleBuilder setShareUrl(String shareUrl) {
        puzzle.setShareUrl(shareUrl);
        return this;
    }

    public PuzzleBuilder setTime(long time) {
        puzzle.setTime(time);
        return this;
    }

    public PuzzleBuilder flagClue(ClueID clueId, boolean flag) {
        flagClue(clueId, flag, Clue.DEFAULT_FLAG_COLOR);
        return this;
    }

    public PuzzleBuilder flagClue(
        ClueID clueId, boolean flag, int flagColor
    ) {
        Clue clue = clueId == null ? null : puzzle.getClue(clueId);
        if (clue != null) {
            clue.setFlagged(flag);
            clue.flagColor = flagColor;
        }
        return this;
    }

    public PuzzleBuilder setPosition(Position position) {
        puzzle.setPosition(position);
        return this;
    }

    public PuzzleBuilder setCurrentClueID(ClueID clueId) {
        puzzle.setCurrentClueID(clueId);
        return this;
    }

    public PuzzleBuilder setHistory(List<ClueID> history) {
        puzzle.setHistory(history);
        return this;
    }

    public PuzzleBuilder addImage(PuzImage image) {
        puzzle.addImage(image);
        return this;
    }

    public PuzzleBuilder setPinnedClueID(ClueID pinnedClueID) {
        puzzle.setPinnedClueID(pinnedClueID);
        return this;
    }

    public PuzzleBuilder setKind(Puzzle.Kind kind) {
        puzzle.setKind(kind);
        return this;
    }

    /**
     * Number puzzle boxes according to standard system
     *
     * @throws IllegalArgumentException if mismatch with existing
     * numbers
     */
    public PuzzleBuilder autoNumberBoxes() throws IllegalArgumentException {
        return autoNumberBoxes(Collections.<Position>emptySet());
    }

    /**
     * Number puzzle boxes according to standard system
     *
     * @param skipNumberCells cells not to auto number. They are skipped
     * and the number goes to the next standard cell to number.
     * @throws IllegalArgumentException if mismatch with existing
     * numbers
     */
    public PuzzleBuilder autoNumberBoxes(
        Set<Position> skipNumberCells
    ) throws IllegalArgumentException {
        int clueCount = 1;

        for (int row = 0; row < puzzle.getHeight(); row++) {
            for (int col = 0; col < puzzle.getWidth(); col++) {
                Position pos = new Position(row, col);
                Box box = puzzle.checkedGetBox(pos);

                if (Box.isBlock(box)) {
                    continue;
                }

                boolean isStart = isStartClue(row, col, true)
                    || isStartClue(row, col, false);

                String boxNumber = box.getClueNumber();
                String autoNumber = String.valueOf(clueCount);

                if (isStart && !skipNumberCells.contains(pos)) {
                    if (boxNumber != null && !boxNumber.equals(autoNumber)) {
                        throw new IllegalArgumentException(
                            "Box clue number " + boxNumber
                                + " does not match expected "
                                + clueCount
                        );
                    }

                    box.setClueNumber(autoNumber);
                    clueCount++;
                } else {
                    if (boxNumber != null) {
                        throw new IllegalArgumentException(
                            "Box at row " + row
                                + " and col " + col
                                + " numbered " + boxNumber
                                + " expected not to be numbered"
                        );
                    }
                }
            }
        }

        return this;
    }

    /**
     * The next contiguous index for a clue in listName
     */
    public int getNextClueIndex(String listName) {
        ClueList clues = puzzle.getClues(listName);
        return clues == null ? 0 : clues.size();
    }

    /**
     * Get standard across zone for clue with number
     *
     * Returns null if number not found
     */
    public Zone getAcrossZone(String number) {
        Position start = getNumberPositions().get(number);
        return getAcrossZone(start);
    }

    /**
     * Get standard across zone from a position
     */
    public Zone getAcrossZone(Position start) {
        return (start != null)
            ? PuzzleUtils.getAcrossZone(puzzle, start)
            : null;
    }

    /**
     * Get standard down zone for clue with number
     *
     * Returns null if number not found
     */
    public Zone getDownZone(String number) {
        Position start = getNumberPositions().get(number);
        return getDownZone(start);
    }

    /**
     * Get standard down zone from a position
     */
    public Zone getDownZone(Position start) {
        return (start != null)
            ? PuzzleUtils.getDownZone(puzzle, start)
            : null;
    }

    public PuzzleBuilder addAcrossClue(String listName, BasicClue basicClue) {
        return addAcrossClue(
            listName,
            basicClue.getNum(),
            basicClue.getHint()
        );
    }

    public PuzzleBuilder addAcrossClue(
        String listName, String number, String hint
    ) {
        return addAcrossClue(listName, number, null, hint);
    }

    public PuzzleBuilder addAcrossClue(
        String listName, String number, String label, String hint
    ) {
        int index = getNextClueIndex(listName);
        Zone zone = getAcrossZone(number);

        puzzle.addClue(new Clue(listName, index, number, label, hint, zone));

        return this;
    }

    public PuzzleBuilder addDownClue(String listName, BasicClue basicClue) {
        return addDownClue(
            listName,
            basicClue.getNum(),
            basicClue.getHint()
        );
    }

    public PuzzleBuilder addDownClue(
        String listName, String number, String hint
    ) {
        return addDownClue(listName, number, null, hint);
    }

    public PuzzleBuilder addDownClue(
        String listName, String number, String label, String hint
    ) {
        int index = getNextClueIndex(listName);
        Zone zone = getDownZone(number);

        puzzle.addClue(
            new Clue(listName, index, number, label, hint, zone)
        );

        return this;
    }

    /**
     * True if box is start of clue in standard numbering system
     *
     * Regardless of whether there is actually a clue in the puzzle
     */
    public boolean isStartClue(int row, int col, boolean across) {
        if (across) {
            return !joinedLeft(row, col) && joinedRight(row, col);
        } else {
            return !joinedTop(row, col) && joinedBottom(row, col);
        }
    }

    /**
     * Replace URLs of images with base64 data
     *
     * Downloads images, relative to relativeUrl (i.e. image in file at
     * relativeUrl).
     */
    public void resolveImages(String relativeUrl) {
        resolveImages(getPuzzle(), relativeUrl);
    }

    public static void resolveImages(Puzzle puz, String relativeUrl) {
        for (PuzImage image : puz.getImages()) {
            String imgUrl = image.getURL();
            if (imgUrl == null)
                continue;
            if (
                imgUrl.length() >= 6
                && imgUrl.substring(0, 5).equalsIgnoreCase("data:")
            )
                continue;

            try {
                URL imgFullUrl = (new URI(relativeUrl))
                    .resolve(new URI(imgUrl))
                    .toURL();
                URLConnection connection = imgFullUrl.openConnection();
                try (
                    InputStream is
                        = new BufferedInputStream(connection.getInputStream())
                ) {
                    byte[] bytes = StreamUtils.getStreamBytes(is);
                    String type = connection.getContentType();
                    image.setURL(
                        "data:" + type + ";base64," + Base64.encodeBytes(bytes)
                    );
                }
            } catch (IOException | URISyntaxException e) {
                // ignore
            }
        }
    }

    public Map<String, Position> getNumberPositions() {
        if (numberPositions == null) {
            numberPositions = new HashMap<>();
            Box[][] boxes = puzzle.getBoxes();

            for (int row = 0; row < puzzle.getHeight(); row++) {
                for (int col = 0; col < puzzle.getWidth(); col++) {
                    Box box = boxes[row][col];
                    if (!Box.isBlock(box) && box.hasClueNumber()) {
                        numberPositions.put(
                            box.getClueNumber(), new Position(row, col)
                        );
                    }
                }
            }
        }

        return numberPositions;
    }

    /**
     * Empty the solution in the puzzle
     */
    public PuzzleBuilder removeSolution() {
        Box[][] boxes = puzzle.getBoxes();

        for (int row = 0; row < puzzle.getHeight(); row++) {
            for (int col = 0; col < puzzle.getWidth(); col++) {
                Box box = boxes[row][col];
                if (!Box.isBlock(box))
                    box.setSolution(null);
            }
        }

        puzzle.setSolutionChecksum((short) 0);

        return this;
    }

    private void invalidateNumberPosition() {
        numberPositions = null;
    }

    private boolean joinedTop(int row, int col) {
        return PuzzleUtils.joinedTop(puzzle, row, col);
    }

    private boolean joinedBottom(int row, int col) {
        return PuzzleUtils.joinedBottom(puzzle, row, col);
    }

    private boolean joinedLeft(int row, int col) {
        return PuzzleUtils.joinedLeft(puzzle, row, col);
    }

    private boolean joinedRight(int row, int col) {
        return PuzzleUtils.joinedRight(puzzle, row, col);
    }
}
