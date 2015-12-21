// Interactive.java, created Jul 29, 2004 3:40:10 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import jwutil.io.SystemProperties;
import net.sf.bddbddb.Solver.MyReader;

/**
 * Command-line interactive bddbddb solver.
 * 
 * The command line accepts anything that is valid in a Datalog file, plus
 * a special query syntax that ends with '?'.  Queries cause the solver to
 * automatically solve the Datalog with respect to the given query.  There
 * are also some extra commands; type "help" to get a list of them.
 * 
 * @author jwhaley
 * @version $Id: Interactive.java 549 2005-05-17 10:17:33Z joewhaley $
 */
public class Interactive {
    
    public static InputStream in = System.in;
    public static PrintStream out = System.out;
    public static PrintStream err = System.err;
    
    public static boolean IGNORE_OUTPUT = !SystemProperties.getProperty("ignoreoutput", "yes").equals("no");
    
    /**
     * Solver we are using.
     */
    protected Solver solver;
    
    /**
     * Datalog parser.
     */
    protected DatalogParser parser;
    
    /**
     * Construct a new interactive solver.
     * 
     * @param s  solver to use
     */
    public Interactive(Solver s) {
        this.solver = s;
        this.solver.out = out;
        this.solver.err = err;
    }
    
    /**
     * The entry point of the application.
     * 
     * @param args  command line args
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
        String solverName = SystemProperties.getProperty("solver", "net.sf.bddbddb.BDDSolver");
        Solver dis = (Solver) Class.forName(solverName).newInstance();
        String file = "";
        if (args.length > 0) {
            file = args[0];
        }
        dis.initializeBasedir(file);
        Interactive a = new Interactive(dis);
        if (file.length() > 0) {
            MyReader in = new MyReader(new LineNumberReader(new FileReader(file)));
            out.println("Reading "+file+"...");
            a.parser = new DatalogParser(dis);
            a.parser.readDatalogProgram(in);
            in.close();
        }
        if (IGNORE_OUTPUT) {
            dis.relationsToDump.clear();
            dis.relationsToDumpTuples.clear();
            dis.relationsToPrintSize.clear();
            dis.relationsToPrintTuples.clear();
        }
        out.println("Welcome to the interactive bddbddb Datalog solver!");
        out.println("Type a Datalog rule or query, or \"help\" for help.");
        a.interactive();
    }
    
    /**
     * Utility function to read a line from the given reader.
     * Allows line wrapping using backslashes, among other things.
     * 
     * @param in  input reader
     * @return  line
     * @throws IOException
     */
    static String readLine(MyReader in) throws IOException {
        String s = in.readLine();
        if (s == null) return null;
        s = s.trim();
        while (s.endsWith("\\")) {
            String s2 = in.readLine();
            if (s2 == null) break;
            s2 = s2.trim();
            s = s.substring(0, s.length() - 1) + s2;
        }
        return s;
    }
    
    /**
     * Flag to record whether or not we need to call the solver again.
     */
    boolean changed = false;
    
    /**
     * Log of what has been typed so far.
     */
    List log;
    
