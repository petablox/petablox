/*
 * Copyright (c) 2010-2011, Ariel Rabkin.
 * All rights reserved.
 */
package chord.analyses.confdep;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.program.Program;
import chord.project.Chord;
import chord.project.Config;
import chord.project.ITask;
import chord.project.OutDirUtils;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.Utils;
import chord.util.tuple.integer.*;
import chord.util.tuple.object.*;
import chord.analyses.confdep.optnames.DomOpts;
import chord.analyses.confdep.rels.RelFailurePath;

import java.util.Set;
import chord.analyses.primtrack.DomUV;
import chord.analyses.string.DomStrConst;
import chord.bddbddb.Dom;
import chord.bddbddb.Rel.*;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.analyses.field.DomF;
import chord.analyses.alloc.DomH;

@Chord(
		name = "ConfDeps"
)
public class ConfDeps extends JavaAnalysis {

	public boolean STATIC = false;
	public boolean DYNTRACK = false;
	public boolean SUPERCONTEXT = false;
	public static final String CONFDEP_SCANLOGS_OPT = "confdep.scanlogs";
	public static final String CONFDEP_DYNAMIC_OPT = "confdep.dynamic"; //should be either static, dynamic-load or dynamic-track
	DomH domH;
	DomI domI;
	DomV domV;
	DomUV domUV;
	DomF domF;
	DomStrConst domConst;
	public boolean lookAtLogs = false;
	boolean fakeExec;


	public void run() {
		ClassicProject Project = ClassicProject.g();

		fakeExec = Utils.buildBoolProperty("programUnchanged", false);
		lookAtLogs = Utils.buildBoolProperty(CONFDEP_SCANLOGS_OPT, true);
		String dynamism = System.getProperty(CONFDEP_DYNAMIC_OPT, "static");
		if(dynamism.equals("static")) {
			STATIC = true;
			DYNTRACK = false;
			SUPERCONTEXT = System.getProperty(RelFailurePath.FAILTRACE_OPT, "").length() > 0;
		} else if (dynamism.equals("dynamic-track")) {
			STATIC = false;
			DYNTRACK = true;
		} else if(dynamism.equals("dynamic-load")) {
			STATIC = false;
			DYNTRACK = false;
		} else {
			System.err.println("ERR: " + CONFDEP_DYNAMIC_OPT + " must be 'static', 'dynamic-track', or dynamic-load");
			System.exit(-1);
		}
		boolean miniStrings = Utils.buildBoolProperty("useMiniStrings", false);
		boolean dumpIntermediates = Utils.buildBoolProperty("dumpArgTaints", false);

		slurpDoms();

		boolean wideCallModel = Utils.buildBoolProperty("externalCallsReachEverything", true);
		if(!wideCallModel)
			makeEmptyRelation(Project, "externalThis");

		if(STATIC) {
			maybeRun(Project,"cipa-0cfa-arr-dlog");

			maybeRun(Project,"findconf-dlog");

			if(miniStrings)
				maybeRun(Project,"mini-str-dlog");
			else
				maybeRun(Project,"strcomponents-dlog");

			Project.runTask("CnfNodeSucc");

			Project.runTask("Opt");
		} else {
			Project.runTask("dynamic-cdep-java");	  
			Project.runTask("cipa-0cfa-arr-dlog");
		}

		if(DYNTRACK) {
			Project.runTask("dyn-datadep");
		} else {
			maybeRun(Project,"datadep-func-dlog");
		}

		if(SUPERCONTEXT) {
			//    	Project.runTask("PobjVarAsgnInst"); //to avoid counting domP time in SCS
			maybeRun(Project,"confdep-dlog"); //to mark primRefDep as done

			Project.runTask("scs-datadep-dlog");
			Project.runTask("scs-confdep-dlog");
		} else 
			Project.runTask("confdep-dlog");

		DomOpts domOpt  = (DomOpts) Project.getTrgt("Opt");

		dumpOptUses(domOpt);
		dumpFieldTaints(domOpt, "instHF", "statHF");
		if(dumpIntermediates)  {
			if(STATIC)
				dumpOptRegexes("conf_regex.txt", DomOpts.optSites());
			Project.runTask("datadep-debug-dlog");
			dumpArgDTaints();
		}
	}

