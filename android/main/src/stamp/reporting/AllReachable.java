package stamp.reporting;

import shord.project.ClassicProject;
import shord.project.analyses.ProgramRel;
import soot.SootMethod;
import java.util.*;

public class AllReachable extends XMLReport
{
    public AllReachable()
	{
		super("Methods");
    }

    public void generate()
	{
        final ProgramRel relReachable = (ProgramRel) ClassicProject.g().getTrgt("out_reachableM");
		relReachable.load();

		Iterable<SootMethod> it = relReachable.getAry1ValTuples();
		for(SootMethod meth : it){
			makeOrGetPkgCat(meth.getDeclaringClass()).newTuple()
				.addValue(meth);
		}
		relReachable.close();
    }
}
