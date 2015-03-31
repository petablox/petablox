/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 *
 * Pointer analysis which performs refinement and pruning using slivers.
 *
 * An abstraction specifies a partitioning of chains (a chain is a list of allocation/call sites).
 * A sliver specifies a set of chains in one of two ways:
 *  - [h1, h2, h3] represents exactly this one sequence.
 *  - [h1, h2, h3, null] represents all sequences with the given prefix.
 * Assume that the abstraction is specified by a consistent set of slivers.
 *
 * Three operations:
 *   - EXTEND: building CH,CI,CC from a set of active slivers (prepending)
 *   - REFINE: growing slivers (appending)
 *   - COARSEN: use a type strategy
 *
 * @author Percy Liang (pliang@cs.berkeley.edu)
 */
package chord.analyses.sliver;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.net.Socket;
import java.net.ServerSocket;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.procedure.TObjectIntProcedure;

import joeq.Class.jq_ClassInitializer;
import joeq.Class.jq_Class;
import joeq.Class.jq_Type;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Reference;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import chord.util.Execution;
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.DomC;
import chord.analyses.alias.CtxtsAnalysis;
import chord.bddbddb.Rel.PairIterable;
import chord.bddbddb.Rel.TrioIterable;
import chord.bddbddb.Rel.QuadIterable;
import chord.bddbddb.Rel.PentIterable;
import chord.bddbddb.Rel.HextIterable;
import chord.bddbddb.Rel.RelView;
import chord.analyses.alloc.DomH;
import chord.analyses.invk.DomI;
import chord.analyses.method.DomM;
import chord.analyses.heapacc.DomE;
import chord.analyses.point.DomP;
import chord.analyses.var.DomV;
import chord.program.Program;
import chord.project.Chord;
import chord.project.Messages;
import chord.project.OutDirUtils;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.DlogAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.IndexMap;
import chord.util.ArraySet;
import chord.util.graph.IGraph;
import chord.util.graph.MutableGraph;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import chord.util.Utils;
import chord.util.StopWatch;

import static chord.analyses.sliver.GlobalInfo.G;

// Specifies the abstraction
class TypeStrategy {
  private Execution X = Execution.v();

  private HashMap<Quad,Quad> prototypes = new HashMap<Quad,Quad>(); // Map each site to the prototype of its equivalence class
  private HashMap<Quad,List<Quad>> clusters = new HashMap<Quad,List<Quad>>(); // Map each prototype to all others in equivalence class
  private boolean use_c; // Use class
  private boolean use_t; // Type of allocation sites
  private boolean isIdentity, isSingle;

  public boolean isIdentity() { return isIdentity; }

  String description;
  boolean disallowRepeats; // Whether to truncate early
  @Override public String toString() { return description; }

  public TypeStrategy(String description, boolean disallowRepeats) {
    this.description = description;
    this.disallowRepeats = disallowRepeats;
    if ("has".equals(description)) use_c = true;
    else if ("is".equals(description)) use_t = true;
    else if ("is,has".equals(description)) use_c = use_t = true;
    else if ("identity".equals(description)) isIdentity = true;
    else if ("single".equals(description)) isSingle = true;
    else throw new RuntimeException("Unknown typeStrategy: "+description);
  }

  // COARSEN
  public Ctxt project(Ctxt c) {
    // Apply the coarsening elementwise
    Quad[] elems = new Quad[c.length()];
    for (int i = 0; i < elems.length; i++)
      elems[i] = project(c.get(i));

    // Might need to truncate more to remove repeats
    Ctxt cc = new Ctxt(elems);
    // Take the longest barely-repeating prefix (quadratic warning!)
    if (disallowRepeats) {
      int len = G.len(cc);
      if (len <= 1) return cc;
      int m;
      for (m = 2; m <= len; m++) { // See if the first m is not non-repeating (c[m-1] exists before)
        boolean found = false;
        for (int k = 0; k < m-1; k++)
          if (elems[k] == elems[m-1]) { found = true; break; }
        if (found) return G.summarize(cc.prefix(m)); // Longest is length m
      }
    }
    return cc; // Take everything
  }

  public Quad project(Quad j) {
    if (j == null) return null;
    Quad proto_j = prototypes.get(j);
    assert proto_j != null : G.jstr(j);
    return proto_j;
  }
  public List<Quad> lift(Quad j) {
    j = project(j);
    List<Quad> cluster = clusters.get(j);
    assert cluster != null : G.jstr(j);
    return cluster;
  }

  public Collection<Quad> usePrototypes(Collection<Quad> sites) {
    if (isIdentity()) return sites;

    Set<Quad> prototypeSites = new HashSet();
    for (Quad j : sites)
      prototypeSites.add(prototypes.get(j));
    return prototypeSites;
  }

  private void addIdentity(Quad j) {
    prototypes.put(j, j);
    List<Quad> l = new ArrayList();
    l.add(j);
    clusters.put(j, l);
  }

  public void init() {
    if (isIdentity) {
      X.logs("TypeStrategy: using identity");
      for (Quad j : G.jSet) addIdentity(j);
    }
    else if (isSingle) { // Put every site into one cluster (just for testing/sanity checking)
      X.logs("TypeStrategy: using single");
      Quad proto_j = null;
      List<Quad> cluster = new ArrayList();
      for (Quad j : G.jSet) {
        if (proto_j == null) {
          proto_j = j;
          clusters.put(proto_j, cluster);
        }
        prototypes.put(j, proto_j);
        cluster.add(j);
      }
    }
    else {
      HashMap<Object,Quad> summary2prototypes = new HashMap<Object,Quad>();
      X.logs("TypeStrategy: containing class (%s), type of site (%s)", use_c, use_t);
      for (Quad h : G.hSet) {
        Object summary = null;
        if (use_c && use_t) summary = new Pair(G.h2c(h), G.h2t(h));
        else if (use_c) summary = G.h2c(h);
        else if (use_t) summary = G.h2t(h);
        else assert false;

        Quad proto_h = summary2prototypes.get(summary);
        if (proto_h == null) summary2prototypes.put(summary, proto_h = h);
        prototypes.put(h, proto_h);

        List<Quad> cluster = clusters.get(proto_h);
        if (cluster == null) clusters.put(proto_h, cluster = new ArrayList());
        cluster.add(h);
      }
      for (Quad i : G.iSet) addIdentity(i);
    }
    X.logs("  %s sites -> %s clusters", G.jSet.size(), clusters.size());

    // Output
    PrintWriter out = Utils.openOut(X.path("typeStrategy"));
    for (Quad proto_j : clusters.keySet()) {
      out.println(G.jstr(proto_j));
      for (Quad j : clusters.get(proto_j))
        out.println("  "+G.jstr(j));
    }
    out.close();
  }
}

class GlobalInfo {
  static GlobalInfo G;

  // Slivers
  final Ctxt emptyCtxt = new Ctxt(new Quad[0]);
  boolean isAlloc(Quad q) { return domH.indexOf(q) != -1; } // Is an allocation site?
  boolean hasHeadSite(Ctxt c) { return c.length() > 0 && c.head() != null; }
  boolean isSummary(Ctxt c) { return c.length() > 0 && c.last() == null; }
  boolean isAtom(Ctxt c) { return c.length() == 0 || c.last() != null; }
  Ctxt summarize(Ctxt c) { assert isAtom(c); return c.append(null); } // add null
  Ctxt atomize(Ctxt c) { assert isSummary(c); return c.prefix(c.length()-1); } // remove null
  int summaryLen(Ctxt c) { assert isSummary(c); return c.length()-1; } // don't count null
  int atomLen(Ctxt c) { assert isAtom(c); return c.length(); }
  int len(Ctxt c) { return isAtom(c) ? c.length() : c.length()-1; }

  // Technical special case designed to handle 0-CFA.
  // Because we always have allocation sites (minH > 0),
  // [*] means any chain starting with a call site.
  // Use [*] if we need to capture those contexts and [] otherwise.
  Ctxt initEmptyCtxt(int minI) { return minI == 0 ? G.summarize(G.emptyCtxt) : G.emptyCtxt; }

  DomV domV;
  DomM domM;
  DomI domI;
  DomH domH;
  DomE domE;
  DomP domP;
  HashMap<jq_Method,List<Quad>> rev_jm;
  Set<Quad> hSet;
  Set<Quad> iSet;
  Set<Quad> jSet;

  void sleep(int seconds) {
    try { Thread.sleep(seconds*1000); } catch(InterruptedException e) { }
  }

  public GlobalInfo() {
    domV = (DomV) ClassicProject.g().getTrgt("V"); ClassicProject.g().runTask(domV);
    domM = (DomM) ClassicProject.g().getTrgt("M"); ClassicProject.g().runTask(domM);
    domI = (DomI) ClassicProject.g().getTrgt("I"); ClassicProject.g().runTask(domI);
    domH = (DomH) ClassicProject.g().getTrgt("H"); ClassicProject.g().runTask(domH);
    domE = (DomE) ClassicProject.g().getTrgt("E"); ClassicProject.g().runTask(domE);
    domP = (DomP) ClassicProject.g().getTrgt("P"); ClassicProject.g().runTask(domP);
  }

  // Map allocation site to its containing class
  jq_Type h2c(Quad h) { return h.getMethod().getDeclaringClass(); }

  jq_Type h2t(Quad h) {
    Operator op = h.getOperator();
    if (op instanceof New) 
      return New.getType(h).getType();
    else if (op instanceof NewArray)
      return NewArray.getType(h).getType();
    else if (op instanceof MultiNewArray)
      return MultiNewArray.getType(h).getType();
    else
      return null;
  }

  // Helpers for displaying stuff
  String pstr(Quad p) { return new File(p.toJavaLocStr()).getName(); }
  String hstr(Quad h) {
    jq_Type t = h2t(h);
    return pstr(h)+"("+(t == null ? "?" : t.shortName())+")";
  }
  String istr(Quad i) {
    jq_Method m = InvokeStatic.getMethod(i).getMethod();
    return pstr(i)+"("+m.getName()+")";
  }
  String jstr(Quad j) { return isAlloc(j) ? hstr(j) : istr(j); }
  String estr(Quad e) {
    Operator op = e.getOperator();
    return pstr(e)+"("+op+")";
  }
  String cstr(Ctxt c) {
    StringBuilder buf = new StringBuilder();
    //buf.append(domC.indexOf(c));
    buf.append('{');
    for (int i = 0; i < c.length(); i++) {
      if (i > 0) buf.append(" | ");
      Quad q = c.get(i);
      buf.append(q == null ? "+" : jstr(q));
    }
    buf.append('}');
    return buf.toString();
  }
  String fstr(jq_Field f) { return f.getDeclaringClass()+"."+f.getName(); }
  String vstr(Register v) { return v+"@"+mstr(domV.getMethod(v)); }
  String mstr(jq_Method m) { return m.getDeclaringClass().shortName()+"."+m.getName(); }

