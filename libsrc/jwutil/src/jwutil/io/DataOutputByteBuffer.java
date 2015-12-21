// DataOutputByteBuffer.java, created Aug 10, 2004 3:56:04 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.io;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import jwutil.strings.Utf8;

/**
 * DataOutputByteBuffer provides an implementation of the DataOutput
 * interface that is backed by a java.nio.ByteBuffer.
 * 
 * @author John Whaley
 * @version $Id: DataOutputByteBuffer.java,v 1.2 2005/05/28 09:20:03 joewhaley Exp $
 */
public class DataOutputByteBuffer implements DataOutput {
    
    protected ByteBuffer buf;
    
    /**
     * Construct a new DataOutputByteBuffer from the given ByteBuffer.
     */
    public DataOutputByteBuffer(ByteBuffer b) {
        super();
        this.buf = b;
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#write(int)
     */
    public void write(int b) throws IOException {
        buf.putInt(b);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#write(byte[])
     */
    public void write(byte[] b) throws IOException {
        buf.put(b);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#write(byte[], int, int)
     */
    public void write(byte[] b, int off, int len) throws IOException {
        buf.put(b, off, len);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeBoolean(boolean)
     */
    public void writeBoolean(boolean v) throws IOException {
        buf.put(v?(byte)1:(byte)0);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeByte(int)
     */
    public void writeByte(int v) throws IOException {
        buf.put((byte)v);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeShort(int)
     */
    public void writeShort(int v) throws IOException {
        buf.putShort((short)v);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeChar(int)
     */
    public void writeChar(int v) throws IOException {
        buf.putChar((char)v);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeInt(int)
     */
    public void writeInt(int v) throws IOException {
        buf.putInt(v);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeLong(long)
     */
    public void writeLong(long v) throws IOException {
        buf.putLong(v);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeFloat(float)
     */
    public void writeFloat(float v) throws IOException {
        buf.putFloat(v);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeDouble(double)
     */
    public void writeDouble(double v) throws IOException {
        buf.putDouble(v);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeBytes(java.lang.String)
     */
    public void writeBytes(String s) throws IOException {
        byte[] b = s.getBytes();
        buf.put(b);
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeChars(java.lang.String)
     */
    public void writeChars(String s) throws IOException {
        for (int i = 0; i < s.length(); ++i) {
            buf.putChar(s.charAt(i));
        }
    }

    /* (non-Javadoc)
     * @see java.io.DataOutput#writeUTF(java.lang.String)
     */
    public void writeUTF(String str) throws IOException {
        byte[] b = Utf8.toUtf8(str);
        buf.put(b);
    }
}
