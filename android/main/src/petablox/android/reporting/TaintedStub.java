package petablox.reporting;

import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;
import soot.SootMethod;
import petablox.android.srcmap.sourceinfo.SourceInfo;

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
