/*
 * Copyright 2000-2006 Sun Microsystems, Inc.  All Rights Reserved.
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
package app.crossword.yourealwaysbe.puz.io.charset

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CoderResult

abstract class SingleByteDecoder protected constructor(
    cs : Charset?,
    private val byteToCharTable : String,
) : CharsetDecoder(cs, 1.0f, 1.0f) {
    private fun decodeArrayLoop(
        src : ByteBuffer,
        dst : CharBuffer,
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
                val b = sa[sp].toInt()

                val c = decode(b)
                if (c == '\uFFFD')
                    return CoderResult.unmappableForLength(1)
                if (dl - dp < 1)
                    return CoderResult.OVERFLOW
                da[dp++] = c
                sp++
            }
            return CoderResult.UNDERFLOW
        } finally {
            src.position(sp - src.arrayOffset())
            dst.position(dp - dst.arrayOffset())
        }
    }

    private fun decodeBufferLoop(
        src : ByteBuffer,
        dst : CharBuffer,
    ) : CoderResult? {
        var mark = src.position()
        try {
            while (src.hasRemaining()) {
                val b = src.get().toInt()

                val c = decode(b)
                if (c == '\uFFFD')
                    return CoderResult.unmappableForLength(1)
                if (!dst.hasRemaining())
                    return CoderResult.OVERFLOW
                mark++
                dst.put(c)
            }
            return CoderResult.UNDERFLOW
        } finally {
            src.position(mark)
        }
    }

    override fun decodeLoop(src : ByteBuffer, dst : CharBuffer) : CoderResult? {
        if (true && src.hasArray() && dst.hasArray())
            return decodeArrayLoop(src, dst)
        else
            return decodeBufferLoop(src, dst)
    }

    fun decode(byteIndex : Int) : Char {
        val n = byteIndex + 128
        if (n >= byteToCharTable.length || n < 0)
            return '\uFFFD'
        return byteToCharTable.get(n)
    }
}

