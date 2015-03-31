/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.snapshot;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * Objects are partitioned based on (reflexive) reachability from allocation site.
 * 
 * @author omertripp (omertrip@post.tau.ac.il)
 *
 */
public class ReachableFromAllocAbstraction extends LabelBasedAbstraction {
	private static class AllocationSiteLabel implements Label {
		public final int h;

		public AllocationSiteLabel(int h) {
			this.h = h;
		}

		@Override
		public String toString() {
			return "<h>: " + h;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
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
			AllocationSiteLabel other = (AllocationSiteLabel) obj;
			if (h != other.h)
				return false;
			return true;
		}
	}

	private final TIntObjectHashMap<TIntHashSet> alloc2objects = new TIntObjectHashMap<TIntHashSet>();

	@Override
	public String toString() {
		return "alloc-reachability";
	}

	@Override
	protected TIntHashSet getRootsImpl(Label l) {
		assert (l instanceof AllocationSiteLabel);
		AllocationSiteLabel allocLabel = (AllocationSiteLabel) l;
		return alloc2objects.get(allocLabel.h);
	}

	@Override
	public void nodeCreated(ThreadInfo info, int o) {
		int h = state.o2h.get(o);
		if (o != 0 && h >= 0) {
			Set<Label> S = new HashSet<Label>(1);
			S.add(new AllocationSiteLabel(h));
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
	protected Set<Label> getLabelsRootedAt(int o) {
		int h = state.o2h.get(o);
		if (h >= 0) {
			return Collections.<Label> singleton(new AllocationSiteLabel(h));
		} else {
			return Collections.emptySet();
		}
	}
}
