package org.tormap.cli

import java.io.OutputStream
import java.io.PrintStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction

/**
 * The one rule for what the CLI may write: visible text. Letters, marks, numbers, punctuation, symbols and spaces are
 * allowed; everything else is not. That excludes control characters (C0, DEL, C1: escape sequences that could rewrite
 * the terminal), format characters (bidirectional overrides and isolates, zero-width characters, soft hyphens, tags),
 * line and paragraph separators, private-use, unassigned and lone surrogate code points.
 *
 * It is an allowlist on purpose: a blocklist of dangerous characters had to grow with each character it missed.
 * Line breaks and tabs are not allowed here: in a value from the API they could fake rows or columns. Only the CLI's
 * own output structure uses them, see [TerminalSafePrintStream].
 */
fun isVisible(codePoint: Int): Boolean = when (Character.getType(codePoint).toByte()) {
    Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER, Character.TITLECASE_LETTER, Character.MODIFIER_LETTER,
    Character.OTHER_LETTER,
    Character.NON_SPACING_MARK, Character.ENCLOSING_MARK, Character.COMBINING_SPACING_MARK,
    Character.DECIMAL_DIGIT_NUMBER, Character.LETTER_NUMBER, Character.OTHER_NUMBER,
    Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION,
    Character.END_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION,
    Character.OTHER_PUNCTUATION,
    Character.MATH_SYMBOL, Character.CURRENCY_SYMBOL, Character.MODIFIER_SYMBOL, Character.OTHER_SYMBOL,
    Character.SPACE_SEPARATOR,
    -> true

    else -> false
}

/** The replacement for every character that is not allowed. */
const val REPLACEMENT = '?'

/**
 * A PrintStream that writes only visible text ([isVisible]), line feeds and tabs to [target]; every other character is
 * replaced with [REPLACEMENT]. A carriage return is kept only as part of a CRLF line break (println on Windows), since
 * a lone one moves the cursor back to overwrite the line.
 *
 * Everything the CLI writes goes through one of these: stdout, stderr and the --output file are wrapped in [runCli]
 * and [main] (which also covers stack traces of uncaught exceptions). So a value that was not sanitized before it is
 * printed can at worst look wrong; it can never reach the terminal as a control sequence. Formatting code should still
 * sanitize values ([sanitize], [escapeControls]) to keep rows intact and JSON lossless; this is the guarantee behind it.
 */
class TerminalSafePrintStream private constructor(filter: Filter) : PrintStream(filter, true, Charsets.UTF_8) {
    companion object {
        /** [target] wrapped, unless it already is one. */
        fun wrap(target: PrintStream): PrintStream =
            target as? TerminalSafePrintStream ?: TerminalSafePrintStream(Filter(target))
    }

    /** Decodes the UTF-8 bytes this PrintStream writes, filters the code points, and prints them to the target. */
    private class Filter(private val target: PrintStream) : OutputStream() {
        // Malformed UTF-8 can not come from this PrintStream's encoder, but would be replaced as well
        private val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
            .replaceWith(REPLACEMENT.toString())

        // Bytes of a character split between two writes wait here for the rest
        private val bytes = ByteBuffer.allocate(8192)
        private val chars = CharBuffer.allocate(8192)

        // A high surrogate at the end of a decoded chunk, waiting for its low surrogate
        private var pendingHighSurrogate: Char? = null

        // A carriage return, waiting to see whether a line feed follows
        private var pendingCarriageReturn = false

        override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)

        override fun write(b: ByteArray, off: Int, len: Int) {
            var offset = off
            var remaining = len
            while (remaining > 0) {
                val count = minOf(remaining, bytes.remaining())
                bytes.put(b, offset, count)
                offset += count
                remaining -= count
                decode(endOfInput = false)
            }
        }

        private fun decode(endOfInput: Boolean) {
            bytes.flip()
            while (true) {
                val result = decoder.decode(bytes, chars, endOfInput)
                emit()
                if (!result.isOverflow) break
            }
            bytes.compact()
        }

        private fun emit() {
            chars.flip()
            val text = StringBuilder(chars.remaining())
            while (chars.hasRemaining()) {
                val char = chars.get()
                val high = pendingHighSurrogate
                if (high != null) {
                    pendingHighSurrogate = null
                    if (Character.isLowSurrogate(char)) {
                        append(text, Character.toCodePoint(high, char))
                        continue
                    }
                    append(text, high.code)
                }
                if (Character.isHighSurrogate(char)) pendingHighSurrogate = char else append(text, char.code)
            }
            chars.clear()
            if (text.isNotEmpty()) target.print(text)
        }

        private fun append(text: StringBuilder, codePoint: Int) {
            if (pendingCarriageReturn) {
                pendingCarriageReturn = false
                if (codePoint == '\n'.code) {
                    text.append("\r\n")
                    return
                }
                text.append(REPLACEMENT)
            }
            when {
                codePoint == '\r'.code -> pendingCarriageReturn = true

                codePoint == '\n'.code || codePoint == '\t'.code || isVisible(
                    codePoint,
                ) -> text.appendCodePoint(codePoint)

                else -> text.append(REPLACEMENT)
            }
        }

        // An incomplete character or a carriage return stays pending: its rest may come with the next write
        override fun flush() = target.flush()

        override fun close() {
            decode(endOfInput = true)
            decoder.flush(chars)
            emit()
            if (pendingHighSurrogate != null || pendingCarriageReturn) target.print(REPLACEMENT)
            pendingHighSurrogate = null
            pendingCarriageReturn = false
            target.close()
        }
    }
}
