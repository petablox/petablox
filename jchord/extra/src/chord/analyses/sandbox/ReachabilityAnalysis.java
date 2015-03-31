/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.sandbox;

import java.util.HashSet;
import java.util.Set;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import chord.analyses.method.DomM;
import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.Messages;
import chord.project.analyses.DynamicAnalysis;
import chord.project.ClassicProject;

/**
 * @author omertripp
 *
 */
@Chord(name = "dynamic-reach-java")
public class ReachabilityAnalysis extends DynamicAnalysis {
	private InstrScheme instrScheme;
	private DomM domM;
	private final Set<jq_Method> visitedMethods = new HashSet<jq_Method>();

	public InstrScheme getInstrScheme() {
		if (instrScheme != null) return instrScheme;
		instrScheme = new InstrScheme();
		instrScheme.setEnterMethodEvent(true, false);
		return instrScheme;
	}
	
	@Override
	public void initAllPasses() {
		domM = (DomM) ClassicProject.g().getTrgt("M");
		ClassicProject.g().runTask(domM);
	}
	
	@Override
	public void processEnterMethod(int m, int t) {
		if (m >= 0)
			visitedMethods.add(domM.get(m));
	}
    
	@Override
    public void doneAllPasses() {
/*
		for (jq_Method m : visitedMethods)
			System.out.println("XXX: <" + m.getName() + ":" + m.getDesc() + "@" + m.getDeclaringClass().getName() + ">");
*/
	}
}
