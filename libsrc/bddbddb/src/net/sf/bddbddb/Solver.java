// Solver.java, created Mar 16, 2004 7:07:16 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.URL;
import java.text.DecimalFormat;
import jwutil.classloader.HijackingClassLoader;
import jwutil.collections.GenericMultiMap;
import jwutil.collections.IndexMap;
import jwutil.collections.MultiMap;
import jwutil.collections.Pair;
import jwutil.io.SystemProperties;
import jwutil.reflect.Reflect;
import jwutil.util.Assert;
import net.sf.bddbddb.InferenceRule.DependenceNavigator;
import net.sf.bddbddb.ir.IR;

/**
 * Solver
 * 
 * @author jwhaley
 * @version $Id: Solver.java 549 2005-05-17 10:17:33Z joewhaley $
 */
public abstract class Solver {
    static {
        SystemProperties.read("solver.properties");
    }
    
    /** Print rules as they are triggered. */
    /** Split all rules. */
    boolean SPLIT_ALL_RULES = !SystemProperties.getProperty("split_all_rules", "no").equals("no");
    /** Split no rules, even if they have a "split" keyword. */
    boolean SPLIT_NO_RULES = !SystemProperties.getProperty("split_no_rules", "no").equals("no");
    int VERBOSE = Integer.getInteger("verbose", 1).intValue();
    /** Report the stats for each rule at the end. */
	boolean REPORT_STATS = VERBOSE >= 2;
    boolean NOISY = VERBOSE >= 3 || System.getProperty("noisy", "no").equals("yes");
    boolean TRACE = VERBOSE >= 4 || System.getProperty("tracesolve", "no").equals("yes");
    boolean TRACE_FULL = VERBOSE >= 5 || System.getProperty("fulltracesolve", "no").equals("yes");
    int MAX = Integer.getInteger("max.tuples", 1000).intValue();
    /** Trace the solver. */
    /** Do a full trace, even dumping the contents of relations. */
    /** Use the IR rather than the rules. */
    boolean USE_IR = !SystemProperties.getProperty("useir", "no").equals("no");
    /** Print the IR before interpreting it. */
    boolean PRINT_IR = SystemProperties.getProperty("printir") != null;
    
    boolean LEARN_ALL_RULES = !SystemProperties.getProperty("learnbestorder", "no").equals("no");
    boolean LEARN_BEST_ORDER = !SystemProperties.getProperty("learnbestorder", "no").equals("no");
    /** Trace output stream. */
    public PrintStream out = System.out;
    public PrintStream err = System.err;
    
    /** Input Datalog filename. */
    String inputFilename;
    /** Base directory where to load/save files. */
    String basedir = SystemProperties.getProperty("basedir");
    /** Include directories. */
    String includedirs = SystemProperties.getProperty("includedirs");
    /** List of paths to search when loading files. */
    List/*<String>*/ includePaths;
    /** Map between id numbers and relations. */
    IndexMap/*<Relation>*/ relations;
    /** Map between names and domains. */
    Map/*<String,Domain>*/ nameToDomain;
    /** Map between names and relations. */
    Map/*<String,Relation>*/ nameToRelation;
    /** Map between domains and equivalence relations. */
    MultiMap/*<Pair<Domain,Domain>,Relation>*/ equivalenceRelations;
    /** Map between domains and less than relations. */
    MultiMap/*<Pair<Domain,Domain>,Relation>*/ lessThanRelations;
    /** Map between domains and greater than relations. */
    MultiMap/*<Pair<Domain,Domain>,Relation>*/ greaterThanRelations;
    /** Map between domains and equivalence relations. */
    Map/*<Pair<Domain,Domain>,Relation>*/ mapRelations;
    /** List of inference rules. */
    List/*<InferenceRule>*/ rules;
    /** Iteration order. */
    IterationFlowGraph ifg;

    /** Flag that is set on initialization. */
    boolean isInitialized;
    
    Collection/*<Relation>*/ relationsToLoad;
    Collection/*<Relation>*/ relationsToLoadTuples;
    Collection/*<Relation>*/ relationsToDump;
    Collection/*<Relation>*/ relationsToDumpTuples;
    Collection/*<Relation>*/ relationsToPrintTuples;
    Collection/*<Relation>*/ relationsToPrintSize;
    Collection/*<Dot>*/ dotGraphsToDump;
    Collection/*<Relation>*/ relationsToPreLoad;

    /**
     * Create a new inference rule.
     * 
     * @param top  list of subgoals of rule
     * @param bottom  head of rule
     * @return  new inference rule
     */
    abstract InferenceRule createInferenceRule(List/*<RuleTerm>*/ top, RuleTerm bottom);

