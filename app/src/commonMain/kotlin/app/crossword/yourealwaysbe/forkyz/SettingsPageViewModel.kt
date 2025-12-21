
package app.crossword.yourealwaysbe.forkyz

import java.io.IOException
import java.time.format.DateTimeFormatter
import kotlin.collections.ArrayDeque
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import org.jetbrains.compose.resources.StringResource

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.aard2
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.about_forkyz
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.about_scrapes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.about_voice_commands
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
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browse_always_show_rating
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browse_always_show_rating_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browse_indicate_if_solution
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browse_indicate_if_solution_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browse_show_percentage_correct
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browse_show_percentage_correct_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browser_settings
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browser_settings_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cleanup_age_never
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cleanup_age_never
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cleanup_age_one_week
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cleanup_age_one_week
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cleanup_age_thirty_days
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cleanup_age_thirty_days
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cleanup_age_two_days
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
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_fifty_pcnt
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_fifty_pcnt
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_forty_pcnt
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_forty_pcnt
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_landscape
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_landscape_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_one_to_one
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_one_to_one
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_portrait
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_portrait_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_puzzle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_puzzle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_sixty_pcnt
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_sixty_pcnt
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_thirty_pcnt
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.grid_ratio_thirty_pcnt
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.guardian_daily
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.guardian_quick
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.guardian_quick_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.guardian_quiptic
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.hamburger_abendblatt_daily
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.hamburger_abendblatt_daily_desc
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
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_settings
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_settings_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_show_sparingly_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_showhide
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_showhide_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_use_native
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_use_native_desc
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
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.legacy_like_theme
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.license
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent_api_key
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent_api_key
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent_api_key_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent_api_key_message
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent_chat_gpt
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent_claude
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_agent_none
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.logcat
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.metro_cryptic
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.metro_quick
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.movement_strategy_next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.movement_strategy_next_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.movement_strategy_next_parallel_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.movement_strategy_next_word
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.movement_strategy_stop_end
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
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.scroll_to_clue_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.select_sources
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.select_sources_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.settings_label
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
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_all_daily_sources
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_all_daily_sources_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_all_scrapers
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_all_scrapers_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_all_weekly_sources
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_all_weekly_sources_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_before_move
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_before_move_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.universal_crossword
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_daily
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_daily
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_daily
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_daily
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_daily
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_mon_fri
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_mon_fri
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_mon_sat
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_mon_sat
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_mon_sat
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_mon_sat
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_mon_sat
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_mon_sat
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_sundays
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_sundays
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_sundays
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.updates_thursdays
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.usa_today
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
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider
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
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.settings.Theme
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.mediate
import app.crossword.yourealwaysbe.forkyz.util.stateInSubscribed

