package com.elishaazaria.sayboard.recognition.text

/**
 * Pure text processing logic extracted from TextManager.
 * Contains auto-spacing and auto-capitalization rules
 * with no Android dependencies.
 */
object TextProcessor {
    private val sentenceTerminators = charArrayOf('.', '\n', '!', '?')

    /**
     * Determines whether to capitalize the next input based on the preceding text.
     * Scans backwards through the string looking for sentence terminators or letters/digits.
     * @return true if should capitalize, false if should not, null if indeterminate
     */
    fun capitalizeAfter(text: CharSequence): Boolean? {
        for (char in text.reversed()) {
            if (char.isLetterOrDigit()) {
                return false
            }
            if (char in sentenceTerminators) {
                return true
            }
        }
        return null
    }

    /**
     * Determines whether a space should be added after the given character.
     */
    fun addSpaceAfter(char: Char): Boolean = when (char) {
        '"' -> false
        '*' -> false
        ' ' -> false
        '\n' -> false
        '\t' -> false
        else -> true
    }

    /**
     * Apply auto-capitalization and auto-spacing to input text.
     * @param text The raw recognized text
     * @param shouldCapitalize Whether the first character should be capitalized
     * @param shouldAddSpace Whether to prepend a space
     * @param autoCapitalizeEnabled Whether the auto-capitalize preference is enabled
     * @param recognizerAddsSpaces Whether the current recognizer adds spaces
     * @return The processed text with capitalization and spacing applied
     */
    fun processText(
        text: String,
        shouldCapitalize: Boolean,
        shouldAddSpace: Boolean,
        autoCapitalizeEnabled: Boolean,
        recognizerAddsSpaces: Boolean
    ): String {
        var result = text
        if (autoCapitalizeEnabled && shouldCapitalize) {
            result = result[0].uppercase() + result.substring(1)
        }
        if (recognizerAddsSpaces && shouldAddSpace) {
            result = " $result"
        }
        return result
    }
}
