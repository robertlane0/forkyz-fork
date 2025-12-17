
package app.crossword.yourealwaysbe.forkyz

import kotlinx.serialization.Serializable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute

import org.jetbrains.compose.resources.stringResource

import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.online_sources_url
import app.crossword.yourealwaysbe.forkyz.exttools.AppNotFoundException
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator
import app.crossword.yourealwaysbe.forkyz.util.NativeFrontendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.toUri
import app.crossword.yourealwaysbe.puz.ClueID

/**
 * Navigation routes
 */
@Serializable
internal object Browse
@Serializable
internal object Play
@Serializable
internal object ClueList
@Serializable
internal data class Notes(
    val listName : String?,
    val clueIndex : Int?,
)
@Serializable
internal object Settings
/**
 * Get unqualifiedFileName from HTMLFile
 *
 * Cannot use complex data in routes.
 * See: https://developer.android.com/guide/navigation/use-graph/navigate
 */
@Serializable
internal data class HTML(val unqualifiedFileName : String)
@Serializable
internal object Logcat

/**
 * Page open funs
 *
 * @param openHTMLPage take unqualified name of file to open in
 * composeResources/files. E.g. releases.html will resolve to en/release.html
 * for example.
 */
data class PageOpener(
    val openPlayPage : () -> Unit,
    val openClueListPage : () -> Unit,
    val openNotesPage : (ClueID?) -> Unit,
    val openHTMLPage : (HTMLFile) -> Unit,
    val openOnlineSources : () -> Unit,
    val openReleaseNotes : () -> Unit,
    val openSettingsPage : () -> Unit,
    val openLogcatPage : () -> Unit,
    val onBack : () -> Unit,
)

@Composable
fun ForkyzApp(
    themeHelper : ThemeHelper,
    utils : NativeFrontendUtils,
) {
    themeHelper.AppTheme() {
        ForkyzAppMain(
            themeHelper = themeHelper,
            utils = utils,
        )
    }
}

@Composable
private fun ForkyzAppMain(
    themeHelper : ThemeHelper,
    utils : NativeFrontendUtils,
) {
    val navController = rememberNavController()

    fun doBackAction() {
        if (!navController.popBackStack())
            utils.onFinish()
    }

    fun openHTMLPage(page : HTMLFile) {
        navController.navigate(route = HTML(page.unqualifiedFileName))
    }

    val onlineSourcesURL = stringResource(Res.string.online_sources_url)

    val pageOpener = PageOpener(
        openPlayPage = { navController.navigate(route = Play) },
        openClueListPage = { navController.navigate(route = ClueList) },
        openNotesPage = { cid ->
            navController.navigate(
                route = Notes(cid?.listName, cid?.index),
            )
        },
        openHTMLPage = ::openHTMLPage,
        openOnlineSources = { utils.openURI(onlineSourcesURL.toUri()) },
        openReleaseNotes = { openHTMLPage(HTMLFile.RELEASE_NOTES) },
        openSettingsPage = { navController.navigate(route = Settings) },
        openLogcatPage = { navController.navigate(route = Logcat) },
        onBack = ::doBackAction,
    )

    NavHost(navController, startDestination = Browse) {
        composable<Browse> {
            val viewModel = koinViewModel<BrowsePageViewModel>()
            BrowsePage(
                viewModel = viewModel,
                themeHelper = themeHelper,
                utils = utils,
                pageOpener = pageOpener,
            )
        }
        composable<Play> {
            val viewModel = koinViewModel<PlayPageViewModel>()
            PlayPage(
                viewModel = viewModel,
                themeHelper = themeHelper,
                utils = utils,
                pageOpener = pageOpener,
            )
        }
        composable<ClueList> {
            val viewModel = koinViewModel<ClueListPageViewModel>()
            ClueListPage(
                viewModel = viewModel,
                themeHelper = themeHelper,
                utils = utils,
                pageOpener = pageOpener,
            )
        }
        composable<Notes> { backStackEntry ->
            val notes : Notes = backStackEntry.toRoute()
            val cid = if (notes.listName != null && notes.clueIndex != null)
                ClueID(notes.listName, notes.clueIndex)
            else
                null
            val viewModel = koinViewModel<NotesPageViewModel>(
                parameters = { parametersOf(cid) }
            )
            NotesPage(
                viewModel = viewModel,
                themeHelper = themeHelper,
                utils = utils,
                pageOpener = pageOpener,
            )
        }
        composable<Settings> {
            val viewModel = koinViewModel<SettingsPageViewModel>()
            SettingsPage(
                viewModel = viewModel,
                themeHelper = themeHelper,
                utils = utils,
                pageOpener = pageOpener,
            )
        }
        composable<HTML> { backStackEntry ->
            val viewModel = koinViewModel<HTMLPageViewModel>()
            val html : HTML = backStackEntry.toRoute()
            HTMLPage(
                viewModel = viewModel,
                themeHelper = themeHelper,
                unqualifiedFileName = html.unqualifiedFileName,
                onBack = ::doBackAction,
            )
        }
        composable<Logcat> {
            val viewModel = koinViewModel<LogcatPageViewModel>()
            LogcatPage(
                viewModel = viewModel,
                themeHelper = themeHelper,
                onShareFile = utils::onShareFile,
                onBack = ::doBackAction,
            )
        }
    }

    themeHelper.LegacyStatusBarColor(utils::setStatusBarColor)
}

