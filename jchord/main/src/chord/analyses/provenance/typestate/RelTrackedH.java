package chord.analyses.provenance.typestate;

import java.util.HashSet;
import java.util.Set;

import joeq.Compiler.Quad.Quad;
import chord.analyses.alloc.DomH;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

/*
 * @author Ravi Mangal
 */
@Chord(
		name = "trackedH",
		consumes = { "checkExcludedH", "currentQueries", "initTrackedH" },
		sign = "H0"
		)
public class RelTrackedH extends ProgramRel {
	private DomH domH;
	ProgramRel relInitTrackedH;
	ProgramRel relCurrentQueries;
	Set<Quad> queryH;

	@Override
	public void fill() {
		domH = (DomH) doms[0];
		queryH = new HashSet<Quad>();
		
		relCurrentQueries = (ProgramRel) ClassicProject.g().getTrgt("currentQueries");
		relCurrentQueries.load();
		Iterable<Pair<Quad, Quad>> tuples = relCurrentQueries.getAry2ValTuples();
        for (Pair<Quad, Quad> p : tuples) {
        	queryH.add(p.val1);
        }
        relCurrentQueries.close();
		
        relInitTrackedH = (ProgramRel) ClassicProject.g().getTrgt("initTrackedH");
        relInitTrackedH.load();
        Iterable<Quad> tuples2 = relInitTrackedH.getAry1ValTuples();
        for (Quad q : tuples2) {
        	if(queryH.contains(q)){
				add(q);
			}
        }		
        relInitTrackedH.close();
	}
}
