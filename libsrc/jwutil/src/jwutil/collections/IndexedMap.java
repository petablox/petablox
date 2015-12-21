// IndexMap.java, created Sep 20, 2003 2:04:05 AM by joewhaley
// Copyright (C) 2003 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.Iterator;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Interface for an indexed map.  An indexed map provides a mapping
 * between elements and (integer) indices.
 * 
 * @author jwhaley
 * @version $Id: IndexedMap.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public interface IndexedMap {

    int get(Object o);
    Object get(int i);
    boolean contains(Object o);
    Iterator iterator();
    int size();
    void dump(final BufferedWriter out) throws IOException;
    void dumpStrings(final BufferedWriter out) throws IOException;
    
}
