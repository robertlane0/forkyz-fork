
package app.crossword.yourealwaysbe.forkyz.net;

import java.time.LocalDate;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized;

import android.content.Context;

import app.crossword.yourealwaysbe.forkyz.settings.DownloadersSettings;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class DownloadersTest {

    private static final String DEFAULT_NAME = "puzzle";

    /**
     * Parameters are the list of downloaders
     *
     * Gotten from the Downloaders class (needs mocking of context and
     * prefs). Returned as a list of pairs: (internalName, downloader).
     */
    @Parameters(name = "Downloader {0}")
    public static Iterable<Object[]> downloaders() {
        Context context = mock(Context.class);
        initialiseMocks(context);
        DownloadersSettings settings = getDownloadersSettings();
        final List<Downloader> downloaders
            = (new Downloaders(context, null, settings)).getDownloaders();
        return new AbstractList<Object[]>() {
            @Override
            public int size() { return downloaders.size(); }

            @Override
            public Object[] get(int i) {
                Downloader d = downloaders.get(i);
                return new Object[] { d.getInternalName(), d };
            }
        };
    }

    private Downloader downloader;

    public DownloadersTest(String name, Downloader downloader) {
        this.downloader = downloader;
    }

    @Test
    public void testDownloaders() {
        LocalDate latest = downloader.getLatestDate();
        Downloader.DownloadResult res =
            downloader.download(latest, Collections.emptySet());
        assertTrue(res.isSuccess());
    }

    private static void initialiseMocks(Context context) {
        // when names come from context, give any
        when(context.getString(anyInt())).thenReturn(DEFAULT_NAME);
    }

    private static DownloadersSettings getDownloadersSettings() {
        return new DownloadersSettings(
            true, // downloadDeStandaard
            true, // downloadDeTelegraaf
            true, // downloadGuardianDailyCryptic
            true, // downloadHamAbend
            true, // downloadIndependentDailyCryptic
            true, // downloadJonesin
            true, // downloadJoseph
            true, // download20Minutes
            true, // downloadLeParisienF1
            true, // downloadLeParisienF2
            true, // downloadLeParisienF3
            true, // downloadLeParisienF4
            true, // downloadNewsday
            true, // downloadPremier
            true, // downloadSheffer
            true, // downloadUniversal
            true, // downloadUSAToday
            true, // downloadWaPoSunday
            true, // downloadWsj
            true, // scrapeCru
            true, // scrapeKegler
            true, // scrapePrivateEye
            true, // scrapePrzekroj
            false , // downloadCustomDaily
            "", // customDailyTitle,
            "", // customDailyUrl,
            true, // suppressSummaryNotifications,
            true, // suppressIndividualNotifications,
            null, // autoDownloaders,
            30000, // downloadTimeout,
            false // downloadOnStartup
        );
    }
}

