
package app.crossword.yourealwaysbe.forkyz

import kotlinx.datetime.LocalDate

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.FloatingActionButtonMenuScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

import be.digitalia.compose.htmlconverter.htmlToAnnotatedString

import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.releaseBookmark

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.add_puzzle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.android_app_settings_button
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.blank_copy
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browse_date_format_non_header
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.by_date_asc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.by_date_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.by_source_date_asc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.by_source_date_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cleanup
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.close
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.collapse
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.confirm_cleanup
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.confirm_cleanup_msg
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.confirm_delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.confirm_delete_msg
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.day_mode
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.delete_selected
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.disable_notifications
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.disable_notifications_button
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_puzzles
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.expand
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.filter_puzzles
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.filter_puzzles_hint
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.has_solution_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_blank_copy
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_download
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_import
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_menu_archive
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_menu_unarchive
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_online_sources
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_solution
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.import_failure_long
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.import_puzzle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.import_success_long
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.imports_title
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.internal_storage_description
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.new_version_message
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.new_version_title
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.night_mode
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.no_cleanup_action
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.no_cleanup_action_msg
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.no_puzzles
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.no_puzzles_download_or_configure_storage
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.notifications_denied_msg
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.num_selected
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.online_sources
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.overflow
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.please_wait
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.sort
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.system_daynight_mode
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.title_view_archives
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.title_view_crosswords
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_archive
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_archive_selected
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.view_release_notes
import app.crossword.yourealwaysbe.forkyz.menu.BaseMenuModifier
import app.crossword.yourealwaysbe.forkyz.menu.MenuDropdownHeading
import app.crossword.yourealwaysbe.forkyz.menu.MenuExit
import app.crossword.yourealwaysbe.forkyz.menu.MenuHelp
import app.crossword.yourealwaysbe.forkyz.menu.MenuSettings
import app.crossword.yourealwaysbe.forkyz.menu.MenuSubEntry
import app.crossword.yourealwaysbe.forkyz.menu.MenuText
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider
import app.crossword.yourealwaysbe.forkyz.settings.AccessorSetting
import app.crossword.yourealwaysbe.forkyz.settings.BrowseSwipeAction
import app.crossword.yourealwaysbe.forkyz.settings.DayNightMode
import app.crossword.yourealwaysbe.forkyz.theme.ForkyzScaffold
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.MATERIAL_MIN_CLICKABLE_DP
import app.crossword.yourealwaysbe.forkyz.util.NativeFrontendUtils
import app.crossword.yourealwaysbe.forkyz.util.NativeImportFinishedResult
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.onKeyEventKeyboardBindings
import app.crossword.yourealwaysbe.forkyz.util.onMiddleClick
import app.crossword.yourealwaysbe.forkyz.util.toUri
import app.crossword.yourealwaysbe.forkyz.view.CircleProgressBar
import app.crossword.yourealwaysbe.forkyz.view.DownloadDialog
import app.crossword.yourealwaysbe.forkyz.view.OKCancelDialog
import app.crossword.yourealwaysbe.forkyz.view.OKDialog
import app.crossword.yourealwaysbe.forkyz.view.RatingBar
import app.crossword.yourealwaysbe.forkyz.view.RatingBarStyle
import app.crossword.yourealwaysbe.forkyz.view.SearchBar
import app.crossword.yourealwaysbe.forkyz.view.SwipeToDismissBox
import app.crossword.yourealwaysbe.forkyz.view.SwipeToDismissBoxValue
import app.crossword.yourealwaysbe.forkyz.view.TwoButtonDialog
import app.crossword.yourealwaysbe.forkyz.view.rememberSwipeToDismissBoxState

