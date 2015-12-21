// FillableInputStream.java, created Oct 5, 2004 10:35:52 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.io;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import jwutil.strings.Utf8;

/**
 * An InputStream that is buffered and refillable.  This class implements the
 * DataOutput interface, so you can fill the buffer with calls to DataOutput
 * methods.  This class is thread-safe, so different threads can be reading
 * and writing the same stream.
 * 
 * @author jwhaley
 * @version $Id: FillableInputStream.java,v 1.5 2005/05/28 10:23:17 joewhaley Exp $
 */
public class FillableInputStream extends InputStream implements DataOutput {

    /**
     * Get an OutputStream that is attached to this FillableInputStream. 
     * 
     * @return  attached output stream
     */
    public OutputStream getOutputStream() {
        FISOutputStream os = new FISOutputStream();
        return os;
    }
    
    private class FISOutputStream extends OutputStream {

        /* (non-Javadoc)
         * @see java.io.OutputStream#write(int)
         */
        public void write(int b) throws IOException {
            FillableInputStream.this.writeByte(b);
        }
        
        /* (non-Javadoc)
         * @see java.io.OutputStream#write(byte[], int, int)
         */
        public void write(byte b[], int off, int len) throws IOException {
            FillableInputStream.this.write(b, off, len);
        }
        
    }
    
    protected byte[] buffer;
    protected int start, end;
    
    /**
     * Construct a new FillableInputStream with a default buffer size of 1024 bytes.
     */
    public FillableInputStream() {
        buffer = new byte[1024];
        start = end = 0;
    }
    
    /* (non-Javadoc)
     * @see java.io.InputStream#read()
     */
    public synchronized int read() throws IOException {
        while (start == end) {
            try {
                wait();
            } catch (InterruptedException x) { }
        }
        int res = buffer[start++];
        if (start == buffer.length) start = 0;
        notify();
        return res;
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        while (start == end) {
            try {
                wait();
            } catch (InterruptedException x) { }
        }
        int a = Math.min(len, (start < end ? end : buffer.length) - start);
        System.arraycopy(buffer, start, b, off, a);
        start += a;
        if (start == buffer.length) start = 0;
        notify();
        return a;
    }
    
    private int nextEnd(int count) {
        int newEnd = end + count;
        if (newEnd >= buffer.length) newEnd -= buffer.length;
        return newEnd;
    }
    
    /* (non-Javadoc)
     * @see java.io.InputStream#available()
     */
    public synchronized int available() {
        return available0();
    }
    
    private int available0() {
        int r = end - start;
        if (r < 0) return r + buffer.length;
        else return r;
    }
    
    /* (non-Javadoc)
     * @see java.io.DataOutput#write(int)
     */
    public synchronized void write(int b) {
        while (start == nextEnd(1)) {
            try {
                wait();
            } catch (InterruptedException x) { }
        }
        buffer[end++] = (byte) b;
        if (end == buffer.length) end = 0;
        notify();
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#write(byte[])
     */
    public synchronized void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#write(byte[], int, int)
     */
    public synchronized void write(byte[] b, int off, int len) {
        while (len > 0) {
            int upTo, a;
            for (;;) {
                upTo = start <= end ? (start > 0 ? buffer.length : buffer.length - 1) : start - 1;
                a = upTo - end;
                if (a > 0) break;
                try {
                    wait();
                } catch (InterruptedException x) { }
            }
            a = Math.min(a, len);
            _write(b, off, a);
            notify();
            off += a;
            len -= a;
        }
    }
    
    private void _write(byte[] b, int off, int len) {
        System.arraycopy(b, off, buffer, end, len);
        end += len;
        if (end == buffer.length) end = 0;
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeBoolean(boolean)
     */
    public void writeBoolean(boolean v) {
        write(v ? 1 : 0);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeByte(int)
     */
    public void writeByte(int v) {
        write(v);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeShort(int)
     */
    public void writeShort(int v) {
        write((v >>> 8) & 0xFF);
        write((v >>> 0) & 0xFF);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeChar(int)
     */
    public void writeChar(int v) {
        write((v >>> 8) & 0xFF);
        write((v >>> 0) & 0xFF);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeInt(int)
     */
    public void writeInt(int v) {
        write((v >>> 24) & 0xFF);
        write((v >>> 16) & 0xFF);
        write((v >>>  8) & 0xFF);
        write((v >>>  0) & 0xFF);
    }

    private byte writeBuffer[] = new byte[8];
    
    /* (non-Javadoc)
     * @see java.io.DataOutput#writeLong(long)
     */
    public void writeLong(long v) {
        writeBuffer[0] = (byte)(v >>> 56);
        writeBuffer[1] = (byte)(v >>> 48);
        writeBuffer[2] = (byte)(v >>> 40);
        writeBuffer[3] = (byte)(v >>> 32);
        writeBuffer[4] = (byte)(v >>> 24);
        writeBuffer[5] = (byte)(v >>> 16);
        writeBuffer[6] = (byte)(v >>>  8);
        writeBuffer[7] = (byte)(v >>>  0);
        write(writeBuffer, 0, 8);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeFloat(float)
     */
    public void writeFloat(float v) {
        writeInt(Float.floatToIntBits(v));
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeDouble(double)
     */
    public void writeDouble(double v) {
        writeLong(Double.doubleToLongBits(v));
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeBytes(java.lang.String)
     */
    public void writeBytes(String s) {
        int len = s.length();
        for (int i = 0 ; i < len ; i++) {
            write((byte)s.charAt(i));
        }
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeChars(java.lang.String)
     */
    public void writeChars(String s) {
        int len = s.length();
        for (int i = 0 ; i < len ; i++) {
            int v = s.charAt(i);
            write((v >>> 8) & 0xFF); 
            write((v >>> 0) & 0xFF); 
        }
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeUTF(java.lang.String)
     */
    public void writeUTF(String str) throws IOException {
        byte[] b = Utf8.toUtf8(str);
        if (b.length > 65535) {
            throw new UTFDataFormatException();
        }
        writeShort(b.length);
        write(b);
    }
}
