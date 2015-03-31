package chord.analyses.confdep.docu;

import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.analyses.confdep.ConfDefines;
import chord.analyses.confdep.ConfDeps;
import chord.analyses.confdep.optnames.DomOpts;
import chord.analyses.confdep.rels.RelFailurePath;
import chord.analyses.invk.DomI;
import chord.bddbddb.Rel.RelView;
import chord.analyses.argret.DomK;
import chord.project.*;
import chord.project.analyses.*;
import chord.util.Utils;
import chord.util.tuple.object.Pair;

@Chord(
		name = "ExplainMyLogs"
)
public class ExplainMyLogs extends JavaAnalysis{

	boolean miniStrings;

	int MAX_CTRLDEPS_TO_DUMP = 30;
	int TOO_MANY_MENTIONS = 8;
	static final String PURE_DYNAMIC = "<dynamic>";
	static final boolean USE_NON_CS_DATADEP= false;

	String[] inScopePrefixes;
	@Override
	public void run() {

		inScopePrefixes = Utils.toArray(System.getProperty("dictionary.scope", ""));
		if(inScopePrefixes.length == 0)
			inScopePrefixes = new String[] {""};

		ClassicProject project = ClassicProject.g();

		miniStrings = Utils.buildBoolProperty("useMiniStrings", false);
		boolean miniDatadep = Utils.buildBoolProperty("explainlogs.minidatadep", false);

		if(!justStatic()) {
			System.out.println("refusing to run ExplainMyLogs with nontrivial ConfDeps set up");
			return;
		}
		project.runTask("cipa-0cfa-arr-dlog");

		project.runTask("findconf-dlog");

		if(miniStrings)
			project.runTask("mini-str-dlog");
		else
			project.runTask("strcomponents-dlog");

		project.runTask("Opt"); //used for name-finding
		if(miniDatadep)
			project.runTask("minidatadep-dlog"); //just rename primFlow to primCdep, etc
		else
			if(!project.isTaskDone("datadep-func-cs-dlog") || USE_NON_CS_DATADEP) //if we already ran a dataflow, hope it was the fancy one
				project.runTask("datadep-func-dlog"); //Use the broad-scope datadep, not narrow flow
			else
				project.runTask("datadep-func-cs-dlog");
		//    	else
		//was datadep-func-
		project.runTask("logconfdep-dlog");

		ConfDeps c = new ConfDeps();
		c.slurpDoms();

		DomOpts opts = (DomOpts) project.getTrgt("Opt");

		Map<Quad, Pair<String,Integer>> renderedMessages = renderAllLogMessages();
		Map<Quad,Integer> depsPerLine = dumpLogDependencies(renderedMessages, opts);
		dumpIndexedDependencies(renderedMessages, depsPerLine);
		checkForExplicitDeps(renderedMessages, opts);
		//    dumpLogToProgPtMap();
	}

	private boolean justStatic() {
		String dynamism = System.getProperty(ConfDeps.CONFDEP_DYNAMIC_OPT, "static");
		return dynamism.equals("static") &&
		System.getProperty(RelFailurePath.FAILTRACE_OPT, "").length() == 0;
	}

	private Map<String, Pattern> depMap(DomOpts opts) {
		Map<String, Pattern> optPats = new LinkedHashMap<String, Pattern>();
		for(String opt: opts) {
			if(opt.equals("UNKNOWN"))
				continue;
			//if(opt.contains("PROP-.*@|myinstance|myScribeInstance|lower") continue;
			if(opt.contains("lower|upper|peerreview") || opt.split("\\|").length > 3 ||
					opt.equals("PROP-Replicas")) {
				System.err.println("skipping option " + opt);
				continue;
			} 
			String prunedName = ConfDefines.pruneName(opt);
			prunedName = prunedName.replaceAll("\\.([^\\*])", "\\\\.$1");
			optPats.put(opt, Pattern.compile("^"+prunedName+"|[^$-_]"+prunedName));
		}
		return optPats;
	}
	private void checkForExplicitDeps(
			Map<Quad, Pair<String, Integer>> renderedMessages, DomOpts opts) {
		PrintWriter writer =  OutDirUtils.newPrintWriter("explicit_dependencies.txt");

		writer.println("list of recognized options:");
		Map<String, Pattern> optPats = depMap(opts);
		for(String opt: optPats.keySet()) {

			writer.println("\t"+opt+ " recognized by " + optPats.get(opt).pattern());
		}
		writer.println("-----------------\n");

		for(Map.Entry<Quad, Pair<String, Integer>> e: renderedMessages.entrySet()) {
			Quad q = e.getKey();
			String message = e.getValue().val0;
			Set<String> depSet = optionMentions(message, optPats);
			if(depSet.size() > 0) {
				writer.print(q.getMethod());
				writer.print(":\t");
				writer.println(message);
				for(String dep : depSet) {
					writer.print('\t');
					writer.println(dep);
				}
			}
		}
		writer.close();
	}

