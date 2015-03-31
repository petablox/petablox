package chord.slicer;

import java.util.Iterator;

import joeq.Compiler.Quad.Inst;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Trio;

/**
 * Relation containing tuples (x,p) such that x is associated 
 * with a program point p. If x is actual-in/out, p is a call site.
 * If x is formal-in/out, p is a entry/exit basic block.
 * @author sangmin
 *
 */
@Chord(
		name = "XP",
		sign = "X0,P0:P0_X0"
)
public class RelXP extends ProgramRel {

	public void fill(){
		DomX domX = (DomX)doms[0];
		
		Iterator<Trio<Object, Inst, Integer>> iter = domX.iterator();
		while(iter.hasNext()){
			Trio<Object, Inst, Integer> x = iter.next();
			Inst i = x.val1;
			add(x, i);		
		}	
	}
}
