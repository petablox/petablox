package chord.analyses.confdep;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.project.*;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;
import chord.analyses.invk.DomI;
import chord.bddbddb.Rel.RelView;
import java.io.PrintWriter;
import java.util.*;

/**
 * Tracks which invoke statements return outputs with more taints than the args had.
 * Outputs in addedTaints.txt
 * @author asrabkin
 *
 */
@Chord(name="OverTaintFinder", //consumes={"primCdep,refCdep"})
		consumes ={"IargCdep","IretDep"})
public class OverTaintFinder extends JavaAnalysis {

	public void run() {
	  ClassicProject project = ClassicProject.g();
	  DomI domI = (DomI) project.getTrgt("I");
	  
	  ProgramRel IargCdep = (ProgramRel) project.getTrgt("IargCdep");
	  ProgramRel IretDep =  (ProgramRel) project.getTrgt("IretDep");
	  IargCdep.load();
	  IretDep.load();
	  PrintWriter out = OutDirUtils.newPrintWriter("addedTaints.txt");
	  //we're trying to count the number of labels added by an invoke --
	  	//the absolute number added by the call that weren't there before
	  for(Quad q: domI) {
	  	Set<String> taintsAdded = new HashSet<String>();
	  	RelView taintsOut = IretDep.getView();
	  	taintsOut.selectAndDelete(0, q);
	  	for(String label: taintsOut.<String>getAry1ValTuples()) {
	  		taintsAdded.add(label);
	  	}
	  	taintsOut.free();
	  	RelView taintsIn = IargCdep.getView();
	  	taintsIn.selectAndDelete(0, q);

	  	for(Pair<Integer, String> argT: taintsIn.<Integer, String>getAry2ValTuples()) {
	  		taintsAdded.remove(argT.val1);
	  	}
	  	taintsIn.free();
	  	
	  	if(taintsAdded.size() == 0)
	  		continue;
	  	
	  	String pos = q.getMethod().getDeclaringClass()+":" + q.getLineNumber() + " (" + q.getMethod().getName()+")";
	  	out.print(taintsAdded.size());
	  	out.print("\t");
	  	out.print(pos);
	  	jq_Method calledMeth = Invoke.getMethod(q).getMethod();
	  	out.print(" call to " + calledMeth.getDeclaringClass().getName() + " " + calledMeth.getName());
	  	out.print("\tadds");
	  	for(String s:taintsAdded) {
	  		out.print(" ");
	  		out.print(s);
	  	}
	  	out.println();
//	  	Set<String> taintsOut = new HashSet<String>();
	  }
	  IargCdep.close();
	  IretDep.close();
	  out.close();
	}

}
