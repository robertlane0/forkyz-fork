
package app.crossword.yourealwaysbe.forkyz.tools

import android.content.Intent
import android.net.Uri

open class HTMLToolData(val uri : Uri) : ExternalToolData() {
    override fun accept(launcher : ExternalToolLauncher) {
        launcher.visit(this)
    }
}

fun ExternalToolLauncher.visit(data : HTMLToolData) {
    val i = Intent(Intent.ACTION_VIEW)
    i.setData(data.uri)
    this.activity.startActivity(i)
}
