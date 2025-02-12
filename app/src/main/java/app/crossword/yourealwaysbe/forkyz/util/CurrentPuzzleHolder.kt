
package app.crossword.yourealwaysbe.forkyz.util

import java.io.IOException
import java.util.function.Consumer
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.CoroutineScope

import androidx.annotation.MainThread

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle
import app.crossword.yourealwaysbe.forkyz.util.files.PuzMetaFile
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.puz.Playboard

/**
 * Holder for the current puzzle being played
 *
 * Singleton and app-wide. Persists until application destroyed
 * (property of Hilt singletons). Can be Hilt-injected into whichever
 * class needs access to get/set the current puzzle.
 */
@Singleton
public class CurrentPuzzleHolder @Inject constructor(
    val settings : ForkyzSettings,
    val fileHandlerProvider : FileHandlerProvider,
) {
    private val LOG = Logger.getLogger(
        CurrentPuzzleHolder::class.toString()
    )

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

    /**
     * Load puzzle and set as current board
     *
     * Delegates to io and calls back on main thread when done.
     */
    @MainThread
    fun loadPuzzle(
        puzMeta : PuzMetaFile,
        cb : Runnable,
        onErr : Consumer<IOException>,
    ) {
        ioScope.launch {
            ioLock.lock()
            // main scope needed because currently settings can only get
            // on main thread (because they return on main thread)
            mainScope.launch {
                settings.getPlayPreserveCorrectLettersInShowErrors() {
                    preserveCorrect ->
                settings.getPlayDeleteCrossingMode() { deleteCrossingMode ->
                settings.getPlayMovementStrategy() { movementStrategy ->
                fileHandlerProvider.get() { fileHandler ->
                    // on main thread so go back to io
                    ioScope.launch {
                        try {
                            val puz = fileHandler.load(puzMeta);
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
                                    deleteCrossingMode
                                )
                                puzHandle = puzMeta.getPuzHandle()
                                cb.run()
                            }
                        } catch (e : IOException) {
                            mainScope.launch { onErr.accept(e) }
                        } finally {
                            ioLock.unlock()
                        }
                    }
                }}}}
            }
        }
    }

    /**
     * Save current board
     *
     * Delegates to io thread
     */
    @MainThread
    fun saveBoard() {
        val puz = board?.getPuzzle()
        if (puz == null) {
            LOG.severe("No puzzle associated to the board to save.")
            return
        }

        ioScope.launch {
            ioLock.lock()
            mainScope.launch {
                fileHandlerProvider.get() { fileHandler ->
                    ioScope.launch {
                        try {
                            fileHandler.save(puz, puzHandle)
                        } catch (e : IOException) {
                            LOG.severe("Error saving puzzle.")
                            e.printStackTrace()
                        } finally {
                            ioLock.unlock()
                        }
                    }
                }
            }
        }
    }

    /**
     * Get/refresh meta of current puzzle
     *
     * Delegates to io thread and calls back on main. Does nothing if
     * nothing loaded. If error, calls back with null.
     */
    @MainThread
    fun getFreshCurrentPuzzleMeta(cb : Consumer<PuzMetaFile?>) {
        if (puzHandle == null)
            return

        ioScope.launch {
            ioLock.lock()
            mainScope.launch {
                fileHandlerProvider.get() { fileHandler ->
                    ioScope.launch {
                        var meta : PuzMetaFile? = null
                        try {
                            meta = fileHandler.loadPuzMetaFile(puzHandle)
                        } catch (e : IOException) {
                            LOG.severe(
                                "Could not get current puzzle meta: " + e,
                            )
                        } finally {
                            ioLock.unlock()
                            mainScope.launch {
                                cb.accept(meta)
                            }
                        }
                    }
                }
            }
        }
    }
}
