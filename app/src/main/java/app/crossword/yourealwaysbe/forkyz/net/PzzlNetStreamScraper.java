
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Pattern;

import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.BrainsOnlyIO;

/**
 * Downloader for Pzzl.net embedded Crosswords
 *
 * Look for "pzzl.net/app/<id>" and window.pzzl = { xword: { date: '<date>' } }
 * then get pzzl.net/servlet/<id>/netcrossword?date=<date> and it's BrainsOnly
 * text.
 */
public class PzzlNetStreamScraper extends AbstractStreamScraper {
    private static final RegexScrape ID_MATCH =
        new RegexScrape(Pattern.compile("pzzl.net/app/([^/]*)/"), 1);
    private static final RegexScrape DATE_MATCH =
        new RegexScrape(
            Pattern.compile("xword:\\s*\\{\\s*date:\\s*('|\")(\\d+)('|\")"), 2
        );

    private static final String DEFAULT_SOURCE = "Pzzl.net";

    // args: id, date
    private static final String TXT_URL_FORMAT
        = "https://pzzl.net/servlet/%s/netcrossword?date=%s";

    @Override
    public Puzzle parseInput(InputStream is, String url) throws Exception {
        String txtURL = getTxtURL(is);

        if (txtURL == null)
            return null;

        try(InputStream txtIS = getInputStream(txtURL)) {
            Puzzle puz = BrainsOnlyIO.parse(txtIS);
            if (puz != null) {
                puz.setSource(DEFAULT_SOURCE);
            }
            return puz;
        } catch (Exception e) {
            // fall through
        }

        return null;
    }

    public String getTxtURL(InputStream is) {
        RegexScrape[] matchers = { ID_MATCH, DATE_MATCH };
        String[] results = regexScrape(is, matchers);
        String setID = results[0];
        String date = results[1];
        if (setID != null && date != null)
            return String.format(Locale.US, TXT_URL_FORMAT, setID, date);
        else
            return null;
    }
}
