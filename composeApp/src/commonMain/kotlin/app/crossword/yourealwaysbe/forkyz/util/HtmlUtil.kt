
package app.crossword.yourealwaysbe.forkyz.util

import kotlin.math.max
import kotlin.math.min

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Entities
import com.fleeksoft.ksoup.safety.Safelist

object HtmlUtil {
    // IPuz tags not to strip from HTML (preserve line breaks)
    private val JSOUP_CLEAN_WHITELIST = Safelist()
    init {
        JSOUP_CLEAN_WHITELIST.addTags("br")
    }

    // all the colors from
    // https://www.w3schools.com/colors/colors_names.asp
    private val namedColors = mapOf<String, Int>(
        "aliceblue" to rgb("f0f8ff"),
        "antiquewhite" to rgb("faebd7"),
        "aqua" to rgb("00ffff"),
        "aquamarine" to rgb("7fffd4"),
        "azure" to rgb("f0ffff"),
        "beige" to rgb("f5f5dc"),
        "bisque" to rgb("ffe4c4"),
        "black" to rgb("000000"),
        "blanchedalmond" to rgb("ffebcd"),
        "blue" to rgb("0000ff"),
        "blueviolet" to rgb("8a2be2"),
        "brown" to rgb("a52a2a"),
        "burlywood" to rgb("deb887"),
        "cadetblue" to rgb("5f9ea0"),
        "chartreuse" to rgb("7fff00"),
        "chocolate" to rgb("d2691e"),
        "coral" to rgb("ff7f50"),
        "cornflowerblue" to rgb("6495ed"),
        "cornsilk" to rgb("fff8dc"),
        "crimson" to rgb("dc143c"),
        "cyan" to rgb("00ffff"),
        "darkblue" to rgb("00008b"),
        "darkcyan" to rgb("008b8b"),
        "darkgoldenrod" to rgb("b8860b"),
        "darkgray" to rgb("a9a9a9"),
        "darkgrey" to rgb("a9a9a9"),
        "darkgreen" to rgb("006400"),
        "darkkhaki" to rgb("bdb76b"),
        "darkmagenta" to rgb("8b008b"),
        "darkolivegreen" to rgb("556b2f"),
        "darkorange" to rgb("ff8c00"),
        "darkorchid" to rgb("9932cc"),
        "darkred" to rgb("8b0000"),
        "darksalmon" to rgb("e9967a"),
        "darkseagreen" to rgb("8fbc8f"),
        "darkslateblue" to rgb("483d8b"),
        "darkslategray" to rgb("2f4f4f"),
        "darkslategrey" to rgb("2f4f4f"),
        "darkturquoise" to rgb("00ced1"),
        "darkviolet" to rgb("9400d3"),
        "deeppink" to rgb("ff1493"),
        "deepskyblue" to rgb("00bfff"),
        "dimgray" to rgb("696969"),
        "dimgrey" to rgb("696969"),
        "dodgerblue" to rgb("1e90ff"),
        "firebrick" to rgb("b22222"),
        "floralwhite" to rgb("fffaf0"),
        "forestgreen" to rgb("228b22"),
        "fuchsia" to rgb("ff00ff"),
        "gainsboro" to rgb("dcdcdc"),
        "ghostwhite" to rgb("f8f8ff"),
        "gold" to rgb("ffd700"),
        "goldenrod" to rgb("daa520"),
        "gray" to rgb("808080"),
        "grey" to rgb("808080"),
        "green" to rgb("008000"),
        "greenyellow" to rgb("adff2f"),
        "honeydew" to rgb("f0fff0"),
        "hotpink" to rgb("ff69b4"),
        "indianred" to rgb("cd5c5c"),
        "indigo" to rgb("4b0082"),
        "ivory" to rgb("fffff0"),
        "khaki" to rgb("f0e68c"),
        "lavender" to rgb("e6e6fa"),
        "lavenderblush" to rgb("fff0f5"),
        "lawngreen" to rgb("7cfc00"),
        "lemonchiffon" to rgb("fffacd"),
        "lightblue" to rgb("add8e6"),
        "lightcoral" to rgb("f08080"),
        "lightcyan" to rgb("e0ffff"),
        "lightgoldenrodyellow" to rgb("fafad2"),
        "lightgray" to rgb("d3d3d3"),
        "lightgrey" to rgb("d3d3d3"),
        "lightgreen" to rgb("90ee90"),
        "lightpink" to rgb("ffb6c1"),
        "lightsalmon" to rgb("ffa07a"),
        "lightseagreen" to rgb("20b2aa"),
        "lightskyblue" to rgb("87cefa"),
        "lightslategray" to rgb("778899"),
        "lightslategrey" to rgb("778899"),
        "lightsteelblue" to rgb("b0c4de"),
        "lightyellow" to rgb("ffffe0"),
        "lime" to rgb("00ff00"),
        "limegreen" to rgb("32cd32"),
        "linen" to rgb("faf0e6"),
        "magenta" to rgb("ff00ff"),
        "maroon" to rgb("800000"),
        "mediumaquamarine" to rgb("66cdaa"),
        "mediumblue" to rgb("0000cd"),
        "mediumorchid" to rgb("ba55d3"),
        "mediumpurple" to rgb("9370db"),
        "mediumseagreen" to rgb("3cb371"),
        "mediumslateblue" to rgb("7b68ee"),
        "mediumspringgreen" to rgb("00fa9a"),
        "mediumturquoise" to rgb("48d1cc"),
        "mediumvioletred" to rgb("c71585"),
        "midnightblue" to rgb("191970"),
        "mintcream" to rgb("f5fffa"),
        "mistyrose" to rgb("ffe4e1"),
        "moccasin" to rgb("ffe4b5"),
        "navajowhite" to rgb("ffdead"),
        "navy" to rgb("000080"),
        "oldlace" to rgb("fdf5e6"),
        "olive" to rgb("808000"),
        "olivedrab" to rgb("6b8e23"),
        "orange" to rgb("ffa500"),
        "orangered" to rgb("ff4500"),
        "orchid" to rgb("da70d6"),
        "palegoldenrod" to rgb("eee8aa"),
        "palegreen" to rgb("98fb98"),
        "paleturquoise" to rgb("afeeee"),
        "palevioletred" to rgb("db7093"),
        "papayawhip" to rgb("ffefd5"),
        "peachpuff" to rgb("ffdab9"),
        "peru" to rgb("cd853f"),
        "pink" to rgb("ffc0cb"),
        "plum" to rgb("dda0dd"),
        "powderblue" to rgb("b0e0e6"),
        "purple" to rgb("800080"),
        "rebeccapurple" to rgb("663399"),
        "red" to rgb("ff0000"),
        "rosybrown" to rgb("bc8f8f"),
        "royalblue" to rgb("4169e1"),
        "saddlebrown" to rgb("8b4513"),
        "salmon" to rgb("fa8072"),
        "sandybrown" to rgb("f4a460"),
        "seagreen" to rgb("2e8b57"),
        "seashell" to rgb("fff5ee"),
        "sienna" to rgb("a0522d"),
        "silver" to rgb("c0c0c0"),
        "skyblue" to rgb("87ceeb"),
        "slateblue" to rgb("6a5acd"),
        "slategray" to rgb("708090"),
        "slategrey" to rgb("708090"),
        "snow" to rgb("fffafa"),
        "springgreen" to rgb("00ff7f"),
        "steelblue" to rgb("4682b4"),
        "tan" to rgb("d2b48c"),
        "teal" to rgb("008080"),
        "thistle" to rgb("d8bfd8"),
        "tomato" to rgb("ff6347"),
        "turquoise" to rgb("40e0d0"),
        "violet" to rgb("ee82ee"),
        "wheat" to rgb("f5deb3"),
        "white" to rgb("ffffff"),
        "whitesmoke" to rgb("f5f5f5"),
        "yellow" to rgb("ffff00"),
        "yellowgreen" to rgb("9acd32"),
    )

