
package app.crossword.yourealwaysbe.forkyz.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import android.content.Context;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerJavaFile;
import app.crossword.yourealwaysbe.forkyz.util.files.MetaCache;
import app.crossword.yourealwaysbe.forkyz.util.files.PuzMetaFile;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.forkyz.versions.TiramisuUtil;
import app.crossword.yourealwaysbe.puz.io.StreamUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class PuzzleImporterTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock
    Context context = mock(Context.class);

    private FileHandler fileHandler;

    @Test
    public void testBasic() throws IOException {
        // create file system
        File crosswords = getCrosswordsFolder();
        File toImport = getToImportFolder();
        File toImportDone = getToImportDoneFolder();
        File toImportFailed = getToImportFailedFolder();

        File testIPuz = new File(toImport, "test.ipuz");
        copyResourceToFile("/test.ipuz", testIPuz);

        File testPuz = new File(toImport, "test.puz");
        copyResourceToFile("/test.puz", testPuz);

        File badPuz = new File(toImport, "test.bad");
        copyResourceToFile("/test.bad", badPuz);

        // do import
        FileHandler fileHandler = getFileHandler();
        PuzzleImporter.processToImportDirectory(
            context, fileHandler, new TiramisuUtil()
        );

        // see what file system is like now
        Set<String> expectedToImport = Collections.emptySet();
        Set<String> expectedToImportDone = Set.of(new String[] {
            "test.ipuz", "test.puz"
        });
        Set<String> expectedToImportFailed = Set.of(new String[] {
            "test.bad"
        });
        Set<String> expectedCrosswordTitles = Set.of(new String[] {
            "NY Times, Fri, Nov 13, 2009",
            "Test &amp; puzzle"
        });

        assertEquals(expectedToImport, Set.of(toImport.list()));
        assertEquals(expectedToImportDone, Set.of(toImportDone.list()));
        assertEquals(expectedToImportFailed, Set.of(toImportFailed.list()));
        assertEquals(expectedCrosswordTitles, getCrosswordTitles());
    }

    @Test
    public void testCopySameFile() throws IOException {
        // create file system
        File crosswords = getCrosswordsFolder();
        File toImport = getToImportFolder();
        File toImportDone = getToImportDoneFolder();

        File testIPuz = new File(toImport, "test.ipuz");
        copyResourceToFile("/test.ipuz", testIPuz);

        // do import
        FileHandler fileHandler = getFileHandler();
        PuzzleImporter.processToImportDirectory(
            context, fileHandler, new TiramisuUtil()
        );

        // one more time
        testIPuz = new File(toImport, "test.ipuz");
        copyResourceToFile("/test.ipuz", testIPuz);
        PuzzleImporter.processToImportDirectory(
            context, fileHandler, new TiramisuUtil()
        );

        // see what file system is like now
        Set<String> expectedToImport = Collections.emptySet();
        Set<String> expectedToImportDone = Set.of(new String[] {
            "test.ipuz", "test (1).ipuz"
        });

        assertEquals(expectedToImport, Set.of(toImport.list()));
        assertEquals(expectedToImportDone, Set.of(toImportDone.list()));
    }

    private Set<String> getDirectoryFiles(File directory) {
        return Arrays.stream(directory.listFiles())
            .filter(f -> f.isFile())
            .map(f -> f.getName())
            .collect(Collectors.toSet());
    }

    private Set<String> getCrosswordTitles() throws IOException {
        FileHandler fileHandler = getFileHandler();

        Set<String> gotCrosswordTitles = new HashSet<>();

        List<PuzMetaFile> pms = fileHandler.getPuzMetas(
            fileHandler.getCrosswordsDirectory()
        );
        for (PuzMetaFile pm : pms) {
            gotCrosswordTitles.add(
                fileHandler.load(pm.getPuzHandle()).getTitle()
            );
        }

        return gotCrosswordTitles;
    }

    private File getCrosswordsFolder() {
        File crosswords = new File(folder.getRoot(), "crosswords");
        crosswords.mkdirs();
        return crosswords;
    }

    private File getToImportFolder() {
        File crosswords = getCrosswordsFolder();
        File toImport = new File(crosswords, "to-import");
        toImport.mkdirs();
        return toImport;
    }

    private File getToImportDoneFolder() {
        File crosswords = getCrosswordsFolder();
        File toImportDone = new File(crosswords, "to-import-done");
        toImportDone.mkdirs();
        return toImportDone;
    }

    private File getToImportFailedFolder() {
        File crosswords = getCrosswordsFolder();
        File toImportFailed = new File(crosswords, "to-import-failed");
        toImportFailed.mkdirs();
        return toImportFailed;
    }

    /**
     * Get file handler
     *
     * Need to mock meta cache db, so can't use puz metas in testing
     */
    private FileHandler getFileHandler() {
        if (fileHandler == null) {
            MetaCache metaCache = mock(MetaCache.class);
            fileHandler = new FileHandlerJavaFile(
                context, metaCache, folder.getRoot()
            ) {
                @Override
                public boolean isStorageMounted() {
                    return true;
                }

                @Override
                public boolean isStorageFull(AndroidVersionUtils utils) {
                    return false;
                }
            };
        }
        return fileHandler;
    }

    private void copyResourceToFile(
        String resourceName, File outFile
    ) throws IOException {
        try (
            InputStream is
                = PuzzleImporterTest.class.getResourceAsStream(resourceName);
            OutputStream os = new FileOutputStream(outFile);
        ) {
            StreamUtils.copyStream(is, os);
        }
    }
}
