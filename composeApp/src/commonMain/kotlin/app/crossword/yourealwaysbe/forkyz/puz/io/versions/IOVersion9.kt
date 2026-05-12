
package app.crossword.yourealwaysbe.forkyz.puz.io.versions

import java.nio.charset.Charset

// Support utf-8
class IOVersion9 : IOVersion8() {
    override val charset = Charset.forName("UTF-8")
}
