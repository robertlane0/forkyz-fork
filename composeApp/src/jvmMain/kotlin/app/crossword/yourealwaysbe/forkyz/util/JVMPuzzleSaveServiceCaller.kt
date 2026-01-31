
package app.crossword.yourealwaysbe.forkyz.util

class JVMPuzzleSaveServiceCaller : PuzzleSaveServiceCaller {
    override fun saveBoard(currentPuzzleHolder : CurrentPuzzleHolder) {
        currentPuzzleHolder.saveBoardService()
    }
}

