// InferenceRule.java, created Mar 16, 2004 12:41:14 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jwutil.collections.GenericMultiMap;
import jwutil.collections.LinearMap;
import jwutil.collections.MultiMap;
import jwutil.graphs.Navigator;
import jwutil.io.SystemProperties;
import jwutil.util.Assert;
import net.sf.bddbddb.ir.highlevel.Copy;
import net.sf.bddbddb.ir.highlevel.Difference;
import net.sf.bddbddb.ir.highlevel.GenConstant;
import net.sf.bddbddb.ir.highlevel.Invert;
import net.sf.bddbddb.ir.highlevel.Join;
import net.sf.bddbddb.ir.highlevel.JoinConstant;
import net.sf.bddbddb.ir.highlevel.Project;
import net.sf.bddbddb.ir.highlevel.Rename;
import net.sf.bddbddb.ir.highlevel.Union;
import net.sf.bddbddb.ir.highlevel.Universe;
import org.jdom.Element;

/**
 * An InferenceRule represents a single Datalog rule.
 * 
 * @author jwhaley
 * @version $Id: InferenceRule.java 549 2005-05-17 10:17:33Z joewhaley $
 */
public abstract class InferenceRule implements IterationElement {
    
    /**
     * Static rule id factory.
     */
    private static int ruleCount;
    
    /**
     * Link to solver.
     */
    protected final Solver solver;
    
    /**
     * Unique id number for this rule.
     */
    public final int id;
    
    /**
     * List of subgoals for the rule (i.e. the terms on the right hand side).
     * May be empty.
     */
    protected List/*<RuleTerm>*/ top;
    
    /**
     * Head of the rule (i.e. the term on the left hand side).
     */
    protected RuleTerm bottom;
    
    /**
     * Set of variables that are necessary.  Initialized after calling initialize().
     */
    protected Set/*<Variable>*/ necessaryVariables;
    
    /**
     * Set of variables that are unnecessary.  Initialized after calling initialize().
     */
    protected Set/*<Variable>*/ unnecessaryVariables;
    
    /**
     * List of IR instructions that implement this rule.  Used for compilation.
     */
    List ir_full, ir_incremental;
    
    /**
     * Set of old values for each of the subgoals.  Used for incrementalization;
     */
    Relation[] oldRelationValues;
    
    /**
     * Flag specifying whether or not to split this rule.
     */
    boolean split;

    /**
     * Flag specifying whether to limit the generated tuples to a single one.
     */
    boolean single;
    
    /**
     * The priority of this rule, used in determining iteration order.
     */
    int priority = 1;
    
    /**
     * Trace flags to control output of trace information about this rule.
     */
    boolean TRACE, TRACE_FULL;
    
    /**
     * Flag specifying whether or not to incrementalize computation of this rule.
     */
    boolean incrementalize = !SystemProperties.getProperty("incremental", "yes").equals("no");
    
    /**
     * Flag specifying whether to cache values before or after renaming.
     * If we cache before rename, we can sometimes avoid renaming the whole relation.
     * However, in some cases we might need to rename both the diff and the whole relation.
     */
    boolean cache_before_rename = true;
    
    /**
     * Flag that shows whether or not this inference rule has been initialized yet.
     */
    boolean isInitialized;
    
    /**
     * Code fragments to be executed before invoking this rule.
     */
    List preCode;

    /**
     * Code fragments to be executed after invoking this rule.
     */
    List postCode;

    /**
     * Relations (besides the head relation) that are modified as a side effect of this rule.
     * Used with code fragments.
     */
    List extraDefines;
    
    /**
     * Construct a new inference rule.
     * 
     * @param solver  solver
     * @param top  subgoal terms
     * @param bottom  head term
     */
    protected InferenceRule(Solver solver, List/*<RuleTerm>*/ top, RuleTerm bottom) {
        this(solver, top, bottom, ruleCount++);
    }
    protected InferenceRule(Solver solver, List/*<RuleTerm>*/ top, RuleTerm bottom, int id) {
        this.solver = solver;
        this.top = top;
        this.bottom = bottom;
        this.TRACE = solver.TRACE;
        this.TRACE_FULL = solver.TRACE_FULL;
        this.id = id;
        this.preCode = new LinkedList();
        this.postCode = new LinkedList();
        this.extraDefines = new LinkedList();
    }

    /**
     * Initialize this inference rule, if it hasn't been already.
     */
    void initialize() {
        if (isInitialized) return;
        calculateNecessaryVariables();
        isInitialized = true;
    }

    /**
     * Returns the list of subgoals.  Subgoals are the terms to the right
     * of the ":-".
     * 
     * @return  list of subgoals
     */
    public List/*<RuleTerm>*/ getSubgoals() {
        return top;
    }
    
    /**
     * Returns the head term.  That is the term on the left of the ":-".
     * 
     * @return  the head term
     */
    public RuleTerm getHead() {
        return bottom;
    }
    
