
package app.crossword.yourealwaysbe.forkyz.view;

import javax.inject.Inject;

import android.app.Dialog;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.databinding.ClueEditDialogBinding;
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Dialog to insert special characters into board
 *
 * Pass the board to the constructor. The dialog will play the special letter
 * when entered.
 */
@AndroidEntryPoint
public class ClueEditDialog extends DialogFragment {
    /**
     * Start with args for Clue ID to edit
     */
    public static final String CLUE_LISTNAME = "clueListName";
    public static final String CLUE_INDEX = "clueIndex";

    @Inject
    protected CurrentPuzzleHolder currentPuzzleHolder;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ClueEditDialogBinding binding =
            ClueEditDialogBinding.inflate(getLayoutInflater());

        MaterialAlertDialogBuilder builder
            = new MaterialAlertDialogBuilder(getActivity());

        builder.setTitle(getString(R.string.edit_clue))
            .setView(binding.getRoot());

        Playboard board = currentPuzzleHolder.getBoard();
        final Clue clue = getClueToEdit();
        if (board == null || clue == null)
            return builder.create();

        String hint = clue.getHint();
        if (hint != null)
            binding.clueHint.setText(hint);

        builder.setPositiveButton(
            R.string.ok,
            (di, i) -> {
                board.editClue(
                    clue,
                    binding.clueHint.getText().toString()
                );
            }
        ).setNegativeButton(R.string.cancel, null);

        return builder.create();
    }

    private Clue getClueToEdit() {
        Playboard board = currentPuzzleHolder.getBoard();
        Puzzle puz = board == null ? null : board.getPuzzle();
        if (puz == null)
            return null;

        Bundle args = getArguments();
        String listName = args.getString(CLUE_LISTNAME);
        int index = args.getInt(CLUE_INDEX, -1);

        if (listName == null || index < 0)
            return null;

        return puz.getClue(new ClueID(listName, index));
   }
}