    /**
     * Dump the log to a file.
     * 
     * @throws IOException
     */
    void dumpLog() throws IOException {
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new FileWriter("bddbddb.log"));
            for (Iterator i = log.iterator(); i.hasNext(); ) {
                w.write(i.next()+"\n");
            }
        } finally {
            if (w != null) try { w.close(); } catch (IOException _) { }
        }
    }
    
    public static void printHelp() {
        out.println("Using Datalog:");
        out.println();
        /////////////12345678901234567890123456789012345678901234567890123456789012345678901234567890
        out.println(" To specify a domain:  \t<domain> <size> {<map-file-name>}");
        out.println("    Example:\tV 1024");
        out.println(" To specify a relation:\t<relation> (<attrib>:<domain>{,<attrib>:<domain>}*)");
        out.println("    Example:\tR (v1:V, v2:V, h:H)");
        out.println(" To specify a rule:    \t<relation>(<var>{,<var>}*) :- {<relation>(<var>{,<var>}*),}*.");
        out.println("    Example:\tR(a,b,c) :- R(b,a,c), Q(c).");
        out.println("    Example:\tX(a,b,c) :- a!=b, b<c, Y(c).");
        out.println(" To perform a query:   \t<relation>(<var>{,<var>}*) {,<relation>(<var>{,<var>}*)}*?");
        out.println("    Example:\tR(x,y,z), Q(z)?");
        out.println();
        out.println("Other commands:");
        out.println();
        out.println("  relations\t: list relations");
        out.println("  rules    \t: list rules");
        out.println("  help     \t: show this message");
        out.println("  deleterule #{,#}*\t: delete rule(s)");
        out.println("  save <relation>{,<relation>}\t: save relations");
        out.println("  solve    \t: solve current set of rules/relations");
        out.println("  dumplog  \t: dump the command log to a file");
        out.println("  .include <file>\t: include (interpret) a file");
    }
    
    /**
     * Invoke the interactive solver.
     */
    public void interactive() {
        log = new LinkedList();
        LineNumberReader lin = new LineNumberReader(new InputStreamReader(in));
        MyReader in = new MyReader(lin);
        outer:
        for (;;) {
            try {
                out.print("> ");
                String s = readLine(in);
                if (s == null) break;
                if (s.equalsIgnoreCase("exit") || s.equalsIgnoreCase("quit")) {
                    out.println("Exiting.");
                    return;
                }
                log.add(s);
                if (s.equalsIgnoreCase("help")) {
                    printHelp();
                    continue;
                }
                if (s.equalsIgnoreCase("dumplog")) {
                    dumpLog();
                    out.println("Log dumped. ("+log.size()+" lines)");
                    continue;
                }
                if (s.equalsIgnoreCase("solve")) {
                    changed = true;
                    solve();
                    continue;
                }
                if (s.equalsIgnoreCase("rules")) {
                    listRules();
                    continue;
                }
                if (s.equalsIgnoreCase("relations")) {
                    listRelations();
                    continue;
                }
                if (s.startsWith("deleterule")) {
                    if (s.length() <= 11) {
                        out.println("Usage: deleterule #{,#}");
                    } else {
                        List rules = parseRules(s.substring(11).trim());
                        if (rules != null && !rules.isEmpty()) {
                            solver.rules.removeAll(rules);
                        }
                    }
                    continue;
                }
                if (s.startsWith("save")) {
                    if (s.length() <= 5) {
                        out.println("Usage: save <relation>{,<relation>}");
                    } else {
                        List relations = parseRelations(s.substring(5).trim());
                        if (relations != null && !relations.isEmpty()) {
                            solver.relationsToDump.addAll(relations);
                            solve();
                            solver.relationsToDump.removeAll(relations);
                        }
                    }
                    continue;
                }
                Object result = parser.parseDatalogLine(s, in);
                if (result != null) {
                    changed = true;
                    if (s.indexOf('?') >= 0 && result instanceof Collection) {
                        // This is a query, so we should run the solver.
                        Collection queries = (Collection) result;
                        solve();
                        solver.rules.removeAll(queries);
                        for (Iterator i = queries.iterator(); i.hasNext(); ) {
                            InferenceRule ir = (InferenceRule) i.next();
                            solver.deleteRelation(ir.bottom.relation);
                            ir.bottom.relation.free();
                            ir.free();
                        }
                    }
                }
            } catch (NoSuchElementException x) {
                out.println("Invalid command.");
                log.remove(log.size()-1);
            } catch (IllegalArgumentException x) {
                out.println("Invalid command.");
                log.remove(log.size()-1);
            } catch (IOException x) {
                out.println("IO Exception occurred: "+x);
            }
        }
    }

    /**
     * Parse a list of relations.
     * 
     * @param s  string rep of list of relations
     * @return  list of relations
     */
    List parseRelations(String s) {
        List relations = new LinkedList();
        while (s.length() != 0) {
            int i = s.indexOf(',');
            String relationName;
            if (i == -1) relationName = s;
            else relationName = s.substring(0, i);
            Relation r = solver.getRelation(relationName);
            if (r == null) {
                out.println("Unknown relation \""+relationName+"\"");
                return null;
            }
            relations.add(r);
            if (i == -1) break;
            s = s.substring(i+1).trim();
        }
        return relations;
    }
    
    /**
     * Parse a list of rules.
     * 
     * @param s  string rep of list of rules
     * @return  list of relations
     */
    List parseRules(String s) {
        String ruleNum = null;
        try {
            List rules = new LinkedList();
            for (;;) {
                int i = s.indexOf(',');
                if (i == -1) ruleNum = s;
                else ruleNum = s.substring(0, i);
                int k = Integer.parseInt(ruleNum);
                if (k < 1 || k > solver.rules.size()) {
                    out.println("Rule number out of range: "+k);
                    return null;
                }
                rules.add(solver.rules.get(k-1));
                if (i == -1) break;
                s = s.substring(i+1).trim();
            }
            return rules;
        } catch (NumberFormatException x) {
            out.println("Not a number: \""+ruleNum+"\"");
        }
        return null;
    }
    
    /**
     * List the relations the solver knows about.
     */
    void listRelations() {
        out.println(solver.nameToRelation.keySet());
    }
    
    /**
     * List the rules the solver knows about.
     */
    void listRules() {
        int k = 0;
        Iterator i = solver.rules.iterator();
        if (!i.hasNext()) {
            out.println("No rules defined.");
        } else {
            while (i.hasNext()) {
                InferenceRule r = (InferenceRule) i.next();
                ++k;
                out.println(k+": "+r);
            }
        }
    }
    
    /**
     * The set of relations that have been loaded, so we don't load them anymore.
     */
    Set loadedRelations = new HashSet();
    
    /**
     * Invoke the solver if we need to.
     * Also loads relations that haven't been loaded, and saves results if necessary.
     * 
     * @throws IOException
     */
    void solve() throws IOException {
        if (changed) {
            solver.splitRules();
            if (solver.NOISY) solver.out.print("Initializing solver: ");
            solver.initialize();
            if (solver.NOISY) solver.out.println("done.");
            if (!loadedRelations.containsAll(solver.relationsToLoad) ||
                !loadedRelations.containsAll(solver.relationsToLoadTuples)) {
                if (solver.NOISY) solver.out.print("Loading initial relations: ");
                Set newRelationsToLoad = new HashSet(solver.relationsToLoad);
                newRelationsToLoad.removeAll(loadedRelations);
                long time = System.currentTimeMillis();
                for (Iterator i = newRelationsToLoad.iterator(); i.hasNext();) {
                    Relation r = (Relation) i.next();
                    try {
                        r.load();
                    } catch (IOException x) {
                        out.println("WARNING: Cannot load bdd " + r + ": " + x.toString());
                    }
                }
                newRelationsToLoad = new HashSet(solver.relationsToLoadTuples);
                newRelationsToLoad.removeAll(loadedRelations);
                for (Iterator i = newRelationsToLoad.iterator(); i.hasNext();) {
                    Relation r = (Relation) i.next();
                    try {
                        r.loadTuples();
                    } catch (IOException x) {
                        out.println("WARNING: Cannot load tuples " + r + ": " + x.toString());
                    }
                }
                time = System.currentTimeMillis() - time;
                if (solver.NOISY) solver.out.println("done. (" + time + " ms)");
                loadedRelations.addAll(solver.relationsToLoad);
                loadedRelations.addAll(solver.relationsToLoadTuples);
            }
            if (solver.relationsToDump.isEmpty() &&
                solver.relationsToDumpTuples.isEmpty() &&
                solver.relationsToPrintSize.isEmpty() &&
                solver.relationsToPrintTuples.isEmpty() &&
                solver.dotGraphsToDump.isEmpty()) {
                solver.out.println("No relations marked as output!  ");
                if (IGNORE_OUTPUT)
                    out.println("(By default, the interactive driver ignores output keywords in the initial datalog.)");
                solver.out.println("You need to specify at least one relation as one of the following:");
                solver.out.println("    output");
                solver.out.println("    outputtuples");
                solver.out.println("    printsize");
                solver.out.println("    printtuples");
                solver.out.println("Alternatively, use a query like \"A(x,y)?\" rather than \"solve\".");
                return;
            }
            if (solver.NOISY) solver.out.println("Stratifying: ");
            long time = System.currentTimeMillis();
            solver.stratify();
            time = System.currentTimeMillis() - time;
            if (solver.NOISY) solver.out.println("done. (" + time + " ms)");
            if (solver.NOISY) solver.out.println("Solving: ");
            time = System.currentTimeMillis();
            solver.solve();
            time = System.currentTimeMillis() - time;
            if (solver.NOISY) solver.out.println("done. (" + time + " ms)");
            long solveTime = time;
            changed = false;
        }
        solver.saveResults();
    }
}
