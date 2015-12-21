// EntryValueComparator.java, created Aug 23, 2004 5:31:02 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.Comparator;
import java.util.Map;

/**
 * Compares Map.Entry objects based on their values.
 * 
 * @author jwhaley
 * @version $Id: EntryValueComparator.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public class EntryValueComparator implements Comparator {
    public static final EntryValueComparator INSTANCE = new EntryValueComparator();
    private EntryValueComparator() { }
    public int compare(Object arg0, Object arg1) {
        if (arg0.equals(arg1)) return 0;
        Map.Entry e0 = (Map.Entry) arg0;
        Map.Entry e1 = (Map.Entry) arg1;
        Comparable v0 = (Comparable) e0.getValue();
        Comparable v1 = (Comparable) e1.getValue();
        int i = v0.compareTo(v1);
        if (i != 0) return i;
        return e0.getKey().hashCode() - e1.getKey().hashCode();
    }
}
