
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

import android.content.Context
import android.util.Log

import app.crossword.yourealwaysbe.forkyz.settings.DownloadersSettings
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.files.NoopFileHandler
import app.crossword.yourealwaysbe.forkyz.versions.TiramisuUtil

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
                true, // downloadLATimes
                true, // downloadLeParisienF1
                true, // downloadLeParisienF2
                true, // downloadLeParisienF3
                true, // downloadLeParisienF4
                true, // downloadMetroCryptic
                true, // downloadMetroQuick
                true, // downloadNewsday
                true, // downloadNewYorkTimesSyndicated
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
         * Gotten from the Downloaders class (needs mocking of context and
         * prefs). Returned as a list of pairs: (internalName, downloader).
         */
        @Parameters(name = "Downloader {0}")
        @JvmStatic
        fun downloaders() : Iterable<Array<Any>>? {
            val context = mock(Context::class.java)
            initialiseMocks(context)
            try {
                mockStatic(Log::class.java).use { _ ->
                    val utils = NativeBackendUtils(
                        context,
                        TiramisuUtil(),
                    )
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
                            return arrayOf(d.getInternalName(), d)
                        }
                    }
                }
            } catch (e : Exception) {
                return null
            }
        }

        private fun initialiseMocks(context : Context) {
            // when names come from context, give any
            `when`(context.getString(anyInt())).thenReturn(DEFAULT_NAME)
        }
    }

    @Test
    fun testDownloaders() {
        mockStatic(Log::class.java).use { _ ->
            downloader.getLatestDate()?.let { latest ->
                val res = downloader.download(latest, setOf())
                assertTrue(res.isSuccess())
            }
        }
    }
}

