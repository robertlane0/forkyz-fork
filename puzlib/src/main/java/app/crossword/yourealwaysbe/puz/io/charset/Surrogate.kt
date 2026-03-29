/*
 * Copyright 2000-2001 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package app.crossword.yourealwaysbe.puz.io.charset

import java.nio.CharBuffer
import java.nio.charset.CoderResult

/**
 * Utility class for dealing with surrogates.
 *
 * @author Mark Reinhold
 */
object Surrogate {
    // UTF-16 surrogate-character ranges
    //
    const val MIN_HIGH : Char = '\uD800'
    const val MAX_HIGH : Char = '\uDBFF'
    const val MIN_LOW : Char = '\uDC00'
    const val MAX_LOW : Char = '\uDFFF'
    val MIN : Char = MIN_HIGH
    val MAX : Char = MAX_LOW

    // Range of UCS-4 values that need surrogates in UTF-16
    //
    const val UCS4_MIN : Int = 0x10000
    val UCS4_MAX : Int = (1 shl 20) + UCS4_MIN - 1

    /**
     * Tells whether or not the given UTF-16 value is a high surrogate.
     */
    fun isHigh(c : Int) : Boolean {
        return (MIN_HIGH.code <= c) && (c <= MAX_HIGH.code)
    }

    /**
     * Tells whether or not the given UTF-16 value is a low surrogate.
     */
    fun isLow(c : Int) : Boolean {
        return (MIN_LOW.code <= c) && (c <= MAX_LOW.code)
    }

    /**
     * Tells whether or not the given UTF-16 value is a surrogate character,
     */
    fun `is`(c : Int) : Boolean {
        return (MIN.code <= c) && (c <= MAX.code)
    }

    /**
     * Tells whether or not the given UCS-4 character must be represented as a
     * surrogate pair in UTF-16.
     */
    fun neededFor(uc : Int) : Boolean {
        return (uc >= UCS4_MIN) && (uc <= UCS4_MAX)
    }

    /**
     * Returns the high UTF-16 surrogate for the given UCS-4 character.
     */
    fun high(uc : Int) : Char {
        return (0xd800 or (((uc - UCS4_MIN) shr 10) and 0x3ff)).toChar()
    }

    /**
     * Returns the low UTF-16 surrogate for the given UCS-4 character.
     */
    fun low(uc : Int) : Char {
        return (0xdc00 or ((uc - UCS4_MIN) and 0x3ff)).toChar()
    }

    /**
     * Converts the given surrogate pair into a 32-bit UCS-4 character.
     */
    fun toUCS4(c : Char, d : Char) : Int {
        return (((c.code and 0x3ff) shl 10) or (d.code and 0x3ff)) + 0x10000
    }

    /**
     * Surrogate parsing support.  Charset implementations may use instances of
     * this class to handle the details of parsing UTF-16 surrogate pairs.
     */
    class Parser {
        private var character = 0 // UCS-4
        private var error : CoderResult? = CoderResult.UNDERFLOW
        private var isPair = false

        /**
         * Returns the UCS-4 character previously parsed.
         */
        fun character() : Int {
            assert(error == null)
            return character
        }

        /**
         * Tells whether or not the previously-parsed UCS-4 character was
         * originally represented by a surrogate pair.
         */
        fun isPair() : Boolean {
            assert(error == null)
            return isPair
        }

        /**
         * Returns the number of UTF-16 characters consumed by the previous
         * parse.
         */
        fun increment() : Int {
            assert(error == null)
            return if (isPair) 2 else 1
        }

        /**
         * If the previous parse operation detected an error, return the object
         * describing that error.
         */
        fun error() : CoderResult {
            checkNotNull(error)
            return error!!
        }

        /**
         * Returns an unmappable-input result object, with the appropriate
         * input length, for the previously-parsed character.
         */
        fun unmappableResult() : CoderResult? {
            assert(error == null)
            return CoderResult.unmappableForLength(if (isPair) 2 else 1)
        }

        /**
         * Parses a UCS-4 character from the given source buffer, handling
         * surrogates.
         *
         * @param  c    The first character
         * @param  in   The source buffer, from which one more character
         * will be consumed if c is a high surrogate
         *
         * @returns  Either a parsed UCS-4 character, in which case the isPair()
         * and increment() methods will return meaningful values, or
         * -1, in which case error() will return a descriptive result
         * object
         */
        fun parse(c : Char, `in` : CharBuffer) : Int {
            if (isHigh(c.code)) {
                if (!`in`.hasRemaining()) {
                    error = CoderResult.UNDERFLOW
                    return -1
                }
                val d = `in`.get()
                if (isLow(d.code)) {
                    character = toUCS4(c, d)
                    isPair = true
                    error = null
                    return character
                }
                error = CoderResult.malformedForLength(1)
                return -1
            }
            if (isLow(c.code)) {
                error = CoderResult.malformedForLength(1)
                return -1
            }
            character = c.code
            isPair = false
            error = null
            return character
        }