private val HORIZONTAL_PADDING = 16.dp
private val FAB_OPEN_ANGLE = 45F
private val FAB_OVERLAY_ALPHA = 0.6F
private val MIN_PUZZLE_ITEM_HEIGHT = 72.dp

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalAnimationApi::class,
)
@Composable
fun BrowsePage(
    viewModel : BrowsePageViewModel,
    themeHelper : ThemeHelper,
    utils : NativeFrontendUtils,
    pageOpener : PageOpener,
    isTop : Boolean,
) {
    fun startLoadPuzzleList(archive : Boolean) {
        viewModel.startLoadPuzzleList(archive)
    }

    fun startLoadPuzzleList() {
        startLoadPuzzleList(viewModel.isViewArchive);
    }

    fun showDownloadDialog() {
        utils.onCheckAndWarnNetworkState()
        if (viewModel.uiState.value.isNotificationPermissionNeeded)
            utils.onCheckRequestNotificationPermissions()

        viewModel.showDownloadDialog()
    }

    @Composable
    fun Launchers() {
        val loadEvent by viewModel.puzzleLoaded.collectAsStateWithLifecycle()

        LaunchedEffect(loadEvent) {
            if (loadEvent) {
                viewModel.clearPuzzleLoaded()
                pageOpener.openPlayPage()
            }
        }

        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val isDownloadOnStartUp by remember {
            derivedStateOf { state.isDownloadOnStartUp }
        }
        val needsNotificationCheck by remember {
            derivedStateOf {
                (
                    state.isBackgroundDownloadEnabled
                    || state.isDownloadOnStartUp
                ) &&  state.isNotificationPermissionNeeded
            }
        }
        LaunchedEffect(needsNotificationCheck) {
            if (needsNotificationCheck)
                utils.onCheckRequestNotificationPermissions()
        }

        val toasts by viewModel.toastMessages.collectAsStateWithLifecycle()
        LaunchedEffect(toasts) {
            toasts.forEach(utils::toast)
            viewModel.clearToastMessages()
        }

        val importFinishResult
            by viewModel.importFinishResult.collectAsStateWithLifecycle()
        LaunchedEffect(importFinishResult) {
            importFinishResult?.let {
                utils.onImportFinish(
                    NativeImportFinishedResult(it.someFailed, it.someSucceeded)
                )
            }
        }
    }

    @Composable
    fun DeleteSelectedButton() {
        IconButton(onClick = viewModel::deleteSelectedPuzzles) {
            Icon(
                painter = painterResource(Res.drawable.ic_delete),
                contentDescription = stringResource(Res.string.delete_selected),
            )
        }
    }

    fun toggleArchiveIcon(isArchive : Boolean) : DrawableResource {
        if (isArchive)
            return Res.drawable.ic_menu_unarchive
        else
            return Res.drawable.ic_menu_archive
    }

    @Composable
    fun ToggleArchiveSelectedButton() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val isArchive by remember {
            derivedStateOf { state.isViewArchive }
        }
        IconButton(onClick = viewModel::toggleArchiveSelectedPuzzles) {
            Icon(
                painter = painterResource(toggleArchiveIcon(isArchive)),
                contentDescription
                    = stringResource(Res.string.toggle_archive_selected),
            )
        }
    }

    @Composable
    fun BlankCopySelectedButton() {
        IconButton(onClick = viewModel::blankCopySelectedPuzzles) {
            Icon(
                painter = painterResource(Res.drawable.ic_blank_copy),
                contentDescription = stringResource(Res.string.blank_copy),
            )
        }
    }

    @Composable
    fun AddPuzzleIcon(modifier : Modifier = Modifier, tint : Color) {
        Icon(
            modifier = modifier,
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(Res.string.add_puzzle),
            tint = tint,
        )
    }

    @Composable
    fun FloatingActionButtonMenuScope.OnlineSourcesButton(close : () -> Unit) {
        FloatingActionButtonMenuItem(
            onClick = {
                close()
                pageOpener.openOnlineSources()
            },
            text = { Text(stringResource(Res.string.online_sources)) },
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_online_sources),
                    contentDescription
                        = stringResource(Res.string.online_sources),
                )
            },
        )
    }

    @Composable
    fun FloatingActionButtonMenuScope.ImportPuzzlesButton(close : () -> Unit) {
        val launcher = rememberFilePickerLauncher(
            mode = FileKitMode.Multiple(maxItems = 10),
        ) {
            it?.let { files ->
                val uris = files.map { it.absolutePath().toUri() }
                viewModel.importURIs(uris) {
                    files.forEach { it.releaseBookmark() }
                }
            }
        }
        FloatingActionButtonMenuItem(
            onClick = {
                close()
                launcher.launch()
            },
            text = { Text(stringResource(Res.string.import_puzzle)) },
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_import),
                    contentDescription
                        = stringResource(Res.string.import_puzzle),
                )
            },
        )
    }

    @Composable
    fun FloatingActionButtonMenuScope.DownloadPuzzlesButton(
        close : () -> Unit,
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                close()
                showDownloadDialog()
            },
            text = { Text(stringResource(Res.string.download_puzzles)) },
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_download),
                    contentDescription
                        = stringResource(Res.string.download_puzzles),
                )
            },
        )
    }

    @Composable
    fun FABOverlay(
        modifier : Modifier = Modifier,
        visible : Boolean,
        onClick : () -> Unit,
        onClickLabel : String,
    ) {
        if (visible) {
            Box(
                modifier = modifier.fillMaxWidth()
                    .fillMaxHeight()
                    .alpha(FAB_OVERLAY_ALPHA)
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(
                        onClick = onClick,
                        onClickLabel = onClickLabel
                    ),
            )
        }
    }

    @Composable
    fun FilterPuzzlesButton() {
        IconButton(onClick = viewModel::startPuzzleFilter) {
            Icon(
                Icons.Filled.Search,
                stringResource(Res.string.filter_puzzles),
            )
        }
    }

    @Composable
    fun ThemeButton() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        val dnmIcon by remember {
            derivedStateOf {
                when (state.dayNightMode) {
                    DayNightMode.DNM_NIGHT -> Res.drawable.night_mode
                    DayNightMode.DNM_SYSTEM -> Res.drawable.system_daynight_mode
                    else -> Res.drawable.day_mode
                }
            }
        }

        IconButton(onClick = viewModel::nextDayNightMode) {
            Icon(
                painter = painterResource(dnmIcon),
                stringResource(Res.string.filter_puzzles),
            )
        }
    }

    @Composable
    fun MenuSort() {
        MenuSubEntry(
            text = { MenuText(stringResource(Res.string.sort)) },
            onClick = {
                viewModel.expandMenu(BrowseSubMenu.SORT_ORDER)
            }
        )
    }

    @Composable
    fun MenuCleanup() {
        DropdownMenuItem(
            text = { MenuText(stringResource(Res.string.cleanup)) },
            onClick = {
                viewModel.cleanupPuzzles()
                viewModel.dismissMenu()
            }
        )
    }

    @Composable
    fun MenuSwitchLists() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val isArchive by remember { derivedStateOf { state.isViewArchive } }
        val menuText by remember {
            derivedStateOf {
                if (isArchive)
                    Res.string.title_view_crosswords
                else
                    Res.string.title_view_archives
            }
        }

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
        val expanded by remember {
            derivedStateOf {
                state.expanded == BrowseSubMenu.SORT_ORDER
            }
        }
        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = expanded,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                Res.string.sort,
                onClick = viewModel::closeMenu,
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(Res.string.by_date_desc)) },
                onClick = {
                    viewModel.setSortOrder(AccessorSetting.AS_DATE_DESC)
                    viewModel.dismissMenu()
                }
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(Res.string.by_date_asc)) },
                onClick = {
                    viewModel.setSortOrder(AccessorSetting.AS_DATE_ASC)
                    viewModel.dismissMenu()
                }
            )
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(Res.string.by_source_date_desc))
                },
                onClick = {
                    viewModel.setSortOrder(AccessorSetting.AS_SOURCE_DATE_DESC)
                    viewModel.dismissMenu()
                }
            )
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(Res.string.by_source_date_asc))
                },
                onClick = {
                    viewModel.setSortOrder(AccessorSetting.AS_SOURCE_DATE_ASC)
                    viewModel.dismissMenu()
                }
            )
        }
    }

    @Composable
    fun OverflowMenu() {
        IconButton(onClick = { viewModel.expandMenu(BrowseSubMenu.MAIN) }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(Res.string.overflow),
            )
        }

        val state by viewModel.menuState.collectAsStateWithLifecycle()
        val expanded by remember {
            derivedStateOf { state.expanded == BrowseSubMenu.MAIN }
        }
        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = expanded,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuSort()
            MenuCleanup()
            MenuSwitchLists()
            MenuHelp(
                openHelp = { pageOpener.openHTMLPage(HTMLFile.BROWSE_HELP) },
                onDismiss = viewModel::dismissMenu,
            )
            MenuSettings(
                openSettings = { pageOpener.openSettingsPage() },
                onDismiss = viewModel::dismissMenu,
            )
            MenuExit(utils)
        }
        MenuSortSub()
    }

    @Composable
    fun TopAppBar() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val isSelecting by remember { derivedStateOf { state.isSelecting } }
        if (isSelecting) {
            themeHelper.ForkyzTopAppBar(
                title = {
                    val numSelected by remember {
                        derivedStateOf { state.selectedPuzzleIDs.size }
                    }
                    Text(
                        pluralStringResource(
                            resource = Res.plurals.num_selected,
                            quantity = numSelected,
                            numSelected,
                        ),
                    )
                },
                actions = {
                    BlankCopySelectedButton()
                    DeleteSelectedButton()
                    ToggleArchiveSelectedButton()
                },
                onBack = viewModel::clearSelection,
            )
        } else {
            val titleId by remember {
                derivedStateOf {
                    if (state.isViewArchive)
                        Res.string.title_view_archives
                    else
                        Res.string.title_view_crosswords
                }
            }
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
        val puzzleFilter by remember {
            derivedStateOf { state.puzzleFilter }
        }
        puzzleFilter?.let { puzzleFilter ->
            SearchBar(
                showSearchBar = true,
                searchTerm = puzzleFilter,
                onValueChange = viewModel::setPuzzleFilter,
                onClose = viewModel::clearPuzzleFilter,
                hint = stringResource(Res.string.filter_puzzles_hint),
            )
        }
    }

    @Composable
    fun IntroMessages() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val displayNoPuzzlesMessage by remember {
            derivedStateOf { state.displayNoPuzzlesMessage }
        }
        if (displayNoPuzzlesMessage) {
            val noPuzzlesMsg by remember {
                derivedStateOf {
                    if (state.isViewArchive)
                        Res.string.no_puzzles
                    else
                        Res.string.no_puzzles_download_or_configure_storage
                }
            }
            Text(
                modifier = Modifier.padding(30.dp),
                text = stringResource(noPuzzlesMsg),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (state.displayInternalStorageMessage) {
            Text(
                modifier = Modifier.padding(30.dp),
                text = stringResource(Res.string.internal_storage_description),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }

    @Composable
    fun PuzzleListHeader(
        group : PuzGroup,
        isCollapsible : Boolean = false,
        isCollapsed : Boolean = false,
        onClick : () -> Unit = {},
    ) {
        val header = group.header
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .then(
                    if (isCollapsible)
                        Modifier.clickable(onClick = onClick)
                            // min for clickable elements in Material
                            .heightIn(min=MATERIAL_MIN_CLICKABLE_DP)
                            .padding(horizontal=HORIZONTAL_PADDING)
                            .padding(vertical=8.dp)
                    else
                        Modifier.padding(horizontal=HORIZONTAL_PADDING)
                            .padding(top=4.dp, bottom=4.dp)
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1.0F),
                text = remember (header) { htmlToAnnotatedString(header) },
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (isCollapsible) {
                Icon(
                    modifier = Modifier.rotate(if (isCollapsed) -90f else 0f),
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isCollapsed)
                        stringResource(Res.string.expand)
                    else
                        stringResource(Res.string.collapse),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }

    @Composable
    fun SwipeActionIcon(
        drawableId : DrawableResource,
        contentDescriptionId : StringResource,
    ) {
        Icon(
            modifier = Modifier.size(MIN_PUZZLE_ITEM_HEIGHT * 0.65F)
                .padding(10.dp),
            painter = painterResource(drawableId),
            contentDescription = stringResource(contentDescriptionId),
        )
    }

    @Composable
    fun formatDateNonHeader(date : LocalDate) : String {
        val dayID = TimeUtils.getDayNameID(date)
        val monthID = TimeUtils.getMonthNameID(date)
        return stringResource(
            Res.string.browse_date_format_non_header,
            stringResource(dayID),
            date.day,
            stringResource(monthID),
            date.year,
        )
    }

    @Composable
    fun PuzzleDetails(pe : PuzEntry) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val percentFilled by remember {
            derivedStateOf {
                if (state.showPercentageCorrect)
                    pe.percentComplete
                else
                    pe.percentFilled
            }
        }
        val percentHinted by remember {
            derivedStateOf {
                if (state.suppressHintHighlighting) 0 else pe.percentHinted
            }
        }
        val isSelected by remember {
            derivedStateOf { pe.id in state.selectedPuzzleIDs }
        }
        val backgroundColor = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant
        val textColor = if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant
        val isSelecting by remember {
            derivedStateOf { state.isSelecting }
        }

        Row(
            modifier = Modifier.heightIn(min=MIN_PUZZLE_ITEM_HEIGHT)
                .background(backgroundColor)
                .then(
                    if (isSelected)
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    else
                        Modifier
                )
                .onMiddleClick { viewModel.toggleSelectPuzzle(pe.id) }
                .combinedClickable(
                    onClick = {
                        if (isSelecting)
                            viewModel.toggleSelectPuzzle(pe.id)
                        else
                            viewModel.loadPuzzle(pe.id)
                    },
                    onLongClick = { viewModel.toggleSelectPuzzle(pe.id) },
                ).padding(vertical=8.dp, horizontal=HORIZONTAL_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val sortOrder by remember {
                derivedStateOf { state.sortOrder }
            }
            if (
                sortOrder == AccessorSetting.AS_SOURCE_DATE_ASC
                || sortOrder == AccessorSetting.AS_SOURCE_DATE_DESC
            ) {
                // Wide enough for a Wednesday in December to fit
                val dateWidth = with(LocalDensity.current) {
                    85.sp.toDp()
                }
                val date = formatDateNonHeader(pe.date)
                Text(
                    modifier = Modifier.width(dateWidth)
                        .padding(end=4.dp),
                    minLines = 3,
                    maxLines = 3,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    text = date,
                    color = textColor,
                )
            }
            val progressColors = themeHelper.getCircleProgressBarColorScheme()
            CircleProgressBar(
                modifier = Modifier.size(56.dp),
                colors = progressColors,
                complete = pe.percentComplete == 100,
                percentFilled = percentFilled,
                percentHinted = percentHinted,
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
                        text = remember (pe.title) {
                            htmlToAnnotatedString(pe.title)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = textColor,
                    )

                    val showHasSolution = state.indicateIfSolution
                        && pe.hasSolution

                    if (showHasSolution) {
                        Icon(
                            modifier = Modifier.width(10.dp),
                            painter = painterResource(Res.drawable.ic_solution),
                            contentDescription
                                = stringResource(Res.string.has_solution_desc),
                            tint = textColor,
                        )
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.weight(1.0F)
                            .padding(horizontal=10.dp),
                        maxLines = 3,
                        text = remember(pe.caption) {
                            htmlToAnnotatedString(pe.caption)
                        },
                        color = textColor,
                    )
                    if (state.showRatings) {
                        RatingBar(
                            modifier = Modifier.padding(top=4.dp),
                            stepSize = 1.0F,
                            isIndicator = true,
                            style = RatingBarStyle.SMALL,
                            rating = pe.rating.code.toFloat(),
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun Puzzle(pe : PuzEntry) {
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

        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val disableSwipe by remember { derivedStateOf { state.disableSwipe } }
        SwipeToDismissBox(
            state = swipeState,
            enabled = !disableSwipe,
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
                val isArchive by remember {
                    derivedStateOf { state.isViewArchive }
                }

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
                                contentDescriptionId
                                    = Res.string.toggle_archive,
                            )
                        } else {
                            SwipeActionIcon(
                                drawableId = Res.drawable.ic_delete,
                                contentDescriptionId = Res.string.delete,
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
        val puzzles by remember {
            derivedStateOf { state.puzzleList }
        }
        val refreshState = rememberPullToRefreshState()

        // move up if items above appear, but not on return to page
        // thanks: https://stackoverflow.com/a/74326906
        var lastScrollToTopID by rememberSaveable {
            mutableStateOf(puzzles.firstOrNull()?.puzzles?.firstOrNull()?.id)
        }
        val firstID by remember {
            derivedStateOf {
                puzzles.firstOrNull()?.puzzles?.firstOrNull()?.id
            }
        }
        LaunchedEffect(firstID) {
            if (!puzzles.isEmpty() && firstID != lastScrollToTopID) {
                lastScrollToTopID = firstID
                listState.animateScrollToItem(0)
            }
        }

        val isRefreshing by remember {
            derivedStateOf { state.isSwipeRefreshing }
        }
        PullToRefreshBox(
            modifier = modifier.fillMaxWidth(),
            isRefreshing = isRefreshing,
            onRefresh = viewModel::swipeRefreshPuzzleList,
            state = refreshState,
        ) {
            val isCollapseHeadings by remember {
                derivedStateOf { state.isCollapseHeadings }
            }
            val expandedHeadingIDs by remember {
                derivedStateOf { state.expandedHeadingIDs }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                state = listState,
            ) {
                puzzles.forEach { puzGroup ->
                    val isCollapsed = (
                        isCollapseHeadings
                        && !expandedHeadingIDs.contains(puzGroup.id)
                    )
                    stickyHeader {
                        key(puzGroup.header) {
                            PuzzleListHeader(
                                group = puzGroup,
                                isCollapsible = isCollapseHeadings,
                                isCollapsed = isCollapsed,
                                onClick = {
                                    viewModel.toggleCollapseHeading(puzGroup)
                                },
                            )
                        }
                    }
                    if (!isCollapsed) {
                        items(
                            items = puzGroup.puzzles,
                            key = { pe -> pe },
                        ) { pe ->
                            Puzzle(pe)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun UIBusy() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val isBusy by remember { derivedStateOf { state.isBusy } }
        if (isBusy) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical=10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
                Text(
                    modifier = Modifier.padding(start=10.dp),
                    text = stringResource(Res.string.please_wait),
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
        LaunchedEffect(isTop) {
            if (isTop)
                requester.requestFocus()
        }
        Box(modifier) {
            Column(Modifier.fillMaxWidth().fillMaxHeight()) {
                PuzzleSearchBar()
                IntroMessages()
                PuzzleList(
                    modifier = Modifier.weight(1.0F)
                        .focusRequester(requester)
                        .focusable(),
                    listState = puzzleListState,
                )
                UIBusy()
            }
        }
    }

    @Composable
    fun NotificationPermissionsDialog() {
        TwoButtonDialog(
            title = Res.string.disable_notifications,
            summary = Res.string.notifications_denied_msg,
            positiveText = Res.string.disable_notifications_button,
            onPositive = viewModel::disableNotifications,
            negativeText = Res.string.android_app_settings_button,
            onNegative = {
                utils.openAppSettings()
                viewModel.dismissNotificationsDialog()
            },
        )
    }

    @Composable
    fun NewVersionDialog() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val currentVersion by remember {
            derivedStateOf { state.currentVersion }
        }
        TwoButtonDialog(
            title = stringResource(Res.string.new_version_title, currentVersion),
            summary = stringResource(Res.string.new_version_message),
            positiveText = stringResource(Res.string.view_release_notes),
            onPositive = {
                pageOpener.openReleaseNotes()
                viewModel.flagSeenCurrentVersion()
            },
            negativeText = stringResource(Res.string.close),
            onNegative = viewModel::flagSeenCurrentVersion,
        )
    }

    @Composable
    fun NoCleanupActionDialog() {
        OKDialog(
            title = Res.string.no_cleanup_action,
            summary = stringResource(Res.string.no_cleanup_action_msg),
            onClose = viewModel::clearNoCleanupAction,
        )
    }

    @Composable
    fun ConfirmCleanupDialog() {
        OKCancelDialog(
            title = Res.string.confirm_cleanup,
            summary = Res.string.confirm_cleanup_msg,
            onCancel = viewModel::clearConfirmCleanup,
            onOK = viewModel::cleanupPuzzlesConfirmed,
        )
    }

    @Composable
    fun ConfirmDeleteSelectedDialog() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val numSelected by remember {
            derivedStateOf { state.selectedPuzzleIDs.size }
        }
        OKCancelDialog(
            title = stringResource(Res.string.confirm_delete),
            summary = pluralStringResource(
                resource = Res.plurals.confirm_delete_msg,
                quantity = numSelected,
            ),
            onCancel = viewModel::cancelDeleteSelectedPuzzles,
            onOK = viewModel::confirmDeleteSelectedPuzzles,
        )
    }

    @Composable
    fun DisplayDialogs() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val showDialog by remember {
            derivedStateOf { state.showDialog }
        }
        when (showDialog) {
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
                    pageOpener.openSettingsPage()
                },
            )
        }

        val exitDialogSuccess by viewModel
            .importedNowExitResult
            .collectAsStateWithLifecycle()
        exitDialogSuccess?.let { success ->
            OKDialog(
                title = stringResource(Res.string.imports_title),
                summary = stringResource(
                    if (success)
                        Res.string.import_success_long
                    else
                        Res.string.import_failure_long
                ),
                onClose = utils::onExit,
            )
        }
    }

    fun handleBack() {
        val isSelecting = viewModel.uiState.value.isSelecting
        if (isSelecting)
            viewModel.clearSelection()
        else
            pageOpener.onBack()
    }

    fun onKeyEvent(event : KeyEvent) : Boolean {
        val handled = onKeyEventKeyboardBindings(
            event = event,
            binds = viewModel.keyboardBindings,
            onBackDown = ::handleBack,
            onExitDown = utils::onExit,
        )

        val isDown = event.type == KeyEventType.KeyDown
        when (event.key) {
            Key.Escape -> {
                if (isDown)
                    handleBack()
                return true
            }
        }
        return false
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshCurrentPuzzleMeta()
        viewModel.autoDownloadIfRequired()
        viewModel.processToImportDirectory()

        val uris = utils.getPendingImports()
        if (!uris.isEmpty()) {
            utils.clearPendingImports()
            if (utils.isExitAfterPendingImports) {
                viewModel.importURIsAndFinish(uris)
            } else
                viewModel.importURIs(uris)
        }
    }

    DisposableEffect(Unit) {
        utils.setOnImportCompleteCallback(viewModel::setImportedNowExitResult)
        utils.setOnNotificationPermissionDeniedCallback(
            viewModel::flagNotificationPermissionsDenied
        )

        onDispose {
            utils.removeOnImportCompleteCallback(
                viewModel::setImportedNowExitResult,
            )
            utils.removeOnNotificationPermissionDeniedCallback(
                viewModel::flagNotificationPermissionsDenied
            )
        }
    }

    Launchers()

    val puzzleListState = rememberLazyListState()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSelecting by remember { derivedStateOf { state.isSelecting } }
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val fabVisible by remember {
        derivedStateOf {
            !isSelecting
            && !state.isViewArchive
            && (
                puzzleListState.lastScrolledBackward
                || !puzzleListState.canScrollBackward
            )
        }
    }
    val toggleFAB = { fabMenuExpanded = !fabMenuExpanded }
    val primaryColor = MaterialTheme.colorScheme.primary

    ForkyzScaffold(
        modifier = Modifier.onKeyEvent(::onKeyEvent),
        topBar = { TopAppBar() },
        floatingActionButton = {
            FloatingActionButtonMenu(
                expanded = fabMenuExpanded,
                button = {
                    ToggleFloatingActionButton(
                        modifier = Modifier.animateFloatingActionButton(
                            visible = fabVisible || fabMenuExpanded,
                            alignment = Alignment.BottomEnd,
                        ),
                        checked = fabMenuExpanded,
                        onCheckedChange = { checked ->
                            fabMenuExpanded = checked
                        },
                        containerColor = { primaryColor },
                    ) {
                        val modifier = if (fabMenuExpanded)
                            Modifier.rotate(FAB_OPEN_ANGLE)
                        else
                            Modifier
                        AddPuzzleIcon(
                            modifier = modifier,
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
            ) {
                DownloadPuzzlesButton(toggleFAB)
                ImportPuzzlesButton(toggleFAB)
                OnlineSourcesButton(toggleFAB)
            }
        },
    ) { innerPadding ->
        ActivityBody(
            modifier = Modifier.padding(innerPadding),
            puzzleListState = puzzleListState,
        )
        FABOverlay(
            visible = fabMenuExpanded,
            onClick = toggleFAB,
            onClickLabel = stringResource(Res.string.close),
        )
        DisplayDialogs()
    }
}

