
package app.crossword.yourealwaysbe.forkyz.util

actual class View

class IOSInputConnectionMediator() : InputConnectionMediator() {
    override fun showNative(forceCaps : Boolean, removePunctuation : Boolean) {
        // no native keyboard management on iOS via this path
    }

    override fun hideNative() {
        // no native keyboard management on iOS via this path
    }
}
