
package app.crossword.yourealwaysbe.forkyz.view;

import javax.inject.Inject;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.core.content.ContextCompat;
import androidx.core.widget.CheckedTextViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.databinding.ChooseFlagColorDialogBinding;
import app.crossword.yourealwaysbe.forkyz.databinding.FlagColorItemBinding;
import app.crossword.yourealwaysbe.forkyz.util.ColorUtils;
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Dialog to choose the flag color for a clue.
 */
@AndroidEntryPoint
public class ChooseFlagColorDialog extends DialogFragment {
    private static final String ARG_CLUE_LIST_NAME = "listName";
    private static final String ARG_CLUE_INDEX = "clueIndex";
    private static final String ARG_POS_ROW = "posRow";
    private static final String ARG_POS_COL = "posCol";

    @Inject
    protected CurrentPuzzleHolder currentPuzzleHolder;

    private ChooseFlagColorDialogBinding binding;
    RecyclerView.Adapter<FlagColorHolder> adapter;
    private String[] colorOptions;
    private int selectedColorIndex = 0;

    /**
     * Either add a clue or a cell before showing
     */
    public static void addClueIDToBundle(Bundle bundle, ClueID cid) {
        bundle.putString(ARG_CLUE_LIST_NAME, cid.getListName());
        bundle.putInt(ARG_CLUE_INDEX, cid.getIndex());
    }

    /**
     * Either add a clue or a cell position before showing
     */
    public static void addBoxPositionToBundle(Bundle bundle, Position pos) {
        bundle.putInt(ARG_POS_ROW, pos.getRow());
        bundle.putInt(ARG_POS_COL, pos.getCol());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        Puzzle puz = getPuzzle();
        if (activity == null || puz == null)
            return null;

        Bundle args = getArguments();
        Clue clue = puz.getClue(getClueIDFromBundle(args));
        Position pos = getBoxPositionFromBundle(args);

        if (clue == null && pos == null)
            return null;

        binding = ChooseFlagColorDialogBinding.inflate(
            activity.getLayoutInflater()
        );

        setColorOptions(clue, pos);

        MaterialAlertDialogBuilder builder
            = new MaterialAlertDialogBuilder(activity);

        builder.setTitle(R.string.flag_color_title)
            .setView(binding.getRoot());

        builder.setPositiveButton(
                R.string.ok,
                (dialog, which) -> {
                    int color = getColor(selectedColorIndex);
                    Playboard board = getBoard();
                    if (board != null) {
                        if (clue != null) {
                            board.flagClue(clue, true);
                            board.setFlagColor(clue, color);
                        }
                        if (pos != null) {
                            board.flagPosition(pos, true);
                            board.setFlagColor(pos, color);
                        }
                    }
                }
            )
            .setNegativeButton(
                R.string.cancel, (dialog, which) -> { dialog.cancel(); }
            );

        return builder.create();
    }

    private Playboard getBoard() {
        return currentPuzzleHolder.getBoard();
    }

    private Puzzle getPuzzle() {
        Playboard board = getBoard();
        if (board == null)
            return null;
        return board.getPuzzle();
    }

    /**
     * Set color options based on passed clue/pos
     *
     * Clue/pos can be null (preferably not both!)
     */
    private void setColorOptions(Clue clue, Position pos) {
        colorOptions = getResources().getStringArray(
            R.array.flag_color_labels
        );

        int argColor = getColor(0);
        if (clue != null) {
            argColor = clue.getFlagColor();
        } else if (pos != null) {
            Puzzle puz = getPuzzle();
            Box box = (puz == null) ? null : puz.checkedGetBox(pos);
            if (box != null)
                argColor = box.getFlagColor();
        }

        for (int i = 0; i < colorOptions.length; i++) {
            if (getColor(i) == argColor) {
                selectedColorIndex = i;
                break;
            }
        }

        LinearLayoutManager layoutManager
            = new LinearLayoutManager(getContext());
        binding.flagColorsList.setLayoutManager(layoutManager);
        adapter = new RecyclerView.Adapter<FlagColorHolder>() {
            @Override
            public FlagColorHolder onCreateViewHolder(
                ViewGroup parent, int viewType
            ) {
                LayoutInflater inflater
                    = (LayoutInflater) parent.getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                FlagColorItemBinding itemBinding
                    = FlagColorItemBinding.inflate(inflater, parent, false);
                return new FlagColorHolder(itemBinding);
            }

            @Override
            public void onBindViewHolder(
                FlagColorHolder holder, int position
            ) {
                holder.setIndex(position);
            }

            @Override
            public int getItemCount() { return colorOptions.length; }
        };
        binding.flagColorsList.setAdapter(adapter);
    }

    private void setSelectedColor(int index) {
        if (index >= 0 && index < colorOptions.length) {
            int oldSelectedIndex = selectedColorIndex;
            selectedColorIndex = index;
            if (adapter != null) {
                adapter.notifyItemChanged(oldSelectedIndex);
                adapter.notifyItemChanged(index);
            }
        }
    }

    private int getColor(int index) {
        switch (index) {
        case 1:
            return getContextColor(R.color.flagColor1);
        case 2:
            return getContextColor(R.color.flagColor2);
        case 3:
            return getContextColor(R.color.flagColor3);
        case 4:
            return getContextColor(R.color.flagColor4);
        default:
            return Clue.DEFAULT_FLAG_COLOR;
        }
    }

    /**
     * Get color from context and remove alpha channel
     */
    private int getContextColor(int id) {
        return ColorUtils.delAlpha(ContextCompat.getColor(getContext(), id));
    }

    /**
     * Get supplied clue ID or null
     */
    private ClueID getClueIDFromBundle(Bundle bundle) {
        boolean hasClueID
            = bundle.containsKey(ARG_CLUE_LIST_NAME)
                && bundle.containsKey(ARG_CLUE_INDEX);
        if (!hasClueID)
            return null;

        String listName = bundle.getString(ARG_CLUE_LIST_NAME);
        int index = bundle.getInt(ARG_CLUE_INDEX);
        return new ClueID(listName, index);
    }

    private Position getBoxPositionFromBundle(Bundle bundle) {
        boolean hasBoxPosition
            = bundle.containsKey(ARG_POS_ROW)
                && bundle.containsKey(ARG_POS_COL);
        if (!hasBoxPosition)
            return null;

        int row = bundle.getInt(ARG_POS_ROW);
        int col = bundle.getInt(ARG_POS_COL);
        return new Position(row, col);
    }

    private class FlagColorHolder extends RecyclerView.ViewHolder {
        private FlagColorItemBinding itemBinding;
        private int index;

        public FlagColorHolder(FlagColorItemBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
            itemBinding.getRoot().setOnClickListener(v -> {
                setSelectedColor(index);
            });
        }

        public void setIndex(int index) {
            if (index < 0 || index >= colorOptions.length)
                return;

            this.index = index;

            itemBinding.flagColorView.setText(colorOptions[index]);
            itemBinding.flagColorView.setChecked(
                selectedColorIndex == index
            );

            int color = getColor(index);
            ColorStateList csl = ColorStateList.valueOf(
                (color == Clue.DEFAULT_FLAG_COLOR)
                    ? ContextCompat.getColor(getContext(), R.color.flagColor)
                    : ColorUtils.addAlpha(color)
            );
            CheckedTextViewCompat.setCheckMarkTintList(
                itemBinding.flagColorView, csl
            );
        }
    }
}
