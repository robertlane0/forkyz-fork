
package app.crossword.yourealwaysbe.forkyz.exttools

import java.util.Locale
import java.util.function.Consumer

import android.app.SearchManager
import android.content.Intent
import androidx.core.net.toUri

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.ExternalDictionarySetting
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Zone

private val FREE_URL_FORMAT = "https://www.thefreedictionary.com/%s"

abstract class ExternalDictionaryData(val word : String) : ExternalToolData()

class FreeDictionaryData(word : String) : ExternalDictionaryData(word) {
    override fun accept(launcher : ExternalToolLauncher) {
        launcher.visit(this)
    }
}

class QuickDictionaryData(word : String) : ExternalDictionaryData(word) {
    override fun accept(launcher : ExternalToolLauncher) {
        launcher.visit(this)
    }
}

class Aard2DictionaryData(word : String) : ExternalDictionaryData(word) {
    override fun accept(launcher : ExternalToolLauncher) {
        launcher.visit(this)
    }
}

class ExternalDictionaries {
    companion object {
        /**
         * Dictionary chosen in settings to search current word on board
         *
         * @param cb call back with value (may not call back if none
         * configured
         */
        fun build(
            settings : ForkyzSettings,
            board : Playboard,
            cb : Consumer<ExternalDictionaryData>,
        ) {
            val zone = board.getCurrentZone()
            if (zone == null)
                return

            val puz = board.getPuzzle()
            if (puz == null)
                return

            val request = StringBuilder()
            for (pos in zone) {
                puz.checkedGetBox(pos)?.let {
                    request.append(it.getResponse())
                }
            }

            build(settings, request.toString(), cb)
        }


        fun build(
            settings : ForkyzSettings?,
            word : String?,
            cb : Consumer<ExternalDictionaryData>?,
        ) {
            word?.let { word ->
                settings?.getExtDictionarySetting() {
                    when (it) {
                        ExternalDictionarySetting.EDS_NONE
                            -> null
                        ExternalDictionarySetting.EDS_QUICK
                            -> QuickDictionaryData(word)
                        ExternalDictionarySetting.EDS_AARD2
                            -> Aard2DictionaryData(word)
                        else -> FreeDictionaryData(word)
                    }?.let { cb?.accept(it) }
                }
            }
        }
    }
}

@Throws(AppNotFoundException::class)
fun ExternalToolLauncher.visit(data : FreeDictionaryData) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setData(
        String.format(
            Locale.getDefault(),
            FREE_URL_FORMAT,
            data.word,
        ).toUri()
    )
    this.activity.startActivity(intent)
}

/**
 * Launch or display warning if app not installed
 */
@Throws(AppNotFoundException::class)
fun ExternalToolLauncher.visit(data : QuickDictionaryData) {
    ExternalCaller.launchApp(
        this.activity,
        "de.reimardoeffinger.quickdic",
        "com.hughes.android.dictionary.DictionaryActivity",
        "searchToken",
        data.word,
        this.activity.getString(R.string.quick_dic),
        "https://github.com/rdoeffinger/Dictionary/releases",
    )
}

/**
 * Launch or display warning if app not installed
 */
@Throws(AppNotFoundException::class)
fun ExternalToolLauncher.visit(data : Aard2DictionaryData) {
    ExternalCaller.launchIntent(
        this.activity,
        "aard2.lookup",
        SearchManager.QUERY,
        data.word,
        this.activity.getString(R.string.aard2),
        "https://aarddict.org/",
    )
}
