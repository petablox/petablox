package chord.slicer;

import java.util.Iterator;

import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Trio;

/**
 * Relation containing tuples (x,u) such that x is associated 
 * with a register u.
 * @author sangmin
 *
 */
@Chord(
		name = "XU",
		sign = "X0,U0:U0_X0"
)
public class RelXU extends ProgramRel {

	public void fill(){
		DomX domX = (DomX)doms[0];
		
		Iterator<Trio<Object, Inst, Integer>> iter = domX.iterator();
		while(iter.hasNext()){		
			Trio<Object, Inst, Integer> x = iter.next();
			Object o = x.val0;
			if(o instanceof Register){
				Register reg = (Register)o;
				add(x, reg);
			}			
		}	
	}
}
