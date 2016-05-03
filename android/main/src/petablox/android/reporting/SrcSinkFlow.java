package petablox.reporting;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;
import petablox.android.analyses.DomLbl;
import petablox.android.analyses.JCFLSolverAnalysis;
import petablox.analyses.alias.Ctxt;
import petablox.program.Program;
import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;
import soot.Unit;
import soot.jimple.Stmt;
import soot.SootClass;
import soot.SootMethod;

import petablox.bddbddb.Rel.RelView;
import petablox.util.tuple.object.Trio;
import petablox.util.tuple.object.Pair;
import petablox.util.soot.SootUtilities;

/*
 * @author Saswat Anand
 * @author Osbert Bastani
 **/
public class SrcSinkFlow extends XMLReport {
	public SrcSinkFlow() {
		super("Source-to-sink Flows");
	}

	public void generate() {
		final ProgramRel relCtxtFlows = (ProgramRel)ClassicProject.g().getTrgt("flow");
		//final ProgramRel relSrcSinkFlow = (ProgramRel)ClassicProject.g().getTrgt("JSrcSinkFlow");

			relCtxtFlows.load();

		/*
		Iterable<Trio<String,String,Integer>> res = relSrcSinkFlow.getAry3ValTuples();
		for(Trio<String,String,Integer> triple : res) {
		    String source = triple.val0;
		    String sink = triple.val1;
		    int weight = triple.val2;
		    newTuple()
			.addValue(source)
			.addValue(sink)
			.addValue(Integer.toString(weight));
		}
		*/

		//Note: As of 7.9.2013, the first block below is used for non-jcfl stuff. The second block is
		//required instead for Osbert's JCFL flow stuff. They are mutually exclusive.


		Iterable<Pair<Pair<String,Ctxt>,Pair<String,Ctxt>>> res = relCtxtFlows.getAry2ValTuples();
		int count = 0;
		for(Pair<Pair<String,Ctxt>,Pair<String,Ctxt>> pair : res) {
			count++;
			String source = pair.val0.val0;
			Ctxt sourceCtxt = pair.val0.val1;
			String sink = pair.val1.val0;
			Ctxt sinkCtxt = pair.val1.val1;

		//	if(Postmortem.processingSrc){
		//		newTuple()
		//			.addValue(source)
		//			.addValue(sink);
		//	} else {
			Category flowCat = makeOrGetSubCat(source + " -> " + sink);
			Category ctxtFlowCat = flowCat.makeOrGetSubCat("Flow "+count);
	        Tuple srcTuple = ctxtFlowCat.newTuple();//makeOrGetSubCat("context");
	        srcTuple.setAttr("source", source);
	        srcTuple.addValue(ctxtToString(sourceCtxt));

	        Tuple sinkTuple = ctxtFlowCat.newTuple();//makeOrGetSubCat(sink).makeOrGetSubCat("context");
	        sinkTuple.setAttr("sink", sink);
	        sinkTuple.addValue(ctxtToString(sinkCtxt));
	    }

		/*
		// Get all source-to-sink flows, regardless of context. To achieve this, we
		// project on the two context columns.
		RelView flowsView = relCtxtFlows.getView();
		flowsView.delete(2);
		flowsView.delete(0);
		Iterable<Pair<String,String>> flows = flowsView.getAry2ValTuples();
		Set<Pair<String,String>> seen = new HashSet<Pair<String,String>>();
		for (Pair<String,String> f : flows) {
			// Projection doesn't actually remove duplicates, so we have to skip
			// those manually.
			if (!seen.add(f)) {
				continue;
			}
		    String source = f.val0;
		    String sink = f.val1;
			// Collect all context-aware flows between two specific endpoints.
			RelView ctxtFlowsSelection = relCtxtFlows.getView();
			ctxtFlowsSelection.selectAndDelete(3, sink);
			ctxtFlowsSelection.selectAndDelete(1, source);
			int numFlows = ctxtFlowsSelection.size();
		    newTuple()
				.addValue(source)
				.addValue(sink)
				.addValue(Integer.toString(numFlows));
		}
		*/

		/*
		  Map<stamp.jcflsolver.Util.Pair<Integer, Integer>, Integer> src2sink = JCFLSolverAnalysis.getSrc2Sink();
		  DomLbl dom = (DomLbl)ClassicProject.g().getTrgt("L");
		System.out.println("LENGTH:" + src2sink.entrySet().size());
		for(Map.Entry<stamp.jcflsolver.Util.Pair<Integer, Integer>, Integer> entry : src2sink.entrySet()) {
			//if(entry.getValue() > 0) {
				newTuple()
					.addValue(dom.get(entry.getKey().getX()))
					.addValue(dom.get(entry.getKey().getY()))
					.addValue(Integer.toString(entry.getValue()));
			//}
		}
	    */

		relCtxtFlows.close();

	}

	private String ctxtToString(Ctxt context) {
		StringBuilder stmtbuilder = new StringBuilder();
		StringBuilder srcbuilder = new StringBuilder();
		boolean first = true;
		for(Unit unit : context.getElems()) {
			Stmt stmt = (Stmt)unit;

			SootMethod method = SootUtilities.getMethod(stmt);
			SootClass klass = method.getDeclaringClass();
			String filename = sourceInfo.filePath(klass);
			int lineNum = sourceInfo.stmtLineNum(stmt);

			if(!first){
			    stmtbuilder.append("~~");
			    srcbuilder.append("~~");
			}else{
			    first = false;
			}

			/*
			String invkExpr = sourceInfo.srcInvkExprFor(stmt);
			if(invkExpr == null)
			    invkExpr = sourceInfo.javaLocStr(stmt);
			invkExpr = "![CDATA["+invkExpr+"]]";
			*/

			SootMethod callee = stmt.getInvokeExpr().getMethod();
			stmtbuilder.append(callee.getDeclaringClass().getName()+"."+callee.getName());
			stmtbuilder.append("("+filename.substring(filename.lastIndexOf('/')+1)+":"+lineNum+")");			
			srcbuilder.append(filename+' '+lineNum);
		}
		return stmtbuilder.toString() + "~~~" + srcbuilder.toString();
	}
}
