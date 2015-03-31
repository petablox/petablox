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

import chord.project.Chord;

/**
 * @author omertripp
 * @author pliang
 *
 */
@Chord(name="ss-thread-access")
public class ThreadAccessAnalysis extends SnapshotAnalysis {
	// object o -> set of threads that have accessed o
	private final TIntObjectHashMap<TIntHashSet> o2threads = new TIntObjectHashMap<TIntHashSet>();
	
	@Override
	public String propertyName() { return "thread-access"; }

	// Need to get list of objects associated with an abstraction
	@Override
	public boolean require_a2o() { return true; }

	@Override
	public void fieldAccessed(int e, final int t, int b, int f, int o) {
		super.fieldAccessed(e, t, b, f, o);

		// Record that object b was accessed by thread t
		TIntHashSet S = o2threads.get(b);
		if (S == null)
			o2threads.put(b, S = new TIntHashSet());
		S.add(t);

		// See if b is accessed by more than one thread under the abstraction
		// Take the union of all threads that access all objects sharing the same abstract value of b
		if (!statementIsExcluded(e)) {
			final BoolRef multiThreaded = new BoolRef();
			if (S.size() > 1)
				multiThreaded.value = true; // Quick check: concrete says yes
			else {
				Object a = abstraction.getValue(b);
				// Other objects bb which are indistinguishable from object b under the abstraction
				abstraction.getObjects(a).forEach(new TIntProcedure() {
					public boolean execute(int bb) {
						TIntHashSet S = o2threads.get(bb);
						if (S != null) {
							if (S.size() > 1) multiThreaded.value = true;
							else if (S.size() == 1 && !S.contains(t)) multiThreaded.value = true;
						}
						return !multiThreaded.value;
					}
				});
			}
			ProgramPointQuery q = new ProgramPointQuery(e);
			answerQuery(q, multiThreaded.value);
		}
	}

	@Override
	public void abstractionChanged(int o, Object a) { }
}
