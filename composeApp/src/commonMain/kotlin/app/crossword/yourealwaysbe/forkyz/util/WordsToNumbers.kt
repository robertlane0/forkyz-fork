/**
 * Port to Kotlin of WordsToNumbersUtil
 * By jgraham0325
 * https://github.com/jgraham0325/words-to-numbers
 * Original License: MIT
 */

package app.crossword.yourealwaysbe.forkyz.util

/**
 * Testing algorithm to convert words in paragraph representing numbers into
 * standard digit representation
 *
 * Note that currently all formatting and occurrences of "and" are getting
 * stripped out. Not needed for current purposes
 *
 * e.g. Input: "There were twenty five coins" Output: "there were 25 coins"
 *
 */
object WordsToNumbers {
    val allowedStrings = setOf(
        "and", "zero", "one", "two", "three", "four", "five", "six",
        "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen",
        "fourteen", "fifteen", "sixteen", "seventeen", "eighteen",
        "nineteen", "twenty", "thirty", "forty", "fifty", "sixty",
        "seventy", "eighty", "ninety", "hundred", "thousand", "million",
        "billion", "trillion"
    )

    /**
     * Main driver method. Converts textual numbers (e.g. twenty five) to
     * integers (e.g. 25)
     *
     * Does not currently cater for decimal points. e.g. "five point two"
     *
     * @param inputText
     */
    fun convertTextualNumbersInDocument(inputText : String) : String {

        // splits text into words and deals with hyphenated numbers. Use linked
        // list due to manipulation during processing
        var words = cleanAndTokenizeText(inputText)

        // replace all the textual numbers
        words = replaceTextualNumbers(words)

        // put spaces back in and return the string. Should be the same as input
        // text except from textual numbers
        return wordListToString(words)
    }

    /**
     * Does the replacement of textual numbers, processing each word at a time
     * and grouping them before doing the conversion
     *
     * @param words
     * @return
     */
    private fun replaceTextualNumbers(wordsList : List<String>) : List<String> {

        val words = wordsList.toMutableList()

        // holds each group of textual numbers being processed together. e.g.
        // "one" or "five hundred and two"
        val processingList = mutableListOf<String>()

        var i = 0
        while (i < words.size || !processingList.isEmpty()) {

            // caters for sentences only containing one word (that is a number)
            var word = ""
            if (i < words.size) {
                word = words.get(i)
            }

            // strip word of all punctuation to make it easier to process
            var wordStripped = word.replace("[^a-zA-Z\\s]", "").lowercase()

            // 2nd condition: skip "and" words by themselves and at start of num
            if (
                allowedStrings.contains(wordStripped)
                && !(
                    processingList.size == 0
                    && wordStripped.equals("and")
                )
            ) {
                words.removeAt(i) // remove from main list, will process later
                processingList.add(word)
            } else if (processingList.size > 0) {
                // found end of group of textual words to process

                //if "and" is the last word, add it back in to original list
                val lastProcessedWord
                    = processingList.get(processingList.size - 1)
                if (lastProcessedWord.equals("and")) {
                    words.add(i, "and")
                    processingList.removeAt(processingList.size - 1)
                }

                // main logic here, does the actual conversion
                var wordAsDigits = convertWordsToNum(processingList).toString()
                wordAsDigits = retainPunctuation(processingList, wordAsDigits)
                words.add(i, wordAsDigits)

                processingList.clear()
                i += 2
            } else {
                i++
            }
        }

        return words
    }

    /**
     * Retain punctuation at the start and end of a textual number.
     *
     * e.g. (seventy two) -> (72)
     *
     * @param processingList
     * @param wordAsDigits
     * @return
     */
    private fun retainPunctuation(
        processingList : List<String>,
        wordAsDigits : String,
    ) : String {
        var result = wordAsDigits

        val lastWord = processingList.get(processingList.size - 1)
        val lastChar = lastWord.trim().get(lastWord.length - 1)
        if (!lastChar.isLetter()) {
            result += lastChar
        }

        val firstWord = processingList.get(0)
        val firstChar = firstWord.trim().get(0)
        if (!firstChar.isLetter()) {
            result = firstChar + result
        }

        return result
    }

    /**
     * Splits up hyphenated textual words. e.g. twenty-two -> twenty two
     *
     * @param sentence
     * @return
     */
    private fun cleanAndTokenizeText(sentence : String) : List<String> {
        val words = sentence.split(" ").toMutableList()

        // remove hyphenated textual numbers
        for (i in 0 until words.size) {
            val str = words.get(i)
            if (str.contains("-")) {
                val splitWords = str.split("-")

                // just check the first word is a textual number. Caters for
                // "twenty-five," without having to strip the comma
                if (
                    splitWords.size > 1
                    && allowedStrings.contains(splitWords.get(0))
                ) {
                    words.removeAt(i)
                    words.addAll(i, splitWords)
                }
            }

        }

        return words
    }

