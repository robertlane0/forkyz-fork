
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.io.RawSource
import kotlinx.io.asSource

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.parseSource
import com.fleeksoft.ksoup.parser.Parser

fun parseXMLElement(source : RawSource) : XMLElement {
    return XMLElement(Ksoup.parseSource(source, parser = Parser.xmlParser()))
}

class XMLElement(private val element : Element) {
    fun select(tag : String) : List<XMLElement> {
        return element.select(tag).map(::XMLElement)
    }

    fun selectFirst(tag : String) : XMLElement? {
        return element.selectFirst(tag)?.let { XMLElement(it) }
    }

    fun hasElement(tag : String) : Boolean {
        return selectFirst(tag) != null
    }

    /**
     * Inner HTML of element
     */
    fun html() : String {
        return element.html()
    }

    /**
     * Inner text with tags removed
     */
    fun text() : String {
        return element.text()
    }

    /**
     * Get attribute, or empty string
     */
    fun attr(key : String) : String {
        return element.attr(key)
    }

    /**
     * Get attribute or null
     */
    fun attrNull(key : String) : String? {
        return if (element.hasAttr(key)) element.attr(key) else null
    }
}

