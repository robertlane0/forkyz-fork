
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;

import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.PMLIO;
import app.crossword.yourealwaysbe.puz.io.StreamUtils;

/**
 * Scraper for embedded PML Puzzles pages
 */
public class PMLStreamScraper extends AbstractStreamScraper {
    private static final Pattern PML_URL_RE = Pattern.compile(
        ".*/puzzles/[a-z-]*crossword/(\\d{6}|latest).html?",
        Pattern.CASE_INSENSITIVE
    );
    private static final String PML_URL_EXTRACT = "$0";

    private static final Pattern IS_LATEST_RE = Pattern.compile(
        "(.*)latest(.html?)", Pattern.CASE_INSENSITIVE
    );
    private static final int IS_LATEST_PRE_URL_GRP = 1;
    private static final int IS_LATEST_POST_URL_GRP = 2;

    private static final RegexScrape EMBED_JSON_MATCH =
        new RegexScrape(
            Pattern.compile("\\{\"pml_id\".*\\}"),
            0
        );

    private static final DateTimeFormatter DATE_FORMATTER
        = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US);

    private static final String DEFAULT_SOURCE = "PML Puzzles";

    @Override
    public Puzzle parseInput(InputStream is, String url) throws Exception {
        ByteArrayInputStream bis = StreamUtils.makeByteArrayInputStream(is);

        String embedURL = getPMLURL(bis);
        if (embedURL != null) {
            try (InputStream embedIS = getInputStream(embedURL)) {
                bis = StreamUtils.makeByteArrayInputStream(embedIS);
            } catch (IOException e) {
                // fall through, try old bis as embedded
                bis.reset();
            }
        } else {
            bis.reset();
        }

        Puzzle puz = null;

        String embedJSON = getPMLJSON(bis);
        if (embedJSON != null) {
            puz = PMLIO.readFromJSON(embedJSON);
        }

        if (puz == null) {
            bis.reset();
            puz = PMLIO.readPuzzle(bis);
        }

        if (puz != null)
            puz.setSource(DEFAULT_SOURCE);

        return puz;
    }

    /**
     * Find url
     *
     * Replace latest.html with yyyyMMdd.html. Return null if nothing
     * that looks like a PML URL found.
     */
    private static String getPMLURL(InputStream is) {
        Document doc = getDocument(is);
        String url = htmlElementFindURL(
            doc,
            "iframe", "src",
            PML_URL_RE, PML_URL_EXTRACT
        );

        if (url == null)
            return null;

        Matcher m = IS_LATEST_RE.matcher(url);
        if (m.matches()) {
            String today = DATE_FORMATTER.format(LocalDate.now());
            return m.group(IS_LATEST_PRE_URL_GRP)
                + today
                + m.group(IS_LATEST_POST_URL_GRP);
        } else {
            return url;
        }
    }

    /**
     * Find directly embedded JSON
     */
    private static String getPMLJSON(InputStream is) {
        return regexScrape(is, EMBED_JSON_MATCH);
    }
}
