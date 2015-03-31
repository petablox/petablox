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

public class RandomAbstraction extends Abstraction {
  int size;
  Random random;
  public RandomAbstraction(int size) {
    this.size = size;
    this.random = new Random(1);
  }

	@Override public String toString() { return "random("+size+")"; }

	@Override public void nodeCreated(ThreadInfo info, int o) {
    setValue(o, random.nextInt(size));
	}
	@Override public void edgeCreated(int b, int f, int o) { }
	@Override public void edgeDeleted(int b, int f, int o) { }
  @Override public boolean requireGraph() { return false; }
}
