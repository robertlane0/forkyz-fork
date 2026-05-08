
package app.crossword.yourealwaysbe.forkyz.util

actual class View

class JVMInputConnectionMediator() : InputConnectionMediator() {
    override fun showNative(forceCaps : Boolean) {
        // no native on JVM
    }

    override fun hideNative() {
        // no native on JVM
    }
}

