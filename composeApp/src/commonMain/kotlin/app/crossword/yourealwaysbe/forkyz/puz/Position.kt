
package app.crossword.yourealwaysbe.forkyz.puz

data class Position(var row : Int, var col : Int) {
    override fun toString() : String {
        return "(" + this.row+ ", " + this.col + ")"
    }
}

