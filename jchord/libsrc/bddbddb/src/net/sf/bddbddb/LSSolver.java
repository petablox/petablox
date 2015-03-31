// LSSolver.java, created Feb 8, 2005 4:28:23 AM by joewhaley
// Copyright (C) 2005 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.List;

/**
 * LSSolver
 * 
 * @author jwhaley
 * @version $Id: LSSolver.java 497 2005-04-06 17:03:47Z joewhaley $
 */
public class LSSolver extends Solver {
    /**
     * 
     */
    public LSSolver() {
        super();
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Solver#createInferenceRule(java.util.List, net.sf.bddbddb.RuleTerm)
     */
    InferenceRule createInferenceRule(List top, RuleTerm bottom) {
        return new LSInferenceRule(this, top, bottom);
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Solver#createEquivalenceRelation(net.sf.bddbddb.Domain, net.sf.bddbddb.Domain)
     */
    Relation createEquivalenceRelation(Domain fd1, Domain fd2) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Solver#createLessThanRelation(net.sf.bddbddb.Domain, net.sf.bddbddb.Domain)
     */
    Relation createLessThanRelation(Domain fd1, Domain fd2) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Solver#createGreaterThanRelation(net.sf.bddbddb.Domain, net.sf.bddbddb.Domain)
     */
    Relation createGreaterThanRelation(Domain fd1, Domain fd2) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Solver#createMapRelation(net.sf.bddbddb.Domain, net.sf.bddbddb.Domain)
     */
    Relation createMapRelation(Domain fd1, Domain fd2) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Solver#createRelation(java.lang.String, java.util.List)
     */
    public Relation createRelation(String name, List attributes) {
        return new LSRelation(this, name, attributes);
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Solver#solve()
     */
    public void solve() {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Solver#finish()
     */
    public void finish() {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Solver#cleanup()
     */
    public void cleanup() {
        // TODO Auto-generated method stub
    }
}
