
package app.crossword.yourealwaysbe.forkyz;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import javax.inject.Inject;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import dagger.hilt.android.lifecycle.HiltViewModel;

import app.crossword.yourealwaysbe.forkyz.net.Downloader;
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder;
import app.crossword.yourealwaysbe.forkyz.util.FilteredList;
import app.crossword.yourealwaysbe.forkyz.util.PuzzleImporter;
import app.crossword.yourealwaysbe.forkyz.util.SingleLiveEvent;
import app.crossword.yourealwaysbe.forkyz.util.files.DirHandle;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider;
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle;
import app.crossword.yourealwaysbe.forkyz.util.files.PuzMetaFile;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;

@HiltViewModel
public class BrowseActivityViewModel extends AndroidViewModel {
    private static final Logger LOGGER
        = Logger.getLogger(BrowseActivityViewModel.class.getCanonicalName());

    private static final DateTimeFormatter MATCHER_DATE_FORMAT
        = DateTimeFormatter.ofPattern("EEEE dd MMM yyyy");

    // important that it is single thread to avoid multiple
    // simultaneous operations
    private ExecutorService executorService
        = Executors.newSingleThreadExecutor();
    // not fixed num in case user creates loads of downloads
    private ExecutorService downloadExecutorService
        = Executors.newCachedThreadPool();
    private Handler handler = new Handler(Looper.getMainLooper());

    private AndroidVersionUtils utils;
    private ForkyzSettings settings;
    private FileHandlerProvider fileHandlerProvider;
    private CurrentPuzzleHolder currentPuzzleHolder;
    private DownloadersProvider downloadersProvider;

    private boolean isViewArchive = false;

    private MutableLiveData<FilteredList<MutableLiveData<PuzMetaFile>>>
        puzzleFiles = new MutableLiveData<>();
    private MutableLiveData<String> puzzleFilter = new MutableLiveData<>();
    // busy with something that isn't downloading
    private MutableLiveData<Boolean> isUIBusy
        = new MutableLiveData<Boolean>();
    private SingleLiveEvent<Void> puzzleLoadEvents
        = new SingleLiveEvent<>();

    @Inject
    public BrowseActivityViewModel(
        Application application,
        AndroidVersionUtils utils,
        ForkyzSettings settings,
        FileHandlerProvider fileHandlerProvider,
        CurrentPuzzleHolder currentPuzzleHolder,
        DownloadersProvider downloadersProvider
    ) {
        super(application);
        this.utils = utils;
        this.settings = settings;
        this.fileHandlerProvider = fileHandlerProvider;
        this.currentPuzzleHolder = currentPuzzleHolder;
        this.downloadersProvider = downloadersProvider;

        isUIBusy.postValue(false);
        this.puzzleFilter.observeForever(this::setNewPuzzleFilter);
    }

    public MutableLiveData<String> getPuzzleFilter() {
        return puzzleFilter;
    }

    public void clearPuzzleFilter() {
        getPuzzleFilter().postValue(null);
    }

    /**
     * Get list of puzzle files in currently viewed directory
     *
     * List of mutable live data. Each live data is for one puzzle --
     * the base puzzle may change, but it might get updated with new
     * meta data (e.g. new % completed). If it gets set to null, it
     * means the puzzle was removed from the puzzle list.
     *
     * The puzzle list might have items added to it, a new value will be
     * posted, containing the same list with the new item at the end.
     */
    public MutableLiveData<FilteredList<MutableLiveData<PuzMetaFile>>>
    getPuzzleFiles() {
        return puzzleFiles;
    }

    public MutableLiveData<Boolean> getIsUIBusy() {
        return isUIBusy;
    }

    public SingleLiveEvent<Void> getPuzzleLoadEvents() {
        return puzzleLoadEvents;
    }

    public boolean getIsViewArchive() {
        return isViewArchive;
    }

    public void startLoadFiles() {
        startLoadFiles(getIsViewArchive());
    }

    public void startLoadFiles(boolean archive) {
        fileHandlerProvider.get(fileHandler -> {
            threadWithUILock(() -> {

                DirHandle directory = archive
                    ? fileHandler.getArchiveDirectory()
                    : fileHandler.getCrosswordsDirectory();

                FilteredList<MutableLiveData<PuzMetaFile>> puzFiles
                    = new FilteredList<>(this::metaMatches);

                for (PuzMetaFile pm : fileHandler.getPuzMetas(directory)) {
                    puzFiles.add(new MutableLiveData<>(pm));
                }

                // use handler for this so viewArchive changes when
                // puzzleFiles does
                handler.post(() -> {
                    setIsViewArchive(archive);
                    setNewPuzzleList(puzFiles);
                });
            });
        });
    }

