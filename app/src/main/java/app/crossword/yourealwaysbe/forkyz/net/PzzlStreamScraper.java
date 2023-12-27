
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Locale;
import java.util.regex.Matcher;
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
public class PzzlStreamScraper extends AbstractStreamScraper {
    private static final Pattern ID_RE = Pattern.compile(
        "pzzl.net/app/([^/]*)/"
    );
    private static final int ID_GRP = 1;
    private static final Pattern DATE_RE = Pattern.compile(
        "xword:\\s*\\{\\s*date:\\s*('|\")(\\d+)('|\")"
    );
    private static final int DATE_GRP = 2;

    private static final String DEFAULT_SOURCE = "Pzzl.net";

    // args: id, date
    private static final String TXT_URL_FORMAT
        = "https://pzzl.net/servlet/%s/netcrossword?date=%s";

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
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
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is)
            );

            String setID = null;
            String date = null;

            String line;
            while (
                (line = reader.readLine()) != null
                    && (setID == null || date == null)
            ) {
                Matcher matcher = ID_RE.matcher(line);
                if (matcher.find()) {
                    setID = matcher.group(ID_GRP);
                }
                matcher = DATE_RE.matcher(line);
                if (matcher.find()) {
                    date = matcher.group(DATE_GRP);
                }
            }

            if (setID != null && date != null)
                return String.format(Locale.US, TXT_URL_FORMAT, setID, date);
        } catch (IOException e) {
            // fall through
        }
        return null;
    }
}