    /**
     * Create a new equivalence relation.
     * 
     * @param fd1  first domain of relation
     * @param fd2  second domain of relation
     * @return  new equivalence relation
     */
    abstract Relation createEquivalenceRelation(Domain fd1, Domain fd2);

    /**
     * Create a new less-than relation.
     * 
     * @param fd1  first domain of relation
     * @param fd2  second domain of relation
     * @return  new equivalence relation
     */
    abstract Relation createLessThanRelation(Domain fd1, Domain fd2);

    /**
     * Create a new greater-than relation.
     * 
     * @param fd1  first domain of relation
     * @param fd2  second domain of relation
     * @return  new equivalence relation
     */
    abstract Relation createGreaterThanRelation(Domain fd1, Domain fd2);

    /**
     * Create a new map relation.
     * 
     * @param fd1  first domain of relation
     * @param fd2  second domain of relation
     * @return  new map relation
     */
    abstract Relation createMapRelation(Domain fd1, Domain fd2);
    
    /**
     * Create a new relation.
     * 
     * @param name  name of relation
     * @param attributes  attributes of relation
     * @return  new relation
     */
    public abstract Relation createRelation(String name, List/*<Attribute>*/ attributes);

    /**
     * Register the given relation with this solver.
     * 
     * @param r  relation to register
     */
    void registerRelation(Relation r) {
        int i = relations.get(r);
        Assert._assert(i == relations.size() - 1);
        Assert._assert(i == r.id);
        Object old = nameToRelation.put(r.name, r);
        Assert._assert(old == null);
    }

    /**
     * Create a numbering rule from the given rule template.
     * 
     * @param ir  incoming rule
     * @return  new numbering rule
     */
    NumberingRule createNumberingRule(InferenceRule ir) {
        return new NumberingRule(this, ir);
    }

    /**
     * Construct a solver object.
     */
    protected Solver() {
        clear();
    }

    /**
     * Clear this solver of all relations, domains, and rules.
     */
    public void clear() {
        relations = new IndexMap("relations");
        nameToDomain = new HashMap();
        nameToRelation = new HashMap();
        equivalenceRelations = new GenericMultiMap();
        greaterThanRelations = new GenericMultiMap();
        lessThanRelations = new GenericMultiMap();
        mapRelations = new HashMap();
        rules = new LinkedList();
        relationsToLoad = new LinkedList();
        relationsToLoadTuples = new LinkedList();
        relationsToDump = new LinkedList();
        relationsToDumpTuples = new LinkedList();
        relationsToPrintTuples = new LinkedList();
        relationsToPrintSize = new LinkedList();
        dotGraphsToDump = new LinkedList();
        relationsToPreLoad = new LinkedList();
    }

    /**
     * Initialize all of the relations and rules.
     */
    public void initialize() {
        for (Iterator i = nameToRelation.values().iterator(); i.hasNext();) {
            Relation r = (Relation) i.next();
            r.initialize();
        }
        for (Iterator i = rules.iterator(); i.hasNext();) {
            InferenceRule r = (InferenceRule) i.next();
            r.initialize();
        }
    }

    Stratify stratify;
    IR ir;
    
    /**
     * Stratify the rules.
     */
    public void stratify() {
        stratify = new Stratify(this);
        stratify.stratify();
        if (USE_IR) {
            ir = IR.create(stratify);
            ifg = ir.graph;
            ir.optimize();
            if (PRINT_IR) ir.printIR();
        } else {
            ifg = new IterationFlowGraph(rules, stratify);
			if (VERBOSE >= 2) ifg.getIterationList().print();
            //IterationList list = ifg.expand();
        }
    }
    
    /**
     * Solve the rules.
     */
    public abstract void solve();

    /**
     * Called after solving.
     */
    public abstract void finish();
    
    /**
     * Clean up the solver, freeing the memory associated with it.
     */
    public abstract void cleanup();

    /**
     * Get the named domain.
     * 
     * @param name  domain name
     * @return  domain that has the name
     */
    public Domain getDomain(String name) {
        return (Domain) nameToDomain.get(name);
    }

    /**
     * Get the named relation.
     * 
     * @param name  relation name
     * @return  relation that has the name
     */
    public Relation getRelation(String name) {
        return (Relation) nameToRelation.get(name);
    }

    /**
     * Get the relation with the given index.
     * 
     * @param index  index desired
     * @return  relation
     */
    public Relation getRelation(int index) {
        return (Relation) relations.get(index);
    }
    
    public IndexMap getRelations(){ return relations; }

    static void addAllValues(Collection c, MultiMap m) {
        for (Iterator i = m.keySet().iterator(); i.hasNext(); ) {
            c.addAll(m.getValues(i.next()));
        }
    }
    
