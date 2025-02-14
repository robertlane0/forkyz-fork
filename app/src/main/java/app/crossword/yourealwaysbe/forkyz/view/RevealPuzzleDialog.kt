
package app.crossword.yourealwaysbe.forkyz.view

import javax.inject.Inject

import android.app.Dialog
import android.content.DialogInterface;
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import dagger.hilt.android.AndroidEntryPoint

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder

@AndroidEntryPoint
class RevealPuzzleDialog() : DialogFragment() {

    @Inject
    lateinit var currentPuzzleHolder : CurrentPuzzleHolder

    override fun onCreateDialog(savedInstanceState : Bundle?) : Dialog {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(getString(R.string.reveal_puzzle))
            .setMessage(getString(R.string.are_you_sure))
            .setPositiveButton(
                R.string.ok,
                { _, _ -> currentPuzzleHolder.board?.revealPuzzle() },
            )
            .setNegativeButton(
                R.string.cancel,
                { d, _ -> d?.dismiss() },
            )

        return builder.create();
    }
}