	protected void makeEmptyRelation(ClassicProject Project, String name) {
		ProgramRel rel = (ProgramRel) Project.getTrgt(name);
		rel.zero();
		rel.save();
	}

	protected void dumpArgDTaints() {
		PrintWriter writer =
			OutDirUtils.newPrintWriter("meth_arg_conf_taints.txt");

		ProgramRel confArgRel =  (ProgramRel) ClassicProject.g().getTrgt("argCdep");//outputs m, z, String
		confArgRel.load();  
		for(Trio<jq_Method, Integer, String> methArg:  confArgRel.<jq_Method, Integer, String>getAry3ValTuples()) {
			String optName = methArg.val2;
			int z = methArg.val1;
			jq_Method meth = methArg.val0;
			if(meth.getParamTypes().length <= z)
				System.err.println("WARN: expected to find more args to " + meth + 
						" (found " + meth.getParamTypes().length + " expected at least " + (z+1) +")");
			else {
				jq_Type ty = meth.getParamTypes()[z];
				writer.println(meth.getDeclaringClass() + " " + meth.getNameAndDesc().toString() +
						" arg " + z +  " of type " + ty + " " +  optName);
			}
		}

		confArgRel.close();
		writer.close();

		writer =
			OutDirUtils.newPrintWriter("inv_arg_conf_taints.txt");

		confArgRel =  (ProgramRel) ClassicProject.g().getTrgt("IargCdep");//outputs i, z, String
		confArgRel.load();  
		for(Trio<Quad, Integer, String> methArg:  confArgRel.<Quad, Integer, String>getAry3ValTuples()) {
			String optName = methArg.val2;
			int z = methArg.val1;
			jq_Method calledMeth = Invoke.getMethod(methArg.val0).getMethod();
			jq_Method callerM = methArg.val0.getMethod();
			if(calledMeth.getParamTypes().length <= z) 
				continue;

			jq_Type ty = calledMeth.getParamTypes()[z];
			String regName = Invoke.getParam(methArg.val0, z).getRegister().toString();
			String caller = callerM.getDeclaringClass() + " " + callerM.getName() + ":" + methArg.val0.getLineNumber();
			writer.println(caller + " calling " + calledMeth.getDeclaringClass() + " " + calledMeth.getNameAndDesc().toString() +
					" arg " + z + "(" +regName +") of type " + ty + " " +  optName);
		}

		confArgRel.close();
		writer.close();

		writer =
			OutDirUtils.newPrintWriter("inv_ret_conf_taints.txt");

		confArgRel =  (ProgramRel) ClassicProject.g().getTrgt("IretDep");//outputs i, i
		confArgRel.load();  
		for(Pair<Quad, String> invkRet:  confArgRel.<Quad, String>getAry2ValTuples()) {
			String optName = invkRet.val1;
			jq_Method calledMeth = Invoke.getMethod(invkRet.val0).getMethod();
			jq_Method callerM = invkRet.val0.getMethod();
			jq_Type ty = calledMeth.getReturnType();
			String caller = callerM.getDeclaringClass() + " " + callerM.getName() + ":" + invkRet.val0.getLineNumber();
			writer.println(caller + " calling " + calledMeth.getDeclaringClass() + " " + calledMeth.getNameAndDesc().toString() +
					" returns type " + ty + " taint: " +  optName);
		}

		confArgRel.close();
		writer.close();
	}

