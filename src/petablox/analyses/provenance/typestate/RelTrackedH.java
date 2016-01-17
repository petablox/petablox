package petablox.analyses.provenance.typestate;

import java.util.HashSet;
import java.util.Set;

import petablox.analyses.alloc.DomH;
import petablox.project.ClassicProject;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.tuple.object.Pair;
import soot.Unit;

/*
 * @author Ravi Mangal
 */
@Petablox(
		name = "trackedH",
		consumes = { "checkExcludedH", "currentQueries", "initTrackedH" },
		sign = "H0"
		)
public class RelTrackedH extends ProgramRel {
	private DomH domH;
	ProgramRel relInitTrackedH;
	ProgramRel relCurrentQueries;
	Set<Unit> queryH;

	@Override
	public void fill() {
		domH = (DomH) doms[0];
		queryH = new HashSet<Unit>();
		
		relCurrentQueries = (ProgramRel) ClassicProject.g().getTrgt("currentQueries");
		relCurrentQueries.load();
		Iterable<Pair<Unit, Unit>> tuples = relCurrentQueries.getAry2ValTuples();
        for (Pair<Unit, Unit> p : tuples) {
        	queryH.add(p.val1);
        }
        relCurrentQueries.close();
		
        relInitTrackedH = (ProgramRel) ClassicProject.g().getTrgt("initTrackedH");
        relInitTrackedH.load();
        Iterable<Unit> tuples2 = relInitTrackedH.getAry1ValTuples();
        for (Unit q : tuples2) {
        	if(queryH.contains(q)){
				add(q);
			}
        }		
        relInitTrackedH.close();
	}
}
