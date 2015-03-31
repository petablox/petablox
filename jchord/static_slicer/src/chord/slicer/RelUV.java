package chord.slicer;

import java.util.Iterator;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.var.DomV;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation that relates registers in domV to domU.
 * @author sangmin
 *
 */
@Chord(
		name = "UV",
		sign = "U0,V0:U0_V0"
)

public class RelUV extends ProgramRel {
	
	public void fill() {
		DomV domV = (DomV) doms[1];
		
		for(Iterator<Register> iter = domV.iterator(); iter.hasNext() ;){			
			Register r = iter.next();
			add(r, r);			
		}		
		
	}
}
