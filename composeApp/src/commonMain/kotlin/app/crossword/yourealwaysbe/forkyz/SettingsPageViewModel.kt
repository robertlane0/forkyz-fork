
package app.crossword.yourealwaysbe.forkyz

import kotlin.collections.ArrayDeque
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO // needed on IOS, not JVM or Android
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.io.IOException

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import org.jetbrains.compose.resources.StringResource

import app.crossword.yourealwaysbe.forkyz.BuildConfig
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.aard2
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.about_forkyz
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.about_libraries
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.about_scrapes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.about_voice_commands
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.action_buttons_mode
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.action_buttons_mode_bottom
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.action_buttons_mode_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.action_buttons_mode_top_always
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.action_buttons_mode_top_wide
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.action_button_toggle_scratch_mode
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.action_button_toggle_scratch_mode_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.action_button_toggle_show_errors_grid
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.action_button_toggle_show_errors_grid_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.action_buttons
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.action_buttons_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_box_always
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_box_always_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_clue_always
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_clue_always_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_clue_button
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_clue_button_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_clue_equals
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_clue_equals_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.archive
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.auto_downloaders
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.auto_downloaders_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.automatic_download
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.automatic_download_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_0
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_1
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_10
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_11
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_12
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_13
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_14
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_15
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_16
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_17
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_18
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_19
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_2
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_20
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_21
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_22
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_23
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_3
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_4
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_5
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_6
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_7
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_8
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_9
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_constraints_config
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_days
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_days_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_days_time
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_days_time_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_fri
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_hourly
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_hourly_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_mon
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_opts
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_opts_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_sat
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_sun
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_thu
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_time_config
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_tue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_download_wed
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.block_auto_downloads
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.block_auto_fri
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.block_auto_fri_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.block_auto_mon
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.block_auto_mon_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.block_auto_sat
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.block_auto_sat_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.block_auto_sun
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.block_auto_sun_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.block_auto_thu
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.block_auto_thu_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.block_auto_tue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.block_auto_tue_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.block_auto_wed
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.block_auto_wed_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.blue_theme
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browse_always_show_rating
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browse_always_show_rating_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browse_indicate_if_solution
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browse_indicate_if_solution_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browse_show_percentage_correct
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browse_show_percentage_correct_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browser_settings
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browser_settings_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cleanup_age_never
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cleanup_age_one_week
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cleanup_age_thirty_days
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cleanup_age_two_days
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cleanup_archives
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cleanup_archives_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cleanup_unfinished
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cleanup_unfinished_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_below_grid
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_below_grid_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_highlight
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_highlight_background
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_highlight_both
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_highlight_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_highlight_none
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_highlight_radio_button
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_list_clue_line_abbreviated
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_list_clue_line_full
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_list_clue_line_none
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_list_name_in_clue_line
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_list_name_in_clue_line_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_tabs_double
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_tabs_double_always
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_tabs_double_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_tabs_double_landscape
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_tabs_double_never
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_tabs_double_wide
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.collapse_headings
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.collapse_headings_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.crossword_solver_enabled
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.crossword_solver_enabled_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cru_puzzle_workshop
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cru_puzzle_workshop_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.custom
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.custom_daily
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.custom_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.custom_title
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.custom_title_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.custom_title_hint
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.custom_url
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.custom_url_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.custom_url_hint
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.custom_url_missing
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cycle_unfilled
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cycle_unfilled_always
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cycle_unfilled_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cycle_unfilled_forwards
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cycle_unfilled_never
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.daily_crosswords
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.daily_crosswords_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.de_standaard
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.de_standaard_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.de_telegraaf
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.de_telegraaf_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.delete_crossing_delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.delete_crossing_preserve_filled_cells
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.delete_crossing_preserve_filled_words
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.delete_on_cleanup
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.delete_on_cleanup_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.desktop_settings
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.desktop_settings_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.development_site
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.development_site_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.dictionary_none
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.disable_ratings
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.disable_ratings_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.disable_swipe
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.disable_swipe_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.display_scratch
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.display_scratch_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.display_separators
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.display_separators_always
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.display_separators_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.display_separators_never
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.display_separators_selected
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.display_settings
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.display_settings_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.dont_confirm_browse_delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.dont_confirm_browse_delete_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.dont_confirm_cleanup
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.dont_confirm_cleanup_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.dont_delete_crossing
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.dont_delete_crossing_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.dont_snap_board_to_borders
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.dont_snap_board_to_borders_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.double_tap_zoom
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.double_tap_zoom_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_charging
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_charging_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_remember_date
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_remember_date_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_roaming
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_roaming_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_settings
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_settings_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_timeout
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_timeout_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_wifi_only
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_wifi_only_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.duckduckgo
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.duckduckgo_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.dynamic_theme
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.elpais_experto
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.elpais_experto_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.enable_auto_downloaders
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.enter_changes_direction
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.enter_changes_direction_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.expandable_clue_line
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.expandable_clue_line_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.external_dictionary
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.external_dictionary_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.external_storage_saf
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.external_storage_saf_current_uri
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.external_storage_saf_none_selected
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.external_tools_settings
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.external_tools_settings_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.fifteen_seconds
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.fifteen_squared
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.fifteen_squared_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.filter_settings_title
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.fit_to_screen_mode
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.fit_to_screen_mode_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.fit_to_screen_mode_locked
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.fit_to_screen_mode_never
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.fit_to_screen_mode_start
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.forty_five_seconds
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.free_dictionary
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.full_screen
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.full_screen_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.green_theme
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_fifty_pcnt
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_forty_pcnt
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_landscape
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_landscape_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_one_to_one
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_portrait
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_portrait_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_puzzle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_sixty_pcnt
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_thirty_pcnt
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.guardian_daily
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.guardian_quick
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.guardian_quick_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.guardian_quiptic
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.hamburger_abendblatt_daily
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.hamburger_abendblatt_daily_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.hardware_keyboard
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.highlight_crossing_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.highlight_crossing_clue_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.independent_daily
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.indicate_show_errors
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.indicate_show_errors_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.infer_separators
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.infer_separators_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.interaction_settings
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.interaction_settings_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.internal_storage
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.irish_news_cryptic
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.jonesin_crosswords
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.joseph_crossword
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keglars_cryptics
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keglars_cryptics_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_always_show
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_azerty
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_announce_box
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_announce_box_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_announce_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_announce_clue_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_back
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_back_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_clue_list
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_clue_list_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_clue_notes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_clue_notes_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_copy_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_copy_clue_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_exit
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_exit_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_fit_to_screen
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_fit_to_screen_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_help
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_help_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_hint
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_insert_special
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_insert_special_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_next_clue_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_open_crossword_solver
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_open_crossword_solver_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_open_dictionary
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_open_dictionary_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_open_info
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_open_info_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_open_share_url
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_open_share_url_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_prev_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_prev_clue_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_puzzle_notes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_puzzle_notes_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_random_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_random_clue_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_reveal_errors
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_reveal_errors_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_reveal_letter
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_reveal_letter_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_reveal_puzzle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_reveal_puzzle_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_reveal_word
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_reveal_word_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_search_duckduckgo
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_search_duckduckgo_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_search_fifteen_squared
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_search_fifteen_squared_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_settings
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_settings_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_clue_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_clue_with_board
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_clue_with_board_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_puzzle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_puzzle_blank
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_puzzle_blank_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_puzzle_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_puzzle_no_ipuz
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_puzzle_no_ipuz_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_puzzle_original
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_puzzle_original_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_shuffle_anagram
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_shuffle_anagram_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_toggle_cell_flag
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_toggle_cell_flag_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_toggle_clue_flag
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_toggle_clue_flag_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_toggle_scratch_mode
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_toggle_scratch_mode_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_toggle_selection
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_toggle_selection_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_toggle_show_errors_cursor
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_toggle_show_errors_cursor_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_toggle_show_errors_grid
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_toggle_show_errors_grid_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_toggle_show_errors_word_cells
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_toggle_show_errors_word_cells_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_toggle_show_errors_word_word
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_toggle_show_errors_word_word_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_unparsable
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_voice_input
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_voice_input_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_zoom_in
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_zoom_in_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_zoom_in_max
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_zoom_in_max_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_zoom_out
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_zoom_out_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_zoom_reset
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_zoom_reset_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_bindings
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_bindings_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_bindings_help
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_bindings_help_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_built_in
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_colemak
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_compact
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_compact_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_dvorak
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_force_caps
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_force_caps_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_haptic
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_haptic_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_hide_button
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_hide_button_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_hide_manual
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_layout
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_layout_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_native
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_never_show
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_qwerty
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_qwertz
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_repeat_delay
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_repeat_delay_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_repeat_delay_hint
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_repeat_interval
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_repeat_interval_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_repeat_interval_hint
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_russian
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_settings
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_settings_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_show_sparingly_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_showhide
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_showhide_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_split_words
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_split_words_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_use_native
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_use_native_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.la_croix
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.la_times
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.la_times_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.large_sized
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.le_parisien_daily_f1
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.le_parisien_daily_f1_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.le_parisien_daily_f2
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.le_parisien_daily_f2_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.le_parisien_daily_f3
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.le_parisien_daily_f3_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.le_parisien_daily_f4
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.le_parisien_daily_f4_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.legacy_like_theme
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.license
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent_api_key
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent_api_key_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent_api_key_message
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent_chat_gpt
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent_claude
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent_none
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_voice_commands
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_voice_commands_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.logcat
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.mcp_host_port
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.mcp_host_port_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.mcp_host_port_hint
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.metro_cryptic
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.metro_quick
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.movement_strategy_next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.movement_strategy_next_parallel_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.movement_strategy_next_word
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.movement_strategy_stop_end
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.movement_style
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.movement_style_clue_list
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.movement_style_clue_list_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.movement_style_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.new_york_times_syndicated
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.newsday
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.no_hint_highlighting
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.no_hint_highlighting_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.no_puzzle_download_notifs
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.no_puzzle_download_notifs_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.no_summary_download_notifs
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.no_summary_download_notifs_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.nrc_in_het_midden
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.nrc_in_het_midden_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.nrc_scrypto
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.nrc_scrypto_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.orange_theme
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.orientation_lock
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.orientation_lock_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.orientation_lock_landscape
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.orientation_lock_portrait
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.orientation_lock_unlocked
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.play_letter_undo
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.play_letter_undo_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.predict_anagram_chars
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.predict_anagram_chars_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.preferences_export
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.preferences_export_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.preferences_export_failed
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.preferences_export_import
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.preferences_export_import_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.preferences_exported
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.preferences_import
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.preferences_import_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.preferences_import_failed
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.preferences_imported
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.premier_crossword
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.preserve_correct
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.preserve_correct_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.private_eye
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.private_eye_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.przekroj
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.przekroj_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzles_export
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzles_export_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.quick_dic
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.random_clue_on_shake
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.random_clue_on_shake_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.regular_sized
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.release_notes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.scraper_sources
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.scraper_sources_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.scroll_to_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.scroll_to_clue_crossing
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.scroll_to_clue_current
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.scroll_to_clue_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.scroll_to_clue_off
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.select_sources
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.select_sources_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.settings_label
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_settings
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_settings_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.sheffer_crossword
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.show_length
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.show_length_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.show_timer
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.show_timer_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.sixty_seconds
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.skip_filled
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.skip_filled_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.snap_current
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.snap_current_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.space_change_dir
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.space_change_dir_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.standard_theme
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.storage_location
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.storage_location_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.sud_ouest_mots_croises
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.sud_ouest_mots_fleches
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.swipe_action
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.swipe_action_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.tf1_mots_croises
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.tf1_mots_fleches
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.theme_preference
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.theme_preference_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.thirty_seconds
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_all_auto_download_blocks
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_all_auto_download_blocks_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_all_daily_sources
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_all_daily_sources_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_all_scrapers
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_all_scrapers_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_all_weekly_sources
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_all_weekly_sources_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_before_move
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_before_move_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.undecorated
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.undecorated_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.universal_crossword
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_daily
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_mon_fri
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_mon_sat
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_sundays
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_thursdays
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.usa_today
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.version
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.vingminutes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.vingminutes_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.voice_settings
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.voice_settings_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.voice_with_button
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.voice_with_button_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.voice_with_volume
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.voice_with_volume_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.wall_street_journal
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.washington_post_sunday
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.weekly_crosswords
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.weekly_crosswords_desc
import app.crossword.yourealwaysbe.forkyz.net.Downloader
import app.crossword.yourealwaysbe.forkyz.net.DownloaderID
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider
import app.crossword.yourealwaysbe.forkyz.settings.ActionButtonsMode
import app.crossword.yourealwaysbe.forkyz.settings.BrowseSwipeAction
import app.crossword.yourealwaysbe.forkyz.settings.ClueHighlight
import app.crossword.yourealwaysbe.forkyz.settings.ClueListClueLine
import app.crossword.yourealwaysbe.forkyz.settings.ClueTabsDouble
import app.crossword.yourealwaysbe.forkyz.settings.CycleUnfilledMode
import app.crossword.yourealwaysbe.forkyz.settings.DeleteCrossingModeSetting
import app.crossword.yourealwaysbe.forkyz.settings.DisplaySeparators
import app.crossword.yourealwaysbe.forkyz.settings.ExternalDictionarySetting
import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.FitToScreenMode
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.GridRatio
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardLayout
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardMode
import app.crossword.yourealwaysbe.forkyz.settings.LLMAgent
import app.crossword.yourealwaysbe.forkyz.settings.MovementStrategySetting
import app.crossword.yourealwaysbe.forkyz.settings.Orientation
import app.crossword.yourealwaysbe.forkyz.settings.SnapClueMode
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.settings.Theme
import app.crossword.yourealwaysbe.forkyz.util.ForkyzKey
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerShared
import app.crossword.yourealwaysbe.forkyz.util.mediate
import app.crossword.yourealwaysbe.forkyz.util.stateInSubscribed

