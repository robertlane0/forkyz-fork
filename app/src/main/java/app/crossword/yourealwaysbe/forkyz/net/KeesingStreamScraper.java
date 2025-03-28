
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Pattern;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.KeesingXMLIO;

/**
 * Downloader for Keesing.com embedded crosswords
 *
 * First look for web.keesing.com and also a data-customerid and a
 * data-puzzleid.
 *
 * Then go to the following, where puzzlesetid is puzzleid_customerid
 *
 * https://web.keesing.com/Content/GetPuzzleInfo/?clientid=<customerid>&puzzleid=<puzzlesetid>
 *
 * Note, De Standaard adds _yyyymmdd to the end of the URL. Try without (it
 * works) because i don't know how to reliably get the date from the webpage.
 *
 * Then gets puzzle from
 *
 * https://web.keesing.com/content/getxml?clientid=<customerid>&puzzleid=<puzzleid>"
 *
 */
public class KeesingStreamScraper extends AbstractStreamScraper {
    private static final String TAG = "ForkyzKeesingStreamScraper";

    private static final RegexScrape CUSTOMER_ID_MATCH
        = new RegexScrape(Pattern.compile("data-customerid=\"([^\"]*)\""), 1);
    private static final RegexScrape PUZZLE_ID_MATCH
        = new RegexScrape(Pattern.compile("data-puzzleid=\"([^\"]*)\""), 1);

    // puzzleid_customerid
    private static final String PUZZLE_SET_ID_FORMAT = "%s_%s";
    private static final String ID_URL_FORMAT
        =  "https://web.keesing.com/Content/GetPuzzleInfo/?"
                + "clientid=%s&puzzleid=%s";
    private static final String XML_URL_FORMAT
        = "https://web.keesing.com/content/getxml?clientid=%s&puzzleid=%s";

    private static final String DEFAULT_SOURCE = "Keesing";

    @Override
    public Puzzle parseInput(InputStream is, String url) throws Exception {
        // get customerID and puzzleID
        RegexScrape[] matchers = { CUSTOMER_ID_MATCH, PUZZLE_ID_MATCH };
        String[] results = regexScrape(is, matchers);
        String customerID = results[0];
        String puzzleID = results[1];
        if (customerID == null || puzzleID == null)
            return null;

        String puzzleSetID = String.format(
            Locale.US, PUZZLE_SET_ID_FORMAT, puzzleID, customerID
        );

        return getPuzzle(getPuzzleURL(customerID, puzzleSetID));
    }

    public Puzzle getPuzzle(String puzzleURL) {
        if (puzzleURL == null)
            return null;

        try (InputStream is = getInputStream(puzzleURL)) {
            Puzzle puz = KeesingXMLIO.readPuzzle(is);
            if (
                puz != null
                    && puz.getSource() != null
                    && !puz.getSource().isEmpty()
            ) {
                puz.setSource(DEFAULT_SOURCE);
            }

            return puz;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Fetch puzzle ID using passed data then make puzzle URL from it.
     *
     * Goes online.
     */
    public String getPuzzleURL(String customerID, String puzzleSetID) {
        String idUrl = String.format(
            Locale.US, ID_URL_FORMAT, customerID, puzzleSetID
        );
        try(
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(getInputStream(idUrl))
            )
        ) {
            StringBuilder rawJson = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null)
                rawJson.append(line + "\n");

            JSONObject json
                = new JSONObject(new JSONTokener(rawJson.toString()));
            String puzzleID = json.getString("puzzleID");
            return String.format(
                Locale.US, XML_URL_FORMAT, customerID, puzzleID
            );
        } catch (JSONException | IOException | URISyntaxException e) {
            Log.e(TAG, "Could not read Keesing JSON: " + e);
            return null;
        }
    }
}
