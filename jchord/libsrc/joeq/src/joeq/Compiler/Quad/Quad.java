// Quad.java, created Fri Jan 11 16:42:38 2002 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package joeq.Compiler.Quad;

import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.AStore;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Putstatic;
import jwutil.strings.Strings;

import java.util.ArrayList;
import java.util.AbstractList;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

/**
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: Quad.java,v 1.31 2005/05/05 19:38:13 joewhaley Exp $
 */
public class Quad implements Inst, Serializable {

    /** The operator.  Operator objects are shared across all quads. */
    private Operator operator;
    /** The four operands.  Operands are quad-specific. */
    private Operand operand1, operand2, operand3, operand4;
    /** Id number for this quad.  THIS NUMBER HOLDS NO MEANING WHATSOEVER.  It is just used for printing. */
    private int id_number;
    private BasicBlock bb;

    /** Creates new Quad */
    Quad(int id, BasicBlock b, Operator operator) {
        this.id_number = id; bb = b; this.operator = operator;
    }
    Quad(int id, BasicBlock b, Operator operator, Operand operand1) {
        this.id_number = id; bb = b; this.operator = operator; this.operand1 = operand1;
        if (operand1 != null) operand1.attachToQuad(this);
    }
    Quad(int id, BasicBlock b, Operator operator, Operand operand1, Operand operand2) {
        this.id_number = id; bb = b; this.operator = operator; this.operand1 = operand1; this.operand2 = operand2;
        if (operand1 != null) operand1.attachToQuad(this);
        if (operand2 != null) operand2.attachToQuad(this);
    }
    Quad(int id, BasicBlock b, Operator operator, Operand operand1, Operand operand2, Operand operand3) {
        this.id_number = id; bb = b; this.operator = operator; this.operand1 = operand1; this.operand2 = operand2; this.operand3 = operand3;
        if (operand1 != null) operand1.attachToQuad(this);
        if (operand2 != null) operand2.attachToQuad(this);
        if (operand3 != null) operand3.attachToQuad(this);
    }
    Quad(int id, BasicBlock b, Operator operator, Operand operand1, Operand operand2, Operand operand3, Operand operand4) {
        this.id_number = id; bb = b; this.operator = operator; this.operand1 = operand1; this.operand2 = operand2; this.operand3 = operand3; this.operand4 = operand4;
        if (operand1 != null) operand1.attachToQuad(this);
        if (operand2 != null) operand2.attachToQuad(this);
        if (operand3 != null) operand3.attachToQuad(this);
        if (operand4 != null) operand4.attachToQuad(this); // maybe null guard
    }
    /** These are not intended to be used outside of the joeq.Compiler.Quad package.
     * Instead, use the static accessor methods for each operator, e.g. Move.getDest(quad).
     */
    public Operand getOp1() { return operand1; }
    public Operand getOp2() { return operand2; }
    public Operand getOp3() { return operand3; }
    public Operand getOp4() { return operand4; }
    public void setOp1(Operand op) { operand1 = op; }
    public void setOp2(Operand op) { operand2 = op; }
    public void setOp3(Operand op) { operand3 = op; }
    public void setOp4(Operand op) { operand4 = op; }

    public Quad copy(int id_number) {
        Operand op1 = (operand1!=null)?operand1.copy():null;
        Operand op2 = (operand2!=null)?operand2.copy():null;
        Operand op3 = (operand3!=null)?operand3.copy():null;
        Operand op4 = (operand4!=null)?operand4.copy():null;
        return new Quad(id_number, bb, operator, op1, op2, op3, op4);
    }
    
    private static class OperandList extends AbstractList<Operand> implements List<Operand> {
        private final Operand[] a;
		public OperandList(Operand c) { a = new Operand[] { c }; }
        public OperandList(Operand c1, Operand c2) { a = new Operand[] { c1, c2 }; }
        public OperandList(Operand c1, Operand c2, Operand c3) { a = new Operand[] { c1, c2, c3 }; }
        public OperandList(Operand c1, Operand c2, Operand c3, Operand c4) { a = new Operand[] { c1, c2, c3, c4 }; }
        public OperandList(Operand[] c) { a = c; }
        public int size() { return a.length; }
        public Operand get(int index) { return a[index]; }
        public static final OperandList EMPTY = new OperandList(new Operand[0]);
        public static OperandList getEmptyList() { return EMPTY; }
    }

    public List<Operand> getAllOperands() {
        int k = 0;
        if (operand1 != null) k += 1;
        if (operand2 != null) k += 2;
        if (operand3 != null) k += 4;
        if (operand4 != null) k += 8;
        switch (k) {
            case 0 :
                return OperandList.EMPTY;
            case 1 :
                return new OperandList(operand1);
            case 2 :
                return new OperandList(operand2);
            case 3 :
               return new OperandList(operand1, operand2);
            case 4 :
               return new OperandList(operand3);
            case 5 :
               return new OperandList(operand1, operand3);
            case 6 :
               return new OperandList(operand2, operand3);
            case 7 :
               return new OperandList(operand1, operand2, operand3);
            case 8 :
               return new OperandList(operand4);
            case 9 :
               return new OperandList(operand1, operand4);
            case 10 :
               return new OperandList(operand2, operand4);
            case 11 :
               return new OperandList(operand1, operand2, operand4);
            case 12 :
               return new OperandList(operand3, operand4);
            case 13 :
               return new OperandList(operand1, operand3, operand4);
            case 14 :
               return new OperandList(operand2, operand3, operand4);
            case 15 :
            default:
               return new OperandList(operand1, operand2, operand3, operand4);
        }
    }