        /**
         * Parses a UCS-4 character from the given source buffer, handling
         * surrogates.
         *
         * @param  c    The first character
         * @param  ia   The input array, from which one more character
         * will be consumed if c is a high surrogate
         * @param  ip   The input index
         * @param  il   The input limit
         *
         * @returns  Either a parsed UCS-4 character, in which case the isPair()
         * and increment() methods will return meaningful values, or
         * -1, in which case error() will return a descriptive result
         * object
         */
        fun parse(c : Char, ia : CharArray, ip : Int, il : Int) : Int {
            assert(ia[ip] == c)
            if (isHigh(c.code)) {
                if (il - ip < 2) {
                    error = CoderResult.UNDERFLOW
                    return -1
                }
                val d = ia[ip + 1]
                if (isLow(d.code)) {
                    character = toUCS4(c, d)
                    isPair = true
                    error = null
                    return character
                }
                error = CoderResult.malformedForLength(1)
                return -1
            }
            if (isLow(c.code)) {
                error = CoderResult.malformedForLength(1)
                return -1
            }
            character = c.code
            isPair = false
            error = null
            return character
        }
    }

    /**
     * Surrogate generation support.  Charset implementations may use instances
     * of this class to handle the details of generating UTF-16 surrogate
     * pairs.
     */
    class Generator {
        private var error : CoderResult? = CoderResult.OVERFLOW

        /**
         * If the previous generation operation detected an error, return the
         * object describing that error.
         */
        fun error() : CoderResult {
            checkNotNull(error)
            return error!!
        }

        /**
         * Generates one or two UTF-16 characters to represent the given UCS-4
         * character.
         *
         * @param  uc   The UCS-4 character
         * @param  len  The number of input bytes from which the UCS-4 value
         * was constructed (used when creating result objects)
         * @param  dst  The destination buffer, to which one or two UTF-16
         * characters will be written
         *
         * @returns  Either a positive count of the number of UTF-16 characters
         * written to the destination buffer, or -1, in which case
         * error() will return a descriptive result object
         */
        fun generate(uc : Int, len : Int, dst : CharBuffer) : Int {
            if (uc <= 0xffff) {
                if (`is`(uc)) {
                    error = CoderResult.malformedForLength(len)
                    return -1
                }
                if (dst.remaining() < 1) {
                    error = CoderResult.OVERFLOW
                    return -1
                }
                dst.put(uc.toChar())
                error = null
                return 1
            }
            if (uc < UCS4_MIN) {
                error = CoderResult.malformedForLength(len)
                return -1
            }
            if (uc <= UCS4_MAX) {
                if (dst.remaining() < 2) {
                    error = CoderResult.OVERFLOW
                    return -1
                }
                dst.put(high(uc))
                dst.put(low(uc))
                error = null
                return 2
            }
            error = CoderResult.unmappableForLength(len)
            return -1
        }

        /**
         * Generates one or two UTF-16 characters to represent the given UCS-4
         * character.
         *
         * @param  uc   The UCS-4 character
         * @param  len  The number of input bytes from which the UCS-4 value
         * was constructed (used when creating result objects)
         * @param  da   The destination array, to which one or two UTF-16
         * characters will be written
         * @param  dp   The destination position
         * @param  dl   The destination limit
         *
         * @returns  Either a positive count of the number of UTF-16 characters
         * written to the destination buffer, or -1, in which case
         * error() will return a descriptive result object
         */
        fun generate(
            uc : Int,
            len : Int,
            da : CharArray,
            dp : Int,
            dl : Int,
        ) : Int {
            if (uc <= 0xffff) {
                if (`is`(uc)) {
                    error = CoderResult.malformedForLength(len)
                    return -1
                }
                if (dl - dp < 1) {
                    error = CoderResult.OVERFLOW
                    return -1
                }
                da[dp] = uc.toChar()
                error = null
                return 1
            }
            if (uc < UCS4_MIN) {
                error = CoderResult.malformedForLength(len)
                return -1
            }
            if (uc <= UCS4_MAX) {
                if (dl - dp < 2) {
                    error = CoderResult.OVERFLOW
                    return -1
                }
                da[dp] = high(uc)
                da[dp + 1] = low(uc)
                error = null
                return 2
            }
            error = CoderResult.unmappableForLength(len)
            return -1
        }
    }
}

