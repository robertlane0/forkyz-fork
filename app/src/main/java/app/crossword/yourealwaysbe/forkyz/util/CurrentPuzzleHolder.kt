
package app.crossword.yourealwaysbe.forkyz.util

import java.io.IOException
import java.util.function.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.CoroutineScope

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.annotation.MainThread
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

import org.koin.android.ext.android.inject

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle
import app.crossword.yourealwaysbe.forkyz.util.files.PuzMetaFile
import app.crossword.yourealwaysbe.forkyz.util.observeForeverWithLifecycle
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils
import app.crossword.yourealwaysbe.puz.Playboard

/**
 * Holder for the current puzzle being played
 *
 * Singleton and app-wide. Persists until application destroyed
 * (property of Hilt singletons). Can be injected into whichever
 * class needs access to get/set the current puzzle.
 */
public class CurrentPuzzleHolder(
    val appContext : Context,
    val settings : ForkyzSettings,
    val fileHandlerProvider : FileHandlerProvider,
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
                settings.getPlayPreserveCorrectLettersInShowErrors() {
                    preserveCorrect ->
                settings.getPlayDeleteCrossingMode() { deleteCrossingMode ->
                settings.getPlayShowErrorsGrid() { showErrorsGrid ->
                settings.getPlayShowErrorsCursor() { showErrorsCursor ->
                settings.getPlayShowErrorsClue() { showErrorsClue ->
                settings.getPlayToggleBeforeMove() { toggleBeforeMove ->
                settings.getPlayMovementStrategy() { movementStrategy ->
                settings.getPlaySkipFilled() { skipFilled ->
                settings.getPlayPlayLetterUndoEnabled() { undoEnabled ->
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
                                    undoEnabled
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
                }}}}}}}}}}
            }
        }
    }

    /**
     * Save current board
     *
     * Delegates to io thread or uses a foreground service if app in
     * foreground. Foreground service is less likely to be killed than a
     * plain thread.
     */
    @MainThread
    fun saveBoard() {
        // If calling from onPause the app is usually in the foreground,
        // except if onPause is being called during the wakeup cycle, in
        // which case you get paused without ever being foregrounded, so you
        // get an exception.
        try {
            Log.i(TAG, "Saving board on foreground")
            ContextCompat.startForegroundService(
                appContext,
                Intent(appContext, PuzzleSaveService::class.java),
            )
        // use IllegalState as ForegroundServiceNotAllowedException
        // requires API 31
        } catch (e : IllegalStateException) {
            Log.i(
                TAG,
                "Save on foreground not allowed, save in background"
            )
            saveBoardService()
        }
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
     * App should use saveBoard()
     */
    fun saveBoardService(cb : () -> Unit = { }) {
        val puz = board?.getPuzzle()
        val curHandle = puzHandle
        if (puz == null || curHandle == null) {
            Log.e(TAG, "No puzzle associated to the board to save.")
            cb()
        } else {
            ioScope.launch {
                ioLock.lock()
                mainScope.launch {
                    fileHandlerProvider.get() { fileHandler ->
                        ioScope.launch {
                            try {
                                fileHandler.save(puz, curHandle)
                            } catch (e : IOException) {
                                Log.e(TAG, "Error saving puzzle.")
                                e.printStackTrace()
                            } finally {
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
            .observeForeverWithLifecycle() {
                board?.setPreserveCorrectLettersInShowErrors(it)
            }
        settings.livePlayDeleteCrossingMode.observeForeverWithLifecycle() {
            board?.setDeleteCrossingMode(it)
        }
        settings.livePlayShowErrorsGrid.observeForeverWithLifecycle() {
            board?.setShowErrorsGrid(it)
        }
        settings.livePlayShowErrorsCursor.observeForeverWithLifecycle() {
            board?.setShowErrorsCursor(it)
        }
        settings.livePlayShowErrorsClue.observeForeverWithLifecycle() {
            board?.setShowErrorsClue(it)
        }
        settings.livePlayToggleBeforeMove.observeForeverWithLifecycle() {
            board?.setToggleBeforeMove(it)
        }
        settings.livePlayMovementStrategy.observeForeverWithLifecycle() {
            board?.setMovementStrategy(it)
        }
        settings.livePlaySkipFilled.observeForeverWithLifecycle {
            board?.setSkipCompletedLetters(it)
        }
        settings.livePlayPlayLetterUndoEnabled.observeForeverWithLifecycle {
            board?.setPlayLetterUndoStackEnabled(it)
        }
    }
}

class PuzzleSaveService() : Service() {
    companion object {
        @JvmField
        final val NOTIFICATION_CHANNEL_ID = "forkyz.save"
        private final val NOTIFICATION_ID = 10102
    }

    private val context : Context by inject()
    private val currentPuzzleHolder : CurrentPuzzleHolder by inject()
    private val utils : AndroidVersionUtils by inject()

    override fun onBind(intent : Intent): IBinder? {
        return null
    }

    override fun onStartCommand(
        intent : Intent,
        flags : Int,
        startId : Int,
    ) : Int {
        val notification = NotificationCompat.Builder(
            this,
            NOTIFICATION_CHANNEL_ID,
        ).setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle(context.getString(R.string.saving_puzzle))
            .setWhen(System.currentTimeMillis())
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            utils.foregroundServiceTypeShortService(),
        )

        currentPuzzleHolder.saveBoardService(this::stopSelf)

        return START_STICKY
    }
}

