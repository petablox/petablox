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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Objects are partitioned based on (reflexive) reachability from allocation site augmented
 * by the first field along the path. Constant <code>AllocPlusFieldLabel.SELF</code> supports 
 * reflexivity.
 * 
 * @author omertripp (omertrip@post.tau.ac.il)
 *
 */
public class ReachableFromAllocPlusFieldsAbstraction extends LabelBasedAbstraction {

	private static class AllocPlusFieldLabel implements Label {
		private static final int SELF = Integer.MIN_VALUE;
		public final int h;
		public final int f;

		public AllocPlusFieldLabel(int h, int f) {
			this.h = h;
			this.f = f;
		}

		public AllocPlusFieldLabel(int h) {
			this(h, SELF);
		}

		@Override
		public String toString() {
			return "<h,f>: " + h + (f == SELF ? "" : "." + f);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + f;
			result = prime * result + h;
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
			AllocPlusFieldLabel other = (AllocPlusFieldLabel) obj;
			if (f != other.f)
				return false;
			if (h != other.h)
				return false;
			return true;
		}
	}

	private final static TIntHashSet EMPTY = new TIntHashSet();
	
	private final TIntObjectHashMap<TIntHashSet> alloc2objects = new TIntObjectHashMap<TIntHashSet>();

	@Override
	public String toString() {
		return "alloc-x-field-reachability";
	}

	@Override
	protected TIntHashSet getRootsImpl(Label l) {
		assert (l instanceof AllocPlusFieldLabel);
		AllocPlusFieldLabel apfl = (AllocPlusFieldLabel) l;
		if (apfl.f == AllocPlusFieldLabel.SELF) {
			return alloc2objects.get(apfl.h);
		} else {
			TIntHashSet alloced = alloc2objects.get(apfl.h);
			if (alloced == null) {
				assert (apfl.h < 0);
				return EMPTY;
			} else {
				TIntHashSet result = new TIntHashSet();
				for (TIntIterator it = alloced.iterator(); it.hasNext(); ) {
					int next = it.next();
					TIntIntHashMap M = heapGraph.get(next);
					if (M != null && M.containsKey(apfl.f)) {
						int o = M.get(apfl.f);
						if (o != 0) {
							result.add(o);
						}
					}
				}
				return result;
			}
		}
	}

	@Override
	protected Collection<Label> freshLabels(int b, int f, int o) {
		int h = state.o2h.get(b);
		return Collections.<Label> singleton(new AllocPlusFieldLabel(h, f));
	}

	@Override
	public void nodeCreated(ThreadInfo info, int o) {
		int h = state.o2h.get(o);
		if (o != 0 && h >= 0) {
			Set<Label> S = new HashSet<Label>(1);
			S.add(new AllocPlusFieldLabel(h));
			object2labels.put(o, S);
			setFreshValue(o, S);
			TIntHashSet T = alloc2objects.get(h);
			if (T == null) {
				T = new TIntHashSet();
				alloc2objects.put(h, T);
			}
			T.add(o);
		}
	}

	@Override
	protected Set<Label> getLabelsRootedAt(final int o) {
		assert (o != 0);
		final Set<Label> result = new HashSet<Label>(4);
		int h = state.o2h.get(o);
		if (h >= 0) {
			result.add(new AllocPlusFieldLabel(h));
		}
		TIntArrayList preds = object2predecessors.get(o);
		if (preds != null) {
			preds.forEach(new TIntProcedure() {
				// @Override
				public boolean execute(final int pred) {
					if (state.o2h.get(pred) >= 0) {
						TIntIntHashMap M = heapGraph.get(pred);
						assert (M != null);
						M.forEachEntry(new TIntIntProcedure() {
							// @Override
							public boolean execute(int f, int val) {
								if (val == o) {
									result.add(new AllocPlusFieldLabel(state.o2h.get(pred), f));
								}
								return true;
							}
						});
					}
					return true;
				}
			});
		}
		return result;
	}
}
