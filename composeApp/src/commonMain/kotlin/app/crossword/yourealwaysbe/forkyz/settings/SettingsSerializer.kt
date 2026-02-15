
package app.crossword.yourealwaysbe.forkyz.settings

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

import okio.BufferedSink
import okio.BufferedSource
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.DataMigration

import app.crossword.yourealwaysbe.forkyz.net.DownloaderID

class SettingsSerializer(
    val defaultSettings : Settings,
) : OkioSerializer<Settings> {
    override val defaultValue : Settings = defaultSettings

    override suspend fun readFrom(source : BufferedSource) : Settings {
        try {
            return Settings.ADAPTER.decode(source)
        } catch (exception: IOException) {
            throw Exception("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t : Settings, sink : BufferedSink) {
        sink.write(t.encode())
    }
}

val CURRENT_SETTINGS_VERSION = 2

/**
 * Migration to convert downloadX and scrapeX booleans to disabledDownloaders set
 */
class DownloaderBooleanToSetMigration : DataMigration<Settings> {
    companion object {
        const val MIGRATION_VERSION = 1
    }

    override suspend fun shouldMigrate(currentData: Settings): Boolean {
        return currentData.settingsVersion < MIGRATION_VERSION
    }

    override suspend fun migrate(currentData: Settings): Settings {
        // Migrate downloadX and scrapeX booleans to disabledDownloaders set
        val disabled = mutableSetOf<String>()

        if (!currentData.download20Minutes)
            disabled.add(DownloaderID.TWENTY_MINUTES.id)
        if (!currentData.downloadCustomDaily)
          disabled.add(DownloaderID.CUSTOM.id)
        if (!currentData.downloadDeStandaard)
          disabled.add(DownloaderID.DE_STANDAARD.id)
        if (!currentData.downloadDeTelegraaf)
          disabled.add(DownloaderID.DE_TELEGRAAF.id)
        if (!currentData.downloadElPaisExperto)
          disabled.add(DownloaderID.EL_PAIS_EXPERTO.id)
        if (!currentData.downloadGuardianDailyCryptic)
          disabled.add(DownloaderID.GUARDIAN_DAILY_CRYPTIC.id)
        if (!currentData.downloadGuardianWeeklyQuiptic)
          disabled.add(DownloaderID.GUARDIAN_WEEKLY_QUIPTIC.id)
        if (!currentData.downloadHamAbend)
          disabled.add(DownloaderID.HAM_ABEND.id)
        if (!currentData.downloadIndependentDailyCryptic)
          disabled.add(DownloaderID.INDEPENDENT_DAILY_CRYPTIC.id)
        if (!currentData.downloadIrishNewsCryptic)
          disabled.add(DownloaderID.IRISH_NEWS_CRYPTIC.id)
        if (!currentData.downloadJonesin)
          disabled.add(DownloaderID.JONESIN.id)
        if (!currentData.downloadJoseph)
          disabled.add(DownloaderID.JOSEPH.id)
        if (!currentData.downloadLaCroix)
          disabled.add(DownloaderID.LA_CROIX.id)
        if (!currentData.downloadLATimes)
          disabled.add(DownloaderID.LA_TIMES.id)
        if (!currentData.downloadLeParisienF1)
          disabled.add(DownloaderID.LE_PARISIEN_F1.id)
        if (!currentData.downloadLeParisienF2)
          disabled.add(DownloaderID.LE_PARISIEN_F2.id)
        if (!currentData.downloadLeParisienF3)
          disabled.add(DownloaderID.LE_PARISIEN_F3.id)
        if (!currentData.downloadLeParisienF4)
          disabled.add(DownloaderID.LE_PARISIEN_F4.id)
        if (!currentData.downloadMetroCryptic)
          disabled.add(DownloaderID.METRO_CRYPTIC.id)
        if (!currentData.downloadMetroQuick)
          disabled.add(DownloaderID.METRO_QUICK.id)
        if (!currentData.downloadNewsday)
          disabled.add(DownloaderID.NEWSDAY.id)
        if (!currentData.downloadNewYorkTimesSyndicated)
          disabled.add(DownloaderID.NYT_SYNDICATED.id)
        if (!currentData.downloadNRCInHetMidden)
          disabled.add(DownloaderID.NRC_IN_HET_MIDDEN.id)
        if (!currentData.downloadNRCScrypto)
          disabled.add(DownloaderID.NRC_SCRYPTO.id)
        if (!currentData.downloadPremier)
          disabled.add(DownloaderID.PREMIER.id)
        if (!currentData.downloadSheffer)
          disabled.add(DownloaderID.SHEFFER.id)
        if (!currentData.downloadSudOuestMotsCroises)
          disabled.add(DownloaderID.SUD_OUEST_MOTS_CROISES.id)
        if (!currentData.downloadSudOuestMotsFleches)
          disabled.add(DownloaderID.SUD_OUEST_MOTS_FLECHES.id)
        if (!currentData.downloadTF1MotsCroises)
          disabled.add(DownloaderID.TF1_MOTS_CROISES.id)
        if (!currentData.downloadTF1MotsFleches)
          disabled.add(DownloaderID.TF1_MOTS_FLECHES.id)
        if (!currentData.downloadUniversal)
          disabled.add(DownloaderID.UNIVERSAL.id)
        if (!currentData.downloadUSAToday)
          disabled.add(DownloaderID.USA_TODAY.id)
        if (!currentData.downloadWaPoSunday)
          disabled.add(DownloaderID.WAPO_SUNDAY.id)
        if (!currentData.downloadWsj)
          disabled.add(DownloaderID.WSJ.id)

        if (!currentData.scrapeCru)
          disabled.add(DownloaderID.CRYPTIC_CRU.id)
        if (!currentData.scrapeGuardianQuick)
          disabled.add(DownloaderID.GUARDIAN_QUICK.id)
        if (!currentData.scrapeKegler)
          disabled.add(DownloaderID.KEGLAR.id)
        if (!currentData.scrapePrivateEye)
          disabled.add(DownloaderID.PRIVATE_EYE.id)
        if (!currentData.scrapePrzekroj)
          disabled.add(DownloaderID.PRZEKROJ.id)

        // Clear old boolean fields by setting them to proto3 default (false)
        return currentData.copy(
            disabledDownloaders = disabled.toList(),
            settingsVersion = MIGRATION_VERSION,
            download20Minutes = false,
            downloadCustomDaily = false,
            downloadDeStandaard = false,
            downloadDeTelegraaf = false,
            downloadElPaisExperto = false,
            downloadGuardianDailyCryptic = false,
            downloadGuardianWeeklyQuiptic = false,
            downloadHamAbend = false,
            downloadIndependentDailyCryptic = false,
            downloadIrishNewsCryptic = false,
            downloadJonesin = false,
            downloadJoseph = false,
            downloadLaCroix = false,
            downloadLATimes = false,
            downloadLeParisienF1 = false,
            downloadLeParisienF2 = false,
            downloadLeParisienF3 = false,
            downloadLeParisienF4 = false,
            downloadMetroCryptic = false,
            downloadMetroQuick = false,
            downloadNewsday = false,
            downloadNewYorkTimesSyndicated = false,
            downloadNRCInHetMidden = false,
            downloadNRCScrypto = false,
            downloadPremier = false,
            downloadSheffer = false,
            downloadSudOuestMotsCroises = false,
            downloadSudOuestMotsFleches = false,
            downloadTF1MotsCroises = false,
            downloadTF1MotsFleches = false,
            downloadUniversal = false,
            downloadUSAToday = false,
            downloadWaPoSunday = false,
            downloadWsj = false,
            scrapeCru = false,
            scrapeGuardianQuick = false,
            scrapeKegler = false,
            scrapePrivateEye = false,
            scrapePrzekroj = false,
        )
    }

    override suspend fun cleanUp() {
        // Nothing to do
    }
}

/**
 * Migration to convert autoDownloaders to autoDownloadersBlock<Day>
 */
class AutoDownloadersToDayBlocksMigration : DataMigration<Settings> {
    companion object {
        const val MIGRATION_VERSION = 2
    }

    override suspend fun shouldMigrate(currentData: Settings): Boolean {
        return currentData.settingsVersion < MIGRATION_VERSION
    }

    override suspend fun migrate(currentData: Settings): Settings {
        val block = mutableSetOf<String>()

        val isAutoDownloadEnabled = (
            currentData.backgroundDownloadHourly
            || currentData.dlOnStartup
            || currentData.backgroundDownloadDays.size > 0
        )

        if (isAutoDownloadEnabled) {
            for (downloaderID in DownloaderID.values()) {
                val id = downloaderID.id
                if (
                    id !in currentData.disabledDownloaders
                    && id !in currentData.autoDownloaders
                ) {
                    block.add(id)
                }
            }
        }

        // Clear autoDownloaders, setup blocks
        return currentData.copy(
            autoDownloaders = listOf(),
            autoDownloadersBlockMon = block.toList(),
            autoDownloadersBlockTue = block.toList(),
            autoDownloadersBlockWed = block.toList(),
            autoDownloadersBlockThu = block.toList(),
            autoDownloadersBlockFri = block.toList(),
            autoDownloadersBlockSat = block.toList(),
            autoDownloadersBlockSun = block.toList(),
            settingsVersion = MIGRATION_VERSION,
        )
    }

    override suspend fun cleanUp() {
        // Nothing to do
    }
}

