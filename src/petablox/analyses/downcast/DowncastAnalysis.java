/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package petablox.analyses.downcast;

import petablox.analyses.alias.CtxtsAnalysis;
import petablox.project.ClassicProject;
import petablox.project.Petablox;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;
import petablox.util.tuple.object.Pair;
import soot.Local;

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
 * (see {@link petablox.analyses.alias.CtxtsAnalysis}).</li>
 * </ul>
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
	name = "downcast-java"
)
public class DowncastAnalysis extends JavaAnalysis {
	public void run() {
		ClassicProject.g().runTask(CtxtsAnalysis.getCspaKind());
		ClassicProject.g().runTask("downcast-dlog");
//		ProgramRel downCastRel = (ProgramRel) ClassicProject.g().getTrgt("downcast");
//		downCastRel.load();
//		ProgramRel mvRel = (ProgramRel) ClassicProject.g().getTrgt("MV");
//		ClassicProject.g().getTaskProducingTrgt(mvRel).run();
//		mvRel.load();
//		for(Pair p : downCastRel.getAry2ValTuples()){
//			Local l = (Local)p.val0;
//			for(Pair p1 : mvRel.getAry2ValTuples()){
//				if(p1.val1.equals(l))
//					System.out.println(p1.val0);
//			}
//			System.out.println(p.val1);
//		}
	}
}
