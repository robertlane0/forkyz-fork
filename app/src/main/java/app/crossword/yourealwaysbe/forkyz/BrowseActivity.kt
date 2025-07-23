
package app.crossword.yourealwaysbe.forkyz

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import dagger.hilt.android.AndroidEntryPoint

import com.leinardi.android.speeddial.compose.FabWithLabel
import com.leinardi.android.speeddial.compose.SpeedDial
import com.leinardi.android.speeddial.compose.SpeedDialOverlay
import com.leinardi.android.speeddial.compose.SpeedDialState

import app.crossword.yourealwaysbe.forkyz.SettingsActivity
import app.crossword.yourealwaysbe.forkyz.menu.BaseMenuModifier
import app.crossword.yourealwaysbe.forkyz.menu.MenuDropdownHeading
import app.crossword.yourealwaysbe.forkyz.menu.MenuHelp
import app.crossword.yourealwaysbe.forkyz.menu.MenuSettings
import app.crossword.yourealwaysbe.forkyz.menu.MenuSubEntry
import app.crossword.yourealwaysbe.forkyz.menu.MenuText
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider
import app.crossword.yourealwaysbe.forkyz.settings.AccessorSetting
import app.crossword.yourealwaysbe.forkyz.settings.BrowseSwipeAction
import app.crossword.yourealwaysbe.forkyz.settings.DayNightMode
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.view.CircleProgressBar
import app.crossword.yourealwaysbe.forkyz.view.DownloadDialog
import app.crossword.yourealwaysbe.forkyz.view.OKCancelDialog
import app.crossword.yourealwaysbe.forkyz.view.OKDialog
import app.crossword.yourealwaysbe.forkyz.view.RatingBar
import app.crossword.yourealwaysbe.forkyz.view.SearchBar
import app.crossword.yourealwaysbe.forkyz.view.TwoButtonDialog

import app.crossword.yourealwaysbe.forkyz.view.rememberSwipeToDismissBoxState
import app.crossword.yourealwaysbe.forkyz.view.SwipeToDismissBox
import app.crossword.yourealwaysbe.forkyz.view.SwipeToDismissBoxValue

/**
 * Notification that intent imports have finished
 *
 * Esp if URL download, the activity might get restarted due to
 * config changes while the import is happening. To handle this, the
 * compete message is broadcast to the current version of the
 * activity.
 */
private val INTENT_IMPORT_COMPLETE_ACTION
    = "app.crossword.yourealwaysbe.INTENT_IMPORT_COMPLETE_ACTION"

/**
 * ID of task that completed intent import
 */
private val INTENT_IMPORT_TASK_ID
    = "app.crossword.yourealwaysbe.INTENT_IMPORT_TASK_ID"

// allow import of all docs (parser will take care of detecting if it's a
// puzzle that's recognised)
private val IMPORT_MIME_TYPE =  "*/*"

private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE\ndd MMM\nyyyy")
private val HORIZONTAL_PADDING = 16.dp
private val FAB_OPEN_ANGLE = 45F
private val MIN_PUZZLE_ITEM_HEIGHT = 72.dp

@AndroidEntryPoint
class BrowseActivity() : ForkyzActivity() {
    companion object {
        // TODO: JvmField can be removed when callers are Kotlin

        /**
         * Request other browsers close
         *
         * An intent to broadcast from BrowseActivity to tell all other
         * BrowseActivities to close down (avoid multiple instances on same
         * file system)
         *
         * launchMode singleTask is not really what we want here as it
         * prevents the user from returning to a puzzle from the home screen
         * (annoying)
         */
        @JvmField
        val BROWSER_CLOSE_ACTION
            = "app.crossword.yourealwaysbe.BROWSER_CLOSE_ACTION"

        /**
         * The task ID of the BrowseActivity requesting the close
         *
         * To avoid closing self
         */
        @JvmField
        val BROWSER_CLOSE_TASK_ID
            = "app.crossword.yourealwaysbe.BROWSER_CLOSE_TASK_ID"

        /**
         * Whether some succeeded (bool)
         */
        @JvmField
        val INTENT_IMPORT_SUCCESS
            = "app.crossword.yourealwaysbe.INTENT_IMPORT_SUCCESS"
    }

