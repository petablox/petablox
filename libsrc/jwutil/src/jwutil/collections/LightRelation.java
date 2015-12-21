// LightRelation.java, created Fri Jun 30 11:11:17 2000 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@alum.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.

package jwutil.collections;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>LightRelation</code>
 * 
 * @author  Alexandru SALCIANU <salcianu@alum.mit.edu>
 * @version $Id: LightRelation.java,v 1.2 2005/04/29 02:32:24 joewhaley Exp $
 */
public class LightRelation extends AbstrRelationMapBased
    implements Serializable {

    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 4120851053534328370L;

    /** Creates a <code>RelationLight</code>. */
    public LightRelation() {
        map = new LightMap();
    }


    protected Relation getEmptyRelation() {
        return new LightRelation();
    }
    

    public boolean add(Object key, Object value) {
        hashCode = 0;
        Collection vals = getValues2(key);
        if(vals == null)
            map.put(key, vals = new LinearSet());
        return vals.add(value);
    }


    public boolean addAll(Object key, Collection values) {
        hashCode = 0;
        if((values == null) || values.isEmpty())
            return false;
        Collection vals = getValues2(key);
        if(vals == null)
            map.put(key, vals = new LinearSet());
        return vals.addAll(values);
    }


    public void removeAll(Object key, Collection values) {
        hashCode = 0;
        Collection vals = getValues2(key);
        if((vals == null) || vals.isEmpty()) return;

        for(Iterator it = values.iterator(); it.hasNext(); )
            vals.remove(it.next());
    }
    
    public Object clone() {
        LightRelation newrel = (LightRelation) super.clone();
        newrel.map = (Map) ((LightMap) map).clone();
        for(Iterator it = newrel.map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            LinearSet newvals = 
                (LinearSet) ((LinearSet) entry.getValue()).clone();
            entry.setValue(newvals);
        }
        return newrel;
    }

}