	//expect message to have options substituted in already
	//returns a set of qualified option names
	private static Set<String> optionMentions(String formattedMsg, Map<String, Pattern> opts) {
		TreeSet<String> deps = new TreeSet<String>();
		for(Map.Entry<String, Pattern> p: opts.entrySet()) {
			Matcher m = p.getValue().matcher(formattedMsg);
			String name = p.getKey();
			if(m.find() )
				deps.add(name);
		}
		return deps;
	}

	//Maps from quad to the associated message and its data dependence count
	private Map<Quad, Pair<String,Integer>> renderAllLogMessages() {
		ClassicProject project = ClassicProject.g();

		Map<Quad, Pair<String,Integer>> rendered = new LinkedHashMap<Quad, Pair<String,Integer>>();
		ProgramRel allLogs = (ProgramRel) project.getTrgt("RlogStmt"); //just quads
		allLogs.load();

		ProgramRel logStrings = (ProgramRel) project.getTrgt("logString"); //i,cst,z
		logStrings.load();

		ProgramRel dataDep = (ProgramRel) project.getTrgt("logFieldDataDep");// i, z, Opt
		dataDep.load();

		for(Object q: allLogs.getAry1ValTuples()) {
			Quad logCall = (Quad) q;
			jq_Method m = logCall.getMethod();
			jq_Class cl = m.getDeclaringClass();

			boolean isInScope = Utils.prefixMatch(cl.getName(), inScopePrefixes);
			if(!isInScope)
				continue;
			String msg = renderLogMsg(logCall, logStrings, dataDep, true);

			Set<String> depsForLine = getDepsForLine(logCall, dataDep);
			rendered.put(logCall, new Pair<String,Integer>(msg, depsForLine.size()));      
		}

		dataDep.close();
		allLogs.close();
		return rendered;
	}


	private Set<String> getDepsForLine(Quad logCall, ProgramRel dataDep) {
		RelView dataDepV = dataDep.getView();
		dataDepV.selectAndDelete(0, logCall);
		Set<String> s = new LinkedHashSet<String>(dataDepV.size());
		for(Pair<Integer,String> t: dataDepV.<Integer,String>getAry2ValTuples()) {
			s.add(t.val1);
		}
		dataDepV.free();
		return s;
	}