    @Inject
    lateinit var fileHandlerProvider : FileHandlerProvider

    private val viewModel by viewModels<BrowseActivityViewModel>()
    private lateinit var nm : NotificationManagerCompat
    private var pendingImports : Collection<Uri>? = null

    /**
     * See note for BROWSER_CLOSE_ACTION
     */
    private val closeActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context : Context?, intent : Intent?) {
            if (intent?.action == BROWSER_CLOSE_ACTION) {
                val myTaskId = (this@BrowseActivity).taskId
                val otherTaskId
                    = intent.getIntExtra(BROWSER_CLOSE_TASK_ID, myTaskId)

                if (myTaskId != otherTaskId) {
                    utils.finishAndRemoveTask(this@BrowseActivity)
                }
            }
        }
    }

    /**
     * See note for INTENT_IMPORT_COMPLETE_ACTION
     */
    private val intentImportCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context : Context?, intent : Intent?) {
            if (intent?.action == INTENT_IMPORT_COMPLETE_ACTION) {
                val myTaskId = (this@BrowseActivity).taskId
                val otherTaskId
                    = intent.getIntExtra(INTENT_IMPORT_TASK_ID, myTaskId)

                if (myTaskId == otherTaskId) {
                    viewModel.setImportedNowExitResult(
                        intent.getBooleanExtra(INTENT_IMPORT_SUCCESS, false),
                    )
                }
            }
        }
    }

    private val getImportURI = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
        ActivityResultCallback<List<Uri>>() { uris ->
            viewModel.importURIs(uris)
        },
    )

    /**
     * When POST_NOTIFICATIONS permission needed
     */
    private val notificationPermissionLauncher
        = registerForActivityResult(RequestPermission()) { isGranted ->
            if (!isGranted)
                viewModel.flagNotificationPermissionsDenied()
        }

    @OptIn(ExperimentalMaterial3Api::class,ExperimentalAnimationApi::class)
    override protected fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)

        nm = NotificationManagerCompat.from(this)

        // ask others to close down
        sendBroadcast(
            Intent(BROWSER_CLOSE_ACTION).putExtra(
                BROWSER_CLOSE_TASK_ID, getTaskId()
            ).setPackage(getPackageName())
        )

        // listen for broadcasts
        ContextCompat.registerReceiver(
            this,
            closeActionReceiver,
            IntentFilter(BROWSER_CLOSE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ContextCompat.registerReceiver(
            this,
            intentImportCompleteReceiver,
            IntentFilter(INTENT_IMPORT_COMPLETE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent() {
            Launchers()

            AppTheme() {
                var speedDialState by rememberSaveable {
                    mutableStateOf(SpeedDialState.Collapsed)
                }
                val toggleSpeedDial = {
                    speedDialState = speedDialState.toggle()
                }
                val puzzleListState = rememberLazyListState()
                val state by viewModel.uiState.observeAsState()
                val isSelecting = state?.isSelecting ?: false

                Scaffold(
                    topBar = { TopAppBar() },
                    floatingActionButton = {
                        AnimatedVisibility(
                            visible = !isSelecting && (
                                puzzleListState.lastScrolledBackward
                                || !puzzleListState.canScrollBackward
                            ),
                        ) {
                            SpeedDial(
                                state = speedDialState,
                                onFabClick = {
                                    speedDialState = speedDialState.toggle()
                                },
                                fabClosedContent = { AddPuzzleIcon() },
                                fabClosedContainerColor
                                    = MaterialTheme.colorScheme.primary,
                                fabClosedContentColor
                                    = MaterialTheme.colorScheme.onPrimary,
                                // doesn't rotate shape in compose
                                fabOpenedContent = {
                                    AddPuzzleIcon(
                                        Modifier.rotate(FAB_OPEN_ANGLE),
                                    )
                                },
                                fabAnimationRotateAngle = FAB_OPEN_ANGLE,
                                reverseAnimationOnClose = true,
                            ) {
                                item { DownloadPuzzlesButton(toggleSpeedDial) }
                                item { ImportPuzzlesButton(toggleSpeedDial) }
                                item { OnlineSourcesButton(toggleSpeedDial) }
                            }
                        }
                    },
                ) { innerPadding ->
                    ActivityBody(
                        modifier = Modifier.padding(innerPadding),
                        puzzleListState = puzzleListState,
                    )
                    SpeedDialOverlay(
                        visible = speedDialState.isExpanded(),
                        onClick = toggleSpeedDial,
                    )
                    DisplayDialogs()
                }
            }
        }

        handleCreateIntent(savedInstanceState);
    }

    override protected fun onResume() {
        super.onResume()

        if (hasPendingImports) {
            getPendingImports()?.let { importUris ->
                clearPendingImports()
                onImportURIsAndFinish(importUris)
            }
        }

        viewModel.refreshCurrentPuzzleMeta()
        viewModel.autoDownloadIfRequired()
        viewModel.processToImportDirectory()
    }

    // don't use Compose because nothing in view requests initial focus
    // and key events in compose need focus set
    override fun onKeyDown(keyCode : Int, event : KeyEvent?) : Boolean {
        // for parity with onKeyUp
        return when(keyCode) {
            KeyEvent.KEYCODE_ESCAPE -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode : Int, event : KeyEvent?) : Boolean {
        val isSelecting = viewModel.uiState.value?.isSelecting ?: false
        return when(keyCode) {
            KeyEvent.KEYCODE_ESCAPE -> {
                if (isSelecting)
                    viewModel.clearSelection()
                else
                    finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun TopAppBar() {
        val state by viewModel.uiState.observeAsState()
        val titleId = if (state?.isViewArchive ?: false)
            R.string.title_view_archives
        else
            R.string.title_view_crosswords

        if (state?.isSelecting ?: false) {
            themeHelper.ForkyzTopAppBar(
                title = {
                    val numSelected = state?.selectedPuzzleIDs?.size ?: 0
                    Text(
                        pluralStringResource(
                            id = R.plurals.num_selected,
                            count = numSelected,
                            numSelected,
                        ),
                    )
                },
                actions = {
                    DeleteSelectedButton()
                    ToggleArchiveSelectedButton()
                },
                onBack = viewModel::clearSelection,
            )
        } else {
            themeHelper.ForkyzTopAppBar(
                title = { Text(stringResource(titleId)) },
                actions = {
                    FilterPuzzlesButton()
                    ThemeButton()
                    OverflowMenu()
                },
            )
        }
    }

    @Composable
    private fun DeleteSelectedButton() {
        IconButton(onClick = viewModel::deleteSelectedPuzzles) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.delete_selected),
            )
        }
    }

    @Composable
    private fun ToggleArchiveSelectedButton() {
        val state by viewModel.uiState.observeAsState()
        val isArchive = state?.isViewArchive ?: false
        IconButton(onClick = viewModel::toggleArchiveSelectedPuzzles) {
            Icon(
                painter = painterResource(toggleArchiveIcon(isArchive)),
                contentDescription
                    = stringResource(R.string.toggle_archive_selected),
            )
        }
    }

    private fun toggleArchiveIcon(isArchive : Boolean) : Int {
        if (isArchive)
            return R.drawable.ic_menu_unarchive
        else
            return R.drawable.ic_menu_archive
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun AddPuzzleIcon(modifier : Modifier = Modifier) {
        Icon(
            modifier = modifier,
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.add_puzzle),
        )
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun SpeedDialButton(
        onClick : () -> Unit,
        labelContent : @Composable () -> Unit,
        painter : Painter,
        contentDescription : String,
        close : () -> Unit,
    ) {
        FabWithLabel(
            onClick = {
                close()
                onClick()
            },
            labelContent = labelContent,
            fabContainerColor = MaterialTheme.colorScheme.primary,
            fabContentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
            )
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun OnlineSourcesButton(close : () -> Unit) {
        SpeedDialButton(
            onClick = {
                val i = Intent(Intent.ACTION_VIEW);
                i.setData(
                    getString(R.string.online_sources_url).toUri()
                )
                startActivity(i);
            },
            labelContent = { Text(stringResource(R.string.online_sources)) },
            painter = painterResource(R.drawable.ic_online_sources),
            contentDescription = stringResource(R.string.online_sources),
            close = close,
        )
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun ImportPuzzlesButton(close : () -> Unit) {
        SpeedDialButton(
            onClick = { getImportURI.launch(IMPORT_MIME_TYPE) },
            labelContent = { Text(stringResource(R.string.import_puzzle)) },
            painter = painterResource(R.drawable.ic_import),
            contentDescription = stringResource(R.string.import_puzzle),
            close = close,
        )
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun DownloadPuzzlesButton(close : () -> Unit) {
        SpeedDialButton(
            onClick = this::showDownloadDialog,
            labelContent = { Text(stringResource(R.string.download_puzzles)) },
            painter = painterResource(R.drawable.ic_download),
            contentDescription = stringResource(R.string.download_puzzles),
            close = close,
        )
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun FilterPuzzlesButton() {
        IconButton(onClick = viewModel::startPuzzleFilter) {
            Icon(
                Icons.Filled.Search,
                stringResource(R.string.filter_puzzles),
            )
        }
    }

    @Composable
    private fun ThemeButton() {
        val state by viewModel.uiState.observeAsState()

        val dnmIcon = when (state?.dayNightMode) {
            DayNightMode.DNM_NIGHT -> R.drawable.night_mode
            DayNightMode.DNM_SYSTEM -> R.drawable.system_daynight_mode
            else -> R.drawable.day_mode
        }

        IconButton(onClick = viewModel::nextDayNightMode) {
            Icon(
                painter = painterResource(dnmIcon),
                stringResource(R.string.filter_puzzles),
            )
        }
    }

    @Composable
    private fun OverflowMenu() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()

        IconButton(onClick = { viewModel.expandMenu(BrowseSubMenu.MAIN) }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.overflow),
            )
        }
        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = state.expanded == BrowseSubMenu.MAIN,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuSort()
            MenuCleanup()
            MenuSwitchLists()
            MenuHelp(
                activity = this@BrowseActivity,
                helpHTMLAsset = "filescreen.html",
                onDismiss = viewModel::dismissMenu,
            )
            MenuSettings(this@BrowseActivity, viewModel::dismissMenu)
        }
        MenuSortSub()
    }

    @Composable
    private fun MenuSort() {
        MenuSubEntry(
            text = { MenuText(stringResource(R.string.sort)) },
            onClick = {
                viewModel.expandMenu(BrowseSubMenu.SORT_ORDER)
            }
        )
    }

    @Composable
    private fun MenuCleanup() {
        DropdownMenuItem(
            text = { MenuText(stringResource(R.string.cleanup)) },
            onClick = {
                viewModel.cleanupPuzzles()
                viewModel.dismissMenu()
            }
        )
    }

    @Composable
    private fun MenuSwitchLists() {
        val state by viewModel.uiState.observeAsState()
        val isArchive = state?.isViewArchive ?: false

        val menuText = if (isArchive)
            R.string.title_view_crosswords
        else
            R.string.title_view_archives

        DropdownMenuItem(
            text = { MenuText(stringResource(menuText)) },
            onClick = {
                viewModel.startLoadPuzzleList(!isArchive)
                viewModel.dismissMenu()
            }
        )
    }

    @Composable
    private fun MenuSortSub() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = state.expanded == BrowseSubMenu.SORT_ORDER,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                R.string.sort,
                onClick = viewModel::closeMenu,
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.by_date_desc)) },
                onClick = {
                    viewModel.setSortOrder(AccessorSetting.AS_DATE_DESC)
                    viewModel.dismissMenu()
                }
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.by_date_asc)) },
                onClick = {
                    viewModel.setSortOrder(AccessorSetting.AS_DATE_ASC)
                    viewModel.dismissMenu()
                }
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.by_source)) },
                onClick = {
                    viewModel.setSortOrder(AccessorSetting.AS_SOURCE)
                    viewModel.dismissMenu()
                }
            )
        }
    }

    @Composable
    private fun ActivityBody(
        modifier : Modifier,
        puzzleListState : LazyListState,
    ) {
        Box(modifier) {
            Column(Modifier.fillMaxWidth().fillMaxHeight()) {
                PuzzleSearchBar()
                IntroMessages()
                PuzzleList(
                    modifier = Modifier.weight(1.0F),
                    listState = puzzleListState,
                )
                UIBusy()
            }
        }
    }

    @Composable
    private fun PuzzleSearchBar() {
        val state by viewModel.uiState.observeAsState()
        state?.puzzleFilter?.let { puzzleFilter ->
            SearchBar(
                showSearchBar = true,
                searchTerm = puzzleFilter,
                onValueChange = viewModel::setPuzzleFilter,
                onClose = viewModel::clearPuzzleFilter,
                hint = stringResource(R.string.filter_puzzles_hint),
            )
        }
    }

    @Composable
    private fun IntroMessages() {
        val state by viewModel.uiState.observeAsState()
        if (state?.displayNoPuzzlesMessage ?: true) {
            val noPuzzlesMsg = if (state?.isViewArchive ?: false)
                R.string.no_puzzles
            else
                R.string.no_puzzles_download_or_configure_storage

            Text(
                modifier = Modifier.padding(30.dp),
                text = stringResource(noPuzzlesMsg),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (state?.displayInternalStorageMessage ?: false) {
            Text(
                modifier = Modifier.padding(30.dp),
                text = stringResource(R.string.internal_storage_description),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class,ExperimentalAnimationApi::class)
    private fun PuzzleList(
        modifier : Modifier,
        listState : LazyListState,
    ) {
        val state by viewModel.uiState.observeAsState()
        val puzzles = state?.puzzleList ?: listOf()
        val refreshState = rememberPullToRefreshState()

        // move up if items above appear
        // thanks: https://stackoverflow.com/a/74326906
        LaunchedEffect(puzzles.firstOrNull()?.puzzles?.firstOrNull()?.id) {
            if (!puzzles.isEmpty())
                listState.animateScrollToItem(0)
        }

        PullToRefreshBox(
            modifier = modifier.fillMaxWidth(),
            isRefreshing = state?.isSwipeRefreshing ?: false,
            onRefresh = viewModel::swipeRefreshPuzzleList,
            state = refreshState,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                state = listState,
            ) {
                puzzles.forEach { puzGroup ->
                    stickyHeader {
                        key(puzGroup.header) {
                            PuzzleListHeader(puzGroup.header)
                        }
                    }
                    itemsIndexed(
                        items = puzGroup.puzzles,
                        key = { _, pe -> pe.id },
                    ) { idx, pe ->
                        Puzzle(pe)
                    }
                }
            }
        }
    }

    @Composable
    private fun UIBusy() {
        val state by viewModel.uiState.observeAsState()
        if (state?.isBusy ?: false) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical=10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
                Text(
                    modifier = Modifier.padding(start=10.dp),
                    text = stringResource(R.string.please_wait),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
    }

    @Composable
    private fun PuzzleListHeader(header : String) {
        Text(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal=HORIZONTAL_PADDING)
                .padding(top=16.dp, bottom=8.dp),
            text = AnnotatedString.fromHtml(header),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }

    @Composable
    private fun Puzzle(pe : PuzEntry) {
        val state by viewModel.uiState.observeAsState()
        val swipeState = rememberSwipeToDismissBoxState()

        LaunchedEffect(swipeState.settledValue) {
            if (
                swipeState.settledValue == SwipeToDismissBoxValue.StartToEnd
                || swipeState.settledValue == SwipeToDismissBoxValue.EndToStart
            ) {
                viewModel.swipePuzzle(pe.id)
                swipeState.snapTo(SwipeToDismissBoxValue.Settled)
            }
        }

        SwipeToDismissBox(
            state = swipeState,
            enabled = !(state?.disableSwipe ?: true),
            backgroundContent = {
                val background = when (swipeState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd,
                    SwipeToDismissBoxValue.EndToStart,
                        -> Modifier.background(
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    else -> Modifier
                }
                val iconAlign = when (swipeState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart,
                    SwipeToDismissBoxValue.LeaningStart
                        -> Alignment.CenterEnd
                    else -> Alignment.CenterStart
                }
                val isArchive = state?.isViewArchive ?: false

                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                        .then(background),
                    contentAlignment = iconAlign,
                ) {
                    when (swipeState.targetValue) {
                        SwipeToDismissBoxValue.Settled -> { }
                        else -> if (
                            state?.swipeAction == BrowseSwipeAction.BSA_ARCHIVE
                        ) {
                            SwipeActionIcon(
                                drawableId = toggleArchiveIcon(isArchive),
                                contentDescriptionId = R.string.toggle_archive,
                            )
                        } else {
                            SwipeActionIcon(
                                drawableId = R.drawable.ic_delete,
                                contentDescriptionId = R.string.delete,
                            )
                        }
                    }
                }
            },
        ) {
            PuzzleDetails(pe)
        }
    }

    @Composable
    private fun SwipeActionIcon(
        drawableId : Int,
        contentDescriptionId : Int,
    ) {
        Icon(
            modifier = Modifier.size(MIN_PUZZLE_ITEM_HEIGHT * 0.65F)
                .padding(10.dp),
            painter = painterResource(drawableId),
            contentDescription = stringResource(contentDescriptionId),
        )
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun PuzzleDetails(pe : PuzEntry) {
        val state by viewModel.uiState.observeAsState()
        val percentFilled = if (state?.showPercentageCorrect ?: false)
            pe.percentComplete
        else
            pe.percentFilled
        val isSelected = pe.id in (state?.selectedPuzzleIDs ?: setOf())
        val backgroundColor = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant
        val textColor = if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant

        Row(
            modifier = Modifier.heightIn(min=MIN_PUZZLE_ITEM_HEIGHT)
                .background(backgroundColor)
                .combinedClickable(
                    onClick = {
                        if (state?.isSelecting ?: false)
                            viewModel.toggleSelectPuzzle(pe.id)
                        else
                            viewModel.loadPuzzle(pe.id)
                    },
                    onLongClick = { viewModel.toggleSelectPuzzle(pe.id) },
                ).padding(vertical=8.dp, horizontal=HORIZONTAL_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state?.sortOrder == AccessorSetting.AS_SOURCE) {
                // wide enough for a wednesday
                val dateWidth = with(LocalDensity.current) {
                    81.sp.toDp()
                }
                Text(
                    modifier = Modifier.width(dateWidth)
                        .padding(end=4.dp),
                    minLines = 3,
                    maxLines = 3,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    text = DATE_FORMAT.format(pe.date),
                    color = textColor,
                )
            }
            AndroidView(
                modifier = Modifier.size(56.dp),
                factory = { context -> CircleProgressBar(context) },
                update = { view ->
                    view.percentFilled = percentFilled
                    view.complete = pe.percentComplete == 100
                }
            )
            Column(Modifier.weight(1.0F).fillMaxWidth()) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.weight(1.0F)
                            .padding(horizontal=10.dp)
                            .padding(top=2.dp),
                        minLines = 1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        text = AnnotatedString.fromHtml(pe.title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = textColor,
                    )

                    val showHasSolution = state?.indicateIfSolution ?: false
                        && pe.hasSolution

                    if (showHasSolution) {
                        Icon(
                            modifier = Modifier.width(10.dp),
                            painter = painterResource(R.drawable.ic_solution),
                            contentDescription
                                = stringResource(R.string.has_solution_desc),
                            tint = textColor,
                        )
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.weight(1.0F)
                            .padding(horizontal=10.dp),
                        maxLines = 3,
                        text = AnnotatedString.fromHtml(pe.caption),
                        color = textColor,
                    )
                    if (state?.showRatings ?: false) {
                        RatingBar(
                            modifier = Modifier.padding(top=4.dp),
                            numStars = integerResource(R.integer.max_rating),
                            stepSize = 1.0F,
                            isIndicator = true,
                            style = android.R.attr.ratingBarStyleSmall,
                            rating = pe.rating.code.toFloat(),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun Launchers() {
        val state by viewModel.uiState.observeAsState()
        val loadEvent by viewModel.puzzleLoaded.collectAsStateWithLifecycle()

        LaunchedEffect(loadEvent) {
            if (loadEvent) {
                viewModel.clearPuzzleLoaded()
                startActivity(
                    Intent(this@BrowseActivity, PlayActivity::class.java)
                )
            }
        }

        val isDownloadOnStartUp = state?.isDownloadOnStartUp ?: false
        val needsNotificationCheck = (
            state?.isBackgroundDownloadEnabled ?: false
            || isDownloadOnStartUp
        )
        LaunchedEffect(needsNotificationCheck) {
            if (needsNotificationCheck)
                checkRequestNotificationPermissions();
        }

        val toasts by viewModel.toastMessages.collectAsStateWithLifecycle()
        LaunchedEffect(toasts) {
            toasts.forEach { msg ->
                Toast.makeText(
                    this@BrowseActivity,
                    msg,
                    Toast.LENGTH_LONG
                ).show()
            }
            viewModel.clearToastMessages()
        }

        val importFinishResult
            by viewModel.importFinishResult.collectAsStateWithLifecycle()
        LaunchedEffect(importFinishResult) {
            importFinishResult?.let { ifr ->
                sendBroadcast(
                    Intent(INTENT_IMPORT_COMPLETE_ACTION).putExtra(
                        INTENT_IMPORT_TASK_ID,
                        taskId,
                    ).putExtra(
                        INTENT_IMPORT_SUCCESS,
                        !ifr.someFailed && ifr.someSucceeded
                    ).setPackage(getPackageName())
                )
            }
        }
    }

    @Composable
    private fun DisplayDialogs() {
        val state by viewModel.uiState.observeAsState()
        state?.showDialog?.let {
            when (it) {
                BrowseDialog.NOTIFICATIONS_PERMISSION
                    -> NotificationPermissionsDialog()
                BrowseDialog.NEW_VERSION
                    -> NewVersionDialog()
                BrowseDialog.CONFIRM_CLEANUP
                    -> ConfirmCleanupDialog()
                BrowseDialog.NO_CLEANUP_ACTION
                    -> NoCleanupActionDialog()
                BrowseDialog.CONFIRM_DELETE_SELECTED
                    -> ConfirmDeleteSelectedDialog()
                BrowseDialog.NONE -> { }
            }
        }

        val downloadDialog
            by viewModel.downloadDialog.collectAsStateWithLifecycle()
        downloadDialog?.let {
            DownloadDialog(
                viewModel = it,
                onCancel = viewModel::clearDownloadDialog,
                onDownload = viewModel::okDownloadDialog,
                onOpenSettings = {
                    viewModel.clearDownloadDialog()
                    startActivity(
                        Intent(
                            this@BrowseActivity,
                            SettingsActivity::class.java
                        )
                    )
                },
            )
        }

        val exitDialogSuccess by viewModel
            .importedNowExitResult
            .collectAsStateWithLifecycle()
        exitDialogSuccess?.let { success ->
            OKDialog(
                title = stringResource(R.string.imports_title),
                summary = stringResource(
                    if (success)
                        R.string.import_success_long
                    else
                        R.string.import_failure_long
                ),
                onClose = { doExit() }
            )
        }
    }

    @Composable
    private fun NoCleanupActionDialog() {
        OKDialog(
            title = R.string.no_cleanup_action,
            summary = stringResource(R.string.no_cleanup_action_msg),
            onClose = viewModel::clearNoCleanupAction,
        )
    }

    @Composable
    private fun ConfirmCleanupDialog() {
        OKCancelDialog(
            title = R.string.confirm_cleanup,
            summary = R.string.confirm_cleanup_msg,
            onCancel = viewModel::clearConfirmCleanup,
            onOK = viewModel::cleanupPuzzlesConfirmed,
        )
    }

    @Composable
    private fun ConfirmDeleteSelectedDialog() {
        val state by viewModel.uiState.observeAsState()
        val numSelected = state?.selectedPuzzleIDs?.size ?: 0
        OKCancelDialog(
            title = stringResource(R.string.confirm_delete),
            summary = pluralStringResource(
                id = R.plurals.confirm_delete_msg,
                count = numSelected,
            ),
            onCancel = viewModel::cancelDeleteSelectedPuzzles,
            onOK = viewModel::confirmDeleteSelectedPuzzles,
        )
    }

    @Composable
    private fun NotificationPermissionsDialog() {
        TwoButtonDialog(
            title = R.string.disable_notifications,
            summary = R.string.notifications_denied_msg,
            positiveText = R.string.disable_notifications_button,
            onPositive = viewModel::disableNotifications,
            negativeText = R.string.android_app_settings_button,
            onNegative = {
                val appPackage = getPackageName()
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    ("package:" + appPackage).toUri(),
                )
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                viewModel.dismissNotificationsDialog()
            },
        )
    }

    @Composable
    private fun NewVersionDialog() {
        val state by viewModel.uiState.observeAsState()
        val currentVersion = state?.currentVersion ?: ""
        TwoButtonDialog(
            title = stringResource(R.string.new_version_title, currentVersion),
            summary = stringResource(R.string.new_version_message),
            positiveText = stringResource(R.string.view_release_notes),
            onPositive = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "release.html".toUri(),
                    this,
                    HTMLActivity::class.java
                )
                startActivity(intent)
                viewModel.flagSeenCurrentVersion()
            },
            negativeText = stringResource(R.string.close),
            onNegative = viewModel::flagSeenCurrentVersion,
        )
    }

    private fun startLoadPuzzleList() {
        startLoadPuzzleList(viewModel.isViewArchive);
    }

    private fun startLoadPuzzleList(archive : Boolean) {
        viewModel.startLoadPuzzleList(archive)
    }

    private fun showDownloadDialog() {
        checkAndWarnNetworkState()
        checkRequestNotificationPermissions()
        viewModel.showDownloadDialog()
    }

    private fun checkAndWarnNetworkState() {
        if (!utils.hasNetworkConnection(this)) {
            Toast.makeText(
                this,
                R.string.download_but_no_active_network,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    /**
     * Request notification permissions if needed
     *
     * E.g. not if settings block them. Doesn't ask twice in an
     * activities life
     */
    private fun checkRequestNotificationPermissions() {
        if (nm.areNotificationsEnabled())
            return

        val needed = viewModel.uiState
            .value
            ?.isNotificationPermissionNeeded
            ?: false

        if (needed) {
            val showRationale
                = utils.shouldShowRequestNotificationPermissionRationale(
                    this
                )

            if (showRationale) {
                Toast.makeText(
                    this,
                    R.string.notifications_request_rationale,
                    Toast.LENGTH_LONG
                ).show()
            }

            utils.requestPostNotifications(notificationPermissionLauncher)
        }
    }

    private val hasPendingImports : Boolean
        get() {
            return pendingImports != null;
        }

    private fun clearPendingImports() {
        pendingImports = null
    }

    private fun setPendingImport(uri : Uri?) {
        uri?.let { setPendingImports(listOf(it)) }
    }

    private fun setPendingImports(uri : Collection<Uri>?) {
        pendingImports = uri
    }

    private fun getPendingImports() : Collection<Uri>? {
        return pendingImports
    }

    /**
     * Handle create intent given saved state
     *
     * Includes reading pending imports from saved instance state
     */
    private fun handleCreateIntent(savedInstanceState : Bundle?) {
        // if there's a saved state, then this is not the first time we've seen
        // the intent, so ignore it
        if (savedInstanceState != null)
            return

        // If this was started by a file open or a share
        val intent = getIntent()
        val action = intent.action
        if (Intent.ACTION_VIEW == action) {
            // loaded by onResume
            setPendingImport(intent.data)
        } else if (Intent.ACTION_SEND == action) {
            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                setPendingImport(
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri(),
                )
            } else {
                setPendingImport(
                    IntentCompat.getParcelableExtra(
                        intent,
                        Intent.EXTRA_STREAM,
                        Uri::class.java,
                    ),
                )
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action) {
            setPendingImports(
                IntentCompat.getParcelableArrayListExtra(
                    intent,
                    Intent.EXTRA_STREAM,
                    Uri::class.java,
                ),
            )
        }
    }

    /**
     * Import from URI, force reload of puz list if asked
     */
    private fun onImportURIsAndFinish(uris : Collection<Uri>) {
        // finish happens in launchers when result posted
        viewModel.importURIsAndFinish(uris)
    }

    private fun doExit() {
        // ask browse activities to close
        val close = Intent(BROWSER_CLOSE_ACTION)
        // -1 so all browse activities close
        close.putExtra(BROWSER_CLOSE_TASK_ID, -1)
        close.setPackage(getPackageName())
        sendBroadcast(close)
        finish()
    }
}

