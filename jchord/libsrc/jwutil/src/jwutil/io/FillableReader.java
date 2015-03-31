// FillableReader.java, created Oct 5, 2004 10:35:52 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.io;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * An Reader that is buffered and refillable.
 * This class is thread-safe, so different threads can be reading
 * and writing the same stream.
 * 
 * @author jwhaley
 * @version $Id: FillableReader.java,v 1.2 2005/05/28 10:23:16 joewhaley Exp $
 */
public class FillableReader extends Reader {

    /**
     * Get a Writer object attached to this FillableReader.
     * 
     * @return  writer object
     */
    public Writer getWriter() {
        FISWriter os = new FISWriter();
        return os;
    }

    private class FISWriter extends Writer {

        /**
         * Insert the given character into the buffer for this FillableReader.
         * 
         * @param c  character to insert
         */
        public void write(char c) {
            FillableReader.this.write(c);
        }
        
        /* (non-Javadoc)
         * @see java.io.Writer#write(char[], int, int)
         */
        public void write(char[] cbuf, int off, int len) throws IOException {
            FillableReader.this.write(cbuf, off, len);
        }

        /* (non-Javadoc)
         * @see java.io.Writer#flush()
         */
        public void flush() throws IOException {
        }

        /* (non-Javadoc)
         * @see java.io.Writer#close()
         */
        public void close() throws IOException {
            FillableReader.this.close();
        }
        
    }
    
    protected char[] buffer;
    volatile int start, end;
    
    /**
     * Construct a new FillableReader with an initial buffer size of 512 characters.
     */
    public FillableReader() {
        buffer = new char[512];
        start = end = 0;
    }
    
    /* (non-Javadoc)
     * @see java.io.Reader#ready()
     */
    public boolean ready() {
        return start != end;
    }
    
    /* (non-Javadoc)
     * @see java.io.Reader#read()
     */
    public int read() throws IOException {
        synchronized (lock) {
            while (start == end) {
                try {
                    lock.wait();
                } catch (InterruptedException x) { }
            }
            int res = buffer[start++];
            if (start == buffer.length) start = 0;
            lock.notify();
            return res;
        }
    }

    /* (non-Javadoc)
     * @see java.io.Reader#read(char[], int, int)
     */
    public int read(char[] b, int off, int len) {
        synchronized (lock) {
            while (start == end) {
                try {
                    lock.wait();
                } catch (InterruptedException x) { }
            }
            int a = Math.min(len, (start < end ? end : buffer.length) - start);
            System.arraycopy(buffer, start, b, off, a);
            start += a;
            if (start == buffer.length) start = 0;
            lock.notify();
            return a;
        }
    }
    
    private int nextEnd(int count) {
        int newEnd = end + count;
        if (newEnd >= buffer.length) newEnd -= buffer.length;
        return newEnd;
    }
    
    /**
     * Insert the given character into the buffer for this FillableReader.
     * 
     * @param b  character to insert
     */
    public void write(int b) {
        synchronized (lock) {
            while (start == nextEnd(1)) {
                try {
                    lock.wait();
                } catch (InterruptedException x) { }
            }
            buffer[end++] = (char) b;
            if (end == buffer.length) end = 0;
            lock.notify();
        }
    }
    
    /**
     * Insert the given array of characters into the buffer for
     * this FillableReader.
     * 
     * @param b  array of characters to insert
     */
    public void write(char[] b) {
        write(b, 0, b.length);
    }
    
    /**
     * Inserts a portion of an array of characters.
     * 
     * @param b  array of characters
     * @param off  offset from which to start writing characters
     * @param len  number of characters to write
     */
    public void write(char[] b, int off, int len) {
        synchronized (lock) {
            while (len > 0) {
                int upTo, a;
                for (;;) {
                    upTo = start <= end ? (start > 0 ? buffer.length : buffer.length - 1) : start - 1;
                    a = upTo - end;
                    if (a > 0) break;
                    try {
                        lock.wait();
                    } catch (InterruptedException x) { }
                }
                a = Math.min(a, len);
                _write(b, off, a);
                lock.notify();
                off += a;
                len -= a;
            }
        }
    }
    
    private void _write(char[] b, int off, int len) {
        System.arraycopy(b, off, buffer, end, len);
        end += len;
        if (end == buffer.length) end = 0;
    }

    /**
     * Inserts a string.
     * 
     * @param s  string to insert
     */
    public void write(String s) {
        int len = s.length();
        synchronized (lock) {
            for (int i = 0 ; i < len ; i++) {
                write(s.charAt(i));
            }
        }
    }

    /* (non-Javadoc)
     * @see java.io.Reader#close()
     */
    public void close() throws IOException {
        synchronized (lock) {
            buffer = null;
            start = -1;
            end = -2;
        }
    }

}
