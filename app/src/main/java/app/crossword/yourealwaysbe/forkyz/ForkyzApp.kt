
package app.crossword.yourealwaysbe.forkyz

import kotlinx.serialization.Serializable

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute

import app.crossword.yourealwaysbe.forkyz.exttools.AppNotFoundException
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator
import app.crossword.yourealwaysbe.forkyz.util.NativeFrontendUtils
import app.crossword.yourealwaysbe.puz.ClueID

/**
 * Navigation routes
 */
@Serializable
private object Browse
@Serializable
private object Play
@Serializable
private object ClueList
@Serializable
private data class Notes(
    val listName : String?,
    val clueIndex : Int?,
)
@Serializable
private object Settings
@Serializable
private data class HTML(val rawAssetID : Int)
@Serializable
private object Logcat

/**
 * Page open funs
 *
 * @param openHTMLPage take ID of raw HTML asset
 */
data class PageOpener(
    val openPlayPage : () -> Unit,
    val openClueListPage : () -> Unit,
    val openNotesPage : (ClueID?) -> Unit,
    val openHTMLPage : (Int) -> Unit,
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
        val navController = rememberNavController()

        fun doBackAction() {
            if (!navController.popBackStack())
                utils.onFinish()
        }

        fun openHTMLPage(rawAssetID : Int) {
            navController.navigate(route = HTML(rawAssetID))
        }

        val onlineSourcesURL = stringResource(R.string.online_sources_url)

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
            openReleaseNotes = { openHTMLPage(R.raw.release) },
            openSettingsPage = { navController.navigate(route = Settings) },
            openLogcatPage = { navController.navigate(route = Logcat) },
            onBack = ::doBackAction,
        )

        NavHost(navController, startDestination = Browse) {
            composable<Browse> {
                val viewModel = hiltViewModel<BrowsePageViewModel>()
                BrowsePage(
                    viewModel = viewModel,
                    themeHelper = themeHelper,
                    utils = utils,
                    pageOpener = pageOpener,
                )
            }
            composable<Play> {
                val viewModel = hiltViewModel<PlayPageViewModel>()
                PlayPage(
                    viewModel = viewModel,
                    themeHelper = themeHelper,
                    utils = utils,
                    pageOpener = pageOpener,
                )
            }
            composable<ClueList> {
                val viewModel = hiltViewModel<ClueListPageViewModel>()
                ClueListPage(
                    viewModel = viewModel,
                    themeHelper = themeHelper,
                    utils = utils,
                    pageOpener = pageOpener,
                )
            }
            composable<Notes> { backStackEntry ->
                val notes : Notes = backStackEntry.toRoute()
                val viewModel = hiltViewModel<
                    NotesPageViewModel,
                    NotesPageViewModel.NotesPageViewModelFactory,
                > { factory ->
                    factory.create(
                        if (notes.listName != null && notes.clueIndex != null)
                            ClueID(notes.listName, notes.clueIndex)
                        else
                            null
                    )
                }
                NotesPage(
                    viewModel = viewModel,
                    themeHelper = themeHelper,
                    utils = utils,
                    pageOpener = pageOpener,
                )
            }
            composable<Settings> {
                val viewModel = hiltViewModel<SettingsPageViewModel>()
                SettingsPage(
                    viewModel = viewModel,
                    themeHelper = themeHelper,
                    utils = utils,
                    pageOpener = pageOpener,
                )
            }
            composable<HTML> { backStackEntry ->
                val viewModel = hiltViewModel<HTMLPageViewModel>()
                val html : HTML = backStackEntry.toRoute()
                HTMLPage(
                    viewModel = viewModel,
                    themeHelper = themeHelper,
                    rawAssetID = html.rawAssetID,
                    onBack = ::doBackAction,
                )
            }
            composable<Logcat> {
                val viewModel = hiltViewModel<LogcatPageViewModel>()
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
}

