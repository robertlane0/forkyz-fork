/*
 * Copyright 2000-2004 Sun Microsystems, Inc.  All Rights Reserved.
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
/*
 */
package app.crossword.yourealwaysbe.forkyz.puz.io.charset

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetEncoder
import java.nio.charset.CoderResult

abstract class SingleByteEncoder protected constructor(
    cs : Charset?,
    private val index1 : ShortArray,
    private val index2 : String,
    private val mask1 : Int,
    private val mask2 : Int,
    private val shift : Int,
) : CharsetEncoder(cs, 1.0f, 1.0f) {
    private val sgp = Surrogate.Parser()

    override fun canEncode(c : Char) : Boolean {
        val testEncode = index2.get(
            index1[(c.code and mask1) shr shift]
                + (c.code and mask2)
        )
        return testEncode != '\u0000' || c == '\u0000'
    }

    private fun encodeArrayLoop(
        src : CharBuffer,
        dst : ByteBuffer,
    ) : CoderResult? {
        val sa = src.array()
        var sp = src.arrayOffset() + src.position()
        val sl = src.arrayOffset() + src.limit()
        sp = (if (sp <= sl) sp else sl)
        val da = dst.array()
        var dp = dst.arrayOffset() + dst.position()
        val dl = dst.arrayOffset() + dst.limit()
        dp = (if (dp <= dl) dp else dl)

        try {
            while (sp < sl) {
                val c = sa[sp]
                if (Surrogate.`is`(c.code)) {
                    if (sgp.parse(c, sa, sp, sl) < 0)
                        return sgp.error()
                    return sgp.unmappableResult()
                }
                if (c >= '\uFFFE')
                    return CoderResult.unmappableForLength(1)
                if (dl - dp < 1)
                    return CoderResult.OVERFLOW

                val e = index2.get(
                    index1[(c.code and mask1) shr shift]
                        + (c.code and mask2)
                )

                // If output byte is zero because input char is zero
                // then character is mappable, o.w. fail
                if (e == '\u0000' && c != '\u0000')
                    return CoderResult.unmappableForLength(1)

                sp++
                da[dp++] = e.code.toByte()
            }
            return CoderResult.UNDERFLOW
        } finally {
            src.position(sp - src.arrayOffset())
            dst.position(dp - dst.arrayOffset())
        }
    }

    private fun encodeBufferLoop(
        src : CharBuffer,
        dst : ByteBuffer,
    ) : CoderResult? {
        var mark = src.position()
        try {
            while (src.hasRemaining()) {
                val c = src.get()
                if (Surrogate.`is`(c.code)) {
                    if (sgp.parse(c, src) < 0)
                        return sgp.error()
                    return sgp.unmappableResult()
                }
                if (c >= '\uFFFE')
                    return CoderResult.unmappableForLength(1)
                if (!dst.hasRemaining())
                    return CoderResult.OVERFLOW

                val e = index2.get(
                    index1[(c.code and mask1) shr shift]
                        + (c.code and mask2)
                )

                // If output byte is zero because input char is zero
                // then character is mappable, o.w. fail
                if (e == '\u0000' && c != '\u0000')
                    return CoderResult.unmappableForLength(1)

                mark++
                dst.put(e.code.toByte())
            }
            return CoderResult.UNDERFLOW
        } finally {
            src.position(mark)
        }
    }

    override fun encodeLoop(src : CharBuffer, dst : ByteBuffer) : CoderResult? {
        if (true && src.hasArray() && dst.hasArray())
            return encodeArrayLoop(src, dst)
        else
            return encodeBufferLoop(src, dst)
    }

    fun encode(inputChar : Char) : Byte {
        return index2.get(
            index1[(inputChar.code and mask1) shr shift]
                + (inputChar.code and mask2)
        ).code.toByte()
    }
}

