
package app.crossword.yourealwaysbe.forkyz.net

import java.time.LocalDate
import java.util.AbstractList
import java.util.Collections
import java.util.List
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters
import org.junit.runners.Parameterized

import app.crossword.yourealwaysbe.forkyz.settings.DownloadersSettings
import app.crossword.yourealwaysbe.forkyz.util.JVMNativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.files.NoopFileHandler

import org.mockito.MockedStatic
import org.junit.Assert.assertTrue
import org.mockito.Mockito.*

private val DEFAULT_NAME = "puzzle"

@RunWith(Parameterized::class)
class DownloadersTest(
    private val name : String,
    private val downloader : Downloader,
) {
    companion object {
        private val settings = DownloadersSettings(
            true, // downloadDeStandaard
            true, // downloadDeTelegraaf
            true, // downloadElPaisExperto
            true, // downloadGuardianDailyCryptic
            true, // downloadGuardianWeeklyQuiptic
            true, // downloadHamAbend
            true, // downloadIndependentDailyCryptic
            true, // downloadIrishNewsCryptic
            true, // downloadJonesin
            true, // downloadJoseph
            true, // download20Minutes
            true, // downloadLaCroix
            true, // downloadLATimes
            true, // downloadLeParisienF1
            true, // downloadLeParisienF2
            true, // downloadLeParisienF3
            true, // downloadLeParisienF4
            true, // downloadMetroCryptic
            true, // downloadMetroQuick
            true, // downloadNewsday
            true, // downloadNewYorkTimesSyndicated
            true, // downloadNRCInHetMidden
            true, // downloadNRCScrypto
            true, // downloadPremier
            true, // downloadSheffer
            true, // downloadSudOuestMotsCroises
            true, // downloadSudOuestMotsFleches
            true, // downloadTF1MotsCroises
            true, // downloadTF1MotsFleches
            true, // downloadUniversal
            true, // downloadUSAToday
            true, // downloadWaPoSunday
            true, // downloadWsj
            true, // scrapeCru
            true, // scrapeGuardianQuick
            true, // scrapeKegler
            true, // scrapePrivateEye
            true, // scrapePrzekroj
            false, // downloadCustomDaily
            "", // customDailyTitle,
            "", // customDailyUrl,
            true, // suppressSummaryNotifications,
            true, // suppressIndividualNotifications,
            Collections.emptySet(), // autoDownloaders,
            30000, // downloadTimeout,
            false, // downloadOnStartup
        )

        /**
         * Parameters are the list of downloaders
         *
         * Gotten from the Downloaders class. Returned as a list of pairs:
         * (internalName, downloader).
         */
        @Parameters(name = "Downloader {0}")
        @JvmStatic
        fun downloaders() : Iterable<Array<Any>>? {
            val utils = JVMNativeBackendUtils()
            val dls = Downloaders(
                utils,
                NoopFileHandler(utils),
                settings,
            )
            val downloaders = runBlocking { dls.getDownloaders() }
            return object : AbstractList<Array<Any>>() {
                override val size : Int = downloaders.size

                override fun get(i : Int) : Array<Any> {
                    val d = downloaders.get(i)
                    return arrayOf(d.internalName, d)
                }
            }
        }
    }

    @Test
    fun testDownloaders() {
        downloader.latestDate?.let { latest ->
            val res = downloader.download(latest, setOf())
            assertTrue(res.isSuccess)
        }
    }
}

