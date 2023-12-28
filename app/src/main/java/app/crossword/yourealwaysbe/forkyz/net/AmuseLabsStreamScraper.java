
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.PuzzleParser;
import app.crossword.yourealwaysbe.puz.io.StreamUtils;

/**
 * Downloader for embedded AmuseLabs crosswords
 */
public class AmuseLabsStreamScraper extends AbstractStreamScraper {

    // Note <iframe> may be generated in javascript. Hamburger uses
    // pagespeed_iframe... So look for something that looks like it's
    // creating it.
    private static final Pattern SECOND_STAGE_URL_RE = Pattern.compile(
        "iframe[^>]*src=\"([^\"]*amuselabs\\.com[^\"]*crossword[^\"]*)\""
    );
    private static final int SECOND_STAGE_URL_GRP = 1;

    private static final String DEFAULT_SOURCE = "AmuseLabs";

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        // so we can reuse
        ByteArrayInputStream bis = StreamUtils.makeByteArrayInputStream(is);
        PuzzleParser secondStage = new SecondStageParser();

        Puzzle puz = null;

        String secondStageURL = getSecondStageURL(bis);

        System.out.println("FORKYZ: second stage " + secondStageURL);

        if (secondStageURL != null) {
            try (InputStream ssIS = getInputStream(secondStageURL)) {
                puz = secondStage.parseInput(ssIS);
            } catch (Exception e) {
                // fall through
            }
        }
        if (puz == null) {
            bis.reset();
            puz = secondStage.parseInput(bis);
        }

        if (puz != null) {
            puz.setSource(DEFAULT_SOURCE);
        }

        return puz;
    }

    /**
     * Parse the first page and get the embedded URL, or null
     *
     * Does not close input stream. Looking for iframes in a Jsoup
     * parsed doc doesn't seem to work. Perhaps the HTML is funky.
     */
    public String getSecondStageURL(InputStream is) {
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is)
            );

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("FORKYZ: " + line);
                Matcher matcher = SECOND_STAGE_URL_RE.matcher(line);
                if (matcher.find()) {
                    return matcher.group(SECOND_STAGE_URL_GRP);
                }
            }
        } catch (IOException e) {
            // fall through
        }
        return null;
    }

    /**
     * Parse the page containing the puzzle ID and get puzzle
     *
     * Finds ID, then gets puzzle from API URL. Does not close input
     * stream.
     */
    public Puzzle parseSecondStage(InputStream is) {
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is)
            );

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("FORKYZ: " + line);
                //Matcher matcher = PUZZLE_ID_PAT.matcher(line);
                //if (matcher.find()) {
                //    String id = matcher.group(PUZZLE_ID_GROUP);
                //    return String.format(Locale.US, JSON_URL_FORMAT, id);
                //}
            }
        } catch (IOException e) {
            // fall through
        }
        return null;
    }

    private class SecondStageParser implements PuzzleParser {
        @Override
        public Puzzle parseInput(InputStream is) throws Exception {
            return parseSecondStage(is);
        }
    }
}