	/**
	 * Dumps a listing of log messages, annotated with the source option.
	 * Returns map of the number of dependences per log message
	 */
	private Map<Quad, Integer> dumpLogDependencies(Map<Quad, Pair<String,Integer>> renderedMessages,
			DomOpts opts) {
		ClassicProject project = ClassicProject.g();

		HashMap<Quad, Integer> depsPerLine = new HashMap<Quad, Integer>();

		Map<String, Pattern> optPats = depMap(opts);

		PrintWriter writer =  OutDirUtils.newPrintWriter("log_dependency.txt");
		ProgramRel logConfDeps =
			(ProgramRel) project.getTrgt("logConfDep");//ouputs I0,Opt (stmt, src)
		logConfDeps.load();

		DomI domI = (DomI) project.getTrgt("I");

		int depCount = 0;

		int mentionCount = 0;
		int detectedMentions = 0;
		for(Map.Entry<Quad, Pair<String,Integer>> msgPair: renderedMessages.entrySet()) {
			Quad logCall = msgPair.getKey();
			int idx = domI.indexOf(logCall);
			jq_Method m = logCall.getMethod();
			jq_Class cl = m.getDeclaringClass();
			int lineno = logCall.getLineNumber();

			String msg = msgPair.getValue().val0;

			Set<String> optionMentions = optionMentions(msg, optPats);

			RelView ctrlDepView = logConfDeps.getView();
			ctrlDepView.selectAndDelete(0, logCall);

			if(msg.equals(PURE_DYNAMIC) && ctrlDepView.size() == 0)
				continue; //ignore messages we don't know anything about

			for(String thisLine: msg.split("\n")) {
				String formatted =  cl.toString()+":" +lineno  + " (" + m.getName() +") " +
				Invoke.getMethod(logCall).getMethod().getName()+ "("  + thisLine+")  Iidx = " + idx;
				writer.println(formatted);
			}
			if(optionMentions.size() > TOO_MANY_MENTIONS) {
				writer.println("too many mentions; ignoring all");
				continue;
			} else 
				mentionCount += optionMentions.size();

			int dataDeps = msgPair.getValue().val1;
			depCount += dataDeps + ctrlDepView.size();

			//first deal with all control dependencies, explicit or otherwise
			for(String ctrlDep: ctrlDepView.<String>getAry1ValTuples()) {
				if(ctrlDep == null)
					continue;
				//          optName = "Unknown Conf";

				boolean foundOpt = optionMentions.remove(ctrlDep);
				if(foundOpt) {
					writer.println("\texplicitly control-depends on "+ctrlDep);
					detectedMentions ++;
					int reduct = removeSimilarOpts(optionMentions, ctrlDep);
					depCount -= reduct;
					mentionCount -= reduct;
				} 
				else
					writer.println("\tcontrol-depends on "+ctrlDep);
			}
			depsPerLine.put(logCall, ctrlDepView.size() + dataDeps);
			
			List<String> possiblyUndetected = new LinkedList<String>();
			List<String> alreadyDetected = new LinkedList<String>();
			for(String s: optionMentions) {
				if(msg.contains(s)) { //we already filtered out the control deps. There's
					//copy of the option name embedded if there's a datadep
					writer.println("\texplicit data dependence:" + s);
					detectedMentions ++;
					alreadyDetected.add(s);
				} else
					possiblyUndetected.add(s);
			}
			
		//handling case where we find an false match before the true one.
			for(String s: alreadyDetected) {
				int reduct = removeSimilarOpts(possiblyUndetected, s);
				depCount -= reduct;
				mentionCount -= reduct;
			}
			
			for(String s: possiblyUndetected)//since not detected, we don't know if it's control or data
				writer.println("\texplicit-but-undetected dep:" + s);
			ctrlDepView.free();
		}

		writer.println("total of " + renderedMessages.size() + " log statements; " + depCount +
				" dependencies, and " + optPats.size() + " options");
		writer.println("explicit: " + mentionCount + " detected-explicit: " + detectedMentions);
		writer.close();

		logConfDeps.close();
		return depsPerLine;
	}

	private int removeSimilarOpts(Collection<String> optionMentions, String ctrlDep) {
		String pruned =  ConfDefines.pruneName(ctrlDep);
		Iterator<String> iter = optionMentions.iterator();
		int removed = 0;
		while(iter.hasNext()) {
			String s = iter.next();
			if(ConfDefines.pruneName(s).equals(pruned)) {
				iter.remove();
				removed ++;
			}
		}	
		return removed;
	}

