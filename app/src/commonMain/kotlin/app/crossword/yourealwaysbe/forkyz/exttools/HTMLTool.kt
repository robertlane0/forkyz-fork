
package app.crossword.yourealwaysbe.forkyz.exttools

import app.crossword.yourealwaysbe.forkyz.util.Uri

open class HTMLToolData(val uri : Uri) : ExternalToolData() {
    override fun accept(launcher : ExternalToolLauncher) {
        launcher.visit(this)
    }
}

expect fun ExternalToolLauncher.visit(data : HTMLToolData)

