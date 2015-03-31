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

public class RecencyAbstraction extends Abstraction {
	HashMap<Object,TIntArrayList> val2lastObjects = new HashMap<Object,TIntArrayList>(); // preliminary value -> latest objects
	LocalAbstraction abstraction;
  int order; // Number of old objects to keep around

	public RecencyAbstraction(LocalAbstraction abstraction, int order) {
		this.abstraction = abstraction;
    this.order = order;
	}

	@Override
	public void init(AbstractionInitializer initializer) {
		super.init(initializer);
		abstraction.init(initializer);
	}

	@Override
	public String toString() {
		return "recency" + order + "(" + abstraction + ")";
	}

	@Override
	public void nodeCreated(ThreadInfo info, int o) {
		Object val = abstraction.computeValue(info, o);
    TIntArrayList objects = val2lastObjects.get(val);
    if (objects == null) 
      val2lastObjects.put(val, objects = new TIntArrayList());
    // i = 0: most recent
    // ...
    // i = order-1
    // i >= order: oldest
    int n = objects.size();
    if (n == 0)
      objects.add(o);
    else {
      // Shift everyone to the right
      for (int i = n-1; i >= 0; i--) {
        int old_o = objects.get(i);
        if (i == n-1) { // The last one
          if (n <= order) { // Expand
            objects.add(old_o);
            setValue(old_o, val+"~"+(i+1));
          }
        }
        else {
          objects.set(i+1, old_o);
          setValue(old_o, val+"~"+(i+1));
        }
      }
      objects.set(0, o);
    }
    setValue(o, val);
	}

	@Override public void edgeCreated(int b, int f, int o) { }
	@Override public void edgeDeleted(int b, int f, int o) { }
  @Override public boolean requireGraph() { return abstraction.requireGraph(); }
}