    public void deletePuzzle(PuzMetaFile puzMeta) {
        deletePuzzles(Collections.singleton(puzMeta));
    }

    public void deletePuzzles(Collection<PuzMetaFile> puzMetas) {
        fileHandlerProvider.get(fileHandler -> {
            threadWithUILock(() -> {
                DirHandle viewedDir = getViewedDirectory(fileHandler);

                for (PuzMetaFile puzMeta : puzMetas) {
                    fileHandler.delete(puzMeta);

                    if (puzMeta.isInDirectory(viewedDir))
                        removeFromPuzzleList(puzMeta);
                }
            });
        });
    }

    public void movePuzzle(PuzMetaFile puzMeta, DirHandle destDir) {
        movePuzzles(Collections.singleton(puzMeta), destDir);
    }

    public void movePuzzles(
        Collection<PuzMetaFile> puzMetas, DirHandle destDir
    ) {
        fileHandlerProvider.get(fileHandler -> {
            threadWithUILock(() -> {
                DirHandle directory = getViewedDirectory(fileHandler);

                for (PuzMetaFile puzMeta : puzMetas) {
                    boolean addToList = destDir.equals(directory);
                    boolean removeFromList = puzMeta.isInDirectory(directory);

                    fileHandler.moveTo(puzMeta, destDir);

                    if (addToList && !removeFromList)
                        addPuzzleToList(puzMeta);
                    else if (removeFromList && !addToList)
                        removeFromPuzzleList(puzMeta);
                }
            });
        });
    }

    public void cleanUpPuzzles() {
        settings.getBrowseDeleteOnCleanup(deleteOnCleanup -> {
        settings.getBrowseCleanupAge(maxAgeString -> {
        settings.getBrowseCleanupAgeArchive(archiveMaxAgeString -> {
        fileHandlerProvider.get(fileHandler -> {
            threadWithUILock(() -> {
                LocalDate maxAge = getMaxAge(maxAgeString);
                LocalDate archiveMaxAge = getMaxAge(archiveMaxAgeString);

                DirHandle crosswords
                    = fileHandler.getCrosswordsDirectory();
                DirHandle archive
                    = fileHandler.getArchiveDirectory();

                List<PuzMetaFile> toArchive = new ArrayList<>();
                List<PuzMetaFile> toDelete = new ArrayList<>();

                List<PuzMetaFile> puzCrosswordFiles
                    = fileHandler.getPuzMetas(crosswords);
                for (PuzMetaFile pm : puzCrosswordFiles) {
                    boolean doClean
                        = pm.getComplete() == 100
                        || (maxAge != null && pm.getDate().isBefore(maxAge));

                    if (doClean) {
                        if (deleteOnCleanup) {
                            toDelete.add(pm);
                        } else {
                            toArchive.add(pm);
                        }
                    }
                }

                if (archiveMaxAge != null) {
                    List<PuzMetaFile> puzArchiveFiles
                        = fileHandler.getPuzMetas(archive);
                    for (PuzMetaFile pm : puzArchiveFiles) {
                        if (pm.getDate().isBefore(archiveMaxAge)) {
                            toDelete.add(pm);
                        }
                    }
                }

                for (PuzMetaFile puzMeta : toDelete)
                    fileHandler.delete(puzMeta);

                for (PuzMetaFile puzMeta : toArchive)
                    fileHandler.moveTo(puzMeta, archive);

                startLoadFiles();
            });
        });});});});
    }

    public void download(LocalDate date, List<Downloader> downloaders) {
        downloadersProvider.get(dls -> {
            downloadExecutorService.execute(() -> {
                dls.download(date, downloaders);
                if (!getIsViewArchive()) {
                    handler.post(() -> { startLoadFiles(); });
                }
            });
        });
    }

