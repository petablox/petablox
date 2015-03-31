// LSInferenceRule.java, created Feb 8, 2005 4:29:36 AM by joewhaley
// Copyright (C) 2005 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.List;

/**
 * LSInferenceRule
 * 
 * @author jwhaley
 * @version $Id: LSInferenceRule.java 497 2005-04-06 17:03:47Z joewhaley $
 */
public class LSInferenceRule extends InferenceRule {
    /**
     * @param solver
     * @param top
     * @param bottom
     */
    public LSInferenceRule(Solver solver, List top, RuleTerm bottom) {
        super(solver, top, bottom);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param solver
     * @param top
     * @param bottom
     * @param id
     */
    public LSInferenceRule(Solver solver, List top, RuleTerm bottom, int id) {
        super(solver, top, bottom, id);
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.InferenceRule#update()
     */
    public boolean update() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.InferenceRule#reportStats()
     */
    public void reportStats() {
        // TODO Auto-generated method stub
    }
}
