
package app.crossword.yourealwaysbe.forkyz.settings

import android.content.SharedPreferences
import androidx.core.content.edit

import app.crossword.yourealwaysbe.forkyz.net.DownloaderID
import app.crossword.yourealwaysbe.forkyz.puz.Playboard.ShowErrorsMode

/**
 * Migrate shared preferences to new settings
 */
fun migratePreferences(
    prefs : SharedPreferences,
    settings : ForkyzSettings,
) {
    // app theme and file handlers updated blocking so they're ready at
    // start

    // Early migrations
    // Dropping backgroundDownload migration. It is very old now, and
    // causes cyclic dependencies in the application injection (since
    // prefs migration needs to happen in app creation for dynamic
    // theme)
    getBoolean(prefs, "useDynamicColors")?.let {
        settings.setAppThemeSync(
            if (it) Theme.T_DYNAMIC else Theme.T_STANDARD
        )
    }
    getBoolean(prefs, "dontDeleteCrossing")?.let {
        settings.setPlayDeleteCrossingModeSetting(
            if (it)
                DeleteCrossingModeSetting.DCMS_PRESERVE_FILLED_CELLS
            else
                DeleteCrossingModeSetting.DCMS_DELETE
        )
    }
    getBoolean(prefs, "cycleUnfilled")?.let {
        settings.setPlayCycleUnfilledMode(
            if (it)
                CycleUnfilledMode.CU_ALWAYS
            else
                CycleUnfilledMode.CU_NEVER
        )
    }
    getBoolean(prefs, "fitToScreen")?.let {
        settings.setPlayFitToScreenMode(
            if (it) FitToScreenMode.FTSM_START else FitToScreenMode.FTSM_NEVER
        )
    }

    // Final settings before change to Datastore
    getBoolean(prefs, "alwaysAnnounceBox")?.let {
        settings.setVoiceAlwaysAnnounceBox(it)
    }
    getBoolean(prefs, "alwaysAnnounceClue")?.let {
        settings.setVoiceAlwaysAnnounceClue(it)
    }
    getString(prefs, "applicationTheme")?.let {
        when (it) {
            "S" -> Theme.T_STANDARD
            "D" -> Theme.T_DYNAMIC
            "L" -> Theme.T_LEGACY_LIKE
            else -> null
        }?.let { settings.setAppThemeSync(it) }
    }
    getInt(prefs, "archiveCleanupAge")?.let {
        settings.setBrowseCleanupAgeArchive(it)
    }
    getBoolean(prefs, "backgroundDownloadAllowRoaming")?.let {
        settings.setDownloadAllowRoaming(it)
    }
    getStringSet(prefs, "backgroundDownloadDays")?.let {
        settings.setDownloadDays(it)
    }
    getString(prefs, "backgroundDownloadDaysTime")?.let {
        try {
            settings.setDownloadDaysTime(it.toInt())
        } catch (e : NumberFormatException) {
            // ignore
        }
    }
    getBoolean(prefs, "backgroundDownloadHourly")?.let {
        settings.setDownloadHourly(it)
    }
    getBoolean(prefs, "backgroundDownloadRequireCharging")?.let {
        settings.setDownloadRequireCharging(it)
    }
    getBoolean(prefs, "backgroundDownloadRequireUnmetered")?.let {
        settings.setDownloadRequireUnmetered(it)
    }
    getBoolean(prefs, "browseAlwaysShowRating")?.let {
        settings.setBrowseAlwaysShowRating(it)
    }
    getBoolean(prefs, "browseIndicateIfSolution")?.let {
        settings.setBrowseIndicateIfSolution(it)
    }
    getBoolean(prefs, "browseNewPuzzle")?.let {
        settings.setBrowseNewPuzzle(it)
    }
    getBoolean(prefs, "browseShowPercentageCorrect")?.let {
        settings.setBrowseShowPercentageCorrect(it)
    }
    getBoolean(prefs, "buttonActivatesVoice")?.let {
        settings.setVoiceButtonActivatesVoice(it)
    }
    getBoolean(prefs, "buttonAnnounceClue")?.let {
        settings.setVoiceButtonAnnounceClue(it)
    }
    getString(prefs, "chatGPTAPIKey")?.let {
        if (it.trim().length > 0) {
            settings.setExtLLMAgent(LLMAgent.LA_CHATGPT)
            settings.setExtLLMAgentAPIKey(it)
        }
    }
    getInt(prefs, "cleanupAge")?.let {
        settings.setBrowseCleanupAge(it)
    }
    getBoolean(prefs, "clueBelowGrid")?.let {
        settings.setPlayClueBelowGrid(it)
    }
    getString(prefs, "clueHighlight")?.let {
        when (it) {
            "NONE" -> ClueHighlight.CH_NONE
            "RADIO_BUTTON" -> ClueHighlight.CH_RADIO_BUTTON
            "BACKGROUND" -> ClueHighlight.CH_BACKGROUND
            "BOTH" -> ClueHighlight.CH_BOTH
            else -> null
        }?.let { settings.setPlayClueHighlight(it) }
    }
    getString(prefs, "clueTabsDouble")?.let {
        when (it) {
            "NEVER" -> ClueTabsDouble.CTD_NEVER
            "LANDSCAPE" -> ClueTabsDouble.CTD_LANDSCAPE
            "WIDE" -> ClueTabsDouble.CTD_WIDE
            "ALWAYS" -> ClueTabsDouble.CTD_ALWAYS
            else -> null
        }?.let { settings.setClueListClueTabsDouble(it) }
    }
    getBoolean(prefs, "crosswordSolverEnabled")?.let {
        settings.setExtCrosswordSolverEnabled(it)
    }
    getString(prefs, "customDailyTitle")?.let {
        settings.setDownloadCustomDailyTitle(it)
    }
    getString(prefs, "customDailyUrl")?.let {
        settings.setDownloadCustomDailyURL(it)
    }
    getString(prefs, "cycleUnfilledMode")?.let {
        when (it) {
            "NEVER" -> CycleUnfilledMode.CU_NEVER
            "FORWARDS" -> CycleUnfilledMode.CU_FORWARDS
            "ALWAYS" -> CycleUnfilledMode.CU_ALWAYS
            else -> null
        }?.let { settings.setPlayCycleUnfilledMode(it) }
    }
    getString(prefs, "deleteCrossingMode")?.let {
        when (it) {
            "DELETE" -> DeleteCrossingModeSetting.DCMS_DELETE
            "PRESERVE_FILLED_WORDS"
                -> DeleteCrossingModeSetting.DCMS_PRESERVE_FILLED_WORDS
            "PRESERVE_FILLED_CELLS"
                -> DeleteCrossingModeSetting.DCMS_PRESERVE_FILLED_CELLS
            else -> null
        }?.let { settings.setPlayDeleteCrossingModeSetting(it) }
    }
    getBoolean(prefs, "deleteOnCleanup")?.let {
        settings.setBrowseDeleteOnCleanup(it)
    }
    getBoolean(prefs, "disableRatings")?.let {
        settings.setRatingsDisableRatings(it)
    }
    getBoolean(prefs, "disableSwipe")?.let {
        settings.setBrowseDisableSwipe(it)
    }
    getBoolean(prefs, "displayScratch")?.let {
        settings.setPlayScratchDisplay(it)
    }
    getString(prefs, "displaySeparators")?.let {
        when (it) {
            "NEVER" -> DisplaySeparators.DS_NEVER
            "SELECTED" -> DisplaySeparators.DS_SELECTED
            "ALWAYS" -> DisplaySeparators.DS_ALWAYS
            else -> null
        }?.let { settings.setPlayDisplaySeparators(it) }
    }
    getLong(prefs, "dlLast")?.let {
        settings.setBrowseLastDownload(it)
    }
    getBoolean(prefs, "dlOnStartup")?.let {
        settings.setDownloadOnStartUp(it)
    }
    getBoolean(prefs, "doubleTap")?.let {
        settings.setPlayDoubleTapFitBoard(it)
    }
    getBoolean(prefs, "download20Minutes")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.TWENTY_MINUTES,
            it,
        )
    }
    getBoolean(prefs, "downloadCustomDaily")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.CUSTOM,
            it,
        )
    }
    getBoolean(prefs, "downloadDeStandaard")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.DE_STANDAARD,
            it,
        )
    }
    getBoolean(prefs, "downloadDeTelegraaf")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.DE_TELEGRAAF,
            it,
        )
    }
    getBoolean(prefs, "downloadGuardianDailyCryptic")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.GUARDIAN_DAILY_CRYPTIC,
            it,
        )
    }
    getBoolean(prefs, "downloadGuardianWeeklyQuiptic")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.GUARDIAN_WEEKLY_QUIPTIC,
            it,
        )
    }
    getBoolean(prefs, "downloadHamAbend")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.HAM_ABEND,
            it,
        )
    }
    getBoolean(prefs, "downloadIndependentDailyCryptic")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.INDEPENDENT_DAILY_CRYPTIC,
            it,
        )
    }
    getBoolean(prefs, "downloadIrishNewsCryptic")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.IRISH_NEWS_CRYPTIC,
            it,
        )
    }
    getBoolean(prefs, "downloadJonesin")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.JONESIN,
            it,
        )
    }
    getBoolean(prefs, "downloadJoseph")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.JOSEPH,
            it,
        )
    }
    getBoolean(prefs, "downloadLeParisienF1")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.LE_PARISIEN_F1,
            it,
        )
    }
    getBoolean(prefs, "downloadLeParisienF2")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.LE_PARISIEN_F2,
            it,
        )
    }
    getBoolean(prefs, "downloadLeParisienF3")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.LE_PARISIEN_F3,
            it,
        )
    }
    getBoolean(prefs, "downloadLeParisienF4")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.LE_PARISIEN_F4,
            it,
        )
    }
    getBoolean(prefs, "downloadMetroCryptic")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.METRO_CRYPTIC,
            it,
        )
    }
    getBoolean(prefs, "downloadMetroQuick")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.METRO_QUICK,
            it,
        )
    }
    getBoolean(prefs, "downloadNewYorkTimesSyndicated")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.NYT_SYNDICATED,
            it,
        )
    }
    getBoolean(prefs, "downloadNewsday")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.NEWSDAY,
            it,
        )
    }
    getBoolean(prefs, "downloadPremier")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.PREMIER,
            it,
        )
    }
    getBoolean(prefs, "downloadSheffer")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.SHEFFER,
            it,
        )
    }
    getString(prefs, "downloadTimeout")?.let {
        try {
            settings.setDownloadTimeout(it.toInt())
        } catch (e : NumberFormatException) {
            // ignore
        }
    }
    getBoolean(prefs, "downloadUSAToday")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.USA_TODAY,
            it,
        )
    }
    getBoolean(prefs, "downloadUniversal")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.UNIVERSAL,
            it,
        )
    }
    getBoolean(prefs, "downloadWaPoSunday")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.WAPO_SUNDAY,
            it,
        )
    }
    getBoolean(prefs, "downloadWsj")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.WSJ,
            it,
        )
    }
    getBoolean(prefs, "duckDuckGoEnabled")?.let {
        settings.setExtDuckDuckGoEnabled(it)
    }
    getBoolean(prefs, "ensureVisible")?.let {
        settings.setPlayEnsureVisible(it)
    }
    getBoolean(prefs, "enterChangesDirection")?.let {
        settings.setPlayEnterChangesDirection(it)
    }
    getBoolean(prefs, "equalsAnnounceClue")?.let {
        settings.setVoiceEqualsAnnounceClue(it)
    }
    getString(prefs, "externalDictionary")?.let {
        when (it) {
            "NONE" -> ExternalDictionarySetting.EDS_NONE
            "QUICK" -> ExternalDictionarySetting.EDS_QUICK
            "AARD2" -> ExternalDictionarySetting.EDS_AARD2
            "FREE" -> ExternalDictionarySetting.EDS_FREE
            else -> null
        }?.let { settings.setExtDictionarySetting(it) }
    }
    getBoolean(prefs, "fifteenSquaredEnabled")?.let {
        settings.setExtFifteenSquaredEnabled(it)
    }
    getString(prefs, "fitToScreenMode")?.let {
        when (it) {
            "NEVER" -> FitToScreenMode.FTSM_NEVER
            "START" -> FitToScreenMode.FTSM_START
            "LOCKED" -> FitToScreenMode.FTSM_LOCKED
            else -> null
        }?.let { settings.setPlayFitToScreenMode(it) }
    }
    getBoolean(prefs, "fullScreen")?.let {
        settings.setPlayFullScreen(it)
    }
    getString(prefs, "gridRatio")?.let {
        when (it) {
            "0" -> GridRatio.GR_ONE_TO_ONE
            "1" -> GridRatio.GR_THIRTY_PCNT
            "2" -> GridRatio.GR_FORTY_PCNT
            "3" -> GridRatio.GR_FIFTY_PCNT
            "4" -> GridRatio.GR_SIXTY_PCNT
            "5" -> GridRatio.GR_PUZZLE_SHAPE
            else -> null
        }?.let { settings.setPlayGridRatioPortrait(it) }
    }
    getString(prefs, "gridRatioLand")?.let {
        when (it) {
            "0" -> GridRatio.GR_ONE_TO_ONE
            "1" -> GridRatio.GR_THIRTY_PCNT
            "2" -> GridRatio.GR_FORTY_PCNT
            "3" -> GridRatio.GR_FIFTY_PCNT
            "4" -> GridRatio.GR_SIXTY_PCNT
            "5" -> GridRatio.GR_PUZZLE_SHAPE
            else -> null
        }?.let { settings.setPlayGridRatioLandscape(it) }
    }
    getBoolean(prefs, "indicateShowErrors")?.let {
        settings.setPlayIndicateShowErrors(it)
    }
    getBoolean(prefs, "inferSeparators")?.let {
        settings.setPlayInferSeparators(it)
    }
    getBoolean(prefs, "keyboardCompact")?.let {
        settings.setKeyboardCompact(it)
    }
    getBoolean(prefs, "keyboardForceCaps")?.let {
        settings.setKeyboardForceCaps(it)
    }
    getBoolean(prefs, "keyboardHaptic")?.let {
        settings.setKeyboardHaptic(it)
    }
    getBoolean(prefs, "keyboardHideButton")?.let {
        settings.setKeyboardHideButton(it)
    }
    getString(prefs, "keyboardLayout")?.let {
        when (it) {
            "0" -> KeyboardLayout.KL_QWERTY
            "1" -> KeyboardLayout.KL_QWERTZ
            "2" -> KeyboardLayout.KL_DVORAK
            "3" -> KeyboardLayout.KL_COLEMAK
            "4" -> KeyboardLayout.KL_AZERTY
            else -> null
        }?.let { settings.setKeyboardLayout(it) }
    }
    getString(prefs, "keyboardRepeatDelay")?.let {
        try {
            settings.setKeyboardRepeatDelay(it.toInt())
        } catch (e : NumberFormatException) {
            // ignore
        }
    }
    getString(prefs, "keyboardRepeatInterval")?.let {
        try {
            settings.setKeyboardRepeatInterval(it.toInt())
        } catch (e : NumberFormatException) {
            // ignore
        }
    }
    getString(prefs, "keyboardShowHide")?.let {
        when (it) {
            "Always show" -> KeyboardMode.KM_ALWAYS_SHOW
            "Hide keyboard manually" -> KeyboardMode.KM_HIDE_MANUAL
            "Show sparingly" -> KeyboardMode.KM_SHOW_SPARINGLY
            "Never show" -> KeyboardMode.KM_NEVER_SHOW
            else -> null
        }?.let { settings.setKeyboardMode(it) }
    }
    getString(prefs, "lastSeenVersion")?.let {
        settings.setBrowseLastSeenVersion(it)
    }
    getString(prefs, "movementStrategy")?.let {
        when (it) {
            "MOVE_NEXT_ON_AXIS" -> MovementStrategySetting.MSS_MOVE_NEXT_ON_AXIS
            "STOP_ON_END" -> MovementStrategySetting.MSS_STOP_ON_END
            "MOVE_NEXT_CLUE" -> MovementStrategySetting.MSS_MOVE_NEXT_CLUE
            "MOVE_PARALLEL_WORD"
                -> MovementStrategySetting.MSS_MOVE_PARALLEL_WORD
            else -> null
        }?.let { settings.setPlayMovementStrategySetting(it) }
    }
    getString(prefs, "orientationLock")?.let {
        when (it) {
            "PORT" -> Orientation.O_PORTRAIT
            "LAND" -> Orientation.O_LANDSCAPE
            "UNLOCKED" -> Orientation.O_UNLOCKED
            else -> null
        }?.let { settings.setAppOrientationLock(it) }
    }
    getInt(prefs, "setPlayActivityClueTabsPage")?.let {
        settings.setPlayClueTabsPage(0, it)
    }
    getInt(prefs, "setPlayActivityClueTabsPage1")?.let {
        settings.setPlayClueTabsPage(1, it)
    }
    getBoolean(prefs, "playLetterUndoEnabled")?.let {
        settings.setPlayPlayLetterUndoEnabled(it)
    }
    getBoolean(prefs, "predictAnagramChars")?.let {
        settings.setPlayPredictAnagramChars(it)
    }
    getBoolean(prefs, "preserveCorrectLettersInShowErrors")?.let {
        settings.setPlayPreserveCorrectLettersInShowErrors(it)
    }
    getBoolean(prefs, "randomClueOnShake")?.let {
        settings.setPlayRandomClueOnShake(it)
    }
    getFloat(prefs, "scale")?.let {
        settings.setPlayScale(it)
    }
    getBoolean(prefs, "scrapeCru")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.CRYPTIC_CRU,
            it,
        )
    }
    getBoolean(prefs, "scrapeGuardianQuick")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.GUARDIAN_QUICK,
            it,
        )
    }
    getBoolean(prefs, "scrapeKegler")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.KEGLAR,
            it,
        )
    }
    getBoolean(prefs, "scrapePrivateEye")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.PRIVATE_EYE,
            it,
        )
    }
    getBoolean(prefs, "scrapePrzekroj")?.let {
        settings.setDownloadEnableDownloader(
            DownloaderID.PRZEKROJ,
            it,
        )
    }
    getBoolean(prefs, "scratchMode")?.let {
        settings.setPlayScratchMode(it)
    }
    getBoolean(prefs, "showCluesOnPlayScreen")?.let {
        settings.setPlayShowClueTabs(it)
    }
    getBoolean(prefs, "showCount")?.let {
        settings.setPlayShowCount(it)
    }
    getBoolean(prefs, "showErrorsCursor")?.let {
        if (it) settings.setPlayShowErrorsMode(ShowErrorsMode.CURSOR)
    }
    getBoolean(prefs, "showErrorsClue")?.let {
        if (it) settings.setPlayShowErrorsMode(ShowErrorsMode.CLUE)
    }
    getBoolean(prefs, "showErrors")?.let {
        if (it) settings.setPlayShowErrorsMode(ShowErrorsMode.GRID)
    }
    getBoolean(prefs, "showTimer")?.let {
        settings.setPlayShowTimer(it)
    }
    getBoolean(prefs, "showWordsInClueList")?.let {
        settings.setClueListShowWords(it)
    }
    getBoolean(prefs, "skipFilled")?.let {
        settings.setPlaySkipFilled(it)
    }
    getBoolean(prefs, "snapClue")?.let {
        if (it) {
            settings.setClueListSnapClueMode(SnapClueMode.SCM_CLUE)
        }
    }
    getInt(prefs, "sort")?.let {
        settings.setBrowseSort(
            when (it) {
                1 -> AccessorSetting.AS_DATE_ASC
                2 -> AccessorSetting.AS_SOURCE
                else -> AccessorSetting.AS_DATE_DESC;
            }
        )
    }
    getBoolean(prefs, "spaceChangesDirection")?.let {
        settings.setPlaySpaceChangesDirection(it)
    }
    getBoolean(prefs, "supressHints")?.let {
        settings.setPlaySuppressHintHighlighting(it)
    }
    getBoolean(prefs, "supressMessages")?.let {
        settings.setDownloadSuppressMessages(it)
    }
    getBoolean(prefs, "supressSummaryMessages")?.let {
        settings.setDownloadSuppressSummaryMessages(it)
    }
    getString(prefs, "swipeAction")?.let {
        when (it) {
            "ARCHIVE" -> BrowseSwipeAction.BSA_ARCHIVE
            "DELETE" -> BrowseSwipeAction.BSA_DELETE
            else -> null
        }?.let { settings.setBrowseSwipeAction(it) }
    }
    getBoolean(prefs, "toggleBeforeMove")?.let {
        settings.setPlayToggleBeforeMove(it)
    }
    getString(prefs, "uiTheme")?.let {
        when (it) {
            "DAY" -> DayNightMode.DNM_DAY
            "NIGHT" -> DayNightMode.DNM_NIGHT
            "SYSTEM" -> DayNightMode.DNM_SYSTEM
            else -> null
        }?.let { settings.setAppDayNightModeSync(it) }
    }
    getBoolean(prefs, "useNativeKeyboard")?.let {
        settings.setKeyboardUseNative(it)
    }
    getBoolean(prefs, "volumeActivatesVoice")?.let {
        settings.setVoiceVolumeActivatesVoice(it)
    }

    getString(prefs, "safRootUri")?.let { root ->
    getString(prefs, "safCrosswordsFolderUri")?.let { crosswords ->
    getString(prefs, "safArchiveFolderUri")?.let { archive ->
    getString(prefs, "safToImportFolderUri")?.let { toImport ->
    getString(prefs, "safToImportDoneFolderUri")?.let { toImportDone ->
    getString(prefs, "safToImportFailedFolderUri")?.let { toImportFailed ->
    getString(prefs, "storageLocation")?.let {
        when (it) {
            "App Internal Storage" -> StorageLocation.SL_INTERNAL
            "External directory" -> StorageLocation.SL_EXTERNAL_SAF
            else -> null
        }?.let { storage ->
            settings.setFileHandlerSettingsSync(
                FileHandlerSettings(
                    storage,
                    root,
                    crosswords,
                    archive,
                    toImport,
                    toImportDone,
                    toImportFailed,
                )
            )
        }
    }}}}}}}

    // finally, delete shared prefs
    prefs.edit { clear() } // !
}

