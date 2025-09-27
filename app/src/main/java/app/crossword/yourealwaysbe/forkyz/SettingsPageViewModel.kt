
package app.crossword.yourealwaysbe.forkyz

import java.io.IOException
import java.time.format.DateTimeFormatter
import javax.inject.Inject
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

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.color.DynamicColors

import dagger.hilt.android.lifecycle.HiltViewModel

import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider
import app.crossword.yourealwaysbe.forkyz.settings.BrowseSwipeAction
import app.crossword.yourealwaysbe.forkyz.settings.ClueHighlight
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
import app.crossword.yourealwaysbe.forkyz.settings.MovementStrategySetting
import app.crossword.yourealwaysbe.forkyz.settings.Orientation
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.settings.Theme
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerSAF
import app.crossword.yourealwaysbe.forkyz.util.mediate
import app.crossword.yourealwaysbe.forkyz.util.stateInSubscribed
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

enum class SettingsPage(val title : Int) {
    ROOT(R.string.settings_label),
    PUZZLE_SOURCES(R.string.select_sources),
    PUZZLE_SOURCES_DAILY(R.string.daily_crosswords),
    PUZZLE_SOURCES_WEEKLY(R.string.weekly_crosswords),
    PUZZLE_SOURCES_SCRAPERS(R.string.scraper_sources),
    DOWNLOAD(R.string.download_settings),
    DOWNLOAD_BACKGROUND(R.string.background_download_opts),
    BROWSER(R.string.browser_settings),
    DISPLAY(R.string.display_settings),
    INTERACTION(R.string.interaction_settings),
    KEYBOARD(R.string.keyboard_settings),
    VOICE_ACCESSIBILITY(R.string.voice_settings),
    EXTERNAL_TOOLS(R.string.external_tools_settings),
    EXPORT_IMPORT(R.string.preferences_export_import),
    SEARCH(R.string.filter_settings_title),
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
    val title : Int,
    val summary : Int?,
    val errorMsg : StateFlow<String?>? = null
) : SettingsItem(page)

/**
 * A base class that marks items as requiring later dialog input via
 * activeSettingsInputItem
 */
open class SettingsInputItem(
    page : SettingsPage,
    title : Int,
    summary : Int?,
    errorMsg : StateFlow<String?>? = null
) : SettingsNamedItem(page, title, summary, errorMsg)

class SettingsSubPage(
    page : SettingsPage,
    summary : Int?,
    val subpage : SettingsPage,
) : SettingsNamedItem(page, subpage.title, summary)

class SettingsBoolean(
    page : SettingsPage,
    title : Int,
    summary : Int?,
    val value : StateFlow<Boolean>,
    val setValue : (Boolean) -> Unit,
) : SettingsNamedItem(page, title, summary)

enum class TriState {
    OFF, INDETERMINATE, ON
}

