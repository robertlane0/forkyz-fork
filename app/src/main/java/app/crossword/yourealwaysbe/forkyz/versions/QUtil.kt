
package app.crossword.yourealwaysbe.forkyz.versions

import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(api = Build.VERSION_CODES.Q)
open class QUtil  : PieUtil() {
    override val filterClueToAlphabeticRegex : String
        get() = "&[^;]*;|<[^>]*>|[^\\p{IsAlphabetic}]"
}

