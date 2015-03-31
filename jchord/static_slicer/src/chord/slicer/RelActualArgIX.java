package chord.slicer;

import java.util.Iterator;

import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Trio;

/**
 * Relation containing tuple (i, x) such that x is an actual-in of a method
 * invocation i.
 * @author sangmin
 *
 */
@Chord(
	name = "actualArgIX",
	sign = "I0,X0:I0_X0"
)
public class RelActualArgIX extends ProgramRel {
	public void fill() {
		DomX domX = (DomX)doms[1];
		for (Trio<Object, Inst, Integer> x : domX) {
			if (x.val2 == 0 && x.val1 instanceof Quad)
				add(x.val1, x);				
		}				
	}
}