  String render(Object o) {
    if (o == null) return "NULL";
    if (o instanceof String) return (String)o;
    if (o instanceof Integer) return o.toString();
    if (o instanceof Ctxt) return cstr((Ctxt)o);
    if (o instanceof jq_Field) return fstr((jq_Field)o);
    if (o instanceof jq_Method) return mstr((jq_Method)o);
    if (o instanceof Register) return vstr((Register)o);
    if (o instanceof Quad) {
      Quad q = (Quad)o;
      if (domH.indexOf(q) != -1) return hstr(q);
      if (domI.indexOf(q) != -1) return istr(q);
      if (domE.indexOf(q) != -1) return estr(q);
      return q.toString();
      //throw new RuntimeException("Quad not H, I, or E: " + q);
    }
    if (o instanceof Pair) {
      Pair p = (Pair)o;
      return "<"+render(p.val0)+","+render(p.val1)+">";
    }
    return o.toString();
    //throw new RuntimeException("Unknown object (not abstract object, contextual variable or field: "+o+" has type "+o.getClass());
  }

  PrintWriter getOut(Socket s) throws IOException { return new PrintWriter(s.getOutputStream(), true); }
  BufferedReader getIn(Socket s) throws IOException { return new BufferedReader(new InputStreamReader(s.getInputStream())); }
}

////////////////////////////////////////////////////////////

interface Query extends Comparable {
  void addToRel(ProgramRel rel);
  String encode();
}

class QueryEE implements Query {
  Quad e1, e2;
  QueryEE(Quad e1, Quad e2) { this.e1 = e1; this.e2 = e2; }
  @Override public int hashCode() { return e1.hashCode() * 37 + e2.hashCode(); }
  @Override public boolean equals(Object _that) {
    QueryEE that = (QueryEE)_that;
    return e1.equals(that.e1) && e2.equals(that.e2);
  }
  public void addToRel(ProgramRel rel) { rel.add(e1, e2); }
  @Override public String toString() { return G.estr(e1)+"|"+G.estr(e2); }
  public String encode() { return "E"+G.domE.indexOf(e1)+","+G.domE.indexOf(e2); }
  @Override public int compareTo(Object _that) {
    QueryEE that = (QueryEE)_that;
    int a, b;
    a = G.domE.indexOf(this.e1);
    b = G.domE.indexOf(that.e1);
    if (a != b) return a < b ? -1 : +1;
    a = G.domE.indexOf(this.e2);
    b = G.domE.indexOf(that.e2);
    if (a != b) return a < b ? -1 : +1;
    return 0;
  }
}

class QueryE implements Query {
  Quad e;
  QueryE(Quad e) { this.e = e; }
  @Override public int hashCode() { return e.hashCode(); }
  @Override public boolean equals(Object _that) { return e.equals(((QueryE)_that).e); }
  public void addToRel(ProgramRel rel) { rel.add(e); }
  @Override public String toString() { return G.estr(e); }
  public String encode() { return "E"+G.domE.indexOf(e); }
  @Override public int compareTo(Object _that) {
    QueryE that = (QueryE)_that;
    int a, b;
    a = G.domE.indexOf(this.e);
    b = G.domE.indexOf(that.e);
    if (a != b) return a < b ? -1 : +1;
    return 0;
  }
}

class QueryI implements Query {
  Quad i;
  QueryI(Quad i) { this.i = i; }
  @Override public int hashCode() { return i.hashCode(); }
  @Override public boolean equals(Object _that) { return i.equals(((QueryI)_that).i); }
  public void addToRel(ProgramRel rel) { rel.add(i); }
  @Override public String toString() { return G.istr(i); }
  public String encode() { return "I"+G.domI.indexOf(i); }
  @Override public int compareTo(Object _that) {
    QueryI that = (QueryI)_that;
    int a, b;
    a = G.domI.indexOf(this.i);
    b = G.domI.indexOf(that.i);
    if (a != b) return a < b ? -1 : +1;
    return 0;
  }
}

class QueryP implements Query {
  Quad p;
  QueryP(Quad p) { this.p = p; }
  @Override public int hashCode() { return p.hashCode(); }
  @Override public boolean equals(Object _that) { return p.equals(((QueryP)_that).p); }
  public void addToRel(ProgramRel rel) { rel.add(p); }
  @Override public String toString() { return G.pstr(p); }
  public String encode() { return "P"+G.domP.indexOf(p); }
  @Override public int compareTo(Object _that) {
    QueryP that = (QueryP)_that;
    int a, b;
    a = G.domE.indexOf(this.p);
    b = G.domE.indexOf(that.p);
    if (a != b) return a < b ? -1 : +1;
    return 0;
  }
}

////////////////////////////////////////////////////////////

// Current status
class Status {
  int numUnproven;
  int absSize;
  int runAbsSize;
  int absHashCode;
  long clientTime;
  long relevantTime;
  String absSummary;
}

// An abstraction actually stores the set of slivers (abstract values), and through this the k value.
// TypeStrategy specifies the abstraction function (minus the k value).
class Abstraction {
  Execution X = Execution.v();

  @Override public int hashCode() { return S.hashCode(); }
  @Override public boolean equals(Object _that) {
    Abstraction that = (Abstraction)_that;
    return S.equals(that.S);
  }

  Set<Ctxt> getSlivers() { return S; }

  void add(Ctxt c) { S.add(c); }
  void add(Abstraction that) { S.addAll(that.S); }
  boolean contains(Ctxt c) { return S.contains(c); }

  Histogram lengthHistogram(Set<Ctxt> slivers) {
    Histogram hist = new Histogram();
    for (Ctxt c : slivers) hist.counts[G.len(c)]++;
    return hist;
  }

  // Extract the sites which are not pruned
  Set<Quad> inducedHeadSites() {
    Set<Quad> set = new HashSet<Quad>();
    for (Ctxt c : S)
      if (G.hasHeadSite(c))
        set.add(c.head());
    return set;
  }

  void printKValues() {
    int[] h_maxLen = new int[G.domH.size()];
    int[] i_maxLen = new int[G.domI.size()];
    for (Ctxt c : S) {
      if (!G.hasHeadSite(c)) continue;
      int len = G.len(c);
      if (G.isAlloc(c.head())) {
        int h = G.domH.indexOf(c.head());
        h_maxLen[h] = Math.max(h_maxLen[h], len);
      }
      else {
        int i = G.domI.indexOf(c.head());
        i_maxLen[i] = Math.max(i_maxLen[i], len);
      }
    }

    int size = 0;
    for (int k = 0; k < 4; k++) {
      int n = 0;
      for (int h = 0; h < G.domH.size(); h++) {
        if (!G.jSet.contains(G.domH.get(h))) continue;
        if (h_maxLen[h] == k) n++; 
      }
      X.logs("KVALUE H %s %s", k, n);
      size += k*n;
      n = 0;
      for (int i = 0; i < G.domI.size(); i++) {
        if (!G.jSet.contains(G.domI.get(i))) continue;
        if (i_maxLen[i] == k) n++; 
      }
      X.logs("KVALUE I %s %s", k, n);
      size += k*n;
    }
    X.logs("KVALUE SIZE %s", size);
  }

  boolean lastElementRepeated(Ctxt c) {
    assert G.isSummary(c);
    int len = G.summaryLen(c);
    if (len <= 1) return false;
    Quad j = c.get(len-1); // Last element
    for (int k = len-2; k >= 0; k--)
      if (c.get(k) == j) return true; // Found another copy
    return false;
  }

  // assertDisjoint: this is when we are pruning slivers (make sure never step on each other's toes)
  // This should really not have typeStrategy in here (more general than useful)
  // REFINE
  void addRefinements(Ctxt c, int depth, TypeStrategy typeStrategy) {
    assert depth >= 0;
    if (S.contains(c)) return;
    if (depth == 0) // No refinement
      S.add(c);
    else if (typeStrategy.disallowRepeats && lastElementRepeated(c))
      S.add(c);
    else {
      assert G.isSummary(c); // Can only refine summarizes
      Ctxt d = G.atomize(c);
      Collection<Quad> extensions;
      if (G.summaryLen(c) == 0) // [*] = {[i,*] : i in I} by definition
        extensions = G.iSet;
      else { // c = [... k *]; consider all possible ways k can be extended
        List<Quad> ks = typeStrategy.lift(d.last()); // Actually with types, might be several values of k (need to consider them all)
        assert ks.size() > 0;
        if (ks.size() == 1) // Just for efficiency, don't create a new array
          extensions = G.rev_jm.get(ks.get(0).getMethod());
        else {
          extensions = new ArrayList();
          for (Quad k : ks) extensions.addAll(G.rev_jm.get(k.getMethod()));
        }
      }

      extensions = typeStrategy.usePrototypes(extensions); // Apply coarsening

      addRefinements(d, 0, typeStrategy);
      for (Quad j : extensions)
        addRefinements(G.summarize(d.append(j)), depth-1, typeStrategy);
    }
  }

  // c could either be a summary or atom, which means the output could technically be a set, but make sure this doesn't happen.
  Ctxt project(Ctxt c, TypeStrategy typeStrategy) {
    if (typeStrategy.disallowRepeats) {
      // EXTEND
      // ASSUMPTION: without the first element, c is barely-repeating, so we just need to check the first element
      // First truncate to eliminate repeats if any
      int k = 0;
      int len = G.len(c);
      for (k = 1; k < len; k++) // Find k, second occurrence of first element (if any)
        if (c.get(0) == c.get(k)) break;
      if (k < len) // Truncate (include position k)
        c = G.summarize(c.prefix(k+1));
    }

    if (G.isAtom(c)) { // atom
      if (S.contains(c)) return c; // Exact match
      // Assume there's at most one that matches
      for (int k = G.atomLen(c); k >= 0; k--) { // Take length k prefix
        Ctxt d = G.summarize(c.prefix(k));
        if (S.contains(d)) return d;
      }
      return null;
    }
    else { // summary
      // If we project ab* (by prepending a to b*) onto S={ab,abc*,abd*,...}, we should return all 3 values.
      // Generally, take every sliver that starts with ab, summary or not.
      // TODO: we don't handle this case, which is okay if all the longest summary slivers differ by length at most one.
      { // Match ab?
        Ctxt d = G.atomize(c);
        if (S.contains(d)) return d;
      }
      for (int k = G.summaryLen(c); k >= 0; k--) { // Take length k prefix (consider {ab*, a*, *}, exactly one should match)
        Ctxt d = G.summarize(c.prefix(k));
        if (S.contains(d)) return d;
      }
      return null;
    }
  }

