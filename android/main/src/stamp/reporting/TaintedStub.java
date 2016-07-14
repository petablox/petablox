package stamp.reporting;

import shord.project.ClassicProject;
import shord.project.analyses.ProgramRel;
import soot.SootMethod;
import stamp.srcmap.sourceinfo.SourceInfo;

public class TaintedStub extends XMLReport
{
    public TaintedStub() {
		super("Tainted Stub Methods");
	}

    public void generate() {
		final ProgramRel relTaintedStub = (ProgramRel)ClassicProject.g().getTrgt("out_taintedStub");
		relTaintedStub.load();

		Iterable<SootMethod> res = relTaintedStub.getAry1ValTuples();
		for(SootMethod stub : res) {
			makeOrGetPkgCat(stub.getDeclaringClass()).newTuple()
				.addValue(stub);
		}

		relTaintedStub.close();
    }
}