private val DEVELOPMENT_URL = "https://gitlab.com/Hague/forkyz"

data class SettingsPage(val title : StringResource) {
    companion object {
        val ROOT = SettingsPage(Res.string.settings_label)
        val PUZZLE_SOURCES = SettingsPage(Res.string.select_sources)
        val PUZZLE_SOURCES_DAILY = SettingsPage(Res.string.daily_crosswords)
        val PUZZLE_SOURCES_WEEKLY = SettingsPage(Res.string.weekly_crosswords)
        val PUZZLE_SOURCES_SCRAPERS = SettingsPage(Res.string.scraper_sources)
        val DOWNLOAD = SettingsPage(Res.string.download_settings)
        val DOWNLOAD_BACKGROUND = SettingsPage(Res.string.background_download_opts)
        val BROWSER = SettingsPage(Res.string.browser_settings)
        val DISPLAY = SettingsPage(Res.string.display_settings)
        val INTERACTION = SettingsPage(Res.string.interaction_settings)
        val KEYBOARD = SettingsPage(Res.string.keyboard_settings)
        val KEYBOARD_BINDINGS = SettingsPage(Res.string.keyboard_bindings)
        val VOICE_ACCESSIBILITY = SettingsPage(Res.string.voice_settings)
        val EXTERNAL_TOOLS = SettingsPage(Res.string.external_tools_settings)
        val EXPORT_IMPORT = SettingsPage(Res.string.preferences_export_import)
        val SEARCH = SettingsPage(Res.string.filter_settings_title)
        val DESKTOP = SettingsPage(Res.string.desktop_settings)
        val ACTION_BUTTONS = SettingsPage(Res.string.action_buttons)
    }
}

/**
 * SettingsItem base class
 *
 * @param page the page the item is on
 */
abstract class SettingsItem(
    val page : SettingsPage,
)

/**
 * SettingsItem base class for most items that have a title/summary
 *
 * @param page the page the item is on
 * @param title the resource id of the title
 * @param summary the resource id of the summary
 * @param errorMsg stateflow for errors messages that may be displayed,
 * null if no error
 */
abstract class SettingsNamedItem(
    page : SettingsPage,
    val title : StringResource,
    val summary : StringResource?,
    val errorMsg : StateFlow<String?>? = null
) : SettingsItem(page)

/**
 * An entry that's just a title and string description
 *
 * @param page the page the item is on
 * @param title the resource id of the title
 * @param summary the summary text to display
 */
class SettingsText(
    page : SettingsPage,
    val title : StringResource,
    val summary : String,
) : SettingsItem(page)

/**
 * A base class that marks items as requiring later dialog input via
 * activeSettingsInputItem
 */
open class SettingsInputItem(
    page : SettingsPage,
    title : StringResource,
    summary : StringResource?,
    errorMsg : StateFlow<String?>? = null
) : SettingsNamedItem(page, title, summary, errorMsg)

class SettingsSubPage(
    page : SettingsPage,
    summary : StringResource?,
    val subpage : SettingsPage,
) : SettingsNamedItem(page, subpage.title, summary)

class SettingsBoolean(
    page : SettingsPage,
    title : StringResource,
    summary : StringResource?,
    val value : StateFlow<Boolean>,
    val setValue : (Boolean) -> Unit,
    errorMsg : StateFlow<String?>? = null,
    val extraSettingsPage : SettingsPage? = null,
) : SettingsNamedItem(page, title, summary, errorMsg)

enum class TriState {
    OFF, INDETERMINATE, ON
}

class SettingsTriState(
    page : SettingsPage,
    title : StringResource,
    summary : StringResource?,
    val value : StateFlow<TriState>,
    val setValue : (TriState) -> Unit,
) : SettingsNamedItem(page, title, summary) {
    companion object {
        fun build(
            scope : CoroutineScope,
            page : SettingsPage,
            title : StringResource,
            summary : StringResource?,
            booleans : List<SettingsBoolean>,
        ) : SettingsTriState {
            return SettingsTriState(
                page,
                title,
                summary,
                combine(
                    booleans.map { it.value }.toTypedArray().asIterable(),
                    { booleans.triState { it.value.value } },
                ).stateInSubscribed(
                    scope,
                    booleans.triState { it.value.value },
                ),
                {
                    when (it) {
                        TriState.ON -> {
                            booleans.forEach { it.setValue(true) }
                        }
                        TriState.OFF -> {
                            booleans.forEach { it.setValue(false) }
                        }
                        else -> { /* do nothing */ }
                    }
                },
            )
        }
    }
}

/**
 * Label can be id or string
 *
 * One must be non-null
 */
class SettingsListEntry<T> private constructor(
    val labelId : StringResource?,
    val labelString : String?,
    val value : T,
) {
    constructor(label : StringResource, value : T) : this(label, null, value)
    constructor(s : String, value : T) : this(null, s, value)
}

open class SettingsDynamicList<T>(
    page : SettingsPage,
    title : StringResource,
    summary : StringResource?,
    val entries : StateFlow<List<SettingsListEntry<T>>>,
    val value : StateFlow<T>,
    private val setValue : (T) -> Unit,
) : SettingsInputItem(page, title, summary) {
    fun selectItem(index : Int) {
        entries.value.let {
            if (0 <= index && index < it.size)
                setValue(it[index].value)
        }
    }
}

class SettingsList<T>(
    page : SettingsPage,
    title : StringResource,
    summary : StringResource?,
    entries : List<SettingsListEntry<T>>,
    value : StateFlow<T>,
    setValue : (T) -> Unit,
) : SettingsDynamicList<T>(
    page,
    title,
    summary,
    MutableStateFlow<List<SettingsListEntry<T>>>(entries),
    value,
    setValue
)

open class SettingsDynamicMultiList<T>(
    page : SettingsPage,
    title : StringResource,
    summary : StringResource?,
    val entries : StateFlow<List<SettingsListEntry<T>>>,
    val value : StateFlow<Set<T>>,
    private val setValue : (Set<T>) -> Unit,
    errorMsg : StateFlow<String?>? = null,
) : SettingsInputItem(page, title, summary, errorMsg) {
    fun selectItems(indices : Set<Int>) {
        entries.value.let { entries ->
            setValue(
                indices
                    .filter { 0 <= it && it <= entries.size }
                    .map { entries[it].value }
                    .toSet()
            )
        }
    }
}

open class SettingsMultiList<T>(
    page : SettingsPage,
    title : StringResource,
    summary : StringResource?,
    entries : List<SettingsListEntry<T>>,
    value : StateFlow<Set<T>>,
    val setValue : (Set<T>) -> Unit,
    errorMsg : StateFlow<String?>? = null,
) : SettingsDynamicMultiList<T>(
    page,
    title,
    summary,
    MutableStateFlow<List<SettingsListEntry<T>>>(entries),
    value,
    setValue,
    errorMsg,
)

/**
 * @param dialogMsg message to display instead of summary in dialog
 */
class SettingsString(
    page : SettingsPage,
    title : StringResource,
    summary : StringResource?,
    val hint : StringResource?,
    val value : StateFlow<String>,
    val setValue : (String) -> Unit,
    errorMsg : StateFlow<String?>? = null,
    val dialogMsg : StringResource? = null,
) : SettingsInputItem(page, title, summary, errorMsg)

class SettingsInteger(
    page : SettingsPage,
    title : StringResource,
    summary : StringResource?,
    val hint : StringResource?,
    val value : StateFlow<Int>,
    val setValue : (Int) -> Unit,
    errorMsg : StateFlow<String?>? = null,
) : SettingsInputItem(page, title, summary, errorMsg)

class SettingsHeading(
    page : SettingsPage,
    title : StringResource,
    summary : StringResource? = null,
) : SettingsNamedItem(page, title, summary)

class SettingsHTMLPage(
    page : SettingsPage,
    title : StringResource,
    summary : StringResource?,
    val htmlPage : HTMLFile,
) : SettingsNamedItem(page, title, summary) {
    constructor(
        page : SettingsPage,
        title : StringResource,
        htmlPage : HTMLFile,
    ) : this(page, title, null, htmlPage)
}

class SettingsOpenURI(
    page : SettingsPage,
    title : StringResource,
    summary : StringResource?,
    val uri : Uri,
) : SettingsNamedItem(page, title, summary)

class SettingsDivider(
    page : SettingsPage,
): SettingsItem(page)

class SettingsAction(
    page : SettingsPage,
    title : StringResource,
    summary : StringResource?,
    val onClick : () -> Unit,
) : SettingsNamedItem(page, title, summary)

/**
 * Call Logcat activity
 *
 * Should be made into a more general class if more activities come along.
 */
class SettingsLogcat(
    page : SettingsPage,
    title : StringResource,
) : SettingsNamedItem(page, title, null)

/**
 * Call page showing libaries
 */
class SettingsAboutLibraries(
    page : SettingsPage,
    title : StringResource,
) : SettingsNamedItem(page, title, null)

data class ShareSettingsData(
    val mimeType : String,
    val uri : Uri,
)

enum class RequestedURI {
    NONE, SAF, SETTINGS_EXPORT, SETTINGS_IMPORT, PUZZLES_EXPORT
}

/**
 * UI State
 *
 * @param currentPage which settings page is current showing
 * @param searchTerm current search term if searching
 * @param hasBackStack if the back button should return to a previous
 * page (if not, activity should exit
 * @param settingsItems list of settings items
 * @param activeInputItem if one of the settings requires input and the
 * UI should be asking for it
 * @param storageChangedAppExit if the storage has changed and the app
 * needs to restart
 */
data class SettingsPageViewState(
    val currentPage : SettingsPage,
    val searchTerm : String,
    val hasBackStack : Boolean,
    val settingsItems : List<SettingsItem>,
    val activeInputItem : SettingsInputItem?,
    val storageChangedAppExit : Boolean,
) {
    val inSearchMode
        get() = currentPage == SettingsPage.SEARCH
}

/**
 * Represents a downloader that will need settings generated for it
 */
private data class DownloaderSettingsData(
    val downloaderID : DownloaderID,
    val page : SettingsPage,
    val title : StringResource,
    val summary : StringResource? = null
)