    /**
     * Get all the equivalence relations.
     * 
     * @return  collection of equivalence relations
     */
    public Collection getComparisonRelations() {
        Collection set = new LinkedList();
        addAllValues(set, equivalenceRelations);
        addAllValues(set, lessThanRelations);
        addAllValues(set, greaterThanRelations);
        set.addAll(mapRelations.values());
        return set;
    }
    
    /**
     * Get the base directory used for output.
     * 
     * @return  base directory used for output
     */
    public String getBaseDir() {
        return basedir;
    }
    
    /**
     * Initialize the basedir variable, given the specified input Datalog filename.
     * 
     * @param inputFilename  input Datalog filename
     */
    void initializeBasedir(String inputFilename) {
        String sep = SystemProperties.getProperty("file.separator");
        if (basedir == null) {
            int i = inputFilename.lastIndexOf(sep);
            if (i >= 0) {
                basedir = inputFilename.substring(0, i + 1);
            } else {
                i = inputFilename.lastIndexOf("/");
                if (!sep.equals("/") && i >= 0) {
                    basedir = inputFilename.substring(0, i + 1);
                } else {
                    basedir = "";
                }
            }
        }
        if (basedir.length() > 0 && !basedir.endsWith(sep) && !basedir.endsWith("/")) {
            basedir += sep;
        }
        if (includedirs == null) includedirs = basedir;
    }
    
