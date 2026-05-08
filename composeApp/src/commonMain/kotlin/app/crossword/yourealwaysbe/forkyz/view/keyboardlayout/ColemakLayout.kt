
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
internal fun KeyboardLayoutScope.ColemakKeyboard() {
    Column(modifier.fillMaxWidth()) {
        Row() {
            val m1 = Modifier.weight(1.0F)
            StringKey(m1, "Q")
            StringKey(m1, "W")
            StringKey(m1, "F")
            StringKey(m1, "P")
            StringKey(m1, "G")
            StringKey(m1, "J")
            StringKey(m1, "L")
            StringKey(m1, "U")
            StringKey(m1, "Y")
            Box(m1)
        }
        Row() {
            val m1 = Modifier.weight(1.0F)
            StringKey(m1, "A")
            StringKey(m1, "R")
            StringKey(m1, "S")
            StringKey(m1, "T")
            StringKey(m1, "D")
            StringKey(m1, "H")
            StringKey(m1, "N")
            StringKey(m1, "E")
            StringKey(m1, "I")
            StringKey(m1, "O")
        }
        Row() {
            val m1 = Modifier.weight(1.0F)
            val m2 = Modifier.weight(2.0F)
            val m3 = Modifier.weight(3.0F)
            ChangeDirectionSpecialEntryKey(m2)
            Box(m1)
            StringKey(m2, "Z")
            StringKey(m2, "X")
            StringKey(m2, "C")
            StringKey(m2, "V")
            StringKey(m2, "B")
            StringKey(m2, "K")
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
