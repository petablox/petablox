// Attribute.java, created Jun 29, 2004 12:37:11 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import org.jdom.Element;

/**
 * An Attribute represents a single attribute of a relation.
 * Every Attribute has a name, a domain, and an optional option string.
 * 
 * Attribute objects are globally unique --- there is exactly one Attribute
 * object for every distinct attribute in the program.
 * 
 * @author jwhaley
 * @version $Id: Attribute.java 353 2004-10-27 19:21:05Z joewhaley $
 */
public class Attribute {
    
    /**
     * Attribute name.
     */
    protected String attributeName;
    
    /**
     * Attribute domain.
     */
    protected Domain attributeDomain;
    
    /**
     * Attribute options.
     */
    protected String attributeOptions;
    
    /**
     * Relation that this attribute is associated with.
     */
    protected Relation relation;

    /**
     * Constructs a new Attribute object.
     * This should only be called internally.
     * 
     * @param name  name of attribute
     * @param domain  domain of attribute
     * @param options  options for attribute
     */
    public Attribute(String name, Domain domain, String options) {
        this.attributeName = name;
        this.attributeDomain = domain;
        this.attributeOptions = options;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return attributeName;
    }

    /**
     * Returns the domain of this attribute.
     * 
     * @return domain
     */
    public Domain getDomain() {
        return attributeDomain;
    }

    /**
     * Returns the options for this attribute.
     * 
     * @return options
     */
    public String getOptions() {
        return attributeOptions;
    }

    /**
     * Sets the relation that this attribute is associated with.
     * This should only be called internally.
     * 
     * @param r
     */
    void setRelation(Relation r) {
        this.relation = r;
    }
    
    /**
     * Returns the relation that this attribute is associated with.
     * 
     * @return options
     */
    public Relation getRelation() {
        return relation;
    }
    
    public static Attribute fromXMLElement(Element e, XMLFactory f) {
        String relationName = e.getAttributeValue("relation");
        Relation r = f.getRelation(relationName);
        String attribName = e.getAttributeValue("name");
        return r.getAttribute(attribName);
    }
    
    public Element toXMLElement() {
        Element e = new Element("attribute");
        e.setAttribute("relation", relation.name);
        e.setAttribute("name", attributeName);
        return e;
    }
}