  // Need this assumption if we're going to prune!
  private void assertNotExists(Ctxt c, Ctxt cc) {
    assert !S.contains(cc) : G.cstr(c) + " exists, but subsumed by coarser " + G.cstr(cc);
  }
  void assertDisjoint() {
    // Make sure each summary sliver doesn't contain proper summary prefixes
    for (Ctxt c : S) {
      if (G.hasHeadSite(c) && !G.isAlloc(c.head())) // if [i...] exists, [*] cannot exist
        assertNotExists(c, G.summarize(G.emptyCtxt));

      if (G.isAtom(c))
        assertNotExists(c, G.summarize(c));
      else {
        for (int k = G.summaryLen(c)-1; k >= 1; k--) // if xy* exists, x* can't exist
          assertNotExists(c, G.summarize(c.prefix(k)));
      }
    }
  }

  @Override public String toString() {
    int numSummaries = 0;
    for (Ctxt c : S) if (G.isSummary(c)) numSummaries++;
    return String.format("%s(%s)%s", S.size(), numSummaries, lengthHistogram(S));
  }
  int size() { return S.size(); }

  private HashSet<Ctxt> S = new HashSet<Ctxt>(); // The set of slivers
}

////////////////////////////////////////////////////////////

interface BlackBox {
  public String apply(String line);
}

@Chord(
  name = "sliver-ctxts-java",
  produces = { "C", "CC", "CH", "CI", "objI", "kcfaSenM", "kobjSenM", "ctxtCpyM" },
  namesOfTypes = { "C" },
  types = { DomC.class }
)
public class SliverCtxtsAnalysis extends JavaAnalysis implements BlackBox {
  private Execution X;

  // Options
  int verbose;
  int maxIters;
  boolean useObjectSensitivity;
  boolean inspectTransRels;
  boolean verifyAfterPrune;
  boolean pruneSlivers, refineSites;
  boolean useCtxtsAnalysis;
  String queryType; // EE or E or P or I
  boolean disallowRepeats;
  TypeStrategy typeStrategy; // Types instead of allocation sites
  TypeStrategy pruningTypeStrategy; // Use this to prune

  String masterHost;
  int masterPort;
  String mode; // worker or master or null
  boolean minimizeAbstraction; // Find the minimal abstraction via repeated calls
  int minH, maxH, minI, maxI;
  List<String> initTasks = new ArrayList<String>();
  List<String> tasks = new ArrayList<String>();
  String relevantTask;
  String transTask;

  // Compute once using 0-CFA
  Set<Quad> hSet = new HashSet<Quad>();
  Set<Quad> iSet = new HashSet<Quad>();
  Set<Quad> jSet = new HashSet<Quad>(); // hSet union iSet
  HashMap<jq_Method,List<Quad>> rev_jm = new HashMap<jq_Method,List<Quad>>(); // method m -> sites that be the prefix of a context for m
  HashMap<Quad,List<jq_Method>> jm = new HashMap<Quad,List<jq_Method>>(); // site to methods
  HashMap<jq_Method,List<Quad>> mj = new HashMap<jq_Method,List<Quad>>(); // method to sites

  List<Query> allQueries = new ArrayList<Query>();
  List<Status> statuses = new ArrayList<Status>(); // Status of the analysis over iterations of refinement
  QueryGroup unprovenGroup = new QueryGroup();
  List<QueryGroup> provenGroups = new ArrayList<QueryGroup>();

  int maxRunAbsSize = -1;
  int lastRunAbsSize = -1;
  long lastClientTime;
  long lastRelevantTime;

  boolean isMaster() { return mode != null && mode.equals("master"); }
  boolean isWorker() { return mode != null && mode.equals("worker"); }

  ////////////////////////////////////////////////////////////

  // Initialization to do anything.
  private void init() {
    X = Execution.v();

    G = new GlobalInfo();
    G.rev_jm = rev_jm;
    G.hSet = hSet;
    G.iSet = iSet;
    G.jSet = jSet;

    this.verbose                 = X.getIntArg("verbose", 0);
    this.maxIters                = X.getIntArg("maxIters", 1);
    this.useObjectSensitivity    = X.getBooleanArg("useObjectSensitivity", false);
    this.inspectTransRels        = X.getBooleanArg("inspectTransRels", false);
    this.verifyAfterPrune        = X.getBooleanArg("verifyAfterPrune", false);
    this.pruneSlivers            = X.getBooleanArg("pruneSlivers", false);
    this.refineSites             = X.getBooleanArg("refineSites", false);
    this.useCtxtsAnalysis        = X.getBooleanArg("useCtxtsAnalysis", false);
    this.queryType               = X.getStringArg("queryType", null);
    this.disallowRepeats         = X.getBooleanArg("disallowRepeats", false);
    this.typeStrategy            = new TypeStrategy(X.getStringArg("typeStrategy", "identity"), disallowRepeats);

    if (X.getStringArg("pruningTypeStrategy", null) != null)
      this.pruningTypeStrategy   = new TypeStrategy(X.getStringArg("pruningTypeStrategy", null), disallowRepeats);

    this.masterHost              = X.getStringArg("masterHost", null);
    this.masterPort              = X.getIntArg("masterPort", 8888);
    this.mode                    = X.getStringArg("mode", null);
    this.minimizeAbstraction     = X.getBooleanArg("minimizeAbstraction", false);

    this.minH = X.getIntArg("minH", 1);
    this.maxH = X.getIntArg("maxH", 2);
    this.minI = X.getIntArg("minI", 1);
    this.maxI = X.getIntArg("maxI", 2);

    this.initTasks.add("sliver-init-dlog");
    for (String name : X.getStringArg("initTaskNames", "").split(","))
      this.initTasks.add(name);
    for (String name : X.getStringArg("taskNames", "").split(","))
      this.tasks.add(name);
    this.relevantTask = X.getStringArg("relevantTaskName", null);
    this.transTask = X.getStringArg("transTaskName", null);

    X.putOption("version", 1);
    X.putOption("program", System.getProperty("chord.work.dir"));
    X.flushOptions();

    // Initialization Datalog programs
    for (String task : initTasks)
      ClassicProject.g().runTask(task);

    // Reachable things
    {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("reachableH"); rel.load();
      Iterable<Quad> result = rel.getAry1ValTuples();
      for (Quad h : result) hSet.add(h);
      rel.close();
    }
    {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("reachableI"); rel.load();
      Iterable<Quad> result = rel.getAry1ValTuples();
      for (Quad i : result) iSet.add(i);
      rel.close();
    }
    X.logs("Finished 0-CFA: |hSet| = %s, |iSet| = %s", hSet.size(), iSet.size());
    if (useObjectSensitivity) iSet.clear(); // Don't need call sites
    jSet.addAll(hSet);
    jSet.addAll(iSet);

    // Allocate memory
    for (Quad h : hSet) jm.put(h, new ArrayList<jq_Method>());
    for (Quad i : iSet) jm.put(i, new ArrayList<jq_Method>());
    for (jq_Method m : G.domM) mj.put(m, new ArrayList<Quad>());
    for (jq_Method m : G.domM) rev_jm.put(m, new ArrayList<Quad>());

    // Extensions of sites depends on the target method
    if (!useObjectSensitivity) {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("IM"); rel.load();
      PairIterable<Quad,jq_Method> result = rel.getAry2ValTuples();
      for (Pair<Quad,jq_Method> pair : result) {
        Quad i = pair.val0;
        jq_Method m = pair.val1;
        assert iSet.contains(i) : G.istr(i);
        jm.get(i).add(m);
        rev_jm.get(m).add(i);
      }
      rel.close();
    }
    else {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("HtoM"); rel.load();
      PairIterable<Quad,jq_Method> result = rel.getAry2ValTuples();
      for (Pair<Quad,jq_Method> pair : result) {
        Quad h = pair.val0;
        jq_Method m = pair.val1;
        assert hSet.contains(h) : G.hstr(h);
        jm.get(h).add(m);
        rev_jm.get(m).add(h);
      }
      rel.close();
    }

    // Sites contained in a method
    if (!useObjectSensitivity) {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("MI"); rel.load();
      PairIterable<jq_Method,Quad> result = rel.getAry2ValTuples();
      for (Pair<jq_Method,Quad> pair : result) {
        jq_Method m = pair.val0;
        Quad i = pair.val1;
        mj.get(m).add(i);
      }
      rel.close();
    }
    { // Note: both allocation and call sites need this
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("MH"); rel.load();
      PairIterable<jq_Method,Quad> result = rel.getAry2ValTuples();
      for (Pair<jq_Method,Quad> pair : result) {
        jq_Method m = pair.val0;
        Quad h = pair.val1;
        mj.get(m).add(h);
      }
      rel.close();
    }

    // Init type strategies
    typeStrategy.init();
    if (pruningTypeStrategy != null)
      pruningTypeStrategy.init();

    // Compute statistics on prependings (for analysis) and extensions (for refinement)
    { // prepends
      Histogram hist = new Histogram();
      for (Quad j : jSet) {
        int n = 0;
        for (jq_Method m : jm.get(j))
          n += mj.get(m).size();
        hist.add(n);
      }
      X.logs("For analysis (building CH,CI,CC): # prependings of sites: %s", hist);
    }
    { // extensions
      Histogram hist = new Histogram();
      for (Quad j : jSet)
        hist.add(rev_jm.get(j.getMethod()).size());
      X.logs("For refinement (growing slivers): # extensions of sites: %s", hist);
    }

    // Compute which queries we should answer in the whole program
    String focus = X.getStringArg("focusQuery", null);
    if (focus != null) {
      throw new RuntimeException("Not supported");
      /*String[] tokens = focus.split(",");
      Quad e1 = G.domE.get(Integer.parseInt(tokens[0]));
      Quad e2 = G.domE.get(Integer.parseInt(tokens[1]));
      allQueries.add(new Query(e1, e2));*/
    }
    else {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("query"+queryType); rel.load();
      readQueries(rel, allQueries);
      rel.close();

      int maxQueries = X.getIntArg("maxQueries", allQueries.size());
      
      int seed = X.getIntArg("randQuery", -1);
      if (seed != -1) {
        X.logs("Using %s/%s random queries (seed %s)", maxQueries, allQueries.size(), seed);
        Random rand = new Random(seed);
        List<Query> queries = new ArrayList();
        int[] perm = Utils.samplePermutation(rand, allQueries.size()); 
        for (int i = 0; i < maxQueries; i++)
          queries.add(allQueries.get(perm[i]));
        allQueries = queries;
      }
      else {
        List<Query> queries = new ArrayList();
        for (int i = 0; i < maxQueries; i++)
          queries.add(allQueries.get(i));
        allQueries = queries;
      }
    }
    sortQueries(allQueries);
    X.logs("Starting with %s total queries", allQueries.size());
    outputQueries(allQueries, "initial.queries");

    X.flushOptions();
  }

