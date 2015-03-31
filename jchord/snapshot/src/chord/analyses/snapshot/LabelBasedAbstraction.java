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

import chord.project.Messages;
import chord.util.ArraySet;

/**
 * 
 * The abstraction of concrete node <code>o</code> is a set of {@link Label}s. {@link LabelBasedAbstraction} 
 * enforces and maintains the following invariant: If <code>o2</code> is reachable from <code>o1</code>, then the set 
 * of labels associated with <code>o2</code> is a superset of the set of labels associated with <code>o1</code>.
 * An invariant resulting from the above invariant is that the set of labels associated with node <code>o</code> is 
 * the union of the sets of labels associated with <code>o</code>'s parents, plus the labels rooted at <code>o</code>.   
 * 
 * @author omertripp (omertrip@post.tau.ac.il)
 *
 */
public abstract class LabelBasedAbstraction extends Abstraction {
	protected static interface Label {
	}

	private static class EfficientUpdateResult {
		protected final static EfficientUpdateResult FALSE = new EfficientUpdateResult(false, true);
		protected final static EfficientUpdateResult TRUE_FALSE = new EfficientUpdateResult(true, false);
		protected final static EfficientUpdateResult TRUE_TRUE = new EfficientUpdateResult(true, true);
		
		protected final boolean succeeded;
		protected final boolean changed;
		
		private EfficientUpdateResult(boolean succeeded, boolean changed) {
			this.succeeded = succeeded;
			this.changed = changed;
		}
	}
	
	private class Procedure implements TIntIntProcedure {
		private final TIntHashSet worklist;
		private final TIntHashSet visited;
		private final Set<Label> labels;
		private final boolean isPos;
		private final boolean propagateOnlyOnChanged;

