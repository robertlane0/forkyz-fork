
package app.crossword.yourealwaysbe.forkyz.net

import java.util.stream.Stream
import kotlinx.coroutines.runBlocking

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import app.crossword.yourealwaysbe.forkyz.settings.DownloadersSettings
import app.crossword.yourealwaysbe.forkyz.util.AppStrings
import app.crossword.yourealwaysbe.forkyz.util.JVMNativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.files.NoopFileHandler

class DownloadersTest {
    companion object {
        private val settings = DownloadersSettings(
            setOf(DownloaderID.CUSTOM.id), // disabledDownloaders (custom daily disabled)
            "", // customDailyTitle,
            "", // customDailyUrl,
            true, // suppressSummaryNotifications,
            true, // suppressIndividualNotifications,
            30000, // downloadTimeout,
            false, // downloadOnStartup
            setOf(), // autoDownloadersBlockMon,
            setOf(), // autoDownloadersBlockTue,
            setOf(), // autoDownloadersBlockWed,
            setOf(), // autoDownloadersBlockThu,
            setOf(), // autoDownloadersBlockFri,
            setOf(), // autoDownloadersBlockSat,
            setOf(), // autoDownloadersBlockSun,
        )

        @JvmStatic
        fun downloaders() : Stream<Arguments> {
            val strings = AppStrings()
            val utils = JVMNativeBackendUtils(strings)
            val dls = Downloaders(
                utils,
                NoopFileHandler(utils),
                settings,
            )
            val downloaders = runBlocking { dls.getDownloaders() }
            return downloaders.stream()
                //filter if need to test a single one
                .filter { d -> d.downloaderID.id == "universal" }
                .map { d -> Arguments.of(d.downloaderID.id, d) }
        }
    }

    @ParameterizedTest(name = "Downloader {0}")
    @MethodSource("downloaders")
    fun testDownloaders(name : String, downloader : Downloader) {
        downloader.latestDate?.let { latest ->
            val res = runBlocking {
                downloader.download(latest, setOf())
            }
            assertTrue(res.isSuccess)
        }
    }
}

