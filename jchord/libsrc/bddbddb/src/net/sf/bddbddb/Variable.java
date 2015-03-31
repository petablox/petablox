// Variable.java, created Mar 16, 2004 12:43:38 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import org.jdom.Element;

/**
 * A Variable is a variable in a rule.
 * 
 * @author jwhaley
 * @version $Id: Variable.java 353 2004-10-27 19:21:05Z joewhaley $
 */
public class Variable {
    
    /**
     * Name of variable.
     */
    protected String name;
    
    /**
     * Domain of variable.
     */
    protected Domain domain;

    /**
     * Create empty variable.
     */
    public Variable() {
        this("_");
    }

    /**
     * Create a new variable with the given name.
     * 
     * @param name  name of variable
     */
    public Variable(String name) {
        super();
        this.name = name;
    }

    /**
     * Create a new variable with the given name and domain.
     * 
     * @param name  name of variable
     * @param fd  domain of variable
     */
    public Variable(String name, Domain fd) {
        super();
        this.name = name;
        this.domain = fd;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this variable.
     * 
     * @param name
     *            The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the domain of this variable.
     * 
     * @return  domain of this variable
     */
    public Domain getDomain() {
        return domain;
    }

    /**
     * Set the domain of this variable.
     * 
     * @param domain  the domain to set.
     */
    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return name;
    }
    
    public static Variable fromXMLElement(Element e, XMLFactory f) {
        String ruleName = e.getAttributeValue("rule");
        InferenceRule ir = f.getRule(ruleName);
        String variableName = e.getAttributeValue("name");
        return ir.getVariable(variableName);
    }
    
    public Element toXMLElement(InferenceRule ir) {
        Element e = new Element("variable");
        e.setAttribute("rule", "rule"+ir.id);
        e.setAttribute("name", name);
        return e;
    }
}
