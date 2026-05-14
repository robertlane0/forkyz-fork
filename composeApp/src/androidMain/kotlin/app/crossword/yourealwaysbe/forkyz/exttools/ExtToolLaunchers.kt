
package app.crossword.yourealwaysbe.forkyz.exttools

import android.app.SearchManager
import android.content.Intent
import androidx.core.app.ShareCompat
import androidx.core.net.toUri

import org.jetbrains.compose.resources.getString

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.aard2
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.crossword_solver
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.quick_dic
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_puzzle_title
import app.crossword.yourealwaysbe.forkyz.util.toAndroidUri

private val CROSSWORD_SOLVER = "org.billthefarmer.crossword"
private val CROSSWORD_SOLVER_WEBSITE_URL
    = "https://billthefarmer.github.io/crossword";

@Throws(AppNotFoundException::class)
actual suspend fun ExternalToolLauncher.visit(data : CrosswordSolverData) {
    ExternalCaller.launchApp(
        this.activity,
        CROSSWORD_SOLVER,
        data.requestType.uri,
        android.content.Intent.EXTRA_TEXT,
        data.request,
        getString(Res.string.crossword_solver),
        CROSSWORD_SOLVER_WEBSITE_URL,
    )
}

@Throws(AppNotFoundException::class)
actual suspend fun ExternalToolLauncher.visit(data : FreeDictionaryData) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setData(
        "https://www.thefreedictionary.com/${data.word}".toUri()
    )
    this.activity.startActivity(intent)
}

/**
 * Launch or display warning if app not installed
 */
@Throws(AppNotFoundException::class)
actual suspend fun ExternalToolLauncher.visit(data : QuickDictionaryData) {
    ExternalCaller.launchApp(
        this.activity,
        "de.reimardoeffinger.quickdic",
        "com.hughes.android.dictionary.DictionaryActivity",
        "searchToken",
        data.word,
        getString(Res.string.quick_dic),
        "https://github.com/rdoeffinger/Dictionary/releases",
    )
}

/**
 * Launch or display warning if app not installed
 */
@Throws(AppNotFoundException::class)
actual suspend fun ExternalToolLauncher.visit(data : Aard2DictionaryData) {
    ExternalCaller.launchIntent(
        this.activity,
        "aard2.lookup",
        SearchManager.QUERY,
        data.word,
        getString(Res.string.aard2),
        "https://aarddict.org/",
    )
}

@Throws(AppNotFoundException::class)
actual suspend fun ExternalToolLauncher.visit(data : HTMLToolData) {
    val i = Intent(Intent.ACTION_VIEW)
    i.setData(data.uri.toAndroidUri())
    this.activity.startActivity(i)
}

@Throws(AppNotFoundException::class)
actual suspend fun ExternalToolLauncher.visit(data : SharePuzzleData) {
    val title = getString(Res.string.share_puzzle_title)
    val shareIntent = ShareCompat.IntentBuilder(this.activity)
        .setChooserTitle(title)
        .setStream(data.uri.toAndroidUri())
        .setType(data.mimeType)
        .createChooserIntent();
    this.activity.startActivity(shareIntent);
}

@Throws(AppNotFoundException::class)
actual suspend fun ExternalToolLauncher.visit(data : ShareTextData) {
    // ShareCompat from
    // https://stackoverflow.com/a/39619468/6882587
    // assume works better than the out-of-date android docs!
    val shareIntent = ShareCompat.IntentBuilder(this.activity)
        .setText(data.message)
        .setType("text/plain")
        .setChooserTitle(data.title)
        .createChooserIntent();
    this.activity.startActivity(shareIntent);
}

