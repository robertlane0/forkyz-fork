
package app.crossword.yourealwaysbe.forkyz

import java.time.format.DateTimeFormatter

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.leinardi.android.speeddial.compose.FabWithLabel
import com.leinardi.android.speeddial.compose.SpeedDial
import com.leinardi.android.speeddial.compose.SpeedDialOverlay
import com.leinardi.android.speeddial.compose.SpeedDialState

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
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
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

private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE\ndd MMM\nyyyy")
private val HORIZONTAL_PADDING = 16.dp
private val FAB_OPEN_ANGLE = 45F
private val MIN_PUZZLE_ITEM_HEIGHT = 72.dp

/**
 * Browse page
 *
 * @param openAppSettings open the native application (e.g. Android)
 * settings
 * @param openSettingsPage open the Forkyz settings page
 */
@OptIn(ExperimentalMaterial3Api::class,ExperimentalAnimationApi::class)
@Composable
fun BrowsePage(
    viewModel : BrowsePageViewModel,
    themeHelper : ThemeHelper,
    toast : (String) -> Unit,
    openAppSettings : () -> Unit,
    openHTMLPage : (Int) -> Unit,
    openOnlineSources : () -> Unit,
    openPlayPage : () -> Unit,
    openReleaseNotes : () -> Unit,
    openSettingsPage : () -> Unit,
    getPendingImports : (() -> Collection<Uri>),
    clearPendingImports : (() -> Unit),
    onBack : () -> Unit,
    onCheckAndWarnNetworkState : () -> Unit,
    onCheckRequestNotificationPermissions : () -> Unit,
    onImport : () -> Unit,
    onImportFinish : (ImportFinishResult) -> Unit,
    onExit : () -> Unit,
    setOnImportCompleteCallback : (((Boolean) -> Unit)?) -> Unit,
    setOnImportURIsCallback : (((Collection<Uri>) -> Unit)?) -> Unit,
    setOnNotificationPermissionDeniedCallback : ((() -> Unit)?) -> Unit,
) {
    fun startLoadPuzzleList(archive : Boolean) {
        viewModel.startLoadPuzzleList(archive)
    }

    fun startLoadPuzzleList() {
        startLoadPuzzleList(viewModel.isViewArchive);
    }

    fun showDownloadDialog() {
        onCheckAndWarnNetworkState()
        if (viewModel.uiState.value.isNotificationPermissionNeeded)
            onCheckRequestNotificationPermissions()

        viewModel.showDownloadDialog()
    }

    @Composable
    fun Launchers() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val loadEvent by viewModel.puzzleLoaded.collectAsStateWithLifecycle()

        LaunchedEffect(loadEvent) {
            if (loadEvent) {
                viewModel.clearPuzzleLoaded()
                openPlayPage()
            }
        }

        val isDownloadOnStartUp = state.isDownloadOnStartUp
        val needsNotificationCheck = (
            state.isBackgroundDownloadEnabled
            || isDownloadOnStartUp
        ) &&  state.isNotificationPermissionNeeded
        LaunchedEffect(needsNotificationCheck) {
            if (needsNotificationCheck)
                onCheckRequestNotificationPermissions()
        }

        val toasts by viewModel.toastMessages.collectAsStateWithLifecycle()
        LaunchedEffect(toasts) {
            toasts.forEach(toast)
            viewModel.clearToastMessages()
        }

        val importFinishResult
            by viewModel.importFinishResult.collectAsStateWithLifecycle()
        LaunchedEffect(importFinishResult) {
            importFinishResult?.let(onImportFinish)
        }
    }

    @Composable
    fun DeleteSelectedButton() {
        IconButton(onClick = viewModel::deleteSelectedPuzzles) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.delete_selected),
            )
        }
    }

    fun toggleArchiveIcon(isArchive : Boolean) : Int {
        if (isArchive)
            return R.drawable.ic_menu_unarchive
        else
            return R.drawable.ic_menu_archive
    }

    @Composable
    fun ToggleArchiveSelectedButton() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val isArchive = state.isViewArchive
        IconButton(onClick = viewModel::toggleArchiveSelectedPuzzles) {
            Icon(
                painter = painterResource(toggleArchiveIcon(isArchive)),
                contentDescription
                    = stringResource(R.string.toggle_archive_selected),
            )
        }
    }

    @Composable
    fun AddPuzzleIcon(modifier : Modifier = Modifier) {
        Icon(
            modifier = modifier,
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.add_puzzle),
        )
    }

    @Composable
    fun SpeedDialButton(
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
    fun OnlineSourcesButton(close : () -> Unit) {
        SpeedDialButton(
            onClick = openOnlineSources,
            labelContent = { Text(stringResource(R.string.online_sources)) },
            painter = painterResource(R.drawable.ic_online_sources),
            contentDescription = stringResource(R.string.online_sources),
            close = close,
        )
    }

    @Composable
    fun ImportPuzzlesButton(close : () -> Unit) {
        SpeedDialButton(
            onClick = onImport,
            labelContent = { Text(stringResource(R.string.import_puzzle)) },
            painter = painterResource(R.drawable.ic_import),
            contentDescription = stringResource(R.string.import_puzzle),
            close = close,
        )
    }

    @Composable
    fun DownloadPuzzlesButton(close : () -> Unit) {
        SpeedDialButton(
            onClick = ::showDownloadDialog,
            labelContent = { Text(stringResource(R.string.download_puzzles)) },
            painter = painterResource(R.drawable.ic_download),
            contentDescription = stringResource(R.string.download_puzzles),
            close = close,
        )
    }

    @Composable
    fun FilterPuzzlesButton() {
        IconButton(onClick = viewModel::startPuzzleFilter) {
            Icon(
                Icons.Filled.Search,
                stringResource(R.string.filter_puzzles),
            )
        }
    }

    @Composable
    fun ThemeButton() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        val dnmIcon = when (state.dayNightMode) {
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
    fun MenuSort() {
        MenuSubEntry(
            text = { MenuText(stringResource(R.string.sort)) },
            onClick = {
                viewModel.expandMenu(BrowseSubMenu.SORT_ORDER)
            }
        )
    }

    @Composable
    fun MenuCleanup() {
        DropdownMenuItem(
            text = { MenuText(stringResource(R.string.cleanup)) },
            onClick = {
                viewModel.cleanupPuzzles()
                viewModel.dismissMenu()
            }
        )
    }

    @Composable
    fun MenuSwitchLists() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val isArchive = state.isViewArchive

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
    fun MenuSortSub() {
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
    fun OverflowMenu() {
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
                openHelp = { openHTMLPage(R.raw.filescreen) },
                onDismiss = viewModel::dismissMenu,
            )
            MenuSettings(
                openSettings = openSettingsPage,
                onDismiss = viewModel::dismissMenu,
            )
        }
        MenuSortSub()
    }

    @Composable
    fun TopAppBar() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val titleId = if (state.isViewArchive)
            R.string.title_view_archives
        else
            R.string.title_view_crosswords

        if (state.isSelecting) {
            themeHelper.ForkyzTopAppBar(
                title = {
                    val numSelected = state.selectedPuzzleIDs.size
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
    fun PuzzleSearchBar() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        state.puzzleFilter?.let { puzzleFilter ->
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
    fun IntroMessages() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        if (state.displayNoPuzzlesMessage) {
            val noPuzzlesMsg = if (state.isViewArchive)
                R.string.no_puzzles
            else
                R.string.no_puzzles_download_or_configure_storage

            Text(
                modifier = Modifier.padding(30.dp),
                text = stringResource(noPuzzlesMsg),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (state.displayInternalStorageMessage) {
            Text(
                modifier = Modifier.padding(30.dp),
                text = stringResource(R.string.internal_storage_description),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }

    @Composable
    fun PuzzleListHeader(header : String) {
        Text(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal=HORIZONTAL_PADDING)
                .padding(top=4.dp, bottom=4.dp),
            text = AnnotatedString.fromHtml(header),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }

    @Composable
    fun SwipeActionIcon(
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
    fun PuzzleDetails(pe : PuzEntry) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val percentFilled = if (state.showPercentageCorrect)
            pe.percentComplete
        else
            pe.percentFilled
        val isSelected = pe.id in state.selectedPuzzleIDs
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
                        if (state.isSelecting)
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
            val progressColors = themeHelper.getCircleProgressBarColorScheme()
            CircleProgressBar(
                modifier = Modifier.size(56.dp),
                colors = progressColors,
                complete = pe.percentComplete == 100,
                percentFilled = percentFilled,
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

                    val showHasSolution = state.indicateIfSolution
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
                    if (state.showRatings) {
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
    fun Puzzle(pe : PuzEntry) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
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
            enabled = !state.disableSwipe,
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
                            state.swipeAction == BrowseSwipeAction.BSA_ARCHIVE
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
    fun PuzzleList(
        modifier : Modifier,
        listState : LazyListState,
    ) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val puzzles = state.puzzleList
        val refreshState = rememberPullToRefreshState()

        // move up if items above appear, but not on return to page
        // thanks: https://stackoverflow.com/a/74326906
        var lastScrollToTopID by rememberSaveable {
            mutableStateOf(puzzles.firstOrNull()?.puzzles?.firstOrNull()?.id)
        }
        val firstID = puzzles.firstOrNull()?.puzzles?.firstOrNull()?.id
        LaunchedEffect(firstID) {
            if (!puzzles.isEmpty() && firstID != lastScrollToTopID) {
                lastScrollToTopID = firstID
                listState.animateScrollToItem(0)
            }
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
    fun UIBusy() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        if (state.isBusy) {
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
    fun ActivityBody(
        modifier : Modifier,
        puzzleListState : LazyListState,
    ) {
        val requester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            requester.requestFocus()
        }
        Box(modifier) {
            Column(Modifier.fillMaxWidth().fillMaxHeight()) {
                PuzzleSearchBar()
                IntroMessages()
                PuzzleList(
                    modifier = Modifier.weight(1.0F)
                        .focusable()
                        .focusRequester(requester),
                    listState = puzzleListState,
                )
                UIBusy()
            }
        }
    }

    @Composable
    fun NotificationPermissionsDialog() {
        TwoButtonDialog(
            title = R.string.disable_notifications,
            summary = R.string.notifications_denied_msg,
            positiveText = R.string.disable_notifications_button,
            onPositive = viewModel::disableNotifications,
            negativeText = R.string.android_app_settings_button,
            onNegative = {
                openAppSettings()
                viewModel.dismissNotificationsDialog()
            },
        )
    }

    @Composable
    fun NewVersionDialog() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val currentVersion = state.currentVersion
        TwoButtonDialog(
            title = stringResource(R.string.new_version_title, currentVersion),
            summary = stringResource(R.string.new_version_message),
            positiveText = stringResource(R.string.view_release_notes),
            onPositive = {
                openReleaseNotes()
                viewModel.flagSeenCurrentVersion()
            },
            negativeText = stringResource(R.string.close),
            onNegative = viewModel::flagSeenCurrentVersion,
        )
    }

    @Composable
    fun NoCleanupActionDialog() {
        OKDialog(
            title = R.string.no_cleanup_action,
            summary = stringResource(R.string.no_cleanup_action_msg),
            onClose = viewModel::clearNoCleanupAction,
        )
    }

    @Composable
    fun ConfirmCleanupDialog() {
        OKCancelDialog(
            title = R.string.confirm_cleanup,
            summary = R.string.confirm_cleanup_msg,
            onCancel = viewModel::clearConfirmCleanup,
            onOK = viewModel::cleanupPuzzlesConfirmed,
        )
    }

    @Composable
    fun ConfirmDeleteSelectedDialog() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val numSelected = state.selectedPuzzleIDs.size
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
    fun DisplayDialogs() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        when (state.showDialog) {
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

        val downloadDialog
            by viewModel.downloadDialog.collectAsStateWithLifecycle()
        downloadDialog?.let {
            DownloadDialog(
                viewModel = it,
                onCancel = viewModel::clearDownloadDialog,
                onDownload = viewModel::okDownloadDialog,
                onOpenSettings = {
                    viewModel.clearDownloadDialog()
                    openSettingsPage()
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
                onClose = onExit,
            )
        }
    }

    fun onKeyEvent(event : KeyEvent) : Boolean {
        val isUp = event.type == KeyEventType.KeyUp
        when (event.key) {
            Key.Escape -> {
                if (isUp) {
                    val isSelecting = viewModel.uiState.value.isSelecting
                    if (isSelecting)
                        viewModel.clearSelection()
                    else
                        onBack()
                }
                return true
            }
        }
        return false
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshCurrentPuzzleMeta()
        viewModel.autoDownloadIfRequired()
        viewModel.processToImportDirectory()

        val uris = getPendingImports()
        if (!uris.isEmpty()) {
            clearPendingImports()
            viewModel.importURIsAndFinish(uris)
        }
    }

    DisposableEffect(Unit) {
        setOnImportCompleteCallback(viewModel::setImportedNowExitResult)
        setOnImportURIsCallback(viewModel::importURIs)
        setOnNotificationPermissionDeniedCallback(
            viewModel::flagNotificationPermissionsDenied
        )

        onDispose {
            setOnImportCompleteCallback(null)
            setOnImportURIsCallback(null)
            setOnNotificationPermissionDeniedCallback(null)
        }
    }

    Launchers()

    var speedDialState by rememberSaveable {
        mutableStateOf(SpeedDialState.Collapsed)
    }
    val toggleSpeedDial = {
        speedDialState = speedDialState.toggle()
    }
    val puzzleListState = rememberLazyListState()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSelecting = state.isSelecting

    Scaffold(
        modifier = Modifier.onKeyEvent(::onKeyEvent),
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

