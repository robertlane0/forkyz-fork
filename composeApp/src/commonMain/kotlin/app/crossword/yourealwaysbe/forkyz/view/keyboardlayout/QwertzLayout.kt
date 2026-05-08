
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
internal fun KeyboardLayoutScope.QwertzKeyboard() {
    Column(modifier.fillMaxWidth()) {
        Row() {
            val m1 = Modifier.weight(1.0F)
            StringKey(m1, "Q")
            StringKey(m1, "W")
            StringKey(m1, "E")
            StringKey(m1, "R")
            StringKey(m1, "T")
            StringKey(m1, "Z")
            StringKey(m1, "U")
            StringKey(m1, "I")
            StringKey(m1, "O")
            StringKey(m1, "P")
        }
        Row() {
            val m1 = Modifier.weight(1.0F)
            val m2 = Modifier.weight(2.0F)
            Box(m1)
            StringKey(m2, "A")
            StringKey(m2, "S")
            StringKey(m2, "D")
            StringKey(m2, "F")
            StringKey(m2, "G")
            StringKey(m2, "H")
            StringKey(m2, "J")
            StringKey(m2, "K")
            StringKey(m2, "L")
            Box(m1)
        }
        Row() {
            val m1 = Modifier.weight(1.0F)
            val m2 = Modifier.weight(2.0F)
            val m3 = Modifier.weight(3.0F)
            ChangeDirectionSpecialEntryKey(m2)
            Box(m1)
            StringKey(m2, "Y")
            StringKey(m2, "X")
            StringKey(m2, "C")
            StringKey(m2, "V")
            StringKey(m2, "B")
            StringKey(m2, "N")
            StringKey(m2, "M")
            OrdinaryIconKey(
                m3,
                OrdinaryKey.KEY_DELETE,
                Res.drawable.delete,
                Res.string.key_delete,
            )
        }
        SpaceRow()
    }
}
