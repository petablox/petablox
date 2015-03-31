// ByteSequence.java, created Wed Mar  5  0:26:34 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.io;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

/**
 * A version of DataInputStream backed by a byte array.
 * It supports getIndex() and unreadByte().
 * 
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: ByteSequence.java,v 1.2 2005/05/28 09:19:09 joewhaley Exp $
 */
public final class ByteSequence extends DataInputStream {
    private ByteArrayStream byte_stream;

    public ByteSequence(byte[] bytes) {
        super(new ByteArrayStream(bytes));
        byte_stream = (ByteArrayStream) in;
    }

    public ByteSequence(byte[] bytes, int offset, int length) {
        super(new ByteArrayStream(bytes, offset, length));
        byte_stream = (ByteArrayStream) in;
    }

    public final int getIndex() {
        return byte_stream.getPosition();
    }
    public final void unreadByte() {
        byte_stream.unreadByte();
    }

    private static final class ByteArrayStream extends ByteArrayInputStream {
        ByteArrayStream(byte[] bytes) {
            super(bytes);
        }
        ByteArrayStream(byte[] bytes, int offset, int length) {
            super(bytes, offset, length);
        }
        final int getPosition() {
            return pos;
        } // is protected in ByteArrayInputStream
        final void unreadByte() {
            if (pos > 0)
                pos--;
        }
    }
}
