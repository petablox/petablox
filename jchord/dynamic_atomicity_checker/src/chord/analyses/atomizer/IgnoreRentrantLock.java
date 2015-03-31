/*
 * Copyright (c) 2009-2010, The Hong Kong University of Science & Technology.
 * All rights reserved.
 * Licensed under the terms of the BSD License; see COPYING for details.
 */
package chord.analyses.atomizer;

import gnu.trove.TIntObjectHashMap;

/**
 * @author Zhifeng Lai (zflai.cn@gmail.com)
 */
public class IgnoreRentrantLock {
	TIntObjectHashMap<IntSetWithCount> thr2Lcks = new TIntObjectHashMap<IntSetWithCount>();

	public boolean processAcquireLock(int t, int o) {
		if (!thr2Lcks.containsKey(t)) {
			thr2Lcks.put(t, new IntSetWithCount());
		}
		IntSetWithCount lcks = thr2Lcks.get(t); 
		return lcks.add(o);
	}

	public boolean processReleaseLock(int t, int o) {
		assert (thr2Lcks.containsKey(t));
		IntSetWithCount lcks = thr2Lcks.get(t);
		return lcks.remove(o);
	}
}
