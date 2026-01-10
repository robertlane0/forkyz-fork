
package app.crossword.yourealwaysbe.forkyz.util

import java.io.IOException
import java.util.function.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

import androidx.annotation.MainThread

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle
import app.crossword.yourealwaysbe.forkyz.util.files.PuzMetaFile
import app.crossword.yourealwaysbe.forkyz.util.observeForeverWithProcessLifecycle
import app.crossword.yourealwaysbe.puz.Playboard

/**
 * Service for calling back to CurrentPuzzleHolder on safe thread
 *
 * E.g. on Android this means calling back from a Service that won't be
 * killed by the OS before save completes.
 *
 * Should call back to saveBoardService on CurrentPuzzleHolder singleton
 * passed as an argument for direct use if appropriate.
 *
 * Platform specific version provided via injection.
 */
interface PuzzleSaveServiceCaller {
    fun saveBoard(currentPuzzleHolder : CurrentPuzzleHolder)
}

/**
 * Holder for the current puzzle being played
 *
 * Singleton and app-wide. Persists until application destroyed
 * (property of Hilt singletons). Can be injected into whichever
 * class needs access to get/set the current puzzle.
 */
public class CurrentPuzzleHolder(
    val settings : ForkyzSettings,
    val fileHandlerProvider : FileHandlerProvider,
    val puzzleSaveServiceCaller : PuzzleSaveServiceCaller,
) {
    private val TAG = "ForkyzCurrentPzlHldr";

    var board : Playboard? = null
        private set
    var puzHandle : PuzHandle? = null
        private set

    // even though using a mutex need one thread to ensure mutex lock
    // in order of request
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val ioScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val ioLock = Mutex()

    init { setupObservers() }

    /**
     * Load puzzle and set as current board
     *
     * Delegates to io and calls back on main thread when done.
     */
    @MainThread
    fun loadPuzzle(
        puzHandle : PuzHandle,
        cb : Runnable,
        onErr : Consumer<IOException>,
    ) {
        ioScope.launch {
            ioLock.lock()
            // main scope needed because currently settings can only get
            // on main thread (because they return on main thread)
            mainScope.launch {
                val preserveCorrect
                    = settings.livePlayPreserveCorrectLettersInShowErrors
                        .first()
                val deleteCrossingMode
                    = settings.livePlayDeleteCrossingMode.first()
                val showErrorsGrid = settings.livePlayShowErrorsGrid.first()
                val showErrorsCursor = settings.livePlayShowErrorsCursor.first()
                val showErrorsClue = settings.livePlayShowErrorsClue.first()
                val toggleBeforeMove = settings.livePlayToggleBeforeMove.first()
                val movementStrategy = settings.livePlayMovementStrategy.first()
                val skipFilled = settings.livePlaySkipFilled.first()
                val undoEnabled = settings.livePlayPlayLetterUndoEnabled.first()
                val scratchMode = settings.livePlayScratchMode.first()
                val splitWords = settings.liveKeyboardSplitWords.first()
                fileHandlerProvider.get() { fileHandler ->
                    // on main thread so go back to io
                    ioScope.launch {
                        try {
                            val puz = fileHandler.load(puzHandle);
                            if (puz == null || puz.getBoxes() == null) {
                                throw IOException(
                                    "Puzzle is null or contains no boxes."
                                )
                            }
                            mainScope.launch (Dispatchers.Main) {
                                board = Playboard(
                                    puz,
                                    movementStrategy,
                                    preserveCorrect,
                                    deleteCrossingMode,
                                    showErrorsCursor,
                                    showErrorsClue,
                                    showErrorsGrid,
                                    toggleBeforeMove,
                                    skipFilled,
                                    undoEnabled,
                                    scratchMode,
                                    splitWords,
                                )
                                this@CurrentPuzzleHolder.puzHandle = puzHandle
                                cb.run()
                            }
                        } catch (e : IOException) {
                            mainScope.launch { onErr.accept(e) }
                        } finally {
                            ioLock.unlock()
                        }
                    }
                }
            }
        }
    }

    @MainThread
    fun saveBoard() {
        puzzleSaveServiceCaller.saveBoard(this)
    }

    /**
     * Save board, callback when done, and block future saves
     *
     * Designed for when the app is about to be killed. Blocking future
     * saves makes it ok to kill the app in the callback.
     */
    fun saveBoardForApplicationExit(cb : () -> Unit) {
        saveBoardService(
            cbWithLock = {
                // cancel ioScope so no more saves
                ioScope.cancel()
                cb()
            }
        )
    }

    @MainThread
    fun getFreshCurrentPuzzleMeta(cb : Consumer<PuzMetaFile?>) {
        val puz = board?.getPuzzle()
        val curHandle = puzHandle
        if (curHandle == null || puz == null)
            return

        ioScope.launch {
            ioLock.lock()
            mainScope.launch {
                fileHandlerProvider.get() { fileHandler ->
                    ioScope.launch {
                        val meta = fileHandler.getRefreshedPuzMetaFile(
                            curHandle,
                            puz,
                        )
                        ioLock.unlock()
                        mainScope.launch { cb.accept(meta) }
                    }
                }
            }
        }
    }

    /**
     * Save board from save service
     *
     * App should use saveBoard(), PuzzleSaveServiceCaller should use
     * this.
     *
     * @param cb call back after all done
     * @param cbWithLock call back after saved but before ioLock is
     * released
     */
    fun saveBoardService(
        cb : suspend () -> Unit = { },
        cbWithLock : suspend () -> Unit = { }
    ) {
        val puz = board?.getPuzzle()
        val curHandle = puzHandle
        if (puz == null || curHandle == null) {
            AppLogger.i(TAG, "No puzzle currently loaded to save.")
            ioScope.launch {
                cbWithLock()
                cb()
            }
        } else {
            ioScope.launch {
                ioLock.lock()
                mainScope.launch {
                    fileHandlerProvider.get() { fileHandler ->
                        ioScope.launch {
                            try {
                                fileHandler.save(puz, curHandle)
                            } catch (e : IOException) {
                                AppLogger.e(TAG, "Error saving puzzle.")
                                e.printStackTrace()
                            } finally {
                                cbWithLock()
                                ioLock.unlock()
                                cb()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupObservers() {
        // these keep the current board up to date with changes in
        // settings -- rather than expecting activities to do it on
        // resume.
        // Ok to observe forever because this is a singleton that lives
        // forever.
        settings.livePlayPreserveCorrectLettersInShowErrors
            .observeForeverWithProcessLifecycle() {
                board?.setPreserveCorrectLettersInShowErrors(it)
            }
        settings.livePlayDeleteCrossingMode
            .observeForeverWithProcessLifecycle() {
                board?.setDeleteCrossingMode(it)
            }
        settings.livePlayShowErrorsGrid.observeForeverWithProcessLifecycle() {
            board?.setShowErrorsGrid(it)
        }
        settings.livePlayShowErrorsCursor.observeForeverWithProcessLifecycle() {
            board?.setShowErrorsCursor(it)
        }
        settings.livePlayShowErrorsClue.observeForeverWithProcessLifecycle() {
            board?.setShowErrorsClue(it)
        }
        settings.livePlayToggleBeforeMove.observeForeverWithProcessLifecycle() {
            board?.setToggleBeforeMove(it)
        }
        settings.livePlayMovementStrategy.observeForeverWithProcessLifecycle() {
            board?.setMovementStrategy(it)
        }
        settings.livePlaySkipFilled.observeForeverWithProcessLifecycle {
            board?.setSkipCompletedLetters(it)
        }
        settings.livePlayPlayLetterUndoEnabled
            .observeForeverWithProcessLifecycle {
                board?.setPlayLetterUndoStackEnabled(it)
            }
        settings.livePlayScratchMode.observeForeverWithProcessLifecycle {
            board?.setScratchMode(it)
        }
        settings.liveKeyboardSplitWords.observeForeverWithProcessLifecycle {
            board?.setSplitResponses(it)
        }
    }
}

