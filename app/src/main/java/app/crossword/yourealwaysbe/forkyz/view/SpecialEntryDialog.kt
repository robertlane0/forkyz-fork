
package app.crossword.yourealwaysbe.forkyz.view

import javax.inject.Inject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import dagger.hilt.android.AndroidEntryPoint

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper

// Move to compose function when all puzzle activities compose
@AndroidEntryPoint
class SpecialEntryDialog() : DialogFragment() {

    @Inject
    lateinit var themeHelper : ThemeHelper

    override fun onCreateView(
        inflater : LayoutInflater,
        container : ViewGroup?,
        savedInstanceState : Bundle?,
    ) : View? {
        val viewModel = ViewModelProvider(requireActivity())
            .get(SpecialEntryDialogViewModel::class.java)
        return ComposeView(requireActivity()).apply {
            setContent {
                themeHelper.AppTheme() {
                    val response by viewModel.response.collectAsStateWithLifecycle()
                    OKCancelDialog(
                        title = R.string.special_entry,
                        summary = R.string.enter_special_desc,
                        onCancel = { dismiss() },
                        onOK = {
                            viewModel.playResponse(response)
                            dismiss()
                        },
                    ) {
                        OutlinedTextField(
                            label = {
                                Text(
                                    stringResource(R.string.special_character)
                                )
                            },
                            value = response,
                            onValueChange = viewModel::setResponse,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
