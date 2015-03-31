/*
 * Copyright (c) 2009-2010, The Hong Kong University of Science & Technology.
 * All rights reserved.
 * Licensed under the terms of the BSD License; see COPYING for details.
 */
package chord.analyses.atomizer;

import gnu.trove.TIntIntHashMap;

/**
 * @author Zhifeng Lai (zflai.cn@gmail.com)
 */
public class IntSetWithCount {
	private TIntIntHashMap elems = new TIntIntHashMap();
    
    public boolean add(int e) {
    	boolean ret;
    	
    	if (!elems.containsKey(e)) {
    		elems.put(e, 0);
    		ret = true;
    	} else {
    		ret = false;
    	}
    	elems.adjustValue(e, 1);
    	return ret;
    }

    public boolean remove(int e) {
    	assert (elems.containsKey(e));
    	
    	boolean ret;
    	elems.adjustValue(e, -1);
    	int count = elems.get(e);
    	if (count == 0) {
    		elems.remove(e);
    		ret = true;
    	} else {
    		ret = false;
    	}
    	return ret;
    }
}
