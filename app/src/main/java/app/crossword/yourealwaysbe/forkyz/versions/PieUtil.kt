
package app.crossword.yourealwaysbe.forkyz.versions

import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(api = Build.VERSION_CODES.P)
open class PieUtil  : OreoUtil() {
    override val semiBoldTypeface : Typeface
        get() = Typeface.create(Typeface.SANS_SERIF, 600, false)
}

