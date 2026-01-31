
package app.crossword.yourealwaysbe.forkyz.exttools

import app.crossword.yourealwaysbe.forkyz.util.Uri

open class HTMLToolData(val uri : Uri) : ExternalToolData() {
    override suspend fun accept(launcher : ExternalToolLauncher) {
        launcher.visit(this)
    }
}

@Throws(AppNotFoundException::class)
expect suspend fun ExternalToolLauncher.visit(data : HTMLToolData)

