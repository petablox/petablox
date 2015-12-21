// DatalogParser.java, created May 15, 2005 4:53:12 PM by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.math.BigInteger;
import jwutil.collections.AppendIterator;
import jwutil.collections.Pair;
import jwutil.io.SystemProperties;
import jwutil.strings.MyStringTokenizer;
import jwutil.util.Assert;
import net.sf.bddbddb.Solver.MyReader;
import net.sf.bddbddb.dataflow.PartialOrder.BeforeConstraint;
import net.sf.bddbddb.dataflow.PartialOrder.InterleavedConstraint;

/**
 * DatalogParser
 * 
 * @author jwhaley
 * @version $Id: DatalogParser.java 549 2005-05-17 10:17:33Z joewhaley $
 */
public class DatalogParser {
    
    /** Solver object we are modifying. */
    Solver solver;
    
    /** Trace flag. */
    boolean TRACE = SystemProperties.getProperty("traceparse") != null;
    
    /** Trace output stream. */
    public PrintStream out;
    public PrintStream err;
    
    /**
     * Construct a new Datalog parser with the given solver.
     * 
     * @param solver  add rules, relations, etc. to this solver
     */
    public DatalogParser(Solver solver) {
        this.solver = solver;
        this.out = solver.out;
        this.err = solver.err;
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
     * Get the next token, skipping over spaces.
     * 
     * @param st  string tokenizer
     * @return  next non-space token
     */
    static String nextToken(MyStringTokenizer st) {
        String s;
        do {
            s = st.nextToken();
        } while (s.equals(" ") || s.equals("\t"));
        return s;
    }

    /**
     * Read and parse a Datalog program.
     * 
     * @param inputFilename  name of file to read
     * @throws IOException
     */
    public void readDatalogProgram(String inputFilename) throws IOException {
        MyReader in = null;
        try {
            in = new MyReader(new LineNumberReader(new FileReader(inputFilename)));
            readDatalogProgram(in);
        } finally {
            if (in != null) try { in.close(); } catch (IOException _) { }
        }
    }
    
    /**
     * Read and parse a Datalog program.
     * 
     * @param in  input reader
     * @throws IOException
     */
    public void readDatalogProgram(MyReader in) throws IOException {
        for (;;) {
            String s = readLine(in);
            if (s == null) break;
            parseDatalogLine(s, in);
        }
    }
    
    /**
     * Parse a line from a Datalog program.
     * 
     * @param s  line to parse
     * @param in  input reader
     * @return  the rule, relation, domain, or list of rules we parsed, or null
     * @throws IOException
     */
    Object parseDatalogLine(String s, MyReader in) throws IOException {
		    int i = s.indexOf('#');
		    if (i != -1) s = s.substring(0, i);
        s = s.trim();
		    if (s.length() == 0) return null;
        int lineNum = in.getLineNumber();
        if (s.startsWith(".")) {
            // directive
            parseDirective(in, lineNum, s);
            return null;
        }
        MyStringTokenizer st = new MyStringTokenizer(s);
        if (st.hasMoreTokens()) {
            st.nextToken(); // name
            if (st.hasMoreTokens()) {
                String num = st.nextToken();
                boolean isNumber;
                try {
                    new BigInteger(num);
                    isNumber = true;
                } catch (NumberFormatException x) {
                    isNumber = false;
                }
                if (isNumber) {
                    // field domain
                    Domain fd = parseDomain(lineNum, s);
                    if (TRACE) out.println("Parsed field domain " + fd + " size " + fd.size);
                    if (solver.nameToDomain.containsKey(fd.name)) {
                        err.println("Error, field domain " + fd.name + " redefined on line " + in.getLineNumber() + ", ignoring.");
                    } else {
                        solver.nameToDomain.put(fd.name, fd);
                    }
                    return fd;
                }
            }
        }
        int dotIndex = s.indexOf('.');
        int braceIndex = s.indexOf('{');
        int qIndex = s.indexOf('?');
        if (dotIndex > 0 && (braceIndex == -1 || dotIndex < braceIndex)) {
            // rule
            InferenceRule ir = parseRule(lineNum, s);
            if (TRACE) out.println("Parsed rule " + ir);
            solver.rules.add(ir);
            return Collections.singletonList(ir);
        } else if (qIndex > 0 && (braceIndex == -1 || qIndex < braceIndex)) {
            // query
            List/*<InferenceRule>*/ ir = parseQuery(lineNum, s);
            if (ir != null) {
                if (TRACE) out.println("Parsed query " + ir);
                solver.rules.addAll(ir);
            }
            return ir;
        } else {
            // relation
            Relation r = parseRelation(lineNum, s);
            if (TRACE && r != null) out.println("Parsed relation " + r);
            return r;
        }
    }

    /**
     * Output a parse error at the given location.
     * 
     * @param linenum  line number of error
     * @param colnum  column number of error
     * @param line  text of line containing error
     * @param msg  error message
     */
    void outputError(int linenum, int colnum, String line, String msg) {
        err.println("Error on line " + linenum + ":");
        err.println(line);
        while (--colnum >= 0)
            err.print(' ');
        err.println('^');
        err.println(msg);
    }

    /**
     * Parse a directive.
     * 
     * @param in  input reader
     * @param lineNum  current line number, for debugging purposes
     * @param s  string containing directive to parse
     * @throws IOException
     */
    void parseDirective(MyReader in, int lineNum, String s) throws IOException {
        if (s.startsWith(".include")) {
            int index = ".include".length() + 1;
            String fileName = s.substring(index).trim();
            if (fileName.startsWith("\"")) {
                if (!fileName.endsWith("\"")) {
                    outputError(lineNum, index, s,
                        "Unmatched quotes");
                    throw new IllegalArgumentException();
                }
                fileName = fileName.substring(1, fileName.length() - 1);
            }
            if (!fileName.startsWith("/")) {
                String[] dirs = solver.includedirs.split(SystemProperties.getProperty("path.separator"));
                for (int i=0; i<dirs.length; i++) {
                    if ((new File(dirs[i],fileName)).exists()) {
                        fileName = dirs[i]+SystemProperties.getProperty("file.separator")+fileName;
                        break;
                    }
                }
            }
            in.registerReader(new LineNumberReader(new FileReader(fileName)));
        } else if (s.startsWith(".split_all_rules")) {
            boolean b = true;
            int index = ".split_all_rules".length() + 1;
            if (s.length() > index) {
                String option = s.substring(index).trim();
                b = !option.equals("false");
            }
            solver.SPLIT_ALL_RULES = b;
        } else if (s.startsWith(".report_stats")) {
            boolean b = true;
            int index = ".report_stats".length() + 1;
            if (s.length() > index) {
                String option = s.substring(index).trim();
                b = !option.equals("false");
            }
            solver.REPORT_STATS = b;
        } else if (s.startsWith(".noisy")) {
            boolean b = true;
            int index = ".noisy".length() + 1;
            if (s.length() > index) {
                String option = s.substring(index).trim();
                b = !option.equals("false");
            }
            solver.NOISY = b;
        } else if (s.startsWith(".trace")) {
            boolean b = true;
            int index = ".trace".length() + 1;
            if (s.length() > index) {
                String option = s.substring(index).trim();
                b = !option.equals("false");
            }
            TRACE = b;
        } else if (s.startsWith(".bddvarorder")) {
            if (SystemProperties.getProperty("bddvarorder") == null) {
                int index = ".bddvarorder".length() + 1;
                String varOrder = s.substring(index).trim();
                if (solver instanceof BDDSolver) {
                    ((BDDSolver) solver).VARORDER = varOrder;
                } else {
                    err.println("Ignoring .bddvarorder "+varOrder);
                }
            }
        } else if (s.startsWith(".bddnodes")) {
            if (SystemProperties.getProperty("bddnodes") == null) {
                int index = ".bddnodes".length() + 1;
                int n = Integer.parseInt(s.substring(index).trim());
                if (solver instanceof BDDSolver) {
                    ((BDDSolver) solver).BDDNODES = n;
                } else {
                    err.println("Ignoring .bddnodes "+n);
                }
            }
        } else if (s.startsWith(".bddcache")) {
            if (SystemProperties.getProperty("bddcache") == null) {
                int index = ".bddcache".length() + 1;
                int n = Integer.parseInt(s.substring(index).trim());
                if (solver instanceof BDDSolver) {
                    ((BDDSolver) solver).BDDCACHE = n;
                } else {
                    err.println("Ignoring .bddcache "+n);
                }
            }
        } else if (s.startsWith(".bddminfree")) {
            if (SystemProperties.getProperty("bddminfree") == null) {
                int index = ".bddminfree".length() + 1;
                double n = Double.parseDouble(s.substring(index).trim());
                if (solver instanceof BDDSolver) {
                    ((BDDSolver) solver).BDDMINFREE = n;
                } else {
                    err.println("Ignoring .bddminfree "+n);
                }
            }
        } else if (s.startsWith(".findbestorder")) {
            int index = ".findbestorder".length() + 1;
            String val = "";
            if (s.length() > index) val = s.substring(index).trim();
            System.setProperty("findbestorder", val);
        } else if (s.startsWith(".learnbestorder")){
            int index = ".learnbestorder".length() + 1;
            String val = "";
            if (s.length() > index) val = s.substring(index).trim();
            solver.LEARN_BEST_ORDER = true;
            solver.LEARN_ALL_RULES = true;
            System.setProperty("learnbestorder", val);
        } else if (s.startsWith(".incremental")) {
            int index = ".incremental".length() + 1;
            String val = "";
            if (s.length() > index) val = s.substring(index).trim();
            System.setProperty("incremental", val);
        } else if (s.startsWith(".dot")) {
            int index = ".dot".length() + 1;
            String dotSpec = "";
            if (s.length() > index) dotSpec = s.substring(index).trim();
            Dot dot = new Dot();
            LineNumberReader lnr = new LineNumberReader(new FileReader(solver.basedir + dotSpec));
            if (TRACE) out.println("Parsing dot " + dotSpec);
            dot.parseInput(solver, lnr);
            if (TRACE) out.println("Done parsing dot " + dotSpec);
            lnr.close();
            solver.dotGraphsToDump.add(dot);
        } else if (s.startsWith(".basedir")) {
            if (SystemProperties.getProperty("basedir") == null) {
                int index = ".basedir".length() + 1;
                String dirName = s.substring(index).trim();
                if (dirName.startsWith("\"") && dirName.endsWith("\"")) {
                    dirName = dirName.substring(1, dirName.length() - 1);
                }
                solver.basedir += dirName;
                String sep = SystemProperties.getProperty("file.separator");
                if (!solver.basedir.endsWith(sep) && !solver.basedir.endsWith("/")) {
                    solver.basedir += sep;
                }
                if (TRACE) out.println("Base directory is now \"" + solver.basedir + "\"");
                Assert._assert(solver.includedirs != null);
                solver.includedirs += SystemProperties.getProperty("path.separator") + solver.basedir;
            }
        } else {
            outputError(lineNum, 0, s,
                "Unknown directive \"" + s + "\"");
            throw new IllegalArgumentException();
        }
    }

    /**
     * Parse a domain declaration.
     * 
     * @param lineNum  current line number for outputting error messages
     * @param s  string containing relation declaration
     * @return  new domain 
     */
    Domain parseDomain(int lineNum, String s) {
        MyStringTokenizer st = new MyStringTokenizer(s);
        String name = nextToken(st);
        String num = nextToken(st);
        BigInteger size;
        try {
            size = new BigInteger(num);
        } catch (NumberFormatException x) {
            outputError(lineNum, st.getPosition(), s,
                "Expected a number, got \"" + num + "\"");
            throw new IllegalArgumentException();
        }
        Domain fd = new Domain(name, size);
        if (st.hasMoreTokens()) {
            String mapName = nextToken(st);
            BufferedReader dis = null;
            try {
                dis = new BufferedReader(new FileReader(solver.basedir + mapName));
                fd.loadMap(dis);
            } catch (IOException x) {
                err.println("WARNING: Cannot load mapfile \"" + solver.basedir + mapName + "\", skipping.");
            } finally {
                if (dis != null) try { dis.close(); } catch (IOException x) { }
            }
        }
        return fd;
    }

    static final char[] badchars = new char[] { '!', '=', ':', '-', '<', '>', '(', ')', ',', ' ', '\t', '\f' };
    
    /**
     * Parse a relation declaration.
     * 
     * @param lineNum  current line number for outputting error messages
     * @param s  string containing relation declaration
     * @return  new relation, or null if the relation was already defined
     */
    Relation parseRelation(int lineNum, String s) {
        MyStringTokenizer st = new MyStringTokenizer(s, " \t(:,)", true);
        String name = nextToken(st);
        for (int i = 0; i < badchars.length; ++i) {
            if (name.indexOf(badchars[i]) >= 0) {
                outputError(lineNum, st.getPosition(), s,
                    "Relation name cannot contain '"+badchars[i]+"'");
                throw new IllegalArgumentException();
            }
        }
        String openParen = nextToken(st);
        if (!openParen.equals("(")) {
            outputError(lineNum, st.getPosition(), s,
                "Expected \"(\", got \"" + openParen + "\"");
            throw new IllegalArgumentException();
        }
        List attributes = new LinkedList();
        for (;;) {
            String fName = nextToken(st);
            String colon = nextToken(st);
            if (!colon.equals(":")) {
                outputError(lineNum, st.getPosition(), s,
                    "Expected \":\", got \"" + colon + "\"");
                throw new IllegalArgumentException();
            }
            String fdName = nextToken(st);
            int numIndex = fdName.length() - 1;
            for (;;) {
                char c = fdName.charAt(numIndex);
                if (c < '0' || c > '9') break;
                --numIndex;
                if (numIndex < 0) {
                    outputError(lineNum, st.getPosition(), s,
                        "Expected field domain name, got \"" + fdName + "\"");
                    throw new IllegalArgumentException();
                }
            }
            ++numIndex;
            int fdNum = -1;
            if (numIndex < fdName.length()) {
                String number = fdName.substring(numIndex);
                try {
                    fdNum = Integer.parseInt(number);
                } catch (NumberFormatException x) {
                    outputError(lineNum, st.getPosition(), s,
                        "Cannot parse field domain number \"" + number + "\"");
                    throw new IllegalArgumentException();
                }
                fdName = fdName.substring(0, numIndex);
            }
            Domain fd = solver.getDomain(fdName);
            if (fd == null) {
                outputError(lineNum, st.getPosition(), s,
                    "Unknown field domain " + fdName);
                throw new IllegalArgumentException();
            }
            String option;
            if (fdNum != -1) option = fdName + fdNum;
            else option = "";
            attributes.add(new Attribute(fName, fd, option));
            String comma = nextToken(st);
            if (comma.equals(")")) break;
            if (!comma.equals(",")) {
                outputError(lineNum, st.getPosition(), s,
                    "Expected \",\" or \")\", got \"" + comma + "\"");
                throw new IllegalArgumentException();
            }
        }
        if (solver.nameToRelation.containsKey(name)) {
            err.println("Error, relation " + name + " redefined on line " + lineNum + ", ignoring.");
            return null;
        }
        Relation r = solver.createRelation(name, attributes);
        Pattern constraintPattern = Pattern.compile("(\\w+)([=<])(\\w+)");
        while (st.hasMoreTokens()) {
            String option = nextToken(st);
            Matcher constraintMatcher = constraintPattern.matcher(option);
            if (option.equals("preload")) {
                solver.relationsToPreLoad.add(r);
            } else if (option.equals("preloadOutput")) {
                solver.relationsToPreLoad.add(r);
                solver.relationsToDump.add(r);
            } else if (option.equals("output")) {
                solver.relationsToDump.add(r);
            } else if (option.equals("outputtuples")) {
                solver.relationsToDumpTuples.add(r);
            } else if (option.equals("input")) {
                solver.relationsToLoad.add(r);
            } else if (option.equals("inputtuples")) {
                solver.relationsToLoadTuples.add(r);
            } else if (option.equals("printtuples")) {
                solver.relationsToPrintTuples.add(r);
            } else if (option.equals("printsize")) {
                solver.relationsToPrintSize.add(r);
            } else if (option.startsWith("pri")) {
                String num = option.substring(4);
                int pri = Integer.parseInt(num);
                r.priority = pri;
            } else if (option.equals("{")) {
                String s2 = nextToken(st);
                StringBuffer sb = new StringBuffer();
                while (!s2.equals("}")) {
                    sb.append(' ');
                    sb.append(s2);
                    if (!st.hasMoreTokens()) {
                        outputError(lineNum, st.getPosition(), s,
                            "Expected \"}\" to terminate code block");
                        throw new IllegalArgumentException();
                    }
                    s2 = nextToken(st);
                }
                CodeFragment f = new CodeFragment(sb.toString(), r);
                r.onUpdate.add(f);
            } else if (constraintMatcher.matches()) {
                parseAndAddConstraint(r, constraintMatcher);
            } else {
                outputError(lineNum, st.getPosition(), s,
                    "Unexpected option '" + option + "'");
                throw new IllegalArgumentException();
            }
        }
        if (!r.constraints.isEmpty()) {
            r.constraints.satisfy();
        }
        return r;
    }
    
    /**
     * Parse and add an attribute constraint.
     * 
     * @param r  relation to add constraint for
     * @param m  constraint pattern matcher
     */
    public void parseAndAddConstraint(Relation r, Matcher m) {
        if (m.matches()) {
            String leftAttr = m.group(1);
            String type = m.group(2);
            String rightAttr = m.group(3);
            Attribute left = null;
            Attribute right = null;
            for (Iterator it = r.getAttributes().iterator(); it.hasNext();) {
                Attribute a = (Attribute) it.next();
                if (a.attributeName.equals(leftAttr)) left = a;
                if (a.attributeName.equals(rightAttr)) right = a;
            }
            if (left == null) {
                out.println("Specified Attribute not found: " + leftAttr);
                throw new IllegalArgumentException();
            } else if (right == null) {
                out.println("Specified Attribute not found: " + rightAttr);
                throw new IllegalArgumentException();
            }
            
            if (type.equals("=")) r.constraints.addInterleavedConstraint(new InterleavedConstraint(r,left,r,right,10));
            else if (type.equals("<")) r.constraints.addBeforeConstraint(new BeforeConstraint(r,left,r,right,10));
            if (TRACE) out.println("parsed constraint: " + leftAttr + " " + type + " " + rightAttr);
        } else {
            //handle error
        }
    }
    
    /**
     * Parse an inference rule.
     * 
     * @param lineNum  current line number for outputting error messages
     * @param s  string containing rule to parse
     * @return  new inference rule
     */
    InferenceRule parseRule(int lineNum, String s) {
        MyStringTokenizer st = new MyStringTokenizer(s, " \t(,/).=!<>", true);
        Map/*<String,Variable>*/ nameToVar = new HashMap();
        undeclaredRelations.clear();
        RuleTerm bottom = parseRuleTerm(lineNum, s, nameToVar, st);
        String sep = nextToken(st);
        List/*<RuleTerm>*/ terms = new LinkedList();
        if (!sep.equals(".")) {
            if (!sep.equals(":-")) {
                outputError(lineNum, st.getPosition(), s,
                    "Expected \":-\", got \"" + sep + "\"");
                throw new IllegalArgumentException();
            }
            for (;;) {
                RuleTerm rt = parseRuleTerm(lineNum, s, nameToVar, st);
                if (rt == null) break;
                terms.add(rt);
                sep = nextToken(st);
                if (sep.equals(".")) break;
                if (!sep.equals(",")) {
                    outputError(lineNum, st.getPosition(), s,
                        "Expected \".\" or \",\", got \"" + sep + "\"");
                    throw new IllegalArgumentException();
                }
            }
        }
        handleUndeclaredRelations(lineNum, s, nameToVar, terms, bottom);
        InferenceRule ir = solver.createInferenceRule(terms, bottom);
        Variable v = ir.checkUniversalVariables();
        if (v != null) {
            outputError(lineNum, st.getPosition(), s,
                "Variable "+v+" was only used once!  Use '_' instead.");
            throw new IllegalArgumentException();
        }
        ir = parseRuleOptions(lineNum, s, ir, st);
        return ir;
    }

    /**
     * Finish the declaration of undeclared relations.
     */
    void handleUndeclaredRelations(int lineNum, String s, Map nameToVar,
        List terms, RuleTerm bottom) {
        for (;;) {
            boolean change = false;
            for (Iterator i = undeclaredRelations.iterator(); i.hasNext(); ) {
                Relation r = (Relation) i.next();
                boolean any = false;
                for (Iterator j = r.attributes.iterator(); j.hasNext(); ) {
                    Attribute a = (Attribute) j.next();
                    if (a.attributeDomain == null) {
                        // We didn't know the domain then, maybe we do now.
                        any = true;
                        String name = a.attributeName;
                        Variable v = (Variable) nameToVar.get(name);
                        if (v == null) {
                            outputError(lineNum, 0, s,
                                "Cannot find variable "+name);
                            throw new IllegalArgumentException();
                        }
                        if (v.domain != null) {
                            a.attributeDomain = v.domain;
                            change = true;
                        }
                    }
                }
                if (!any) {
                    if (solver.NOISY) out.println("Implicitly defining relation "+r.verboseToString());
                    i.remove();
                    // Handle other uses of this relation in this rule.
                    for (Iterator j = new AppendIterator(terms.iterator(), Collections.singleton(bottom).iterator());
                         j.hasNext(); ) {
                        RuleTerm rt = (RuleTerm) j.next();
                        if (rt.relation != r) continue;
                        for (int k = 0; k < r.numberOfAttributes(); ++k) {
                            Variable v = rt.getVariable(k);
                            if (v.domain != null) continue;
                            Attribute a = r.getAttribute(k);
                            v.domain = a.attributeDomain;
                            change = true;
                        }
                    }
                }
            }
            if (!change) break;
        }
        if (!undeclaredRelations.isEmpty()) {
            outputError(lineNum, 0, s,
                "Cannot infer attributes for undeclared relations "+undeclaredRelations);
            throw new IllegalArgumentException();
        }
    }
    
    /**
     * Parse the options for an inference rule.
     * 
     * @param lineNum  current line number for outputting error messages
     * @param s  current line for outputting error messages
     * @param ir  inference rule to parse options for
     * @param st  string tokenizer containing options to parse
     * @return  the resulting rule
     */
    InferenceRule parseRuleOptions(int lineNum, String s, InferenceRule ir, MyStringTokenizer st) {
        while (st.hasMoreTokens()) {
            String option = nextToken(st);
            if (option.equals("split")) {
                if (!solver.SPLIT_NO_RULES) {
                    if (TRACE) out.println("Splitting rule " + ir);
                    ir.split = true;
                }
            } else if (option.equals("number")) {
                if (TRACE) out.println("Rule " + ir + " defines a numbering");
                ir = solver.createNumberingRule(ir);
            } else if (option.equals("single")) {
                if (TRACE) out.println("Rule " + ir + " only adds a single satisfying assignment");
                ir.single = true;
            } else if (option.equals("cacheafterrename")) {
                BDDInferenceRule r = (BDDInferenceRule) ir;
                r.cache_before_rename = false;
            } else if (option.equals("findbestorder")) {
                BDDInferenceRule r = (BDDInferenceRule) ir;
                r.find_best_order = true;
            } else if (option.equals("trace")) {
                BDDInferenceRule r = (BDDInferenceRule) ir;
                r.TRACE = true;
            } else if (option.equals("tracefull")) {
                BDDInferenceRule r = (BDDInferenceRule) ir;
                r.TRACE = true;
                r.TRACE_FULL = true;
            } else if (option.equals("pri")) {
                String num = nextToken(st);
                if (num.equals("=")) num = nextToken(st);
                int pri = Integer.parseInt(num);
                ir.priority = pri;
            } else if (option.equals("pre") || option.equals("post") || option.equals("{")) {
                StringBuffer sb = new StringBuffer();
                String s2 = nextToken(st);
                int type = 2;
                if (!option.equals("{")) {
                    if (option.equals("pre")) type = 1;
                    if (!s2.equals("{")) {
                        outputError(lineNum, st.getPosition(), s,
                            "Expected \"{\", but found \""+s2+"\"");
                        throw new IllegalArgumentException();
                    }
                    s2 = nextToken(st);
                }
                while (!s2.equals("}")) {
                    sb.append(' ');
                    sb.append(s2);
                    if (!st.hasMoreTokens()) {
                        outputError(lineNum, st.getPosition(), s,
                            "Expected \"}\" to terminate code block");
                        throw new IllegalArgumentException();
                    }
                    s2 = nextToken(st);
                }
                CodeFragment f = new CodeFragment(sb.toString(), ir);
                if (type == 1) ir.preCode.add(f);
                else ir.postCode.add(f);
            } else if (option.equals("modifies")) {
                String relationName = nextToken(st);
                if (relationName.equals("(")) {
                    for (;;) {
                        relationName = nextToken(st);
                        if (relationName.equals(",")) continue;
                        if (relationName.equals(")")) break;
                        Relation r = solver.getRelation(relationName);
                        if (r == null) {
                            outputError(lineNum, st.getPosition(), s,
                                "Unknown relation \""+relationName+"\"");
                            throw new IllegalArgumentException();
                        }
                        ir.extraDefines.add(r);
                    }
                } else {
                    Relation r = solver.getRelation(relationName);
                    if (r == null) {
                        outputError(lineNum, st.getPosition(), s,
                            "Unknown relation \""+relationName+"\"");
                        throw new IllegalArgumentException();
                    }
                    ir.extraDefines.add(r);
                }
            } else {
                // todo: maxiter=#
                outputError(lineNum, st.getPosition(), s,
                    "Unknown rule option \"" + option + "\"");
                throw new IllegalArgumentException();
            }
        }
        if (hasDuplicateVars && !(ir instanceof NumberingRule)) {
            outputError(lineNum, st.getPosition(), s,
                "Variable repeated multiple times in a single term");
            throw new IllegalArgumentException();
        }
        hasDuplicateVars = false;
        return ir;
    }

    /**
     * Flag that is set if a rule term has repeated variables.
     */
    boolean hasDuplicateVars;
    
    /**
     * Temporary collection to hold undeclared relations.
     */
    Collection undeclaredRelations = new LinkedList();
    
    /**
     * Parse a term of an inference rule.
     * 
     * @param lineNum  current line number for outputting error messages
     * @param s  current line for outputting error messages
     * @param nameToVar  map from variable names to variables
     * @param st  string tokenizer containing rule term to parse
     * @return  rule term, or null if string is "?"
     */
    RuleTerm parseRuleTerm(int lineNum, String s, Map/*<String,Variable>*/ nameToVar, MyStringTokenizer st) {
        boolean negated = false;
        String relationName = nextToken(st);
        if (relationName.equals("?")) {
            return null;
        }
        if (relationName.equals("!")) {
            negated = true;
            relationName = nextToken(st);
        }
        String openParen = nextToken(st);
        boolean flip = false;
        boolean less = false;
        boolean greater = false;
        if (openParen.equals("!")) {
            flip = true;
            openParen = nextToken(st);
        } else if (openParen.equals("<")) {
            less = true;
            openParen = nextToken(st);
        } else if (openParen.equals(">")) {
            greater = true;
            openParen = nextToken(st);
        }
        if (openParen.equals("=") || less || greater) {
            if (negated) {
                outputError(lineNum, st.getPosition(), s,
                    "Unexpected \"!\"");
                throw new IllegalArgumentException();
            }
            // "a = b".
            boolean equals = openParen.equals("=");
            String varName1 = relationName;
            String varName2 = equals ? nextToken(st) : openParen;
            Variable var1, var2;
            var1 = (Variable) nameToVar.get(varName1);
            Relation r;
            if (equals && varName2.equals(">")) {
                if (negated || less || greater) {
                    outputError(lineNum, st.getPosition(), s,
                        "Unexpected \"!\", \"<\", or \">\" with \"=>\"");
                    throw new IllegalArgumentException();
                }
                // "a => b".
                varName2 = nextToken(st);
                var2 = (Variable) nameToVar.get(varName2);
                if (var1 == null || var2 == null) {
                    outputError(lineNum, st.getPosition(), s,
                        "Cannot use \"=>\" on unbound variables.");
                    throw new IllegalArgumentException();
                }
                r = solver.getMapRelation(var1.domain, var2.domain);
            } else {
                var2 = (Variable) nameToVar.get(varName2);
                if (var1 == null) {
                    if (var2 == null) {
                        outputError(lineNum, st.getPosition(), s,
                            "Cannot use \"=\", \"!=\", \"<\", \"<=\", \">\", \">=\", on two unbound variables.");
                        throw new IllegalArgumentException();
                    }
                    var1 = parseVariable(var2.domain, nameToVar, varName1);
                } else {
                    if (var2 == null) {
                        var2 = parseVariable(var1.domain, nameToVar, varName2);
                    }
                }
                if (var1.domain == null) var1.domain = var2.domain;
                if (var2.domain == null) var2.domain = var1.domain;
                if (var1.domain == null && var2.domain == null) {
                    outputError(lineNum, st.getPosition(), s,
                        "Cannot infer domain for variables "+var1+" and "+var2+" used in comparison.");
                    throw new IllegalArgumentException();
                }
                if (less) {
                    r = equals ? solver.getLessThanOrEqualRelation(var1.domain, var2.domain) : solver.getLessThanRelation(var1.domain, var2.domain);
                } else if (greater) {
                    r = equals ? solver.getGreaterThanOrEqualRelation(var1.domain, var2.domain) : solver.getGreaterThanRelation(var1.domain, var2.domain);
                } else {
                    r = flip ? solver.getNotEquivalenceRelation(var1.domain, var2.domain) : solver.getEquivalenceRelation(var1.domain, var2.domain);
                }
            }
            List vars = new Pair(var1, var2);
            RuleTerm rt = new RuleTerm(r, vars);
            return rt;
        } else if (!openParen.equals("(")) {
            outputError(lineNum, st.getPosition(), s,
                "Expected \"(\" or \"=\", got \"" + openParen + "\"");
            throw new IllegalArgumentException();
        }
        if (flip) {
            outputError(lineNum, st.getPosition(), s,
                "Unexpected \"!\"");
            throw new IllegalArgumentException();
        }
        Relation r = solver.getRelation(relationName);
        //if (r == null) {
        //    outputError(lineNum, st.getPosition(), s,
        //        "Unknown relation " + relationName);
        //    throw new IllegalArgumentException();
        //}
        if (negated && r != null) r = r.makeNegated(solver);
        List/*<Variable>*/ vars = new LinkedList();
        for (;;) {
            if (r != null && r.attributes.size() <= vars.size()) {
                outputError(lineNum, st.getPosition(), s,
                    "Too many fields for " + r);
                throw new IllegalArgumentException();
            }
            Attribute a = null;
            Domain fd = null;
            if (r != null) {
                a = (Attribute) r.attributes.get(vars.size());
                fd = a.attributeDomain;
            }
            String varName = nextToken(st);
            Variable var = parseVariable(fd, nameToVar, varName);
            if (vars.contains(var)) {
                hasDuplicateVars = true;
            }
            vars.add(var);
            if (var.domain == null) var.domain = fd;
            else if (fd != null && var.domain != fd) {
                outputError(lineNum, st.getPosition(), s,
                    "Variable " + var + " used as both " + var.domain + " and " + fd);
                throw new IllegalArgumentException();
            }
            String sep = nextToken(st);
            if (sep.equals(")")) break;
            if (!sep.equals(",")) {
                outputError(lineNum, st.getPosition(), s,
                    "Expected ',' or ')', got '" + sep + "'");
                throw new IllegalArgumentException();
            }
        }
        if (r != null && r.attributes.size() != vars.size()) {
            outputError(lineNum, st.getPosition(), s,
                "Wrong number of vars in rule term for " + relationName);
            throw new IllegalArgumentException();
        }
        if (r == null) {
            // Implicit creation of relation.
            List/*<Attribute>*/ attribs = new ArrayList(vars.size());
            for (Iterator i = vars.iterator(); i.hasNext(); ) {
                Variable v = (Variable) i.next();
                if (v instanceof Constant || "_".equals(v.name)) {
                    outputError(lineNum, st.getPosition(), s,
                        "Cannot infer attribute for '"+v.name+"' in undeclared relation "+relationName);
                    throw new IllegalArgumentException();
                }
                Attribute a = new Attribute(v.name, v.domain, null);
                attribs.add(a);
            }
            r = solver.createRelation(relationName, attribs);
            undeclaredRelations.add(r);
        }
        RuleTerm rt = new RuleTerm(r, vars);
        return rt;
    }

    /**
     * Parse a variable or a constant.
     * 
     * @param fd  domain of variable/constant
     * @param nameToVar  map from names to variables for this rule
     * @param varName  name of variable/constant
     * @return  variable/constant
     */
    Variable parseVariable(Domain fd, Map nameToVar, String varName) {
        char firstChar = varName.charAt(0);
        Variable var;
        if (firstChar >= '0' && firstChar <= '9') {
            var = new Constant(Long.parseLong(varName));
        } else if (firstChar == '"') {
            String namedConstant = varName.substring(1, varName.length() - 1);
            var = new Constant(fd.namedConstant(namedConstant));
        } else if (!varName.equals("_")) {
            var = (Variable) nameToVar.get(varName);
            if (var == null) nameToVar.put(varName, var = new Variable(varName));
        } else {
            var = new Variable();
        }
        if (var.domain == null) var.domain = fd;
        return var;
    }
    
    /**
     * Parse a query.  A query is a statement that ends with '?'.
     * 
     * @param lineNum  current line number, for outputting error messages
     * @param s  line to parse
     * @return  list of inference rules implementing the query.
     */
    List/*<InferenceRule>*/ parseQuery(int lineNum, String s) {
        MyStringTokenizer st = new MyStringTokenizer(s, " \t(,/).=~!?<>", true);
        Map/*<String,Variable>*/ nameToVar = new HashMap();
        
        if (s.indexOf(":-") > 0) {
            RuleTerm rt = parseRuleTerm(lineNum, s, nameToVar, st);
            String sep = nextToken(st);
            if (!sep.equals(":-")) {
                outputError(lineNum, st.getPosition(), s, "Expected \":-\", got \"" + sep + "\"");
                throw new IllegalArgumentException();
            }
            List/*<RuleTerm>*/ extras = new LinkedList();
            for (;;) {
                RuleTerm rt2 = parseRuleTerm(lineNum, s, nameToVar, st);
                if (rt2 == null) break;
                extras.add(rt2);
                String sep2 = nextToken(st);
                if (sep2.equals("?")) break;
                if (!sep2.equals(",")) {
                    outputError(lineNum, st.getPosition(), s, "Expected \",\", got \"" + sep2 + "\"");
                    throw new IllegalArgumentException();
                }
            }
            boolean single = false;
            while (st.hasMoreTokens()) {
                String option = nextToken(st);
                if (option.equals("single")) {
                    single = true;
                } else {
                    outputError(lineNum, st.getPosition(), s, "Unknown query option \"" + option + "\"");
                    throw new IllegalArgumentException();
                }
            }
            return solver.comeFromQuery(rt, extras, single);
        }
        List/*<RuleTerm>*/ terms = new LinkedList();
        Map varMap = new LinkedHashMap();
        for (;;) {
            RuleTerm rt = parseRuleTerm(lineNum, s, nameToVar, st);
            if (rt == null) break;
            terms.add(rt);
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
            String sep = nextToken(st);
            if (sep.equals("?")) break;
            if (!sep.equals(",")) {
                outputError(lineNum, st.getPosition(), s,
                    "Expected \",\", got \"" + sep + "\"");
                throw new IllegalArgumentException();
            }
        }
        List vars = new ArrayList(varMap.keySet());
        List attributes = new ArrayList(vars.size());
        for (Iterator i = varMap.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry) i.next();
            Variable v = (Variable) e.getKey();
            String name = (String) e.getValue();
            attributes.add(new Attribute(name, v.getDomain(), ""));
        }
        Relation r = solver.createRelation("query@"+lineNum, attributes);
        RuleTerm bottom = new RuleTerm(r, vars);
        InferenceRule ir = solver.createInferenceRule(terms, bottom);
        Variable v = ir.checkUniversalVariables();
        if (v != null) {
            outputError(lineNum, st.getPosition(), s,
                "Variable "+v+" was only used once!  Use '_' instead.");
            throw new IllegalArgumentException();
        }
        ir = parseRuleOptions(lineNum, s, ir, st);
        solver.relationsToPrintTuples.add(r);
        return Collections.singletonList(ir);
    }
    
}
