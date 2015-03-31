// IdentityHashCodeWrapper.java, created Wed Mar  5  0:26:27 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import jwutil.util.Assert;

/*
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: IdentityHashCodeWrapper.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public class IdentityHashCodeWrapper {
    
    private Object o;
    private IdentityHashCodeWrapper(Object o) {
        this.o = o;
    }
    public static IdentityHashCodeWrapper create(Object o) {
        Assert._assert(o != null);
        return new IdentityHashCodeWrapper(o);
    }
    public boolean equals(Object that) {
        if (this == that) return true;
        if (!(that instanceof IdentityHashCodeWrapper)) return false;
        return this.o == ((IdentityHashCodeWrapper)that).o;
    }
    public int hashCode() {
        return System.identityHashCode(o);
    }
    
    public Object getObject() { return o; }
    
}
