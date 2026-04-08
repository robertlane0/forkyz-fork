
package app.crossword.yourealwaysbe.forkyz.injection

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

import app.crossword.yourealwaysbe.forkyz.BrowsePageViewModel
import app.crossword.yourealwaysbe.forkyz.ClueListPageViewModel
import app.crossword.yourealwaysbe.forkyz.ForkyzAppViewModel
import app.crossword.yourealwaysbe.forkyz.HTMLPageViewModel
import app.crossword.yourealwaysbe.forkyz.LogcatPageViewModel
import app.crossword.yourealwaysbe.forkyz.NotesPageViewModel
import app.crossword.yourealwaysbe.forkyz.PlayPageViewModel
import app.crossword.yourealwaysbe.forkyz.SettingsPageViewModel
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.AppStrings
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder

val sharedModules = module {
    singleOf(::AppStrings)
    singleOf(::CurrentPuzzleHolder)
    singleOf(::ForkyzSettings)
    singleOf(::ThemeHelper)
    singleOf(::DownloadersProvider)
    viewModelOf(::BrowsePageViewModel)
    viewModelOf(::ClueListPageViewModel)
    viewModelOf(::ForkyzAppViewModel)
    viewModel { params ->
        NotesPageViewModel(params[0], get(), get(), get(), get(), get())
    }
    viewModelOf(::PlayPageViewModel)
    viewModelOf(::SettingsPageViewModel)
    viewModelOf(::HTMLPageViewModel)
    viewModelOf(::LogcatPageViewModel)
}

