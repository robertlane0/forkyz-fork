
package app.crossword.yourealwaysbe.forkyz.exttools

import android.content.Intent

import app.crossword.yourealwaysbe.forkyz.util.Uri

open class HTMLToolData(val uri : Uri) : ExternalToolData() {
    override fun accept(launcher : ExternalToolLauncher) {
        launcher.visit(this)
    }
}

fun ExternalToolLauncher.visit(data : HTMLToolData) {
    val i = Intent(Intent.ACTION_VIEW)
    i.setData(data.uri.toAndroidUri())
    this.activity.startActivity(i)
}
