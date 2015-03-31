/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */

package chord.analyses.primtrack;

import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.analyses.invk.DomI;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (i,z,v) such that local variable v
 * is the zth argument variable of method invocation statement i.
 *
 */
@Chord(
	name = "IinvkPrimArg",
	sign = "I0,Z0,U0:I0_U0_Z0"
)
public class RelIinvkPrimArg extends ProgramRel {
	public void fill() {
		DomI domI = (DomI) doms[0];
		DomU domU = (DomU) doms[2];
		int numI = domI.size();
		for (int iIdx = 0; iIdx < numI; iIdx++) {
			Quad q = (Quad) domI.get(iIdx);
			ParamListOperand l = Invoke.getParamList(q);
			int numArgs = l.length();
			for (int zIdx = 0; zIdx < numArgs; zIdx++) {
				RegisterOperand vo = l.get(zIdx);
				Register v = vo.getRegister();
				if (!v.getType().isReferenceType()) {
					int vIdx = domU.indexOf(v);
					add(iIdx, zIdx, vIdx);
				}
			}
		}
	}
}