    /**
     * Runs autodownload if it hasn't run too recently
     *
     * Runs async but only runs one request at a time to avoid parallel
     * auto download jobs.
     */
    public void autoDownloadIfRequired() {
        downloadExecutorService.execute(() -> {
            settings.getDownloadersSettings(downloaderSettings -> {
            downloadersProvider.get(dls -> {
                // Should not run in parallel with other auto downloads.
                // If multiple calls are made rapidly, the first will
                // lock the block below and update lastDL so future
                // blocks don't redownload. Could have used a separate
                // single-thread autodownload executor instead, but this
                // avoids an extra thread and field variable.
                synchronized (BrowseActivityViewModel.this) {
                    long lastDL = settings.getBrowseLastDownloadSync();
                    long downloadCutoff = System.currentTimeMillis()
                        - (long) (12 * 60 * 60 * 1000);

                    boolean isDownload
                        = downloaderSettings.getDownloadOnStartup()
                        && downloadCutoff > lastDL;

                    if (isDownload) {
                        settings.setBrowseLastDownloadSync(
                            System.currentTimeMillis()
                        );
                        download(LocalDate.now(), dls.getAutoDownloaders());
                    }
                }
            ;});});
        });
    }

    public void loadPuzzle(PuzMetaFile puzMeta) {
        settings.getPlayPreserveCorrectLettersInShowErrors(preserveCorrect -> {
        settings.getPlayDeleteCrossingMode(deleteCrossingMode -> {
        settings.getPlayMovementStrategy(movementStrategy -> {
        fileHandlerProvider.get(fileHandler -> {
            threadWithUILock(() -> {
                try {
                    Puzzle puz = fileHandler.load(puzMeta);
                    if (puz == null || puz.getBoxes() == null) {
                        throw new IOException(
                            "Puzzle is null or contains no boxes."
                        );
                    }
                    handler.post(() -> {
                        currentPuzzleHolder.setBoard(
                            new Playboard(
                                puz,
                                movementStrategy,
                                preserveCorrect,
                                deleteCrossingMode
                            ),
                            puzMeta.getPuzHandle()
                        );
                        puzzleLoadEvents.call();
                    });
                } catch (IOException e) {
                    handler.post(() -> {
                        String filename = null;
                        try {
                            filename
                                = fileHandler.getName(puzMeta.getPuzHandle());
                        } catch (Exception ee) {
                            ee.printStackTrace();
                        }


                        Application application = getApplication();
                        Toast t = Toast.makeText(
                            application,
                            application.getString(
                                R.string.unable_to_read_file,
                                (filename != null ?  filename : "")
                            ),
                            Toast.LENGTH_SHORT
                        );
                        t.show();
                    });
                }
            });
        });});});});
    }

