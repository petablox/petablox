// MapSet.java, created Fri Mar 28 23:58:36 2003 by joewhaley
// Copyright (C) 2001-3 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.Map;
import java.util.Set;

/**
 * A <code>MapSet</code> is a <code>java.util.Set</code> of
 * <code>Map.Entry</code>s which can also be accessed as a
 * <code>java.util.Map</code>.  Use the <code>entrySet()</code>
 * method of the <code>Map</code> to get back the <code>MapSet</code>.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: MapSet.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public interface MapSet/*<K,V>*/ extends Set/*<Map.Entry<K,V>>*/ {
    public Map/*<K,V>*/ asMap();
}