private fun getBoolean(prefs : SharedPreferences, key : String) : Boolean? {
    try {
        return if (prefs.contains(key)) prefs.getBoolean(key, false) else null
    } catch (e : ClassCastException) {
        return null
    }
}

private fun getString(prefs : SharedPreferences, key : String) : String? {
    try {
        return if (prefs.contains(key)) prefs.getString(key, null) else null
    } catch (e : ClassCastException) {
        return null
    }
}

private fun getInt(prefs : SharedPreferences, key : String) : Int? {
    try {
        return if (prefs.contains(key)) prefs.getInt(key, 0) else null
    } catch (e : ClassCastException) {
        return null
    }
}

private fun getStringSet(
    prefs : SharedPreferences, key : String
) : Set<String>? {
    try {
        return if (prefs.contains(key)) prefs.getStringSet(key, null) else null
    } catch (e : ClassCastException) {
        return null
    }
}

private fun getLong(prefs : SharedPreferences, key : String) : Long? {
    try {
        return if (prefs.contains(key)) prefs.getLong(key, 0) else null
    } catch (e : ClassCastException) {
        return null
    }
}

private fun getFloat(prefs : SharedPreferences, key : String) : Float? {
    try {
        return if (prefs.contains(key)) prefs.getFloat(key, 0.0F) else null
    } catch (e : ClassCastException) {
        return null
    }
}

