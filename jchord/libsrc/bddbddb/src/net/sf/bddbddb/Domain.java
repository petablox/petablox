// Domain.java, created Mar 16, 2004 3:44:18 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;
import jwutil.collections.IndexMap;

/**
 * A Domain represents a domain in bddbddb.
 * Domain objects are globally unique: There is only one Domain object for each
 * domain in the system.
 * 
 * @author jwhaley
 * @version $Id: Domain.java 513 2005-04-18 20:30:57Z joewhaley $
 */
public class Domain {
    /**
     * Name of domain.
     */
    protected String name;
    
    /**
     * Number of elements in domain.
     */
    protected BigInteger size;
    
    /**
     * Optional map from element numbers to string representations.
     */
    protected IndexMap map;

    /**
     * Construct a new domain.
     * This is not to be called externally.
     * 
     * @param name  name of domain
     * @param size  size of domain
     */
    Domain(String name, BigInteger size) {
        super();
        this.name = name;
        this.size = size;
    }

    /**
     * Load the string map for this domain.
     * 
     * @param in  input stream of this map
     * @throws IOException
     */
    public void loadMap(BufferedReader in) throws IOException {
        //map = IndexMap.load(name, in);
        map = IndexMap.loadStringMap(name, in);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return name;
    }

    /**
     * Returns the size of this domain.
     * 
     * @return  size of domain
     */
    public BigInteger getSize() {
        return size;
    }
    
    /**
     * Sets the size of this domain.
     * 
     * @param size  new size
     */
    public void setSize(BigInteger size) {
        this.size = size;
    }
    
    /**
     * Returns the string representation of the given element in this domain.
     * 
     * @param v  element number
     * @return  string representation
     */
    public String toString(BigInteger v) {
        int val = v.intValue();
        if (map == null || val < 0 || val >= map.size()) return Integer.toString(val);
        else return map.get(val).toString();
    }

    /**
     * Return the map for this domain if it exists, null otherwise.
     * 
     * @return  map for this domain, or null
     */
    public IndexMap getMap() {
        return map;
    }
    
    /**
     * Returns the index of the given named constant in this domain.
     * If it doesn't exist, output a warning message and add it to the domain,
     * giving it a new index.
     * 
     * @param constant  named constant to get
     * @return  index
     */
    public int namedConstant(String constant) {
        if (false && map == null) throw new IllegalArgumentException("No constant map for Domain " + name + " in which to look up constant " + constant);
        if (map == null) map = new IndexMap(name);
        if (!map.contains(constant)) System.err.println("Warning: Constant " + constant + " not found in map for relation " + name);
        return map.get(constant);
    }
}
