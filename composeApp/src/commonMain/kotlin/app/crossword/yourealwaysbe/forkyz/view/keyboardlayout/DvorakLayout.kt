
package app.crossword.yourealwaysbe.forkyz.view.keyboardlayout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.key_delete
import app.crossword.yourealwaysbe.forkyz.view.OrdinaryKey

@Composable
internal fun KeyboardLayoutScope.DvorakKeyboard() {
    Column(modifier.fillMaxWidth()) {
        Row() {
            val m1 = Modifier.weight(1.0F)
            val m2 = Modifier.weight(2.0F)
            val m3 = Modifier.weight(3.0F)
            OrdinaryIconKey(
                m3,
                OrdinaryKey.KEY_DELETE,
                Res.drawable.delete,
                Res.string.key_delete,
            )
            ChangeDirectionSpecialEntryKey(m2)
            StringKey(m2, "P")
            StringKey(m2, "Y")
            StringKey(m2, "F")
            StringKey(m2, "G")
            StringKey(m2, "C")
            StringKey(m2, "R")
            StringKey(m2, "L")
        }
        Row() {
            val m1 = Modifier.weight(1.0F)
            StringKey(m1, "A")
            StringKey(m1, "O")
            StringKey(m1, "E")
            StringKey(m1, "U")
            StringKey(m1, "I")
            StringKey(m1, "D")
            StringKey(m1, "H")
            StringKey(m1, "T")
            StringKey(m1, "N")
            StringKey(m1, "S")
        }
        Row() {
            val m1 = Modifier.weight(1.0F)
            Box(m1)
            StringKey(m1, "Q")
            StringKey(m1, "J")
            StringKey(m1, "K")
            StringKey(m1, "X")
            StringKey(m1, "B")
            StringKey(m1, "M")
            StringKey(m1, "W")
            StringKey(m1, "V")
            StringKey(m1, "Z")
        }
        SpaceRow()
    }
}