	protected void dumpFieldTaints(DomOpts opts, String instRelName, String statRelName) {

		HashSet<String> printedLines = new HashSet<String>();

		PrintWriter writer =
			OutDirUtils.newPrintWriter("conf_fields.txt");

		{ //a new block to mask 'tuples'
			ProgramRel relConfFields =
				(ProgramRel) ClassicProject.g().getTrgt(instRelName);//ouputs H0,F0,Opt
			relConfFields.load();
			IntTrioIterable tuples = relConfFields.getAry3IntTuples();
			for (IntTrio p : tuples) {

				jq_Field f = domF.get(p.idx1);
				String optName = opts.get(p.idx2);
				String clname,fieldName, fType;
				if(f == null ) {
					if(p.idx1 == 0) { //null f when p.idx1 == 0 is uninteresting; that's just the array case
						fieldName = "array_contents";
						clname = "Array";
						fType = "java.lang.Object";
					} else {
						System.out.println("ERR: no F entry for " + p.idx1+ " (Option was " + optName+")");
						continue;
					}
				} else {
					jq_Class cl = f.getDeclaringClass();
					if(cl == null)
						clname = "UNKNOWN";
					else
						clname = cl.getName();
					fieldName = f.getName().toString();
					fType = f.getType().getName();
				}
				
				String optAndLine = optName + clname+ fieldName;
				if(!printedLines.contains(optAndLine)) {
					printedLines.add(optAndLine);
					writer.println(clname+ " "+ ": " + optName  + " affects field " + 
							fieldName+ " of type " + fType + ".");
				}
			}
			relConfFields.close();
		} //end dynamic block
		ProgramRel relStatFields =
			(ProgramRel) ClassicProject.g().getTrgt(statRelName);//ouputs F0,Opt
		relStatFields.load();
		IntPairIterable statTuples = relStatFields.getAry2IntTuples();
		for (IntPair p : statTuples) {
			jq_Field f = domF.get(p.idx0);
			String optName = opts.get(p.idx1);

			if(f == null&& p.idx1 != 0) {
				System.out.println("ERR: no F entry for " + p.idx0+ " (Option was " + optName+")");
				continue;
			}
			jq_Class cl = f.getDeclaringClass();
			String clname;
			if(cl == null)
				clname = "UNKNOWN";
			else
				clname = cl.getName();
			String optAndLine = optName + clname+ f.getName();
			if(!printedLines.contains(optAndLine)) {
				printedLines.add(optAndLine);
				writer.println(clname+ " "+ ": " + optName + " affects static field " +
						f.getName() + " of type " + f.getType().getName()+ ".");
			}
		}
		relStatFields.close();
		writer.close();
	}


	protected void dumpOptUses(DomOpts opts) {

		HashSet<String> printedLines = new HashSet<String>();
		HashSet<Integer> quadsPrinted = new HashSet<Integer>();

		HashSet<Integer> quadsSeen = new HashSet<Integer>();
		int confUseQuad = 0;

		PrintWriter writer =
			OutDirUtils.newPrintWriter("conf_uses.txt");

		ProgramRel relConfUses =
			(ProgramRel) ClassicProject.g().getTrgt("cOnLine");//ouputs I0,Opt
		relConfUses.load();
		IntPairIterable tuples = relConfUses.getAry2IntTuples();
		for (IntPair p : tuples) {
			Quad q1 = (Quad) domI.get(p.idx0);

			quadsSeen.add(p.idx0);
			confUseQuad++;

			jq_Method m = q1.getMethod();
			int lineno = q1.getLineNumber();

			String optName = opts.get(p.idx1);

			String filename = m.getDeclaringClass().getSourceFileName();
			String optAndLine = optName+lineno + filename;
			if(!printedLines.contains(optAndLine)) {
				printedLines.add(optAndLine);
				quadsPrinted.add(p.idx0);
				jq_Method calledMethod = Invoke.getMethod(q1).getMethod();
				String calltarg = calledMethod.getDeclaringClass().getName() + " "+ calledMethod.getName();
				writer.println(filename+ " "+ lineno + ": " + optName + " in use  (" + m.getName() + "). Called method was " + calltarg);
			}
		}
		writer.close();
		relConfUses.close();
		int quadCount = quadsPrinted.size();
		int confUses = printedLines.size();


		ProgramRel reachableI =  (ProgramRel) ClassicProject.g().getTrgt("reachableI");
		reachableI.load();
		int reachableIsize = reachableI.size();
		reachableI.close();


		PrintWriter stats = OutDirUtils.newPrintWriter("confdep_stats.txt");
		stats.println("saw " + quadCount + " lines with a conf option and " + confUses + " conf uses. (LRatio = " + confUses *1.0 / quadCount+ ")");
		quadCount = quadsSeen.size();
		stats.println("saw " + quadCount + " quads with a conf option and " + confUseQuad + " conf uses. (QRatio = " + confUseQuad *1.0 / quadCount+ ")");
		stats.println("saw " + reachableIsize + " invokes, and " + confUses + " uses over those. IRatio =" + confUseQuad *1.0 / reachableIsize);
		stats.println(opts.size() + " total options");
		stats.close();
	}


