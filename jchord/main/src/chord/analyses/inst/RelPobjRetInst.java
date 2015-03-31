package chord.analyses.inst;

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
 * Relation containing each tuple (p,v) such that the statement at program
 * point p is a return statement where reference variable v is returned.
 *
 * @author Ravi Mangal
 */
@Chord(name = "PobjRetInst", sign = "P0,V0:P0_V0")
public class RelPobjRetInst extends ProgramRel implements IReturnInstVisitor {
    public void visit(jq_Class c) { }
    public void visit(jq_Method m) { }
    
    @Override
    public void visitReturnInst(Quad q) {
        Operand rx = Return.getSrc(q);
        // note: rx is null if this method returns void
        if (rx instanceof RegisterOperand) {
            RegisterOperand ro = (RegisterOperand) rx;
            if (ro.getType().isReferenceType()) {
                Register v = ro.getRegister();
                add(q, v);
            }
        }
    }
}

