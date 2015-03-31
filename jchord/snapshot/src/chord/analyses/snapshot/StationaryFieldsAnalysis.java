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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chord.project.Chord;

/**
 * @author omertripp
 * @author pliang
 * 
 */
@Chord(name = "ss-stationary-fields")
public class StationaryFieldsAnalysis extends SnapshotAnalysis {
	private class StationaryFieldQuery extends Query {
		public final int f;

		public StationaryFieldQuery(int f) {
			this.f = f;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + f;
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
			StationaryFieldQuery other = (StationaryFieldQuery) obj;
			if (f != other.f)
				return false;
			return true;
		}
    @Override public String toString() { return fstr(f); }
	}

	private final TIntObjectHashMap<TIntHashSet> obj2readFields = new TIntObjectHashMap<TIntHashSet>(); // object -> set of fields read from it
  
	@Override public String propertyName() { return "stationary-fields"; }
  @Override public boolean require_a2o() { return true; } // Need to get list of objects associated with an abstraction

	@Override public void onProcessPutfieldPrimitive(int e, int t, int b, int f) { onFieldWrite(e, b, f); }
	@Override public void onProcessPutfieldReference(int e, int t, int b, int f, int o) { onFieldWrite(e, b, f); }
	@Override public void onProcessGetfieldPrimitive(int e, int t, int b, int f) { onFieldRead(e, b, f); }
	@Override public void onProcessGetfieldReference(int e, int t, int b, int f, int o) { onFieldRead(e, b, f); }
	
	private void onFieldRead(int e, int b, int f) {
    if (fieldIsExcluded(f)) return;

    TIntHashSet S = obj2readFields.get(b);
    if (S == null) obj2readFields.put(b, S = new TIntHashSet());
    S.add(f);

    StationaryFieldQuery q = new StationaryFieldQuery(f);
    answerQuery(q, false); // No harm in reading...
	}

	private void onFieldWrite(int e, final int b, final int f) {
    if (fieldIsExcluded(f)) return;

    // Check if any object with same abstraction as b has been read
    Object a = abstraction.getValue(b);
    final BoolRef nonStationary = new BoolRef();
    // Other objects bb which are indistinguishable from object b under the abstraction
    abstraction.getObjects(a).forEach(new TIntProcedure() { public boolean execute(int bb) {
      TIntHashSet S = obj2readFields.get(bb);
      if (S != null && S.contains(f)) // Already read!
        nonStationary.value = true;
      return !nonStationary.value;
    } });
    StationaryFieldQuery q = new StationaryFieldQuery(f);
    answerQuery(q, nonStationary.value);
	}

  @Override public void abstractionChanged(int o, Object a) { }
}
