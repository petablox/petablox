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
import gnu.trove.TObjectIntHashMap;
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
import chord.util.tuple.object.Pair;
import chord.util.ArraySet;

/**
 * Note: this version of pointed-to-by includes the abstraction of the current node as well.
 */
public class PointedToByAbstraction extends Abstraction {
  LocalAbstraction abstraction;
  public PointedToByAbstraction(LocalAbstraction abstraction) { this.abstraction = abstraction; }
	@Override public void init(AbstractionInitializer initializer) {
		super.init(initializer);
		abstraction.init(initializer);
	}

  // o -> (abstract value, f) -> number of times an object o' with given abstract value points to o via f
	private final TIntObjectHashMap<TObjectIntHashMap<Pair<Object,Integer>>> object2pointers =
    new TIntObjectHashMap<TObjectIntHashMap<Pair<Object,Integer>>>();
  // o -> abstract value of o
  private TIntObjectHashMap object2value = new TIntObjectHashMap();

	@Override public String toString() { return "point("+abstraction+")"; }

  // Pointers
  public void mySetValue(int o, Object[] pointers) {
    ArraySet a = new ArraySet(pointers.length+1);
    a.add(object2value.get(o)); // Abstraction of me
    //for (Object p : pointers) a.add(p); // Abstractions of who points to me (keep track of field as well)
    for (Object p : pointers) a.add(((Pair)p).val0); // Abstractions of who points to me (keep track of field as well)
    setValue(o, a);
  }

	@Override
	public void edgeCreated(int b, int f, int o) {
    assert (b > 0 && o > 0);
    if (f < 0) return;
    Object val = object2value.get(b);
    if (val == null) return;

    TObjectIntHashMap<Pair<Object,Integer>> M = object2pointers.get(o);
    if (M == null) {
      M = new TObjectIntHashMap();
      object2pointers.put(o, M);
    }

    if (M.adjustOrPutValue(new Pair(val, f), 1, 1) == 1)
      mySetValue(o, M.keys());
	}

	@Override
	public void edgeDeleted(int b, int f, int o) {
    assert (b > 0 && o > 0);
    if (f < 0) return;
    Object val = object2value.get(b);
    if (val == null) return;

    TObjectIntHashMap<Pair<Object,Integer>> M = object2pointers.get(o);
    assert (M != null);

    Pair key = new Pair(val, f);
    if (M.adjustOrPutValue(key, -1, 0) == 0) {
      M.remove(key);
      mySetValue(o, M.keys());
    }
	}

	@Override public void nodeCreated(ThreadInfo info, int o) {
    object2value.put(o, abstraction.computeValue(info, o));
		mySetValue(o, new Object[0]); // No one points to o yet
	}

  @Override public boolean requireGraph() { return true; }
}
