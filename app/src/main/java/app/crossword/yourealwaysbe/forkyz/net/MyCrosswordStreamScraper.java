
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.GuardianJSONIO;

/**
 * Downloader for MyCrossword.co.uk embedded Crosswords
 *
 * Look for puzzle type/number in URL
 *
 *  https://mycrossword.co.uk/<type>/<num>
 *
 * Get puzzle in Guardian JSON format from
 *
 *  https://mycrossword.co.uk/api/crossword/getpublishedbytypenum?crosswordType=<type>&crosswordNum=<num>
 */
public class MyCrosswordStreamScraper extends AbstractStreamScraper {
    private static final Pattern URL_RE = Pattern.compile(
        ".*mycrossword.*\\/[^/]+\\/\\d+", Pattern.CASE_INSENSITIVE
    );
    private static final RegexScrape JSON_MATCH = new RegexScrape(
        Pattern.compile(
            "window.__remixContext = (\\{.*?\\});",
            Pattern.CASE_INSENSITIVE
        ),
        1
    );

    private static final String DATA_URL_FMT
        = "https://mycrossword.co.uk/api/crossword/getpublishedbytypenum?"
            + "crosswordType=%s&crosswordNum=%s";

    private static final String DEFAULT_SOURCE = "MyCrossword.co.uk";

    @Override
    public Puzzle parseInput(InputStream is, String url) throws Exception {
        Matcher m = URL_RE.matcher(url);
        if (!m.matches())
            return null;

        try (InputStream puzIS = getInputStream(url)) {
            String jsonString = regexScrape(puzIS, JSON_MATCH);
            if (jsonString == null)
                return null;

            JSONObject json = new JSONObject(jsonString);
            JSONObject data = json.optJSONObject("state");
            if (data == null)
                return null;
            data = data.optJSONObject("loaderData");
            if (data == null)
                return null;
            data = data.optJSONObject("routes/cryptic.$crosswordId");
            if (data == null)
                return null;
            data = data.optJSONObject("crossword");
            if (data == null)
                return null;
            data = data.optJSONObject("data");
            if (data == null)
                return null;

            // need to bridge between competing json libraries :/
            Puzzle puz = GuardianJSONIO.readPuzzle(data.toString());
            if (puz != null) {
                if (puz.getSource() == null)
                    puz.setSource(DEFAULT_SOURCE);
                return puz;
            }
        } catch (IOException | JSONException e) {
            /* fall through */
        }

        return null;
    }
}
