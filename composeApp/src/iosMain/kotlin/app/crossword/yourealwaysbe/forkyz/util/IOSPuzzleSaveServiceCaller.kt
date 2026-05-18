
package app.crossword.yourealwaysbe.forkyz.util

class IOSPuzzleSaveServiceCaller : PuzzleSaveServiceCaller {
    override fun saveBoard(currentPuzzleHolder : CurrentPuzzleHolder) {
        currentPuzzleHolder.saveBoardService()
    }
}
