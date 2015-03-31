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
import gnu.trove.TObjectIntHashMap;
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

public class ReachableFromAbstraction extends LabelBasedAbstraction {
  // Trivial wrap around an abstract value
	private static class MyLabel implements Label {
		public final Object value;
		public MyLabel(Object value) { this.value = value; }
		@Override public String toString() { return value.toString(); }
		@Override public int hashCode() { return value.hashCode(); }
		@Override public boolean equals(Object that) { return value.equals(((MyLabel)that).value); }
	}

	private final HashMap<Object,TIntHashSet> value2objects = new HashMap<Object,TIntHashSet>();

  // o -> abstract value of o (must be set on nodeCreated)
  private TIntObjectHashMap object2value = new TIntObjectHashMap();

  LocalAbstraction abstraction;
  public ReachableFromAbstraction(LocalAbstraction abstraction) { this.abstraction = abstraction; }
	@Override public void init(AbstractionInitializer initializer) {
		super.init(initializer);
		abstraction.init(initializer);
	}

	@Override public String toString() { return "reach("+abstraction+")"; }

	@Override
	protected TIntHashSet getRootsImpl(Label l) {
		return value2objects.get(((MyLabel)l).value);
	}

	@Override
	public void nodeCreated(ThreadInfo info, int o) {
    if (o <= 0) return;
    Object val = abstraction.computeValue(info, o);
    object2value.put(o, val);

    Set<Label> S = new HashSet<Label>(1);
    S.add(new MyLabel(val));
    object2labels.put(o, S);
    setFreshValue(o, S);

    TIntHashSet T = value2objects.get(val);
    if (T == null) {
      T = new TIntHashSet();
      value2objects.put(val, T);
    }
    T.add(o);
	}

	@Override
	protected Set<Label> getLabelsRootedAt(int o) {
		Object val = object2value.get(o);
    return Collections.<Label> singleton(new MyLabel(val));
	}
}