    /**
     * Calculate the set of necessary variables in the given list of terms
     * assuming that the given collection of variables has already been listed once.
     * 
     * @param s  initial collection of variables that have been listed
     * @param terms  terms to check
     * @return  set of necessary variables
     */
    static Set calculateNecessaryVariables(Collection s, List terms) {
        Set necessaryVariables = new HashSet();
        Set unnecessaryVariables = new HashSet(s);
        for (int i = 0; i < terms.size(); ++i) {
            RuleTerm rt = (RuleTerm) terms.get(i);
            for (int j = 0; j < rt.variables.size(); ++j) {
                Variable v = (Variable) rt.variables.get(j);
                if (necessaryVariables.contains(v)) continue;
                if (unnecessaryVariables.contains(v)) {
                    necessaryVariables.add(v);
                    unnecessaryVariables.remove(v);
                } else {
                    unnecessaryVariables.add(v);
                }
            }
        }
        return necessaryVariables;
    }

    /**
     * Calculate and return the set of necessary variables for this rule.
     * Sets the necessaryVariables and unnecessaryVariables fields.
     * 
     * @return  set of necessary variables.
     */
    protected Set calculateNecessaryVariables() {
        necessaryVariables = new HashSet();
        unnecessaryVariables = new HashSet();
        for (int i = 0; i < top.size(); ++i) {
            RuleTerm rt = (RuleTerm) top.get(i);
            for (int j = 0; j < rt.variables.size(); ++j) {
                Variable v = (Variable) rt.variables.get(j);
                if (necessaryVariables.contains(v)) continue;
                if (unnecessaryVariables.contains(v)) {
                    necessaryVariables.add(v);
                    unnecessaryVariables.remove(v);
                } else {
                    unnecessaryVariables.add(v);
                }
            }
        }
        for (int j = 0; j < bottom.variables.size(); ++j) {
            Variable v = (Variable) bottom.variables.get(j);
            if (necessaryVariables.contains(v)) continue;
            if (unnecessaryVariables.contains(v)) {
                necessaryVariables.add(v);
                unnecessaryVariables.remove(v);
            } else {
                unnecessaryVariables.add(v);
            }
        }
        return necessaryVariables;
    }

    /**
     * Checks to see if there are any variables that only appear once.
     */
    public Variable checkUniversalVariables() {
        calculateNecessaryVariables();
        for (Iterator i = unnecessaryVariables.iterator(); i.hasNext(); ) {
            Variable v = (Variable) i.next();
            if (v instanceof Constant) continue;
            if (!"_".equals(v.name)) {
                return v;
            }
        }
        return null;
    }
    
    /**
     * Update the head relation of this rule based on the subgoal relations.
     * Returns true if the head relation changed.
     * 
     * @return  true if the head relation changed
     */
    public abstract boolean update();

    /**
     * Report statistics about this rule.
     */
    public abstract void reportStats();

    /**
     * Free the memory associated with this rule.  After calling this, the rule can
     * no longer be used.
     */
    public void free() {
        if (oldRelationValues != null) {
            for (int i = 0; i < oldRelationValues.length; ++i) {
                oldRelationValues[i].free();
            }
        }
    }

