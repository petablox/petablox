// Relation.java, created Mar 16, 2004 12:39:48 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.math.BigInteger;
import net.sf.bddbddb.dataflow.PartialOrder.Constraints;
import org.jdom.Element;

/**
 * Represents a relation in bddbddb.
 * 
 * @author jwhaley
 * @version $Id: Relation.java 534 2005-05-05 19:35:51Z joewhaley $
 */
public abstract class Relation {
    
    /**
     * Static relation id factory.
     */
    private static int relationCounter;
    
    /**
     * Name of this relation.
     */
    protected String name;
    
    /**
     * Attributes of this relation.
     */
    protected List/*<Attribute>*/ attributes;
    
    /**
     * Negated form of this relation, or null if it doesn't exist.
     */
    protected Relation negated;
    
    /**
     * Priority of this relation, used in determining iteration order.
     */
    int priority = 2;
    
    /**
     * Unique id number for this relation.
     */
    public final int id;
    
    /**
     * Ordering constraints for this relation.
     */
    Constraints constraints;
    
    /**
     * Code fragments to be executed whenever this relation is updated.
     */
    List onUpdate;
    
    /**
     * Flag saying whether or not this relation is initialized.
     */
    boolean isInitialized;

    /**
     * Create a new Relation.
     * 
     * @param solver  solver
     * @param name  name of relation
     * @param attributes  attributes for relation
     */
    protected Relation(Solver solver, String name, List attributes) {
        this.name = name;
        this.attributes = attributes;
        this.id = relationCounter++;
        solver.registerRelation(this);
        for (Iterator i = attributes.iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.relation == null) a.relation = this;
        }
        constraints = new Constraints();
        onUpdate = new LinkedList();
    }

    /**
     * Initialize this relation.
     */
    public abstract void initialize();

    /**
     * Load this relation from disk in its native format.
     * 
     * @throws IOException
     */
    public abstract void load() throws IOException;

    /**
     * Load the tuple form of this relation from disk.
     * 
     * @throws IOException
     */
    public abstract void loadTuples() throws IOException;

    /**
     * Load this relation in tuple form from the given file.
     * 
     * @param filename  the file to load
     * @throws IOException
     */
    public abstract void loadTuples(String filename) throws IOException;
    
    /**
     * Save the current value of this relation to disk in its native format.
     * 
     * @throws IOException
     */
    public abstract void save() throws IOException;

    /**
     * Save the current value of this relation to disk in tuple form.
     * 
     * @throws IOException
     */
    public abstract void saveTuples() throws IOException;

    /**
     * Save the value of this relation in tuple form to the given file.
     * 
     * @param filename  name of file to save
     * @throws IOException
     */
    public abstract void saveTuples(String filename) throws IOException;
    
    /**
     * Make a copy of this relation.  The new relation will have the same attributes
     * and a derived name.
     * 
     * @return  the new relation
     */
    public abstract Relation copy();

    /**
     * Free the memory associated with this relation.  After calling this, the relation can
     * no longer be used.
     */
    public abstract void free();

    /**
     * Return the number of tuples in this relation.
     * 
     * @return number of tuples in relation
     */
    public int size() {
        return (int) dsize();
    }

    /**
     * Return the number of tuples in this relation, in double format.
     * 
     * @return number of tuples in relation
     */
    public abstract double dsize();

    /**
     * Return an iterator over the tuples of this relation.
     * 
     * @return iterator of BigInteger[]
     */
    public abstract TupleIterator iterator();

    /**
     * Return an iterator over the values in the kth field of the relation. k is
     * zero-based.
     * 
     * @param k
     *            zero-based field number
     * @return iterator of BigInteger[]
     */
    public abstract TupleIterator iterator(int k);

    /**
     * Return an iterator over the tuples where the kth field has value j. k is
     * zero-based.
     * 
     * @param k
     *            zero-based field number
     * @param j
     *            value
     * @return iterator of BigInteger[]
     */
    public abstract TupleIterator iterator(int k, BigInteger j);

    /**
     * Return an iterator over the tuples where the fields match the values in
     * the given array. A -1 value in the array matches any value.
     * 
     * @param j
     *            values
     * @return iterator of BigInteger[]
     */
    public abstract TupleIterator iterator(BigInteger[] j);

    /**
     * Returns true iff this relation contains a tuple where the kth field is
     * value j. k is zero-based.
     * 
     * @param k
     *            zero-based field number
     * @param j
     *            value
     * @return whether the given value appears in the given field
     */
    public abstract boolean contains(int k, BigInteger j);

    /**
     * Adds the given tuple to this relation.  Returns true if the relation changed.
     * 
     * @param tuple  new tuple
     * @return  true iff relation changed
     */
    public abstract boolean add(BigInteger[] tuple);
    
    /**
     * Return the negated form of this relation, or null if it does not exist.
     * 
     * @return negated version of this relation, or null
     */
    public Relation getNegated() {
        return negated;
    }

    /**
     * Get or create the negated form of this relation.
     * 
     * @param solver
     *            solver
     * @return negated version of this relation
     */
    public Relation makeNegated(Solver solver) {
        if (negated != null) return negated;
        negated = solver.createRelation("!" + name, attributes);
        negated.negated = this;
        return negated;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return name;
    }

    /**
     * Returns a verbose representation of the relation
     */
     public abstract String verboseToString();
    
     public String elementsToString() {
         TupleIterator i = this.iterator();
         StringBuffer sb = new StringBuffer("[");
         while (i.hasNext()) {
             BigInteger t[] = i.nextTuple();
             sb.append('<');
             for (int k = 0; k < t.length; ++k) {
                 if (k > 0) sb.append(',');
                 sb.append(this.getAttribute(k).getDomain().toString(t[k]));
             }
             sb.append('>');
             if (i.hasNext()) sb.append(",\n");
         }
         sb.append(']');
         return sb.toString();
     }
     
    /**
     * Returns the list of attributes of this relation.
     * 
     * @return  attributes
     */
    public List getAttributes() {
        return attributes;
    }

    /**
     * Get the attribute at the given index.
     * 
     * @param x  index
     * @return  attribute
     */
    public Attribute getAttribute(int x) {
        return (Attribute) attributes.get(x);
    }

    /**
     * Get the attribute with the given name.
     * 
     * @param x  name
     * @return  attribute
     */
    public Attribute getAttribute(String x) {
        for (Iterator i = attributes.iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (x.equals(a.attributeName)) return a;
        }
        return null;
    }

    /**
     * Returns the number of attributes.
     * 
     * @return  number of attributes
     */
    public int numberOfAttributes() {
        return attributes.size();
    }

    /**
     * Returns the constraints.
     * 
     * @return  constraints
     */
    public Constraints getConstraints() {
        return constraints;
    }

    /**
     * Set the constraints for this relation.
     * 
     * @param constraints  The constraints to set.
     */
    public void setConstraints(Constraints constraints) {
        this.constraints = constraints;
    }
    
    /**
     * The hashCode for relations is deterministic.  (We use the unique id number.)
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return id;
    }

    /**
     * @return  map from names to variables
     */
    public Map getAttribNameMap() {
        HashMap nameToAttrib = new HashMap();
        for (Iterator i = attributes.iterator(); i.hasNext(); ) {
            Attribute a = (Attribute) i.next();
            nameToAttrib.put(a.attributeName, a);
        }
        return nameToAttrib;
    }
    
    public static Relation fromXMLElement(Element e, XMLFactory f) {
        // TODO.
        return null;
    }
    
    public Element toXMLElement() {
        // TODO.
        return null;
    }
}
