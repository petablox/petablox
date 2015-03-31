/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.downcast;

import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.analyses.alias.CtxtsAnalysis;

/**
 * Static downcast safety analysis.
 * <p> 
 * Outputs relations <tt>safeDowncast</tt> and <tt>unsafeDowncast</tt>
 * containing pairs (v,t) such that local variable v of reference type
 * (say) t' may be cast to reference type t which is not a supertype
 * of t', and the cast, as deemed by this analysis, is either provably
 * safe or possibly unsafe, respectively.
 * <p>
 * Recognized system properties:
 * <ul>
 * <li>All system properties recognized by abstract contexts analysis
 * (see {@link chord.analyses.alias.CtxtsAnalysis}).</li>
 * </ul>
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "downcast-java"
)
public class DowncastAnalysis extends JavaAnalysis {
	public void run() {
		ClassicProject.g().runTask(CtxtsAnalysis.getCspaKind());
		ClassicProject.g().runTask("downcast-dlog");
	}
}
