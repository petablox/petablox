/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.snapshot;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntProcedure;
import gnu.trove.TIntIterator;
import gnu.trove.TIntObjectHashMap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import chord.project.analyses.DynamicAnalysis;
import chord.instr.InstrScheme;

/**
 * @author omertripp
 *
 */
public abstract class LabelBasedAnalysis extends DynamicAnalysis {

	protected static interface Label {
	}
	
	private class Procedure implements TIntIntProcedure {
		private final TIntHashSet worklist;
		private final TIntHashSet visited;
		private final Set<Label> labels;
		private final boolean isPos;
		private final boolean propagateOnlyOnChange;

		public Procedure(TIntHashSet worklist, TIntHashSet visited, Set<Label> labels, boolean isPos, boolean propagateOnlyOnChange) {
			// The invariant does not guarantee the correctness of propagating only on changes in case of negative propagation.
			assert (!(!isPos && propagateOnlyOnChange)); 
			this.worklist = worklist;
			this.visited = visited;
			this.labels = labels;
			this.isPos = isPos;
			this.propagateOnlyOnChange = propagateOnlyOnChange;
		}
		
		// @Override
		public boolean execute(int arg0, int arg1) {
			if (arg1 != 0) {
				for (Label label : labels) {
					boolean hasChanged;
					if (isPos) {
						hasChanged = posLabel(arg1, label);
					} else { 
						hasChanged = negLabel(arg1, label);
					}
					if (hasChanged || !propagateOnlyOnChange) {
						if (!visited.contains(arg1)) {
							worklist.add(arg1);
						}
					}
				}
			}
			return true;
		}
	}
	
//	private final static int ARRAY_CONTENT = Integer.MIN_VALUE;
	
	private InstrScheme instrScheme;
	private final TIntObjectHashMap<TIntIntHashMap> heapGraph = new TIntObjectHashMap<TIntIntHashMap>();
	protected final TIntObjectHashMap<Set<Label>> object2labels = new TIntObjectHashMap<Set<Label>>();

	/* 
	 * This method *must not* rely on <code>object2labels</code>, which might temporarily remove the association
	 * between the roots of a label and a label while performing negative propagation!  
	 * 
	 * */
	protected abstract TIntHashSet getRoots(Label l);
	
	private Set<Label> getLabels(int b) {
		return object2labels.get(b);
	}
	
	private boolean posLabel(int o, Label l) {
		Set<Label> S = object2labels.get(o);
		if (S == null) {
			object2labels.put(o, S = new HashSet<Label>(1));
		}
		return S.add(l);
	}
	
	private boolean negLabel(int o, Label l) {
		Set<Label> S = object2labels.get(o);
		if (S != null) {
			return S.remove(l);
		} else {
			return false;
		}
	}
	
	@Override
	public InstrScheme getInstrScheme() {
		if (instrScheme != null) return instrScheme;
		instrScheme = new InstrScheme();
		instrScheme.setPutfieldReferenceEvent(false, false, true, true, true);
		instrScheme.setAstoreReferenceEvent(false, false, true, true, true);
		return instrScheme;
	}
	
	@Override
	public void processAstoreReference(int e, int t, int b, int i, int o) {
		if (b != 0) {
			updateHeapGraph(b, i, o);
		}
	}
	
	@Override
	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		if (b != 0 && f >= 0) {
			updateHeapGraph(b, f, o);
		}
	}
	
	private void updateHeapGraph(int b, int f, int o) {
		TIntIntHashMap M = heapGraph.get(b);
		if (M == null) {
			heapGraph.put(b, M = new TIntIntHashMap());
		}
		M.put(f, o);
		Set<Label> labels = getLabels(b);
		if (labels != null) {
			if (o == 0) {
				// We cannot propagate only on changes as this is a negative propagation process.
				propagateLabels(o, labels, false, false);
				for (Label l : labels) {
					TIntHashSet roots = getRoots(l);
					for (TIntIterator it=roots.iterator(); it.hasNext(); ) {
						/* 
						 * Here, too, we must propagate all the time, as the invariant
						 * 
						 * 		x -> y => labels(x) \subset labels(y)
						 * 
						 * is temporarily violated.
						 */
						propagateLabels(it.next(), Collections.<Label> singleton(l), true, false);
					}
				}
			} else {
				/* 
				 * Since this is a positive propagation process, we are guaranteed that the invariant
				 * 
				 * 		x -> y => labels(x) \subset labels(y)
				 * 
				 * holds, which means that if x is already associated with all the labels we attempt to propagate to it, then
				 * it is redundant to propagate them to its descendants.
				 */
				propagateLabels(o, labels, true, true);
			}
		}
	}
	
	private void propagateLabels(int o, Set<Label> labels, boolean isPos, boolean propagateOnlyOnChange) {
		TIntHashSet worklist = new TIntHashSet();
		TIntHashSet visited = new TIntHashSet();
		for (Label l : labels) {
			if (isPos)
				posLabel(o, l);
			else
				negLabel(o, l);
		}
		worklist.add(o);
		while (!worklist.isEmpty()) {
			TIntIterator it = worklist.iterator();
			worklist = new TIntHashSet();
			Procedure proc = new Procedure(worklist, visited, labels, isPos, propagateOnlyOnChange);
			while (it.hasNext()) {
				final int next = it.next();
				visited.add(next);
				TIntIntHashMap M = heapGraph.get(next);
				if (M != null) {
					M.forEachEntry(proc);
				}
			}
		}
	}
}
