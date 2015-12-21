// RuleTerm.java, created Mar 16, 2004 12:42:16 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import jwutil.util.Assert;
import org.jdom.Element;

/**
 * A term in a Datalog rule.
 * 
 * @author jwhaley
 * @version $Id: RuleTerm.java 549 2005-05-17 10:17:33Z joewhaley $
 */
public class RuleTerm {
    
    /**
     * Relation for this rule term.
     */
    protected Relation relation;
    
    /**
     * List of variables in this rule term.
     */
    protected List/*<Variable>*/ variables;

    /**
     * Create a new RuleTerm with the given relation and list of variables.
     * 
     * @param relation
     * @param variables
     */
    public RuleTerm(Relation relation, List variables) {
        super();
        this.relation = relation;
        this.variables = variables;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(relation);
        sb.append("(");
        for (Iterator i = variables.iterator(); i.hasNext();) {
            sb.append(i.next());
            if (i.hasNext()) sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * @return Returns the relation.
     */
    public Relation getRelation() {
        return relation;
    }

    /**
     * @return Returns the variables.
     */
    public List getVariables() {
        return variables;
    }

    /**
     * @return  number of variables in this rule term
     */
    public int numberOfVariables() {
        return variables.size();
    }

    /**
     * @param i  index
     * @return  variable at the given index
     */
    public Variable getVariable(int i) {
        return (Variable) variables.get(i);
    }

    /**
     * @param v  variable
     * @return  index of the given variable
     */
    public int getVariableIndex(Variable v) {
        return variables.indexOf(v);
    }

    /**
     * @param v  variable
     * @return  attribute of the given variable
     */
    public Attribute getAttribute(Variable v) {
        int index = getVariableIndex(v);
        if (index < 0) return null;
        return relation.getAttribute(index);
    }
    
    public static RuleTerm fromXMLElement(Element e, Solver s, Map nameToVar) {
        Relation r = s.getRelation(e.getAttributeValue("relation"));
        List vars = new LinkedList();
        for (Iterator i = e.getContent().iterator(); i.hasNext(); ) {
            Element e2 = (Element) i.next();
            Variable v = (Variable) nameToVar.get(e2.getName());
            Domain d = r.getAttribute(vars.size()).getDomain();
            if (v == null) nameToVar.put(e2.getName(), v = new Variable(e2.getName(), d));
            Assert._assert(v.getDomain() == d);
        }
        return new RuleTerm(r, vars);
    }
    
    public Element toXMLElement() {
        Element e = new Element("ruleTerm");
        e.setAttribute("relation", relation.toString());
        for (Iterator i = variables.iterator(); i.hasNext(); ) {
            Variable v = (Variable) i.next();
            e.addContent(new Element(v.toString()));
        }
        return e;
    }
}