class SettingsPageViewModel(
    settings : ForkyzSettings,
    private val utils : NativeBackendUtils,
    private val downloadersProvider : DownloadersProvider,
) : ForkyzPageViewModel(settings) {

    private val fileActionMutex = Mutex()

    private val availableThemeSettings = listOf(
            SettingsListEntry(Res.string.standard_theme, Theme.T_STANDARD),
        ) + (
            if (utils.isDynamicColorAvailable) {
                listOf(
                    SettingsListEntry(
                        Res.string.dynamic_theme,
                        Theme.T_DYNAMIC,
                    ),
                )
            } else {
                listOf()
            }
        ) + listOf(
            SettingsListEntry(Res.string.legacy_like_theme, Theme.T_LEGACY_LIKE),
            SettingsListEntry(Res.string.blue_theme, Theme.T_BLUE),
            SettingsListEntry(Res.string.green_theme, Theme.T_GREEN),
            SettingsListEntry(Res.string.orange_theme, Theme.T_ORANGE),
        )

    private val storageLocationsStateFlow
        : StateFlow<List<SettingsListEntry<StorageLocation>>>
            = settings.liveFileHandlerSettings.map { fhSettings ->
                val safRootURI = if (fhSettings.safRootURI.length > 0)
                    fhSettings.safRootURI
                else
                    utils.getString(Res.string.external_storage_saf_none_selected)

                val label = (
                    utils.getString(Res.string.external_storage_saf)
                    + " "
                    + utils.getString(
                        Res.string.external_storage_saf_current_uri,
                        safRootURI
                    )
                )

                listOf(
                    SettingsListEntry(
                        Res.string.internal_storage,
                        StorageLocation.SL_INTERNAL,
                    ),
                    SettingsListEntry(label, StorageLocation.SL_EXTERNAL_SAF),
                )
           }.stateInSubscribed(viewModelScope, listOf())

    private val liveDownloaders : Flow<List<Downloader>>
        = downloadersProvider.liveDownloaders.filterNotNull().map { dls ->
            dls.getDownloaders()
        }

    private val liveDownloaderIDs : Flow<List<DownloaderID>>
        = downloadersProvider.liveDownloaders.filterNotNull().map { dls ->
            dls.getDownloaders().map { it.downloaderID }
        }

    private val downloadCustomDailyURLErrorMsg : StateFlow<String?>
        = combine(
            settings.liveDownloadEnabledDownloader(DownloaderID.CUSTOM),
            settings.liveDownloadCustomDailyURL,
        ) { enabled, url ->
            if (enabled) {
                if (url.trim().isEmpty()) {
                    utils.getString(Res.string.custom_url_missing)
                } else {
                    try {
                        TimeUtils.convertUrlFormatPattern(url)
                        null
                    } catch (e : IllegalArgumentException) {
                        e.message
                    }
                }
            } else {
                null
            }
        }.stateInSubscribed(viewModelScope, null)

    private val downloadersDaily = listOf(
        DownloaderSettingsData(
            DownloaderID.DE_STANDAARD,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.de_standaard,
            Res.string.de_standaard_desc,
        ),
        DownloaderSettingsData(
            DownloaderID.EL_PAIS_EXPERTO,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.elpais_experto,
            Res.string.elpais_experto_desc,
        ),
        DownloaderSettingsData(
            DownloaderID.GUARDIAN_DAILY_CRYPTIC,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.guardian_daily,
            Res.string.updates_mon_fri,
        ),
        DownloaderSettingsData(
            DownloaderID.HAM_ABEND,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.hamburger_abendblatt_daily,
            Res.string.hamburger_abendblatt_daily_desc,
        ),
        DownloaderSettingsData(
            DownloaderID.INDEPENDENT_DAILY_CRYPTIC,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.independent_daily,
        ),
        DownloaderSettingsData(
            DownloaderID.IRISH_NEWS_CRYPTIC,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.irish_news_cryptic,
            Res.string.updates_mon_fri,
        ),
        DownloaderSettingsData(
            DownloaderID.JOSEPH,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.joseph_crossword,
            Res.string.updates_mon_sat,
        ),
        DownloaderSettingsData(
            DownloaderID.TWENTY_MINUTES,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.vingminutes,
            Res.string.vingminutes_desc,
        ),
        DownloaderSettingsData(
            DownloaderID.LA_CROIX,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.la_croix,
            Res.string.updates_mon_fri,
        ),
        DownloaderSettingsData(
            DownloaderID.LA_TIMES,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.la_times,
            Res.string.la_times_desc,
        ),
        DownloaderSettingsData(
            DownloaderID.LE_PARISIEN_F1,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.le_parisien_daily_f1,
            Res.string.le_parisien_daily_f1_desc,
        ),
        DownloaderSettingsData(
            DownloaderID.LE_PARISIEN_F2,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.le_parisien_daily_f2,
            Res.string.le_parisien_daily_f2_desc,
        ),
        DownloaderSettingsData(
            DownloaderID.LE_PARISIEN_F3,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.le_parisien_daily_f3,
            Res.string.le_parisien_daily_f3_desc,
        ),
        DownloaderSettingsData(
            DownloaderID.LE_PARISIEN_F4,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.le_parisien_daily_f4,
            Res.string.le_parisien_daily_f4_desc,
        ),
        DownloaderSettingsData(
            DownloaderID.METRO_CRYPTIC,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.metro_cryptic,
            Res.string.updates_mon_sat,
        ),
        DownloaderSettingsData(
            DownloaderID.METRO_QUICK,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.metro_quick,
            Res.string.updates_mon_sat,
        ),
        DownloaderSettingsData(
            DownloaderID.NEWSDAY,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.newsday,
        ),
        DownloaderSettingsData(
            DownloaderID.NYT_SYNDICATED,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.new_york_times_syndicated,
        ),
        DownloaderSettingsData(
            DownloaderID.NRC_IN_HET_MIDDEN,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.nrc_in_het_midden,
            Res.string.nrc_in_het_midden_desc,
        ),
        DownloaderSettingsData(
            DownloaderID.SHEFFER,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.sheffer_crossword,
            Res.string.updates_mon_sat,
        ),
        DownloaderSettingsData(
            DownloaderID.SUD_OUEST_MOTS_CROISES,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.sud_ouest_mots_croises,
            Res.string.updates_daily,
        ),
        DownloaderSettingsData(
            DownloaderID.SUD_OUEST_MOTS_FLECHES,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.sud_ouest_mots_fleches,
            Res.string.updates_daily,
        ),
        DownloaderSettingsData(
            DownloaderID.TF1_MOTS_CROISES,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.tf1_mots_croises,
            Res.string.updates_daily,
        ),
        DownloaderSettingsData(
            DownloaderID.TF1_MOTS_FLECHES,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.tf1_mots_fleches,
            Res.string.updates_daily,
        ),
        DownloaderSettingsData(
            DownloaderID.UNIVERSAL,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.universal_crossword,
            Res.string.updates_mon_sat,
        ),
        DownloaderSettingsData(
            DownloaderID.USA_TODAY,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.usa_today,
            Res.string.updates_daily,
        ),
        DownloaderSettingsData(
            DownloaderID.WSJ,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.wall_street_journal,
            Res.string.updates_mon_sat,
        ),
    )

    private val downloadersCustomDaily = listOf(
        DownloaderSettingsData(
            DownloaderID.CUSTOM,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.custom,
            Res.string.custom_desc,
        )
    )

    private val downloadersWeeklyRegular = listOf(
        DownloaderSettingsData(
            DownloaderID.DE_TELEGRAAF,
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            Res.string.de_telegraaf,
            Res.string.de_telegraaf_desc,
        ),
        DownloaderSettingsData(
            DownloaderID.JONESIN,
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            Res.string.jonesin_crosswords,
            Res.string.updates_thursdays,
        ),
        DownloaderSettingsData(
            DownloaderID.GUARDIAN_WEEKLY_QUIPTIC,
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            Res.string.guardian_quiptic,
            Res.string.updates_sundays,
        ),
        DownloaderSettingsData(
            DownloaderID.NRC_SCRYPTO,
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            Res.string.nrc_scrypto,
            Res.string.nrc_scrypto_desc,
        )
    )

    private val downloadersWeeklyLarge = listOf(
        DownloaderSettingsData(
            DownloaderID.PREMIER,
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            Res.string.premier_crossword,
            Res.string.updates_sundays,
        ),
        DownloaderSettingsData(
            DownloaderID.WAPO_SUNDAY,
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            Res.string.washington_post_sunday,
            Res.string.updates_sundays,
        ),
    )

    private val downloadersScrapers = listOf(
        DownloaderSettingsData(
            DownloaderID.CRYPTIC_CRU,
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            Res.string.cru_puzzle_workshop,
            Res.string.cru_puzzle_workshop_desc,
        ),
        DownloaderSettingsData(
            DownloaderID.GUARDIAN_QUICK,
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            Res.string.guardian_quick,
            Res.string.guardian_quick_desc,
        ),
        DownloaderSettingsData(
            DownloaderID.KEGLAR,
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            Res.string.keglars_cryptics,
            Res.string.keglars_cryptics_desc,
        ),
        DownloaderSettingsData(
            DownloaderID.PRIVATE_EYE,
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            Res.string.private_eye,
            Res.string.private_eye_desc,
        ),
        DownloaderSettingsData(
            DownloaderID.PRZEKROJ,
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            Res.string.przekroj,
            Res.string.przekroj_desc,
        ),
    )

    private val downloadersTogglesDaily
        = downloadersDaily.map(this::makeDownloaderToggle)

    private val downloadersExtraDaily
        = downloadersDaily.flatMap(this::makeDownloaderExtraSettings)

    val downloadersTogglesCustomDaily
        = downloadersCustomDaily.map(this::makeDownloaderToggle)

    private val downloadersExtraCustomDaily
        = downloadersCustomDaily.flatMap(this::makeDownloaderExtraSettings)

    val downloadersTogglesWeeklyRegular
        = downloadersWeeklyRegular.map(this::makeDownloaderToggle)

    private val downloadersExtraWeeklyRegular
        = downloadersWeeklyRegular.flatMap(this::makeDownloaderExtraSettings)

    val downloadersTogglesWeeklyLarge
        = downloadersWeeklyLarge.map(this::makeDownloaderToggle)

    private val downloadersExtraWeeklyLarge
        = downloadersWeeklyLarge.flatMap(this::makeDownloaderExtraSettings)

    val downloadersTogglesScrapers
        = downloadersScrapers.map(this::makeDownloaderToggle)

    private val downloadersExtraScrapers
        = downloadersScrapers.flatMap(this::makeDownloaderExtraSettings)

    private val settingsItemsList : List<SettingsItem> = utils.filterSettings(
        listOf(
            SettingsDynamicList<StorageLocation>(
                SettingsPage.ROOT,
                Res.string.storage_location,
                Res.string.storage_location_desc,
                storageLocationsStateFlow,
                settings.liveFileHandlerStorageLocation.stateInHere(
                    StorageLocation.SL_INTERNAL,
                ),
                this::onStorageLocationChange,
            ),
            SettingsSubPage(
                SettingsPage.ROOT,
                Res.string.select_sources_desc,
                SettingsPage.PUZZLE_SOURCES,
            ),
            SettingsSubPage(
                SettingsPage.ROOT,
                Res.string.download_settings_desc,
                SettingsPage.DOWNLOAD,
            ),
            SettingsSubPage(
                SettingsPage.ROOT,
                Res.string.browser_settings_desc,
                SettingsPage.BROWSER,
            ),
            SettingsSubPage(
                SettingsPage.ROOT,
                Res.string.display_settings_desc,
                SettingsPage.DISPLAY,
            ),
            SettingsSubPage(
                SettingsPage.ROOT,
                Res.string.interaction_settings_desc,
                SettingsPage.INTERACTION,
            ),
            SettingsSubPage(
                SettingsPage.ROOT,
                Res.string.action_buttons_desc,
                SettingsPage.ACTION_BUTTONS,
            ),
            SettingsSubPage(
                SettingsPage.ROOT,
                Res.string.keyboard_settings_desc,
                SettingsPage.KEYBOARD,
            ),
            SettingsSubPage(
                SettingsPage.ROOT,
                Res.string.voice_settings_desc,
                SettingsPage.VOICE_ACCESSIBILITY,
            ),
            SettingsSubPage(
                SettingsPage.ROOT,
                Res.string.external_tools_settings_desc,
                SettingsPage.EXTERNAL_TOOLS,
            ),
            SettingsSubPage(
                SettingsPage.ROOT,
                Res.string.preferences_export_import_desc,
                SettingsPage.EXPORT_IMPORT,
            ),
            SettingsSubPage(
                SettingsPage.ROOT,
                Res.string.desktop_settings_desc,
                SettingsPage.DESKTOP,
            ),
            SettingsHeading(
                SettingsPage.ROOT,
                Res.string.about_forkyz,
            ),
            SettingsText(
                SettingsPage.ROOT,
                Res.string.version,
                BuildConfig.APP_VERSION,
            ),
            SettingsOpenURI(
                SettingsPage.ROOT,
                Res.string.development_site,
                Res.string.development_site_desc,
                Uri.parse(DEVELOPMENT_URL),
            ),
            SettingsHTMLPage(
                SettingsPage.ROOT,
                Res.string.release_notes,
                HTMLFile.RELEASE_NOTES,
            ),
            SettingsHTMLPage(
                SettingsPage.ROOT,
                Res.string.license,
                HTMLFile.LICENSE,
            ),
            SettingsAboutLibraries(
                SettingsPage.ROOT,
                Res.string.about_libraries,
            ),
            SettingsLogcat(
                SettingsPage.ROOT,
                Res.string.logcat,
            ),

            // Sources
            SettingsSubPage(
                SettingsPage.PUZZLE_SOURCES,
                Res.string.daily_crosswords_desc,
                SettingsPage.PUZZLE_SOURCES_DAILY,
            ),
            SettingsSubPage(
                SettingsPage.PUZZLE_SOURCES,
                Res.string.weekly_crosswords_desc,
                SettingsPage.PUZZLE_SOURCES_WEEKLY,
            ),
            SettingsSubPage(
                SettingsPage.PUZZLE_SOURCES,
                Res.string.scraper_sources_desc,
                SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            ),
            SettingsTriState.build(
                viewModelScope,
                SettingsPage.PUZZLE_SOURCES_DAILY,
                Res.string.toggle_all_daily_sources,
                Res.string.toggle_all_daily_sources_desc,
                downloadersTogglesDaily + downloadersTogglesCustomDaily,
            ),
        ) + downloadersTogglesDaily + downloadersExtraDaily + listOf(
            SettingsHeading(
                SettingsPage.PUZZLE_SOURCES_DAILY,
                Res.string.custom_daily,
            ),
        ) + downloadersTogglesCustomDaily + downloadersExtraCustomDaily + listOf(
            SettingsTriState.build(
                viewModelScope,
                SettingsPage.PUZZLE_SOURCES_WEEKLY,
                Res.string.toggle_all_weekly_sources,
                Res.string.toggle_all_weekly_sources_desc,
                downloadersTogglesWeeklyRegular + downloadersTogglesWeeklyLarge,
            ),
            SettingsHeading(
                SettingsPage.PUZZLE_SOURCES_WEEKLY,
                Res.string.regular_sized,
                null,
            ),
        ) + downloadersTogglesWeeklyRegular + downloadersExtraWeeklyRegular + listOf(
            SettingsHeading(
                SettingsPage.PUZZLE_SOURCES_WEEKLY,
                Res.string.large_sized,
                null,
            ),
        ) + downloadersTogglesWeeklyLarge + downloadersExtraWeeklyLarge + listOf(
            SettingsTriState.build(
                viewModelScope,
                SettingsPage.PUZZLE_SOURCES_SCRAPERS,
                Res.string.toggle_all_scrapers,
                Res.string.toggle_all_scrapers_desc,
                downloadersTogglesScrapers,
            ),
        ) + downloadersTogglesScrapers + downloadersExtraScrapers + listOf(
            SettingsDivider(SettingsPage.PUZZLE_SOURCES_SCRAPERS),
            SettingsHTMLPage(
                SettingsPage.PUZZLE_SOURCES_SCRAPERS,
                Res.string.about_scrapes,
                HTMLFile.SCRAPERS_HELP,
            ),

            // Download
            SettingsBoolean(
                SettingsPage.DOWNLOAD,
                Res.string.download_remember_date,
                Res.string.download_remember_date_desc,
                settings.liveDownloadRememberDate.stateInHereBool(),
                settings::setDownloadRememberDate,
            ),
            SettingsBoolean(
                SettingsPage.DOWNLOAD,
                Res.string.automatic_download,
                Res.string.automatic_download_desc,
                settings.liveDownloadOnStartUp.stateInHereBool(),
                settings::setDownloadOnStartUp,
            ),
            SettingsSubPage(
                SettingsPage.DOWNLOAD,
                Res.string.background_download_opts_desc,
                SettingsPage.DOWNLOAD_BACKGROUND,
            ),
            SettingsBoolean(
                SettingsPage.DOWNLOAD,
                Res.string.no_summary_download_notifs,
                Res.string.no_summary_download_notifs_desc,
                settings.liveDownloadSuppressSummaryMessages.stateInHereBool(),
                settings::setDownloadSuppressSummaryMessages,
            ),
            SettingsBoolean(
                SettingsPage.DOWNLOAD,
                Res.string.no_puzzle_download_notifs,
                Res.string.no_puzzle_download_notifs_desc,
                settings.liveDownloadSuppressMessages.stateInHereBool(),
                settings::setDownloadSuppressMessages,
            ),
            SettingsList<Int>(
                SettingsPage.DOWNLOAD,
                Res.string.download_timeout,
                Res.string.download_timeout_desc,
                listOf(
                    SettingsListEntry(Res.string.fifteen_seconds, 15000),
                    SettingsListEntry(Res.string.thirty_seconds, 30000),
                    SettingsListEntry(Res.string.forty_five_seconds, 45000),
                    SettingsListEntry(Res.string.sixty_seconds, 60000),
                ),
                settings.liveDownloadTimeout.stateInHereInt(),
                settings::setDownloadTimeout,
            ),

            // Background Download
            SettingsHeading(
                SettingsPage.DOWNLOAD_BACKGROUND,
                Res.string.background_download_time_config,
            ),
            SettingsBoolean(
                SettingsPage.DOWNLOAD_BACKGROUND,
                Res.string.background_download_hourly,
                Res.string.background_download_hourly_desc,
                settings.liveDownloadHourly.stateInHereBool(),
                { value -> settings.setDownloadHourly(value) },
            ),
            SettingsMultiList(
                SettingsPage.DOWNLOAD_BACKGROUND,
                Res.string.background_download_days,
                Res.string.background_download_days_desc,
                listOf(
                    // matches Java DayOfWeek
                    SettingsListEntry(Res.string.background_download_mon, "1"),
                    SettingsListEntry(Res.string.background_download_tue, "2"),
                    SettingsListEntry(Res.string.background_download_wed, "3"),
                    SettingsListEntry(Res.string.background_download_thu, "4"),
                    SettingsListEntry(Res.string.background_download_fri, "5"),
                    SettingsListEntry(Res.string.background_download_sat, "6"),
                    SettingsListEntry(Res.string.background_download_sun, "7"),
                ),
                settings.liveDownloadDays.stateInHereSet(),
                { value -> settings.setDownloadDays(value) },
            ),
            SettingsList<Int>(
                SettingsPage.DOWNLOAD_BACKGROUND,
                Res.string.background_download_days_time,
                Res.string.background_download_days_time_desc,
                listOf(
                    SettingsListEntry(Res.string.background_download_0, 0),
                    SettingsListEntry(Res.string.background_download_1, 1),
                    SettingsListEntry(Res.string.background_download_2, 2),
                    SettingsListEntry(Res.string.background_download_3, 3),
                    SettingsListEntry(Res.string.background_download_4, 4),
                    SettingsListEntry(Res.string.background_download_5, 5),
                    SettingsListEntry(Res.string.background_download_6, 6),
                    SettingsListEntry(Res.string.background_download_7, 7),
                    SettingsListEntry(Res.string.background_download_8, 8),
                    SettingsListEntry(Res.string.background_download_9, 9),
                    SettingsListEntry(Res.string.background_download_10, 10),
                    SettingsListEntry(Res.string.background_download_11, 11),
                    SettingsListEntry(Res.string.background_download_12, 12),
                    SettingsListEntry(Res.string.background_download_13, 13),
                    SettingsListEntry(Res.string.background_download_14, 14),
                    SettingsListEntry(Res.string.background_download_15, 15),
                    SettingsListEntry(Res.string.background_download_16, 16),
                    SettingsListEntry(Res.string.background_download_17, 17),
                    SettingsListEntry(Res.string.background_download_18, 18),
                    SettingsListEntry(Res.string.background_download_19, 19),
                    SettingsListEntry(Res.string.background_download_20, 20),
                    SettingsListEntry(Res.string.background_download_21, 21),
                    SettingsListEntry(Res.string.background_download_22, 22),
                    SettingsListEntry(Res.string.background_download_23, 23),
                ),
                settings.liveDownloadDaysTime.stateInHereInt(),
                { value -> settings.setDownloadDaysTime(value) },
            ),
            SettingsHeading(
                SettingsPage.DOWNLOAD_BACKGROUND,
                Res.string.background_download_constraints_config,
            ),
            SettingsBoolean(
                SettingsPage.DOWNLOAD_BACKGROUND,
                Res.string.download_wifi_only,
                Res.string.download_wifi_only_desc,
                settings.liveDownloadRequireUnmetered.stateInHereBool(),
                { value -> settings.setDownloadRequireUnmetered(value) },
            ),
            SettingsBoolean(
                SettingsPage.DOWNLOAD_BACKGROUND,
                Res.string.download_roaming,
                Res.string.download_roaming_desc,
                settings.liveDownloadAllowRoaming.stateInHereBool(),
                { value -> settings.setDownloadAllowRoaming(value) },
            ),
            SettingsBoolean(
                SettingsPage.DOWNLOAD_BACKGROUND,
                Res.string.download_charging,
                Res.string.download_charging_desc,
                settings.liveDownloadRequireCharging.stateInHereBool(),
                { value -> settings.setDownloadRequireCharging(value) },
            ),

            // Browser
            SettingsBoolean(
                SettingsPage.BROWSER,
                Res.string.dont_confirm_cleanup,
                Res.string.dont_confirm_cleanup_desc,
                settings.liveBrowseDontConfirmCleanup.stateInHereBool(),
                settings::setBrowseDontConfirmCleanup,
            ),
            SettingsBoolean(
                SettingsPage.BROWSER,
                Res.string.delete_on_cleanup,
                Res.string.delete_on_cleanup_desc,
                settings.liveBrowseDeleteOnCleanup.stateInHereBool(),
                settings::setBrowseDeleteOnCleanup,
            ),
            SettingsList<Int>(
                SettingsPage.BROWSER,
                Res.string.cleanup_unfinished,
                Res.string.cleanup_unfinished_desc,
                listOf(
                    SettingsListEntry(Res.string.cleanup_age_two_days, 2),
                    SettingsListEntry(Res.string.cleanup_age_one_week, 7),
                    SettingsListEntry(Res.string.cleanup_age_thirty_days, 30),
                    SettingsListEntry(Res.string.cleanup_age_never, -1),
                ),
                settings.liveBrowseCleanupAge.stateInHereInt(),
                settings::setBrowseCleanupAge,
            ),
            SettingsList<Int>(
                SettingsPage.BROWSER,
                Res.string.cleanup_archives,
                Res.string.cleanup_archives_desc,
                listOf(
                    SettingsListEntry(Res.string.cleanup_age_two_days, 2),
                    SettingsListEntry(Res.string.cleanup_age_one_week, 7),
                    SettingsListEntry(Res.string.cleanup_age_thirty_days, 30),
                    SettingsListEntry(Res.string.cleanup_age_never, -1),
                ),
                settings.liveBrowseCleanupAgeArchive.stateInHereInt(),
                settings::setBrowseCleanupAgeArchive,
            ),
            SettingsBoolean(
                SettingsPage.BROWSER,
                Res.string.disable_swipe,
                Res.string.disable_swipe_desc,
                settings.liveBrowseDisableSwipe.stateInHereBool(),
                settings::setBrowseDisableSwipe,
            ),
            SettingsList<BrowseSwipeAction>(
                SettingsPage.BROWSER,
                Res.string.swipe_action,
                Res.string.swipe_action_desc,
                listOf(
                    SettingsListEntry(
                        Res.string.delete,
                        BrowseSwipeAction.BSA_DELETE
                    ),
                    SettingsListEntry(
                        Res.string.archive,
                        BrowseSwipeAction.BSA_ARCHIVE
                    ),
                ),
                settings.liveBrowseSwipeAction.stateInHere(
                    BrowseSwipeAction.BSA_ARCHIVE,
                ),
                settings::setBrowseSwipeAction,
            ),
            SettingsBoolean(
                SettingsPage.BROWSER,
                Res.string.dont_confirm_browse_delete,
                Res.string.dont_confirm_browse_delete_desc,
                settings.liveBrowseDontConfirmDelete.stateInHereBool(),
                settings::setBrowseDontConfirmDelete,
            ),
            SettingsBoolean(
                SettingsPage.BROWSER,
                Res.string.browse_show_percentage_correct,
                Res.string.browse_show_percentage_correct_desc,
                settings.liveBrowseShowPercentageCorrect.stateInHereBool(),
                settings::setBrowseShowPercentageCorrect,
            ),
            SettingsBoolean(
                SettingsPage.BROWSER,
                Res.string.browse_always_show_rating,
                Res.string.browse_always_show_rating_desc,
                settings.liveBrowseAlwaysShowRating.stateInHereBool(),
                settings::setBrowseAlwaysShowRating,
            ),
            SettingsBoolean(
                SettingsPage.BROWSER,
                Res.string.browse_indicate_if_solution,
                Res.string.browse_indicate_if_solution_desc,
                settings.liveBrowseIndicateIfSolution.stateInHereBool(),
                settings::setBrowseIndicateIfSolution,
            ),
            SettingsBoolean(
                SettingsPage.BROWSER,
                Res.string.collapse_headings,
                Res.string.collapse_headings_desc,
                settings.liveBrowseCollapseHeadings.stateInHereBool(),
                settings::setBrowseCollapseHeadings,
            ),

            // Display
            SettingsList<Theme>(
                SettingsPage.DISPLAY,
                Res.string.theme_preference,
                Res.string.theme_preference_desc,
                availableThemeSettings,
                settings.liveAppTheme.stateInHere(Theme.T_STANDARD),
                settings::setAppTheme,
            ),
            SettingsBoolean(
                SettingsPage.DISPLAY,
                Res.string.clue_below_grid,
                Res.string.clue_below_grid_desc,
                settings.livePlayClueBelowGrid.stateInHereBool(),
                settings::setPlayClueBelowGrid,
            ),
            SettingsBoolean(
                SettingsPage.DISPLAY,
                Res.string.expandable_clue_line,
                Res.string.expandable_clue_line_desc,
                settings.livePlayExpandableClueLine.stateInHereBool(),
                settings::setPlayExpandableClueLine,
            ),
            SettingsList<ClueListClueLine>(
                SettingsPage.DISPLAY,
                Res.string.clue_list_name_in_clue_line,
                Res.string.clue_list_name_in_clue_line_desc,
                listOf(
                    SettingsListEntry(
                        Res.string.clue_list_clue_line_full,
                        ClueListClueLine.CLCL_FULL,
                    ),
                    SettingsListEntry(
                        Res.string.clue_list_clue_line_abbreviated,
                        ClueListClueLine.CLCL_ABBREVIATED,
                    ),
                    SettingsListEntry(
                        Res.string.clue_list_clue_line_none,
                        ClueListClueLine.CLCL_NONE,
                    ),
                ),
                settings.livePlayClueListNameInClueLine.stateInHere(
                    ClueListClueLine.CLCL_FULL,
                ),
                settings::setPlayClueListNameInClueLine,
            ),
            SettingsBoolean(
                SettingsPage.DISPLAY,
                Res.string.show_length,
                Res.string.show_length_desc,
                settings.livePlayShowCount.stateInHereBool(),
                settings::setPlayShowCount,
            ),
            SettingsList<FitToScreenMode>(
                SettingsPage.DISPLAY,
                Res.string.fit_to_screen_mode,
                Res.string.fit_to_screen_mode_desc,
                listOf(
                    SettingsListEntry(
                        Res.string.fit_to_screen_mode_never,
                        FitToScreenMode.FTSM_NEVER,
                    ),
                    SettingsListEntry(
                        Res.string.fit_to_screen_mode_start,
                        FitToScreenMode.FTSM_START,
                    ),
                    SettingsListEntry(
                        Res.string.fit_to_screen_mode_locked,
                        FitToScreenMode.FTSM_LOCKED,
                    ),
                ),
                settings.livePlayFitToScreenMode.stateInHere(
                    FitToScreenMode.FTSM_NEVER,
                ),
                settings::setPlayFitToScreenMode,
            ),
            SettingsBoolean(
                SettingsPage.DISPLAY,
                Res.string.display_scratch,
                Res.string.display_scratch_desc,
                settings.livePlayScratchDisplay.stateInHereBool(),
                settings::setPlayScratchDisplay,
            ),
            SettingsBoolean(
                SettingsPage.DISPLAY,
                Res.string.no_hint_highlighting,
                Res.string.no_hint_highlighting_desc,
                settings.livePlaySuppressHintHighlighting.stateInHereBool(),
                settings::setPlaySuppressHintHighlighting,
            ),
            SettingsList<DisplaySeparators>(
                SettingsPage.DISPLAY,
                Res.string.display_separators,
                Res.string.display_separators_desc,
                listOf(
                    SettingsListEntry(
                        Res.string.display_separators_never,
                        DisplaySeparators.DS_NEVER,
                    ),
                    SettingsListEntry(
                        Res.string.display_separators_selected,
                        DisplaySeparators.DS_SELECTED,
                    ),
                    SettingsListEntry(
                        Res.string.display_separators_always,
                        DisplaySeparators.DS_ALWAYS,
                    ),
                ),
                settings.livePlayDisplaySeparators.stateInHere(
                    DisplaySeparators.DS_ALWAYS,
                ),
                settings::setPlayDisplaySeparators,
            ),
            SettingsBoolean(
                SettingsPage.DISPLAY,
                Res.string.infer_separators,
                Res.string.infer_separators_desc,
                settings.livePlayInferSeparators.stateInHereBool(),
                settings::setPlayInferSeparators,
            ),
            SettingsBoolean(
                SettingsPage.DISPLAY,
                Res.string.indicate_show_errors,
                Res.string.indicate_show_errors_desc,
                settings.livePlayIndicateShowErrors.stateInHereBool(),
                settings::setPlayIndicateShowErrors,
            ),
            SettingsBoolean(
                SettingsPage.DISPLAY,
                Res.string.show_timer,
                Res.string.show_timer_desc,
                settings.livePlayShowTimer.stateInHereBool(),
                settings::setPlayShowTimer,
            ),
            SettingsBoolean(
                SettingsPage.DISPLAY,
                Res.string.full_screen,
                Res.string.full_screen_desc,
                settings.livePlayFullScreen.stateInHereBool(),
                settings::setPlayFullScreen,
            ),
            SettingsList<Orientation>(
                SettingsPage.DISPLAY,
                Res.string.orientation_lock,
                Res.string.orientation_lock_desc,
                listOf(
                    SettingsListEntry(
                        Res.string.orientation_lock_unlocked,
                        Orientation.O_UNLOCKED,
                    ),
                    SettingsListEntry(
                        Res.string.orientation_lock_portrait,
                        Orientation.O_PORTRAIT,
                    ),
                    SettingsListEntry(
                        Res.string.orientation_lock_landscape,
                        Orientation.O_LANDSCAPE,
                    ),
                ),
                settings.liveAppOrientationLock.stateInHere(
                    Orientation.O_UNLOCKED,
                ),
                settings::setAppOrientationLock,
            ),
            SettingsList<GridRatio>(
                SettingsPage.DISPLAY,
                Res.string.grid_ratio_portrait,
                Res.string.grid_ratio_portrait_desc,
                listOf(
                    SettingsListEntry(
                        Res.string.grid_ratio_puzzle,
                        GridRatio.GR_PUZZLE_SHAPE,
                    ),
                    SettingsListEntry(
                        Res.string.grid_ratio_one_to_one,
                        GridRatio.GR_ONE_TO_ONE,
                    ),
                    SettingsListEntry(
                        Res.string.grid_ratio_thirty_pcnt,
                        GridRatio.GR_THIRTY_PCNT,
                    ),
                    SettingsListEntry(
                        Res.string.grid_ratio_forty_pcnt,
                        GridRatio.GR_FORTY_PCNT,
                    ),
                    SettingsListEntry(
                        Res.string.grid_ratio_fifty_pcnt,
                        GridRatio.GR_FIFTY_PCNT,
                    ),
                    SettingsListEntry(
                        Res.string.grid_ratio_sixty_pcnt,
                        GridRatio.GR_SIXTY_PCNT,
                    ),
                ),
                settings.livePlayGridRatioPortrait.stateInHere(
                    GridRatio.GR_PUZZLE_SHAPE,
                ),
                settings::setPlayGridRatioPortrait,
            ),
            SettingsList<GridRatio>(
                SettingsPage.DISPLAY,
                Res.string.grid_ratio_landscape,
                Res.string.grid_ratio_landscape_desc,
                listOf(
                    SettingsListEntry(
                        Res.string.grid_ratio_puzzle,
                        GridRatio.GR_PUZZLE_SHAPE,
                    ),
                    SettingsListEntry(
                        Res.string.grid_ratio_one_to_one,
                        GridRatio.GR_ONE_TO_ONE,
                    ),
                    SettingsListEntry(
                        Res.string.grid_ratio_thirty_pcnt,
                        GridRatio.GR_THIRTY_PCNT,
                    ),
                    SettingsListEntry(
                        Res.string.grid_ratio_forty_pcnt,
                        GridRatio.GR_FORTY_PCNT,
                    ),
                    SettingsListEntry(
                        Res.string.grid_ratio_fifty_pcnt,
                        GridRatio.GR_FIFTY_PCNT,
                    ),
                    SettingsListEntry(
                        Res.string.grid_ratio_sixty_pcnt,
                        GridRatio.GR_SIXTY_PCNT,
                    ),
                ),
                settings.livePlayGridRatioLandscape.stateInHere(
                    GridRatio.GR_PUZZLE_SHAPE,
                ),
                settings::setPlayGridRatioLandscape,
            ),
            SettingsList<ClueTabsDouble>(
                SettingsPage.DISPLAY,
                Res.string.clue_tabs_double,
                Res.string.clue_tabs_double_desc,
                listOf(
                    SettingsListEntry(
                        Res.string.clue_tabs_double_never,
                        ClueTabsDouble.CTD_NEVER,
                    ),
                    SettingsListEntry(
                        Res.string.clue_tabs_double_landscape,
                        ClueTabsDouble.CTD_LANDSCAPE,
                    ),
                    SettingsListEntry(
                        Res.string.clue_tabs_double_wide,
                        ClueTabsDouble.CTD_WIDE,
                    ),
                    SettingsListEntry(
                        Res.string.clue_tabs_double_always,
                        ClueTabsDouble.CTD_ALWAYS,
                    ),
                ),
                settings.liveClueListClueTabsDouble.stateInHere(
                    ClueTabsDouble.CTD_NEVER,
                ),
                settings::setClueListClueTabsDouble,
            ),
            SettingsList<ClueHighlight>(
                SettingsPage.DISPLAY,
                Res.string.clue_highlight,
                Res.string.clue_highlight_desc,
                listOf(
                    SettingsListEntry(
                        Res.string.clue_highlight_none,
                        ClueHighlight.CH_NONE,
                    ),
                    SettingsListEntry(
                        Res.string.clue_highlight_radio_button,
                        ClueHighlight.CH_RADIO_BUTTON,
                    ),
                    SettingsListEntry(
                        Res.string.clue_highlight_background,
                        ClueHighlight.CH_BACKGROUND,
                    ),
                    SettingsListEntry(
                        Res.string.clue_highlight_both,
                        ClueHighlight.CH_BOTH,
                    ),
                ),
                settings.livePlayClueHighlight.stateInHere(
                    ClueHighlight.CH_RADIO_BUTTON,
                ),
                settings::setPlayClueHighlight,
            ),
            SettingsBoolean(
                SettingsPage.DISPLAY,
                Res.string.highlight_crossing_clue,
                Res.string.highlight_crossing_clue_desc,
                settings.liveClueListHighlightCrossingClue.stateInHereBool(),
                settings::setClueListHighlightCrossingClue,
            ),

            // Action Buttons
            SettingsList<ActionButtonsMode>(
                SettingsPage.ACTION_BUTTONS,
                Res.string.action_buttons_mode,
                Res.string.action_buttons_mode_desc,
                listOf(
                    SettingsListEntry(
                        Res.string.action_buttons_mode_bottom,
                        ActionButtonsMode.ABM_BOTTOM,
                    ),
                    SettingsListEntry(
                        Res.string.action_buttons_mode_top_always,
                        ActionButtonsMode.ABM_TOP_ALWAYS,
                    ),
                    SettingsListEntry(
                        Res.string.action_buttons_mode_top_wide,
                        ActionButtonsMode.ABM_TOP_WIDE,
                    ),
                ),
                settings.liveActionButtonsMode.stateInHere(
                    ActionButtonsMode.ABM_BOTTOM,
                ),
                settings::setActionButtonsMode,
            ),
            SettingsBoolean(
                SettingsPage.ACTION_BUTTONS,
                Res.string.announce_clue_button,
                Res.string.announce_clue_button_desc,
                settings.liveActionButtonAnnounceClue.stateInHereBool(),
                settings::setActionButtonAnnounceClue,
            ),
            SettingsBoolean(
                SettingsPage.ACTION_BUTTONS,
                Res.string.action_button_toggle_scratch_mode,
                Res.string.action_button_toggle_scratch_mode_desc,
                settings.liveActionButtonToggleScratchMode.stateInHereBool(),
                settings::setActionButtonToggleScratchMode,
            ),
            SettingsBoolean(
                SettingsPage.ACTION_BUTTONS,
                Res.string.action_button_toggle_show_errors_grid,
                Res.string.action_button_toggle_show_errors_grid_desc,
                settings.liveActionButtonToggleShowErrorsGrid.stateInHereBool(),
                settings::setActionButtonToggleShowErrorsGrid,
            ),
            SettingsBoolean(
                SettingsPage.ACTION_BUTTONS,
                Res.string.voice_with_button,
                Res.string.voice_with_button_desc,
                settings.liveActionButtonVoiceInput.stateInHereBool(),
                settings::setActionButtonVoiceInput,
            ),

            // Keyboard
            SettingsList<KeyboardMode>(
                SettingsPage.KEYBOARD,
                Res.string.keyboard_showhide,
                Res.string.keyboard_showhide_desc,
                listOf(
                    SettingsListEntry(
                        Res.string.keyboard_always_show,
                        KeyboardMode.KM_ALWAYS_SHOW,
                    ),
                    SettingsListEntry(
                        Res.string.keyboard_hide_manual,
                        KeyboardMode.KM_HIDE_MANUAL,
                    ),
                    SettingsListEntry(
                        Res.string.keyboard_show_sparingly_desc,
                        KeyboardMode.KM_SHOW_SPARINGLY,
                    ),
                    SettingsListEntry(
                        Res.string.keyboard_never_show,
                        KeyboardMode.KM_NEVER_SHOW,
                    ),
                ),
                settings.liveKeyboardMode.stateInHere(
                    KeyboardMode.KM_ALWAYS_SHOW,
                ),
                settings::setKeyboardMode,
            ),
            SettingsBoolean(
                SettingsPage.KEYBOARD,
                Res.string.keyboard_use_native,
                Res.string.keyboard_use_native_desc,
                settings.liveKeyboardUseNative.stateInHereBool(),
                settings::setKeyboardUseNative,
            ),
            SettingsHeading(
                SettingsPage.KEYBOARD,
                Res.string.keyboard_built_in,
            ),
            SettingsList<KeyboardLayout>(
                SettingsPage.KEYBOARD,
                Res.string.keyboard_layout,
                Res.string.keyboard_layout_desc,
                listOf(
                    SettingsListEntry(
                        Res.string.keyboard_qwerty,
                        KeyboardLayout.KL_QWERTY,
                    ),
                    SettingsListEntry(
                        Res.string.keyboard_qwertz,
                        KeyboardLayout.KL_QWERTZ,
                    ),
                    SettingsListEntry(
                        Res.string.keyboard_azerty,
                        KeyboardLayout.KL_AZERTY,
                    ),
                    SettingsListEntry(
                        Res.string.keyboard_dvorak,
                        KeyboardLayout.KL_DVORAK,
                    ),
                    SettingsListEntry(
                        Res.string.keyboard_colemak,
                        KeyboardLayout.KL_COLEMAK,
                    ),
                    SettingsListEntry(
                        Res.string.keyboard_russian,
                        KeyboardLayout.KL_RUSSIAN,
                    ),
                ),
                settings.liveKeyboardLayout.stateInHere(
                    KeyboardLayout.KL_QWERTY,
                ),
                settings::setKeyboardLayout,
            ),
            SettingsBoolean(
                SettingsPage.KEYBOARD,
                Res.string.keyboard_hide_button,
                Res.string.keyboard_hide_button_desc,
                settings.liveKeyboardHideButton.stateInHereBool(),
                settings::setKeyboardHideButton,
            ),
            SettingsBoolean(
                SettingsPage.KEYBOARD,
                Res.string.keyboard_compact,
                Res.string.keyboard_compact_desc,
                settings.liveKeyboardCompact.stateInHereBool(),
                settings::setKeyboardCompact,
            ),
            SettingsBoolean(
                SettingsPage.KEYBOARD,
                Res.string.keyboard_haptic,
                Res.string.keyboard_haptic_desc,
                settings.liveKeyboardHaptic.stateInHereBool(),
                settings::setKeyboardHaptic,
            ),
            SettingsInteger(
                SettingsPage.KEYBOARD,
                Res.string.keyboard_repeat_delay,
                Res.string.keyboard_repeat_delay_desc,
                Res.string.keyboard_repeat_delay_hint,
                settings.liveKeyboardRepeatDelay.stateInHereInt(),
                settings::setKeyboardRepeatDelay,
            ),
            SettingsInteger(
                SettingsPage.KEYBOARD,
                Res.string.keyboard_repeat_interval,
                Res.string.keyboard_repeat_interval_desc,
                Res.string.keyboard_repeat_interval_hint,
                settings.liveKeyboardRepeatInterval.stateInHereInt(),
                settings::setKeyboardRepeatInterval,
            ),
            SettingsHeading(
                SettingsPage.KEYBOARD,
                Res.string.keyboard_native,
            ),
            SettingsBoolean(
                SettingsPage.KEYBOARD,
                Res.string.keyboard_force_caps,
                Res.string.keyboard_force_caps_desc,
                settings.liveKeyboardForceCaps.stateInHereBool(),
                settings::setKeyboardForceCaps,
            ),
            SettingsBoolean(
                SettingsPage.KEYBOARD,
                Res.string.keyboard_split_words,
                Res.string.keyboard_split_words_desc,
                settings.liveKeyboardSplitWords.stateInHereBool(),
                settings::setKeyboardSplitWords,
            ),
            SettingsHeading(
                SettingsPage.KEYBOARD,
                Res.string.hardware_keyboard,
            ),
            SettingsSubPage(
                SettingsPage.KEYBOARD,
                Res.string.keyboard_bindings_desc,
                SettingsPage.KEYBOARD_BINDINGS,
            ),

            // Keyboard Bindings
            SettingsHTMLPage(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_bindings_help,
                Res.string.keyboard_bindings_help_desc,
                HTMLFile.KEYBOARD_BINDINGS_HELP,
            ),
            SettingsDivider(SettingsPage.KEYBOARD_BINDINGS),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_announce_box,
                Res.string.keyboard_binding_announce_box_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingAnnounceBox.stateInHereString(),
                settings::setKeyboardBindingAnnounceBox,
                settings.liveKeyboardBindingAnnounceBox.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_announce_clue,
                Res.string.keyboard_binding_announce_clue_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingAnnounceClue.stateInHereString(),
                settings::setKeyboardBindingAnnounceClue,
                settings.liveKeyboardBindingAnnounceClue.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_back,
                Res.string.keyboard_binding_back_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingBack.stateInHereString(),
                settings::setKeyboardBindingBack,
                settings.liveKeyboardBindingBack.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_clue_list,
                Res.string.keyboard_binding_clue_list_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingClueList.stateInHereString(),
                settings::setKeyboardBindingClueList,
                settings.liveKeyboardBindingClueList.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_clue_notes,
                Res.string.keyboard_binding_clue_notes_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingClueNotes.stateInHereString(),
                settings::setKeyboardBindingClueNotes,
                settings.liveKeyboardBindingClueNotes.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_copy_clue,
                Res.string.keyboard_binding_copy_clue_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingCopyClue.stateInHereString(),
                settings::setKeyboardBindingCopyClue,
                settings.liveKeyboardBindingCopyClue.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_exit,
                Res.string.keyboard_binding_exit_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingExit.stateInHereString(),
                settings::setKeyboardBindingExit,
                settings.liveKeyboardBindingExit.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_fit_to_screen,
                Res.string.keyboard_binding_fit_to_screen_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingFitToScreen.stateInHereString(),
                settings::setKeyboardBindingFitToScreen,
                settings.liveKeyboardBindingFitToScreen.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_help,
                Res.string.keyboard_binding_help_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingHelp.stateInHereString(),
                settings::setKeyboardBindingHelp,
                settings.liveKeyboardBindingHelp.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_insert_special,
                Res.string.keyboard_binding_insert_special_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingInsertSpecial.stateInHereString(),
                settings::setKeyboardBindingInsertSpecial,
                settings.liveKeyboardBindingInsertSpecial.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_next_clue,
                Res.string.keyboard_binding_next_clue_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingNextClue.stateInHereString(),
                settings::setKeyboardBindingNextClue,
                settings.liveKeyboardBindingNextClue.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_open_crossword_solver,
                Res.string.keyboard_binding_open_crossword_solver_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingOpenCrosswordSolver.stateInHereString(),
                settings::setKeyboardBindingOpenCrosswordSolver,
                settings.liveKeyboardBindingOpenCrosswordSolver.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_open_dictionary,
                Res.string.keyboard_binding_open_dictionary_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingOpenDictionary.stateInHereString(),
                settings::setKeyboardBindingOpenDictionary,
                settings.liveKeyboardBindingOpenDictionary.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_open_info,
                Res.string.keyboard_binding_open_info_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingOpenInfo.stateInHereString(),
                settings::setKeyboardBindingOpenInfo,
                settings.liveKeyboardBindingOpenInfo.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_open_share_url,
                Res.string.keyboard_binding_open_share_url_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingOpenShareUrl.stateInHereString(),
                settings::setKeyboardBindingOpenShareUrl,
                settings.liveKeyboardBindingOpenShareUrl.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_prev_clue,
                Res.string.keyboard_binding_prev_clue_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingPrevClue.stateInHereString(),
                settings::setKeyboardBindingPrevClue,
                settings.liveKeyboardBindingPrevClue.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_puzzle_notes,
                Res.string.keyboard_binding_puzzle_notes_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingPuzzleNotes.stateInHereString(),
                settings::setKeyboardBindingPuzzleNotes,
                settings.liveKeyboardBindingPuzzleNotes.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_random_clue,
                Res.string.keyboard_binding_random_clue_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingRandomClue.stateInHereString(),
                settings::setKeyboardBindingRandomClue,
                settings.liveKeyboardBindingRandomClue.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_reveal_errors,
                Res.string.keyboard_binding_reveal_errors_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingRevealErrors.stateInHereString(),
                settings::setKeyboardBindingRevealErrors,
                settings.liveKeyboardBindingRevealErrors.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_reveal_letter,
                Res.string.keyboard_binding_reveal_letter_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingRevealLetter.stateInHereString(),
                settings::setKeyboardBindingRevealLetter,
                settings.liveKeyboardBindingRevealLetter.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_reveal_puzzle,
                Res.string.keyboard_binding_reveal_puzzle_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingRevealPuzzle.stateInHereString(),
                settings::setKeyboardBindingRevealPuzzle,
                settings.liveKeyboardBindingRevealPuzzle.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_reveal_word,
                Res.string.keyboard_binding_reveal_word_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingRevealWord.stateInHereString(),
                settings::setKeyboardBindingRevealWord,
                settings.liveKeyboardBindingRevealWord.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_search_duckduckgo,
                Res.string.keyboard_binding_search_duckduckgo_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingSearchDuckduckgo.stateInHereString(),
                settings::setKeyboardBindingSearchDuckduckgo,
                settings.liveKeyboardBindingSearchDuckduckgo.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_search_fifteen_squared,
                Res.string.keyboard_binding_search_fifteen_squared_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingSearchFifteenSquared.stateInHereString(),
                settings::setKeyboardBindingSearchFifteenSquared,
                settings.liveKeyboardBindingSearchFifteenSquared.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_settings,
                Res.string.keyboard_binding_settings_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingSettings.stateInHereString(),
                settings::setKeyboardBindingSettings,
                settings.liveKeyboardBindingSettings.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_share_clue,
                Res.string.keyboard_binding_share_clue_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingShareClue.stateInHereString(),
                settings::setKeyboardBindingShareClue,
                settings.liveKeyboardBindingShareClue.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_share_clue_with_board,
                Res.string.keyboard_binding_share_clue_with_board_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingShareClueWithBoard.stateInHereString(),
                settings::setKeyboardBindingShareClueWithBoard,
                settings.liveKeyboardBindingShareClueWithBoard.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_share_puzzle,
                Res.string.keyboard_binding_share_puzzle_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingSharePuzzle.stateInHereString(),
                settings::setKeyboardBindingSharePuzzle,
                settings.liveKeyboardBindingSharePuzzle.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_share_puzzle_blank,
                Res.string.keyboard_binding_share_puzzle_blank_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingSharePuzzleBlank.stateInHereString(),
                settings::setKeyboardBindingSharePuzzleBlank,
                settings.liveKeyboardBindingSharePuzzleBlank.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_share_puzzle_no_ipuz,
                Res.string.keyboard_binding_share_puzzle_no_ipuz_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingSharePuzzleNoIpuz.stateInHereString(),
                settings::setKeyboardBindingSharePuzzleNoIpuz,
                settings.liveKeyboardBindingSharePuzzleNoIpuz.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_share_puzzle_original,
                Res.string.keyboard_binding_share_puzzle_original_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingSharePuzzleOriginal.stateInHereString(),
                settings::setKeyboardBindingSharePuzzleOriginal,
                settings.liveKeyboardBindingSharePuzzleOriginal.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_shuffle_anagram,
                Res.string.keyboard_binding_shuffle_anagram_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingShuffleAnagram.stateInHereString(),
                settings::setKeyboardBindingShuffleAnagram,
                settings.liveKeyboardBindingShuffleAnagram.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_toggle_cell_flag,
                Res.string.keyboard_binding_toggle_cell_flag_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingToggleCellFlag.stateInHereString(),
                settings::setKeyboardBindingToggleCellFlag,
                settings.liveKeyboardBindingToggleCellFlag.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_toggle_clue_flag,
                Res.string.keyboard_binding_toggle_clue_flag_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingToggleClueFlag.stateInHereString(),
                settings::setKeyboardBindingToggleClueFlag,
                settings.liveKeyboardBindingToggleClueFlag.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_toggle_scratch_mode,
                Res.string.keyboard_binding_toggle_scratch_mode_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingToggleScratchMode.stateInHereString(),
                settings::setKeyboardBindingToggleScratchMode,
                settings.liveKeyboardBindingToggleScratchMode.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_toggle_selection,
                Res.string.keyboard_binding_toggle_selection_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingToggleSelection.stateInHereString(),
                settings::setKeyboardBindingToggleSelection,
                settings.liveKeyboardBindingToggleSelection.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_toggle_show_errors_cursor,
                Res.string.keyboard_binding_toggle_show_errors_cursor_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingToggleShowErrorsCursor.stateInHereString(),
                settings::setKeyboardBindingToggleShowErrorsCursor,
                settings.liveKeyboardBindingToggleShowErrorsCursor.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_toggle_show_errors_grid,
                Res.string.keyboard_binding_toggle_show_errors_grid_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingToggleShowErrorsGrid.stateInHereString(),
                settings::setKeyboardBindingToggleShowErrorsGrid,
                settings.liveKeyboardBindingToggleShowErrorsGrid.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_toggle_show_errors_word_cells,
                Res.string.keyboard_binding_toggle_show_errors_word_cells_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingToggleShowErrorsWordCells.stateInHereString(),
                settings::setKeyboardBindingToggleShowErrorsWordCells,
                settings.liveKeyboardBindingToggleShowErrorsWordCells.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_toggle_show_errors_word_word,
                Res.string.keyboard_binding_toggle_show_errors_word_word_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingToggleShowErrorsWordWord.stateInHereString(),
                settings::setKeyboardBindingToggleShowErrorsWordWord,
                settings.liveKeyboardBindingToggleShowErrorsWordWord.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_voice_input,
                Res.string.keyboard_binding_voice_input_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingVoiceInput.stateInHereString(),
                settings::setKeyboardBindingVoiceInput,
                settings.liveKeyboardBindingVoiceInput.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_zoom_in,
                Res.string.keyboard_binding_zoom_in_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingZoomIn.stateInHereString(),
                settings::setKeyboardBindingZoomIn,
                settings.liveKeyboardBindingZoomIn.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_zoom_in_max,
                Res.string.keyboard_binding_zoom_in_max_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingZoomInMax.stateInHereString(),
                settings::setKeyboardBindingZoomInMax,
                settings.liveKeyboardBindingZoomInMax.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_zoom_out,
                Res.string.keyboard_binding_zoom_out_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingZoomOut.stateInHereString(),
                settings::setKeyboardBindingZoomOut,
                settings.liveKeyboardBindingZoomOut.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
            SettingsString(
                SettingsPage.KEYBOARD_BINDINGS,
                Res.string.keyboard_binding_zoom_reset,
                Res.string.keyboard_binding_zoom_reset_desc,
                Res.string.keyboard_binding_hint,
                settings.liveKeyboardBindingZoomReset.stateInHereString(),
                settings::setKeyboardBindingZoomReset,
                settings.liveKeyboardBindingZoomReset.map(
                    this::getKeyboardBindingErrorMessage,
                ).stateInSubscribed(viewModelScope, null),
            ),
                        // Interaction
                        SettingsBoolean(
                            SettingsPage.INTERACTION,
                            Res.string.snap_current,
                            Res.string.snap_current_desc,
                            settings.livePlayEnsureVisible.stateInHereBool(),
                            settings::setPlayEnsureVisible,
                        ),
                        SettingsList<SnapClueMode>(
                            SettingsPage.INTERACTION,
                            Res.string.scroll_to_clue,
                            Res.string.scroll_to_clue_desc,
                            listOf(
                                SettingsListEntry(
                                    Res.string.scroll_to_clue_off,
                                    SnapClueMode.SCM_OFF,
                                ),
                                SettingsListEntry(
                                    Res.string.scroll_to_clue_current,
                                    SnapClueMode.SCM_CLUE,
                                ),
                                SettingsListEntry(
                                    Res.string.scroll_to_clue_crossing,
                                    SnapClueMode.SCM_CROSSING,
                                ),
                            ),
                            settings.liveClueListSnapClueMode.stateInHere(
                                SnapClueMode.SCM_OFF,
                            ),
                            settings::setClueListSnapClueMode,
                        ),
                        SettingsList<MovementStrategySetting>(
                            SettingsPage.INTERACTION,
                            Res.string.movement_style,
                            Res.string.movement_style_desc,
                            listOf(
                                SettingsListEntry(
                                    Res.string.movement_strategy_next_word,
                                    MovementStrategySetting.MSS_MOVE_NEXT_ON_AXIS,
                                ),
                                SettingsListEntry(
                                    Res.string.movement_strategy_stop_end,
                                    MovementStrategySetting.MSS_STOP_ON_END,
                                ),
                                SettingsListEntry(
                                    Res.string.movement_strategy_next_clue,
                                    MovementStrategySetting.MSS_MOVE_NEXT_CLUE,
                                ),
                                SettingsListEntry(
                                    Res.string.movement_strategy_next_parallel_clue,
                                    MovementStrategySetting.MSS_MOVE_PARALLEL_WORD,
                                ),
                            ),
                            settings.livePlayMovementStrategySetting.stateInHere(
                                MovementStrategySetting.MSS_MOVE_NEXT_ON_AXIS,
                            ),
                            settings::setPlayMovementStrategySetting,
                        ),
                        SettingsList<MovementStrategySetting>(
                            SettingsPage.INTERACTION,
                            Res.string.movement_style_clue_list,
                            Res.string.movement_style_clue_list_desc,
                            listOf(
                                SettingsListEntry(
                                    Res.string.movement_strategy_stop_end,
                                    MovementStrategySetting.MSS_STOP_ON_END,
                                ),
                                SettingsListEntry(
                                    Res.string.movement_strategy_next_clue,
                                    MovementStrategySetting.MSS_MOVE_NEXT_CLUE,
                                ),
                            ),
                            settings.livePlayMovementStrategyClueListSetting.stateInHere(
                                MovementStrategySetting.MSS_MOVE_NEXT_CLUE,
                            ),
                            settings::setPlayMovementStrategyClueListSetting,
                        ),
                        SettingsBoolean(
                            SettingsPage.INTERACTION,
                            Res.string.skip_filled,
                            Res.string.skip_filled_desc,
                            settings.livePlaySkipFilled.stateInHereBool(),
                            settings::setPlaySkipFilled,
                        ),
                        SettingsList<CycleUnfilledMode>(
                            SettingsPage.INTERACTION,
                            Res.string.cycle_unfilled,
                            Res.string.cycle_unfilled_desc,
                            listOf(
                                SettingsListEntry(
                                    Res.string.cycle_unfilled_never,
                                    CycleUnfilledMode.CU_NEVER,
                                ),
                                SettingsListEntry(
                                    Res.string.cycle_unfilled_forwards,
                                    CycleUnfilledMode.CU_FORWARDS,
                                ),
                                SettingsListEntry(
                                    Res.string.cycle_unfilled_always,
                                    CycleUnfilledMode.CU_ALWAYS,
                                ),
                            ),
                            settings.livePlayCycleUnfilledMode.stateInHere(
                                CycleUnfilledMode.CU_NEVER,
                            ),
                            settings::setPlayCycleUnfilledMode,
                        ),
                        SettingsBoolean(
                            SettingsPage.INTERACTION,
                            Res.string.double_tap_zoom,
                            Res.string.double_tap_zoom_desc,
                            settings.livePlayDoubleTapFitBoard.stateInHereBool(),
                            settings::setPlayDoubleTapFitBoard,
                        ),
                        SettingsBoolean(
                            SettingsPage.INTERACTION,
                            Res.string.dont_snap_board_to_borders,
                            Res.string.dont_snap_board_to_borders_desc,
                            settings.livePlayDontSnapBoardToBorders.stateInHereBool(),
                            settings::setPlayDontSnapBoardToBorders,
                        ),
                        SettingsList<DeleteCrossingModeSetting>(
                            SettingsPage.INTERACTION,
                            Res.string.dont_delete_crossing,
                            Res.string.dont_delete_crossing_desc,
                            listOf(
                                SettingsListEntry(
                                    Res.string.delete_crossing_delete,
                                    DeleteCrossingModeSetting.DCMS_DELETE,
                                ),
                                SettingsListEntry(
                                    Res.string.delete_crossing_preserve_filled_words,
                                    DeleteCrossingModeSetting.DCMS_PRESERVE_FILLED_WORDS,
                                ),
                                SettingsListEntry(
                                    Res.string.delete_crossing_preserve_filled_cells,
                                    DeleteCrossingModeSetting.DCMS_PRESERVE_FILLED_CELLS,
                                ),
                            ),
                            settings.livePlayDeleteCrossingModeSetting.stateInHere(
                                DeleteCrossingModeSetting.DCMS_DELETE,
                            ),
                            settings::setPlayDeleteCrossingModeSetting,
                        ),
                        SettingsBoolean(
                            SettingsPage.INTERACTION,
                            Res.string.preserve_correct,
                            Res.string.preserve_correct_desc,
                            settings.livePlayPreserveCorrectLettersInShowErrors
                                .stateInHereBool(),
                            settings::setPlayPreserveCorrectLettersInShowErrors,
                        ),
                        SettingsBoolean(
                            SettingsPage.INTERACTION,
                            Res.string.play_letter_undo,
                            Res.string.play_letter_undo_desc,
                            settings.livePlayPlayLetterUndoEnabled.stateInHereBool(),
                            settings::setPlayPlayLetterUndoEnabled,
                        ),
                        SettingsBoolean(
                            SettingsPage.INTERACTION,
                            Res.string.space_change_dir,
                            Res.string.space_change_dir_desc,
                            settings.livePlaySpaceChangesDirection.stateInHereBool(),
                            settings::setPlaySpaceChangesDirection,
                        ),
                        SettingsBoolean(
                            SettingsPage.INTERACTION,
                            Res.string.enter_changes_direction,
                            Res.string.enter_changes_direction_desc,
                            settings.livePlayEnterChangesDirection.stateInHereBool(),
                            settings::setPlayEnterChangesDirection,
                        ),
                        SettingsBoolean(
                            SettingsPage.INTERACTION,
                            Res.string.toggle_before_move,
                            Res.string.toggle_before_move_desc,
                            settings.livePlayToggleBeforeMove.stateInHereBool(),
                            settings::setPlayToggleBeforeMove,
                        ),
                        SettingsBoolean(
                            SettingsPage.INTERACTION,
                            Res.string.random_clue_on_shake,
                            Res.string.random_clue_on_shake_desc,
                            settings.livePlayRandomClueOnShake.stateInHereBool(),
                            settings::setPlayRandomClueOnShake,
                        ),
                        SettingsBoolean(
                            SettingsPage.INTERACTION,
                            Res.string.predict_anagram_chars,
                            Res.string.predict_anagram_chars_desc,
                            settings.livePlayPredictAnagramChars.stateInHereBool(),
                            settings::setPlayPredictAnagramChars,
                        ),
                        SettingsBoolean(
                            SettingsPage.INTERACTION,
                            Res.string.disable_ratings,
                            Res.string.disable_ratings_desc,
                            settings.liveRatingsDisableRatings.stateInHereBool(),
                            settings::setRatingsDisableRatings,
                        ),

            // Voice & Accessibility
            SettingsBoolean(
                SettingsPage.VOICE_ACCESSIBILITY,
                Res.string.voice_with_volume,
                Res.string.voice_with_volume_desc,
                settings.liveVoiceVolumeActivatesVoice.stateInHereBool(),
                settings::setVoiceVolumeActivatesVoice,
            ),
            SettingsBoolean(
                SettingsPage.VOICE_ACCESSIBILITY,
                Res.string.voice_with_button,
                Res.string.voice_with_button_desc,
                settings.liveActionButtonVoiceInput.stateInHereBool(),
                settings::setActionButtonVoiceInput,
            ),
            SettingsBoolean(
                SettingsPage.VOICE_ACCESSIBILITY,
                Res.string.llm_voice_commands,
                Res.string.llm_voice_commands_desc,
                settings.liveVoiceLLMVoiceCommands.stateInHereBool(),
                settings::setVoiceLLMVoiceCommands,
            ),
            SettingsBoolean(
                SettingsPage.VOICE_ACCESSIBILITY,
                Res.string.announce_clue_button,
                Res.string.announce_clue_button_desc,
                settings.liveActionButtonAnnounceClue.stateInHereBool(),
                settings::setActionButtonAnnounceClue,
            ),
            SettingsBoolean(
                SettingsPage.VOICE_ACCESSIBILITY,
                Res.string.announce_clue_equals,
                Res.string.announce_clue_equals_desc,
                settings.liveVoiceEqualsAnnounceClue.stateInHereBool(),
                settings::setVoiceEqualsAnnounceClue,
            ),
            SettingsBoolean(
                SettingsPage.VOICE_ACCESSIBILITY,
                Res.string.announce_clue_always,
                Res.string.announce_clue_always_desc,
                settings.liveVoiceAlwaysAnnounceClue.stateInHereBool(),
                settings::setVoiceAlwaysAnnounceClue,
            ),
            SettingsBoolean(
                SettingsPage.VOICE_ACCESSIBILITY,
                Res.string.announce_box_always,
                Res.string.announce_box_always_desc,
                settings.liveVoiceAlwaysAnnounceBox.stateInHereBool(),
                settings::setVoiceAlwaysAnnounceBox,
            ),
            SettingsDivider(SettingsPage.VOICE_ACCESSIBILITY),
            SettingsHTMLPage(
                SettingsPage.VOICE_ACCESSIBILITY,
                Res.string.about_voice_commands,
                HTMLFile.VOICE_COMMANDS_HELP,
            ),

            // External Tools
            SettingsList<ExternalDictionarySetting>(
                SettingsPage.EXTERNAL_TOOLS,
                Res.string.external_dictionary,
                Res.string.external_dictionary_desc,
                listOf(
                    SettingsListEntry(
                        Res.string.dictionary_none,
                        ExternalDictionarySetting.EDS_NONE,
                    ),
                    SettingsListEntry(
                        Res.string.free_dictionary,
                        ExternalDictionarySetting.EDS_FREE,
                    ),
                    SettingsListEntry(
                        Res.string.quick_dic,
                        ExternalDictionarySetting.EDS_QUICK,
                    ),
                    SettingsListEntry(
                        Res.string.aard2,
                        ExternalDictionarySetting.EDS_AARD2,
                    ),
                ),
                settings.liveExtDictionarySetting.stateInHere(
                    ExternalDictionarySetting.EDS_NONE,
                ),
                settings::setExtDictionarySetting,
            ),
            SettingsBoolean(
                SettingsPage.EXTERNAL_TOOLS,
                Res.string.crossword_solver_enabled,
                Res.string.crossword_solver_enabled_desc,
                settings.liveExtCrosswordSolverEnabled.stateInHereBool(),
                settings::setExtCrosswordSolverEnabled,
            ),
            SettingsList<LLMAgent>(
                SettingsPage.EXTERNAL_TOOLS,
                Res.string.llm_agent,
                Res.string.llm_agent_desc,
                listOf(
                    SettingsListEntry(Res.string.llm_agent_none, LLMAgent.LA_NONE),
                    SettingsListEntry(
                        Res.string.llm_agent_chat_gpt,
                        LLMAgent.LA_CHATGPT,
                    ),
                    SettingsListEntry(
                        Res.string.llm_agent_claude,
                        LLMAgent.LA_CLAUDE,
                    ),
                ),
                settings.liveExtLLMAgent.stateInHere(LLMAgent.LA_NONE),
                settings::setExtLLMAgent,
            ),
            SettingsString(
                SettingsPage.EXTERNAL_TOOLS,
                Res.string.llm_agent_api_key,
                Res.string.llm_agent_api_key_desc,
                Res.string.llm_agent_api_key,
                settings.liveExtLLMAgentAPIKey.stateInHereString(),
                settings::setExtLLMAgentAPIKey,
                dialogMsg = Res.string.llm_agent_api_key_message,
            ),
            SettingsBoolean(
                SettingsPage.EXTERNAL_TOOLS,
                Res.string.duckduckgo,
                Res.string.duckduckgo_desc,
                settings.liveExtDuckDuckGoEnabled.stateInHereBool(),
                settings::setExtDuckDuckGoEnabled,
            ),
            SettingsBoolean(
                SettingsPage.EXTERNAL_TOOLS,
                Res.string.fifteen_squared,
                Res.string.fifteen_squared_desc,
                settings.liveExtFifteenSquaredEnabled.stateInHereBool(),
                settings::setExtFifteenSquaredEnabled,
            ),
            SettingsString(
                SettingsPage.EXTERNAL_TOOLS,
                Res.string.mcp_host_port,
                Res.string.mcp_host_port_desc,
                Res.string.mcp_host_port_hint,
                settings.liveExtMCPHostPort.stateInHereString(),
                settings::setExtMCPHostPort,
            ),

            // Export/Import
            SettingsAction(
                SettingsPage.EXPORT_IMPORT,
                Res.string.preferences_export,
                Res.string.preferences_export_desc,
                this::startSettingsExport,
            ),
            SettingsAction(
                SettingsPage.EXPORT_IMPORT,
                Res.string.share_settings,
                Res.string.share_settings_desc,
                this::shareSettings,
            ),
            SettingsAction(
                SettingsPage.EXPORT_IMPORT,
                Res.string.preferences_import,
                Res.string.preferences_import_desc,
                this::startSettingsImport,
            ),
            SettingsAction(
                SettingsPage.EXPORT_IMPORT,
                Res.string.puzzles_export,
                Res.string.puzzles_export_desc,
                this::startPuzzlesExport,
            ),

            // Desktop
            SettingsBoolean(
                SettingsPage.DESKTOP,
                Res.string.undecorated,
                Res.string.undecorated_desc,
                settings.liveAppUndecorated.stateInHereBool(),
                settings::setAppUndecorated,
            ),
        )
    )

    private val rootState = settingsPageState(SettingsPage.ROOT, false)
    private val _state = MutableStateFlow<SettingsPageViewState>(rootState)
    private val stateBackStack = ArrayDeque<SettingsPageViewState>()
    // separate as should not appear on back stack
    private val _needsURI = MutableStateFlow<RequestedURI>(RequestedURI.NONE)
    private val _messages = MutableStateFlow<List<String>>(listOf())
    private val _shareSettingsEvent = MutableStateFlow<ShareSettingsData?>(null)

    val state : StateFlow<SettingsPageViewState> = _state
    val needsURI : StateFlow<RequestedURI> = _needsURI
    val messages : StateFlow<List<String>> = _messages
    val shareSettingsEvent : StateFlow<ShareSettingsData?> = _shareSettingsEvent

    fun startSearch() {
        if (!state.value.inSearchMode) {
            resetBackStack()
            viewModelScope.launch {
                val filter = stringFilter("")
                _state.value = state.value.copy(
                    currentPage = SettingsPage.SEARCH,
                    searchTerm = "",
                    hasBackStack = true,
                    settingsItems = settingsItemsList.filter { filter(it) },
                )
            }
        }
    }

    fun setSearchTerm(term : String) {
        if (state.value.inSearchMode) {
            // no back stack push for refining a search
            viewModelScope.launch {
                val filter = stringFilter(term)
                _state.value = state.value.copy(
                    searchTerm = term,
                    hasBackStack = true,
                    settingsItems = settingsItemsList.filter { filter(it) },
                )
            }
        }
    }

    fun endSearch() {
        if (state.value.inSearchMode)
            popState()
    }

    fun setCurrentPage(page : SettingsPage) {
        pushNewState(settingsPageState(page, true))
    }

    fun openInputSetting(item : SettingsInputItem) {
        pushNewState(state.value.copy(activeInputItem = item))
    }

    fun popState() {
        if (!stateBackStack.isEmpty()) {
            _state.value = stateBackStack.removeLast()
        }
    }

    /**
     * Call when all messages have been shown
     */
    fun clearMessages() {
        _messages.value = listOf()
    }

    /**
     * Set new URI return false if failed
     */
    fun setNewExternalStorageSAFURI(uri : Uri) : Boolean {
        val fileHandler = utils.initialiseSAFForRoot(uri)
        val handlerSettings = fileHandler?.settings
        if (fileHandler != null && handlerSettings != null) {
            settings.setFileHandlerSettings(
                handlerSettings,
                this::newFileHandlerAppExit,
            )
            return true
        } else {
            return false
        }
    }

    /**
     * Call when URI request services
     *
     * Stop anyone else asking
     */
    fun clearNeedsURI() {
        _needsURI.value = RequestedURI.NONE
    }

    fun exportSettings(uri : Uri, onDone : () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            fileActionMutex.withLock {
                try {
                    utils.openOutput(uri).use(settings::exportSettings)
                    showMessage(Res.string.preferences_exported)
                } catch (e : IOException) {
                    showMessage(Res.string.preferences_export_failed)
                } finally {
                    viewModelScope.launch(Dispatchers.Main) { onDone() }
                }
            }
        }
    }

    fun shareSettings() {
        FileHandlerShared.getSettingsShareUri(viewModelScope, utils, settings) {
            it?.let { uri ->
                val mimeType = FileHandlerShared.SETTINGS_SHARE_URI_MIME_TYPE
                _shareSettingsEvent.value = ShareSettingsData(mimeType, uri)
            }
        }
    }

    fun clearShareSettingsEvent() {
        _shareSettingsEvent.value = null
    }

    fun importSettings(uri : Uri, onDone : () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            fileActionMutex.withLock {
                try {
                    utils.openInput(uri).use(settings::importSettings)
                    showMessage(Res.string.preferences_imported)
                } catch (e : IOException) {
                    showMessage(Res.string.preferences_import_failed)
                } finally {
                    viewModelScope.launch(Dispatchers.Main) { onDone() }
                }
            }
        }
    }

    private fun showMessage(msg : StringResource) {
        viewModelScope.launch {
            _messages.value = messages.value + utils.getString(msg)
        }
    }

    private fun raiseNeedsURI(uriType : RequestedURI) {
        _needsURI.value = uriType
    }

    private fun pushNewState(newState : SettingsPageViewState) {
        stateBackStack.addLast(state.value)
        _state.value = newState
    }

    private fun resetBackStack() {
        stateBackStack.clear()
        stateBackStack.addLast(rootState)
    }

    private fun pageFilter(page : SettingsPage) : (SettingsItem) -> Boolean
        = { item : SettingsItem -> item.page == page }

    private fun stringFilter(s : String) : suspend (SettingsItem) -> Boolean {
        return { item : SettingsItem ->
            when (item) {
                is SettingsHeading -> false
                is SettingsNamedItem -> {
                    utils.getString(item.title).contains(s, ignoreCase = true)
                        || item.summary?.let {
                            utils.getString(it).contains(s, ignoreCase = true)
                        } ?: false
                }
                else -> false
            }
        }
    }

    private fun settingsPageState(
        page : SettingsPage,
        hasBackStack : Boolean,
    ) = SettingsPageViewState(
            currentPage = page,
            searchTerm = "",
            hasBackStack = hasBackStack,
            settingsItems = settingsItemsList.filter(
                pageFilter(page)
            ),
            activeInputItem = null,
            storageChangedAppExit = false,
        )

    /**
     * Called when the use selects a storage location
     *
     * If they selected external storage (Storage Access Framework),
     * then they may be prompted to select a directory. This happens if
     * they have not already set the directory up, they are
     * reselecting the same option, suggesting they want to change it,
     * or there is a problem with the current one.
     */
    private fun onStorageLocationChange(newLocation : StorageLocation) {
        settings.getFileHandlerSettings({ fileHandlerSettings ->
            val isSAF = StorageLocation.SL_EXTERNAL_SAF.equals(newLocation)

            if (isSAF) {
                if (newLocation == fileHandlerSettings.storageLocation) {
                    // reselected same, want to change
                    raiseNeedsURI(RequestedURI.SAF)
                } else {
                    // possibly returning to SAF, see if something
                    // viable is set up already
                    val fileHandler = utils.readSAFHandlerFromSettings(
                        fileHandlerSettings,
                        settings,
                    )

                    if (fileHandler == null) {
                        raiseNeedsURI(RequestedURI.SAF)
                    } else {
                        settings.setFileHandlerSettings(
                            fileHandler.settings,
                            this::newFileHandlerAppExit,
                        )
                    }
                }
            } else {
                // keep old URIs so they can be reselected in future
                settings.setFileHandlerSettings(
                    FileHandlerSettings(
                        newLocation,
                        fileHandlerSettings.safRootURI,
                        fileHandlerSettings.safCrosswordsURI,
                        fileHandlerSettings.safArchiveURI,
                        fileHandlerSettings.safToImportURI,
                        fileHandlerSettings.safToImportDoneURI,
                        fileHandlerSettings.safToImportFailedURI,
                    ),
                    this::newFileHandlerAppExit,
                )
            }
        })
    }

    private fun newFileHandlerAppExit() {
        _state.value = state.value.copy(storageChangedAppExit = true)
    }

    private fun startSettingsExport() {
        raiseNeedsURI(RequestedURI.SETTINGS_EXPORT)
    }

    private fun startSettingsImport() {
        raiseNeedsURI(RequestedURI.SETTINGS_IMPORT)
    }

    private fun startPuzzlesExport() {
        raiseNeedsURI(RequestedURI.PUZZLES_EXPORT)
    }

    private fun <T> Flow<T>.stateInHere(initialValue : T) : StateFlow<T> {
        return this.stateInSubscribed(viewModelScope, initialValue)
    }

    private fun Flow<Boolean>.stateInHereBool() : StateFlow<Boolean> {
        return this.stateInHere(false)
    }

    private fun Flow<String>.stateInHereString() : StateFlow<String> {
        return this.stateInHere("")
    }

    private fun Flow<Int>.stateInHereInt() : StateFlow<Int> {
        return this.stateInHere(0)
    }

    private fun <T> Flow<Set<T>>.stateInHereSet() : StateFlow<Set<T>> {
        return this.stateInHere(setOf())
    }

    /**
     * Make the toggle button settings entry for a downloader
     */
    private fun makeDownloaderToggle(
        settingsData : DownloaderSettingsData,
    ) : SettingsBoolean {
        val errorMsg = if (settingsData.downloaderID == DownloaderID.CUSTOM)
            downloadCustomDailyURLErrorMsg
        else
            null

        return SettingsBoolean(
            settingsData.page,
            settingsData.title,
            settingsData.summary,
            settings.liveDownloadEnabledDownloader(settingsData.downloaderID)
                .stateInHereBool(),
            { enabled ->
                settings.setDownloadEnableDownloader(
                    settingsData.downloaderID,
                    enabled,
                )
            },
            errorMsg = errorMsg,
            extraSettingsPage = SettingsPage(settingsData.title),
        )
    }

    /**
     * Make the toggle button settings entry for a downloader
     */
    private fun makeDownloaderExtraSettings(
        settingsData : DownloaderSettingsData,
    ) : List<SettingsItem> {
        val settingsPage = SettingsPage(settingsData.title)
        val did = settingsData.downloaderID

        val customSettings = if (did == DownloaderID.CUSTOM) {
            listOf(
                SettingsString(
                    settingsPage,
                    Res.string.custom_title,
                    Res.string.custom_title_desc,
                    Res.string.custom_title_hint,
                    settings.liveDownloadCustomDailyTitle.stateInHereString(),
                    settings::setDownloadCustomDailyTitle,
                ),
                SettingsString(
                    settingsPage,
                    Res.string.custom_url,
                    Res.string.custom_url_desc,
                    Res.string.custom_url_hint,
                    settings.liveDownloadCustomDailyURL.stateInHereString(),
                    settings::setDownloadCustomDailyURL,
                    downloadCustomDailyURLErrorMsg,
                ),
            )
        } else {
            listOf()
        }

        val heading = SettingsHeading(
            settingsPage,
            Res.string.block_auto_downloads,
        )

        val dayToggles = listOf(
            SettingsBoolean(
                settingsPage,
                Res.string.block_auto_mon,
                Res.string.block_auto_mon_desc,
                settings.liveDownloadAutoDownloaderBlockMon(did)
                    .stateInHereBool(),
                { settings.setDownloadAutoDownloaderBlockMon(did, it) }
            ),
            SettingsBoolean(
                settingsPage,
                Res.string.block_auto_tue,
                Res.string.block_auto_tue_desc,
                settings.liveDownloadAutoDownloaderBlockTue(did)
                    .stateInHereBool(),
                { settings.setDownloadAutoDownloaderBlockTue(did, it) }
            ),
            SettingsBoolean(
                settingsPage,
                Res.string.block_auto_wed,
                Res.string.block_auto_wed_desc,
                settings.liveDownloadAutoDownloaderBlockWed(did)
                    .stateInHereBool(),
                { settings.setDownloadAutoDownloaderBlockWed(did, it) }
            ),
            SettingsBoolean(
                settingsPage,
                Res.string.block_auto_thu,
                Res.string.block_auto_thu_desc,
                settings.liveDownloadAutoDownloaderBlockThu(did)
                    .stateInHereBool(),
                { settings.setDownloadAutoDownloaderBlockThu(did, it) }
            ),
            SettingsBoolean(
                settingsPage,
                Res.string.block_auto_fri,
                Res.string.block_auto_fri_desc,
                settings.liveDownloadAutoDownloaderBlockFri(did)
                    .stateInHereBool(),
                { settings.setDownloadAutoDownloaderBlockFri(did, it) }
            ),
            SettingsBoolean(
                settingsPage,
                Res.string.block_auto_sat,
                Res.string.block_auto_sat_desc,
                settings.liveDownloadAutoDownloaderBlockSat(did)
                    .stateInHereBool(),
                { settings.setDownloadAutoDownloaderBlockSat(did, it) }
            ),
            SettingsBoolean(
                settingsPage,
                Res.string.block_auto_sun,
                Res.string.block_auto_sun_desc,
                settings.liveDownloadAutoDownloaderBlockSun(did)
                    .stateInHereBool(),
                { settings.setDownloadAutoDownloaderBlockSun(did, it) }
            ),
        )

        val toggle = SettingsTriState.build(
            viewModelScope,
            settingsPage,
            Res.string.toggle_all_auto_download_blocks,
            Res.string.toggle_all_auto_download_blocks_desc,
            dayToggles,
        )

        return customSettings + listOf(heading, toggle) + dayToggles
    }

    /**
     * Check keyboard binding value, return error string if bad
     */
    private suspend fun getKeyboardBindingErrorMessage(
        value : String,
    ) : String? {
        return if (value.isNotEmpty() && ForkyzKey.parseKey(value) == null) {
            utils.getString(
                Res.string.keyboard_binding_unparsable,
                value,
            )
        } else {
            null
        }
    }
}

private fun <T> List<T>.triState(predicate : (T) -> Boolean) : TriState {
    var hasFalse = false
    var hasTrue = false
    this.forEach {
        if (predicate(it)) {
            if (hasFalse)
                return TriState.INDETERMINATE
            hasTrue = true
        } else {
            if (hasTrue)
                return TriState.INDETERMINATE
            hasFalse = true
        }
    }
    if (hasTrue)
        return TriState.ON
    else
        return TriState.OFF
}

