package stamp.reporting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import stamp.analyses.IdentifyCallbackMethods;

import shord.project.ClassicProject;
import shord.project.analyses.ProgramRel;

import soot.SootMethod;

public class PotentialCallbacks extends XMLReport
{
    public PotentialCallbacks()
	{
		super("Possibly-missing Callback Methods");
    }

    public void generate()
	{
		IdentifyCallbackMethods cbAnalysis = new IdentifyCallbackMethods();
		cbAnalysis.analyze();

		ProgramRel relReachableM = (ProgramRel) ClassicProject.g().getTrgt("out_reachableM");
		relReachableM.load();

		//IndexSet<jq_Method> inScopeMethods = program.getMethods();
		for(Map.Entry<SootMethod, List<SootMethod>> entry : cbAnalysis.frameworkMethodToCallbacks().entrySet()){
			SootMethod fmeth = entry.getKey();
			Category cat = null;
			for(SootMethod cb : entry.getValue()){
				boolean flag = true;
				//if(inScopeMethods.contains(cb))
				flag = !relReachableM.contains(cb);
				if(flag){
					if(cat == null)
						cat = makeOrGetPkgCat(fmeth);
					cat.newTuple().addValue(cb, true, "method");
				}
			}
		}
		relReachableM.close();
	}
}
