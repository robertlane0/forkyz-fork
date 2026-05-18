
package app.crossword.yourealwaysbe.forkyz.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.forkyz.util.WordsToNumbers.convertTextualNumbersInDocument

class WordsToNumbersTest {
    @Test
    fun testNoNumbers() {
        assertEquals("hello world", convertTextualNumbersInDocument("hello world"))
    }

    @Test
    fun testEmptyString() {
        assertEquals("", convertTextualNumbersInDocument(""))
    }

    @Test
    fun testSingleDigitWords() {
        assertEquals("0", convertTextualNumbersInDocument("zero"))
        assertEquals("1", convertTextualNumbersInDocument("one"))
        assertEquals("5", convertTextualNumbersInDocument("five"))
        assertEquals("9", convertTextualNumbersInDocument("nine"))
    }

    @Test
    fun testTeens() {
        assertEquals("10", convertTextualNumbersInDocument("ten"))
        assertEquals("11", convertTextualNumbersInDocument("eleven"))
        assertEquals("12", convertTextualNumbersInDocument("twelve"))
        assertEquals("13", convertTextualNumbersInDocument("thirteen"))
        assertEquals("19", convertTextualNumbersInDocument("nineteen"))
    }

    @Test
    fun testTens() {
        assertEquals("20", convertTextualNumbersInDocument("twenty"))
        assertEquals("30", convertTextualNumbersInDocument("thirty"))
        assertEquals("90", convertTextualNumbersInDocument("ninety"))
    }

    @Test
    fun testTwoWordNumber() {
        assertEquals("25", convertTextualNumbersInDocument("twenty five"))
    }

    @Test
    fun testHundred() {
        assertEquals("100", convertTextualNumbersInDocument("one hundred"))
        assertEquals("500", convertTextualNumbersInDocument("five hundred"))
    }

    @Test
    fun testHundredsAndTens() {
        assertEquals(
            "523",
            convertTextualNumbersInDocument("five hundred twenty three"),
        )
    }

    @Test
    fun testHundredsWithAnd() {
        assertEquals(
            "502",
            convertTextualNumbersInDocument("five hundred and two"),
        )
    }

    @Test
    fun testThousand() {
        assertEquals("1000", convertTextualNumbersInDocument("one thousand"))
        assertEquals("2000", convertTextualNumbersInDocument("two thousand"))
    }

    @Test
    fun testThousandsWithRemainder() {
        assertEquals(
            "2050",
            convertTextualNumbersInDocument("two thousand and fifty"),
        )
        assertEquals(
            "1234",
            convertTextualNumbersInDocument(
                "one thousand two hundred thirty four",
            ),
        )
    }

    @Test
    fun testMillion() {
        assertEquals(
            "1000000",
            convertTextualNumbersInDocument("one million"),
        )
    }

    @Test
    fun testBillion() {
        assertEquals(
            "1000000000",
            convertTextualNumbersInDocument("one billion"),
        )
    }

    @Test
    fun testTrillion() {
        assertEquals(
            "1000000000000",
            convertTextualNumbersInDocument("one trillion"),
        )
    }

    @Test
    fun testNumberInSentence() {
        assertEquals(
            "there were 25 coins",
            convertTextualNumbersInDocument("there were twenty five coins"),
        )
    }

    @Test
    fun testNumberAtStartOfSentence() {
        assertEquals(
            "25 coins",
            convertTextualNumbersInDocument("twenty five coins"),
        )
    }

    @Test
    fun testNumberAtEndOfSentence() {
        assertEquals(
            "there are 25",
            convertTextualNumbersInDocument("there are twenty five"),
        )
    }

    @Test
    fun testMultipleNumbersInSentence() {
        assertEquals(
            "I have 2 cats and 5 dogs",
            convertTextualNumbersInDocument("I have two cats and five dogs"),
        )
    }

    @Test
    fun testHyphenatedNumber() {
        assertEquals("25", convertTextualNumbersInDocument("twenty-five"))
    }

    @Test
    fun testHyphenatedNumberInSentence() {
        assertEquals(
            "there were 25 coins",
            convertTextualNumbersInDocument("there were twenty-five coins"),
        )
    }

    @Test
    fun testCaseInsensitive() {
        assertEquals("25", convertTextualNumbersInDocument("Twenty Five"))
        assertEquals("25", convertTextualNumbersInDocument("TWENTY FIVE"))
    }

    @Test
    fun testTrailingPeriod() {
        assertEquals("25.", convertTextualNumbersInDocument("twenty five."))
    }

    @Test
    fun testTrailingCommaInSentence() {
        assertEquals(
            "there are 25,",
            convertTextualNumbersInDocument("there are twenty five,"),
        )
    }

    @Test
    fun testTrailingExclamationMark() {
        assertEquals("25!", convertTextualNumbersInDocument("twenty five!"))
    }

    @Test
    fun testLeadingParenthesis() {
        assertEquals("(25", convertTextualNumbersInDocument("(twenty five"))
    }

    @Test
    fun testWrappingParentheses() {
        assertEquals("(72)", convertTextualNumbersInDocument("(seventy two)"))
    }

    @Test
    fun testHyphenatedWithTrailingComma() {
        assertEquals(
            "there were 25, coins",
            convertTextualNumbersInDocument("there were twenty-five, coins"),
        )
    }

    @Test
    fun testNumberEndingWithPeriodInSentence() {
        assertEquals(
            "I ate 3. Done.",
            convertTextualNumbersInDocument("I ate three. Done."),
        )
    }

    @Test
    fun testStandaloneAndNotConverted() {
        assertEquals("and", convertTextualNumbersInDocument("and"))
    }

    @Test
    fun testAndAtEndOfNumberGroupRestoredToOutput() {
        assertEquals("1 and", convertTextualNumbersInDocument("one and"))
    }

    @Test
    fun testAndBetweenNonNumbers() {
        assertEquals(
            "cats and dogs",
            convertTextualNumbersInDocument("cats and dogs"),
        )
    }
}
