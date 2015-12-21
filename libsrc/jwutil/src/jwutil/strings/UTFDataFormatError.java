// UTFDataFormatError.java, created Mon Feb  5 23:23:22 2001 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.strings;

/**
 * Indicates a UTF format error.
 * 
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: UTFDataFormatError.java,v 1.2 2005/04/29 02:32:26 joewhaley Exp $
 */
public class UTFDataFormatError extends RuntimeException {

    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 3257569499053110328L;

    /**
     * Creates new <code>UTFDataFormatError</code> without detail message.
     */
    public UTFDataFormatError() {
    }

    /**
     * Constructs an <code>UTFDataFormatError</code> with the specified detail message.
     * @param msg the detail message.
     */
    public UTFDataFormatError(String msg) {
        super(msg);
    }
}
