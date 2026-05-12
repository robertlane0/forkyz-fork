
package app.crossword.yourealwaysbe.forkyz.exttools

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Playboard
import app.crossword.yourealwaysbe.forkyz.puz.Zone
import app.crossword.yourealwaysbe.forkyz.settings.ExternalDictionarySetting
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings

abstract class ExternalDictionaryData(val word : String) : ExternalToolData()

class FreeDictionaryData(word : String) : ExternalDictionaryData(word) {
    override suspend fun accept(launcher : ExternalToolLauncher) {
        launcher.visit(this)
    }
}

class QuickDictionaryData(word : String) : ExternalDictionaryData(word) {
    override suspend fun accept(launcher : ExternalToolLauncher) {
        launcher.visit(this)
    }
}

class Aard2DictionaryData(word : String) : ExternalDictionaryData(word) {
    override suspend fun accept(launcher : ExternalToolLauncher) {
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
            val zone = board.currentZone
            if (zone == null)
                return

            val puz = board.puzzle

            val request = StringBuilder()
            for (pos in zone) {
                puz.checkedGetBox(pos)?.let {
                    request.append(it.response)
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
expect suspend fun ExternalToolLauncher.visit(data : FreeDictionaryData)
@Throws(AppNotFoundException::class)
expect suspend fun ExternalToolLauncher.visit(data : QuickDictionaryData)
@Throws(AppNotFoundException::class)
expect suspend fun ExternalToolLauncher.visit(data : Aard2DictionaryData)

