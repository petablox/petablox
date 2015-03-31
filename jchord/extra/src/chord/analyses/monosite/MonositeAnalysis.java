/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.monosite;

import chord.analyses.alias.CtxtsAnalysis;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;

/**
 * Static monomorphic call site analysis.
 * <p>
 * Outputs relations <tt>monoSite</tt> and <tt>polySite</tt>
 * containing dynamically dispatching method invocation statements
 * (of kind <tt>INVK_VIRTUAL</tt> or <tt>INVK_INTERFACE</tt>)
 * that, as deemed by this analysis, have either at most a single
 * target method or possibly multiple target methods,
 * respectively.
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
	name="monosite-java"
)
public class MonositeAnalysis extends JavaAnalysis {
	private int maxIters;
	
	private ProgramRel relRefineH;
	private ProgramRel relRefineM;
    private ProgramRel relRefineV;
    private ProgramRel relRefineI;
    
	public void run() {
		maxIters = Integer.getInteger("chord.max.iters", 0);
		assert (maxIters >= 0);

		relRefineH = (ProgramRel) ClassicProject.g().getTrgt("refineH");
		relRefineM = (ProgramRel) ClassicProject.g().getTrgt("refineM");
		relRefineV = (ProgramRel) ClassicProject.g().getTrgt("refineV");
		relRefineI = (ProgramRel) ClassicProject.g().getTrgt("refineI");

		String cspaKind = CtxtsAnalysis.getCspaKind();
		for (int numIters = 0; true; numIters++) {
			ClassicProject.g().runTask(cspaKind);
			ClassicProject.g().runTask("monosite-dlog");
			if (numIters == maxIters)
				break;			
			ClassicProject.g().runTask("monosite-feedback-dlog");
			ClassicProject.g().runTask("refine-hybrid-dlog");
            relRefineH.load();
            int numRefineH = relRefineH.size();
            relRefineH.close();
            relRefineM.load();
            int numRefineM = relRefineM.size();
            relRefineM.close();
            relRefineV.load();
            int numRefineV = relRefineV.size();
            relRefineV.close();
            relRefineI.load();
            int numRefineI = relRefineI.size();
            relRefineI.close();
            if (numRefineH == 0 && numRefineM == 0 &&
            	numRefineV == 0 && numRefineI == 0)
                break;
            ClassicProject.g().resetTaskDone(cspaKind);
		}
	}
}
