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

public class AllocAbstraction extends LocalAbstraction {
	int kCFA;

	public AllocAbstraction(int kCFA) { this.kCFA = kCFA; }

	@Override
	public String toString() {
		if (kCFA == 0) return "alloc";
		return String.format("alloc(k=%d)", kCFA);
	}

	@Override
	public void nodeCreated(ThreadInfo info, int o) {
		setValue(o, computeValue(info, o));
	}

	@Override
	public void edgeCreated(int b, int f, int o) { }

	@Override
	public void edgeDeleted(int b, int f, int o) { }

	public Object computeValue(ThreadInfo info, int o) {
		if (kCFA == 0) return state.o2h.get(o); // No context

		StringBuilder buf = new StringBuilder();
		buf.append(state.o2h.get(o));

		for (int i = 0; i < kCFA; i++) {
			int j = info.callSites.size() - i - 1;
			if (j < 0)
				break;
			buf.append('_');
			buf.append(info.callSites.get(j));
		}

		return buf.toString();
	}

	@Override
	public boolean requireGraph() { return false; }
}