    public void load(String filename) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        inputFilename = filename;
        if (NOISY) out.println("Opening Datalog program \"" + inputFilename + "\"");
        MyReader in = new MyReader(new LineNumberReader(new FileReader(inputFilename)));
        initializeBasedir(inputFilename);
        load(in);
    }
    
    public void load(MyReader in) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        DatalogParser parser = new DatalogParser(this);
        parser.readDatalogProgram(in);
        if (NOISY) out.println(nameToDomain.size() + " field domains.");
        if (NOISY) out.println(nameToRelation.size() + " relations.");
        if (NOISY) out.println(rules.size() + " rules.");
        in.close();
        if (NOISY) out.print("Splitting rules: ");
        splitRules();
        if (NOISY) out.println("done.");
        //out.print("Magic Set Transformation: ");
        //MagicSetTransformation mst = new MagicSetTransformation(this);
        //mst.transform(rules);
        //out.println("done.");
        if (NOISY) out.print("Initializing solver: ");
        initialize();
        if (NOISY) out.println("done.");
        if (NOISY) out.print("Loading initial relations: ");
        long time = System.currentTimeMillis();
        loadInitialRelations();
        time = System.currentTimeMillis() - time;
        if (NOISY) out.println("done. (" + time + " ms)");
        if (VERBOSE >= 2) out.println("Stratifying: ");
        time = System.currentTimeMillis();
        stratify();
        time = System.currentTimeMillis() - time;
        if (VERBOSE >= 2) out.println("done. (" + time + " ms)");
        if (VERBOSE >= 2) out.println("Solving: ");
    }
    public long startTime;
    public void run() {
        startTime = System.currentTimeMillis();
        solve();
        long solveTime = System.currentTimeMillis() - startTime;
        if (VERBOSE >= 2) out.println("done. (" + solveTime + " ms)");
        
        finish();
        if (REPORT_STATS) {
        	out.println("SOLVE_TIME=" + solveTime);
            reportStats();
        }
    }
    
    public void save() throws IOException {
        if (NOISY) out.print("Saving results: ");
        long time = System.currentTimeMillis();
        saveResults();
        doCallbacks(onSave);
        time = System.currentTimeMillis() - time;
        if (NOISY) out.println("done. (" + time + " ms)");
        cleanup();
    }
    
    Collection onSave = new LinkedList();
    
    public void addSaveHook(Runnable r) {
        onSave.add(r);
    }
    
    public void doCallbacks(Collection c) {
        for (Iterator i = c.iterator(); i.hasNext(); ) {
            Runnable r = (Runnable) i.next();
            r.run();
        }
    }
    
    /**
     * The entry point to the application.
     * 
     * @param args  command line arguments
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    public static void main2(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        String inputFilename = SystemProperties.getProperty("datalog");
        if (args.length > 0) inputFilename = args[0];
        if (inputFilename == null) {
            printUsage();
            return;
        }
        String solverName = SystemProperties.getProperty("solver", "net.sf.bddbddb.BDDSolver");
        Solver dis;
        dis = (Solver) Class.forName(solverName).newInstance();
        long startTime = System.currentTimeMillis();
        
        dis.load(inputFilename);
        
        long loadTime = System.currentTimeMillis() - startTime;
        startTime = System.currentTimeMillis();
        
        dis.run();
        
        long runTime = System.currentTimeMillis() - startTime;
        startTime = System.currentTimeMillis();
        
        dis.save();
        
        long saveTime = System.currentTimeMillis() - startTime;
        
        System.out.println("DLOG LOAD TIME: " + loadTime/1000 + " seconds");
        System.out.println("DLOG RUN TIME: " + runTime/1000 + " seconds");
        System.out.println("DLOG SAVE TIME: " + saveTime/1000 + " seconds");
    }

    /**
     * Print usage information.
     */
    public static void printUsage() {
        System.out.println("Usage: java {properties} " + Solver.class.getName() + " <datalog file>");
        System.out.println("System properties:");
        System.out.println("  -Dnoisy           Print rules as they are applied.");
        System.out.println("  -Dtracesolve      Turn on trace information.");
        System.out.println("  -Dfulltracesolve  Also print contents of relations.");
        System.out.println("  -Dsolver          Solver class name.");
        System.out.println("  -Ddatalog         Datalog file name, if not specified on command line.");
        System.out.println("  -Dbddinfo         BDD info file name.");
        System.out.println("  -Dbddvarorder     BDD variable order.");
        System.out.println("  -Dbddnodes        BDD initial node table size.");
        System.out.println("  -Dbddcache        BDD operation cache size.");
        System.out.println("  -Dbddminfree      BDD minimum free parameter.");
        System.out.println("  -Dincremental     Incrementalize all rules by default.");
        System.out.println("  -Dfindbestorder   Find best BDD domain order.");
        System.out.println("  -Ddumpnumberinggraph  Dump the context numbering in dot graph format.");
        System.out.println("  -Ddumprulegraph   Dump the graph of rules in dot format.");
        System.out.println("  -Duseir           Compile rules using intermediate representation.");
        System.out.println("  -Dprintir         Print intermediate representation before interpreting.");
    }
    
    /**
     * A LineNumberReader that can nest through multiple included files.
     * 
     * @author John Whaley
     * @version $Id: Solver.java 549 2005-05-17 10:17:33Z joewhaley $
     */
    public static class MyReader {
        /**
         * Stack of readers, to handle including files.
         */
        List readerStack = new LinkedList();
        /**
         * Current line number reader.
         */
        LineNumberReader current;

        /**
         * Construct a new reader from the given line number reader.
         * 
         * @param r  reader
         */
        public MyReader(LineNumberReader r) {
            current = r;
        }

        /**
         * Register a new reader, pushing the current one on the stack.
         * 
         * @param r  reader to register
         */
        public void registerReader(LineNumberReader r) {
            if (current != null) readerStack.add(current);
            current = r;
        }

        /**
         * Read a line.  If the current reader is empty, we pop one off the stack.
         * If it is empty and the stack is empty, returns null.
         * 
         * @return  the line that was read
         * @throws IOException
         */
        public String readLine() throws IOException {
            String s;
            for (;;) {
                s = current.readLine();
                if (s != null) return s;
                if (readerStack.isEmpty()) return null;
                current = (LineNumberReader) readerStack.remove(readerStack.size() - 1);
            }
        }

        /**
         * Return the line number in the current reader.
         * 
         * @return  line number
         */
        public int getLineNumber() {
            return current.getLineNumber();
        }

        /**
         * Close all of the readers we have open.
         * 
         * @throws IOException
         */
        public void close() throws IOException {
            for (;;) {
                current.close();
                if (readerStack.isEmpty()) return;
                current = (LineNumberReader) readerStack.remove(readerStack.size() - 1);
            }
        }
    }

    /**
     * Delete the given relation.
     * 
     * @param r  relation to delete
     */
    void deleteRelation(Relation r) {
        relationsToDump.remove(r);
        relationsToDumpTuples.remove(r);
        relationsToLoad.remove(r);
        relationsToLoadTuples.remove(r);
        relationsToPrintSize.remove(r);
        relationsToPrintTuples.remove(r);
        nameToRelation.remove(r.name);
        relationsToPreLoad.remove(r);
    }

    boolean includeRelationInComeFromQuery(Relation r, boolean includeDerivations) {
        String comeFromIncludes = SystemProperties.getProperty("comeFromRelations");
        if (comeFromIncludes == null) return true;
        String[] names = comeFromIncludes.split(":");
        boolean include = false;
        for (int i=0; !include && i<names.length; i++) {
            if (includeDerivations) {
                if (r.name.startsWith(names[i])) include = true;
            }
            else {
                if (r.name.equals(names[i])) include = true;
            }
        }
        return include;
    }

    /**
     * Compute a come-from query.  A come-from query includes both :- and ?.
     * 
     * @param rt initial rule term
     * @return  list of inference rules implementing the come-from query
     */
    List/*<InferenceRule>*/ comeFromQuery(RuleTerm rt, List extras, boolean single) {
        List newRules = new LinkedList();
        
        boolean oldTRACE = TRACE;
        TRACE = TRACE || (SystemProperties.getProperty("traceComeFromQuery") != null);

        Relation r = rt.relation;
        Relation r2 = createRelation(r.name+"_q", r.attributes);
        
        RuleTerm my_rt = new RuleTerm(r2, rt.variables);
        InferenceRule my_ir = createInferenceRule(Collections.singletonList(rt), my_rt);
        //my_ir.single = single;
        if (TRACE) out.println("Adding rule: "+my_ir);
        newRules.add(my_ir);
        
        DependenceNavigator nav = new DependenceNavigator(rules);
        Map/*<Relation,Relation>*/ toQueryRelation = new LinkedHashMap();
        LinkedList worklist = new LinkedList();
        toQueryRelation.put(r, r2);
        worklist.add(r);
        while (!worklist.isEmpty()) {
            // r: the relation we want to query.
            r = (Relation) worklist.removeFirst();
            // r2: the tuples in the relation that contribute to the answer.
            r2 = (Relation) toQueryRelation.get(r);
            if (TRACE) out.println("Finding contributions in relation "+r+": "+r2);
            
            // Visit each rule that can add tuples to "r".
            Collection rules = nav.prev(r);
            for (Iterator i = rules.iterator(); i.hasNext(); ) {
                InferenceRule ir = (InferenceRule) i.next();
                if (TRACE) out.println("This rule can contribute: "+ir);
                Assert._assert(ir.bottom.relation == r);
                
                // Build up a new query that consists of "r2" and all of the subgoals.
                List/*<RuleTerm>*/ terms = new LinkedList();
                Map varMap = new LinkedHashMap();
                RuleTerm rt2 = new RuleTerm(r2, ir.bottom.variables);
                terms.add(rt2);
                addToVarMap(varMap, rt2);
                for (Iterator j = ir.top.iterator(); j.hasNext(); ) {
                    RuleTerm rt3 = (RuleTerm) j.next();
                    terms.add(rt3);
                    addToVarMap(varMap, rt3);
                    Relation r3 = rt3.relation;
                    Relation r4 = (Relation) toQueryRelation.get(r3);
                    if (r4 == null) {
                        boolean relevantRelation = includeRelationInComeFromQuery(r3,true);
                        //relevantRelation = true;
                        if (!relevantRelation) {
                            if (TRACE) out.println("Skipping contribution relation "+r3);
                        }
                        else {
                        // New relation, visit it.
                        worklist.add(r3);
                        r4 = createRelation(r3.name+"_q", r3.attributes);
                        toQueryRelation.put(r3, r4);
                        if (TRACE) out.println("Adding contribution relation "+r3+": "+r4);
                        }
                    }
                }
                List vars = new ArrayList(varMap.keySet());
                List attributes = new ArrayList(vars.size());
                for (Iterator k = varMap.entrySet().iterator(); k.hasNext(); ) {
                    Map.Entry e = (Map.Entry) k.next();
                    Variable v = (Variable) e.getKey();
                    String name = (String) e.getValue();
                    attributes.add(new Attribute(name, v.getDomain(), ""));
                }
                Relation bottomr = createRelation(r.name+"_q"+ir.id, attributes);
                RuleTerm bottom = new RuleTerm(bottomr, vars);
                InferenceRule newrule = createInferenceRule(terms, bottom);
                newrule.single = single;
                if (TRACE) out.println("Adding rule: "+newrule);
                newRules.add(newrule);
                
                // Now bottomr contains assignments to all of the variables.

                // Make a printable relation of bottomr, without the contexts:
                if(includeRelationInComeFromQuery(r,false)) {
                    List vars_noC = new ArrayList(varMap.keySet());
                    List attributes_noC = new ArrayList(vars.size());
                    for (Iterator k = varMap.entrySet().iterator(); k.hasNext(); ) {
                        Map.Entry e = (Map.Entry) k.next();
                        Variable v = (Variable) e.getKey();
                        String name = (String) e.getValue();
                        if (!name.startsWith("c")) {
                            attributes_noC.add(new Attribute(name, v.getDomain(), ""));
                        }
                        else {
                            vars_noC.remove(v);
                            if (TRACE) out.println("Excluding from non-context version: "+name);
                        }
                    }
                    Relation bottomr_noC = createRelation(r.name+"_q"+ir.id+"_noC", attributes_noC);
                    RuleTerm bottom_noC = new RuleTerm(bottomr_noC, vars_noC);
                    InferenceRule newrule_noC = createInferenceRule(Collections.singletonList(bottom), bottom_noC);
                    newrule_noC.single = single;
                    if (TRACE) out.println("Adding rule: "+newrule_noC);
                    newRules.add(newrule_noC);
                    relationsToPrintTuples.add(bottomr_noC);
                    relationsToDump.add(bottomr_noC);
                }
                
                // For each subgoal, build a new rule that adds to the contribute relations
                // for that subgoal.
                List terms2 = Collections.singletonList(bottom);
                for (Iterator j = ir.top.iterator(); j.hasNext(); ) {
                    RuleTerm rt3 = (RuleTerm) j.next();
                    Relation r3 = rt3.relation;
                    Relation r4 = (Relation) toQueryRelation.get(r3);
                    if (r4 != null) {
                    Assert._assert(r4 != null, "no mapping for "+r3);
                    RuleTerm rt4 = new RuleTerm(r4, rt3.variables);
                    InferenceRule newrule2 = createInferenceRule(terms2, rt4);
                    //newrule2.single = single;
                    if (TRACE) out.println("Adding rule: "+newrule2);
                    newRules.add(newrule2);
                    }
                }
            }
        }
        
        for (Iterator i = toQueryRelation.values().iterator(); i.hasNext(); ) {
            Relation r4 = (Relation) i.next();
            relationsToPrintTuples.add(r4);
        }

        TRACE = oldTRACE;
        
        return newRules;
    }
    
    /**
     * Utility function to add the variables in the given rule term to the given
     * variable map from Variables to Strings.  This is used in constructing
     * come-from queries.
     * 
     * @param varMap  variable map
     * @param rt  rule term whose variables we want to add
     */
    private void addToVarMap(Map/*<Variable,String>*/ varMap, RuleTerm rt) {
        for (Iterator i = rt.variables.iterator(); i.hasNext(); ) {
            Variable v = (Variable) i.next();
            if (v.name.equals("_")) continue;
            String name;
            if (v instanceof Constant) {
                name = rt.relation.getAttribute(rt.variables.indexOf(v)).attributeName;
            } else {
                name = v.toString();
            }
            varMap.put(v, name);
        }
    }
    
    /**
     * Get the equivalence relation for the given domains.
     * 
     * @param fd1  first domain
     * @param fd2  second domain
     * @return  equivalence relation on those domains
     */
    Relation getEquivalenceRelation(Domain fd1, Domain fd2) {
        if (fd1.name.compareTo(fd2.name) > 0) return getEquivalenceRelation(fd2, fd1);
        Object key = new Pair(fd1, fd2);
        Collection c = equivalenceRelations.getValues(key);
        Relation r;
        if (c.isEmpty()) {
            equivalenceRelations.put(key, r = createEquivalenceRelation(fd1, fd2));
        } else {
            r = (Relation) c.iterator().next();
        }
        return r;
    }

    /**
     * Get the negated equivalence relation for the given domains.
     * 
     * @param fd1  first domain
     * @param fd2  second domain
     * @return  negated equivalence relation on those domains
     */
    Relation getNotEquivalenceRelation(Domain fd1, Domain fd2) {
        Relation r = getEquivalenceRelation(fd1, fd2);
        return r.makeNegated(this);
    }
    
    /**
     * Get the greater-than-or-equal-to relation for the given domains.
     * 
     * @param fd1  first domain
     * @param fd2  second domain
     * @return  greater-than-or-equal-to relation on those domains
     */
    Relation getGreaterThanOrEqualRelation(Domain fd1, Domain fd2) {
        Relation r = getLessThanRelation(fd1, fd2);
        return r.makeNegated(this);
    }

    /**
     * Get the greater-than relation for the given domains.
     * 
     * @param fd1  first domain
     * @param fd2  second domain
     * @return  greater-than relation on those domains
     */
    Relation getGreaterThanRelation(Domain fd1, Domain fd2) {
        Object key = new Pair(fd1, fd2);
        Collection c = greaterThanRelations.getValues(key);
        Relation r;
        if (c.isEmpty()) {
            greaterThanRelations.put(key, r = createGreaterThanRelation(fd1, fd2));
        } else {
            r = (Relation) c.iterator().next();
        }
        return r;
    }

    /**
     * Get the greater-than relation for the given domains.
     * 
     * @param fd1  first domain
     * @param fd2  second domain
     * @return  less-than relation on those domains
     */
    Relation getLessThanRelation(Domain fd1, Domain fd2) {
        Object key = new Pair(fd1, fd2);
        Collection c = lessThanRelations.getValues(key);
        Relation r;
        if (c.isEmpty()) {
            lessThanRelations.put(key, r = createLessThanRelation(fd1, fd2));
        } else {
            r = (Relation) c.iterator().next();
        }
        return r;
    }

    /**
     * Get the less-than-or-equal-to relation for the given domains.
     * 
     * @param fd1  first domain
     * @param fd2  second domain
     * @return  less-than-or-equal-to relation on those domains
     */
    Relation getLessThanOrEqualRelation(Domain fd1, Domain fd2) {
        Relation r = getGreaterThanRelation(fd1, fd2);
        return r.makeNegated(this);
    }

    /**
     * Get the map relation for the given domains.
     * 
     * @param fd1  first domain
     * @param fd2  second domain
     * @return  map relation on those domains
     */
    Relation getMapRelation(Domain fd1, Domain fd2) {
        Object key = new Pair(fd1, fd2);
        Relation r = (Relation) mapRelations.get(key);
        if (r == null) {
            mapRelations.put(key, r = createMapRelation(fd1, fd2));
        }
        return r;
    }
    
    /**
     * Load in the initial relations.
     * 
     * @throws IOException
     */
    void loadInitialRelations() throws IOException {
        if (USE_IR) return;
        for (Iterator i = relationsToLoad.iterator(); i.hasNext();) {
            Relation r = (Relation) i.next();
            try {
                r.load();
            } catch (IOException x) {
                out.println("WARNING: Cannot load bdd " + r + ": " + x.toString());
            }
        }
        for (Iterator i = relationsToLoadTuples.iterator(); i.hasNext();) {
            Relation r = (Relation) i.next();
            try {
                r.loadTuples();
            } catch (IOException x) {
                out.println("WARNING: Cannot load tuples " + r + ": " + x.toString());
            }
        }
        for (Iterator i = relationsToPreLoad.iterator(); i.hasNext();) {
            Relation r = (Relation) i.next();
            try {
                r.load();
            } catch (IOException x) {
                out.println("WARNING: Cannot load bdd " + r + ": " + x.toString());
            }
        }
    }

    /**
     * Split inference rules.
     */
    void splitRules() {
        List newRules = new LinkedList();
        for (Iterator i = rules.iterator(); i.hasNext();) {
            InferenceRule r = (InferenceRule) i.next();
            if (!SPLIT_NO_RULES && (SPLIT_ALL_RULES || r.split)) newRules.addAll(r.split(rules.indexOf(r)));
        }
        rules.addAll(newRules);
    }

