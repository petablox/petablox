/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.thread;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import chord.project.ClassicProject;
import chord.project.Project;
import chord.project.analyses.ProgramDom;
import chord.util.tuple.object.Pair;
import chord.analyses.method.DomM;
import chord.analyses.alloc.DomH;

/**
 * Domain of abstract threads.
 *
 * @see chord.analyses.thread.ThreadsAnalysis
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DomD extends ProgramDom<Pair<Quad, jq_Method>> {
	private DomH domH;
	private DomM domM;
    public String toXMLAttrsString(Pair<Quad, jq_Method> bVal) {
		if (domH == null)
			domH = (DomH) ClassicProject.g().getTrgt("H");
		if (domM == null)
			domM = (DomM) ClassicProject.g().getTrgt("M");
		if (bVal == null)
			return "";
		int h = domH.indexOf(bVal.val0);
		int m = domM.indexOf(bVal.val1);
		return "Hid=\"H" + h + "\" Mid=\"M" + m + "\"";
    }
}
