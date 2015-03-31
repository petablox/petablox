// MultiMapSet.java, created Fri Mar 28 23:58:36 2003 by joewhaley
// Copyright (C) 2001-3 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.Map;

/**
 * A <code>MultiMapSet</code> is a <code>java.util.Set</code> of
 * <code>Map.Entry</code>s which can also be accessed as a
 * <code>MultiMap</code>.  Use the <code>entrySet()</code> method
 * of the <code>MultiMap</code> to get back the <code>MultiMapSet</code>.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: MultiMapSet.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public interface MultiMapSet/*<K,V>*/ extends MapSet/*<K,V>*/ {
    public Map/*<K,V>*/ asMap();
    public MultiMap/*<K,V>*/ asMultiMap();
}
