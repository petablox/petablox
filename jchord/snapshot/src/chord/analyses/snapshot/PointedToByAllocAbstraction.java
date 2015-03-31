/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.snapshot;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntProcedure;
import gnu.trove.TIntIterator;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntProcedure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

class PointedToByAllocAbstraction extends Abstraction {
	private static class Value {
		
		protected final static Value EMPTY_VALUE = new Value(new int[0]);
		private final int[] values;
		
		public Value(int[] values) {
			this.values = values.clone(); // Arrays.copyOf(values, values.length);
			Arrays.sort(this.values);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(values);
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
			Value other = (Value) obj;
			if (!Arrays.equals(values, other.values))
				return false;
			return true;
		}
	}

	private final TIntObjectHashMap<TIntIntHashMap> object2pointers = new TIntObjectHashMap<TIntIntHashMap>();

	@Override
	public String toString() {
		return "pointed-to";
	}

	@Override
	public void edgeCreated(int b, int f, int o) {
		if (b != 0 && o != 0 && f != 0) {
			int h = state.o2h.get(b);
			if (h >= 0) {
				TIntIntHashMap M = object2pointers.get(o);
				if (M == null) {
					M = new TIntIntHashMap();
					object2pointers.put(o, M);
				}
				boolean hasChanged = !M.containsValue(h);
				M.put(f, h);
				if (hasChanged) {
					Value v = new Value(M.getValues()); // XXX: these should be sorted?
					setValue(o, v);
				}
			}
		}
	}

	@Override
	public void edgeDeleted(int b, int f, int o) {
		if (b != 0 && o != 0 && f != 0) {
			int h = state.o2h.get(b);
			if (h >= 0) {
				TIntIntHashMap M = object2pointers.get(o);
				assert (M != null);
				M.remove(f);
				boolean hasChanged = !M.containsValue(h);
				if (hasChanged) {
					Value v = new Value(M.getValues());
					setValue(o, v);
				}
			}
		}
	}

	@Override
	public void nodeCreated(ThreadInfo info, int o) {
		// We assign a fresh object the default abstraction.
		object2pointers.put(o, new TIntIntHashMap());
		setValue(o, Value.EMPTY_VALUE);
	}

  @Override public boolean requireGraph() { return true; }
}
