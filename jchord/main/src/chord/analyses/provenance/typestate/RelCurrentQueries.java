package chord.analyses.provenance.typestate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
		name = "currentQueries",
		consumes = { "allTypestateQueries" },
		sign = "P0,H0:P0_H0"
		)
public class RelCurrentQueries extends ProgramRel {	
	ProgramRel relAllTypestateQueries;

	@Override
	public void fill() {
		List<Pair<Quad,Quad>> allQueries = new ArrayList<Pair<Quad,Quad>>();
    	ProgramRel relAllQueries= (ProgramRel) ClassicProject.g().getTrgt("allTypestateQueries");
    	relAllQueries.load();
    	Iterable<Pair<Quad,Quad>> tuples = relAllQueries.getAry2ValTuples();
    	for(Pair<Quad,Quad> p : tuples){
    		allQueries.add(p);
    	}
    	relAllQueries.close();
    	
    	Collections.shuffle(allQueries);
    	int numQueries = Integer.getInteger("chord.provenance.typestateQueries", 500);
    	for(int i = 0; i < numQueries && i < allQueries.size(); i++){
    		Pair<Quad,Quad> chosenQuery = allQueries.get(i);
    		add(chosenQuery.val0,chosenQuery.val1);
    	}
	}
}
