/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
import java.util.HashSet;
import java.util.Set;
import java.io.PrintWriter;
import java.io.File;
import java.io.IOException;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import chord.program.Program;
import chord.analyses.method.DomM;
import chord.analyses.alloc.DomH;
import chord.analyses.basicblock.DomW;
import chord.analyses.basicblock.DomB;
import chord.analyses.point.DomP;
import chord.analyses.invk.DomI;
import chord.analyses.field.DomF;
import chord.analyses.heapacc.DomE;
import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.Messages;
import chord.project.analyses.DynamicAnalysis;
import chord.project.ClassicProject;
import chord.project.Config;


@Chord(name = "dynamic-test-java")
public class DynamicTestAnalysis extends DynamicAnalysis {
	private InstrScheme instrScheme;
	private DomM domM;
	private DomW domW;
	private DomB domB;
	private DomP domP;
	private DomI domI;
	private DomF domF;
	private DomE domE;
	private DomH domH;
	private PrintWriter out;

	@Override
	public InstrScheme getInstrScheme() {
		if (instrScheme != null) return instrScheme;
		instrScheme = new InstrScheme();
		instrScheme.setEnterMainMethodEvent(true);
		instrScheme.setEnterMethodEvent(true, true);
		instrScheme.setLeaveMethodEvent(true, true);
		instrScheme.setBasicBlockEvent();
		instrScheme.setQuadEvent();
		instrScheme.setBefMethodCallEvent(true, true, true);
		instrScheme.setAftMethodCallEvent(true, true, true);
		instrScheme.setGetstaticPrimitiveEvent(true, true, true, true);
		instrScheme.setGetstaticReferenceEvent(true, true, true, true, true);
		instrScheme.setPutstaticPrimitiveEvent(true, true, true, true);
		instrScheme.setPutstaticReferenceEvent(true, true, true, true, true);
		instrScheme.setGetfieldPrimitiveEvent(true, true, true, true);
		instrScheme.setGetfieldReferenceEvent(true, true, true, true, true);
		instrScheme.setPutfieldPrimitiveEvent(true, true, true, true);
		instrScheme.setPutfieldReferenceEvent(true, true, true, true, true);
		instrScheme.setAloadPrimitiveEvent(true, true, true, true);
		instrScheme.setAloadReferenceEvent(true, true, true, true, true);
		instrScheme.setAstorePrimitiveEvent(true, true, true, true);
		instrScheme.setAstoreReferenceEvent(true, true, true, true, true);
		return instrScheme;
	}
	@Override
	public void initAllPasses() {
		try {
			out = new PrintWriter(new File(Config.outDirName, "trace.txt"));
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		domM = (DomM) ClassicProject.g().getTrgt("M");
		domI = (DomI) ClassicProject.g().getTrgt("I");
		domB = (DomB) ClassicProject.g().getTrgt("B");
		domP = (DomP) ClassicProject.g().getTrgt("P");
		domW = (DomW) ClassicProject.g().getTrgt("W");
		domF = (DomF) ClassicProject.g().getTrgt("F");
		domE = (DomE) ClassicProject.g().getTrgt("E");
	}
	@Override
	public void doneAllPasses() {
		out.close();
	}
	@Override
	public void processEnterMainMethod(int t) {
		out.println(t + ": ENTER_MAIN_METHOD " + Program.g().getMainMethod());
    }
	@Override
	public void processEnterMethod(int m, int t) {
		if (m >= 0)
			out.println(t + ": ENTER_METHOD " + domM.get(m));
    }
	@Override
	public void processLeaveMethod(int m, int t) {
		if (m >= 0)
			out.println(t + ": LEAVE_METHOD " + domM.get(m));
    }
	@Override
	public void processEnterLoop(int w, int t) {
		if (w >= 0)
			out.println(t + ": ENTER LOOP " + domW.toUniqueString(w));
	}
	@Override
	public void processLeaveLoop(int w, int t) {
		if (w >= 0)
			out.println(t + ": LEAVE LOOP " + domW.toUniqueString(w));
	}
	@Override
	public void processLoopIteration(int w, int t) {
		if (w >= 0)
			out.println(t + ": ITER LOOP " + domW.toUniqueString(w));
	}
	@Override
	public void processBasicBlock(int b, int t) {
		if (b >= 0)
			out.println(t + ": BASIC BLOCK " + domB.toUniqueString(b));
	}
	@Override
	public void processQuad(int p, int t) {
		if (p >= 0)
			out.println(t + ": QUAD " + domP.toUniqueString(p));
	}
	@Override
	public void processBefMethodCall(int i, int t, int o) {
		if (i >= 0)
			out.println(t + ": BEF_METHOD_CALL " + domI.toUniqueString(i) + " " + o); 
	}
	@Override
    public void processAftMethodCall(int i, int t, int o) {
		if (i >= 0)
			out.println(t + ": AFT_METHOD_CALL " + domI.toUniqueString(i) + " " + o); 
	}
	@Override
    public void processBefNew(int h, int t, int o) {
		if (h >= 0)
			out.println(t + ": BEF_NEW " + domH.toUniqueString(h) + " " + o);
	}
	@Override
    public void processAftNew(int h, int t, int o) {
		if (h >= 0)
			out.println(t + ": AFT_NEW " + domH.toUniqueString(h) + " " + o);
	}
	@Override
    public void processNewArray(int h, int t, int o) {
		if (h >= 0)
			out.println(t + ": NEWARRAY " + domH.toUniqueString(h) + " " + o);
	}
	@Override
    public void processGetstaticPrimitive(int e, int t, int b, int f) {
		if (e >= 0 && f >= 0)
			out.println(t + ": GETSTATIC_PRI " + domE.toUniqueString(e) + " " + b + " " + domF.toUniqueString(f));
	}
	@Override
    public void processGetstaticReference(int e, int t, int b, int f, int o) {
		if (e >= 0 && f >= 0)
			out.println(t + ": GETSTATIC_REF " + domE.toUniqueString(e) + " " + b + " " + domF.toUniqueString(f) + " " + o);
	}
	@Override
    public void processPutstaticPrimitive(int e, int t, int b, int f) {
		if (e >= 0 && f >= 0)
			out.println(t + ": PUTSTATIC_PRI " + domE.toUniqueString(e) + " " + b + " " + domF.toUniqueString(f));
	}
	@Override
    public void processPutstaticReference(int e, int t, int b, int f, int o) {
		if (e >= 0 && f >= 0)
			out.println(t + ": PUTSTATIC_REF " + domE.toUniqueString(e) + " " + b + " " + domF.toUniqueString(f) + " " + o);
	}
	@Override
    public void processGetfieldPrimitive(int e, int t, int b, int f) {
		if (e >= 0 && f >= 0)
			out.println(t + ": GETFIELD_PRI " + domE.toUniqueString(e) + " " + b + " " + domF.toUniqueString(f));
	}
	@Override
    public void processGetfieldReference(int e, int t, int b, int f, int o) {
		if (e >= 0 && f >= 0)
			out.println(t + ": GETFIELD_REF " + domE.toUniqueString(e) + " " + b + " " + domF.toUniqueString(f) + " " + o);
	}
	@Override
    public void processPutfieldPrimitive(int e, int t, int b, int f) {
		if (e >= 0 && f >= 0)
			out.println(t + ": PUTFIELD_PRI " + domE.toUniqueString(e) + " " + b + " " + domF.toUniqueString(f));
	}
	@Override
    public void processPutfieldReference(int e, int t, int b, int f, int o) {
		if (e >= 0 && f >= 0)
			out.println(t + ": PUTFIELD_REF " + domE.toUniqueString(e) + " " + b + " " + domF.toUniqueString(f) + " " + o);
	}
	@Override
    public void processAloadPrimitive(int e, int t, int b, int i) {
		if (e >= 0)
			out.println(t + ": ALOAD_PRI " + domE.toUniqueString(e) + " " + b + " " + i);
	}
	@Override
    public void processAloadReference(int e, int t, int b, int i, int o) {
		if (e >= 0)
			out.println(t + ": ALOAD_REF " + domE.toUniqueString(e) + " " + b + " " + i + " " + o);
	}
	@Override
    public void processAstorePrimitive(int e, int t, int b, int i) {
		if (e >= 0)
			out.println(t + ": ASTORE_PRI " + domE.toUniqueString(e) + " " + b + " " + i);
	}
	@Override
    public void processAstoreReference(int e, int t, int b, int i, int o) {
		if (e >= 0)
			out.println(t + ": ASTORE_REF " + domE.toUniqueString(e) + " " + b + " " + i + " " + o);
	}
}