  void finish() { X.finish(null); }

  public void run() {
    init();
    if (isWorker()) runWorker();
    else if (minimizeAbstraction) {
      Client client = new AbstractionMinimizer(allQueries, jSet, minH, maxH, minI, maxI, this);
      new Master(masterPort, client);
    }
    else refinePruneLoop();
    finish();
  }

  String callMaster(String line) {
    try {
      Socket master = new Socket(masterHost, masterPort);
      BufferedReader in = G.getIn(master);
      PrintWriter out = G.getOut(master);
      out.println(line);
      out.flush();
      line = in.readLine();
      in.close();
      out.close();
      master.close();
      return line;
    } catch (IOException e) {
      return null;
    }
  }

  void readQueries(ProgramRel rel, Collection<Query> queries) {
    if (queryType.equals("P")) {
      Iterable<Quad> result = rel.getAry1ValTuples();
      for (Quad p : result) queries.add(new QueryP(p));
    }
    else if (queryType.equals("I")) {
      Iterable<Quad> result = rel.getAry1ValTuples();
      for (Quad i : result) queries.add(new QueryI(i));
    }
    else if (queryType.equals("E")) {
      Iterable<Quad> result = rel.getAry1ValTuples();
      for (Quad e : result) queries.add(new QueryE(e));
    }
    else if (queryType.equals("EE")) {
      PairIterable<Quad,Quad> result = rel.getAry2ValTuples();
      for (Pair<Quad,Quad> pair : result) queries.add(new QueryEE(pair.val0, pair.val1));
    }
    else
      throw new RuntimeException("Unknown queryType: "+queryType);
  }

  public String apply(String line) {
    // Hijack and call CtxtsAnalysis
    if (useCtxtsAnalysis) {
      X.logs("Setting global_kobjValue and global_kcfaValue");
      Set<Query> queries = new HashSet();
      HashMap<Quad,Integer> lengths = decodeAbstractionQueries(line, null, queries);
//      int[] kobjValue = CtxtsAnalysis.global_kobjValue = new int[G.domH.size()];
//      int[] kcfaValue = CtxtsAnalysis.global_kcfaValue = new int[G.domI.size()];
      int[] kobjValue = new int[G.domH.size()];
      int[] kcfaValue = new int[G.domI.size()];
      for (Quad q : lengths.keySet()) {
        if (G.isAlloc(q))
          kobjValue[G.domH.indexOf(q)] = lengths.get(q);
        else
          kcfaValue[G.domI.indexOf(q)] = lengths.get(q);
      }
      ClassicProject.g().resetTaskDone("ctxts-java");
      ClassicProject.g().runTask("ctxts-java");

      ProgramRel relInQuery = (ProgramRel) ClassicProject.g().getTrgt("inQuery"+queryType);
      relInQuery.zero();
      for (Query q : queries) q.addToRel(relInQuery);
      relInQuery.save();

      for (String task : tasks)
        ClassicProject.g().runTask(task);

      Set<Query> unproven = new HashSet<Query>();
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("outQuery"+queryType); rel.load();
      readQueries(rel, unproven);
      rel.close();
      return encodeQueries(unproven);
    }

    QueryGroup group = new QueryGroup();
    decodeAbstractionQueries(line, group.abs, group.queries);
    /*boolean fastWrongHack = false;
    if (fastWrongHack) {
      // Fast and wrong hack (for testing master/worker connection):
      // Prove a query if we've given context sensitivity to an allocation site in the containing method.
      Set<jq_Method> methods = new HashSet(); // Good methods
      for (Ctxt c : group.abs.getSlivers()) {
        if (G.isSummary(c) && G.summaryLen(c) > (G.isAlloc(c.head()) ? minH : minI))
          methods.add(c.head().getMethod());
      }
      Set<Query> outQueries = new HashSet();
      for (Query q : group.queries)
        if (!methods.contains(q.e1.getMethod()) && !methods.contains(q.e2.getMethod()))
          outQueries.add(q);
      return encodeQueries(outQueries);
    }*/
    group.runAnalysis(false);
    group.removeProvenQueries();
    return encodeQueries(group.queries);
  }

  void runWorker() {
    X.logs("Starting worker...");
    int numJobs = 0;
    while (true) {
      X.logs("============================================================");
      // Get a job
      String line = callMaster("GET");
      if (line == null) { X.logs("Got null, something bad happened to master..."); G.sleep(5); }
      else if (line.equals("WAIT")) { X.logs("Waiting..."); G.sleep(5); X.putOutput("exec.status", "waiting"); X.flushOutput(); }
      else if (line.equals("EXIT")) { X.logs("Exiting..."); break; }
      else {
        X.putOutput("exec.status", "running"); X.flushOutput();

        String[] tokens = line.split(" ", 2);
        String id = tokens[0];
        String input = tokens[1];
        String output = apply(input);
        line = callMaster("PUT "+id+" "+output);
        X.logs("Sent result to master, got reply: %s", line);
        numJobs++;

        X.putOutput("numJobs", numJobs); X.flushOutput();
      }
    }
  }

  // Format: H*:1 I*:0 H3:5 I6:1 ## E2,4 (things not shown are 0)
  HashMap<Quad,Integer> decodeAbstractionQueries(String line, Abstraction abs, Set<Query> queries) {
    HashMap<Quad,Integer> lengths = new HashMap();
    int minH = 0;
    int minI = 0;
    boolean includeAllQueries = true;
    assert queryType.equals("EE");
    //X.logs("DECODE %s", line);
    for (String s : line.split(" ")) {
      if (s.charAt(0) == 'E') {
        String[] tokens = s.substring(1).split(",");
        Quad e1 = G.domE.get(Integer.parseInt(tokens[0]));
        Quad e2 = G.domE.get(Integer.parseInt(tokens[1]));
        queries.add(new QueryEE(e1, e2));
      }
      else if (s.charAt(0) == 'H' || s.charAt(0) == 'I') {
        String[] tokens = s.substring(1).split(":");
        int len = Integer.parseInt(tokens[1]);
        if (tokens[0].equals("*")) {
          if (s.charAt(0) == 'H') minH = len;
          else if (s.charAt(0) == 'I') minI = len;
          else throw new RuntimeException(s);
        }
        else {
          int n = Integer.parseInt(tokens[0]);
          Quad q = null;
          if (s.charAt(0) == 'H') q = (Quad)G.domH.get(n);
          else if (s.charAt(0) == 'I') q = G.domI.get(n);
          else throw new RuntimeException(s);
          assert jSet.contains(q);
          lengths.put(q, len);
        }
      }
      else if (s.equals("##"))
        includeAllQueries = false;
      else throw new RuntimeException("Bad: " + line);
    }
    for (Quad q : jSet)
      if (!lengths.containsKey(q))
        lengths.put(q, G.isAlloc(q) ? minH : minI);
    assert lengths.size() == jSet.size();

    if (includeAllQueries) queries.addAll(allQueries);
    X.logs("decodeAbstractionQueries: got minH=%s, minI=%s, %s queries", minH, minI, queries.size());

    if (abs != null) {
      // Initialize abstraction (CREATE)
      abs.add(G.initEmptyCtxt(minI));
      for (Quad q : lengths.keySet()) {
        int len = lengths.get(q);
        if (len > 0)
          abs.addRefinements(G.summarize(G.emptyCtxt.append(typeStrategy.project(q))), len-1, typeStrategy);
      }
    }
    return lengths;
  }

  String encodeQueries(Set<Query> queries) {
    X.logs("encodeQueries: |Y|=%s", queries.size());
    StringBuilder buf = new StringBuilder();
    for (Query q : queries) {
      if (buf.length() > 0) buf.append(' ');
      buf.append(q.encode());
    }
    return buf.toString();
  }

  void refinePruneLoop() {
    X.logs("Initializing abstraction with length minH=%s,minI=%s slivers (|jSet|=%s)", minH, minI, jSet.size());
    // Initialize abstraction (CREATE)
    unprovenGroup.abs.add(G.initEmptyCtxt(minI));
    for (Quad j : jSet) {
      int len = G.isAlloc(j) ? minH : minI;
      if (len > 0)
        unprovenGroup.abs.addRefinements(G.summarize(G.emptyCtxt.append(typeStrategy.project(j))), len-1, typeStrategy);
    }

    X.logs("Unproven group with %s queries", allQueries.size());
    unprovenGroup.queries.addAll(allQueries);

    for (int iter = 1; ; iter++) {
      X.logs("====== Iteration %s", iter);
      boolean runRelevantAnalysis = iter < maxIters && (pruneSlivers || refineSites);
      unprovenGroup.runAnalysis(runRelevantAnalysis);
      backupRelations(iter);
      if (inspectTransRels) unprovenGroup.inspectAnalysisOutput();
      QueryGroup provenGroup = unprovenGroup.removeProvenQueries();
      if (provenGroup != null) provenGroups.add(provenGroup);

      if (pruneSlivers && runRelevantAnalysis) {
        unprovenGroup.pruneAbstraction();
        if (verifyAfterPrune && provenGroup != null) {
          X.logs("verifyAfterPrune");
          // Make sure this is a complete abstraction
          assert provenGroup.abs.inducedHeadSites().equals(jSet);
          provenGroup.runAnalysis(runRelevantAnalysis);
        }
      }

      outputStatus(iter);

      if (statuses.get(statuses.size()-1).numUnproven == 0) {
        X.logs("Proven all queries, exiting...");
        X.putOutput("conclusion", "prove");
        break;
      }
      if (iter == maxIters) {
        X.logs("Reached maximum number of iterations, exiting...");
        X.putOutput("conclusion", "max");
        break;
      }
      if (converged()) {
        X.logs("Refinement converged, exiting...");
        X.putOutput("conclusion", "conv");
        break;
      }

      refineAbstraction();
    }
  }