    /**
     * Remove IPuz HTML from a string
     * @return decoded string or null if value was null
     */
    fun unHtmlString(value : String?) : String? {
        if (value == null)
            return null

        // this is a bit hacky: any break tag is normalised to "\r?\n<br>"
        // by the clean method, we remove the \r\ns and turn <br> into \n
        return Entities.unescape(
            Ksoup.clean(value, JSOUP_CLEAN_WHITELIST)
                .replace("\r", "")
                .replace("\n", "")
                .replace("<br>", "\n")
        )
    }

    /**
     * Return IPuz HTML encoding of string
     * @return encoded string or null if value was null
     */
    fun htmlString(value : String?) : String? {
        if (value == null)
            return null

        return Entities.escape(value)
            .replace("\r", "")
            .replace("\n", "<br/>")
            .replace("  ", " &nbsp;")
    }

    /**
     * Parse a colour as if it was html
     *
     * Ignores alpha channels since not supported by IPuz (our backend
     * format). Color always positive or 0 if success. I.e. 0x00rrggbb.
     */
    fun parseHtmlColor(color : String?) : Int {
        var color = color
        if (color == null || color.isEmpty())
            return -1

        color = color.trim().lowercase()

        if (color.startsWith("#")) {
            try {
                if (color.length > 4) {
                    return rgb(color.substring(1))
                } else if (color.length == 4) {
                    val fullColor = (
                        ""
                        + color.get(1) + color.get(1)
                        + color.get(2) + color.get(2)
                        + color.get(3) + color.get(3)
                    )
                    return rgb(fullColor)
                } else {
                    return -1
                }
            } catch (e : NumberFormatException) {
                return -1
            }
        } else if (color.startsWith("rgb")) {
            // rgb or rgba
            try {
                val args = getFunArgs(color)
                if (args == null || args.size < 3)
                    return -1

                val red = HtmlUtil.parse255String(args[0])
                val green = HtmlUtil.parse255String(args[1])
                val blue = HtmlUtil.parse255String(args[2])

                return (red shl 16) or (green shl 8) or (blue shl 0)
            } catch (e : NumberFormatException) {
                return -1
            }
        } else if (color.startsWith("hsl")) {
            // hsl or hsla
            try {
                val args = getFunArgs(color)
                if (args == null || args.size < 3)
                    return -1

                val hue = max(0, min(360, args[0].toInt()))
                val sat = HtmlUtil.parse255String(args[1])
                val light = HtmlUtil.parse255String(args[2])

                return hslToRgb(hue, sat, light)
            } catch (e : NumberFormatException) {
                return -1
            }
        } else {
            color = color.replace("\\s".toRegex(), "")
            val intColor = namedColors.get(color)
            if (intColor != null)
                return intColor

            // be really generous, search for prefixes if failed.
            for (entry in namedColors.entries) {
                if (entry.key.startsWith(color))
                    return entry.value
            }
            return -1
        }
    }

