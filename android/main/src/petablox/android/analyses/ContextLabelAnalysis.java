package petablox.android.analyses;

import petablox.analyses.alias.Ctxt;

import petablox.project.ClassicProject;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;

import soot.SootMethod;

import petablox.bddbddb.Rel.RelView;
import petablox.project.Petablox;
import petablox.util.tuple.object.Pair;
import petablox.util.tuple.object.Trio;

@Petablox(name = "context-label-java",
	   consumes = { "InLabelArg", "InLabelRet", "OutLabelArg", "OutLabelRet", "CM" },
	   produces = { "CLbl", "CCL", "LCL" },
	   namesOfTypes = { "CLbl" },
	   types = { DomCLbl.class },
	   namesOfSigns = { "CCL", "LCL" },
	   signs = { "C0,CLbl0:C0_CLbl0", "Lbl0,CLbl0:Lbl0_CLbl0" }
	   )
public class ContextLabelAnalysis extends JavaAnalysis
{
	private ProgramRel relCM;
	private DomCLbl domCL;

	public void run()
	{
        domCL = (DomCLbl) ClassicProject.g().getTrgt("CLbl");
		relCM = (ProgramRel) ClassicProject.g().getTrgt("CM");
		relCM.load();

		CL();
		
		relCM.close();

		ProgramRel relCCL = (ProgramRel) ClassicProject.g().getTrgt("CCL");
		relCCL.zero();
		ProgramRel relLCL = (ProgramRel) ClassicProject.g().getTrgt("LCL");
		relLCL.zero();
		
		int numCL = domCL.size();
		for(int clIdx = 0; clIdx < numCL; clIdx++){
			Pair<String,Ctxt> pair = (Pair<String,Ctxt>) domCL.get(clIdx);
			String label = pair.val0;
			Ctxt ctxt = pair.val1;
			relCCL.add(ctxt, pair);
			relLCL.add(label, pair);
		}
		relCCL.save();
		relLCL.save();
	}

	private void processLabelArg(String relName)
	{
		ProgramRel relLabelArg = (ProgramRel) ClassicProject.g().getTrgt(relName);
		relLabelArg.load();
		Iterable<Trio<String,SootMethod,Integer>> it1 = relLabelArg.getAry3ValTuples();
		for(Trio<String,SootMethod,Integer> trio : it1) {
			String label = trio.val0;
			SootMethod meth = trio.val1;
			
			for(Ctxt ctxt : getContexts(meth)){
				domCL.getOrAdd(new Pair(label,ctxt));
			}
		}
		relLabelArg.close();
	}

	private void processLabelRet(String relName)
	{
		ProgramRel relLabelRet = (ProgramRel) ClassicProject.g().getTrgt(relName);
		relLabelRet.load();
		Iterable<Pair<String,SootMethod>> it2 = relLabelRet.getAry2ValTuples();
		for(Pair<String,SootMethod> pair : it2) {
			String label = pair.val0;
			SootMethod meth = pair.val1;
			
			for(Ctxt ctxt : getContexts(meth)){
				domCL.getOrAdd(new Pair(label,ctxt));
			}
		}
		relLabelRet.close();
	}

	private void CL()
	{
		processLabelArg("InLabelArg");
		processLabelArg("OutLabelArg");

		processLabelRet("InLabelRet");
		processLabelRet("OutLabelRet");

		domCL.save();
		System.out.println("PRT print domCL: "+domCL.toString());
	}

	private Iterable<Ctxt> getContexts(SootMethod meth)
	{
        RelView view = relCM.getView();
        view.selectAndDelete(1, meth);
        return view.getAry1ValTuples();
    }

}
