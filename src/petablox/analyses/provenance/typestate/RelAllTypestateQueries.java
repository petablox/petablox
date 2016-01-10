package petablox.analyses.provenance.typestate;

import petablox.analyses.alias.CICGAnalysis;
import petablox.analyses.alias.CIPAAnalysis;
import petablox.analyses.alias.ICICG;
import petablox.project.ClassicProject;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;
import soot.Local;
import soot.SootMethod;
import soot.Unit;

/**
 * @author Ravi Mangal
 */
@Petablox(
    name = "allTypestateQueries",
	consumes = { "checkIncludedI","initTrackedH"},
    sign = "P0,H0:P0_H0"
)
public class RelAllTypestateQueries extends ProgramRel {
	CIPAAnalysis cipa;
	CICGAnalysis cicgAnalysis;
	ICICG cicg;
	ProgramRel relInitTrackedH;
	ProgramRel relCheckIncludedI;
	
    @Override
    public void fill() {
    	
    	String cipaName = System.getProperty("petablox.typestate.cipa", "cipa-java");
        cipa = (CIPAAnalysis) ClassicProject.g().getTask(cipaName);
        ClassicProject.g().runTask(cipa);
        
        cicgAnalysis = (CICGAnalysis) ClassicProject.g().getTask("cicg-java");
		ClassicProject.g().runTask(cicgAnalysis);
		cicg = cicgAnalysis.getCallGraph();
        
        relInitTrackedH = (ProgramRel) ClassicProject.g().getTrgt("initTrackedH");
        relInitTrackedH.load();
        
        relCheckIncludedI = (ProgramRel) ClassicProject.g().getTrgt("checkIncludedI");
        relCheckIncludedI.load();
        Iterable<Unit> tuples = relCheckIncludedI.getAry1ValTuples();
		for (Unit q : tuples) {
			if(SootUtilities.isVirtualInvoke(q) || SootUtilities.isInterfaceInvoke(q))
			{
				Local v = (Local)SootUtilities.getInstanceInvkBase(q);
				for (Unit h: cipa.pointsTo(v).pts) {
					if (relInitTrackedH.contains(h)) {
						for (SootMethod m : cicg.getTargets(q)) {
							if (isInterestingMethod(m, h, q)) {
								add(q, h);
								continue;
							}
						}
					}
				}
			}
		}
		relCheckIncludedI.close();
		relInitTrackedH.close();
    }
    
    public boolean isInterestingMethod(SootMethod m, Unit allocSite, Unit invoke) {
		if (m.isStatic() || m.toString().equals("<init>:()V@java.lang.Object"))
			return false;
		if (relCheckIncludedI.contains(invoke)) {
			return true;
		}
		return false;
	}
}