    /**
     * Extracts values for args in fun(blah, blah, blah)
     *
     * Returns as many args as there are. Returns null if something goes
     * wrong.
     */
    private fun getFunArgs(color : String?) : Array<String>? {
        if (color == null)
            return null

        val start = color.indexOf("(")
        val end = color.indexOf(")")
        if (start < 0 || end < 0) return null

        return color.substring(start + 1, end)
            .split(",".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()
    }

    /**
     * Turns a value into 0-255
     *
     * Either 0-255 or x% of 255.
     */
    private fun parse255String(value : String) : Int {
        var iValue = 0
        val value = value.trim()
        if (value.endsWith("%")) {
            val num = value.substring(0, value.length - 1)
            iValue = (num.toInt() / 100.0 * 255).toInt()
        } else {
            iValue = value.toInt()
        }

        return iValue and 0xff
    }

    /**
     * Convert HSL to RGBA int
     *
     * hue should be 0-360
     * sat, light, alpha 0-255
     * Algorithm from https://en.wikipedia.org/wiki/HSL_and_HSV
     */
    private fun hslToRgb(hue : Int, sat : Int, light : Int) : Int {
        val red = hslFun(0, hue, sat, light)
        val green = hslFun(8, hue, sat, light)
        val blue = hslFun(4, hue, sat, light)
        return (red shl 16) or (green shl 8) or (blue shl 0)
    }

    /**
     * The f from rgb = (f(0), f(8), f(4)) on Wiki
     *
     * See hslToRgba.
     *
     * Returns 0-255. Limits to 0-255 if something is wrong.
     */
    private fun hslFun(n : Int, hue : Int, sat : Int, light : Int) : Int {
        val h = hue.toDouble()
        val s = sat / 255.0
        val l = light / 255.0

        val a = s * min(l, 1 - l)
        val k = (n + h / 30.0) % 12.0

        val f = l - a * max(-1.0, min(k - 3.0, min(9.0 - k, 1.0)))

        return ((f * 255).toInt()) and 0xff
    }

    /**
     * Convert bbbbbb to int color with full alpha
     */
    private fun rgb(rgb : String) : Int {
        return rgb.toInt(16)
    }
}

