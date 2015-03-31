// LittleEndianOutputStream.java, created Wed Mar  5  0:26:34 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.io;

import java.io.IOException;
import java.io.OutputStream;
import jwutil.strings.Utf8;

/**
 * A wrapper for an OutputStream that outputs data in little-endian format.
 * Little-endian means that the least-significant byte goes first.  The default
 * for Java is big-endian, which means that the most-significant byte goes first.
 * 
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: LittleEndianOutputStream.java,v 1.3 2005/05/28 10:23:17 joewhaley Exp $
 */
public class LittleEndianOutputStream implements ExtendedDataOutput {

    private OutputStream out;
    
    /** Creates new LittleEndianOutputStream */
    public LittleEndianOutputStream(OutputStream out) { this.out = out; }

    public void write(byte[] p1) throws IOException { out.write(p1); }
    public void write(byte[] p1, int p2, int p3) throws IOException { out.write(p1, p2, p3); }
    public void write(int p1) throws IOException { out.write(p1); }
    public void writeByte(int p1) throws IOException { out.write(p1); }
    public void writeUByte(int p1) throws IOException { out.write(p1); }
    public void writeBytes(String p1) throws IOException {
        for (int i=0; i<p1.length(); ++i) out.write(p1.charAt(i));
    }
    public void writeChars(java.lang.String p1) throws IOException {
        for (int i=0; i<p1.length(); ++i) writeChar(p1.charAt(i));
    }
    public void writeBoolean(boolean p1) throws IOException { out.write(p1?1:0); }
    public void writeUTF(String p1) throws IOException {
        writeShort(p1.length());
        out.write(Utf8.toUtf8(p1));
    }
    public void writeFloat(float p1) throws IOException { writeInt(Float.floatToRawIntBits(p1)); }
    public void writeDouble(double p1) throws IOException { writeLong(Double.doubleToRawLongBits(p1)); }
    public void writeShort(int p1) throws IOException { write_s16(out, (short)p1); }
    public void writeUShort(int p1) throws IOException { write_u16(out, (char)p1); }
    public void writeChar(int p1) throws IOException { write_u16(out, (char)p1); }
    public void writeInt(int p1) throws IOException { write_s32(out, p1); }
    public void writeUInt(int p1) throws IOException { write_u32(out, p1); }
    public void writeLong(long p1) throws IOException { write_s64(out, p1); }
    public void writeULong(long p1) throws IOException { write_u64(out, p1); }

    private static final void write_s16(OutputStream out, short b) throws IOException {
        out.write(b); out.write(b>>8);
    }
    private static final void write_u16(OutputStream out, char b) throws IOException {
        out.write(b); out.write(b>>8);
    }
    private static final void write_s32(OutputStream out, int b) throws IOException {
        out.write(b); out.write(b>>8); out.write(b>>16); out.write(b>>24);
    }
    private static final void write_u32(OutputStream out, int b) throws IOException {
        out.write(b); out.write(b>>8); out.write(b>>16); out.write(b>>24);
    }
    private static final void write_s64(OutputStream out, long b) throws IOException {
        int lo = (int)b; int hi = (int)(b >> 32);
        out.write(lo); out.write(lo>>8); out.write(lo>>16); out.write(lo>>24);
        out.write(hi); out.write(hi>>8); out.write(hi>>16); out.write(hi>>24);
    }
    private static final void write_u64(OutputStream out, long b) throws IOException {
        int lo = (int)b; int hi = (int)(b >> 32);
        out.write(lo); out.write(lo>>8); out.write(lo>>16); out.write(lo>>24);
        out.write(hi); out.write(hi>>8); out.write(hi>>16); out.write(hi>>24);
    }
    
}