    /**
     * Given a collection of rules, returns a multimap that maps relations to
     * the rules that use that relation.
     * 
     * @param rules  collection of rules
     * @return  multimap from relations to rules that use them
     */
    public static MultiMap getRelationToUsingRule(Collection rules) {
        MultiMap mm = new GenericMultiMap();
        for (Iterator i = rules.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof InferenceRule) {
                InferenceRule ir = (InferenceRule) o;
                for (Iterator j = ir.top.iterator(); j.hasNext();) {
                    RuleTerm rt = (RuleTerm) j.next();
                    mm.add(rt.relation, ir);
                }
            }
        }
        return mm;
    }

    /**
     * Given a collection of rules, returns a multimap that maps relations to
     * the rules that define that relation.
     * 
     * @param rules  collection of rules
     * @return  multimap from relations to rules that define them
     */
    public static MultiMap getRelationToDefiningRule(Collection rules) {
        MultiMap mm = new GenericMultiMap();
        for (Iterator i = rules.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof InferenceRule) {
                InferenceRule ir = (InferenceRule) o;
                mm.add(ir.bottom.relation, ir);
                for (Iterator j = ir.extraDefines.iterator(); j.hasNext(); ) {
                    Relation r = (Relation) j.next();
                    mm.add(r, ir);
                }
            }
        }
        return mm;
    }

    /**
     * Splits a rule into a collection of rules, such that each of the new rules
     * has only two subgoals.  The current rule is simply mutated and not returned
     * in the collection.
     * 
     * @param myIndex  index number used to name the new rules
     * @return  collection of new inference rules
     */
    public Collection/*<InferenceRule>*/ split(int myIndex) {
        List newRules = new LinkedList();
        int count = 0;
        while (top.size() > 2) {
            RuleTerm rt1 = (RuleTerm) top.remove(0);
            RuleTerm rt2 = (RuleTerm) top.remove(0);
            if (TRACE) solver.out.println("Combining " + rt1 + " and " + rt2 + " into a new rule.");
            // Calculate our new necessary variables.
            LinkedList ll = new LinkedList();
            ll.addAll(rt1.variables);
            ll.addAll(rt2.variables);
            LinkedList terms = new LinkedList(top);
            terms.add(bottom);
            Set myNewNecessaryVariables = calculateNecessaryVariables(ll, terms);
            List newTop = new LinkedList();
            newTop.add(rt1);
            newTop.add(rt2);
            // Make a new relation for the bottom.
            Map neededVariables = new LinearMap();
            Map variableOptions = new HashMap();
            Iterator i = rt1.variables.iterator();
            Iterator j = rt1.relation.attributes.iterator();
            while (i.hasNext()) {
                Variable v = (Variable) i.next();
                Attribute a = (Attribute) j.next();
                Domain d = a.attributeDomain;
                String o = a.attributeOptions;
                if (!myNewNecessaryVariables.contains(v)) continue;
                Domain d2 = (Domain) neededVariables.get(v);
                if (d2 != null && d != d2) {
                    throw new IllegalArgumentException(v + ": " + d + " != " + d2);
                }
                neededVariables.put(v, d);
                String o2 = (String) variableOptions.get(v);
                if (o == null || o.equals("")) o = o2;
                if (o2 == null || o2.equals("")) o2 = o;
                if (o != null && o2 != null && !o.equals(o2)) {
                    throw new IllegalArgumentException(v + ": " + o + " != " + o2);
                }
                variableOptions.put(v, o);
            }
            i = rt2.variables.iterator();
            j = rt2.relation.attributes.iterator();
            while (i.hasNext()) {
                Variable v = (Variable) i.next();
                Attribute a = (Attribute) j.next();
                Domain d = a.attributeDomain;
                String o = a.attributeOptions;
                if (!myNewNecessaryVariables.contains(v)) continue;
                Domain d2 = (Domain) neededVariables.get(v);
                if (d2 != null && d != d2) {
                    throw new IllegalArgumentException(v + ": " + d + " != " + d2);
                }
                neededVariables.put(v, d);
                String o2 = (String) variableOptions.get(v);
                if (o == null || o.equals("")) o = o2;
                if (o2 == null || o2.equals("")) o2 = o;
                if (o != null && o2 != null && !o.equals(o2)) {
                    throw new IllegalArgumentException(v + ": " + o + " != " + o2);
                }
                variableOptions.put(v, o);
            }
            // Make a new relation for the bottom.
            List attributes = new LinkedList();
            List newVariables = new LinkedList();
            for (i = neededVariables.entrySet().iterator(); i.hasNext();) {
                Map.Entry e = (Map.Entry) i.next();
                Variable v = (Variable) e.getKey();
                Domain d = (Domain) e.getValue();
                String o = (String) variableOptions.get(v);
                Attribute a = new Attribute("_" + v, d, o);
                attributes.add(a);
                newVariables.add(v);
            }
            String relationName = bottom.relation.name + "_" + myIndex + "_" + count;
            if (TRACE) solver.out.println("New attributes: " + attributes);
            Relation newRelation = solver.createRelation(relationName, attributes);
            //newRelation.priority = bottom.relation.priority;
            if (TRACE) solver.out.println("New relation: " + newRelation);
            RuleTerm newBottom = new RuleTerm(newRelation, newVariables);
            InferenceRule newRule = solver.createInferenceRule(newTop, newBottom);
            if (TRACE) solver.out.println("New rule: " + newRule);
            newRule.calculateNecessaryVariables();
            if (TRACE) solver.out.println("Necessary variables: " + newRule.necessaryVariables);
            //s.rules.add(newRule);
            newRules.add(newRule);
            newRule.copyOptions(this);
            boolean changed = newRule.preCode.addAll(this.preCode);
            this.preCode.clear();
            if (changed) {
                // todo: split extraDefines into pre- and post-
                newRule.extraDefines.addAll(this.extraDefines);
            }
            // Now include the bottom of the new rule on the top of our rule.
            top.add(0, newBottom);
            // Reinitialize this rule because the terms have changed.
            this.calculateNecessaryVariables();
            if (TRACE) solver.out.println("Current rule is now: " + this);
            if (TRACE) solver.out.println("My new necessary variables: " + necessaryVariables);
            Assert._assert(necessaryVariables.equals(myNewNecessaryVariables));
            ++count;
        }
        return newRules;
    }

    /**
     * Utility function to retain in a multimap only the elements in a given collection.
     * 
     * @param mm  multimap
     * @param c  collection
     */
    static void retainAll(MultiMap mm, Collection c) {
        for (Iterator i = mm.keySet().iterator(); i.hasNext();) {
            Object o = i.next();
            if (!c.contains(o)) {
                i.remove();
                continue;
            }
            Collection vals = mm.getValues(o);
            for (Iterator j = vals.iterator(); j.hasNext();) {
                Object o2 = j.next();
                if (!c.contains(o2)) {
                    j.remove();
                }
            }
            if (vals.isEmpty()) i.remove();
        }
    }

    /**
     * Utility function to remove from a multimap all of the elements in a given collection.
     * 
     * @param mm  multimap
     * @param c  collection
     */
    static void removeAll(MultiMap mm, Collection c) {
        for (Iterator i = mm.keySet().iterator(); i.hasNext();) {
            Object o = i.next();
            if (c.contains(o)) {
                i.remove();
                continue;
            }
            Collection vals = mm.getValues(o);
            for (Iterator j = vals.iterator(); j.hasNext();) {
                Object o2 = j.next();
                if (c.contains(o2)) {
                    j.remove();
                }
            }
            if (vals.isEmpty()) i.remove();
        }
    }

    /**
     * Copy the options from another rule into this one.
     * 
     * @param that
     */
    public void copyOptions(InferenceRule that) {
        this.TRACE = that.TRACE;
        this.TRACE_FULL = that.TRACE_FULL;
        this.incrementalize = that.incrementalize;
        this.cache_before_rename = that.cache_before_rename;
        //this.priority = that.priority;
    }
    
    /**
     * A navigator that can navigate over rule dependencies.
     * next() on a rule returns its head relation, and
     * next() on a relation returns the rules that use that relation.
     * Likewise, prev() on a rule returns its subgoal relations,
     * and prev() on a relation returns the rules that define that relation.
     * 
     * This class also allows you to remove edges/nodes from it.
     */
    public static class DependenceNavigator implements Navigator {
        MultiMap relationToUsingRule;
        MultiMap relationToDefiningRule;

        /**
         * Construct a new DependenceNavigator using the given collection of rules.
         * 
         * @param rules  rules
         */
        public DependenceNavigator(Collection/*<InferenceRule>*/ rules) {
            this(getRelationToUsingRule(rules), getRelationToDefiningRule(rules));
        }

        /**
         * Retain only the relations/rules in the given collection.
         * 
         * @param c  collection to retain
         */
        public void retainAll(Collection c) {
            InferenceRule.retainAll(relationToUsingRule, c);
            InferenceRule.retainAll(relationToDefiningRule, c);
        }

        /**
         * Remove all of the relations/rules in the given collection.
         * 
         * @param c  collection to remove
         */
        public void removeAll(Collection c) {
            InferenceRule.removeAll(relationToUsingRule, c);
            InferenceRule.removeAll(relationToDefiningRule, c);
        }

        /**
         * Remove a specific edge from this navigator.
         * 
         * @param from  source of edge
         * @param to  target of edge
         */
        public void removeEdge(Object from, Object to) {
            if (from instanceof InferenceRule) {
                InferenceRule ir = (InferenceRule) from;
                Relation r = (Relation) to;
                relationToDefiningRule.remove(r, ir);
            } else {
                Relation r = (Relation) from;
                InferenceRule ir = (InferenceRule) to;
                relationToUsingRule.remove(r, ir);
            }
        }

        /**
         * Construct a new DependenceNavigator that is a copy of another
         * DependenceNavigator.
         * 
         * @param that  the one to copy from
         */
        public DependenceNavigator(DependenceNavigator that) {
            this(((GenericMultiMap) that.relationToUsingRule).copy(), ((GenericMultiMap) that.relationToDefiningRule).copy());
        }

        /**
         * Not to be called externally.
         * 
         * @param relationToUsingRule
         * @param relationToDefiningRule
         */
        private DependenceNavigator(MultiMap relationToUsingRule, MultiMap relationToDefiningRule) {
            super();
            this.relationToUsingRule = relationToUsingRule;
            this.relationToDefiningRule = relationToDefiningRule;
        }

        /*
         * (non-Javadoc)
         * 
         * @see joeq.Util.Graphs.Navigator#next(java.lang.Object)
         */
        public Collection next(Object node) {
            if (node instanceof InferenceRule) {
                InferenceRule ir = (InferenceRule) node;
                Collection extras = ir.extraDefines;
                if (extras == null || extras.isEmpty()) {
                    if (relationToDefiningRule.contains(ir.bottom.relation, ir)) return Collections.singleton(ir.bottom.relation);
                    else return Collections.EMPTY_SET;
                } else {
                    LinkedList result = new LinkedList();
                    for (Iterator i = extras.iterator(); i.hasNext(); ) {
                        Relation r = (Relation) i.next();
                        if (relationToDefiningRule.contains(r, ir)) result.add(r);
                    }
                    if (relationToDefiningRule.contains(ir.bottom.relation, ir)) result.add(ir.bottom.relation);
                    return result;
                }
            } else {
                Relation r = (Relation) node;
                Collection c = relationToUsingRule.getValues(r);
                return c;
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see joeq.Util.Graphs.Navigator#prev(java.lang.Object)
         */
        public Collection prev(Object node) {
            if (node instanceof InferenceRule) {
                InferenceRule ir = (InferenceRule) node;
                List list = new LinkedList();
                for (Iterator i = ir.top.iterator(); i.hasNext();) {
                    RuleTerm rt = (RuleTerm) i.next();
                    if (relationToUsingRule.contains(rt.relation, ir)) list.add(rt.relation);
                }
                return list;
            } else {
                Relation r = (Relation) node;
                Collection c = relationToDefiningRule.getValues(r);
                return c;
            }
        }
    }

    /**
     * Helper function for IR generation.
     * Generate code to project away unnecessary variables and restrict constants.
     * 
     * @param ir  list of IR instructions
     * @param rt  subgoal term
     * @return  resulting relation after projecting and restricting
     */
    Relation generate1(List ir, RuleTerm rt) {
        Relation top_r = rt.relation;
        Collection varsToProject = new LinkedList(rt.variables);
        
        varsToProject.removeAll(necessaryVariables);
        if (!varsToProject.isEmpty()) {
            if (solver.TRACE) solver.out.println("Projecting away variables: " + varsToProject);
            List newAttributes = new LinkedList();
            for (int j = 0; j < rt.numberOfVariables(); ++j) {
                Variable v = rt.getVariable(j);
                if (!varsToProject.contains(v)) {
                    newAttributes.add(top_r.getAttribute(j));
                } else if (v instanceof Constant) {
                    Relation new_r = top_r.copy();
                    new_r.initialize();
                    Attribute a = top_r.getAttribute(j);
                    long value = ((Constant) v).value;
                    JoinConstant jc = new JoinConstant(new_r, top_r, a, value);
                    if (solver.TRACE) solver.out.println("Generated: " + jc);
                    ir.add(jc);
                    top_r = new_r;
                }
            }
            Relation new_r = solver.createRelation(top_r + "_p", newAttributes);
            new_r.initialize();
            Project p = new Project(new_r, top_r);
            if (solver.TRACE) solver.out.println("Generated: " + p);
            ir.add(p);
            top_r = new_r;
        }
        return top_r;
    }

    /**
     * Generate and return the IR that implements this rule.
     * 
     * @return  list of IR instructions
     */
    public List/*<Operation>*/ generateIR() {
        if (ir_full != null) return ir_full;
        List ir = new LinkedList();
        Relation result = null;
        Map varToAttrib = new HashMap();
        int x = 0;
        for (Iterator i = top.iterator(); i.hasNext(); ++x) {
            RuleTerm rt = (RuleTerm) i.next();
            // Step 1: Project away unnecessary variables and restrict
            // constants.
            Relation r = generate1(ir, rt);
            // If we are incrementalizing, cache copies of the input relations.
            // This happens after we have quantified away and restricted constants,
            // but before we do renaming.
            if (incrementalize && cache_before_rename) {
                if (oldRelationValues == null) oldRelationValues = new Relation[top.size()];
                oldRelationValues[x] = r.copy();
                oldRelationValues[x].initialize();
                Copy c = new Copy(oldRelationValues[x], r);
                if (solver.TRACE) solver.out.println("Generated: " + c);
                ir.add(c);
            }
            // Calculate renames.
            List newAttributes = new LinkedList();
            Map renames = new LinearMap();
            for (int j = 0; j < rt.numberOfVariables(); ++j) {
                Variable v = rt.getVariable(j);
                if (unnecessaryVariables.contains(v)) continue;
                Attribute a = rt.relation.getAttribute(j);
                Attribute a2 = (Attribute) varToAttrib.get(v);
                if (a2 == null) {
                    if (result != null && result.attributes.contains(a)) {
                        // Attribute is already present in result, use a
                        // different attribute.
                        a2 = new Attribute(a.attributeName + '\'', a.attributeDomain, "");
                        renames.put(a, a2);
                        a = a2;
                    }
                    varToAttrib.put(v, a2 = a);
                } else if (!a2.equals(a)) {
                    renames.put(a, a2);
                }
                newAttributes.add(a2);
            }
            if (!renames.isEmpty()) {
                Relation new_r = solver.createRelation(r + "_r", newAttributes);
                new_r.initialize();
                Rename rename = new Rename(new_r, r, renames);
                if (solver.TRACE) solver.out.println("Generated: " + rename);
                ir.add(rename);
                r = new_r;
            }
            // If we are incrementalizing, cache copies of the input relations.
            // If the option is set, we do this after the rename.
            if (incrementalize && !cache_before_rename) {
                if (oldRelationValues == null) oldRelationValues = new Relation[top.size()];
                oldRelationValues[x] = r.copy();
                oldRelationValues[x].initialize();
                Copy c = new Copy(oldRelationValues[x], r);
                if (solver.TRACE) solver.out.println("Generated: " + c);
                ir.add(c);
            }
            if (result != null) {
                // Do a "join".
                newAttributes = new LinkedList(result.attributes);
                newAttributes.removeAll(r.attributes);
                newAttributes.addAll(r.attributes);
                Relation new_r = solver.createRelation(r + "_j", newAttributes);
                new_r.initialize();
                Join join = new Join(new_r, r, result);
                if (solver.TRACE) solver.out.println("Generated: " + join);
                ir.add(join);
                result = new_r;
            } else {
                // This is the first loop iteration, so there is no prior result
                // to join with.
                result = r;
            }
            if (solver.TRACE && result != null) solver.out.println("Result attributes after join: " + result.attributes);
            // Project away unnecessary attributes.
            List toProject = new LinkedList();
            outer : for (int k = 0; k < rt.numberOfVariables(); ++k) {
                Variable v = rt.getVariable(k);
                if (unnecessaryVariables.contains(v)) continue;
                Attribute a = (Attribute) varToAttrib.get(v);
                Assert._assert(a != null);
                if (solver.TRACE) solver.out.print("Variable " + v + " Attribute " + a + ": ");
                Assert._assert(result.attributes.contains(a));
                if (bottom.variables.contains(v)) {
                    if (solver.TRACE) solver.out.println("variable needed for bottom");
                    continue;
                }
                Iterator j = top.iterator();
                while (j.next() != rt);
                while (j.hasNext()) {
                    RuleTerm rt2 = (RuleTerm) j.next();
                    if (rt2.variables.contains(v)) {
                        if (solver.TRACE) solver.out.println("variable needed for future term");
                        continue outer;
                    }
                }
                if (solver.TRACE) solver.out.println("Not needed anymore, projecting away");
                toProject.add(a);
            }
            if (!toProject.isEmpty()) {
                newAttributes = new LinkedList(result.attributes);
                newAttributes.removeAll(toProject);
                Relation result2 = solver.createRelation(result + "_p2", newAttributes);
                result2.initialize();
                Project p = new Project(result2, result);
                if (solver.TRACE) solver.out.println("Generated: " + p);
                ir.add(p);
                result = result2;
            }
        }
        // Rename result to match head relation.
        Map renames = new LinearMap();
        List newAttributes = new LinkedList();
        for (int j = 0; j < bottom.numberOfVariables(); ++j) {
            Variable v = bottom.getVariable(j);
            if (unnecessaryVariables.contains(v)) continue;
            Attribute a = bottom.relation.getAttribute(j);
            Attribute a2 = (Attribute) varToAttrib.get(v);
            //solver.out.println("Variable "+v+" has attribute "+a2);
            Assert._assert(a2 != null);
            if (!a2.equals(a)) {
                renames.put(a2, a);
            }
            newAttributes.add(a);
        }
        if (!renames.isEmpty()) {
            Relation result2 = solver.createRelation(result + "_r2", newAttributes);
            result2.initialize();
            Rename rename = new Rename(result2, result, renames);
            if (solver.TRACE) solver.out.println("Generated: " + rename);
            ir.add(rename);
            result = result2;
        }
        // Restrict constants.
        for (int j = 0; j < bottom.numberOfVariables(); ++j) {
            Variable v = bottom.getVariable(j);
            if (v instanceof Constant) {
                Attribute a = bottom.relation.getAttribute(j);
                long value = ((Constant) v).getValue();
                if (result == null) {
                    // Empty right-hand-side.
                    result = bottom.relation.copy();
                    result.initialize();
                    GenConstant c = new GenConstant(result, a, value);
                    if (solver.TRACE) solver.out.println("Generated: " + c);
                    ir.add(c);
                } else {
                    List a2 = new LinkedList(result.attributes);
                    a2.add(a);
                    Relation result2 = solver.createRelation(result.name+"_jc", a2);
                    result2.initialize();
                    JoinConstant jc = new JoinConstant(result2, result, a, value);
                    if (solver.TRACE) solver.out.println("Generated: " + jc);
                    ir.add(jc);
                    result = result2;
                }
            }
        }
        if (result != null) {
            // Finally, union in the result.
            Union u = new Union(bottom.relation, bottom.relation, result);
            if (solver.TRACE) solver.out.println("Generated: " + u);
            ir.add(u);
        } else {
            // No constants: Universal set.
            Universe u = new Universe(bottom.relation);
            if (solver.TRACE) solver.out.println("Generated: " + u);
            ir.add(u);
        }
        if (bottom.relation.negated != null) {
            // Update negated.
            Invert i = new Invert(bottom.relation.negated, bottom.relation);
            if (solver.TRACE) solver.out.println("Generated: " + i);
            ir.add(i);
        }
        ir_full = ir;
        return ir;
    }

    /**
     * Generate and return the IR that implements the incrementalized version of this rule.
     * 
     * @return  list of IR instructions
     */
    public List/*<Operation>*/ generateIR_incremental() {
        if (ir_incremental != null) return ir_incremental;
        LinkedList ir = new LinkedList();
        Map varToAttrib = new HashMap();
        Relation[] allRelationValues = new Relation[top.size()];
        Relation[] newRelationValues = new Relation[top.size()];
        List[] toProject = new LinkedList[top.size()];
        List oldAttributes = null;
        int x = 0;
        for (Iterator i = top.iterator(); i.hasNext(); ++x) {
            RuleTerm rt = (RuleTerm) i.next();
            // Step 1: Project away unnecessary variables and restrict
            // constants.
            Relation r = generate1(ir, rt);
            allRelationValues[x] = r;
            if (cache_before_rename) {
                if (oldRelationValues == null) oldRelationValues = new Relation[top.size()];
                if (oldRelationValues[x] == null) {
                    oldRelationValues[x] = r.copy();
                    oldRelationValues[x].initialize();
                }
                // TODO: calculate if we need the whole relation.
                newRelationValues[x] = oldRelationValues[x].copy();
                newRelationValues[x].initialize();
                Difference diff = new Difference(newRelationValues[x], allRelationValues[x], oldRelationValues[x]);
                if (solver.TRACE) solver.out.println("Generated: " + diff);
                ir.add(diff);
                Copy copy = new Copy(oldRelationValues[x], allRelationValues[x]);
                if (solver.TRACE) solver.out.println("Generated: " + copy);
                ir.add(copy);
            }
            // Calculate renames.
            List newAttributes = new LinkedList();
            Map renames = new LinearMap();
            for (int j = 0; j < rt.numberOfVariables(); ++j) {
                Variable v = rt.getVariable(j);
                if (unnecessaryVariables.contains(v)) continue;
                Attribute a = rt.relation.getAttribute(j);
                Attribute a2 = (Attribute) varToAttrib.get(v);
                if (a2 == null) {
                    if (oldAttributes != null && oldAttributes.contains(a)) {
                        // Attribute is already present in result, use a
                        // different attribute.
                        a2 = new Attribute(a.attributeName + '\'', a.attributeDomain, "");
                        renames.put(a, a2);
                        a = a2;
                    }
                    varToAttrib.put(v, a2 = a);
                } else if (!a2.equals(a)) {
                    renames.put(a, a2);
                }
                newAttributes.add(a2);
            }
            if (!renames.isEmpty()) {
                if (cache_before_rename) {
                    Relation new_r = solver.createRelation(newRelationValues[x] + "_r", newAttributes);
                    new_r.initialize();
                    Rename rename = new Rename(new_r, newRelationValues[x], renames);
                    if (solver.TRACE) solver.out.println("Generated: " + rename);
                    ir.add(rename);
                    newRelationValues[x] = new_r;
                }
                // TODO: only rename whole relation if it is actually needed.
                Relation new_r = solver.createRelation(r + "_r", newAttributes);
                new_r.initialize();
                Rename rename = new Rename(new_r, r, renames);
                if (solver.TRACE) solver.out.println("Generated: " + rename);
                ir.add(rename);
                r = new_r;
            }
            allRelationValues[x] = r;
            if (!cache_before_rename) {
                if (oldRelationValues == null) oldRelationValues = new Relation[top.size()];
                if (oldRelationValues[x] == null) {
                    oldRelationValues[x] = r.copy();
                    oldRelationValues[x].initialize();
                }
                newRelationValues[x] = oldRelationValues[x].copy();
                newRelationValues[x].initialize();
                Difference diff = new Difference(newRelationValues[x], allRelationValues[x], oldRelationValues[x]);
                if (solver.TRACE) solver.out.println("Generated: " + diff);
                ir.add(diff);
                Copy copy = new Copy(oldRelationValues[x], allRelationValues[x]);
                if (solver.TRACE) solver.out.println("Generated: " + copy);
                ir.add(copy);
            }
            oldAttributes = new LinkedList();
            if (x > 0) oldAttributes.addAll(allRelationValues[x - 1].attributes);
            oldAttributes.removeAll(r.attributes);
            oldAttributes.addAll(r.attributes);
            // Project away unnecessary attributes.
            toProject[x] = new LinkedList();
            outer : for (int k = 0; k < rt.numberOfVariables(); ++k) {
                Variable v = rt.getVariable(k);
                if (unnecessaryVariables.contains(v)) continue;
                Attribute a = (Attribute) varToAttrib.get(v);
                Assert._assert(a != null);
                if (solver.TRACE) solver.out.print("Variable " + v + " Attribute " + a + ": ");
                Assert._assert(oldAttributes.contains(a));
                if (bottom.variables.contains(v)) {
                    if (solver.TRACE) solver.out.println("variable needed for bottom");
                    continue;
                }
                Iterator j = top.iterator();
                while (j.next() != rt);
                while (j.hasNext()) {
                    RuleTerm rt2 = (RuleTerm) j.next();
                    if (rt2.variables.contains(v)) {
                        if (solver.TRACE) solver.out.println("variable needed for future term");
                        continue outer;
                    }
                }
                if (solver.TRACE) solver.out.println("Not needed anymore, projecting away");
                toProject[x].add(a);
            }
        }
        for (x = 0; x < newRelationValues.length; ++x) {
            Relation result = newRelationValues[x];
            for (int y = 0; y < allRelationValues.length; ++y) {
                if (x != y) {
                    Relation r = allRelationValues[y];
                    List newAttributes = new LinkedList(result.attributes);
                    newAttributes.removeAll(r.attributes);
                    newAttributes.addAll(r.attributes);
                    Relation new_r = solver.createRelation(result + "_j", newAttributes);
                    new_r.initialize();
                    Join join = new Join(new_r, r, result);
                    if (solver.TRACE) solver.out.println("Generated: " + join);
                    ir.add(join);
                    result = new_r;
                }
                if (!toProject[y].isEmpty()) {
                    List newAttributes = new LinkedList(result.attributes);
                    newAttributes.removeAll(toProject[y]);
                    Relation result2 = solver.createRelation(result + "_p2", newAttributes);
                    result2.initialize();
                    Project p = new Project(result2, result);
                    if (solver.TRACE) solver.out.println("Generated: " + p);
                    ir.add(p);
                    result = result2;
                }
            }
            // Rename result to match head relation.
            Map renames = new LinearMap();
            List renamedAttributes = new LinkedList();
            for (int j = 0; j < bottom.numberOfVariables(); ++j) {
                Variable v = bottom.getVariable(j);
                if (unnecessaryVariables.contains(v)) continue;
                Attribute a = bottom.relation.getAttribute(j);
                Attribute a2 = (Attribute) varToAttrib.get(v);
                //solver.out.println("Variable "+v+" has attribute "+a2);
                Assert._assert(a2 != null);
                if (!a2.equals(a)) {
                    renames.put(a2, a);
                }
                renamedAttributes.add(a);
            }
            if (!renames.isEmpty()) {
                Relation result2 = solver.createRelation(result + "_r2", renamedAttributes);
                result2.initialize();
                Rename rename = new Rename(result2, result, renames);
                if (solver.TRACE) solver.out.println("Generated: " + rename);
                ir.add(rename);
                result = result2;
            }
            // Restrict constants.
            for (int j = 0; j < bottom.numberOfVariables(); ++j) {
                Variable v = bottom.getVariable(j);
                if (v instanceof Constant) {
                    Attribute a = bottom.relation.getAttribute(j);
                    long value = ((Constant) v).getValue();
                    List a2 = new LinkedList(result.attributes);
                    a2.add(a);
                    Relation result2 = solver.createRelation(result.name+"_jc", a2);
                    result2.initialize();
                    JoinConstant jc = new JoinConstant(result2, result, a, value);
                    if (solver.TRACE) solver.out.println("Generated: " + jc);
                    ir.add(jc);
                    result = result2;
                }
            }
            // Finally, union in the result.
            Union u = new Union(bottom.relation, bottom.relation, result);
            if (solver.TRACE) solver.out.println("Generated: " + u);
            ir.add(u);
        }
        if (bottom.relation.negated != null) {
            // Update negated.
            Invert i = new Invert(bottom.relation.negated, bottom.relation);
            if (solver.TRACE) solver.out.println("Generated: " + i);
            ir.add(i);
        }
        ir_incremental = ir;
        return ir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(bottom);
        sb.append(" :- ");
        for (Iterator i = top.iterator(); i.hasNext();) {
            sb.append(i.next());
            if (i.hasNext()) sb.append(", ");
        }
        sb.append(".");
        return sb.toString();
    }
    
    /**
     * The hashCode for rules is deterministic.  (We use the unique id number.)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return id;
    }

    /**
     * Returns the variable with the given name, or null if there is none.
     * 
     * @param name  variable name
     * @return  variable
     */
    public Variable getVariable(String name) {
        for (Iterator i = necessaryVariables.iterator(); i.hasNext(); ) {
            Variable v = (Variable) i.next();
            if (name.equals(v.getName())) return v;
        }
        for (Iterator i = unnecessaryVariables.iterator(); i.hasNext(); ) {
            Variable v = (Variable) i.next();
            if (name.equals(v.getName())) return v;
        }
        return null;
    }
    
    public int numberOfVariables() {
        return necessaryVariables.size() + unnecessaryVariables.size();
    }
    
    public Set getNecessaryVariables() {
        return necessaryVariables;
    }
    
    public Set getUnnecessaryVariables() {
        return unnecessaryVariables;
    }
    
    public Set getVariables() {
        HashSet s = new HashSet();
        s.addAll(necessaryVariables);
        s.addAll(unnecessaryVariables);
        return s;
    }
    
    /**
     * @return  map from names to variables
     */
    public Map getVarNameMap() {
        HashMap nameToVar = new HashMap();
        for (Iterator i = necessaryVariables.iterator(); i.hasNext(); ) {
            Variable v = (Variable) i.next();
            nameToVar.put(v.getName(), v);
        }
        for (Iterator i = unnecessaryVariables.iterator(); i.hasNext(); ) {
            Variable v = (Variable) i.next();
            nameToVar.put(v.getName(), v);
        }
        return nameToVar;
    }
    
    /**
     * Returns the attribute associated with the given variable.
     * 
     * @param v  variable
     * @return  attribute
     */
    public Attribute getAttribute(Variable v) {
        Attribute a = bottom.getAttribute(v);
        if (a != null) return a;
        for (Iterator i = top.iterator(); i.hasNext(); ) {
            RuleTerm rt = (RuleTerm) i.next();
            a = rt.getAttribute(v);
            if (a != null) return a;
        }
        return null;
    }
    
    public static InferenceRule fromXMLElement(Element e, Solver s) {
        Map nameToVar = new HashMap();
        RuleTerm b = RuleTerm.fromXMLElement((Element) e.getContent(0), s, nameToVar);
        List t = new ArrayList(e.getContentSize()-1);
        for (Iterator i = e.getContent().subList(1, e.getContentSize()).iterator(); i.hasNext(); ) {
            Element e2 = (Element) i.next();
            t.add(RuleTerm.fromXMLElement(e2, s, nameToVar));
        }
        InferenceRule ir = s.createInferenceRule(t, b);
        Assert._assert(Integer.parseInt(e.getAttributeValue("id")) == ir.id);
        return ir;
    }
    
    public Element toXMLElement() {
        Element e = new Element("rule");
        e.setAttribute("id", Integer.toString(id));
        e.addContent(bottom.toXMLElement());
        for (Iterator i = top.iterator(); i.hasNext(); ) {
            RuleTerm rt = (RuleTerm) i.next();
            e.addContent(rt.toXMLElement());
        }
        return e;
    }
    
}
