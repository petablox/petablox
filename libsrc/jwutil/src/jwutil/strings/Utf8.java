// Utf8.java, created Mon Feb  5 23:23:22 2001 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.strings;

/**
 * Utf8 conversion routines
 *
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: Utf8.java,v 1.2 2005/05/28 10:07:22 joewhaley Exp $
 */
public abstract class Utf8 {

    //// Utf8 conversion routines
    
    /**
     * Strictly check the format of the utf8/pseudo-utf8 byte array in
     * fromUtf8.
     */
    static final boolean STRICTLY_CHECK_FORMAT = false;
    /**
     * Set fromUtf8 to not throw an exception when given a normal utf8
     * byte array.
     */
    static final boolean ALLOW_NORMAL_UTF8 = false;
    /**
     * Set fromUtf8 to not throw an exception when given a pseudo utf8
     * byte array.
     */
    static final boolean ALLOW_PSEUDO_UTF8 = true;
    /**
     * Set toUtf8 to write in pseudo-utf8 (rather than normal utf8).
     */
    static final boolean WRITE_PSEUDO_UTF8 = true;

    /**
     * Convert the given sequence of (pseudo-)utf8 formatted bytes
     * into a String.
     *
     * The acceptable input formats are controlled by the
     * STRICTLY_CHECK_FORMAT, ALLOW_NORMAL_UTF8, and ALLOW_PSEUDO_UTF8
     * flags.
     *
     * @param utf8 (pseudo-)utf8 byte array
     * @throws UTFDataFormatError if the (pseudo-)utf8 byte array is not valid (pseudo-)utf8
     * @return unicode string
     */
    public static String fromUtf8(byte[] utf8)
    throws UTFDataFormatError {
        char[] result = new char[utf8.length];
        int result_index = 0;
        for (int i=0, n=utf8.length; i<n; ) {
            byte b = utf8[i++];
            if (STRICTLY_CHECK_FORMAT && !ALLOW_NORMAL_UTF8)
                if (b == 0)
                    throw new UTFDataFormatError("0 byte encountered at location "+(i-1));
            if (b >= 0) {  // < 0x80 unsigned
                // in the range '\001' to '\177'
                result[result_index++] = (char)b;
                continue;
            }
            try {
                byte nb = utf8[i++];
                if (b < -32) {  // < 0xe0 unsigned
                    // '\000' or in the range '\200' to '\u07FF'
                    char c = result[result_index++] =
                        (char)(((b & 0x1f) << 6) | (nb & 0x3f));
                    if (STRICTLY_CHECK_FORMAT) {
                        if (((b & 0xe0) != 0xc0) ||
                            ((nb & 0xc0) != 0x80))
                            throw new UTFDataFormatError("invalid marker bits for double byte char at location "+(i-2));
                        if (c < '\200') {
                            if (!ALLOW_PSEUDO_UTF8 || (c != '\000'))
                                throw new UTFDataFormatError("encountered double byte char that should have been single byte at location "+(i-2));
                        } else if (c > '\u07FF')
                            throw new UTFDataFormatError("encountered double byte char that should have been triple byte at location "+(i-2));
                    }
                } else {
                    byte nnb = utf8[i++];
                    // in the range '\u0800' to '\uFFFF'
                    char c = result[result_index++] =
                        (char)(((b & 0x0f) << 12) |
                               ((nb & 0x3f) << 6) |
                               (nnb & 0x3f));
                    if (STRICTLY_CHECK_FORMAT) {
                        if (((b & 0xf0) != 0xe0) ||
                            ((nb & 0xc0) != 0x80) ||
                            ((nnb & 0xc0) != 0x80))
                            throw new UTFDataFormatError("invalid marker bits for triple byte char at location "+(i-3));
                        if (c < '\u0800')
                            throw new UTFDataFormatError("encountered triple byte char that should have been fewer bytes at location "+(i-3));
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new UTFDataFormatError("unexpected end at location "+i);
            }
        }
        return new String(result, 0, result_index);
    }

    /**
     * Convert the given String into a sequence of (pseudo-)utf8
     * formatted bytes.
     *
     * The output format is controlled by the WRITE_PSEUDO_UTF8 flag.
     *
     * @param s String to convert
     * @return array containing sequence of (pseudo-)utf8 formatted bytes
     */
    public static byte[] toUtf8(String s) {
        byte[] result = new byte[lengthUtf8(s)];
        int result_index = 0;
        for (int i = 0, n = s.length(); i < n; ++i) {
            char c = (char)s.charAt(i);
            // in all shifts below, c is an (unsigned) char,
            // so either >>> or >> is ok
            if (((!WRITE_PSEUDO_UTF8) || (c >= 0x0001)) && (c <= 0x007F))
                result[result_index++] = (byte)c;
            else if (c > 0x07FF) {
                result[result_index++] = (byte)(0xe0 | (byte)(c >> 12));
                result[result_index++] = (byte)(0x80 | ((c & 0xfc0) >> 6));
                result[result_index++] = (byte)(0x80 | (c & 0x3f));
            } else {
                result[result_index++] = (byte)(0xc0 | (byte)(c >> 6));
                result[result_index++] = (byte)(0x80 | (c & 0x3f));
            }
        }
        return result;
    }

    /**
     * Converts a character to utf8 in the given byte array.
     * Returns the new offset in the byte array.
     */
    public static int toUtf8(char c, byte[] to, int off, int end) {
        int k = 0;
        if ((c >= 0x0001) && (c <= 0x007F)) {
            to[off++] = (byte) c;
        } else {
            if (c > 0x07FF) {
                to[off++] = (byte)(0xe0 | (byte)(c >> 12));
                if (off == end) return -1;
                to[off++] = (byte)(0x80 | ((c & 0xfc0) >> 6));
                if (off == end) return -1;
                to[off++] = (byte)(0x80 | (c & 0x3f));
            } else {
                to[off++] = (byte)(0xc0 | (byte)(c >> 6));
                if (off == end) return -1;
                to[off++] = (byte)(0x80 | (c & 0x3f));
            }
        }
        return off;
    }
    
    /**
     * Returns the length of a string's utf8 encoded form.
     */
    public static int lengthUtf8(String s) {
        int utflen = 0;
        for (int i = 0, n = s.length(); i < n; ++i) {
            int c = s.charAt(i);
            if (((!WRITE_PSEUDO_UTF8) || (c >= 0x0001)) && (c <= 0x007F))
                ++utflen;
            else if (c > 0x07FF)
                utflen += 3;
            else
                utflen += 2;
        }
        return utflen;
    }

    /**
     * Returns the length of a string's utf8 encoded form.
     */
    public static int lengthUtf8(char[] cs, int off, int len) {
        int result = 0;
        for (int i = 0; i < len; ++i) {
            char c = cs[off + i];
            if ((c >= 0x0001) && (c <= 0x007F)) {
                ++result;
            } else {
                if (c > 0x07FF) {
                    result += 3;
                } else {
                    result += 2;
                }
            }
        }
        return result;
    }
    
    /**
     * Check whether the given sequence of bytes is valid (pseudo-)utf8.
     *
     * @param bytes byte array to check
     * @return true iff the given sequence is valid (pseudo-)utf8.
     */
    public static boolean checkUtf8(byte[] bytes) {
        for (int i=0, n=bytes.length; i<n; ) {
            byte b = bytes[i++];
            if (STRICTLY_CHECK_FORMAT && !ALLOW_NORMAL_UTF8)
                if (b == 0) return false;
            if (b >= 0) {  // < 0x80 unsigned
                // in the range '\001' to '\177'
                continue;
            }
            try {
                byte nb = bytes[i++];
                if (b < -32) {  // < 0xe0 unsigned
                    // '\000' or in the range '\200' to '\u07FF'
                    char c = (char)(((b & 0x1f) << 6) | (nb & 0x3f));
                    if (STRICTLY_CHECK_FORMAT) {
                        if (((b & 0xe0) != 0xc0) ||
                            ((nb & 0xc0) != 0x80))
                            return false;
                        if (c < '\200') {
                            if (!ALLOW_PSEUDO_UTF8 || (c != '\000'))
                                return false;
                            } else if (c > '\u07FF')
                                return false;
                    }
                } else {
                    byte nnb = bytes[i++];
                    // in the range '\u0800' to '\uFFFF'
                    char c = (char)(((b & 0x0f) << 12) |
                                    ((nb & 0x3f) << 6) |
                                    (nnb & 0x3f));
                    if (STRICTLY_CHECK_FORMAT) {
                        if (((b & 0xf0) != 0xe0) ||
                            ((nb & 0xc0) != 0x80) ||
                            ((nnb & 0xc0) != 0x80))
                            return false;
                        if (c < '\u0800')
                            return false;
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return false;
            }
        }
        return true;
    }

}
