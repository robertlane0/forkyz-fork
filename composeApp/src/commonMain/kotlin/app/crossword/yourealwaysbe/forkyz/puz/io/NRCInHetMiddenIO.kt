
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder.BasicClue

/**
 * Parser specifically for the "In Het Midden" crossword of the NRC newspaper.
 *
 * The difference with a normal crossword is that there is a special horizontal
 * word which has to be found (the "middenwoord"). The affected cells are
 * highlighted in the input (using a "#" sign). This class injects the
 * middenwoord as an additional clue that can be filled out. This is not
 * strictly necessary as it is made up from the letters of other (vertical)
 * words, but it is more satisfactory for puzzlers who figure the middenwoord
 * out from partial information when they can actually fill it out. The
 * middenwoord might be broken up in multiple words, separated by a highlighted
 * block. In this case, all middenwoord words are added as separate clues.
 */
class NRCInHetMiddenIO : FortyTwoPuzzlesIO() {
    private var inMiddenWoord = false
    private val middenWoordStartBoxes = ArrayList<Box>()

    var middenwoordHint : String = "MIDDENWOORD"

    @Throws(Exception::class)
    override suspend fun parsePuzzle(input : Source) : PuzzleBuilder? {
        val builder = super.parsePuzzle(input)
        if (builder == null)
            return null

        // If there's just one middenwoord woord, relabel the clue from "m1" to
        // just "m".
        if (
            middenWoordStartBoxes.size == 1
            && middenWoordStartBoxes.get(0).clueNumber == "m1"
        ) {
            builder.getNumberPositions().get("m1")?.let { pos ->
                builder.setBoxClueNumber(pos.row, pos.col, "m")
            }
        }

        // Inject the middenwoord clues
        for (box in middenWoordStartBoxes) {
            box.clueNumber?.let { clueNumber ->
                val clue = BasicClue(clueNumber, middenwoordHint)
                builder.addAcrossClue(
                    DefaultListNames.acrossList(),
                    clue,
                )
            }
        }

        return builder
    }

    override fun createBox(value : String, solution : String) : Box {
        // 42games marks the "middenwoord" as highlighted boxes, using a # sign
        var isMiddenWoord = value.startsWith("#")

        val box = super.createBox(value, solution)

        if (inMiddenWoord && box.isBlock) {
            // We encountered a block inside the middenwoord, which means we're
            // in a situation where the middenwoord consists of multiple words,
            // and we just left one of them. So mark that we're not in the
            // middenwoord (anymore), so the next cycle detects that we entered
            // the middenwoord.
            isMiddenWoord = false
            inMiddenWoord = false
        }

        if (isMiddenWoord) {
            // If this is the first cell of the middenwoord and there is no
            // clue number assigned from a vertical word, we have to assign it
            // a special clue "number", which we'll label "m" plus the word
            // number.
            if (!inMiddenWoord) {
                middenWoordStartBoxes.add(box)
                if (box.clueNumber?.isEmpty() ?: true) {
                    box.clueNumber = "m" + middenWoordStartBoxes.size
                }
            }
            inMiddenWoord = true
        }

        return box
    }
}

