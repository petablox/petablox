/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.primtrack;

import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operator.AStore;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.analyses.var.DomV;
import chord.analyses.method.DomM;
import chord.analyses.field.DomF;

import chord.program.visitors.IHeapInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,b,f,u) such that method m
 * contains a statement of the form <tt>b.f = u</tt>,
 * where u is a primitive.
 *
 */
@Chord(
	name = "MputInstFldPrimInst",
	sign = "M0,V0,F0,U0:F0_M0_V0xU0"
)
public class RelMputInstFldPrimInst extends ProgramRel
		implements IHeapInstVisitor {
	private DomM domM;
	private DomV domV;
  private DomU domU;
	private DomF domF;
	private jq_Method ctnrMethod;
	public void init() {
		domM = (DomM) doms[0];
		domV = (DomV) doms[1];
		domF = (DomF) doms[2];
    domU = (DomU) doms[3];
	}
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		ctnrMethod = m;
	}
	public void visitHeapInst(Quad q) {
		Operator op = q.getOperator();
		if (op instanceof AStore) {
			if (!((AStore) op).getType().isReferenceType()) {
				Operand rx = AStore.getValue(q);
				if (rx instanceof RegisterOperand) {
					RegisterOperand ro = (RegisterOperand) rx;
					Register r = ro.getRegister();
					RegisterOperand bo = (RegisterOperand) AStore.getBase(q);
					Register b = bo.getRegister();
					int mIdx = domM.indexOf(ctnrMethod);
					assert (mIdx != -1);
					int rIdx = domU.indexOf(r);
					assert (rIdx != -1);
					int bIdx = domV.indexOf(b);
					assert (bIdx != -1);
					int fIdx = 0;
					add(mIdx, bIdx, fIdx, rIdx);
				}
			}
			return;
		}
		if (op instanceof Putfield) {
			FieldOperand fo = Putfield.getField(q);
			fo.resolve();
			jq_Field f = fo.getField();
			if (!f.getType().isReferenceType()) {
				Operand rx = Putfield.getSrc(q);
				if (rx instanceof RegisterOperand) {
					Operand bx = Putfield.getBase(q);
					if (bx instanceof RegisterOperand) {
						RegisterOperand bo = (RegisterOperand) bx;
						RegisterOperand ro = (RegisterOperand) rx;
						Register b = bo.getRegister();
						Register r = ro.getRegister();
						int mIdx = domM.indexOf(ctnrMethod);
						assert (mIdx != -1);
						int bIdx = domV.indexOf(b);
						assert (bIdx != -1);
						int rIdx = domU.indexOf(r);
						assert (rIdx != -1);
						int fIdx = domF.indexOf(f);
						if (fIdx == -1) {
							System.out.println("WARNING: MputInstFldInst: method: " +
								ctnrMethod + " quad: " + q);
						} else
							add(mIdx, bIdx, fIdx, rIdx);
					}
				}
			}
		}
	}
}
