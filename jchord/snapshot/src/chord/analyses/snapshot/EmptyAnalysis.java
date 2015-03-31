/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.snapshot;

import chord.project.Chord;

/**
 * Do nothing.
 * Used to collect the trace.
 */
@Chord(name = "ss-empty")
public class EmptyAnalysis extends SnapshotAnalysis {
	@Override public String propertyName() { return "empty"; }
  @Override public void abstractionChanged(int o, Object a) { }
}
