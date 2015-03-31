/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.snapshot;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntProcedure;
import chord.project.Chord;

/**
 * Thread-escape in the snapshot framework.
 * Does not reclaim global variables.
 *
 * @author Percy Liang (pliang@cs.berkeley.edu)
 */
@Chord(name="ss-thread-escape")
public class ThreadEscapeAnalysis extends SnapshotAnalysis {
	// Set of objects reachable from some global at any point in time
	TIntHashSet escapedNodes = new TIntHashSet();

	@Override
	public String propertyName() { return "thread-escape"; }

	// Need to get list of objects associated with an abstraction
	@Override
	public boolean require_a2o() { return true; }

	// Need the graph
	@Override
	public boolean requireGraph() { return true; }

	// These methods add nodes to the graph
	@Override
	public void onProcessPutstaticReference(int e, int t, int b, int f, int o) {
		setEscape(o);
	}

	@Override
	public void onProcessThreadStart(int i, int t, int o) {
		setEscape(o);
	}

	@Override
	public void edgeCreated(int t, int b, int f, int o) {
		super.edgeCreated(t, b, f, o);
		// NOTE: the addition of this edge changes the abstraction and could in principle
		// cause more nodes to escape.  Ignore this effect for now.
		if (escapedNodes.contains(b))
			setEscape(o);
	}

	@Override
	public void abstractionChanged(int o, Object a) {
		// When the abstraction changes, more things might get smashed together, so
		// we need to propagate more escaping.
		// Does anything escape?
		final BoolRef escaped = new BoolRef();
    
		abstraction.getObjects(a).forEach(new TIntProcedure() {
			public boolean execute(int oo) {
				if (escapedNodes.contains(oo)) escaped.value = true;
				return !escaped.value;
			}
		});
		// If anything escapes, then everything escapes.
		if (escaped.value) {
			abstraction.getObjects(a).forEach(new TIntProcedure() {
				public boolean execute(int oo) {
					setEscapeRecurse(oo);
					return true;
				}
			});
		}
	}

	public void setEscape(int o) {
		if (o == 0) return;
		setEscapeRecurse(o);
	}

	private void setEscapeRecurse(int o) {
		if (escapedNodes.contains(o)) return;
		escapedNodes.add(o);

		// Follow edges
		for (Edge e : state.o2edges.get(o))
			setEscapeRecurse(e.o);

		// Follow abstractions
		Object a = abstraction.getValue(o);
		if (verbose >= 1) X.logs("SETESCAPE o=%s a=%s", ostr(o), a);
		if (fieldAccessOut != null)
			fieldAccessOut.printf("ESC %s | %s\n", ostr(o), estr(curr_e));
		abstraction.getObjects(a).forEach(new TIntProcedure() {
			public boolean execute(int oo) {
				setEscapeRecurse(oo);
				return true;
			}
		});
	}

	@Override
	public void fieldAccessed(int e, int t, int b, int f, int o) {
		super.fieldAccessed(e, t, b, f, o);
		Query query = new ProgramPointQuery(e);
		if (!statementIsExcluded(e))
			answerQuery(query, escapedNodes.contains(b)); // Query is already computed for us
	}
}
