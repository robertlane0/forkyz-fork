
package app.crossword.yourealwaysbe.forkyz.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import org.jetbrains.compose.resources.stringResource

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.filter_settings_close

@Composable
fun SearchBar(
    showSearchBar : Boolean,
    searchTerm : String,
    onValueChange : (String) -> Unit,
    onClose : () -> Unit,
    hint : String,
) {
    if (showSearchBar) {
        // needs immediate updates:
        // https://medium.com/androiddevelopers/effective-state-management-for-textfield-in-compose-d6e5b070fbe5
        var localSearchTerm by remember(searchTerm) {
            mutableStateOf(searchTerm)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                label = { Text(hint) },
                value = localSearchTerm,
                onValueChange = {
                    localSearchTerm = it
                    onValueChange(it)
                },
                modifier = Modifier.weight(1.0f)
                    .padding(4.dp),
            )
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Filled.Close,
                    stringResource(Res.string.filter_settings_close),
                )
            }
        }
    }
}


