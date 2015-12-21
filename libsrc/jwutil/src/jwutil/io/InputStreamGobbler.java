// InputStreamGobbler.java, created Oct 5, 2004 8:44:20 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * InputStreamGobbler is a thread that reads from a given InputStream and writes whatever
 * is read to an OutputStream or an object that implements the included Output interface.
 * You can change the InputStream and/or the OutputStream/Output by calling the appropriate
 * set methods.
 * 
 * You can use either the OutputStream or the Output, but not both at the same time.
 * 
 * Don't forget to call start() after creation!
 * 
 * @author John Whaley
 * @version $Id: InputStreamGobbler.java,v 1.3 2005/05/28 10:23:17 joewhaley Exp $
 */
public class InputStreamGobbler extends Thread {
    protected InputStream is;
    protected OutputStream out;
    protected Output out2;
    
    /**
     * A simple interface clients can implement to receive output from an input stream.
     * 
     * @author jwhaley
     * @version $Id: InputStreamGobbler.java,v 1.3 2005/05/28 10:23:17 joewhaley Exp $
     */
    public static interface Output {
        /**
         * Write the len bytes at the given offset in the given array.
         * 
         * @param buffer  byte buffer
         * @param off  offset in buffer
         * @param len  number of bytes
         */
        void write(byte[] buffer, int off, int len);
    }
    
    /**
     * Construct a new InputStreamGobbler that dumps output to System.out.
     * 
     * @param is  input stream
     */
    public InputStreamGobbler(InputStream is) {
        this(is, System.out);
    }
    
    /**
     * Construct a new InputStreamGobbler that reads from System.in and dumps
     * output to the given OutputStream.
     * 
     * @param o  output stream
     */
    public InputStreamGobbler(OutputStream o) {
        this(System.in, o);
    }
    
    /**
     * Construct a new InputStreamGobbler that reads from System.in and dumps
     * output to the given Output.
     * 
     * @param o  output object
     */
    public InputStreamGobbler(Output o) {
        this(System.in, o);
    }
    
    /**
     * Construct a new InputStreamGobbler that reads from the given InputStream
     * and dumps output to the given OutputStream.
     * 
     * @param is  input stream
     * @param o  output stream
     */
    public InputStreamGobbler(InputStream is, OutputStream o) {
        this.is = is;
        this.out = o;
    }
    
    /**
     * Construct a new InputStreamGobbler that reads from the given InputStream
     * and dumps output to the given Output.
     * 
     * @param is  input stream
     * @param o  output object
     */
    public InputStreamGobbler(InputStream is, Output o) {
        this.is = is;
        this.out2 = o;
    }
    
    /**
     * Set the input for this gobbler to the given input stream.
     * 
     * @param r  new input stream
     */
    public void setInput(InputStream r) {
        this.is = r;
    }
    
    /**
     * Set the output for this gobbler to the given output stream.
     * 
     * @param o  new output stream
     */
    public void setOutput(OutputStream o) {
        this.out = o;
        this.out2 = null;
    }
    
    /**
     * Set the output for this gobbler to the given output object.
     * 
     * @param o  new output object
     */
    public void setOutput(Output o) {
        this.out = null;
        this.out2 = o;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try {
            byte[] buffer = new byte[1024];
            boolean eos = false;
            while (!eos) {
                int i;
                // if there is nothing available, just block on reading a byte.
                if (is.available() == 0) {
                    int r = is.read();
                    if (r < 0) {
                        eos = true;
                        break;
                    } else {
                        buffer[0] = (byte) r;
                        i = 1;
                    }
                } else {
                    i = 0;
                    // while bytes are available, read them up to buffer.length bytes.
                    while (i < buffer.length && is.available() > 0) {
                        int r = is.read(buffer, i, Math.min(is.available(), buffer.length-i));
                        if (r < 0) {
                            eos = true; break;
                        } else if (r == 0) {
                            break;
                        } else {
                            i += r;
                        }
                    }
                }
                // write the bytes we just read.
                if (out != null) {
                    out.write(buffer, 0, i);
                } else if (out2 != null) {
                    out2.write(buffer, 0, i);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();  
        }
    }
}
