// BufferedDataInput.java, created Jul 28, 2004 4:52:28 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.io;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.EOFException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UTFDataFormatException;

/**
 * A class to convert from a Reader to a DataInput, with buffering support.
 * 
 * @author jwhaley
 * @version $Id: BufferedDataInput.java,v 1.2 2005/05/28 09:14:20 joewhaley Exp $
 */
public class BufferedDataInput implements DataInput {
    
    protected BufferedReader in;
    
    public BufferedDataInput(Reader s) {
        in = new BufferedReader(s);
    }
    
    public BufferedDataInput(String fileName) throws IOException {
        this(new FileReader(fileName));
    }
    
    /* (non-Javadoc)
     * @see java.io.DataInput#readFully(byte[])
     */
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    /* (non-Javadoc)
     * @see java.io.DataInput#readFully(byte[], int, int)
     */
    public void readFully(byte[] b, int off, int len) throws IOException {
        char[] c = new char[len];
        int k = 0;
        while (len > 0) {
            int k2 = in.read(c, k, len);
            if (k2 == -1) throw new IOException();
            k += k2;
            len -= k2;
        }
        for (int i = 0; i < c.length; ++i) {
            // truncate
            b[off + i] = (byte) c[i];
        }
    }

    /* (non-Javadoc)
     * @see java.io.DataInput#skipBytes(int)
     */
    public int skipBytes(int n) throws IOException {
        return (int) in.skip(n);
    }

    /* (non-Javadoc)
     * @see java.io.DataInput#readBoolean()
     */
    public boolean readBoolean() throws IOException {
        int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return ch != 0;
    }

    /* (non-Javadoc)
     * @see java.io.DataInput#readByte()
     */
    public byte readByte() throws IOException {
        int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return (byte) ch;
    }

    /* (non-Javadoc)
     * @see java.io.DataInput#readUnsignedByte()
     */
    public int readUnsignedByte() throws IOException {
        int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return ch;
    }

    /* (non-Javadoc)
     * @see java.io.DataInput#readShort()
     */
    public short readShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (short)((ch1 << 8) + (ch2 << 0));
    }

    /* (non-Javadoc)
     * @see java.io.DataInput#readUnsignedShort()
     */
    public int readUnsignedShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (ch1 << 8) + (ch2 << 0);
    }

    /* (non-Javadoc)
     * @see java.io.DataInput#readChar()
     */
    public char readChar() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (char)((ch1 << 8) + (ch2 << 0));
    }

    /* (non-Javadoc)
     * @see java.io.DataInput#readInt()
     */
    public int readInt() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    private byte readBuffer[] = new byte[8];
    
    /* (non-Javadoc)
     * @see java.io.DataInput#readLong()
     */
    public long readLong() throws IOException {
        readFully(readBuffer, 0, 8);
        return (((long)readBuffer[0] << 56) +
                ((long)(readBuffer[1] & 255) << 48) +
                ((long)(readBuffer[2] & 255) << 40) +
                ((long)(readBuffer[3] & 255) << 32) +
                ((long)(readBuffer[4] & 255) << 24) +
                ((readBuffer[5] & 255) << 16) +
                ((readBuffer[6] & 255) <<  8) +
                ((readBuffer[7] & 255) <<  0));
    }

    /* (non-Javadoc)
     * @see java.io.DataInput#readFloat()
     */
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    /* (non-Javadoc)
     * @see java.io.DataInput#readDouble()
     */
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    /* (non-Javadoc)
     * @see java.io.DataInput#readLine()
     */
    public String readLine() throws IOException {
        return in.readLine();
    }

    /* (non-Javadoc)
     * @see java.io.DataInput#readUTF()
     */
    public String readUTF() throws IOException {
        int utflen = readUnsignedShort();
        byte[] bytearr = new byte[utflen];
        char[] chararr = new char[utflen];

        int c, char2, char3;
        int count = 0;
        int chararr_count=0;

        readFully(bytearr, 0, utflen);

        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;      
            if (c > 127) break;
            count++;
            chararr[chararr_count++]=(char)c;
        }

        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;
            switch (c >> 4) {
                case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
                    /* 0xxxxxxx*/
                    count++;
                    chararr[chararr_count++]=(char)c;
                    break;
                case 12: case 13:
                    /* 110x xxxx   10xx xxxx*/
                    count += 2;
                    if (count > utflen)
                        throw new UTFDataFormatException(
                            "malformed input: partial character at end");
                    char2 = (int) bytearr[count-1];
                    if ((char2 & 0xC0) != 0x80)
                        throw new UTFDataFormatException(
                            "malformed input around byte " + count); 
                    chararr[chararr_count++]=(char)(((c & 0x1F) << 6) | 
                                                    (char2 & 0x3F));  
                    break;
                case 14:
                    /* 1110 xxxx  10xx xxxx  10xx xxxx */
                    count += 3;
                    if (count > utflen)
                        throw new UTFDataFormatException(
                            "malformed input: partial character at end");
                    char2 = (int) bytearr[count-2];
                    char3 = (int) bytearr[count-1];
                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
                        throw new UTFDataFormatException(
                            "malformed input around byte " + (count-1));
                    chararr[chararr_count++]=(char)(((c     & 0x0F) << 12) |
                                                    ((char2 & 0x3F) << 6)  |
                                                    ((char3 & 0x3F) << 0));
                    break;
                default:
                    /* 10xx xxxx,  1111 xxxx */
                    throw new UTFDataFormatException(
                        "malformed input around byte " + count);
            }
        }
        // The number of chars produced may be less than utflen
        return new String(chararr, 0, chararr_count);
    }
}