  void refineAbstraction() {
    unprovenGroup.refineAbstraction();

    if (pruningTypeStrategy == null) return;

    X.logs("==== Using type strategy %s to prune", pruningTypeStrategy);
    // Use pruningTypeStrategy to project unprovenGroup.abs onto a helper abstraction
    QueryGroup helperGroup = new QueryGroup();
    helperGroup.prefix = "  helper: ";
    helperGroup.queries.addAll(unprovenGroup.queries);
    assert typeStrategy.isIdentity();
    for (Ctxt c : unprovenGroup.abs.getSlivers()) {
      Ctxt cc = pruningTypeStrategy.project(c);
      //X.logs("HELPER %s -> %s", G.cstr(c), G.cstr(cc));
      helperGroup.abs.add(cc);
    }
    helperGroup.abs.assertDisjoint();
    X.logs("  projected original %s to helper %s", unprovenGroup.abs, helperGroup.abs);

    // Run the analysis using the helper abstraction
    TypeStrategy saveTypeStrategy = typeStrategy;
    typeStrategy = pruningTypeStrategy;
    helperGroup.runAnalysis(true);
    helperGroup.removeProvenQueries(); // See how many we can prove
    helperGroup.pruneAbstraction();
    typeStrategy = saveTypeStrategy;
  
    // Refine original and use helper abstraction to prune
    Abstraction prunedAbs = new Abstraction();
    for (Ctxt c : unprovenGroup.abs.getSlivers())
      if (helperGroup.abs.contains(pruningTypeStrategy.project(c)))
        prunedAbs.add(c);
    X.logs("  helper pruned original from %s to %s", unprovenGroup.abs, prunedAbs);
    unprovenGroup.abs = prunedAbs;
  }

  int numUnproven() {
    int n = 0;
    for (QueryGroup g : provenGroups)
      n += g.queries.size();
    return allQueries.size()-n;
  }

  int[] parseIntArray(String s) {
    String[] tokens = s.split(",");
    int[] l = new int[tokens.length];
    for (int i = 0; i < l.length; i++)
      l[i] = Integer.parseInt(tokens[i]);
    return l;
  }

  int relSize(String name) {
    ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt(name); rel.load();
    int n = rel.size();
    rel.close();
    return n;
  }

  boolean converged() {
    if (statuses.size() < 2) return false;
    Status a = statuses.get(statuses.size()-2);
    Status b = statuses.get(statuses.size()-1);
    return a.absHashCode == b.absHashCode;
  }

  void outputStatus(int iter) {
    X.logs("outputStatus(iter=%s)", iter);
   
    X.addSaveFiles("abstraction.S."+iter);
    {
      PrintWriter out = OutDirUtils.newPrintWriter("abstraction.S."+iter);
      for (Ctxt a : sortSlivers(new ArrayList(unprovenGroup.abs.getSlivers())))
        out.println(G.cstr(a));
      out.close();
    }
    X.addSaveFiles("unproven.queries."+iter);
    outputQueries(sortQueries(new ArrayList(unprovenGroup.queries)), "unproven.queries."+iter);

    int numUnproven = numUnproven();
    Status status = new Status();
    status.numUnproven = numUnproven;
    status.runAbsSize = lastRunAbsSize; // Before pruning (real measure of complexity)
    status.absSize = unprovenGroup.abs.size(); // After pruning
    status.absHashCode = unprovenGroup.abs.hashCode();
    status.absSummary = unprovenGroup.abs.toString();
    status.clientTime = lastClientTime;
    status.relevantTime = lastRelevantTime;
    statuses.add(status);

    if (refineSites) unprovenGroup.abs.printKValues();

    X.putOutput("currIter", iter);
    X.putOutput("maxRunAbsSize", maxRunAbsSize);
    X.putOutput("lastRunAbsSize", lastRunAbsSize);
    X.putOutput("numQueries", allQueries.size());
    X.putOutput("numProven", allQueries.size()-numUnproven);
    X.putOutput("numUnproven", numUnproven);
    X.putOutput("numUnprovenHistory", getHistory("numUnproven"));
    X.putOutput("runAbsSizeHistory", getHistory("runAbsSize"));
    X.putOutput("clientTimeHistory", getHistory("clientTime"));
    X.putOutput("relevantTimeHistory", getHistory("relevantTime"));
    X.flushOutput();
  }

  String getHistory(String field) {
    StringBuilder buf = new StringBuilder();
    for (Status s : statuses) {
      if (buf.length() > 0) buf.append(',');
      Object value;
      if (field.equals("numUnproven")) value = s.numUnproven;
      else if (field.equals("runAbsSize")) value = s.runAbsSize;
      else if (field.equals("clientTime")) value = new StopWatch(s.clientTime);
      else if (field.equals("relevantTime")) value = new StopWatch(s.relevantTime);
      else throw new RuntimeException("Unknown field: " + field);
      buf.append(value);
    }
    return buf.toString();
  }

  void outputQueries(List<Query> queries, String path) {
    PrintWriter out = OutDirUtils.newPrintWriter(path);
    for (Query q : queries)
      out.println(q.encode()+" "+q);
    out.close();
  }

  List<Query> sortQueries(List<Query> queries) {
    Collections.sort(queries);
    return queries;
  }

