
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import app.crossword.yourealwaysbe.forkyz.R

@Composable
fun SearchBar(
    showSearchBar : Boolean,
    searchTerm : String,
    onValueChange : (String) -> Unit,
    onClose : () -> Unit,
    hint : String,
) {
    if (showSearchBar) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                label = {
                    Text(hint)
                },
                value = searchTerm,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1.0f)
                    .padding(4.dp),
            )
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Filled.Close,
                    stringResource(R.string.filter_settings_close),
                )
            }
        }
    }
}


