/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.primtrack;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.program.visitors.IReturnInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,z,v) such that local variable
 * v is the zth return variable of method m.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "MmethPrimRet",
	sign = "M0,Z0,U0:M0_U0_Z0"
)
public class RelMmethPrimRet extends ProgramRel
		implements IReturnInstVisitor {
	private static Integer ZERO = new Integer(0);
	private jq_Method ctnrMethod;
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		ctnrMethod = m;
	}
	public void visitReturnInst(Quad q) {
		Operand rx = Return.getSrc(q);
		// note: rx is null if this method returns void
		if (rx instanceof RegisterOperand) {
			RegisterOperand ro = (RegisterOperand) rx;
			if (!ro.getType().isReferenceType()) {
				Register v = ro.getRegister();
				add(ctnrMethod, ZERO, v);
			}
		}
	}
}