  List<Ctxt> sortSlivers(List<Ctxt> slivers) {
    Collections.sort(slivers, new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            Ctxt c1 = (Ctxt) o1;
            Ctxt c2 = (Ctxt) o2;
            Quad[] elems1 = c1.getElems();
            Quad[] elems2 = c2.getElems();
            if (elems1.length == 0)
                return elems2.length == 0 ? 0 : -1;
            if (elems2.length == 0)
                return 1;
            return compare(elems1, elems2, 0);
        }
        private int compare(Quad[] elems1, Quad[] elems2, int i) {
           if (i == elems1.length) return (i == elems2.length) ? 0 : -1;
           if (i == elems2.length) return 1;
           Quad q1 = elems1[i];
           Quad q2 = elems2[i];
           if (q1 == q2)
                return compare(elems1, elems2, i + 1);
           if (q1 == null) return -1;
           if (q2 == null) return 1;
           Operator op1 = q1.getOperator();
           Operator op2 = q2.getOperator();
           if (op1 instanceof Invoke) {
              if (op2 instanceof Invoke) {
                  int i1 = G.domI.indexOf(q1);
                  int i2 = G.domI.indexOf(q2);
                  assert (i1 >= 0 && i2 >= 0);
                  return i1 < i2 ? -1 : 1;
              } else
                   return -1;
           } else {
               if (op2 instanceof Invoke)
                  return 1;
               else {
                  int h1 = G.domH.indexOf(q1);
                  int h2 = G.domH.indexOf(q2);
                  assert (h1 >= 0 && h2 >= 0);
                  return h1 < h2 ? -1 : 1;
               }
           }
        }
    });
    return slivers;
  }

  // Group of queries which have the same abstraction and be have the same as far as we can tell.
  class QueryGroup {
    String prefix = ""; // Just for printing out
    Set<Query> queries = new LinkedHashSet<Query>();
    // Invariant: abs + prunedAbs is a full abstraction and gets same results as abs
    Abstraction abs = new Abstraction(); // Current abstraction for this query
    Abstraction prunedAbs = new Abstraction(); // This abstraction keeps all the slivers that have been pruned

    void runAnalysis(boolean runRelevantAnalysis) {
      X.logs("%srunAnalysis: %s", prefix, abs);
      maxRunAbsSize = Math.max(maxRunAbsSize, abs.size());
      lastRunAbsSize = abs.size();

      // Domain (these are the slivers)
      DomC domC = (DomC) ClassicProject.g().getTrgt("C");
      domC.clear();
      assert abs.project(G.emptyCtxt, typeStrategy) != null;
      List<Ctxt> sortedC = new ArrayList<Ctxt>();
      for (Ctxt c : abs.getSlivers()) sortedC.add(c);
      sortSlivers(sortedC);
      domC.add(abs.project(G.emptyCtxt, typeStrategy));
      for (Ctxt c : sortedC) domC.add(c);
      domC.save();

      // Relations
      ProgramRel CH = (ProgramRel) ClassicProject.g().getTrgt("CH");
      ProgramRel CI = (ProgramRel) ClassicProject.g().getTrgt("CI");
      ProgramRel CC = (ProgramRel) ClassicProject.g().getTrgt("CC");
      CH.zero();
      CI.zero();
      CC.zero();
      for (Ctxt c : abs.getSlivers()) { // From sliver c...
        if (G.hasHeadSite(c)) {
          //X.logs("%s %s", G.jstr(c.head()), typeStrategy.clusters.size());
          for (Quad k : typeStrategy.lift(c.head())) // k is the actual starting site of a chain that c represents
            for (jq_Method m : jm.get(k))
              for (Quad j : mj.get(m)) // Extend with some site j that could be prepended
                addPrepending(j, c, CH, CI, CC);
        }
        else {
          for (Quad j : jSet) // Extend with any site j
            addPrepending(j, c, CH, CI, CC);
        }
      }
      CH.save();
      CI.save();
      CC.save();

      // Determine CFA or object-sensitivity
      ProgramRel relobjI = (ProgramRel) ClassicProject.g().getTrgt("objI");
      relobjI.zero();
      if (useObjectSensitivity) {
        for (Quad i : G.domI) relobjI.add(i);
      }
      relobjI.save();
      ProgramRel relKcfaSenM = (ProgramRel) ClassicProject.g().getTrgt("kcfaSenM");
      ProgramRel relKobjSenM = (ProgramRel) ClassicProject.g().getTrgt("kobjSenM");
      ProgramRel relCtxtCpyM = (ProgramRel) ClassicProject.g().getTrgt("ctxtCpyM");
      relKcfaSenM.zero();
      relKobjSenM.zero();
      relCtxtCpyM.zero();
      if (useObjectSensitivity) {
        for (jq_Method m : G.domM) {
          if (m.isStatic()) relCtxtCpyM.add(m);
          else              relKobjSenM.add(m);
        }
      }
      else {
        for (jq_Method m : G.domM) relKcfaSenM.add(m);
      }
      relKcfaSenM.save();
      relKobjSenM.save();
      relCtxtCpyM.save();

      ProgramRel relInQuery = (ProgramRel) ClassicProject.g().getTrgt("inQuery"+queryType);
      relInQuery.zero();
      for (Query q : queries) q.addToRel(relInQuery);
      relInQuery.save();

      ClassicProject.g().resetTrgtDone(domC); // Make everything that depends on domC undone
      ClassicProject.g().setTaskDone(SliverCtxtsAnalysis.this); // We are generating all this stuff, so mark it as done...
      ClassicProject.g().setTrgtDone(domC);
      ClassicProject.g().setTrgtDone(CH);
      ClassicProject.g().setTrgtDone(CI);
      ClassicProject.g().setTrgtDone(CC);
      ClassicProject.g().setTrgtDone(relobjI);
      ClassicProject.g().setTrgtDone(relKcfaSenM);
      ClassicProject.g().setTrgtDone(relKobjSenM);
      ClassicProject.g().setTrgtDone(relCtxtCpyM);
      ClassicProject.g().setTrgtDone(relInQuery);

      StopWatch watch = new StopWatch();
      watch.start();
      for (String task : tasks)
        ClassicProject.g().runTask(task);
      watch.stop();
      lastClientTime = watch.ms;

      if (runRelevantAnalysis) {
        watch.start();
        ClassicProject.g().runTask(relevantTask);
        watch.stop();
        lastRelevantTime = watch.ms;
      }

      if (inspectTransRels) ClassicProject.g().runTask(transTask);
    }

    void addPrepending(Quad j, Ctxt c, ProgramRel CH, ProgramRel CI, ProgramRel CC) {
      Quad jj = typeStrategy.project(j);
      Ctxt d = abs.project(c.prepend(jj), typeStrategy);
      if (!pruneSlivers) assert d != null;
      if (d != null) {
        //X.logs("PREPEND %s <- %s %s", G.cstr(d), G.jstr(j), G.cstr(c));
        (G.isAlloc(j) ? CH : CI).add(d, j);
        CC.add(c, d);
      }
    }

    Abstraction relevantAbs() {
      // From Datalog, read out the pruned abstraction
      Abstraction relevantAbs = new Abstraction(); // These are the slivers we keep
      relevantAbs.add(abs.project(G.emptyCtxt, typeStrategy)); // Always keep this, because it probably won't show up in CH or CI
      for (String relName : new String[] {"r_CH", "r_CI"}) {
        if (useObjectSensitivity && relName.equals("r_CI")) continue;
        ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt(relName); rel.load();
        PairIterable<Ctxt,Quad> result = rel.getAry2ValTuples();
        for (Pair<Ctxt,Quad> pair : result)
          relevantAbs.add(pair.val0);
        rel.close();
      }
      return relevantAbs;
    }

    void pruneAbstraction() {
      assert pruneSlivers;
      Abstraction newAbs = relevantAbs();

      // Record the pruned slivers (abs - newAbs)
      for (Ctxt c : abs.getSlivers())
        if (!newAbs.contains(c))
          prunedAbs.add(c);

      X.logs("%sSTATUS pruneAbstraction: %s -> %s", prefix, abs, newAbs);
      abs = newAbs;
      abs.assertDisjoint();
    }

    // Remove queries that have been proven
    QueryGroup removeProvenQueries() {
      // From Datalog, read out all unproven queries
      Set<Query> unproven = new HashSet<Query>();
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("outQuery"+queryType); rel.load();
      readQueries(rel, unproven);
      rel.close();

      // Put all proven queries in a new group
      QueryGroup provenGroup = new QueryGroup();
      provenGroup.abs.add(prunedAbs); // Build up complete abstraction
      provenGroup.abs.add(abs);
      assert abs.size()+prunedAbs.size() == provenGroup.abs.size(); // No duplicates
      for (Query q : queries)
        if (!unproven.contains(q))
          provenGroup.queries.add(q);
      for (Query q : provenGroup.queries)
        queries.remove(q);

      X.logs("%sSTATUS %s/%s queries unproven", prefix, queries.size(), allQueries.size());

      return provenGroup;
    }

    void inspectAnalysisOutput() {
      // Display the transition graph over relations
      try {
        RelGraph graph = new RelGraph();
        String dlogPath = ((DlogAnalysis)ClassicProject.g().getTask(transTask)).getFileName();
        X.logs("Reading transitions from "+dlogPath);
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(dlogPath)));
        String line;
        while ((line = in.readLine()) != null) {
          if (!line.startsWith("# TRANS")) continue;
          String[] tokens = line.split(" ");
          graph.loadTransition(tokens[2], tokens[3], tokens[4], tokens[5], parseIntArray(tokens[6]), parseIntArray(tokens[7]));
        }
        in.close();
        graph.display();

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    void refineAbstraction() {
      String oldAbsStr = abs.toString();
      Abstraction newAbs = new Abstraction();

      if (refineSites) {
        Set<Quad> relevantSites = relevantAbs().inducedHeadSites();
        X.logs("%s%s/%s sites relevant", prefix, relevantSites.size(), jSet.size());
        for (Ctxt c : abs.getSlivers()) { // For each sliver...
          if (G.isSummary(c) && (!G.hasHeadSite(c) || relevantSites.contains(c.head()))) // If have a relevant head site (or empty)
            newAbs.addRefinements(c, 1, typeStrategy);
          else
            newAbs.add(c); // Leave atomic ones alone (already precise as possible)
        }
      }
      else {
        for (Ctxt c : abs.getSlivers()) { // For each sliver
          if (G.isSummary(c))
            newAbs.addRefinements(c, 1, typeStrategy);
          else
            newAbs.add(c); // Leave atomic ones alone (already precise as possible)
        }
        newAbs.assertDisjoint();
      }

      abs = newAbs;
      String newAbsStr = abs.toString();

      assert !abs.getSlivers().contains(G.summarize(G.emptyCtxt));

      X.logs("%sSTATUS refineAbstraction: %s -> %s", prefix, oldAbsStr, newAbsStr);
    }
  }

  void backupRelations(int iter) {
    try {
      if (X.getBooleanArg("saveRelations", false)) {
        X.logs("backupRelations");
        String path = X.path(""+iter);
        new File(path).mkdir();

        DomC domC = (DomC) ClassicProject.g().getTrgt("C");
        domC.save(path, true);

        String[] names = new String[] { "CH", "CI", "CH", "inQuery", "outQuery" };
        for (String name : names) {
          X.logs("  Saving relation "+name);
          ProgramRel rel = (ProgramRel) ClassicProject.g().getTrgt(name);
          rel.load();
          rel.print(path);
          //rel.close(); // Crashes
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

class Histogram {
  int[] counts = new int[1000];
  void clear() {
    for (int i = 0; i < counts.length; i++)
      counts[i] = 0;
  }
  public void add(int i) {
    if (i >= counts.length) {
      int[] newCounts = new int[Math.max(counts.length*2,i+1)];
      System.arraycopy(counts, 0, newCounts, 0, counts.length);
      counts = newCounts;
    }
    counts[i]++;
  }
  public void add(Histogram h) {
    for (int i = 0; i < counts.length; i++)
      counts[i] += h.counts[i];
  }
  @Override public String toString() {
    StringBuilder buf = new StringBuilder();
    for (int n = 0; n < counts.length; n++) {
      if (counts[n] == 0) continue;
      if (buf.length() > 0) buf.append(" ");
      buf.append(n+":"+counts[n]);
    }
    return '['+buf.toString()+']';
  }
}

// For visualization
class RelNode {
  Execution X = Execution.v();
  List<Object> rel;
  List<RelNode> edges = new ArrayList<RelNode>();
  List<String> names = new ArrayList<String>();
  boolean visited;
  boolean root = true;

  RelNode(List<Object> rel) { this.rel = rel; }

  // Assume: if node visited iff children are also visited
  void clearVisited() {
    if (!visited) return;
    visited = false;
    for (RelNode node : edges)
      node.clearVisited();
  }

  String nameContrib(String name) { return name == null ? "" : "("+name+") "; }

  String extra() { return ""; }

  void display(String prefix, String parentName) {
    X.logs(prefix + extra() + nameContrib(parentName) + this + (edges.size() > 0 ? " {" : ""));
    String newPrefix = prefix+"  ";
    visited = true;
    for (int i = 0; i < edges.size(); i++) {
      RelNode node = edges.get(i);
      String name = names.get(i);
      if (node.visited) X.logs(newPrefix + node.extra() + nameContrib(name) + node + " ...");
      else node.display(newPrefix, name);
    }
    if (edges.size() > 0) X.logs(prefix+"}");
  }

  @Override public String toString() {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < rel.size(); i++) {
      b.append(G.render(rel.get(i)));
      if (i == 0) b.append('[');
      else if (i == rel.size()-1) b.append(']');
      else b.append(' ');
    }
    return b.toString();
  }
}

// Graph where nodes are relations r_X and edges are transitions t_X_Y
class RelGraph {
  Execution X = Execution.v();
  HashMap<List<Object>,RelNode> nodes = new HashMap<List<Object>,RelNode>();

  RelNode getNode(List<Object> rel) {
    RelNode node = nodes.get(rel);
    if (node == null)
      nodes.put(rel, node = new RelNode(rel));
    return node;
  }

  void add(List<Object> s, String name, List<Object> t) {
    RelNode node_s = getNode(s);
    RelNode node_t = getNode(t);
    //X.logs("EDGE | %s | %s", node_s, node_t);
    node_s.names.add(name);
    node_s.edges.add(node_t);
    node_t.root = false;
  }

  void display() {
    X.logs("===== GRAPH =====");
    for (RelNode node : nodes.values()) {
      if (node.root) {
        node.clearVisited();
        node.display("", null);
      }
    }
  }

  List<Object> buildRel(String relName, Object[] l, int[] indices) {
    List<Object> rel = new ArrayList<Object>();
    rel.add(relName);
    for (int i : indices) rel.add(l[i]);
    return rel;
  }

  void loadTransition(String name, String relName, String rel_s, String rel_t, int[] indices_s, int[] indices_t) {
    if (name.equals("-")) name = null;
    ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt(relName); rel.load();
    for (Object[] l : rel.getAryNValTuples()) {
      List<Object> s = buildRel(rel_s, l, indices_s);
      List<Object> t = buildRel(rel_t, l, indices_t);
      add(t, name, s); // Backwards
    }
    rel.close();
  }
}

////////////////////////////////////////////////////////////

class Scenario {
  //private static int newId = 0;
  //int id = newId++;

  private static Random random = new Random();
  int id = random.nextInt();

  String in, out;
  public Scenario(String line) {
    String[] tokens = line.split(" ## ");
    in = tokens[0];
    out = tokens[1];
  }
  public Scenario(String in, String out) { this.in = in; this.out = out; }
  @Override public String toString() { return in + " ## " + out; }
}

interface Client {
  Scenario createJob(); // Ask client for job
  void onJobResult(Scenario scenario); // Return job result
  boolean isDone();
  void saveState();
  int maxWorkersNeeded();
}

class Master {
  boolean shouldExit;
  Execution X = Execution.v();
  int port;

  HashMap<Integer,Scenario> inprogress = new HashMap<Integer,Scenario>();
  HashMap<String,Long> lastContact = new HashMap();
  Client client;

  final boolean waitForWorkersToExit = true;

  int numWorkers() { return lastContact.size(); }

  public Master(int port, Client client) {
    this.port = port;
    this.client = client;
    boolean exitFlag = false;

    X.logs("MASTER: listening at port %s", port);
    try {
      ServerSocket master = new ServerSocket(port);
      while (true) {
        if (exitFlag && (!waitForWorkersToExit || lastContact.size() == 0)) break;
        X.putOutput("numWorkers", numWorkers());
        X.flushOutput();
        X.logs("============================================================");
        boolean clientIsDone = client.isDone();
        if (clientIsDone) {
          if (!waitForWorkersToExit || lastContact.size() == 0) break;
          X.logs("Client is done but still waiting for %s workers to exit...", lastContact.size());
        }
        Socket worker = master.accept();
        String hostname = worker.getInetAddress().getHostAddress();
        BufferedReader in = G.getIn(worker);
        PrintWriter out = G.getOut(worker);

        X.logs("MASTER: Got connection from worker %s [hostname=%s]", worker, hostname);
        String cmd = in.readLine();
        if (cmd.equals("GET")) {
          lastContact.put(hostname, System.currentTimeMillis()); // Only add if it's getting stuff
          if (clientIsDone || numWorkers() > client.maxWorkersNeeded() + 1) { // 1 for extra buffer
            // If client is done or we have more workers than we need, then quit
            out.println("EXIT");
            lastContact.remove(hostname);
          }
          else {
            Scenario scenario = client.createJob();
            if (scenario == null) {
              X.logs("  No job, waiting (%s workers, %s workers needed)", numWorkers(), client.maxWorkersNeeded());
              out.println("WAIT");
            }
            else {
              inprogress.put(scenario.id, scenario);
              out.println(scenario.id + " " + scenario); // Response: <ID> <task spec>
              X.logs("  GET => id=%s", scenario.id);
            }
          }
        }
        else if (cmd.equals("CLEAR")) {
          lastContact.clear();
          out.println("Cleared workers");
        }
        else if (cmd.equals("SAVE")) {
          client.saveState();
          out.println("Saved");
        }
        else if (cmd.equals("EXIT")) {
          exitFlag = true;
          out.println("Going to exit...");
        }
        else if (cmd.equals("FLUSH")) {
          // Flush dead workers
          HashMap<String,Long> newLastContact = new HashMap();
          for (String name : lastContact.keySet()) {
            long t = lastContact.get(name);
            if (System.currentTimeMillis() - t < 60*60*1000)
              newLastContact.put(name, t);
          }
          lastContact = newLastContact;
          X.logs("%d workers", lastContact.size());
          out.println(lastContact.size()+" workers left");
        }
        else if (cmd.startsWith("PUT")) {
          String[] tokens = cmd.split(" ", 3); // PUT <ID> <task result>
          int id = Integer.parseInt(tokens[1]);
          Scenario scenario = inprogress.remove(id);
          if (scenario == null) {
            X.logs("  PUT id=%s, but doesn't exist", id);
            out.println("INVALID");
          }
          else {
            X.logs("  PUT id=%s", id);
            scenario.out = tokens[2];
            client.onJobResult(scenario);
            out.println("OK");
          }
        }

        in.close();
        out.close();
      }
      master.close();
      client.saveState();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

class AbstractionMinimizer implements Client {
  Execution EX = Execution.v();
  Random random = new Random();
  String defaults;
  
  boolean[] isAlloc;
  ArrayList<Quad> sites;
  IndexMap<String> components;
  int C;
  int[] bottomX, topX;
  HashMap<String,Query> y2queries;
  double initIncrProb = 0.5;
  double incrThetaStepSize = 0.1;
  int scanThreshold = 30;

  int numScenarios = 0; // Total number of calls to the analysis oracle
  List<Group> groups = new ArrayList();

  Set<String> allY() { return y2queries.keySet(); }

  public AbstractionMinimizer(List<Query> allQueries, Set<Quad> jSet,
      int minH, int maxH, int minI, int maxI, BlackBox box) {
    this.C = jSet.size();
    this.isAlloc = new boolean[C];
    this.sites = new ArrayList();
    this.components = new IndexMap();
    this.bottomX = new int[C];
    this.topX = new int[C];
    int c = 0;
    for (Quad q : jSet) {
      sites.add(q);
      if (G.isAlloc(q)) {
        components.add("H"+G.domH.indexOf(q));
        isAlloc[c] = true;
        bottomX[c] = minH;
        topX[c] = maxH;
      }
      else {
        components.add("I"+G.domI.indexOf(q));
        isAlloc[c] = false;
        bottomX[c] = minI;
        topX[c] = maxI;
      }
      c++;
    }
    this.defaults = "H*:"+minH + " " + "I*:"+minI;

    // Run the analysis twice to find the queries that differ between top and bottom
    Set<String> bottomY = decodeY(box.apply(encodeX(bottomX)));
    Set<String> topY = decodeY(box.apply(encodeX(topX)));
    EX.logs("bottom (kobj=%s,kcfa=%s) : %s/%s queries unproven", minH, minI, bottomY.size(), allQueries.size());
    EX.logs("top (kobj=%s,kcfa=%s) : %s/%s queries unproven", maxH, maxI, topY.size(), allQueries.size());

    this.y2queries = new HashMap();
    for (Query q : allQueries)
      y2queries.put(q.encode(), q);

    // Keep only queries that bottom was unable to prove but top was able to prove
    HashSet<String> Y = new HashSet();
    for (Query q : allQueries) {
      String y = q.encode();
      if (bottomY.contains(y) && !topY.contains(y)) // Unproven by bottom, proven by top
        Y.add(y);
      else
        y2queries.remove(y); // Don't need this
    }
    assert Y.size() == y2queries.size();
    EX.logs("|Y| = %s", Y.size());
    EX.putOutput("numY", Y.size());
    EX.putOutput("topComplexity", complexity(topX));

    groups.add(new Group(Y));

    outputStatus();
    loadGroups();
    loadScenarios();
  }

  void loadGroups() {
    String path = EX.path("groups");
    if (!new File(path).exists()) return;
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
      String line;
      groups.clear();
      while ((line = in.readLine()) != null) {
        // Format: <step size> ## <lower> ## <upper> ## queries
        String[] tokens = line.split(" ## ");
        Group g = new Group(decodeY(tokens[3]));
        g.incrTheta = invLogistic(Double.parseDouble(tokens[0]));
        g.lowerX = decodeX(tokens[1]);
        g.upperX = decodeX(tokens[2]);
        g.updateStatus();
        g.updateStatus();
        groups.add(g);
      }
      outputStatus();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  void loadScenarios() {
    String scenariosPath = EX.path("scenarios");
    if (!new File(scenariosPath).exists()) return;
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(scenariosPath)));
      String line;
      while ((line = in.readLine()) != null)
        incorporateScenario(new Scenario(line), false);
      in.close();
      outputStatus();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  String encodeX(int[] X) {
    StringBuilder buf = new StringBuilder();
    buf.append(defaults);
    for (int c = 0; c < C; c++) {
      if (X[c] == bottomX[c]) continue;
      buf.append(' ' + components.get(c) + ':' + X[c]);
    }
    return buf.toString();
  }
  String encodeY(Set<String> Y) {
    StringBuilder buf = new StringBuilder();
    for (String y : Y) {
      if (buf.length() > 0) buf.append(' ');
      buf.append(y);
    }
    return buf.toString();
  }
  int[] decodeX(String line) {
    int[] X = new int[C];
    for (int c = 0; c < C; c++) X[c] = -1;
    int minH = 0, minI = 0; // Defaults
    for (String s : line.split(" ")) {
      String[] tokens = s.split(":");
      assert tokens.length == 2 : s;
      int len = Integer.parseInt(tokens[1]);
      if (tokens[0].equals("H*")) minH = len;
      else if (tokens[0].equals("I*")) minI = len;
      else {
        int c = components.indexOf(tokens[0]);
        assert c != -1 : s;
        X[c] = len;
      }
    }
    // Fill in defaults
    for (int c = 0; c < C; c++)
      if (X[c] == -1)
        X[c] = isAlloc[c] ? minH : minI;
    return X;
  }
  HashSet<String> decodeY(String line) {
    HashSet<String> Y = new HashSet<String>();
    for (String y : line.split(" ")) Y.add(y);
    return Y;
  }

  // General utilities
  int complexity(int[] X) {
    int sum = 0;
    for (int c = 0; c < C; c++) {
      assert X[c] >= bottomX[c] : c + " " + X[c] + " " + bottomX[c];
      sum += X[c] - bottomX[c];
    }
    return sum;
  }
  int[] copy(int[] X) {
    int[] newX = new int[C];
    System.arraycopy(X, 0, newX, 0, C);
    return newX;
  }
  void set(int[] X1, int[] X2) { System.arraycopy(X2, 0, X1, 0, C); }
  boolean eq(int[] X1, int[] X2) {
    for (int c = 0; c < C; c++)
      if (X1[c] != X2[c]) return false;
    return true;
  }
  boolean lessThanEq(int[] X1, int[] X2) {
    for (int c = 0; c < C; c++)
      if (X1[c] > X2[c]) return false;
    return true;
  }
  int findUniqueDiff(int[] X1, int[] X2) {
    int diffc = -1;
    for (int c = 0; c < C; c++) {
      int d = Math.abs(X1[c]-X2[c]);
      if (d > 1) return -1; // Not allowed
      if (d == 1) {
        if (diffc != -1) return -1; // Can't have two diff
        diffc = c;
      }
    }
    return diffc;
  }

  double logistic(double theta) { return 1/(1+Math.exp(-theta)); }
  double invLogistic(double mu) { return Math.log(mu/(1-mu)); }

  class Group {
    boolean done;
    boolean scanning;
    int[] lowerX;
    int[] upperX;
    HashSet<String> Y; // Unproven 
    double incrTheta; // For the step size
    HashMap<Integer,Integer> jobCounts; // job ID -> number of jobs in the queue at the time when this job was created

    boolean inRange(int[] X) { return lessThanEq(lowerX, X) && lessThanEq(X, upperX); }

    @Override public String toString() {
      String status = done ? "done" : (scanning ? "scan" : "rand");
      return String.format("Group(%s,%s<=|X|<=%s,|Y|=%s,incrProb=%.2f,#wait=%s)",
        status, complexity(lowerX), complexity(upperX), Y.size(), logistic(incrTheta), jobCounts.size());
    }

    public Group(HashSet<String> Y) {
      this.done = false;
      this.scanning = false;
      this.lowerX = copy(bottomX);
      this.upperX = copy(topX);
      this.Y = Y;
      this.incrTheta = invLogistic(initIncrProb);
      this.jobCounts = new HashMap();
    }

    public Group(Group g, HashSet<String> Y) {
      this.done = g.done;
      this.scanning = g.scanning;
      this.lowerX = copy(g.lowerX);
      this.upperX = copy(g.upperX);
      this.Y = Y;
      this.incrTheta = g.incrTheta;
      this.jobCounts = new HashMap(g.jobCounts);
    }

    boolean wantToLaunchJob() {
      if (done) return false;
      if (scanning) return jobCounts.size() == 0; // Don't parallelize
      return true;
    }

    Scenario createNewScenario() {
      double incrProb = logistic(incrTheta);
      EX.logs("createNewScenario %s: incrProb=%.2f", this, incrProb);
      if (scanning) {
        if (jobCounts.size() == 0) { // This is sequential - don't waste energy parallelizing
          int diff = complexity(upperX) - complexity(lowerX);
          assert diff > 0 : diff;
          int target_j = random.nextInt(diff);
          EX.logs("Scanning: dipping target_j=%s of diff=%s", target_j, diff);
          // Sample a minimal dip from upperX
          int j = 0;
          int[] X = new int[C];
          for (int c = 0; c < C; c++) {
            X[c] = lowerX[c];
            for (int i = lowerX[c]; i < upperX[c]; i++, j++)
              if (j != target_j) X[c]++;
          }
          return createScenario(X);
        }
        else {
          EX.logs("Scanning: not starting new job, still waiting for %s (shouldn't happen)", jobCounts.keySet());
          return null;
        }
      }
      else {
        // Sample a random element between the upper and lower bounds
        int[] X = new int[C];
        for (int c = 0; c < C; c++) {
          X[c] = lowerX[c];
          for (int i = lowerX[c]; i < upperX[c]; i++)
            if (random.nextDouble() < incrProb) X[c]++;
        }
        if (!eq(X, lowerX) && !eq(X, upperX)) // Don't waste time
          return createScenario(X);
        else
          return null;
      }
    }

    Scenario createScenario(int[] X) {
      //Scenario scenario = new Scenario(encodeX(X), encodeY(Y));
      Scenario scenario = new Scenario(encodeX(X), encodeY(allY())); // Always include all the queries, otherwise, it's unclear what the reference set is
      jobCounts.put(scenario.id, 1+jobCounts.size());
      return scenario;
    }

    void update(int id, int[] X, boolean unproven) {
      if (done) return;

      // Update refinement probability to make p(y=1) reasonable
      // Only update probability if we were responsible for launching this run
      // This is important in the initial iterations when getting data for updateLower to avoid polarization of probabilities.
      if (jobCounts.containsKey(id)) {
        double oldIncrProb = logistic(incrTheta);
        double singleTargetProb = Math.exp(-1); // Desired p(y=1)

        // Exploit parallelism: idea is that probability that two of the number of processors getting y=1 should be approximately p(y=1)
        double numProcessors = jobCounts.size(); // Approximate number of processors (for this group) with the number of things in the queue.
        //double targetProb = 1 - Math.pow(1-singleTargetProb, 1.0/numProcessors);
        double targetProb = 1 - Math.pow(1-singleTargetProb, 1.0/Math.sqrt(numProcessors+1)); // HACK

        // Due to parallelism, we want to temper the amount of probability increment
        double stepSize = incrThetaStepSize; // Simplify
        //double stepSize = incrThetaStepSize / Math.sqrt(jobCounts.get(id)); // Size of jobCounts at the time the job was created
        if (!unproven) incrTheta -= (1-targetProb) * stepSize; // Proven -> cheaper abstractions
        else incrTheta += targetProb * stepSize; // Unproven -> more expensive abstractions

        EX.logs("    targetProb = %.2f (%.2f eff. proc), stepSize = %.2f/sqrt(%d) = %.2f, incrProb : %.2f -> %.2f [unproven=%s]",
            targetProb, numProcessors,
            incrThetaStepSize, jobCounts.get(id), stepSize,
            oldIncrProb, logistic(incrTheta), unproven);
        jobCounts.remove(id);
      }

      // Detect minimal dip: negative scenario that differs by upperX by one site (that site must be necessary)
      // This should only really be done in the scanning part
      if (unproven) {
        int c = findUniqueDiff(X, upperX);
        if (c != -1) {
          EX.logs("    updateLowerX %s: found that c=%s is necessary", this, components.get(c));
          lowerX[c] = upperX[c];
        }
      }
      else { // Proven
        EX.logs("    updateUpperX %s: reduced |upperX|=%s to |upperX|=%s", this, complexity(upperX), complexity(X));
        set(upperX, X);
      }

      updateStatus();
    }

    void updateStatus() {
      if (scanning) {
        if (eq(lowerX, upperX)) {
          EX.logs("    DONE with group %s!", this);
          done = true;
        }
      }
      else {
        int lowerComplexity = complexity(lowerX);
        int upperComplexity = complexity(upperX);
        int diff = upperComplexity-lowerComplexity;

        if (upperComplexity == 1) { // Can't do better than 1
          EX.logs("    DONE with group %s!", this);
          done = true;
        }
        else if (diff <= scanThreshold) {
          EX.logs("    SCAN group %s now!", this);
          scanning = true;
        }
      }
    }
  }

  int sample(double[] weights) {
    double sumWeight = 0;
    for (double w : weights) sumWeight += w;
    double target = random.nextDouble() * sumWeight;
    double accum = 0;
    for (int i = 0; i < weights.length; i++) {
      accum += weights[i];
      if (accum >= target) return i;
    }
    throw new RuntimeException("Bad");
  }

  List<Group> getCandidateGroups() {
    List<Group> candidates = new ArrayList();
    for (Group g : groups)
      if (g.wantToLaunchJob())
        candidates.add(g);
    return candidates;
  }

  public Scenario createJob() {
    List<Group> candidates = getCandidateGroups();
    if (candidates.size() == 0) return null;
    // Sample a group proportional to the number of effects in that group
    // This is important in the beginning to break up the large groups
    double[] weights = new double[candidates.size()];
    for (int i = 0; i < candidates.size(); i++)
      weights[i] = candidates.get(i).Y.size();
    int chosen = sample(weights);
    Group g = candidates.get(chosen);
    return g.createNewScenario();
  }

  public boolean isDone() {
    for (Group g : groups)
      if (!g.done) return false;
    return true;
  }

  public void onJobResult(Scenario scenario) {
    incorporateScenario(scenario, true);
    outputStatus();
  }

  String render(int[] X, Set<String> Y) { return String.format("|X|=%s,|Y|=%s", complexity(X), Y.size()); }

  // Incorporate the scenario into all groups
  void incorporateScenario(Scenario scenario, boolean saveToDisk) {
    numScenarios++;
    if (saveToDisk) {
      PrintWriter f = Utils.openOutAppend(EX.path("scenarios"));
      f.println(scenario);
      f.close();
    }

    int[] X = decodeX(scenario.in);
    Set<String> Y = decodeY(scenario.out);

    EX.logs("Incorporating scenario id=%s,%s into %s groups (numScenarios = %s)", scenario.id, render(X, Y), groups.size(), numScenarios);
    List<Group> newGroups = new ArrayList();
    boolean changed = false;
    for (Group g : groups)
      changed |= incorporateScenario(scenario.id, X, Y, g, newGroups);
    groups = newGroups;
    if (!changed) // Didn't do anything - probably an outdated scenario
      EX.logs("  Useless: |X|=%s,|Y|=%s", complexity(X), Y.size());
  }

  // Incorporate into group g
  boolean incorporateScenario(int id, int[] X, Set<String> Y, Group g, List<Group> newGroups) {
    // Don't need this since Y is with respect to allY
    // Don't update on jobs we didn't ask for! (Important because we are passing around subset of queries which make sense only with respect to the group that launched the job)
    /*if (!g.jobCounts.containsKey(id)) {
      newGroups.add(g);
      return false;
    }*/

    if (!g.inRange(X)) { // We asked for this job, but now it's useless
      g.jobCounts.remove(id);
      newGroups.add(g);
      return false;
    }

    // Now we can make an impact
    EX.logs("  into %s", g);

    HashSet<String> Y0 = new HashSet();
    HashSet<String> Y1 = new HashSet();
    for (String y : g.Y) {
      if (Y.contains(y)) Y1.add(y);
      else               Y0.add(y);
    }
    if (Y0.size() == 0 || Y1.size() == 0) { // Don't split: all of Y still behaves the same
      assert !(Y0.size() == 0 && Y1.size() == 0); // At least one must be true
      g.update(id, X, Y1.size() > 0);
      newGroups.add(g);
    }
    else {
      Group g0 = new Group(g, Y0);
      Group g1 = new Group(g, Y1);
      g0.update(id, X, false);
      g1.update(id, X, true);
      newGroups.add(g0);
      newGroups.add(g1);
    }
    return true;
  }

  void outputStatus() {
    int numDone = 0, numScanning = 0;
    for (Group g : groups) {
      if (g.done) numDone++;
      else if (g.scanning) numScanning++;
    }

    EX.putOutput("numScenarios", numScenarios);
    EX.putOutput("numDoneGroups", numDone);
    EX.putOutput("numScanGroups", numScanning);
    EX.putOutput("numGroups", groups.size());

    // Print groups
    EX.logs("%s groups", groups.size());
    int sumComplexity = 0;
    int[] X = new int[C];
    for (Group g : groups) {
      EX.logs("  %s", g);
      sumComplexity += complexity(g.upperX);
      for (int c = 0; c < C; c++)
        X[c] = Math.max(X[c], g.upperX[c]);
    }
    EX.putOutput("sumComplexity", sumComplexity);
    EX.putOutput("complexity", complexity(X));

    EX.flushOutput();
  }

  public void saveState() {
    // Save to disk
    {
      PrintWriter out = Utils.openOut(EX.path("groups"));
      for (Group g : groups)
        out.println(logistic(g.incrTheta) + " ## " + encodeX(g.lowerX) + " ## " + encodeX(g.upperX) + " ## " + encodeY(g.Y));
      out.close();
    }
    {
      PrintWriter out = Utils.openOut(EX.path("groups.txt"));
      for (Group g : groups) {
        out.println("=== "+g);
        out.println("Sites ("+defaults+"):");
        for (int c = 0; c < C; c++)
          if (g.upperX[c] != bottomX[c])
            out.println("  "+components.get(c)+":"+g.upperX[c]+ " "+G.jstr(sites.get(c)));
        out.println("Queries:");
        for (String y : g.Y) {
          Query q = y2queries.get(y);
          out.println("  "+q);
        }
      }
      out.close();
    }
  }

  public int maxWorkersNeeded() {
    // If everyone's scanning, just need one per scan
    // Otherwise, need as many workers as we can get.
    int n = 0;
    for (Group g : groups) {
      if (g.done) continue;
      if (g.scanning) n++;
      else return 10000; // Need a lot
    }
    return n;
  }
}
