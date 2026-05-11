
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.coroutines.runBlocking
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.readString

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stubbing

import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.util.AppStrings
import app.crossword.yourealwaysbe.forkyz.util.files.File
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerSystemFile
import app.crossword.yourealwaysbe.forkyz.util.files.MetaCache
import app.crossword.yourealwaysbe.forkyz.util.files.MetaCache.CachedMeta
import app.crossword.yourealwaysbe.forkyz.util.files.MetaCache.MetaRecord
import app.crossword.yourealwaysbe.forkyz.util.toUri

// thanks: https://derekwilson.net/blog/2018/08/23/mokito-kotlin
object MockitoHelper {
    @Suppress("CheckResult")
    fun <T> anyObject() : T {
        Mockito.any<T>()
        return uninitialized()
    }
    @Suppress("UNCHECKED_CAST")
    fun <T> uninitialized() : T = null as T
}

class PuzzleImporterTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val utils = JVMNativeBackendUtils(AppStrings())
    private var fileHandler : FileHandler? = null

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

        assertEquals(expectedToImport, getNames(toImport))
        assertEquals(expectedToImportDone, getNames(toImportDone))
        assertEquals(expectedToImportFailed, getNames(toImportFailed))
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

        assertEquals(expectedToImport, getNames(toImport))
        assertEquals(expectedToImportDone, getNames(toImportDone))
    }

    private fun getDirectoryFiles(directory : File) : Set<String> {
        return directory.list()
            .filter { it.isRegularFile }
            .map { it.name }
            .toSet()
    }

    private fun getCrosswordTitles() : Set<String> {
        val fileHandler = getFileHandler()

        val gotCrosswordTitles = mutableSetOf<String>()

        val pms = fileHandler.getPuzMetas(
            fileHandler.crosswordsDirectory
        )
        for (pm in pms) {
            fileHandler.load(pm.puzHandle)?.let {
                it.title?.let(gotCrosswordTitles::add)
            }
        }

        return gotCrosswordTitles
    }

    private fun getCrosswordsFolder() : File {
        val crosswords = File(folder.root.getPath(), "crosswords")
        crosswords.mkdirs()
        return crosswords
    }

    private fun getToImportFolder() : File {
        val crosswords = getCrosswordsFolder()
        val toImport = File(crosswords, "to-import")
        toImport.mkdirs()
        return toImport
    }

    private fun getToImportDoneFolder() : File {
        val crosswords = getCrosswordsFolder()
        val toImportDone = File(crosswords, "to-import-done")
        toImportDone.mkdirs()
        return toImportDone
    }

    private fun getToImportFailedFolder() : File {
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
            stubbing(metaCache) {
                on {
                    metaCache.addRecord(
                        MockitoHelper.anyObject(),
                        MockitoHelper.anyObject(),
                        MockitoHelper.anyObject(),
                    )
                } doReturn
                    MetaRecord(
                        CachedMeta(
                            mainFileUri = "content://none".toUri(),
                            metaFileUri = null,
                            directoryUri = "content://none".toUri(),
                            isUpdatable = false,
                            date = null,
                            percentComplete = 0,
                            percentFilled = 0,
                            source = null,
                            title = null,
                            author = null,
                            ratingCode = 0,
                            hasSolution = false,
                            percentHinted = 0,
                        )
                    )
                on {
                    metaCache.getDirCache(
                        MockitoHelper.anyObject(),
                        MockitoHelper.anyObject(),
                    )
                } doReturn emptyMap()
                on {
                    metaCache.cleanupCache(
                        MockitoHelper.anyObject(),
                        MockitoHelper.anyObject(),
                        MockitoHelper.anyObject(),
                    )
                } doReturn Unit
            }

            fileHandler = object : FileHandlerSystemFile(
                utils,
                metaCache,
                getCrosswordsFolder(),
            ) {
                override val isStorageMounted = true
                override val isStorageFull = false
                override val settings = FileHandlerSettings(
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
        return fileHandler!!
    }

    private fun copyResourceToFile(
        resourceName : String,
        outFile : File,
    ) {
        PuzzleImporterTest::class.java
            .getResourceAsStream(resourceName)
            .asSource()
            .buffered()
            .use { input ->
                outFile.sink().buffered().use { output ->
                    input.transferTo(output)
                }
            }
    }

    private fun getNames(dir : File) : Set<String> {
        return dir.list().map { it.name }.toSet()
    }
}

