/*
 * Copyright (c) 2009-2010, The Hong Kong University of Science & Technology.
 * All rights reserved.
 * Licensed under the terms of the BSD License; see COPYING for details.
 */
package chord.analyses.atomizer;

/**
 * @author Zhifeng Lai (zflai.cn@gmail.com)
 */
public class CommitState {
	public static final int PRE_COMM = 10;
	public static final int POS_COMM = 11;
	public static final int OUT_SIDE = 12;
	
	public static String getName(int state) {
		String name;
		switch (state) {
		case PRE_COMM:
			name = "PRE_COMM";
			break;
		case POS_COMM:
			name = "POS_COMM";
			break;
		case OUT_SIDE:
			name = "OUT_SIDE";
			break;
		default:
			throw new RuntimeException("Invalide commit state.");
		}
		return name;
	} 
}
