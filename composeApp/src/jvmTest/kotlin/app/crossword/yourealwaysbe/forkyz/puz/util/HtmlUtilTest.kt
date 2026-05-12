
package app.crossword.yourealwaysbe.forkyz.puz.util

import app.crossword.yourealwaysbe.forkyz.puz.util.HtmlUtil.parseHtmlColor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class HtmlUtilTest {
    @Test
    fun testColor() {
        checkColor("#000000", "000000")
        checkColor("black", "000000")
        checkColor("#ffffff", "ffffff")
        checkColor(" WHITe ", "ffffff")
        checkColor("light golden rod", "fafad2")
        checkColor("rgb(255, 0, 51%)", "ff0082")
        checkColor("hsl(64, 35%, 75%)", "d2d5a8")
        checkColor("hsl(120, 64%, 100%)", "ffffff")
        checkColor("hsl(120, 64%, 10%)", "092809")
        // alpha not supported, should be 82 at start of hex
        checkColor("rgba(0, 127, 51%, 0.51)", "007f82")
        checkColor("hsla(64, 35%, 75%, 0.51)", "d2d5a8")
    }

    private fun checkColor(input : String, expected : String) {
        val i = parseHtmlColor(input)
        Assertions.assertEquals(parseHtmlColor(input), argb(expected))
    }

    private fun argb(argb : String) : Int {
        return (argb.toLong(16) + 0).toInt()
    }
}

