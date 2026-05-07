
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
internal fun KeyboardLayoutScope.AzertyKeyboard() {
    Column(modifier.fillMaxWidth()) {
        Row() {
            val m1 = Modifier.weight(1.0F)
            StringKey(m1, "A")
            StringKey(m1, "Z")
            StringKey(m1, "E")
            StringKey(m1, "R")
            StringKey(m1, "T")
            StringKey(m1, "Y")
            StringKey(m1, "U")
            StringKey(m1, "I")
            StringKey(m1, "O")
            StringKey(m1, "P")
        }
        Row() {
            val m1 = Modifier.weight(1.0F)
            StringKey(m1, "Q")
            StringKey(m1, "S")
            StringKey(m1, "D")
            StringKey(m1, "F")
            StringKey(m1, "G")
            StringKey(m1, "H")
            StringKey(m1, "J")
            StringKey(m1, "K")
            StringKey(m1, "L")
            StringKey(m1, "M")
        }
        Row() {
            val m1 = Modifier.weight(1.0F)
            val m2 = Modifier.weight(1.5F)
            ChangeDirectionSpecialEntryKey(m2)
            StringKey(m1, "W")
            StringKey(m1, "X")
            StringKey(m1, "C")
            StringKey(m1, "V")
            StringKey(m1, "B")
            StringKey(m1, "N")
            Box(m1)
            OrdinaryIconKey(
                m2,
                OrdinaryKey.KEY_DELETE,
                Res.drawable.delete,
                Res.string.key_delete,
            )
        }
        SpaceRow()
    }
}
