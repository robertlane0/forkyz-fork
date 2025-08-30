
package app.crossword.yourealwaysbe.forkyz.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper

@Composable
fun SpecialEntryDialog(
    viewModel : SpecialEntryDialogViewModel,
    onClose : () -> Unit,
) {
    val response by viewModel.response.collectAsStateWithLifecycle()
    val forceCaps by viewModel.forceCaps.collectAsStateWithLifecycle()
    OKCancelDialog(
        title = R.string.special_entry,
        summary = R.string.enter_special_desc,
        onCancel = { onClose() },
        onOK = {
            viewModel.playResponse()
            onClose()
        },
    ) {
        OutlinedTextField(
            label = {
                Text(stringResource(R.string.special_character))
            },
            value = response,
            onValueChange = viewModel::setResponse,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                modifier = Modifier.padding(end = 10.dp),
                checked = forceCaps,
                onCheckedChange = viewModel::setForceCaps,
            )
            Text(
                stringResource(
                    R.string.special_entry_force_caps
                ),
            )
        }
    }
}