	private void dumpIndexedDependencies(Map<Quad, Pair<String,Integer>> renderedMessages,
			Map<Quad,Integer> depsPerLine) {
		//  HashMap<String, Set<String>> messagesByDataOpt = new HashMap<String, Set<String>>();
		//HashMap<String, Set<String>> messagesByCtrlOpt = new HashMap<String, Set<String>>();
		ClassicProject project = ClassicProject.g();

		PrintWriter writer = OutDirUtils.newPrintWriter("messages_by_option.txt");
		ProgramRel logConfDeps =
			(ProgramRel) project.getTrgt("logConfDep");//outputs I0,Opt (stmt, src)
		logConfDeps.load();
		ProgramRel dataDep = (ProgramRel) project.getTrgt("logFieldDataDep");// i, z, Opt
		dataDep.load();
		//    RelView dataDep = dataDepWide.getView();
		//   dataDep.delete(1);

		DomOpts opts = (DomOpts) project.getTrgt("Opt");

		for(String option: opts) {
			TreeSet<String> messagesForThisOpt = new TreeSet<String>();//for sorting

			//start with data dependences
			RelView myDataDeps = dataDep.getView();
			myDataDeps.selectAndDelete(2, option);
			myDataDeps.delete(1);
			for(Quad stmt: myDataDeps.<Quad>getAry1ValTuples()) {
				Pair<String,Integer> p = renderedMessages.get(stmt);

				//    		if( p == null) {
				//   			System.err.println("WARN: shouldn't get " + )
				//   		}
				if(p == null) {
					System.out.println("WARN: no rendered version of message at " + stmt.getMethod().getDeclaringClass() + "."+stmt.getMethod());
				} else
					if(p != null && p.val1 < 10)
						tagAndAddMsg(renderedMessages, messagesForThisOpt, stmt);
			}
			int dataDeps = messagesForThisOpt.size();

			//compute control dependencies
			RelView deptsOfThisOpt = logConfDeps.getView();
			deptsOfThisOpt.selectAndDelete(1, option);
			int ctrlDepCnt = deptsOfThisOpt.size();
			if(dataDeps + ctrlDepCnt > 0)
				writer.println(option + " has " + (dataDeps + ctrlDepCnt) + " dependences. "+
						ctrlDepCnt + " control, " + dataDeps + " data");
			//don't dump excess control deps
			if(ctrlDepCnt <= MAX_CTRLDEPS_TO_DUMP) {
				for(Quad logStmt: deptsOfThisOpt.<Quad>getAry1ValTuples()) {
					tagAndAddMsg(renderedMessages, messagesForThisOpt, logStmt);
				}
			}

			//should add datadeps here
			if(messagesForThisOpt.size() <= MAX_CTRLDEPS_TO_DUMP) {
				for(String s: messagesForThisOpt)
					writer.println(s);
				writer.println();
			}
		}

		dataDep.close();
		logConfDeps.close();
		writer.close();

	}


	private void tagAndAddMsg(Map<Quad, Pair<String,Integer>> renderedMessages,
			TreeSet<String> messagesForThisOpt, Quad logStmt) {
		Pair<String,Integer> p = renderedMessages.get(logStmt);
		if(p == null || p.val0 == null)
			return;

		String rendered = p.val0;

		jq_Method m = logStmt.getMethod();
		jq_Class cl = m.getDeclaringClass();
		int lineno = logStmt.getLineNumber();
		for(String thisLine: rendered.split("\n")) {
			String formatted = "\t" + cl.toString()+":" +lineno  + " (" + m.getName() +") " +
			Invoke.getMethod(logStmt).getMethod().getName()+ "("  + thisLine+")";
			messagesForThisOpt.add(formatted);
		}
	}



	//reconstruct the string at program point quad, using relation logStrings, of form I,Cst,Z
	public static String renderLogMsg(Quad quad, ProgramRel logStrings, ProgramRel dataDep,
			boolean addOptName) {
		RelView constStrs = logStrings.getView();
		constStrs.selectAndDelete(0, quad);
		String[] wordsByPos = new String[DomK.MAXZ];

		int maxFilled = -1;
		if(constStrs.size() == 0)
			return PURE_DYNAMIC;

		for(Pair<String,Integer> t: constStrs.<String,Integer>getAry2ValTuples()) {
			int i = t.val1;
			if(wordsByPos[i] == null)
				wordsByPos[i] = t.val0;
			else 
				wordsByPos[i] = wordsByPos[i]+"|"+t.val0;
			maxFilled = Math.max(maxFilled, i);
		}
		RelView dataDepV = dataDep.getView();
		dataDepV.selectAndDelete(0, quad);

		if(addOptName) {
			for(Pair<Integer,String> t: dataDepV.<Integer,String>getAry2ValTuples()) {
				int i = t.val0;
				String optStr =  "[" + t.val1 + "]";

				if(wordsByPos[i] == null)
					wordsByPos[i] = optStr;
				else 
					wordsByPos[i] = wordsByPos[i]+"|"+optStr;
				maxFilled = Math.max(maxFilled, i);
			}
			dataDepV.free();
		}

		StringBuilder sb = new StringBuilder();
		for(int i =0; i < maxFilled+1 ; ++ i) {
			if(wordsByPos[i] != null)
				sb.append(wordsByPos[i]);
			else
				sb.append(" X ");
		}
		constStrs.free();
		return sb.toString();
	}

}
