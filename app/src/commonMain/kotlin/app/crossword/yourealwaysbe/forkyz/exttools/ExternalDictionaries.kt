
package app.crossword.yourealwaysbe.forkyz.exttools

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.ExternalDictionarySetting
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Zone

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
            cb : (ExternalDictionaryData) -> Unit,
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
            cb : (ExternalDictionaryData) -> Unit,
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
                    }?.let { cb(it) }
                }
            }
        }
    }
}

@Throws(AppNotFoundException::class)
expect fun ExternalToolLauncher.visit(data : FreeDictionaryData)
@Throws(AppNotFoundException::class)
expect fun ExternalToolLauncher.visit(data : QuickDictionaryData)
@Throws(AppNotFoundException::class)
expect fun ExternalToolLauncher.visit(data : Aard2DictionaryData)

