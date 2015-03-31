/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.thread;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.program.Program;
import chord.analyses.method.DomM;
import chord.analyses.alloc.DomH;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

/**
 * Static analysis computing reachable abstract threads.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "threadDHM-java",
	consumes = { "threadH" },
	produces = { "D", "threadDHM" },
    namesOfSigns = { "threadDHM" },
    signs = { "D0,H0,M0:D0_M0_H0" },
	namesOfTypes = { "D" },
	types = { DomD.class }
)
public class ThreadDHM extends JavaAnalysis {
	public void run() {
		Program program = Program.g();
        DomH domH = (DomH) ClassicProject.g().getTrgt("H");
        DomM domM = (DomM) ClassicProject.g().getTrgt("M");
        DomD domD = (DomD) ClassicProject.g().getTrgt("D");
        domD.clear();
		domD.add(null);
        jq_Method mainMeth = program.getMainMethod();
        domD.add(new Pair<Quad, jq_Method>(null, mainMeth));
        jq_Method threadStartMeth = program.getThreadStartMethod();
		if (threadStartMeth != null) {
        	ProgramRel relThreadH = (ProgramRel) ClassicProject.g().getTrgt("threadH");
			relThreadH.load();
			Iterable<Quad> tuples = relThreadH.getAry1ValTuples();
			for (Quad q : tuples) {
				domD.add(new Pair<Quad, jq_Method>(q, threadStartMeth));
			}
        	relThreadH.close();
		}
		domD.save();
        ProgramRel relThreadDHM = (ProgramRel) ClassicProject.g().getTrgt("threadDHM");
        relThreadDHM.zero();
        for (int b = 1; b < domD.size(); b++) {
			Pair<Quad, jq_Method> hm = domD.get(b);
			int h = domH.indexOf(hm.val0);
            int m = domM.indexOf(hm.val1);
            relThreadDHM.add(b, h, m);
        }
        relThreadDHM.save();
	}
}