class RuleSorter implements Comparator {
        long getRuleTime(Object rule) {
            if (rule instanceof BDDInferenceRule) {
                return ((BDDInferenceRule)rule).totalTime;
            }
            else if (rule instanceof NumberingRule) {
                return ((NumberingRule)rule).totalTime;
            }
            else {
                return 0;
            }
        }

        public int compare(Object o1, Object o2) {
            return (int)(getRuleTime(o1) - getRuleTime(o2));
        }
    }

    /**
     * Report rule statistics.
     */
    void reportStats() {
        if(USE_IR) return;
        List sortedRules = new LinkedList(rules);
        Collections.sort(sortedRules,new RuleSorter());
        //        List fbsList = new LinkedList();
        for (Iterator i = sortedRules.iterator(); i.hasNext();) {
            InferenceRule r = (InferenceRule) i.next();
            r.reportStats();
            /*
            if (r instanceof BDDInferenceRule) {
                BDDInferenceRule bddir = (BDDInferenceRule) r;
                if (bddir.fbs != null) fbsList.add(bddir.fbs);
            }
        }
        for (Iterator i = fbsList.iterator(); i.hasNext();) {
            ((FindBestSplit)i.next()).reportStats();
            */
        }
    }

    /**
     * Save the results and print sizes.
     * 
     * @throws IOException
     */
    void saveResults() throws IOException {
        if (USE_IR) return;
        for (Iterator i = relationsToPrintSize.iterator(); i.hasNext();) {
            Relation r = (Relation) i.next();
            double size = r.dsize();
            DecimalFormat myFormatter = new DecimalFormat("0.");
            String output = myFormatter.format(size);
            out.println("SIZE OF " + r + ": " + output);
        }
        for (Iterator i = relationsToPrintTuples.iterator(); i.hasNext();) {
            Relation r = (Relation) i.next();
            double size = r.dsize();
            DecimalFormat myFormatter = new DecimalFormat("0.");
            String output = myFormatter.format(size);
            out.println("Tuples in "+r+": ("+output+")");
            int num = MAX;
            TupleIterator j = r.iterator();
            while (j.hasNext()) {
                if (--num < 0) break;
                BigInteger[] a = j.nextTuple();
                out.print("\t(");
                for (int k = 0; k < a.length; ++k) {
                    if (k > 0) out.print(',');
                    Attribute at = r.getAttribute(k);
                    out.print(at);
                    out.print('=');
                    out.print(at.attributeDomain.toString(a[k]));
                    if (at.attributeDomain.map != null &&
                        a[k].signum() >= 0 && a[k].intValue() < at.attributeDomain.map.size()) {
                        out.print('(');
                        out.print(a[k]);
                        out.print(')');
                    }
                }
                out.println(")");
            }
            if (j.hasNext()) {
                out.println("\tand more ("+r.size()+" in total).");
            }
        }
        for (Iterator i = relationsToDump.iterator(); i.hasNext();) {
            Relation r = (Relation) i.next();
            if (NOISY) out.println("Dumping BDD for " + r);
            r.save();
        }
        for (Iterator i = relationsToDumpTuples.iterator(); i.hasNext();) {
            Relation r = (Relation) i.next();
            if (NOISY) out.println("Dumping tuples for " + r);
            r.saveTuples();
        }
        for (Iterator i = dotGraphsToDump.iterator(); i.hasNext();) {
            Dot dot = (Dot) i.next();
            if (NOISY) out.println("Dumping dot graph");
            dot.outputGraph();
        }
    }

    /**
     * Return the number of relations.
     * 
     * @return the number of relations
     */
    public int getNumberOfRelations() {
        return relations.size();
    }

    /**
     * Return the list of rules.
     * 
     * @return the list of rules
     */
    public List getRules() {
        return rules;
    }

    /**
     * Returns the ith rule.
     * 
     * @param i  index
     * @return  inference rule
     */
    public InferenceRule getRule(int i) {
        InferenceRule ir = (InferenceRule) rules.get(i);
        if (ir.id == i) return ir;
        out.println("Id "+i+" doesn't match id "+ir.id+": "+ir);
        for (Iterator j = rules.iterator(); j.hasNext(); ) {
            ir = (InferenceRule) j.next();
            if (ir.id == i) return ir;
        }
        return null;
    }
    
    /**
     * Returns the inference rule with the given name.
     * 
     * @param s  rule name
     * @return  inference rul
     */
    public InferenceRule getRule(String s) {
        if (!s.startsWith("rule")) return null;
        int index = Integer.parseInt(s.substring(4));
        return getRule(index);
    }
    
    public InferenceRule getRuleThatContains(Variable v) {
        for (Iterator i = rules.iterator(); i.hasNext(); ) {
            InferenceRule ir = (InferenceRule) i.next();
            if (ir.necessaryVariables == null) continue; // NumberingRule (?)
            if (ir.necessaryVariables.contains(v) ||
                ir.unnecessaryVariables.contains(v)) return ir;
        }
        return null;
    }
    
    /**
     * Return the iteration flow graph.  This contains the iteration order.
     * 
     * @return  iteration flow graph
     */
    public IterationFlowGraph getIterationFlowGraph() {
        return ifg;
    }
    
    /**
     * Return the collection of relations to load.
     * 
     * @return  the collection of relations to load.
     */
    public Collection getRelationsToLoad() {
        return relationsToLoad;
    }

    /**
     * Return the collection of relations to save.
     * 
     * @return  the collection of relations to save.
     */
    public Collection getRelationsToSave() {
        return relationsToDump;
    }
    
    /**
     * Return the collection of relations to preload.
     * 
     * @return  the collection of relations to preload.
     */
    public Collection getRelationsToPreLoad() {
        return relationsToPreLoad;
    }
    
    /**
     * Replacement main() function that checks if we have the BDD library in the
     * classpath.
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        // Make sure we have the BDD library in our classpath.
        try {
            Class.forName("net.sf.javabdd.BDD");
        } catch (ClassNotFoundException x) {
            ClassLoader cl = addBDDLibraryToClasspath(args);
            // Reflective invocation under the new class loader.
            Reflect.invoke(cl, Solver.class.getName(), "main2", new Class[] {String[].class}, new Object[] {args});
            return;
        }
        // Just call it directly.
        main2(args);
    }
    
    public static ClassLoader addBDDLibraryToClasspath(String[] args) throws IOException {
        System.out.print("BDD library is not in classpath!  ");
        URL url;
        url = HijackingClassLoader.getFileURL("javabdd.jar");
        if (url == null) {
            String sep = SystemProperties.getProperty("file.separator");
            url = HijackingClassLoader.getFileURL(".."+sep+"JavaBDD");
        }
        if (url == null) {
            System.err.println("Cannot find JavaBDD library!");
            System.exit(-1);
            return null;
        }
        System.out.println("Adding "+url+" to classpath.");
        URL url2 = new File(".").toURL();
        return new HijackingClassLoader(new URL[] {url, url2});
    }
    
}
