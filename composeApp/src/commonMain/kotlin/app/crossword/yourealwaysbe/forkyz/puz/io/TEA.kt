
package app.crossword.yourealwaysbe.forkyz.puz.io

/**
 * Implementation of the Tiny Encryption Algorithm (TEA).
 *
 * The Tiny Encryption Algorithm is one of the fastest and most efficient
 * cryptographic algorithms in existence. It was developed by David Wheeler and
 * Roger Needham at the Computer Laboratory of Cambridge University.
 *
 * See http://www.cl.cam.ac.uk/ftp/users/djw3/tea.ps
 *
 * This software was written to provide simple encryption for J2ME.
 * The homepage for this software is http://winterwell.com/software/TEA.php
 *
 * (c) 2008 Joe Halliwell <joe.halliwell></joe.halliwell>@gmail.com>
 *
 * It has been ported to Kotlin in 2026 by yourealwaysbe.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
class TEA(key : ByteArray) {
    private val S = IntArray(4)

    /**
     * Encrypt an array of bytes.
     * @param clear the cleartext to encrypt
     * @return the encrypted text
     */
    fun encrypt(clear : ByteArray) : ByteArray {
        val paddedSize = 2 * (
            (clear.size / 8)
            + (if ((clear.size % 8) == 0) 0 else 1)
        )
        val buffer = IntArray(paddedSize + 1)
        buffer[0] = clear.size
        pack(clear, buffer, 1)
        brew(buffer)
        return unpack(buffer, 0, buffer.size * 4)
    }

    /**
     * Decrypt an array of bytes.
     * @param crypt the cipher text to decrypt
     * @return the decrypted text
     */
    fun decrypt(crypt : ByteArray) : ByteArray {
//        assert crypt.length % 4 == 0;
//        assert (crypt.length / 4) % 2 == 1;
        val buffer = IntArray(crypt.size / 4)
        pack(crypt, buffer, 0)
        unbrew(buffer)
        return unpack(buffer, 1, buffer[0])
    }

    fun brew(buf : IntArray) {
        //assert buf.length % 2 == 1;
        var i : Int
        var v0 : Int
        var v1 : Int
        var sum : Int
        var n : Int
        i = 1
        while (i < buf.size) {
            n = CUPS
            v0 = buf[i]
            v1 = buf[i + 1]
            sum = 0
            while (n-- > 0) {
                sum += SUGAR
                v0 += ((v1 shl 4) + S[0] xor v1) + (sum xor (v1 ushr 5)) + S[1]
                v1 += ((v0 shl 4) + S[2] xor v0) + (sum xor (v0 ushr 5)) + S[3]
            }
            buf[i] = v0
            buf[i + 1] = v1
            i += 2
        }
    }

    fun unbrew(buf : IntArray) {
        //assert buf.length % 2 == 1;
        var i : Int
        var v0 : Int
        var v1 : Int
        var sum : Int
        var n : Int
        i = 1
        while (i < buf.size) {
            n = CUPS
            v0 = buf[i]
            v1 = buf[i + 1]
            sum = UNSUGAR
            while (n-- > 0) {
                v1 -= ((v0 shl 4) + S[2] xor v0) + (sum xor (v0 ushr 5)) + S[3]
                v0 -= ((v1 shl 4) + S[0] xor v1) + (sum xor (v1 ushr 5)) + S[1]
                sum -= SUGAR
            }
            buf[i] = v0
            buf[i + 1] = v1
            i += 2
        }
    }

    fun pack(src : ByteArray, dest : IntArray, destOffset : Int) {
//        assert destOffset + (src.length / 4) <= dest.length;
        var i = 0
        var shift = 24
        var j = destOffset
        dest[j] = 0
        while (i < src.size) {
            dest[j] = dest[j] or ((src[i].toInt() and 0xff) shl shift)
            if (shift == 0) {
                shift = 24
                j++
                if (j < dest.size) dest[j] = 0
            } else {
                shift -= 8
            }
            i++
        }
    }

    fun unpack(src : IntArray, srcOffset : Int, destLength : Int) : ByteArray {
//        assert destLength <= (src.length - srcOffset) * 4;
        val dest = ByteArray(destLength)
        var i = srcOffset
        var count = 0
        for (j in 0..<destLength) {
            dest[j] = ((src[i] shr (24 - (8 * count))) and 0xff).toByte()
            count++
            if (count == 4) {
                count = 0
                i++
            }
        }
        return dest
    }

    //0a1634be95150fec57d638d6d93549320c71892252f93c9423df7898e1269b344ab893ed3a07c797bd3fb722324e90e601d97309ed87a6e2461bc878425471e0e258a0ebf5f96076be2e9f2d88ab5fa454220836be9e7fa733dac92603ab2cbf
    //sjri5jf85jdor560
    //
    //    public static void main(String... args) throws Exception {
    //        System.out.println("sjri5jf85jdor560".length());
    //        TEA tea = new TEA("sjri5jf85jdor560".getBytes(StandardCharsets.UTF_16));
    //        ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //        IOUtils.copy(new FileInputStream("/Users/rcooper/Downloads/cs150608.jpz"), baos);
    //        System.out.println(baos.toByteArray().length);
    //        byte[] result = tea.decrypt(baos.toByteArray());
    //        System.out.println(new String(result));
    //    }

    /**
     * Initialize the cipher for encryption or decryption.
     * @param key a 16 byte (128-bit) key
     */
    init {
        if (key.size < 16)
            throw RuntimeException("Invalid key: Length was less than 16 bytes")
        var off = 0
        var i = 0
        while (i < 4) {
            S[i] = (
                (key[off++].toInt() and 0xff)
                or ((key[off++].toInt() and 0xff) shl 8)
                or ((key[off++].toInt() and 0xff) shl 16)
                or ((key[off++].toInt() and 0xff) shl 24)
            )
            i++
        }
    }

    companion object {
        private const val SUGAR = -0x61c88647
        private const val CUPS = 32
        private const val UNSUGAR = -0x3910c8e0

        fun hexStringToByteArray(s : String) : ByteArray {
            val len = s.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = (
                    ((s.get(i).digitToIntOrNull(16) ?: -1) shl 4)
                    + (s.get(i + 1).digitToIntOrNull(16) ?: -1)
                ).toByte()
                i += 2
            }
            return data
        }

        /* Simple usage example */
        var quote : String = "Now rise, and show your strength. Be eloquent, and deep, and tender; see, with a clear eye, into Nature, and into life:  spread your white wings of quivering thought, and soar, a god-like spirit, over the whirling world beneath you, up through long lanes of flaming stars to the gates of eternity!"

        //    public static void main(String[] args) {
        //		/* Create a cipher using the first 16 bytes of the passphrase */
        //        TEA tea = new TEA("And is there honey still for tea?".getBytes());
        //
        //        byte[] original = quote.getBytes();
        //
        //		/* Run it through the cipher... and back */
        //        byte[] crypt = tea.encrypt(original);
        //        byte[] result = tea.decrypt(crypt);
        //
        //		/* Ensure that all went well */
        //        String test = new String(result);
        //        if (!test.equals(quote))
        //            throw new RuntimeException("Fail");
        //    }
    }
}

