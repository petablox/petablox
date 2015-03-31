// Textualizable.java, created Oct 26, 2003 2:53:27 PM by joewhaley
// Copyright (C) 2003 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.io;

import java.io.IOException;

/**
 * Textualizable
 * 
 * @author John Whaley
 * @version $Id: Textualizable.java,v 1.1 2004/09/27 22:42:34 joewhaley Exp $
 */
public interface Textualizable {
    
    void write(Textualizer t) throws IOException;
    void writeEdges(Textualizer t) throws IOException;
    void addEdge(String edge, Textualizable t);
    
}