enum class SettingsPage(val title : StringResource) {
    ROOT(Res.string.settings_label),
    PUZZLE_SOURCES(Res.string.select_sources),
    PUZZLE_SOURCES_DAILY(Res.string.daily_crosswords),
    PUZZLE_SOURCES_WEEKLY(Res.string.weekly_crosswords),
    PUZZLE_SOURCES_SCRAPERS(Res.string.scraper_sources),
    DOWNLOAD(Res.string.download_settings),
    DOWNLOAD_BACKGROUND(Res.string.background_download_opts),
    BROWSER(Res.string.browser_settings),
    DISPLAY(Res.string.display_settings),
    INTERACTION(Res.string.interaction_settings),
    KEYBOARD(Res.string.keyboard_settings),
    VOICE_ACCESSIBILITY(Res.string.voice_settings),
    EXTERNAL_TOOLS(Res.string.external_tools_settings),
    EXPORT_IMPORT(Res.string.preferences_export_import),
    SEARCH(Res.string.filter_settings_title),
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
) : SettingsNamedItem(page, title, summary)

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
                    { booleans.triState { it.value.value ?: false } },
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
        entries.value?.let {
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
) : SettingsInputItem(page, title, summary) {
    fun selectItems(indices : Set<Int>) {
        entries.value?.let { entries ->
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
) : SettingsDynamicMultiList<T>(
    page,
    title,
    summary,
    MutableStateFlow<List<SettingsListEntry<T>>>(entries),
    value,
    setValue,
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

class SettingsPageViewModel(
    private val settings : ForkyzSettings,
    private val utils : NativeBackendUtils,
    private val downloadersProvider : DownloadersProvider,
) : ViewModel() {

    private val fileActionMutex = Mutex()

    private val availableThemeSettings =
        if (utils.isDynamicColorAvailable) {
            listOf(
                SettingsListEntry(Res.string.standard_theme, Theme.T_STANDARD),
                SettingsListEntry(Res.string.dynamic_theme, Theme.T_DYNAMIC),
                SettingsListEntry(
                    Res.string.legacy_like_theme,
                    Theme.T_LEGACY_LIKE,
                ),
            )
        } else {
            listOf(
                SettingsListEntry(Res.string.standard_theme, Theme.T_STANDARD),
                SettingsListEntry(
                    Res.string.legacy_like_theme,
                    Theme.T_LEGACY_LIKE,
                ),
            )
        }

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

    private val downloadAutoDownloadersStateFlow
        : StateFlow<List<SettingsListEntry<String>>>
            = downloadersProvider.liveDownloaders.filterNotNull().map { dls ->
                dls.getDownloaders().map { downloader ->
                    SettingsListEntry(
                        downloader.name,
                        downloader.internalName,
                    )
                }
            }.stateInSubscribed(viewModelScope, listOf())

    private val downloadCustomDailyURLErrorMsg : StateFlow<String?>
        = combine(
            settings.liveDownloadCustomDaily,
            settings.liveDownloadCustomDailyURL,
        ) { enabled, url ->
            if (enabled) {
                if (url.trim().isEmpty()) {
                    utils.getString(Res.string.custom_url_missing)
                } else {
                    try {
                        DateTimeFormatter.ofPattern(url)
                        null
                    } catch (e : IllegalArgumentException) {
                        e.message
                    }
                }
            } else {
                null
            }
        }.stateInSubscribed(viewModelScope, null)

    private val downloadersTogglesDaily = listOf(
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.de_standaard,
            Res.string.de_standaard_desc,
            settings.liveDownloadDeStandaard.stateInHere(),
            settings::setDownloadDeStandaard,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.elpais_experto,
            Res.string.elpais_experto_desc,
            settings.liveDownloadElPaisExperto.stateInHere(),
            settings::setDownloadElPaisExperto,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.guardian_daily,
            Res.string.updates_mon_fri,
            settings.liveDownloadGuardianDailyCryptic.stateInHere(),
            settings::setDownloadGuardianDailyCryptic,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.hamburger_abendblatt_daily,
            Res.string.hamburger_abendblatt_daily_desc,
            settings.liveDownloadHamAbend.stateInHere(),
            settings::setDownloadHamAbend,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.independent_daily,
            null,
            settings.liveDownloadIndependentDailyCryptic.stateInHere(),
            settings::setDownloadIndependentDailyCryptic,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.irish_news_cryptic,
            Res.string.updates_mon_fri,
            settings.liveDownloadIrishNewsCryptic.stateInHere(),
            settings::setDownloadIrishNewsCryptic,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.joseph_crossword,
            Res.string.updates_mon_sat,
            settings.liveDownloadJoseph.stateInHere(),
            settings::setDownloadJoseph,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.vingminutes,
            Res.string.vingminutes_desc,
            settings.liveDownload20Minutes.stateInHere(),
            settings::setDownload20Minutes,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.la_times,
            Res.string.la_times_desc,
            settings.liveDownloadLATimes.stateInHere(),
            settings::setDownloadLATimes,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.le_parisien_daily_f1,
            Res.string.le_parisien_daily_f1_desc,
            settings.liveDownloadLeParisienF1.stateInHere(),
            settings::setDownloadLeParisienF1,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.le_parisien_daily_f2,
            Res.string.le_parisien_daily_f2_desc,
            settings.liveDownloadLeParisienF2.stateInHere(),
            settings::setDownloadLeParisienF2,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.le_parisien_daily_f3,
            Res.string.le_parisien_daily_f3_desc,
            settings.liveDownloadLeParisienF3.stateInHere(),
            settings::setDownloadLeParisienF3,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.le_parisien_daily_f4,
            Res.string.le_parisien_daily_f4_desc,
            settings.liveDownloadLeParisienF4.stateInHere(),
            settings::setDownloadLeParisienF4,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.metro_cryptic,
            Res.string.updates_mon_sat,
            settings.liveDownloadMetroCryptic.stateInHere(),
            settings::setDownloadMetroCryptic,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.metro_quick,
            Res.string.updates_mon_sat,
            settings.liveDownloadMetroQuick.stateInHere(),
            settings::setDownloadMetroQuick,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.newsday,
            null,
            settings.liveDownloadNewsday.stateInHere(),
            settings::setDownloadNewsday,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.new_york_times_syndicated,
            null,
            settings.liveDownloadNewYorkTimesSyndicated.stateInHere(),
            settings::setDownloadNewYorkTimesSyndicated,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.nrc_in_het_midden,
            Res.string.nrc_in_het_midden_desc,
            settings.liveDownloadNRCInHetMidden.stateInHere(),
            settings::setDownloadNRCInHetMidden,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.sheffer_crossword,
            Res.string.updates_mon_sat,
            settings.liveDownloadSheffer.stateInHere(),
            settings::setDownloadSheffer,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.sud_ouest_mots_croises,
            Res.string.updates_daily,
            settings.liveDownloadSudOuestMotsCroises.stateInHere(),
            settings::setDownloadSudOuestMotsCroises,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.sud_ouest_mots_fleches,
            Res.string.updates_daily,
            settings.liveDownloadSudOuestMotsFleches.stateInHere(),
            settings::setDownloadSudOuestMotsFleches,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.tf1_mots_croises,
            Res.string.updates_daily,
            settings.liveDownloadTF1MotsCroises.stateInHere(),
            settings::setDownloadTF1MotsCroises,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.tf1_mots_fleches,
            Res.string.updates_daily,
            settings.liveDownloadTF1MotsFleches.stateInHere(),
            settings::setDownloadTF1MotsFleches,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.universal_crossword,
            Res.string.updates_mon_sat,
            settings.liveDownloadUniversal.stateInHere(),
            settings::setDownloadUniversal,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.usa_today,
            Res.string.updates_daily,
            settings.liveDownloadUSAToday.stateInHere(),
            settings::setDownloadUSAToday,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.wall_street_journal,
            Res.string.updates_mon_sat,
            settings.liveDownloadWsj.stateInHere(),
            settings::setDownloadWsj,
        ),
    )

    val downloadersTogglesCustomDaily = listOf(
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.custom,
            Res.string.custom_desc,
            settings.liveDownloadCustomDaily.stateInHere(),
            settings::setDownloadCustomDaily,
        )
    )

    val downloadersTogglesWeeklyRegular = listOf(
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            Res.string.de_telegraaf,
            Res.string.de_telegraaf_desc,
            settings.liveDownloadDeTelegraaf.stateInHere(),
            settings::setDownloadDeTelegraaf,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            Res.string.jonesin_crosswords,
            Res.string.updates_thursdays,
            settings.liveDownloadJonesin.stateInHere(),
            settings::setDownloadJonesin,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            Res.string.guardian_quiptic,
            Res.string.updates_sundays,
            settings.liveDownloadGuardianWeeklyQuiptic.stateInHere(),
            settings::setDownloadGuardianWeeklyQuiptic,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            Res.string.nrc_scrypto,
            Res.string.nrc_scrypto_desc,
            settings.liveDownloadNRCScrypto.stateInHere(),
            settings::setDownloadNRCScrypto,
        )
    )

    val downloadersTogglesWeeklyLarge = listOf(
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            Res.string.premier_crossword,
            Res.string.updates_sundays,
            settings.liveDownloadPremier.stateInHere(),
            settings::setDownloadPremier,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            Res.string.washington_post_sunday,
            Res.string.updates_sundays,
            settings.liveDownloadWaPoSunday.stateInHere(),
            settings::setDownloadWaPoSunday,
        ),
    )

    val downloadersTogglesScrapers = listOf(
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            Res.string.cru_puzzle_workshop,
            Res.string.cru_puzzle_workshop_desc,
            settings.liveScrapeCru.stateInHere(),
            settings::setScrapeCru,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            Res.string.guardian_quick,
            Res.string.guardian_quick_desc,
            settings.liveScrapeGuardianQuick.stateInHere(),
            settings::setScrapeGuardianQuick,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            Res.string.keglars_cryptics,
            Res.string.keglars_cryptics_desc,
            settings.liveScrapeKegler.stateInHere(),
            settings::setScrapeKegler,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            Res.string.private_eye,
            Res.string.private_eye_desc,
            settings.liveScrapePrivateEye.stateInHere(),
            settings::setScrapePrivateEye,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            Res.string.przekroj,
            Res.string.przekroj_desc,
            settings.liveScrapePrzekroj.stateInHere(),
            settings::setScrapePrzekroj,
        ),
    )

    private val settingsItemsList : List<SettingsItem> = listOf(
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
        SettingsHeading(
            SettingsPage.ROOT,
            Res.string.about_forkyz,
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
    ) + downloadersTogglesDaily + listOf(
        SettingsHeading(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.custom_daily,
        ),
    ) + downloadersTogglesCustomDaily + listOf(
        SettingsString(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.custom_title,
            Res.string.custom_title_desc,
            Res.string.custom_title_hint,
            settings.liveDownloadCustomDailyTitle.stateInHere(),
            settings::setDownloadCustomDailyTitle,
        ),
        SettingsString(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            Res.string.custom_url,
            Res.string.custom_url_desc,
            Res.string.custom_url_hint,
            settings.liveDownloadCustomDailyURL.stateInHere(),
            settings::setDownloadCustomDailyURL,
            downloadCustomDailyURLErrorMsg,
        ),
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
    ) + downloadersTogglesWeeklyRegular + listOf(
        SettingsHeading(
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            Res.string.large_sized,
            null,
        ),
    ) + downloadersTogglesWeeklyLarge + listOf(
        SettingsTriState.build(
            viewModelScope,
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            Res.string.toggle_all_scrapers,
            Res.string.toggle_all_scrapers_desc,
            downloadersTogglesScrapers,
        ),
    ) + downloadersTogglesScrapers + listOf(
        SettingsDivider(SettingsPage.PUZZLE_SOURCES_SCRAPERS),
        SettingsHTMLPage(
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            Res.string.about_scrapes,
            HTMLFile.SCRAPERS_HELP,
        ),

        // Download
        SettingsBoolean(
            SettingsPage.DOWNLOAD,
            Res.string.automatic_download,
            Res.string.automatic_download_desc,
            settings.liveDownloadOnStartUp.stateInHere(),
            settings::setDownloadOnStartUp,
        ),
        SettingsSubPage(
            SettingsPage.DOWNLOAD,
            Res.string.background_download_opts_desc,
            SettingsPage.DOWNLOAD_BACKGROUND,
        ),
        SettingsDynamicMultiList<String>(
            SettingsPage.DOWNLOAD,
            Res.string.auto_downloaders,
            Res.string.auto_downloaders_desc,
            downloadAutoDownloadersStateFlow,
            settings.liveDownloadAutoDownloaders.stateInHere(),
            settings::setDownloadAutoDownloaders,
        ),
        SettingsBoolean(
            SettingsPage.DOWNLOAD,
            Res.string.no_summary_download_notifs,
            Res.string.no_summary_download_notifs_desc,
            settings.liveDownloadSuppressSummaryMessages.stateInHere(),
            settings::setDownloadSuppressSummaryMessages,
        ),
        SettingsBoolean(
            SettingsPage.DOWNLOAD,
            Res.string.no_puzzle_download_notifs,
            Res.string.no_puzzle_download_notifs_desc,
            settings.liveDownloadSuppressMessages.stateInHere(),
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
            settings.liveDownloadTimeout.stateInHere(),
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
            settings.liveDownloadHourly.stateInHere(),
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
            settings.liveDownloadDays.stateInHere(),
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
            settings.liveDownloadDaysTime.stateInHere(),
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
            settings.liveDownloadRequireUnmetered.stateInHere(),
            { value -> settings.setDownloadRequireUnmetered(value) },
        ),
        SettingsBoolean(
            SettingsPage.DOWNLOAD_BACKGROUND,
            Res.string.download_roaming,
            Res.string.download_roaming_desc,
            settings.liveDownloadAllowRoaming.stateInHere(),
            { value -> settings.setDownloadAllowRoaming(value) },
        ),
        SettingsBoolean(
            SettingsPage.DOWNLOAD_BACKGROUND,
            Res.string.download_charging,
            Res.string.download_charging_desc,
            settings.liveDownloadRequireCharging.stateInHere(),
            { value -> settings.setDownloadRequireCharging(value) },
        ),

        // Browser
        SettingsBoolean(
            SettingsPage.BROWSER,
            Res.string.dont_confirm_cleanup,
            Res.string.dont_confirm_cleanup_desc,
            settings.liveBrowseDontConfirmCleanup.stateInHere(),
            settings::setBrowseDontConfirmCleanup,
        ),
        SettingsBoolean(
            SettingsPage.BROWSER,
            Res.string.delete_on_cleanup,
            Res.string.delete_on_cleanup_desc,
            settings.liveBrowseDeleteOnCleanup.stateInHere(),
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
            settings.liveBrowseCleanupAge.stateInHere(),
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
            settings.liveBrowseCleanupAgeArchive.stateInHere(),
            settings::setBrowseCleanupAgeArchive,
        ),
        SettingsBoolean(
            SettingsPage.BROWSER,
            Res.string.disable_swipe,
            Res.string.disable_swipe_desc,
            settings.liveBrowseDisableSwipe.stateInHere(),
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
            settings.liveBrowseDontConfirmDelete.stateInHere(),
            settings::setBrowseDontConfirmDelete,
        ),
        SettingsBoolean(
            SettingsPage.BROWSER,
            Res.string.browse_show_percentage_correct,
            Res.string.browse_show_percentage_correct_desc,
            settings.liveBrowseShowPercentageCorrect.stateInHere(),
            settings::setBrowseShowPercentageCorrect,
        ),
        SettingsBoolean(
            SettingsPage.BROWSER,
            Res.string.browse_always_show_rating,
            Res.string.browse_always_show_rating_desc,
            settings.liveBrowseAlwaysShowRating.stateInHere(),
            settings::setBrowseAlwaysShowRating,
        ),
        SettingsBoolean(
            SettingsPage.BROWSER,
            Res.string.browse_indicate_if_solution,
            Res.string.browse_indicate_if_solution_desc,
            settings.liveBrowseIndicateIfSolution.stateInHere(),
            settings::setBrowseIndicateIfSolution,
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
            settings.livePlayClueBelowGrid.stateInHere(),
            settings::setPlayClueBelowGrid,
        ),
        SettingsBoolean(
            SettingsPage.DISPLAY,
            Res.string.expandable_clue_line,
            Res.string.expandable_clue_line_desc,
            settings.livePlayExpandableClueLine.stateInHere(),
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
            settings.livePlayShowCount.stateInHere(),
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
            settings.livePlayScratchDisplay.stateInHere(),
            settings::setPlayScratchDisplay,
        ),
        SettingsBoolean(
            SettingsPage.DISPLAY,
            Res.string.no_hint_highlighting,
            Res.string.no_hint_highlighting_desc,
            settings.livePlaySuppressHintHighlighting.stateInHere(),
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
            settings.livePlayInferSeparators.stateInHere(),
            settings::setPlayInferSeparators,
        ),
        SettingsBoolean(
            SettingsPage.DISPLAY,
            Res.string.indicate_show_errors,
            Res.string.indicate_show_errors_desc,
            settings.livePlayIndicateShowErrors.stateInHere(),
            settings::setPlayIndicateShowErrors,
        ),
        SettingsBoolean(
            SettingsPage.DISPLAY,
            Res.string.show_timer,
            Res.string.show_timer_desc,
            settings.livePlayShowTimer.stateInHere(),
            settings::setPlayShowTimer,
        ),
        SettingsBoolean(
            SettingsPage.DISPLAY,
            Res.string.full_screen,
            Res.string.full_screen_desc,
            settings.livePlayFullScreen.stateInHere(),
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
            settings.liveKeyboardUseNative.stateInHere(),
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
            settings.liveKeyboardHideButton.stateInHere(),
            settings::setKeyboardHideButton,
        ),
        SettingsBoolean(
            SettingsPage.KEYBOARD,
            Res.string.keyboard_compact,
            Res.string.keyboard_compact_desc,
            settings.liveKeyboardCompact.stateInHere(),
            settings::setKeyboardCompact,
        ),
        SettingsBoolean(
            SettingsPage.KEYBOARD,
            Res.string.keyboard_haptic,
            Res.string.keyboard_haptic_desc,
            settings.liveKeyboardHaptic.stateInHere(),
            settings::setKeyboardHaptic,
        ),
        SettingsInteger(
            SettingsPage.KEYBOARD,
            Res.string.keyboard_repeat_delay,
            Res.string.keyboard_repeat_delay_desc,
            Res.string.keyboard_repeat_delay_hint,
            settings.liveKeyboardRepeatDelay.stateInHere(),
            settings::setKeyboardRepeatDelay,
        ),
        SettingsInteger(
            SettingsPage.KEYBOARD,
            Res.string.keyboard_repeat_interval,
            Res.string.keyboard_repeat_interval_desc,
            Res.string.keyboard_repeat_interval_hint,
            settings.liveKeyboardRepeatInterval.stateInHere(),
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
            settings.liveKeyboardForceCaps.stateInHere(),
            settings::setKeyboardForceCaps,
        ),

        // Interaction
        SettingsBoolean(
            SettingsPage.INTERACTION,
            Res.string.snap_current,
            Res.string.snap_current_desc,
            settings.livePlayEnsureVisible.stateInHere(),
            settings::setPlayEnsureVisible,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            Res.string.scroll_to_clue,
            Res.string.scroll_to_clue_desc,
            settings.liveClueListSnapToClue.stateInHere(),
            settings::setClueListSnapToClue,
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
            settings.livePlaySkipFilled.stateInHere(),
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
            settings.livePlayDoubleTapFitBoard.stateInHere(),
            settings::setPlayDoubleTapFitBoard,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            Res.string.dont_snap_board_to_borders,
            Res.string.dont_snap_board_to_borders_desc,
            settings.livePlayDontSnapBoardToBorders.stateInHere(),
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
            settings.livePlayPreserveCorrectLettersInShowErrors.stateInHere(),
            settings::setPlayPreserveCorrectLettersInShowErrors,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            Res.string.play_letter_undo,
            Res.string.play_letter_undo_desc,
            settings.livePlayPlayLetterUndoEnabled.stateInHere(),
            settings::setPlayPlayLetterUndoEnabled,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            Res.string.space_change_dir,
            Res.string.space_change_dir_desc,
            settings.livePlaySpaceChangesDirection.stateInHere(),
            settings::setPlaySpaceChangesDirection,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            Res.string.enter_changes_direction,
            Res.string.enter_changes_direction_desc,
            settings.livePlayEnterChangesDirection.stateInHere(),
            settings::setPlayEnterChangesDirection,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            Res.string.toggle_before_move,
            Res.string.toggle_before_move_desc,
            settings.livePlayToggleBeforeMove.stateInHere(),
            settings::setPlayToggleBeforeMove,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            Res.string.random_clue_on_shake,
            Res.string.random_clue_on_shake_desc,
            settings.livePlayRandomClueOnShake.stateInHere(),
            settings::setPlayRandomClueOnShake,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            Res.string.predict_anagram_chars,
            Res.string.predict_anagram_chars_desc,
            settings.livePlayPredictAnagramChars.stateInHere(),
            settings::setPlayPredictAnagramChars,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            Res.string.disable_ratings,
            Res.string.disable_ratings_desc,
            settings.liveRatingsDisableRatings.stateInHere(),
            settings::setRatingsDisableRatings,
        ),

        // Voice & Accessibility
        SettingsBoolean(
            SettingsPage.VOICE_ACCESSIBILITY,
            Res.string.voice_with_volume,
            Res.string.voice_with_volume_desc,
            settings.liveVoiceVolumeActivatesVoice.stateInHere(),
            settings::setVoiceVolumeActivatesVoice,
        ),
        SettingsBoolean(
            SettingsPage.VOICE_ACCESSIBILITY,
            Res.string.voice_with_button,
            Res.string.voice_with_button_desc,
            settings.liveVoiceButtonActivatesVoice.stateInHere(),
            settings::setVoiceButtonActivatesVoice,
        ),
        SettingsBoolean(
            SettingsPage.VOICE_ACCESSIBILITY,
            Res.string.announce_clue_button,
            Res.string.announce_clue_button_desc,
            settings.liveVoiceButtonAnnounceClue.stateInHere(),
            settings::setVoiceButtonAnnounceClue,
        ),
        SettingsBoolean(
            SettingsPage.VOICE_ACCESSIBILITY,
            Res.string.announce_clue_equals,
            Res.string.announce_clue_equals_desc,
            settings.liveVoiceEqualsAnnounceClue.stateInHere(),
            settings::setVoiceEqualsAnnounceClue,
        ),
        SettingsBoolean(
            SettingsPage.VOICE_ACCESSIBILITY,
            Res.string.announce_clue_always,
            Res.string.announce_clue_always_desc,
            settings.liveVoiceAlwaysAnnounceClue.stateInHere(),
            settings::setVoiceAlwaysAnnounceClue,
        ),
        SettingsBoolean(
            SettingsPage.VOICE_ACCESSIBILITY,
            Res.string.announce_box_always,
            Res.string.announce_box_always_desc,
            settings.liveVoiceAlwaysAnnounceBox.stateInHere(),
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
            settings.liveExtCrosswordSolverEnabled.stateInHere(),
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
            settings.liveExtLLMAgentAPIKey.stateInHere(),
            settings::setExtLLMAgentAPIKey,
            dialogMsg = Res.string.llm_agent_api_key_message,
        ),
        SettingsBoolean(
            SettingsPage.EXTERNAL_TOOLS,
            Res.string.duckduckgo,
            Res.string.duckduckgo_desc,
            settings.liveExtDuckDuckGoEnabled.stateInHere(),
            settings::setExtDuckDuckGoEnabled,
        ),
        SettingsBoolean(
            SettingsPage.EXTERNAL_TOOLS,
            Res.string.fifteen_squared,
            Res.string.fifteen_squared_desc,
            settings.liveExtFifteenSquaredEnabled.stateInHere(),
            settings::setExtFifteenSquaredEnabled,
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
    )

    private val rootState = settingsPageState(SettingsPage.ROOT, false)
    private val _state = MutableStateFlow<SettingsPageViewState>(rootState)
    private val stateBackStack = ArrayDeque<SettingsPageViewState>()
    // separate as should not appear on back stack
    private val _needsURI = MutableStateFlow<RequestedURI>(RequestedURI.NONE)
    private val _messages = MutableStateFlow<List<String>>(listOf())

    val state : StateFlow<SettingsPageViewState> = _state
    val needsURI : StateFlow<RequestedURI> = _needsURI
    val messages : StateFlow<List<String>> = _messages

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
                    utils.openOutputStream(uri)
                        .use(settings::exportSettings)
                    showMessage(Res.string.preferences_exported)
                } catch (e : IOException) {
                    showMessage(Res.string.preferences_export_failed)
                } finally {
                    viewModelScope.launch(Dispatchers.Main) { onDone() }
                }
            }
        }
    }

    fun importSettings(uri : Uri, onDone : () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            fileActionMutex.withLock {
                try {
                    utils.openInputStream(uri)
                        .use(settings::importSettings)
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

    @JvmName("stateInHereBoolean")
    private fun Flow<Boolean>.stateInHere() : StateFlow<Boolean> {
        return this.stateInHere(false)
    }

    @JvmName("stateInHereString")
    private fun Flow<String>.stateInHere() : StateFlow<String> {
        return this.stateInHere("")
    }

    @JvmName("stateInHereInt")
    private fun Flow<Int>.stateInHere() : StateFlow<Int> {
        return this.stateInHere(0)
    }

    @JvmName("stateInHereStringSet")
    private fun <T> Flow<Set<T>>.stateInHere() : StateFlow<Set<T>> {
        return this.stateInHere(setOf())
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


