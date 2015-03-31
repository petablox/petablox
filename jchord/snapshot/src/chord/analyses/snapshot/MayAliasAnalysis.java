/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.snapshot;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectProcedure;
import gnu.trove.TObjectProcedure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Set;

import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.*;
import joeq.Class.jq_Field;
import chord.bddbddb.Rel.PairIterable;
import chord.analyses.heapacc.DomE;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.program.Program;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.integer.IntPair;
import chord.util.tuple.object.Pair;

/**
 * @author omertripp
 * @author pliang
 * 
 */
@Chord(name = "ss-may-alias")
public class MayAliasAnalysis extends SnapshotAnalysis {
	private class MayAliasQuery extends Query {
		public final int e1;
		public final int e2;

		public MayAliasQuery(int e1, int e2) {
			this.e1 = e1;
			this.e2 = e2;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + e1;
			result = prime * result + e2;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MayAliasQuery other = (MayAliasQuery) obj;
			if (e1 != other.e1)
				return false;
			if (e2 != other.e2)
				return false;
			return true;
		}

    @Override public String toString() { return estr(e1) + " ; " + estr(e2); }
	}

	//private final TIntObjectHashMap<Set<Object>> loc2abstractions = new TIntObjectHashMap<Set<Object>>();
	//protected final Set<IntPair> aliasingRacePairSet = new HashSet<IntPair>();

  TIntObjectHashMap<TIntHashSet> o2es = new TIntObjectHashMap<TIntHashSet>(); // object -> set of statements e accessing o

	@Override
	public String propertyName() {
		return "may-alias";
	}

	@Override
	public void fieldAccessed(int e, int t, int b, int f, int o) {
		super.fieldAccessed(e, t, b, f, o);
    //updatePointsTo(e, abstraction.getValue(b));
    updatePointsTo(e, b);
	}

	@Override
	public void initPass() {
		super.initPass();
		//loc2abstractions.clear();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void donePass() {
    //donePassDatalog();
    donePassJava();
  }

  public void donePassJava() {
    // For each accessing statement, see what field and whether it is written
    int E = domE.size();
    int[] e2f = new int[E];
    boolean[] isWrite = new boolean[E];
	Program program = Program.g();
    for(int e = 0; e < E; e++) {
      Quad q = (Quad) domE.get(e);
      Operator op = q.getOperator();
      jq_Field field = q.getField();
      if (field == null) {
        assert (op instanceof ALoad || op instanceof AStore);
        e2f[e] = 0;
        isWrite[e] = op instanceof AStore;
      }
      else if (op instanceof Getstatic || op instanceof Putstatic) // Ignore
        e2f[e] = -1;
      else {
        assert (op instanceof Getfield || op instanceof Putfield);
        e2f[e] = domF.indexOf(field); // Could be -1
        isWrite[e] = op instanceof Putfield;
      }
    }

    // Smash objects together
    HashMap<Object,TIntHashSet> a2es = new HashMap<Object,TIntHashSet>(); // abstract value -> set of accessing statements e
    for (int o : o2es.keys()) {
      Object a = abstraction.getValue(o);
      TIntHashSet set = a2es.get(a);
      if (set == null) a2es.put(a, set = new TIntHashSet());
      for (int e : o2es.get(o).toArray())
        set.add(e);
    }

    // Find pairs of racing statements
    HashSet<IntPair> racingPairs = new HashSet<IntPair>();
    for (TIntHashSet es : a2es.values()) {
      for (int e1 : es.toArray()) {
        for (int e2 : es.toArray()) {
          if (e1 >= e2) continue; // Don't double count
          if (e2f[e1] == -1 || e2f[e2] == -1) continue; // Error in the field
          if (e2f[e1] != e2f[e2]) continue; // Has to be same field
          if (!isWrite[e1] && !isWrite[e2]) continue; // At least one has to be a write
          racingPairs.add(new IntPair(e1, e2));
        }
      }
    }

    for (IntPair pair : racingPairs) {
      int e1 = pair.idx0;
      int e2 = pair.idx1;
      MayAliasQuery q = new MayAliasQuery(e1, e2);
      answerQuery(q, true);
    }
  }

  /*public void donePassDatalog() {
		final ProgramDom<Object> domS = (ProgramDom) ClassicProject.g().getTrgt("S");
		loc2abstractions.forEachValue(new TObjectProcedure<Set<Object>>() {
			@Override
			public boolean execute(Set<Object> arg0) {
				domS.addAll(arg0);
				return true;
			}
		});
		domS.save();
		final ProgramRel absvalRel = (ProgramRel) ClassicProject.g().getTrgt("absval");
		absvalRel.zero();
		loc2abstractions.forEachEntry(new TIntObjectProcedure<Set<Object>>() {
			@Override
			public boolean execute(int idx0, Set<Object> arg1) {
				for (Object o : arg1) {
					int idx1 = domS.indexOf(o);
					if (idx1 != -1)
						absvalRel.add(idx0, idx1);
				}
				return true;
			}
		});
		absvalRel.save();
		ClassicProject.g().runTask("aliasing-race-pair-dlog");
		ProgramRel aliasingRel = (ProgramRel) Project
				.getTrgt("aliasingRacePair");
		aliasingRel.load();
		PairIterable<Inst, Inst> tuples = aliasingRel.getAry2ValTuples();
		for (Pair<Inst, Inst> p : tuples) {
			Quad quad0 = (Quad) p.val0;
			int e1 = domE.indexOf(quad0);
			Quad quad1 = (Quad) p.val1;
			int e2 = domE.indexOf(quad1);

	    if (e1 == e2) continue; // pliang 03/20/10: temporary fix (don't change this)
			//assert (e1 != e2);
      if (statementIsExcluded(e1) || statementIsExcluded(e2)) continue;

			MayAliasQuery q = new MayAliasQuery(e1, e2);
      answerQuery(q, true);
		}
		aliasingRel.close();
	}*/

	private void updatePointsTo(int e, int b) {
    /*Set<Object> pts = loc2abstractions.get(e);
    if (pts == null) {
      pts = new HashSet<Object>();
      loc2abstractions.put(e, pts);
    }
    pts.add(b);*/
    if (statementIsExcluded(e)) return;

    TIntHashSet set = o2es.get(b);
    if (set == null) o2es.put(b, set = new TIntHashSet());
    set.add(e);
	}

  @Override public void abstractionChanged(int o, Object a) { }
}
