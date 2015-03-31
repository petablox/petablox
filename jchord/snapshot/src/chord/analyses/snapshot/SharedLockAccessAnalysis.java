/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.snapshot;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;
import gnu.trove.TIntProcedure;
import gnu.trove.TIntObjectHashMap;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.lang.Boolean;

import chord.instr.InstrScheme;
import chord.project.Chord;

/**
 * @author omertripp
 * @author pliang
 *
 */
@Chord(name="ss-shared-lock-access")
public class SharedLockAccessAnalysis extends SnapshotAnalysis {
	private final TIntObjectHashMap<TIntHashSet> o2threads = new TIntObjectHashMap<TIntHashSet>(); // object o -> set of threads that have locked o

	@Override public String propertyName() { return "shared-lock-access"; }
  @Override public boolean require_a2o() { return true; } // Need to get list of objects associated with an abstraction

  private class SharedLockAccessQuery extends Query {
    public final int l;
    public SharedLockAccessQuery(int l) { this.l = l; }
    @Override public int hashCode() { return l; }
    @Override public boolean equals(Object that) { return this.l == ((SharedLockAccessQuery)that).l; }
    @Override public String toString() { return lstr(l); }
  }
	
	@Override public void onProcessAcquireLock(int l, final int t, int o) {
    if (l < 0) return;

    // Record that object o was locked by thread t
		TIntHashSet S = o2threads.get(o);
		if (S == null) o2threads.put(o, S = new TIntHashSet());
    S.add(t);

    // See if o is locked by more than one thread under the abstraction
    // Take the union of all threads that have locked all objects sharing the same abstract value of b
    if (!lockIsExcluded(l)) {
      final BoolRef multiThreaded = new BoolRef();
      if (S.size() > 1) multiThreaded.value = true; // Quick check: concrete says yes
      else {
        Object a = abstraction.getValue(o);
        // Other objects oo which are indistinguishable from object o under the abstraction
        abstraction.getObjects(a).forEach(new TIntProcedure() { public boolean execute(int oo) {
          TIntHashSet S = o2threads.get(oo);
          if (S != null) {
            if (S.size() > 1) multiThreaded.value = true;
            else if (S.size() == 1 && !S.contains(t)) multiThreaded.value = true;
          }
          return !multiThreaded.value;
        } });
      }

			SharedLockAccessQuery q = new SharedLockAccessQuery(l);
      answerQuery(q, multiThreaded.value);
    }
	}

  @Override public void abstractionChanged(int o, Object a) { }
}