    /** Return the operator for this quad. */
    public Operator getOperator() { return operator; }
    
    /** Accepts a quad visitor to this quad.  For the visitor pattern. */
    public void accept(QuadVisitor qv) { this.operator.accept(this, qv); }

    /** Returns the id number of this quad.  THIS NUMBER HOLDS NO MEANING WHATSOEVER.  It is just used for printing. */
    public int getID() { return id_number; }
    
    /** Returns a list of the types of exceptions that this quad can throw.
     * Note that types in this list are not exact, therefore subtypes of the
     * returned types may also be thrown. */
    public List<jq_Class> getThrownExceptions() {
        if (operator == Return.THROW_A.INSTANCE) {
            Operand op = Return.getSrc(this);
            if (op instanceof RegisterOperand) {
                // use the operand type.
            	List<jq_Class> l = new ArrayList<jq_Class>(1);
                l.add((jq_Class)((RegisterOperand)op).getType());
                return l;
            }
        }
        return joeq.Compiler.CompilationState.DEFAULT.getThrownExceptions(this);
    }

    /** Returns a list of the registers defined by this quad. */
    public List<RegisterOperand> getDefinedRegisters() { return this.operator.getDefinedRegisters(this); }
    /** Returns a list of the registers used by this quad. */
    public List<RegisterOperand> getUsedRegisters() { return this.operator.getUsedRegisters(this); }
    
    public jq_Field getField() {
        if (operator instanceof ALoad || operator instanceof AStore)
            return null;
        if (operator instanceof Getfield)
            return Getfield.getField(this).getField();
        if (operator instanceof Putfield)
            return Putfield.getField(this).getField();
        if (operator instanceof Getstatic)
            return Getstatic.getField(this).getField();
        if (operator instanceof Putstatic)
            return Putstatic.getField(this).getField();
        throw new RuntimeException();
    }

	public int getBCI() {
        Map<Quad, Integer> map = getBasicBlock().getMethod().getBCMap();
        if (map != null) {
            Integer bci = map.get(this);
            if (bci != null)
                return bci.intValue();
        }
        return -1;
	}

	@Override
	public jq_Method getMethod() { return bb.getMethod(); }

	@Override
	public BasicBlock getBasicBlock() { return bb; }

	@Override
	public int getLineNumber() {
        int bci = getBCI();
        return (bci == -1) ? 0 : getBasicBlock().getMethod().getLineNumber(bci);
    }

	@Override
	public String toByteLocStr() {
        int bci = getBCI();
        jq_Method method = getBasicBlock().getMethod();
        String mName = method.getName().toString();
        String mDesc = method.getDesc().toString();
        String cName = method.getDeclaringClass().getName();
        return ((bci == -1) ? ("-" + id_number) : bci) +
			"!" + mName + ":" + mDesc + "@" + cName;
	}

	@Override
	public String toJavaLocStr() {
        jq_Class c = getBasicBlock().getMethod().getDeclaringClass();
        String fileName = c.getSourceFileName();
        int lineNumber = getLineNumber();
        return fileName + ":" + lineNumber;
	}

	@Override
	public String toLocStr() {
        return toByteLocStr() + " (" + toJavaLocStr() + ")";
	}

	@Override
    public String toVerboseStr() {
        return toByteLocStr() + " (" + toJavaLocStr() + ") [" + toString() + "]";
    }

    /** Returns a string representation of this quad. */
    public String toString() {
		String s = id_number + ": " + operator.toString();
        if (operand1 == null) {
            if (operand2 == null)
				return s;
            s += " ";
        } else {
            s += " " + operand1.toString();
            if (operand2 == null)
				return s;
            s += ", ";
        }
        s += operand2.toString();
        if (operand3 == null) return s;
        s += ", " + operand3.toString();
        if (operand4 == null) return s;
        s += ", " + operand4.toString();
        return s;
/*
        StringBuffer s = new StringBuffer();
        s.append(Strings.left(Integer.toString(id_number), 4));
        s.append(Strings.left(operator.toString(), 24));
        if (operand1 == null) {
            if (operand2 == null) return s.toString();
            s.append("    \t");
        } else {
            s.append(operand1.toString());
            if (operand2 == null) return s.toString();
            s.append(",\t");
        }
        s.append(operand2.toString());
        if (operand3 == null) return s.toString();
        s.append(",\t");
        s.append(operand3.toString());
        if (operand4 == null) return s.toString();
        s.append(",\t");
        s.append(operand4.toString());
        return s.toString();
*/
    }
    
    /** Returns a short string representation of this quad, without any operands. */
    public String toString_short() {
        StringBuffer s = new StringBuffer();
        s.append(Strings.left(Integer.toString(id_number), 4));
        s.append(operator.toString());
        return s.toString();
    }
    
    public static final boolean DETERMINISTIC = false;
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        if (DETERMINISTIC) return getID();
        else return System.identityHashCode(this);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object that) {
        return this == that;
    }
    
}
