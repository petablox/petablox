// ReaderInputStream.java, created Oct 6, 2004 3:44:58 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import jwutil.strings.Utf8;

/**
 * Adapter between a Reader and an InputStream.
 * 
 * @author jwhaley
 * @version $Id: ReaderInputStream.java,v 1.2 2005/05/28 10:23:17 joewhaley Exp $
 */
public class ReaderInputStream extends InputStream {

    protected final Reader reader;
    protected byte[] buf;
    protected int index;
    
    public ReaderInputStream(Reader r) {
        this.reader = r;
        this.buf = new byte[2];
        this.index = 2;
    }
    
    /* (non-Javadoc)
     * @see java.io.InputStream#read()
     */
    public synchronized int read() throws IOException {
        if (index < buf.length) {
            byte b = buf[index++];
            return b;
        }
        int c = reader.read();
        if (c < 0 || ((c >= 0x0001) && (c <= 0x007F))) {
            return c;
        } else {
            if (c > 0x07FF) {
                index = buf.length-2;
                buf[index] = (byte)(0x80 | ((c & 0xfc0) >> 6));
                buf[index+1] = (byte)(0x80 | (c & 0x3f));
                return (byte)(0xe0 | (byte)(c >> 12));
            } else {
                index = buf.length-1;
                buf[index] = (byte)(0x80 | (c & 0x3f));
                return (byte)(0xc0 | (byte)(c >> 6));
            }
        }
    }
    
    /* (non-Javadoc)
     * @see java.io.InputStream#available()
     */
    public int available() throws IOException {
        return buf.length - index + (reader.ready() ? 1 : 0);
    }
    
    protected transient char[] charbuf;
    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public int read(byte[] b, int off, int len) throws IOException {
        int n;
        if (index < buf.length) {
            n = Math.min(len, buf.length - index);
            System.arraycopy(buf, index, b, off, n);
            index += n;
            return n;
        }
        if (len == 0) return 0;
        if ((off < 0) || (off > b.length) || (len < 0) ||
           ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        if (charbuf == null || charbuf.length < len) {
            charbuf = new char[len];
        }
        int nChars = reader.read(charbuf, 0, len);
        if (nChars == -1) return -1;
        int byteIndex = off, charIndex = 0, byteEnd = off + len;
        while (byteIndex < byteEnd && charIndex < nChars) {
            int newByteIndex = Utf8.toUtf8(charbuf[charIndex], b, byteIndex, byteEnd);
            if (newByteIndex == -1) {
                // Overflowed the buffer!
                break;
            }
            byteIndex = newByteIndex;
            charIndex++;
        }
        if (charIndex < nChars) {
            // More characters to do!
            int length = Utf8.lengthUtf8(charbuf, charIndex, nChars - charIndex);
            if (length > buf.length) {
                buf = new byte[length];
            }
            index = buf.length - length;
            int bufIndex = index;
            while (charIndex < nChars) {
                bufIndex = Utf8.toUtf8(charbuf[charIndex++], buf, bufIndex, buf.length);
            }
            // assert bufIndex == buf.length;
        }
        return byteIndex - off;
    }
    
}
