
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import app.crossword.yourealwaysbe.forkyz.util.JSONUtils;
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
        ".*mycrossword.*\\/([^/]+)\\/(\\d+)", Pattern.CASE_INSENSITIVE
    );
    private static final int URL_TYPE_GRP = 1;
    private static final int URL_NUM_GRP = 2;

    private static final String DATA_URL_FMT
        = "https://mycrossword.co.uk/api/crossword/getpublishedbytypenum?"
            + "crosswordType=%s&crosswordNum=%s";

    private static final String DEFAULT_SOURCE = "MyCrossword.co.uk";

    @Override
    public Puzzle parseInput(InputStream is, String url) throws Exception {
        Matcher m = URL_RE.matcher(url);
        if (!m.matches())
            return null;

        String type = m.group(URL_TYPE_GRP);
        String num = m.group(URL_NUM_GRP);

        String dataUrl = String.format(Locale.US, DATA_URL_FMT, type, num);

        try (InputStream puzIS = getInputStream(dataUrl)) {
            JSONObject json = JSONUtils.streamToJSON(puzIS);
            JSONObject data = json.optJSONObject("data");
            if (data == null)
                return null;

            // need to bridge between competing json libraries :/
            Puzzle puz = GuardianJSONIO.readPuzzle(data.toString());
            if (puz != null) {
                if (puz.getSource() == null)
                    puz.setSource(DEFAULT_SOURCE);

                String title = puz.getTitle();
                puz.setTitle(title.replace("?", num));

                return puz;
            }
        } catch (IOException | JSONException e) {
            /* fall through */
        }

        return null;
    }
}
