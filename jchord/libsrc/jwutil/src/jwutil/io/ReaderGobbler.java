// ReaderGobbler.java, created Oct 5, 2004 9:27:40 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.io;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * Reads data from a Reader, optionally outputting the read data to a Writer
 * or another object that implements the included Output interface.  You can
 * change the Reader and/or the Writer/Output by calling the appropriate set
 * methods.
 * 
 * You can use either the Writer or the Output, but not both at the same time.
 * 
 * Don't forget to call start() after creation!
 * 
 * @author jwhaley
 * @version $Id: ReaderGobbler.java,v 1.3 2005/05/28 10:23:17 joewhaley Exp $
 */
public class ReaderGobbler extends Thread {
    protected Reader is;
    protected Writer out;
    protected Output out2;
    
    /**
     * A simple interface clients can implement to receive output from an reader.
     * 
     * @author jwhaley
     * @version $Id: ReaderGobbler.java,v 1.3 2005/05/28 10:23:17 joewhaley Exp $
     */
    public static interface Output {
        /**
         * Write the len characters at the given offset in the given array.
         * 
         * @param buffer  character buffer
         * @param off  offset in buffer
         * @param len  number of characters
         */
        void write(char[] buffer, int off, int len);
    }
    
    /**
     * Construct a new ReaderGobbler that dumps output to System.out.
     * 
     * @param is  input reader
     */
    public ReaderGobbler(Reader is) {
        this(is, new OutputStreamWriter(System.out));
    }
    
    /**
     * Construct a new ReaderGobbler that reads from System.in and dumps
     * output to the given Writer.
     * 
     * @param o  writer
     */
    public ReaderGobbler(Writer o) {
        this(new InputStreamReader(System.in), o);
    }
    
    /**
     * Construct a new ReaderGobbler that reads from System.in and dumps
     * output to the given Output.
     * 
     * @param o  output object
     */
    public ReaderGobbler(Output o) {
        this(new InputStreamReader(System.in), o);
    }
    
    /**
     * Construct a new ReaderGobbler that reads from the given Reader and dumps
     * output to the given Writer.
     * 
     * @param is  reader
     * @param o  writer
     */
    public ReaderGobbler(Reader is, Writer o) {
        this.is = is;
        this.out = o;
    }
    
    /**
     * Construct a new ReaderGobbler that reads from the given Reader and dumps
     * output to the given Output.
     * 
     * @param is  reader
     * @param o  output object
     */
    public ReaderGobbler(Reader is, Output o) {
        this.is = is;
        this.out2 = o;
    }
    
    /**
     * Set the reader for this gobbler to the given reader.
     * 
     * @param r  new reader
     */
    public void setReader(Reader r) {
        this.is = r;
    }
    
    /**
     * Set the writer for this gobbler to the given writer.
     * 
     * @param o  new writer
     */
    public void setWriter(Writer o) {
        this.out = o;
        this.out2 = null;
    }
    
    /**
     * Set the output for this gobbler to the given output.
     * 
     * @param o  new output
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
            char[] buffer = new char[512];
            boolean eos = false;
            while (!eos) {
                int i = 0;
                // while chars are available, read them up to buffer.length chars.
                while (i < buffer.length && is.ready()) {
                    int r = is.read();
                    if (r < 0) {
                        eos = true; break;
                    } else {
                        buffer[i++] = (char) r;
                    }
                }
                // write the chars we just read.
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