class SettingsTriState(
    page : SettingsPage,
    title : Int,
    summary : Int?,
    val value : StateFlow<TriState>,
    val setValue : (TriState) -> Unit,
) : SettingsNamedItem(page, title, summary) {
    companion object {
        fun build(
            scope : CoroutineScope,
            page : SettingsPage,
            title : Int,
            summary : Int?,
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
    val labelId : Int?,
    val labelString : String?,
    val value : T,
) {
    constructor(label : Int, value : T) : this(label, null, value)
    constructor(s : String, value : T) : this(null, s, value)
}

open class SettingsDynamicList<T>(
    page : SettingsPage,
    title : Int,
    summary : Int?,
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
    title : Int,
    summary : Int?,
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
    title : Int,
    summary : Int?,
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
    title : Int,
    summary : Int?,
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
    title : Int,
    summary : Int?,
    val hint : Int?,
    val value : StateFlow<String>,
    val setValue : (String) -> Unit,
    errorMsg : StateFlow<String?>? = null,
    val dialogMsg : Int? = null,
) : SettingsInputItem(page, title, summary, errorMsg)

class SettingsInteger(
    page : SettingsPage,
    title : Int,
    summary : Int?,
    val hint : Int?,
    val value : StateFlow<Int>,
    val setValue : (Int) -> Unit,
    errorMsg : StateFlow<String?>? = null,
) : SettingsInputItem(page, title, summary, errorMsg)

class SettingsHeading(
    page : SettingsPage,
    title : Int,
    summary : Int? = null,
) : SettingsNamedItem(page, title, summary)

class SettingsHTMLPage(
    page : SettingsPage,
    title : Int,
    summary : Int?,
    val url : String,
) : SettingsNamedItem(page, title, summary) {
    constructor(
        page : SettingsPage,
        title : Int,
        url : String
    ) : this(page, title, null, url)
}

class SettingsDivider(
    page : SettingsPage,
): SettingsItem(page)

class SettingsAction(
    page : SettingsPage,
    title : Int,
    summary : Int?,
    val onClick : () -> Unit,
) : SettingsNamedItem(page, title, summary)

/**
 * Call Logcat activity
 *
 * Should be made into a more general class if more activities come along.
 */
class SettingsLogcat(
    page : SettingsPage,
    title : Int,
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

@HiltViewModel
class SettingsPageViewModel @Inject constructor(
    application : Application,
    private val settings : ForkyzSettings,
    private val utils : AndroidVersionUtils,
    private val downloadersProvider : DownloadersProvider,
) : AndroidViewModel(application) {

    private val fileActionMutex = Mutex()

    private val availableThemeSettings =
        if (DynamicColors.isDynamicColorAvailable()) {
            listOf(
                SettingsListEntry(R.string.standard_theme, Theme.T_STANDARD),
                SettingsListEntry(R.string.dynamic_theme, Theme.T_DYNAMIC),
                SettingsListEntry(
                    R.string.legacy_like_theme,
                    Theme.T_LEGACY_LIKE,
                ),
            )
        } else {
            listOf(
                SettingsListEntry(R.string.standard_theme, Theme.T_STANDARD),
                SettingsListEntry(
                    R.string.legacy_like_theme,
                    Theme.T_LEGACY_LIKE,
                ),
            )
        }

    private val storageLocationsStateFlow
        : StateFlow<List<SettingsListEntry<StorageLocation>>>
            = settings.liveFileHandlerSettings.map { fhSettings ->
                val app : Application = getApplication()

                val label = (
                    app.getString(R.string.external_storage_saf)
                    + " "
                    + app.getString(
                        R.string.external_storage_saf_current_uri,
                        fhSettings.safRootURI
                    )
                )

                listOf(
                    SettingsListEntry(
                        R.string.internal_storage,
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
                        downloader.getName(), downloader.getInternalName()
                    )
                }
            }.stateInSubscribed(viewModelScope, listOf())

    private val downloadCustomDailyURLErrorMsg : StateFlow<String?>
        = settings.liveDownloadCustomDailyURL.map { url ->
            if (url.trim().isEmpty()) {
                application.getString(R.string.custom_url_missing)
            } else {
                try {
                    DateTimeFormatter.ofPattern(url)
                    null
                } catch (e : IllegalArgumentException) {
                    e.message
                }
            }
        }.stateInSubscribed(viewModelScope, null)

    private val downloadersTogglesDaily = listOf(
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.de_standaard,
            R.string.de_standaard_desc,
            settings.liveDownloadDeStandaard.stateInHere(),
            settings::setDownloadDeStandaard,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.elpais_experto,
            R.string.elpais_experto_desc,
            settings.liveDownloadElPaisExperto.stateInHere(),
            settings::setDownloadElPaisExperto,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.guardian_daily,
            R.string.updates_mon_fri,
            settings.liveDownloadGuardianDailyCryptic.stateInHere(),
            settings::setDownloadGuardianDailyCryptic,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.hamburger_abendblatt_daily,
            R.string.hamburger_abendblatt_daily_desc,
            settings.liveDownloadHamAbend.stateInHere(),
            settings::setDownloadHamAbend,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.independent_daily,
            null,
            settings.liveDownloadIndependentDailyCryptic.stateInHere(),
            settings::setDownloadIndependentDailyCryptic,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.irish_news_cryptic,
            R.string.updates_mon_fri,
            settings.liveDownloadIrishNewsCryptic.stateInHere(),
            settings::setDownloadIrishNewsCryptic,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.joseph_crossword,
            R.string.updates_mon_sat,
            settings.liveDownloadJoseph.stateInHere(),
            settings::setDownloadJoseph,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.vingminutes,
            R.string.vingminutes_desc,
            settings.liveDownload20Minutes.stateInHere(),
            settings::setDownload20Minutes,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.la_times,
            R.string.la_times_desc,
            settings.liveDownloadLATimes.stateInHere(),
            settings::setDownloadLATimes,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.le_parisien_daily_f1,
            R.string.le_parisien_daily_f1_desc,
            settings.liveDownloadLeParisienF1.stateInHere(),
            settings::setDownloadLeParisienF1,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.le_parisien_daily_f2,
            R.string.le_parisien_daily_f2_desc,
            settings.liveDownloadLeParisienF2.stateInHere(),
            settings::setDownloadLeParisienF2,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.le_parisien_daily_f3,
            R.string.le_parisien_daily_f3_desc,
            settings.liveDownloadLeParisienF3.stateInHere(),
            settings::setDownloadLeParisienF3,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.le_parisien_daily_f4,
            R.string.le_parisien_daily_f4_desc,
            settings.liveDownloadLeParisienF4.stateInHere(),
            settings::setDownloadLeParisienF4,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.metro_cryptic,
            R.string.updates_mon_sat,
            settings.liveDownloadMetroCryptic.stateInHere(),
            settings::setDownloadMetroCryptic,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.metro_quick,
            R.string.updates_mon_sat,
            settings.liveDownloadMetroQuick.stateInHere(),
            settings::setDownloadMetroQuick,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.newsday,
            null,
            settings.liveDownloadNewsday.stateInHere(),
            settings::setDownloadNewsday,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.new_york_times_syndicated,
            null,
            settings.liveDownloadNewYorkTimesSyndicated.stateInHere(),
            settings::setDownloadNewYorkTimesSyndicated,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.sheffer_crossword,
            R.string.updates_mon_sat,
            settings.liveDownloadSheffer.stateInHere(),
            settings::setDownloadSheffer,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.universal_crossword,
            R.string.updates_mon_sat,
            settings.liveDownloadUniversal.stateInHere(),
            settings::setDownloadUniversal,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.usa_today,
            R.string.updates_daily,
            settings.liveDownloadUSAToday.stateInHere(),
            settings::setDownloadUSAToday,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.wall_street_journal,
            R.string.updates_mon_sat,
            settings.liveDownloadWsj.stateInHere(),
            settings::setDownloadWsj,
        ),
    )

    val downloadersTogglesCustomDaily = listOf(
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.custom,
            R.string.custom_desc,
            settings.liveDownloadCustomDaily.stateInHere(),
            settings::setDownloadCustomDaily,
        )
    )

    val downloadersTogglesWeeklyRegular = listOf(
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            R.string.de_telegraaf,
            R.string.de_telegraaf_desc,
            settings.liveDownloadDeTelegraaf.stateInHere(),
            settings::setDownloadDeTelegraaf,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            R.string.jonesin_crosswords,
            R.string.updates_thursdays,
            settings.liveDownloadJonesin.stateInHere(),
            settings::setDownloadJonesin,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            R.string.guardian_quiptic,
            R.string.updates_sundays,
            settings.liveDownloadGuardianWeeklyQuiptic.stateInHere(),
            settings::setDownloadGuardianWeeklyQuiptic,
        ),
    )

    val downloadersTogglesWeeklyLarge = listOf(
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            R.string.premier_crossword,
            R.string.updates_sundays,
            settings.liveDownloadPremier.stateInHere(),
            settings::setDownloadPremier,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            R.string.washington_post_sunday,
            R.string.updates_sundays,
            settings.liveDownloadWaPoSunday.stateInHere(),
            settings::setDownloadWaPoSunday,
        ),
    )

    val downloadersTogglesScrapers = listOf(
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            R.string.cru_puzzle_workshop,
            R.string.cru_puzzle_workshop_desc,
            settings.liveScrapeCru.stateInHere(),
            settings::setScrapeCru,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            R.string.guardian_quick,
            R.string.guardian_quick_desc,
            settings.liveScrapeGuardianQuick.stateInHere(),
            settings::setScrapeGuardianQuick,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            R.string.keglars_cryptics,
            R.string.keglars_cryptics_desc,
            settings.liveScrapeKegler.stateInHere(),
            settings::setScrapeKegler,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            R.string.private_eye,
            R.string.private_eye_desc,
            settings.liveScrapePrivateEye.stateInHere(),
            settings::setScrapePrivateEye,
        ),
        SettingsBoolean(
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            R.string.przekroj,
            R.string.przekroj_desc,
            settings.liveScrapePrzekroj.stateInHere(),
            settings::setScrapePrzekroj,
        ),
    )

    private val settingsItemsList : List<SettingsItem> = listOf(
        SettingsDynamicList<StorageLocation>(
            SettingsPage.ROOT,
            R.string.storage_location,
            R.string.storage_location_desc,
            storageLocationsStateFlow,
            settings.liveFileHandlerStorageLocation.stateInHere(
                StorageLocation.SL_INTERNAL,
            ),
            this::onStorageLocationChange,
        ),
        SettingsSubPage(
            SettingsPage.ROOT,
            R.string.select_sources_desc,
            SettingsPage.PUZZLE_SOURCES,
        ),
        SettingsSubPage(
            SettingsPage.ROOT,
            R.string.download_settings_desc,
            SettingsPage.DOWNLOAD,
        ),
        SettingsSubPage(
            SettingsPage.ROOT,
            R.string.browser_settings_desc,
            SettingsPage.BROWSER,
        ),
        SettingsSubPage(
            SettingsPage.ROOT,
            R.string.display_settings_desc,
            SettingsPage.DISPLAY,
        ),
        SettingsSubPage(
            SettingsPage.ROOT,
            R.string.interaction_settings_desc,
            SettingsPage.INTERACTION,
        ),
        SettingsSubPage(
            SettingsPage.ROOT,
            R.string.keyboard_settings_desc,
            SettingsPage.KEYBOARD,
        ),
        SettingsSubPage(
            SettingsPage.ROOT,
            R.string.voice_settings_desc,
            SettingsPage.VOICE_ACCESSIBILITY,
        ),
        SettingsSubPage(
            SettingsPage.ROOT,
            R.string.external_tools_settings_desc,
            SettingsPage.EXTERNAL_TOOLS,
        ),
        SettingsSubPage(
            SettingsPage.ROOT,
            R.string.preferences_export_import_desc,
            SettingsPage.EXPORT_IMPORT,
        ),
        SettingsHeading(
            SettingsPage.ROOT,
            R.string.about_forkyz,
        ),
        SettingsHTMLPage(
            SettingsPage.ROOT,
            R.string.release_notes,
            "release.html",
        ),
        SettingsHTMLPage(
            SettingsPage.ROOT,
            R.string.license,
            "license.html",
        ),
        SettingsLogcat(
            SettingsPage.ROOT,
            R.string.logcat,
        ),

        // Sources
        SettingsSubPage(
            SettingsPage.PUZZLE_SOURCES,
            R.string.daily_crosswords_desc,
            SettingsPage.PUZZLE_SOURCES_DAILY,
        ),
        SettingsSubPage(
            SettingsPage.PUZZLE_SOURCES,
            R.string.weekly_crosswords_desc,
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
        ),
        SettingsSubPage(
            SettingsPage.PUZZLE_SOURCES,
            R.string.scraper_sources_desc,
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
        ),
        SettingsTriState.build(
            viewModelScope,
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.toggle_all_daily_sources,
            R.string.toggle_all_daily_sources_desc,
            downloadersTogglesDaily + downloadersTogglesCustomDaily,
        ),
    ) + downloadersTogglesDaily + listOf(
        SettingsHeading(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.custom_daily,
        ),
    ) + downloadersTogglesCustomDaily + listOf(
        SettingsString(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.custom_title,
            R.string.custom_title_desc,
            R.string.custom_title_hint,
            settings.liveDownloadCustomDailyTitle.stateInHere(),
            settings::setDownloadCustomDailyTitle,
        ),
        SettingsString(
            SettingsPage.PUZZLE_SOURCES_DAILY,
            R.string.custom_url,
            R.string.custom_url_desc,
            R.string.custom_url_hint,
            settings.liveDownloadCustomDailyURL.stateInHere(),
            settings::setDownloadCustomDailyURL,
            downloadCustomDailyURLErrorMsg,
        ),
        SettingsTriState.build(
            viewModelScope,
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            R.string.toggle_all_weekly_sources,
            R.string.toggle_all_weekly_sources_desc,
            downloadersTogglesWeeklyRegular + downloadersTogglesWeeklyLarge,
        ),
        SettingsHeading(
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            R.string.regular_sized,
            null,
        ),
    ) + downloadersTogglesWeeklyRegular + listOf(
        SettingsHeading(
            SettingsPage.PUZZLE_SOURCES_WEEKLY,
            R.string.large_sized,
            null,
        ),
    ) + downloadersTogglesWeeklyLarge + listOf(
        SettingsTriState.build(
            viewModelScope,
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            R.string.toggle_all_scrapers,
            R.string.toggle_all_scrapers_desc,
            downloadersTogglesScrapers,
        ),
    ) + downloadersTogglesScrapers + listOf(
        SettingsDivider(SettingsPage.PUZZLE_SOURCES_SCRAPERS),
        SettingsHTMLPage(
            SettingsPage.PUZZLE_SOURCES_SCRAPERS,
            R.string.about_scrapes,
            "scrapes.html",
        ),

        // Download
        SettingsBoolean(
            SettingsPage.DOWNLOAD,
            R.string.automatic_download,
            R.string.automatic_download_desc,
            settings.liveDownloadOnStartUp.stateInHere(),
            settings::setDownloadOnStartUp,
        ),
        SettingsSubPage(
            SettingsPage.DOWNLOAD,
            R.string.background_download_opts_desc,
            SettingsPage.DOWNLOAD_BACKGROUND,
        ),
        SettingsDynamicMultiList<String>(
            SettingsPage.DOWNLOAD,
            R.string.auto_downloaders,
            R.string.auto_downloaders_desc,
            downloadAutoDownloadersStateFlow,
            settings.liveDownloadAutoDownloaders.stateInHere(),
            settings::setDownloadAutoDownloaders,
        ),
        SettingsBoolean(
            SettingsPage.DOWNLOAD,
            R.string.no_summary_download_notifs,
            R.string.no_summary_download_notifs_desc,
            settings.liveDownloadSuppressSummaryMessages.stateInHere(),
            settings::setDownloadSuppressSummaryMessages,
        ),
        SettingsBoolean(
            SettingsPage.DOWNLOAD,
            R.string.no_puzzle_download_notifs,
            R.string.no_puzzle_download_notifs_desc,
            settings.liveDownloadSuppressMessages.stateInHere(),
            settings::setDownloadSuppressMessages,
        ),
        SettingsList<Int>(
            SettingsPage.DOWNLOAD,
            R.string.download_timeout,
            R.string.download_timeout_desc,
            listOf(
                SettingsListEntry(R.string.fifteen_seconds, 15000),
                SettingsListEntry(R.string.thirty_seconds, 30000),
                SettingsListEntry(R.string.forty_five_seconds, 45000),
                SettingsListEntry(R.string.sixty_seconds, 60000),
            ),
            settings.liveDownloadTimeout.stateInHere(),
            settings::setDownloadTimeout,
        ),

        // Background Download
        SettingsHeading(
            SettingsPage.DOWNLOAD_BACKGROUND,
            R.string.background_download_time_config,
        ),
        SettingsBoolean(
            SettingsPage.DOWNLOAD_BACKGROUND,
            R.string.background_download_hourly,
            R.string.background_download_hourly_desc,
            settings.liveDownloadHourly.stateInHere(),
            { value -> settings.setDownloadHourly(value) },
        ),
        SettingsMultiList(
            SettingsPage.DOWNLOAD_BACKGROUND,
            R.string.background_download_days,
            R.string.background_download_days_desc,
            listOf(
                // matches Java DayOfWeek
                SettingsListEntry(R.string.background_download_mon, "1"),
                SettingsListEntry(R.string.background_download_tue, "2"),
                SettingsListEntry(R.string.background_download_wed, "3"),
                SettingsListEntry(R.string.background_download_thu, "4"),
                SettingsListEntry(R.string.background_download_fri, "5"),
                SettingsListEntry(R.string.background_download_sat, "6"),
                SettingsListEntry(R.string.background_download_sun, "7"),
            ),
            settings.liveDownloadDays.stateInHere(),
            { value -> settings.setDownloadDays(value) },
        ),
        SettingsList<Int>(
            SettingsPage.DOWNLOAD_BACKGROUND,
            R.string.background_download_days_time,
            R.string.background_download_days_time_desc,
            listOf(
                SettingsListEntry(R.string.background_download_0, 0),
                SettingsListEntry(R.string.background_download_1, 1),
                SettingsListEntry(R.string.background_download_2, 2),
                SettingsListEntry(R.string.background_download_3, 3),
                SettingsListEntry(R.string.background_download_4, 4),
                SettingsListEntry(R.string.background_download_5, 5),
                SettingsListEntry(R.string.background_download_6, 6),
                SettingsListEntry(R.string.background_download_7, 7),
                SettingsListEntry(R.string.background_download_8, 8),
                SettingsListEntry(R.string.background_download_9, 9),
                SettingsListEntry(R.string.background_download_10, 10),
                SettingsListEntry(R.string.background_download_11, 11),
                SettingsListEntry(R.string.background_download_12, 12),
                SettingsListEntry(R.string.background_download_13, 13),
                SettingsListEntry(R.string.background_download_14, 14),
                SettingsListEntry(R.string.background_download_15, 15),
                SettingsListEntry(R.string.background_download_16, 16),
                SettingsListEntry(R.string.background_download_17, 17),
                SettingsListEntry(R.string.background_download_18, 18),
                SettingsListEntry(R.string.background_download_19, 19),
                SettingsListEntry(R.string.background_download_20, 20),
                SettingsListEntry(R.string.background_download_21, 21),
                SettingsListEntry(R.string.background_download_22, 22),
                SettingsListEntry(R.string.background_download_23, 23),
            ),
            settings.liveDownloadDaysTime.stateInHere(),
            { value -> settings.setDownloadDaysTime(value) },
        ),
        SettingsHeading(
            SettingsPage.DOWNLOAD_BACKGROUND,
            R.string.background_download_constraints_config,
        ),
        SettingsBoolean(
            SettingsPage.DOWNLOAD_BACKGROUND,
            R.string.download_wifi_only,
            R.string.download_wifi_only_desc,
            settings.liveDownloadRequireUnmetered.stateInHere(),
            { value -> settings.setDownloadRequireUnmetered(value) },
        ),
        SettingsBoolean(
            SettingsPage.DOWNLOAD_BACKGROUND,
            R.string.download_roaming,
            R.string.download_roaming_desc,
            settings.liveDownloadAllowRoaming.stateInHere(),
            { value -> settings.setDownloadAllowRoaming(value) },
        ),
        SettingsBoolean(
            SettingsPage.DOWNLOAD_BACKGROUND,
            R.string.download_charging,
            R.string.download_charging_desc,
            settings.liveDownloadRequireCharging.stateInHere(),
            { value -> settings.setDownloadRequireCharging(value) },
        ),

        // Browser
        SettingsBoolean(
            SettingsPage.BROWSER,
            R.string.dont_confirm_cleanup,
            R.string.dont_confirm_cleanup_desc,
            settings.liveBrowseDontConfirmCleanup.stateInHere(),
            settings::setBrowseDontConfirmCleanup,
        ),
        SettingsBoolean(
            SettingsPage.BROWSER,
            R.string.delete_on_cleanup,
            R.string.delete_on_cleanup_desc,
            settings.liveBrowseDeleteOnCleanup.stateInHere(),
            settings::setBrowseDeleteOnCleanup,
        ),
        SettingsList<Int>(
            SettingsPage.BROWSER,
            R.string.cleanup_unfinished,
            R.string.cleanup_unfinished_desc,
            listOf(
                SettingsListEntry(R.string.cleanup_age_two_days, 2),
                SettingsListEntry(R.string.cleanup_age_one_week, 7),
                SettingsListEntry(R.string.cleanup_age_thirty_days, 30),
                SettingsListEntry(R.string.cleanup_age_never, -1),
            ),
            settings.liveBrowseCleanupAge.stateInHere(),
            settings::setBrowseCleanupAge,
        ),
        SettingsList<Int>(
            SettingsPage.BROWSER,
            R.string.cleanup_archives,
            R.string.cleanup_archives_desc,
            listOf(
                SettingsListEntry(R.string.cleanup_age_two_days, 2),
                SettingsListEntry(R.string.cleanup_age_one_week, 7),
                SettingsListEntry(R.string.cleanup_age_thirty_days, 30),
                SettingsListEntry(R.string.cleanup_age_never, -1),
            ),
            settings.liveBrowseCleanupAgeArchive.stateInHere(),
            settings::setBrowseCleanupAgeArchive,
        ),
        SettingsBoolean(
            SettingsPage.BROWSER,
            R.string.disable_swipe,
            R.string.disable_swipe_desc,
            settings.liveBrowseDisableSwipe.stateInHere(),
            settings::setBrowseDisableSwipe,
        ),
        SettingsList<BrowseSwipeAction>(
            SettingsPage.BROWSER,
            R.string.swipe_action,
            R.string.swipe_action_desc,
            listOf(
                SettingsListEntry(
                    R.string.delete,
                    BrowseSwipeAction.BSA_DELETE
                ),
                SettingsListEntry(
                    R.string.archive,
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
            R.string.dont_confirm_browse_delete,
            R.string.dont_confirm_browse_delete_desc,
            settings.liveBrowseDontConfirmDelete.stateInHere(),
            settings::setBrowseDontConfirmDelete,
        ),
        SettingsBoolean(
            SettingsPage.BROWSER,
            R.string.browse_show_percentage_correct,
            R.string.browse_show_percentage_correct_desc,
            settings.liveBrowseShowPercentageCorrect.stateInHere(),
            settings::setBrowseShowPercentageCorrect,
        ),
        SettingsBoolean(
            SettingsPage.BROWSER,
            R.string.browse_always_show_rating,
            R.string.browse_always_show_rating_desc,
            settings.liveBrowseAlwaysShowRating.stateInHere(),
            settings::setBrowseAlwaysShowRating,
        ),
        SettingsBoolean(
            SettingsPage.BROWSER,
            R.string.browse_indicate_if_solution,
            R.string.browse_indicate_if_solution_desc,
            settings.liveBrowseIndicateIfSolution.stateInHere(),
            settings::setBrowseIndicateIfSolution,
        ),

        // Display
        SettingsList<Theme>(
            SettingsPage.DISPLAY,
            R.string.theme_preference,
            R.string.theme_preference_desc,
            availableThemeSettings,
            settings.liveAppTheme.stateInHere(Theme.T_STANDARD),
            settings::setAppTheme,
        ),
        SettingsBoolean(
            SettingsPage.DISPLAY,
            R.string.clue_below_grid,
            R.string.clue_below_grid_desc,
            settings.livePlayClueBelowGrid.stateInHere(),
            settings::setPlayClueBelowGrid,
        ),
        SettingsList<FitToScreenMode>(
            SettingsPage.DISPLAY,
            R.string.fit_to_screen_mode,
            R.string.fit_to_screen_mode_desc,
            listOf(
                SettingsListEntry(
                    R.string.fit_to_screen_mode_never,
                    FitToScreenMode.FTSM_NEVER,
                ),
                SettingsListEntry(
                    R.string.fit_to_screen_mode_start,
                    FitToScreenMode.FTSM_START,
                ),
                SettingsListEntry(
                    R.string.fit_to_screen_mode_locked,
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
            R.string.display_scratch,
            R.string.display_scratch_desc,
            settings.livePlayScratchDisplay.stateInHere(),
            settings::setPlayScratchDisplay,
        ),
        SettingsBoolean(
            SettingsPage.DISPLAY,
            R.string.no_hint_highlighting,
            R.string.no_hint_highlighting_desc,
            settings.livePlaySuppressHintHighlighting.stateInHere(),
            settings::setPlaySuppressHintHighlighting,
        ),
        SettingsList<DisplaySeparators>(
            SettingsPage.DISPLAY,
            R.string.display_separators,
            R.string.display_separators_desc,
            listOf(
                SettingsListEntry(
                    R.string.display_separators_never,
                    DisplaySeparators.DS_NEVER,
                ),
                SettingsListEntry(
                    R.string.display_separators_selected,
                    DisplaySeparators.DS_SELECTED,
                ),
                SettingsListEntry(
                    R.string.display_separators_always,
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
            R.string.infer_separators,
            R.string.infer_separators_desc,
            settings.livePlayInferSeparators.stateInHere(),
            settings::setPlayInferSeparators,
        ),
        SettingsBoolean(
            SettingsPage.DISPLAY,
            R.string.indicate_show_errors,
            R.string.indicate_show_errors_desc,
            settings.livePlayIndicateShowErrors.stateInHere(),
            settings::setPlayIndicateShowErrors,
        ),
        SettingsBoolean(
            SettingsPage.DISPLAY,
            R.string.show_length,
            R.string.show_length_desc,
            settings.livePlayShowCount.stateInHere(),
            settings::setPlayShowCount,
        ),
        SettingsBoolean(
            SettingsPage.DISPLAY,
            R.string.show_timer,
            R.string.show_timer_desc,
            settings.livePlayShowTimer.stateInHere(),
            settings::setPlayShowTimer,
        ),
        SettingsBoolean(
            SettingsPage.DISPLAY,
            R.string.full_screen,
            R.string.full_screen_desc,
            settings.livePlayFullScreen.stateInHere(),
            settings::setPlayFullScreen,
        ),
        SettingsList<Orientation>(
            SettingsPage.DISPLAY,
            R.string.orientation_lock,
            R.string.orientation_lock_desc,
            listOf(
                SettingsListEntry(
                    R.string.orientation_lock_unlocked,
                    Orientation.O_UNLOCKED,
                ),
                SettingsListEntry(
                    R.string.orientation_lock_portrait,
                    Orientation.O_PORTRAIT,
                ),
                SettingsListEntry(
                    R.string.orientation_lock_landscape,
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
            R.string.grid_ratio_portrait,
            R.string.grid_ratio_portrait_desc,
            listOf(
                SettingsListEntry(
                    R.string.grid_ratio_puzzle,
                    GridRatio.GR_PUZZLE_SHAPE,
                ),
                SettingsListEntry(
                    R.string.grid_ratio_one_to_one,
                    GridRatio.GR_ONE_TO_ONE,
                ),
                SettingsListEntry(
                    R.string.grid_ratio_thirty_pcnt,
                    GridRatio.GR_THIRTY_PCNT,
                ),
                SettingsListEntry(
                    R.string.grid_ratio_forty_pcnt,
                    GridRatio.GR_FORTY_PCNT,
                ),
                SettingsListEntry(
                    R.string.grid_ratio_fifty_pcnt,
                    GridRatio.GR_FIFTY_PCNT,
                ),
                SettingsListEntry(
                    R.string.grid_ratio_sixty_pcnt,
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
            R.string.grid_ratio_landscape,
            R.string.grid_ratio_landscape_desc,
            listOf(
                SettingsListEntry(
                    R.string.grid_ratio_puzzle,
                    GridRatio.GR_PUZZLE_SHAPE,
                ),
                SettingsListEntry(
                    R.string.grid_ratio_one_to_one,
                    GridRatio.GR_ONE_TO_ONE,
                ),
                SettingsListEntry(
                    R.string.grid_ratio_thirty_pcnt,
                    GridRatio.GR_THIRTY_PCNT,
                ),
                SettingsListEntry(
                    R.string.grid_ratio_forty_pcnt,
                    GridRatio.GR_FORTY_PCNT,
                ),
                SettingsListEntry(
                    R.string.grid_ratio_fifty_pcnt,
                    GridRatio.GR_FIFTY_PCNT,
                ),
                SettingsListEntry(
                    R.string.grid_ratio_sixty_pcnt,
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
            R.string.clue_tabs_double,
            R.string.clue_tabs_double_desc,
            listOf(
                SettingsListEntry(
                    R.string.clue_tabs_double_never,
                    ClueTabsDouble.CTD_NEVER,
                ),
                SettingsListEntry(
                    R.string.clue_tabs_double_landscape,
                    ClueTabsDouble.CTD_LANDSCAPE,
                ),
                SettingsListEntry(
                    R.string.clue_tabs_double_wide,
                    ClueTabsDouble.CTD_WIDE,
                ),
                SettingsListEntry(
                    R.string.clue_tabs_double_always,
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
            R.string.clue_highlight,
            R.string.clue_highlight_desc,
            listOf(
                SettingsListEntry(
                    R.string.clue_highlight_none,
                    ClueHighlight.CH_NONE,
                ),
                SettingsListEntry(
                    R.string.clue_highlight_radio_button,
                    ClueHighlight.CH_RADIO_BUTTON,
                ),
                SettingsListEntry(
                    R.string.clue_highlight_background,
                    ClueHighlight.CH_BACKGROUND,
                ),
                SettingsListEntry(
                    R.string.clue_highlight_both,
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
            R.string.keyboard_showhide,
            R.string.keyboard_showhide_desc,
            listOf(
                SettingsListEntry(
                    R.string.keyboard_always_show,
                    KeyboardMode.KM_ALWAYS_SHOW,
                ),
                SettingsListEntry(
                    R.string.keyboard_hide_manual,
                    KeyboardMode.KM_HIDE_MANUAL,
                ),
                SettingsListEntry(
                    R.string.keyboard_show_sparingly_desc,
                    KeyboardMode.KM_SHOW_SPARINGLY,
                ),
                SettingsListEntry(
                    R.string.keyboard_never_show,
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
            R.string.keyboard_use_native,
            R.string.keyboard_use_native_desc,
            settings.liveKeyboardUseNative.stateInHere(),
            settings::setKeyboardUseNative,
        ),
        SettingsHeading(
            SettingsPage.KEYBOARD,
            R.string.keyboard_built_in,
        ),
        SettingsList<KeyboardLayout>(
            SettingsPage.KEYBOARD,
            R.string.keyboard_layout,
            R.string.keyboard_layout_desc,
            listOf(
                SettingsListEntry(
                    R.string.keyboard_qwerty,
                    KeyboardLayout.KL_QWERTY,
                ),
                SettingsListEntry(
                    R.string.keyboard_qwertz,
                    KeyboardLayout.KL_QWERTZ,
                ),
                SettingsListEntry(
                    R.string.keyboard_azerty,
                    KeyboardLayout.KL_AZERTY,
                ),
                SettingsListEntry(
                    R.string.keyboard_dvorak,
                    KeyboardLayout.KL_DVORAK,
                ),
                SettingsListEntry(
                    R.string.keyboard_colemak,
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
            R.string.keyboard_hide_button,
            R.string.keyboard_hide_button_desc,
            settings.liveKeyboardHideButton.stateInHere(),
            settings::setKeyboardHideButton,
        ),
        SettingsBoolean(
            SettingsPage.KEYBOARD,
            R.string.keyboard_compact,
            R.string.keyboard_compact_desc,
            settings.liveKeyboardCompact.stateInHere(),
            settings::setKeyboardCompact,
        ),
        SettingsBoolean(
            SettingsPage.KEYBOARD,
            R.string.keyboard_haptic,
            R.string.keyboard_haptic_desc,
            settings.liveKeyboardHaptic.stateInHere(),
            settings::setKeyboardHaptic,
        ),
        SettingsInteger(
            SettingsPage.KEYBOARD,
            R.string.keyboard_repeat_delay,
            R.string.keyboard_repeat_delay_desc,
            R.string.keyboard_repeat_delay_hint,
            settings.liveKeyboardRepeatDelay.stateInHere(),
            settings::setKeyboardRepeatDelay,
        ),
        SettingsInteger(
            SettingsPage.KEYBOARD,
            R.string.keyboard_repeat_interval,
            R.string.keyboard_repeat_interval_desc,
            R.string.keyboard_repeat_interval_hint,
            settings.liveKeyboardRepeatInterval.stateInHere(),
            settings::setKeyboardRepeatInterval,
        ),
        SettingsHeading(
            SettingsPage.KEYBOARD,
            R.string.keyboard_native,
        ),
        SettingsBoolean(
            SettingsPage.KEYBOARD,
            R.string.keyboard_force_caps,
            R.string.keyboard_force_caps_desc,
            settings.liveKeyboardForceCaps.stateInHere(),
            settings::setKeyboardForceCaps,
        ),

        // Interaction
        SettingsBoolean(
            SettingsPage.INTERACTION,
            R.string.snap_current,
            R.string.snap_current_desc,
            settings.livePlayEnsureVisible.stateInHere(),
            settings::setPlayEnsureVisible,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            R.string.scroll_to_clue,
            R.string.scroll_to_clue_desc,
            settings.liveClueListSnapToClue.stateInHere(),
            settings::setClueListSnapToClue,
        ),
        SettingsList<MovementStrategySetting>(
            SettingsPage.INTERACTION,
            R.string.movement_style,
            R.string.movement_style_desc,
            listOf(
                SettingsListEntry(
                    R.string.movement_strategy_next_word,
                    MovementStrategySetting.MSS_MOVE_NEXT_ON_AXIS,
                ),
                SettingsListEntry(
                    R.string.movement_strategy_stop_end,
                    MovementStrategySetting.MSS_STOP_ON_END,
                ),
                SettingsListEntry(
                    R.string.movement_strategy_next_clue,
                    MovementStrategySetting.MSS_MOVE_NEXT_CLUE,
                ),
                SettingsListEntry(
                    R.string.movement_strategy_next_parallel_clue,
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
            R.string.movement_style_clue_list,
            R.string.movement_style_clue_list_desc,
            listOf(
                SettingsListEntry(
                    R.string.movement_strategy_stop_end,
                    MovementStrategySetting.MSS_STOP_ON_END,
                ),
                SettingsListEntry(
                    R.string.movement_strategy_next_clue,
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
            R.string.skip_filled,
            R.string.skip_filled_desc,
            settings.livePlaySkipFilled.stateInHere(),
            settings::setPlaySkipFilled,
        ),
        SettingsList<CycleUnfilledMode>(
            SettingsPage.INTERACTION,
            R.string.cycle_unfilled,
            R.string.cycle_unfilled_desc,
            listOf(
                SettingsListEntry(
                    R.string.cycle_unfilled_never,
                    CycleUnfilledMode.CU_NEVER,
                ),
                SettingsListEntry(
                    R.string.cycle_unfilled_forwards,
                    CycleUnfilledMode.CU_FORWARDS,
                ),
                SettingsListEntry(
                    R.string.cycle_unfilled_always,
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
            R.string.double_tap_zoom,
            R.string.double_tap_zoom_desc,
            settings.livePlayDoubleTapFitBoard.stateInHere(),
            settings::setPlayDoubleTapFitBoard,
        ),
        SettingsList<DeleteCrossingModeSetting>(
            SettingsPage.INTERACTION,
            R.string.dont_delete_crossing,
            R.string.dont_delete_crossing_desc,
            listOf(
                SettingsListEntry(
                    R.string.delete_crossing_delete,
                    DeleteCrossingModeSetting.DCMS_DELETE,
                ),
                SettingsListEntry(
                    R.string.delete_crossing_preserve_filled_words,
                    DeleteCrossingModeSetting.DCMS_PRESERVE_FILLED_WORDS,
                ),
                SettingsListEntry(
                    R.string.delete_crossing_preserve_filled_cells,
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
            R.string.preserve_correct,
            R.string.preserve_correct_desc,
            settings.livePlayPreserveCorrectLettersInShowErrors.stateInHere(),
            settings::setPlayPreserveCorrectLettersInShowErrors,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            R.string.play_letter_undo,
            R.string.play_letter_undo_desc,
            settings.livePlayPlayLetterUndoEnabled.stateInHere(),
            settings::setPlayPlayLetterUndoEnabled,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            R.string.space_change_dir,
            R.string.space_change_dir_desc,
            settings.livePlaySpaceChangesDirection.stateInHere(),
            settings::setPlaySpaceChangesDirection,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            R.string.enter_changes_direction,
            R.string.enter_changes_direction_desc,
            settings.livePlayEnterChangesDirection.stateInHere(),
            settings::setPlayEnterChangesDirection,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            R.string.toggle_before_move,
            R.string.toggle_before_move_desc,
            settings.livePlayToggleBeforeMove.stateInHere(),
            settings::setPlayToggleBeforeMove,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            R.string.random_clue_on_shake,
            R.string.random_clue_on_shake_desc,
            settings.livePlayRandomClueOnShake.stateInHere(),
            settings::setPlayRandomClueOnShake,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            R.string.predict_anagram_chars,
            R.string.predict_anagram_chars_desc,
            settings.livePlayPredictAnagramChars.stateInHere(),
            settings::setPlayPredictAnagramChars,
        ),
        SettingsBoolean(
            SettingsPage.INTERACTION,
            R.string.disable_ratings,
            R.string.disable_ratings_desc,
            settings.liveRatingsDisableRatings.stateInHere(),
            settings::setRatingsDisableRatings,
        ),

        // Voice & Accessibility
        SettingsBoolean(
            SettingsPage.VOICE_ACCESSIBILITY,
            R.string.voice_with_volume,
            R.string.voice_with_volume_desc,
            settings.liveVoiceVolumeActivatesVoice.stateInHere(),
            settings::setVoiceVolumeActivatesVoice,
        ),
        SettingsBoolean(
            SettingsPage.VOICE_ACCESSIBILITY,
            R.string.voice_with_button,
            R.string.voice_with_button_desc,
            settings.liveVoiceButtonActivatesVoice.stateInHere(),
            settings::setVoiceButtonActivatesVoice,
        ),
        SettingsBoolean(
            SettingsPage.VOICE_ACCESSIBILITY,
            R.string.announce_clue_button,
            R.string.announce_clue_button_desc,
            settings.liveVoiceButtonAnnounceClue.stateInHere(),
            settings::setVoiceButtonAnnounceClue,
        ),
        SettingsBoolean(
            SettingsPage.VOICE_ACCESSIBILITY,
            R.string.announce_clue_equals,
            R.string.announce_clue_equals_desc,
            settings.liveVoiceEqualsAnnounceClue.stateInHere(),
            settings::setVoiceEqualsAnnounceClue,
        ),
        SettingsBoolean(
            SettingsPage.VOICE_ACCESSIBILITY,
            R.string.announce_clue_always,
            R.string.announce_clue_always_desc,
            settings.liveVoiceAlwaysAnnounceClue.stateInHere(),
            settings::setVoiceAlwaysAnnounceClue,
        ),
        SettingsBoolean(
            SettingsPage.VOICE_ACCESSIBILITY,
            R.string.announce_box_always,
            R.string.announce_box_always_desc,
            settings.liveVoiceAlwaysAnnounceBox.stateInHere(),
            settings::setVoiceAlwaysAnnounceBox,
        ),
        SettingsDivider(SettingsPage.VOICE_ACCESSIBILITY),
        SettingsHTMLPage(
            SettingsPage.VOICE_ACCESSIBILITY,
            R.string.about_voice_commands,
            "voice_commands.html",
        ),

        // External Tools
        SettingsList<ExternalDictionarySetting>(
            SettingsPage.EXTERNAL_TOOLS,
            R.string.external_dictionary,
            R.string.external_dictionary_desc,
            listOf(
                SettingsListEntry(
                    R.string.dictionary_none,
                    ExternalDictionarySetting.EDS_NONE,
                ),
                SettingsListEntry(
                    R.string.free_dictionary,
                    ExternalDictionarySetting.EDS_FREE,
                ),
                SettingsListEntry(
                    R.string.quick_dic,
                    ExternalDictionarySetting.EDS_QUICK,
                ),
                SettingsListEntry(
                    R.string.aard2,
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
            R.string.crossword_solver_enabled,
            R.string.crossword_solver_enabled_desc,
            settings.liveExtCrosswordSolverEnabled.stateInHere(),
            settings::setExtCrosswordSolverEnabled,
        ),
        SettingsString(
            SettingsPage.EXTERNAL_TOOLS,
            R.string.chat_gpt_api_key,
            R.string.chat_gpt_api_key_desc,
            R.string.chat_gpt_api_key,
            settings.liveExtChatGPTAPIKey.stateInHere(),
            settings::setExtChatGPTAPIKey,
            dialogMsg = R.string.chat_gpt_api_key_message,
        ),
        SettingsBoolean(
            SettingsPage.EXTERNAL_TOOLS,
            R.string.duckduckgo,
            R.string.duckduckgo_desc,
            settings.liveExtDuckDuckGoEnabled.stateInHere(),
            settings::setExtDuckDuckGoEnabled,
        ),
        SettingsBoolean(
            SettingsPage.EXTERNAL_TOOLS,
            R.string.fifteen_squared,
            R.string.fifteen_squared_desc,
            settings.liveExtFifteenSquaredEnabled.stateInHere(),
            settings::setExtFifteenSquaredEnabled,
        ),

        // Export/Import
        SettingsAction(
            SettingsPage.EXPORT_IMPORT,
            R.string.preferences_export,
            R.string.preferences_export_desc,
            this::startSettingsExport,
        ),
        SettingsAction(
            SettingsPage.EXPORT_IMPORT,
            R.string.preferences_import,
            R.string.preferences_import_desc,
            this::startSettingsImport,
        ),
        SettingsAction(
            SettingsPage.EXPORT_IMPORT,
            R.string.puzzles_export,
            R.string.puzzles_export_desc,
            this::startPuzzlesExport,
        ),
    )

    private val rootState = settingsPageState(SettingsPage.ROOT, false)
    private val _state = MutableStateFlow<SettingsPageViewState>(rootState)
    private val stateBackStack = ArrayDeque<SettingsPageViewState>()
    // separate as should not appear on back stack
    private val _needsURI = MutableStateFlow<RequestedURI>(RequestedURI.NONE)
    // separate from back stack, int is resource id of messages to show
    private val _messages = MutableStateFlow<List<Int>>(listOf())

    val state : StateFlow<SettingsPageViewState> = _state
    val needsURI : StateFlow<RequestedURI> = _needsURI
    val messages : StateFlow<List<Int>> = _messages

    fun startSearch() {
        if (!state.value.inSearchMode) {
            resetBackStack()
            _state.value = state.value.copy(
                currentPage = SettingsPage.SEARCH,
                searchTerm = "",
                hasBackStack = true,
                settingsItems = settingsItemsList.filter(string_filter(""))
            )
        }
    }

    fun setSearchTerm(term : String) {
        if (state.value.inSearchMode) {
            // no back stack push for refining a search
            _state.value = state.value.copy(
                searchTerm = term,
                hasBackStack = true,
                settingsItems = settingsItemsList.filter(string_filter(term))
            )
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
        val fileHandler = FileHandlerSAF.initialiseSAFForRoot(
            getApplication(), uri
        )

        if (fileHandler != null) {
            settings.setFileHandlerSettings(
                fileHandler.getSettings(),
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

    fun exportSettings(uri : Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            fileActionMutex.withLock {
                val app : Application = getApplication()
                try {
                    app.getContentResolver()
                        .openOutputStream(uri)
                        .use(settings::exportSettings)
                    showMessage(R.string.preferences_exported)
                } catch (e : IOException) {
                    showMessage(R.string.preferences_export_failed)
                }
            }
        }
    }

    fun importSettings(uri : Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            fileActionMutex.withLock {
                val app : Application = getApplication()
                try {
                    app.getContentResolver()
                        .openInputStream(uri)
                        .use(settings::importSettings)
                    showMessage(R.string.preferences_imported)
                } catch (e : IOException) {
                    showMessage(R.string.preferences_import_failed)
                }
            }
        }
    }

    private fun showMessage(msg : Int) {
        _messages.value = messages.value + msg
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

    private fun page_filter(page : SettingsPage) : (SettingsItem) -> Boolean
        = { item : SettingsItem -> item.page == page }

    private fun string_filter(s : String) : (SettingsItem) -> Boolean {
        // annoying to have to get resources in view model!
        val app : Application = getApplication()
        return { item : SettingsItem ->
            when (item) {
                is SettingsHeading -> false
                is SettingsNamedItem -> {
                    app.getString(item.title).contains(s, ignoreCase = true)
                        || item.summary?.let {
                            app.getString(it).contains(s, ignoreCase = true)
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
                page_filter(page)
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
                    val fileHandler
                        = FileHandlerSAF.readHandlerFromSettings(
                            getApplication(),
                            fileHandlerSettings,
                            settings
                        )

                    if (fileHandler == null) {
                        raiseNeedsURI(RequestedURI.SAF)
                    } else {
                        settings.setFileHandlerSettings(
                            fileHandler.getSettings(),
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


