// MultiMapFactory.java, created Wed Feb 27 13:14:06 2002 by cananian
// Copyright (C) 2000 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package jwutil.collections;

import java.util.Collections;
import java.util.Map;
/** <code>MultiMapFactory</code> is a <code>MultiMap</code> generator.
 *  Subclasses should implement constructions of specific types of
 *  <code>MultiMap</code>s.
 *
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: MultiMapFactory.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public class MultiMapFactory/*<K,V>*/ extends MapFactory/*<K,V>*/ {
    
    /** Creates a <code>MultiMapFactory</code>. */
    public MultiMapFactory() {
    }
    // MapFactory interface
    public final Map/*<K,V>*/ makeMap() {
        return makeMultiMap();
    }
    public final /*<K2 extends K, V2 extends V>*/ Map/*<K,V>*/ makeMap(Map/*<K2,V2>*/ map) {
        return makeMultiMap(map);
    }
    // MultiMapFactory interface.
    // XXX: why do we have default implementations here but not in the other
    //  *Factory classes?
    public MultiMap/*<K,V>*/ makeMultiMap() {
        return makeMultiMap(Collections.EMPTY_MAP);
    }

    /** Creates a new <code>MultiMap</code> initialized with all 
    of the <code>Map.Entry</code>s in <code>map</code>
    */
    public /*<K2 extends K, V2 extends V>*/ MultiMap/*<K,V>*/ makeMultiMap(Map/*<K2,V2>*/ map) {
        return new GenericMultiMap/*<K,V>*/(map);
    }
    public MultiMap/*<K,V>*/ makeMultiMap(MapFactory/*<K,Collection<V>>*/ mf, CollectionFactory/*<V>*/ cf) {
        return new GenericMultiMap/*<K,V>*/(mf, cf);
    }
}
