
package app.crossword.yourealwaysbe.forkyz.net

import java.util.AbstractList
import java.util.Collections
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import app.crossword.yourealwaysbe.forkyz.settings.DownloadersSettings
import app.crossword.yourealwaysbe.forkyz.util.AppStrings
import app.crossword.yourealwaysbe.forkyz.util.JVMNativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.files.NoopFileHandler

import org.junit.Assert.assertTrue
import org.mockito.MockedStatic
import org.mockito.Mockito.*

private val DEFAULT_NAME = "puzzle"

@RunWith(Parameterized::class)
class DownloadersTest(
    private val name : String,
    private val downloader : Downloader,
) {
    companion object {
        private val settings = DownloadersSettings(
            setOf(DownloaderID.CUSTOM.id), // disabledDownloaders (custom daily disabled)
            "", // customDailyTitle,
            "", // customDailyUrl,
            true, // suppressSummaryNotifications,
            true, // suppressIndividualNotifications,
            30000, // downloadTimeout,
            false, // downloadOnStartup
            Collections.emptySet(), // autoDownloadersBlockMon,
            Collections.emptySet(), // autoDownloadersBlockTue,
            Collections.emptySet(), // autoDownloadersBlockWed,
            Collections.emptySet(), // autoDownloadersBlockThu,
            Collections.emptySet(), // autoDownloadersBlockFri,
            Collections.emptySet(), // autoDownloadersBlockSat,
            Collections.emptySet(), // autoDownloadersBlockSun,
        )

        /**
         * Parameters are the list of downloaders
         *
         * Gotten from the Downloaders class. Returned as a list of pairs:
         * (downloaderID, downloader).
         */
        @Parameters(name = "Downloader {0}")
        @JvmStatic
        fun downloaders() : Iterable<Array<Any>>? {
            val strings = AppStrings()
            val utils = JVMNativeBackendUtils(strings)
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
                    return arrayOf(d.downloaderID.id, d)
                }
            }
        }
    }

    @Test
    fun testDownloaders() {
        downloader.latestDate?.let { latest ->
            val res = runBlocking {
                downloader.download(latest, setOf())
            }
            assertTrue(res.isSuccess)
        }
    }
}

