package petablox.analyses.provenance.typestate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import petablox.project.ClassicProject;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.tuple.object.Pair;
import soot.Unit;

/*
 * @author Ravi Mangal
 */
@Petablox(
		name = "currentQueries",
		consumes = { "allTypestateQueries" },
		sign = "P0,H0:P0_H0"
		)
public class RelCurrentQueries extends ProgramRel {	
	ProgramRel relAllTypestateQueries;

	@Override
	public void fill() {
		List<Pair<Unit,Unit>> allQueries = new ArrayList<Pair<Unit,Unit>>();
    	ProgramRel relAllQueries= (ProgramRel) ClassicProject.g().getTrgt("allTypestateQueries");
    	relAllQueries.load();
    	Iterable<Pair<Unit,Unit>> tuples = relAllQueries.getAry2ValTuples();
    	for(Pair<Unit,Unit> p : tuples){
    		allQueries.add(p);
    	}
    	relAllQueries.close();
    	
    	Collections.shuffle(allQueries);
    	int numQueries = Integer.getInteger("petablox.provenance.typestateQueries", 500);
    	for(int i = 0; i < numQueries && i < allQueries.size(); i++){
    		Pair<Unit,Unit> chosenQuery = allQueries.get(i);
    		add(chosenQuery.val0,chosenQuery.val1);
    	}
	}
}
