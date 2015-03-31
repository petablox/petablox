// Relation.java, created Tue Jan 11 14:52:48 2000 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@alum.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package jwutil.collections;

import java.util.Collection;
import java.util.Set;

/**
 * <code>Relation</code> is a mathematical relation, accepting one to many
 * and many to one mappings.
 *
 * <p>It is similar to harpoon.Util.Collections.MultiMap but it is intended
 * to be simpler and better tailored for the implementation of the Pointer
 * Analysis algorithm.
 *
 * @author  Alexandru SALCIANU <salcianu@alum.mit.edu>
 * @version $Id: Relation.java,v 1.2 2005/05/05 18:52:19 joewhaley Exp $
 */
public interface Relation {
    
    /** Adds the pair <code>&lt;key, value&gt;</code> to the relation.
        Returns <code>true</code> if the new relation is bigger. */
    boolean add(Object key, Object value);


    /** Adds a relation from <code>key</code> to each element of the set
        <code>values</code>. <code>values</code> should not contain
        duplicated elements.
        Returns <code>true</code> if the new relation is bigger. */
    boolean addAll(Object key, Collection values);


    /** Removes the relation between <code>key</code> and 
        <code>value</code>. */ 
    void remove(Object key, Object value);


    /** Removes the relation between <code>key</code> and 
        any element from <code>values</code>. */
    void removeAll(Object key, Collection values);


    /** Removes all the relations attached to <code>key</code>. */
    void removeKey(Object key);


    /** Removes all the keys that satisfy <code>predicate.check()</code>. */
    void removeKeys(PredicateWrapper predicate);


    /** Removes all the values that satisfy <code>predicate.check()</code>. */
    void removeValues(PredicateWrapper predicate);


    /** Removes all the relations involving at least one object that
        satisfy <code>predicate.check()</code>. */
    void removeObjects(PredicateWrapper predicate);


    /** Checks the existence of the relation <code>&lt;key,value&gt;</code>. */
    boolean contains(Object key, Object value);


    /** Checks the existence of the <code>key</code> key in this relation. */
    boolean containsKey(Object key);


    /** Tests if this relation is empty or not. */
    boolean isEmpty();


    /** Returns the image of <code>key</code> through this relation.
        The returned collection is guarranted not to contain duplicates.
        Can return <code>null</code> if no value is attached to key.
        If the result is non-null, additions and removals on the returned
        collection take effect on the relation. */
    Set getValues(Object key);


    /** Returns all the keys appearing in this relation. The result is
        guaranted not to contain duplicates. */
    Set keys();


    /** Returns all the values appearing in this relation. */
    Set values();


    /** Combines <code>this</code> relation with a new one.
        A <code>null</code> parameter is considered to be an empty relation. */
    void union(Relation rel);


    /** Checks the equality of two relations */
    boolean equals(Object o);

    /** Returns the hashCode of a relation. */
    int hashCode();
    
    /** Returns the subrelation of this relation that contains
        only the keys that appear in <code>selected_keys</code>. */
    Relation select(Collection selected_keys);


    /** Visits all the entries <code>&lt;key,value&gt;</code> of
        <code>this</code> relation and calls <code>visitor.visit</code>
        on each of them. */
    void forAllEntries(EntryVisitor visitor);


    /** Clones this relation. */
    Object clone();
    
    /**
     * <code>RelationEntryVisitor</code> is a wrapper for a function that is
     called on a relation entry of the form <code>&lt;key,value&gt;</code>.
     There is no other way to pass a function in Java (no pointers to methods ...)
     * 
     * @author  Alexandru SALCIANU <salcianu@alum.mit.edu>
     * @version $Id: Relation.java,v 1.2 2005/05/05 18:52:19 joewhaley Exp $
     */
    public static interface EntryVisitor {
        /** Visits a <code>&lt;key,value&gt;</code> entry of a relation. */
        void visit(Object key, Object value);
    }
    
}
