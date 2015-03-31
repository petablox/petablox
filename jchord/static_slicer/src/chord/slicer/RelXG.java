package chord.slicer;

import java.util.Iterator;

import joeq.Class.jq_Field;
import joeq.Compiler.Quad.Inst;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Trio;

/**
 * Relation containing tuples (x,f) such that x is associated 
 * with a static field f.
 * @author sangmin
 *
 */
@Chord(
		name = "XG",
		sign = "X0,F0:F0_X0"
)
public class RelXG extends ProgramRel{
	
	public void fill(){
		DomX domX = (DomX) doms[0];
		
		for (Trio<Object, Inst, Integer> x : domX) {
			Object o = x.val0;
			if(o instanceof jq_Field){
				jq_Field field = (jq_Field)o;
				add(x, field);
			}			
		}		
		
	}

}