		public Procedure(TIntHashSet worklist, TIntHashSet visited,
				Set<Label> labels, boolean isPos, boolean propagateOnlyOnChanged) {
			// The invariant does not guarantee the correctness of propagating
			// only on changes in case of negative propagation.
			assert (!(!isPos && propagateOnlyOnChanged));
			this.worklist = worklist;
			this.visited = visited;
			this.labels = labels;
			this.isPos = isPos;
			this.propagateOnlyOnChanged = propagateOnlyOnChanged;
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
					if (hasChanged || !propagateOnlyOnChanged) {
						if (!visited.contains(arg1)) {
							worklist.add(arg1);
						}
					}
				}
			}
			return true;
		}
	}

	private static final boolean VERBOSE = false;
	private static final int THRESHOLD = 3;

	// private final static int ARRAY_CONTENT = Integer.MIN_VALUE;
	protected final TIntObjectHashMap<TIntArrayList> object2predecessors = new TIntObjectHashMap<TIntArrayList>();
	protected final TIntObjectHashMap<TIntIntHashMap> heapGraph = new TIntObjectHashMap<TIntIntHashMap>();
	protected final TIntObjectHashMap<Set<Label>> object2labels = new TIntObjectHashMap<Set<Label>>();

	/* Used for debugging. */
	private int numEdgesCreated;
	private int numEdgesDeleted;

	/*
	 * This method *must not* rely on <code>object2labels</code>, which might
	 * temporarily remove the association between the roots of a label and a
	 * label while performing negative propagation!
	 */
	protected abstract TIntHashSet getRootsImpl(Label l);
	protected abstract Set<Label> getLabelsRootedAt(int o);

	private TIntHashSet getRoots(Label l, TIntHashSet visited) {
		TIntHashSet result = getRootsImpl(l);
		if (result.size() > THRESHOLD) {
			result = refine(visited, result);
		}
		if (VERBOSE) {
			Messages.log("The number of roots for label " + l
					+ " is: " + result.size() + ".");
		}
		return result;
	}

	// Check which roots actually reach the set of explored objects.
	private TIntHashSet refine(TIntHashSet explored, TIntHashSet roots) {
		final TIntHashSet result = new TIntHashSet(); 
		final TIntHashSet visited = new TIntHashSet(explored.size());
		TIntHashSet worklist = new TIntHashSet(explored);
		L: while (!worklist.isEmpty()) {
			TIntIterator it = worklist.iterator();
			final TIntHashSet templist = new TIntHashSet();
			while (it.hasNext()) {
				int next = it.next();
				visited.add(next);
				if (roots.contains(next)) {
					result.add(next);
					// We've added all the roots already.
					if (result.equals(roots)) {
						break L;
					}
				}
				TIntArrayList L = object2predecessors.get(next);
				if (L != null) {
					L.forEach(new TIntProcedure() {
						// @Override
						public boolean execute(int arg0) {
							if (!visited.contains(arg0)) {
								templist.add(arg0);
							}
							return true;
						}
					});
				}
			}
			worklist = templist;
		}
		return result;
	}

	@Override
	public void edgeCreated(int b, int f, int o) {
		if (VERBOSE) {
			Messages.log("So far " + (++numEdgesCreated)
					+ " edges were created.");
		}
		if (b != 0 && f >= 0) {
			updateHeapGraph(b, f, o);
		}
	}

	@Override
	public void edgeDeleted(int b, int f, int o) {
		if (VERBOSE) {
			Messages.log("So far " + (++numEdgesDeleted)
					+ " edges were deleted.");
		}
		if (b != 0 && f >= 0) {
			updateHeapGraph(b, f, 0);
		}
	}

	private Set<Label> getLabels(int b) {
		return object2labels.get(b);
	}

  public void setFreshValue(int o, Set<Label> S) {
    setValue(o, new ArraySet<Label>(S));
  }

	private boolean posLabel(int o, Label l) {
		Set<Label> S = object2labels.get(o);
		if (S == null) {
			object2labels.put(o, S = new HashSet<Label>(1));
		}
		boolean hasChanged = S.add(l);
		if (hasChanged) setFreshValue(o, S);
		return hasChanged;
	}

	private boolean negLabel(int o, Label l) {
		Set<Label> S = object2labels.get(o);
		boolean hasChanged = false;
		if (S != null) {
			hasChanged |= S.remove(l);
		}
		if (hasChanged) setFreshValue(o, S);
		return hasChanged;
	}

	private void updateHeapGraph(final int b, final int f, final int o) {
		EfficientUpdateResult efficientUpdateResult = efficientUpdateLabels(b, f, o);
		if (efficientUpdateResult.succeeded) {
			// Now <code>o</code>'s labels are consistent, and we need to recurse on its children.
			if (efficientUpdateResult.changed) {
				TIntIntHashMap M = heapGraph.get(o);
				if (M != null) {
					M.forEachEntry(new TIntIntProcedure() {
						// @Override
						public boolean execute(int arg0, int arg1) {
							if (arg1 != 0) {
								updateHeapGraph(o, arg0, arg1);
							}
							return true;
						}
					});
				}
			}
		} else {
			if (VERBOSE) {
				Messages.log("Entered updateHeapGraph with arguments <" + b + "," + f + "," + o + ">.");
			}
			doBookKeeping(b, f, o);
			Set<Label> labels = collectLabels(b, f, o);
			if (labels != null) {
				if (o == 0) {
					// We cannot propagate only on changes as this is a negative
					// propagation process.
					TIntHashSet visited = propagateLabels(o, labels, false, false);
					for (Label l : labels) {
						TIntHashSet roots = getRoots(l, visited);
						if (VERBOSE) {
							Messages.log("About to restart propagation of a label with "
								+ roots.size() + " roots.");
						}
						for (TIntIterator it = roots.iterator(); it.hasNext();) {
							int next = it.next();
							// assert (object2labels.get(next).contains(l)); // The
							// root should be associated with the label supposedly
							// originating from it.
							/*
							 * Here too we must propagate all the time as the
							 * invariant
							 * 
							 * x -> y => labels(x) \subset labels(y)
							 * 
							 * is temporarily violated.
							 */
							propagateLabels(next, Collections.<Label> singleton(l),
									true, false);
						}
					}
				} else {
					/*
					 * Since this is a positive propagation process, we are
					 * guaranteed that the invariant
					 * 
					 * x -> y => labels(x) \subset labels(y)
					 * 
					 * holds, which means that if x is already associated with all
					 * the labels we attempt to propagate to it, then it is
					 * redundant to propagate them to its descendants.
					 */
					propagateLabels(o, labels, true, true);
				}
			}
			if (VERBOSE) {
				Messages.log("Left updateHeapGraph with arguments <" + b + "," + f + "," + o + ">.");
			}
		}
	}

	private void doBookKeeping(final int b, final int f, final int o) {
		TIntIntHashMap M = heapGraph.get(b);
		if (M == null) {
			heapGraph.put(b, M = new TIntIntHashMap());
		}
		updateObject2predecessors(b, f, o, M);
		M.put(f, o);
	}

	private EfficientUpdateResult efficientUpdateLabels(int b, int f, int o) {
		// We're only interested in optimizing the hard case, where an edge is deleted.
		if (o == 0) {
			// We first retrieve the value that was overwritten by null, and only then update the heap graph.
			TIntIntHashMap M = heapGraph.get(b);
			if (M.containsKey(f)) {
				int old = M.get(f);
				assert (old != 0);
				Set<Label> oldLabels = getLabels(old);
				doBookKeeping(b, f, o);
				// Now the heap graph is up-to-date, and we can traverse the parents of <code>old</code>.
				final Set<Label> labels = getLabelsRootedAt(old);
				final TIntArrayList preds = object2predecessors.get(old);
				final boolean[] result = new boolean[] { true };
				/* 
				 * <code>newLabels</code> consists of the labels rooted at <code>old</code>, plus those associated
				 * with its immediate predecessors.
				 */
				final Set<Label> newLabels = new HashSet<Label>(labels);
				/*
				 * Iterate over all the predecessors of 
				 */
				preds.forEach(new TIntProcedure() {
					// @Override
					public boolean execute(int arg0) {
						final Set<Label> predLabels = getLabelsRootedAt(arg0);
						for (Label l : labels) {
							if (predLabels.contains(l)) {
								result[0] = false;
								return false;
							}
						}
						newLabels.addAll(predLabels);
						return true;
					}
				});
				if (result[0]) {
					if (newLabels.equals(oldLabels)) {
						return EfficientUpdateResult.TRUE_FALSE;
					} else {
						object2labels.put(old, newLabels);
            setFreshValue(old, newLabels);
						return EfficientUpdateResult.TRUE_TRUE;
					}
				} else {
					return EfficientUpdateResult.FALSE;
				}
			} 
		}
		return EfficientUpdateResult.FALSE;
	}

	private void updateObject2predecessors(final int b, final int f,
			final int o, final TIntIntHashMap M) {
		if (M.containsKey(f)) {
			final int val = M.get(f);
			if (val != 0) {
				// Is <code>val</code> pointed to by any other field?
				final boolean[] isPointedToByAnotherField = new boolean[] { false };
				M.forEachEntry(new TIntIntProcedure() {
					// @Override
					public boolean execute(int arg0, int arg1) {
						if (arg0 != f) {
							if (arg1 == val) {
								isPointedToByAnotherField[0] = true;
								return false;
							}
						}
						return true;
					}
				});
				if (!isPointedToByAnotherField[0]) {
					TIntArrayList L = object2predecessors.get(val);
					assert (L != null);
					int i = L.indexOf(b);
					assert (i >= 0);
					//In trove 3.02, TIntArrayList.remove(i) removes the value=i, rather than the ith element
					L.removeAt(i);
				}
			}
		}
		if (o != 0) {
			TIntArrayList L = object2predecessors.get(o);
			if (L == null) {
				object2predecessors.put(o, L = new TIntArrayList(1));
			}
			L.add(b);
		}
	}

	private Set<Label> collectLabels(int b, int f, int o) {
		Set<Label> labels = getLabels(b);
		Collection<Label> L = freshLabels(b, f, o);
		if (!L.isEmpty()) {
			if (labels == null) {
				labels = new HashSet<Label>(L.size());
			}
			labels.addAll(L);
		}
		return labels;
	}

	protected Collection<Label> freshLabels(int b, int f, int o) {
		return Collections.emptySet();
	}

	private TIntHashSet propagateLabels(int o, Set<Label> labels,
			boolean isPos, boolean propagateOnlyOnChanged) {
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
			Procedure proc = new Procedure(worklist, visited, labels, isPos,
					propagateOnlyOnChanged);
			while (it.hasNext()) {
				final int next = it.next();
				visited.add(next);
				TIntIntHashMap M = heapGraph.get(next);
				if (M != null) {
					M.forEachEntry(proc);
				}
			}
		}
		return visited;
	}

  @Override public boolean requireGraph() { return true; }
}
