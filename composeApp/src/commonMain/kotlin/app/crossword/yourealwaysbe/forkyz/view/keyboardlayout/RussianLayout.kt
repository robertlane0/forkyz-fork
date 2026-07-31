
package app.crossword.yourealwaysbe.forkyz.view.keyboardlayout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_delete
import app.crossword.yourealwaysbe.forkyz.view.OrdinaryKey

// Standard ЙЦУКЕН layout covering all 33 Russian letters
@Composable
internal fun KeyboardLayoutScope.RussianKeyboard() {
    Column(modifier.fillMaxWidth()) {
        Row() {
            val m1 = Modifier.weight(1.0F)
            StringKey(m1, "Й")
            StringKey(m1, "Ц")
            StringKey(m1, "У")
            StringKey(m1, "К")
            StringKey(m1, "Е")
            StringKey(m1, "Н")
            StringKey(m1, "Г")
            StringKey(m1, "Ш")
            StringKey(m1, "Щ")
            StringKey(m1, "З")
            StringKey(m1, "Х")
            StringKey(m1, "Ъ")
        }
        Row() {
            val m1 = Modifier.weight(1.0F)
            StringKey(m1, "Ф")
            StringKey(m1, "Ы")
            StringKey(m1, "В")
            StringKey(m1, "А")
            StringKey(m1, "П")
            StringKey(m1, "Р")
            StringKey(m1, "О")
            StringKey(m1, "Л")
            StringKey(m1, "Д")
            StringKey(m1, "Ж")
            StringKey(m1, "Э")
            StringKey(m1, "Ё")
        }
        Row() {
            val m1 = Modifier.weight(1.0F)
            val m15 = Modifier.weight(1.5F)
            ChangeDirectionSpecialEntryKey(m15)
            StringKey(m1, "Я")
            StringKey(m1, "Ч")
            StringKey(m1, "С")
            StringKey(m1, "М")
            StringKey(m1, "И")
            StringKey(m1, "Т")
            StringKey(m1, "Ь")
            StringKey(m1, "Б")
            StringKey(m1, "Ю")
            OrdinaryIconKey(
                m15,
                OrdinaryKey.KEY_DELETE,
                Res.drawable.delete,
                Res.string.key_delete,
            )
        }
        SpaceRow()
    }
}
