
package app.crossword.yourealwaysbe.forkyz.injection

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

import app.crossword.yourealwaysbe.forkyz.BrowsePageViewModel
import app.crossword.yourealwaysbe.forkyz.ClueListPageViewModel
import app.crossword.yourealwaysbe.forkyz.HTMLPageViewModel
import app.crossword.yourealwaysbe.forkyz.LogcatPageViewModel
import app.crossword.yourealwaysbe.forkyz.NotesPageViewModel
import app.crossword.yourealwaysbe.forkyz.PlayPageViewModel
import app.crossword.yourealwaysbe.forkyz.SettingsPageViewModel
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.BackgroundDownloadManager
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.MigrationHelper
import app.crossword.yourealwaysbe.forkyz.util.OrientationHelper

val sharedModule = module {
    singleOf(::BackgroundDownloadManager)
    singleOf(::CurrentPuzzleHolder)
    singleOf(::ForkyzSettings)
    singleOf(::MigrationHelper)
    singleOf(::OrientationHelper)
    singleOf(::ThemeHelper)
    singleOf(::DownloadersProvider)
    viewModelOf(::BrowsePageViewModel)
    viewModelOf(::ClueListPageViewModel)
    viewModel { params ->
        NotesPageViewModel(params[0], get(), get(), get(), get())
    }
    viewModelOf(::PlayPageViewModel)
    viewModelOf(::SettingsPageViewModel)
    viewModelOf(::HTMLPageViewModel)
    viewModelOf(::LogcatPageViewModel)
}

