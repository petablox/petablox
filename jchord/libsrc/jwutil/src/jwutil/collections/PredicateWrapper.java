// PredicateWrapper.java, created Thu Feb 24 15:56:13 2000 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@alum.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package jwutil.collections;

/**
 * <code>PredicateWrapper</code> wraps a predicate on an <code>Object</code>.
 This is supposed to allow us to send predicate functions as arguments to
 high-level functions.
 * 
 * @author  Alexandru SALCIANU <salcianu@alum.mit.edu>
 * @version $Id: PredicateWrapper.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public interface PredicateWrapper {
    boolean check(Object obj);
}
