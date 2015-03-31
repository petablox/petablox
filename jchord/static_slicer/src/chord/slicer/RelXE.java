package chord.slicer;

import java.util.Iterator;

import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Trio;

/**
 * Relation containing tuples (x,e) such that x is associated 
 * with a heap location accessed by e.
 * @author sangmin
 *
 */
@Chord(
		name = "XE",
		sign = "X0,E0:X0_E0"
)
public class RelXE extends ProgramRel {
	
	public void fill(){
		DomX domX = (DomX)doms[0];
		
		Iterator<Trio<Object, Inst, Integer>> iter = domX.iterator();
		while(iter.hasNext()){			
			Trio<Object, Inst, Integer> x = iter.next();
			Object o = x.val0;
			if(o instanceof Quad){
				Quad quad = (Quad)o;
				add(x, quad);
			}			
		}		
		
	}
}