    /**
     * Creates string including spaces from a list of words
     *
     * @param list
     * @return
     */
    private fun wordListToString(list : List<String>) : String {
        val result = StringBuilder()
        for (i in 0 until list.size) {
            val str = list.get(i)
            if (i == 0) {
                result.append(list.get(i))
            } else {
                result.append(" " + list.get(i))
            }
        }

        return result.toString()
    }

    /**
     * Logic for taking a textual number string and converting it into a number
     * e.g. twenty five -> 25
     *
     * This relies on there only being one textual number being processed. Steps
     * prior to this deal with breaking a paragraph down into individual textual
     * numbers, which could consist of a number of words.
     *
     * @param input
     * @return
     */
    private fun convertWordsToNum(words : List<String>) : Long {
        var finalResult = 0L
        var intermediateResult = 0L
        for (strOrig in words) {
            // clean up string for easier processing
            val str = strOrig.lowercase().replace("[^a-zA-Z\\s]", "")
            if (str.equals("zero", ignoreCase = true)) {
                intermediateResult += 0
            } else if (str.equals("one", ignoreCase = true)) {
                intermediateResult += 1
            } else if (str.equals("two", ignoreCase = true)) {
                intermediateResult += 2
            } else if (str.equals("three", ignoreCase = true)) {
                intermediateResult += 3
            } else if (str.equals("four", ignoreCase = true)) {
                intermediateResult += 4
            } else if (str.equals("five", ignoreCase = true)) {
                intermediateResult += 5
            } else if (str.equals("six", ignoreCase = true)) {
                intermediateResult += 6
            } else if (str.equals("seven", ignoreCase = true)) {
                intermediateResult += 7
            } else if (str.equals("eight", ignoreCase = true)) {
                intermediateResult += 8
            } else if (str.equals("nine", ignoreCase = true)) {
                intermediateResult += 9
            } else if (str.equals("ten", ignoreCase = true)) {
                intermediateResult += 10
            } else if (str.equals("eleven", ignoreCase = true)) {
                intermediateResult += 11
            } else if (str.equals("twelve", ignoreCase = true)) {
                intermediateResult += 12
            } else if (str.equals("thirteen", ignoreCase = true)) {
                intermediateResult += 13
            } else if (str.equals("fourteen", ignoreCase = true)) {
                intermediateResult += 14
            } else if (str.equals("fifteen", ignoreCase = true)) {
                intermediateResult += 15
            } else if (str.equals("sixteen", ignoreCase = true)) {
                intermediateResult += 16
            } else if (str.equals("seventeen", ignoreCase = true)) {
                intermediateResult += 17
            } else if (str.equals("eighteen", ignoreCase = true)) {
                intermediateResult += 18
            } else if (str.equals("nineteen", ignoreCase = true)) {
                intermediateResult += 19
            } else if (str.equals("twenty", ignoreCase = true)) {
                intermediateResult += 20
            } else if (str.equals("thirty", ignoreCase = true)) {
                intermediateResult += 30
            } else if (str.equals("forty", ignoreCase = true)) {
                intermediateResult += 40
            } else if (str.equals("fifty", ignoreCase = true)) {
                intermediateResult += 50
            } else if (str.equals("sixty", ignoreCase = true)) {
                intermediateResult += 60
            } else if (str.equals("seventy", ignoreCase = true)) {
                intermediateResult += 70
            } else if (str.equals("eighty", ignoreCase = true)) {
                intermediateResult += 80
            } else if (str.equals("ninety", ignoreCase = true)) {
                intermediateResult += 90
            } else if (str.equals("hundred", ignoreCase = true)) {
                intermediateResult *= 100
            } else if (str.equals("thousand", ignoreCase = true)) {
                intermediateResult *= 1000
                finalResult += intermediateResult
                intermediateResult = 0
            } else if (str.equals("million", ignoreCase = true)) {
                intermediateResult *= 1000000
                finalResult += intermediateResult
                intermediateResult = 0
            } else if (str.equals("billion", ignoreCase = true)) {
                intermediateResult *= 1000000000
                finalResult += intermediateResult
                intermediateResult = 0
            } else if (str.equals("trillion", ignoreCase = true)) {
                intermediateResult *= 1000000000000L
                finalResult += intermediateResult
                intermediateResult = 0
            }
        }

        finalResult += intermediateResult

        return finalResult
    }
}

