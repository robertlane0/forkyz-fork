/**
 * Representation of configured keys for binding with parser
 */

package app.crossword.yourealwaysbe.forkyz.util

enum class ForkyzKeySymbol(
    val text : String,
) {
    ZERO("0"),
    ONE("1"),
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    SIX("6"),
    SEVEN("7"),
    EIGHT("8"),
    NINE("9"),
    A("A"),
    APOSTROPHE("APOSTROPHE"),
    AT("AT"),
    B("B"),
    BACK("BACK"),
    BACKSLASH("BACKSLASH"),
    BACKSPACE("BACKSPACE"),
    BOOKMARK("BOOKMARK"),
    BREAK("BREAK"),
    C("C"),
    CAPS_LOCK("CAPSLOCK"),
    COMMA("COMMA"),
    D("D"),
    DELETE("DELETE"),
    DOWN("DOWN"),
    E("E"),
    END("END"),
    ENTER("ENTER"),
    EQUALS("EQUALS"),
    ESCAPE("ESCAPE"),
    F("F"),
    F1("F1"),
    F10("F10"),
    F11("F11"),
    F12("F12"),
    F2("F2"),
    F3("F3"),
    F4("F4"),
    F5("F5"),
    F6("F6"),
    F7("F7"),
    F8("F8"),
    F9("F9"),
    FORWARD("FORWARD"),
    G("G"),
    GRAVE("GRAVE"),
    H("H"),
    HOME("HOME"),
    I("I"),
    INSERT("INSERT"),
    J("J"),
    K("K"),
    L("L"),
    LEFT("LEFT"),
    LEFT_BRACKET("LEFTBRACKET"),
    M("M"),
    MENU("MENU"),
    MINUS("MINUS"),
    N("N"),
    O("O"),
    P("P"),
    PAGE_DOWN("PAGEDOWN"),
    PAGE_UP("PAGEUP"),
    PERIOD("PERIOD"),
    PLUS("PLUS"),
    POUND("POUND"),
    PRINT_SCREEN("PRINTSCREEN"),
    Q("Q"),
    R("R"),
    RIGHT("RIGHT"),
    RIGHT_BRACKET("RIGHTBRACKET"),
    S("S"),
    SEMI_COLON("SEMICOLON"),
    SPACE("SPACE"),
    T("T"),
    TAB("TAB"),
    U("U"),
    UP("UP"),
    V("V"),
    W("W"),
    X("X"),
    Y("Y"),
    Z("Z"),
}

private val LOOKUP_TABLE : Map<String, ForkyzKeySymbol>
    = ForkyzKeySymbol.values().map { it.text to it }.toMap()

private val SHIFT = "S"
private val CONTROL = "C"
private val ALT = "A"
private val META = "M"
private val MODIFIER_SEP = "-"

data class ForkyzKey(
    val symbol : ForkyzKeySymbol,
    val alt : Boolean = false,
    val control : Boolean = false,
    val meta : Boolean = false,
    val shift : Boolean = false,
) {
    companion object {
        fun parseKey(description : String) : ForkyzKey? {
            val splitDescription = description.split(MODIFIER_SEP)
            if (splitDescription.isEmpty())
                return null

            val modifiers = splitDescription
                .dropLast(1)
                .map { it.uppercase() }
                .toSet()

            return LOOKUP_TABLE
                .get(splitDescription.last().uppercase())?.let { symbol ->
                    ForkyzKey(
                        symbol,
                        modifiers.contains(ALT),
                        modifiers.contains(CONTROL),
                        modifiers.contains(META),
                        modifiers.contains(SHIFT),
                    )
                }
        }
    }

    val text : String
        get() {
            val sb = StringBuilder()
            if (alt) {
                sb.append(ALT)
                sb.append(MODIFIER_SEP)
            }
            if (control) {
                sb.append(CONTROL)
                sb.append(MODIFIER_SEP)
            }
            if (shift) {
                sb.append(SHIFT)
                sb.append(MODIFIER_SEP)
            }
            if (meta) {
                sb.append(META)
                sb.append(MODIFIER_SEP)
            }
            sb.append(symbol.text)
            return sb.toString()
        }
}

fun fkey(symbol : ForkyzKeySymbol) : ForkyzKey = ForkyzKey(symbol)

fun fctrl(symbol : ForkyzKeySymbol) : ForkyzKey = ForkyzKey(symbol, control = true)

