
package app.crossword.yourealwaysbe.forkyz.util

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.runBlocking

import android.content.Context

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

import org.koin.core.context.stopKoin

import org.mockito.Mock
import org.mockito.Mockito.mock

import org.robolectric.RobolectricTestRunner

import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerJavaFile
import app.crossword.yourealwaysbe.forkyz.util.files.MetaCache
import app.crossword.yourealwaysbe.forkyz.versions.TiramisuUtil
import app.crossword.yourealwaysbe.puz.io.StreamUtils

@RunWith(RobolectricTestRunner::class)
class PuzzleImporterTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Mock
    val context: Context = mock(Context::class.java)

    private val utils = NativeBackendUtils(context, TiramisuUtil())
    private var fileHandler: FileHandler? = null

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testBasic() {
        // create file system
        val crosswords = getCrosswordsFolder()
        val toImport = getToImportFolder()
        val toImportDone = getToImportDoneFolder()
        val toImportFailed = getToImportFailedFolder()

        val testIPuz = File(toImport, "test.ipuz")
        copyResourceToFile("/test.ipuz", testIPuz)

        val testPuz = File(toImport, "test.puz")
        copyResourceToFile("/test.puz", testPuz)

        val badPuz = File(toImport, "test.bad")
        copyResourceToFile("/test.bad", badPuz)

        // do import
        val fileHandler = getFileHandler()
        runBlocking { processPuzzleToImportDirectory(utils, fileHandler) }

        // see what file system is like now
        val expectedToImport = emptySet<String>()
        val expectedToImportDone = setOf(
            "test.ipuz", "test.puz"
        )
        val expectedToImportFailed = setOf(
            "test.bad"
        )
        val expectedCrosswordTitles = setOf(
            "NY Times, Fri, Nov 13, 2009",
            "Test &amp; puzzle"
        )

        assertEquals(expectedToImport, toImport.list()?.toSet())
        assertEquals(expectedToImportDone, toImportDone.list()?.toSet())
        assertEquals(expectedToImportFailed, toImportFailed.list()?.toSet())
        assertEquals(expectedCrosswordTitles, getCrosswordTitles())
    }

    @Test
    fun testCopySameFile() {
        // create file system
        val crosswords = getCrosswordsFolder()
        val toImport = getToImportFolder()
        val toImportDone = getToImportDoneFolder()

        var testIPuz = File(toImport, "test.ipuz")
        copyResourceToFile("/test.ipuz", testIPuz)

        // do import
        val fileHandler = getFileHandler()
        runBlocking { processPuzzleToImportDirectory(utils, fileHandler) }

        // one more time
        testIPuz = File(toImport, "test.ipuz")
        copyResourceToFile("/test.ipuz", testIPuz)
        runBlocking { processPuzzleToImportDirectory(utils, fileHandler) }

        // see what file system is like now
        val expectedToImport = emptySet<String>()
        val expectedToImportDone = setOf(
            "test.ipuz", "test (1).ipuz"
        )

        assertEquals(expectedToImport, toImport.list()?.toSet())
        assertEquals(expectedToImportDone, toImportDone.list()?.toSet())
    }

    private fun getDirectoryFiles(directory: File): Set<String> {
        return directory.listFiles()
            ?.filter { it.isFile }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
    }

    private fun getCrosswordTitles(): Set<String> {
        val fileHandler = getFileHandler()

        val gotCrosswordTitles = mutableSetOf<String>()

        val pms = fileHandler.getPuzMetas(
            fileHandler.crosswordsDirectory
        )
        for (pm in pms) {
            gotCrosswordTitles.add(
                fileHandler.load(pm.puzHandle).title
            )
        }

        return gotCrosswordTitles
    }

    private fun getCrosswordsFolder(): File {
        val crosswords = File(folder.root, "crosswords")
        crosswords.mkdirs()
        return crosswords
    }

    private fun getToImportFolder(): File {
        val crosswords = getCrosswordsFolder()
        val toImport = File(crosswords, "to-import")
        toImport.mkdirs()
        return toImport
    }

    private fun getToImportDoneFolder(): File {
        val crosswords = getCrosswordsFolder()
        val toImportDone = File(crosswords, "to-import-done")
        toImportDone.mkdirs()
        return toImportDone
    }

    private fun getToImportFailedFolder(): File {
        val crosswords = getCrosswordsFolder()
        val toImportFailed = File(crosswords, "to-import-failed")
        toImportFailed.mkdirs()
        return toImportFailed
    }

    /**
     * Get file handler
     *
     * Need to mock meta cache db, so can't use puz metas in testing
     */
    private fun getFileHandler() : FileHandler {
        if (fileHandler == null) {
            val metaCache = mock(MetaCache::class.java)
            fileHandler = object : FileHandlerJavaFile(
                utils,
                metaCache,
                folder.root,
            ) {
                override fun isStorageMounted() : Boolean {
                    return true
                }

                override fun isStorageFull() : Boolean {
                    return false
                }

                override fun getSettings() : FileHandlerSettings {
                    return FileHandlerSettings(
                        StorageLocation.SL_INTERNAL,
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                    )
                }
            }
        }
        return fileHandler!!
    }

    private fun copyResourceToFile(
        resourceName : String,
        outFile : File,
    ) {
        PuzzleImporterTest::class.java
            .getResourceAsStream(resourceName).use { inputStream ->
                FileOutputStream(outFile).use { os ->
                    StreamUtils.copyStream(inputStream, os)
                }
            }
    }
}