    public void refreshPuzzleMeta(PuzHandle refreshHandle) {
        fileHandlerProvider.get(fileHandler -> {
            threadWithUILock(() -> {
                try {
                    PuzMetaFile refreshedMeta
                        = fileHandler.loadPuzMetaFile(refreshHandle);

                    if (refreshedMeta == null)
                        return;

                    if (refreshedMeta != null) {
                        List<MutableLiveData<PuzMetaFile>> pmList
                            = puzzleFiles.getValue();

                        int index = -1;
                        for (MutableLiveData<PuzMetaFile> pm : pmList) {
                            index += 1;
                            if (pm.getValue().isSameMainFile(refreshHandle)) {
                                pm.postValue(refreshedMeta);
                            }
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warning("Could not refresh puz meta " + e);
                }
            });
        });
    }

    /**
     * Import files from uri to crosswords folder
     *
     * callback when finished, arguments are someFailed and someSucceeded
     */
    public void importURIs(
        Collection<Uri> uris,
        boolean forceReload,
        BiConsumer<Boolean, Boolean> callback
    ) {
        fileHandlerProvider.get(fileHandler -> {
        settings.getDownloadTimeout(timeout -> {
            threadWithUILock(() -> {
                boolean someFailed = false;
                boolean someSucceeded = false;
                boolean needsReload = forceReload;

                for (Uri uri : uris) {
                    if (uri == null)
                        continue;

                    PuzHandle ph = PuzzleImporter.importUri(
                        getApplication(), fileHandler, utils, uri, timeout
                    );

                    someFailed |= (ph == null);
                    someSucceeded |= (ph != null);

                    if (!getIsViewArchive() && ph != null && !needsReload) {
                        try {
                            PuzMetaFile pm = fileHandler.loadPuzMetaFile(ph);
                            addPuzzleToList(pm);
                        } catch (IOException e) {
                            // fall back to full reload
                            needsReload = true;
                        }
                    }
                }

                if (needsReload)
                    startLoadFiles();

                final boolean finalSomeFailed = someFailed;
                final boolean finalSomeSucceeded = someSucceeded;
                handler.post(() -> {
                    callback.accept(finalSomeFailed, finalSomeSucceeded);
                });
            });
        });});
    }

    /**
     * Process to-import folder
     *
     * Import puzzles in there, move successful files to to-import-done
     */
    public void processToImportDirectory() {
        fileHandlerProvider.get(fileHandler -> {
            threadWithUILock(() -> {
                List<PuzHandle> imported
                    = PuzzleImporter.processToImportDirectory(
                        getApplication(), fileHandler, utils
                    );

                for (PuzHandle ph : imported) {
                    try {
                        PuzMetaFile pm = fileHandler.loadPuzMetaFile(ph);
                        addPuzzleToList(pm);
                    } catch (IOException e) {
                        LOGGER.warning("Could not load meta for handle " + ph);
                    }
                }
            });
        });
    }

    private LocalDate getMaxAge(String preferenceValue) {
        int cleanupValue = Integer.parseInt(preferenceValue) + 1;
        if (cleanupValue > 0)
            return LocalDate.now().minus(Period.ofDays(cleanupValue));
        else
            return null;
    }

    private void setIsViewArchive(boolean isViewArchive) {
        this.isViewArchive = isViewArchive;
    }

    /**
     * crosswords if not viewing archive, else archive
     */
    private DirHandle getViewedDirectory(FileHandler fileHandler) {
        return getIsViewArchive()
            ? fileHandler.getArchiveDirectory()
            : fileHandler.getCrosswordsDirectory();
    }

    @Override
    protected void onCleared() {
        executorService.shutdown();
        downloadExecutorService.shutdown();
    }

    private void threadWithUILock(Runnable r) {
        // no lock actually needed because executorService is single
        // threaded guaranteed
        executorService.execute(() -> {
            try {
                isUIBusy.postValue(true);
                r.run();
            } finally {
                isUIBusy.postValue(false);
            }
        });
    }

    /**
     * Don't add the same file twice!
     */
    private void addPuzzleToList(PuzMetaFile puzMeta) {
        FilteredList<MutableLiveData<PuzMetaFile>> files
            = puzzleFiles.getValue();

        if (files == null)
            return;

        files.add(new MutableLiveData<>(puzMeta));
        puzzleFiles.postValue(files);
    }

    private void setNewPuzzleList(
        FilteredList<MutableLiveData<PuzMetaFile>> newList
    ) {
        setNewPuzzleListAndFilter(newList, puzzleFilter.getValue());
    }

    private void setNewPuzzleFilter(String filterValue) {
        setNewPuzzleListAndFilter(puzzleFiles.getValue(), filterValue);
    }

    private void setNewPuzzleListAndFilter(
        FilteredList<MutableLiveData<PuzMetaFile>> newList, String newFilter
    ) {
        if (newList != null)
            newList.applyFilter(newFilter);
        puzzleFiles.setValue(newList);
    }

    /**
     * Assumes files only appear once in list
     */
    private void removeFromPuzzleList(PuzMetaFile delPuzMeta) {
        List<MutableLiveData<PuzMetaFile>> puzList = puzzleFiles.getValue();

        if (puzList == null)
            return;

        int index = 0;
        int delIndex = -1;

        while (index < puzList.size() && delIndex < 0) {
            PuzMetaFile pm = puzList.get(index).getValue();

            if (pm.isSameMainFile(delPuzMeta))
                delIndex = index;

            index += 1;
        }

        if (delIndex >= 0) {
            puzList
                .remove(delIndex)
                .postValue(null);
        }
    }

    private boolean metaMatches(
        MutableLiveData<PuzMetaFile> pmLiveData, String filterText
    ) {
        PuzMetaFile pm = (pmLiveData == null) ? null : pmLiveData.getValue();

        if (pm == null)
            return false;

        if (filterText == null)
            return false;

        Locale locale = Locale.getDefault();

        filterText = filterText.toUpperCase(locale);

        if (pm.getCaption().toUpperCase(locale).contains(filterText))
            return true;

        if (pm.getTitle().toUpperCase(locale).contains(filterText))
            return true;

        if (pm.getAuthor().toUpperCase(locale).contains(filterText))
            return true;

        if (pm.getSource().toUpperCase(locale).contains(filterText))
            return true;

        if (
            MATCHER_DATE_FORMAT.format(pm.getDate())
                .toUpperCase(locale)
                .contains(filterText)
        )
            return true;

        return false;
    }
}
