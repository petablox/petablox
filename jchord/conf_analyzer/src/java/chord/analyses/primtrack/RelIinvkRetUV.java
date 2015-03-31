/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */

package chord.analyses.primtrack;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.invk.DomI;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (i,z,v) such that primitive local variable v
 * is the zth return variable of method invocation statement i.
 *
 * @author Ari Rabkin
 */
@Chord(
	name = "IinvkRetUV",
	sign = "I0,Z0,UV0:I0_UV0_Z0"
)
public class RelIinvkRetUV extends ProgramRel {
	public void fill() {
		DomI domI = (DomI) doms[0];
		DomUV domUV = (DomUV) doms[2];
		int numI = domI.size();
		for (int iIdx = 0; iIdx < numI; iIdx++) {
			Quad q = (Quad) domI.get(iIdx);
			RegisterOperand vo = Invoke.getDest(q);
			if (vo != null) {
				Register v = vo.getRegister();
				int vIdx = domUV.indexOf(v);
				add(iIdx, 0, vIdx);
			}
		}
	}
}