	public static void dumpOptRegexes(String filename, Set<Pair<Quad,String>> names) {
		PrintWriter writer =
			OutDirUtils.newPrintWriter(filename);

		for(Pair<Quad, String> s: names) {
			Quad quad = s.val0;
			String regexStr = s.val1;
			jq_Method m = quad.getMethod();
			jq_Class cl = m.getDeclaringClass();
			int lineno = quad.getLineNumber();
			String optType = ConfDefines.optionPrefix(quad);
			String readingMeth = Invoke.getMethod(quad).getMethod().getName().toString();

			writer.println(optType + regexStr + " read by " +
					m.getDeclaringClass().getName()+ " " + m.getName() + ":" + lineno + " " + readingMeth);
		}
		writer.close();
	}

	public void slurpDoms() {

		ClassicProject project = ClassicProject.g();

		domConst = (DomStrConst) project.getTrgt("StrConst");
		domI = (DomI) project.getTrgt("I");
		domH = (DomH) project.getTrgt("H");
		domV = (DomV) project.getTrgt("V");
		domUV = (DomUV) project.getTrgt("UV");
		domF = (DomF) project.getTrgt("F");
	}

	public void maybeRun(ClassicProject Project, String taskName) {
		ITask task = Project.getTask(taskName);

		if(fakeExec && resultsExist(task)) {
			System.out.println("marking " + taskName + " as done");
			fakeExec(task);
		}
		else {
			System.out.println("Can't use cached results for " + task +"; fakeExec = " + fakeExec);
			Project.runTask(taskName);
		}
	}
	public void fakeExec(ITask task) {
		ClassicProject p = ClassicProject.g();

		List<Object> consumedTrgts = p.taskToConsumedTrgtsMap.get(task);
		for (Object trgt : consumedTrgts) {
			if (p.isTrgtDone(trgt))
				continue;
			ITask task2 = p.getTaskProducingTrgt(trgt);
			if (task2 instanceof Dom<?>) {
				task2.run();
			} else
				fakeExec(task2);
		}
		System.out.println("Not running " + task.getName() + "; faking instead");
		p.setTaskDone(task);
		List<Object> producedTrgts = p.taskToProducedTrgtsMap.get(task);
		assert(producedTrgts != null);
		for (Object trgt : producedTrgts) {
			p.setTrgtDone(trgt);
		}
	}

	public boolean resultsExist(ITask task) {
		ClassicProject p = ClassicProject.g();

		List<Object> producedTrgts = p.taskToProducedTrgtsMap.get(task);
		boolean outRelsExist = true;
		for (Object trgt : producedTrgts) {
			if (trgt instanceof ProgramRel) {
				ProgramRel trgtRel = (ProgramRel) trgt;
				File relOnDisk = new File(Config.bddbddbWorkDirName, trgtRel.getName()+".bdd");
				if (!relOnDisk.exists()) {
					System.err.println("no such target " + relOnDisk+", regenerating?");
					outRelsExist = false;
					break;
				}
			}
		}
		return outRelsExist;
	}
}

