package chord.analyses.primtrack;


import java.util.*;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramDom;

/**
 * The domain of values in a program.
 * 
 * @author asrabkin
 * 
 */
@SuppressWarnings("serial")
@Chord(name = "UV")
public class DomUV extends ProgramDom<Register> implements IMethodVisitor{

  private Map<Register, jq_Method> varToMethodMap;
  private jq_Method ctnrMethod;

  @Override
  public void init() {
    varToMethodMap = new HashMap<Register, jq_Method>();
  }
  public jq_Method getMethod(Register v) {
    return varToMethodMap.get(v);
  }
  
  @Override
  public void visit(jq_Class c) { }
  @Override
    public void visit(jq_Method m) {
        if (m.isAbstract())
            return;
        ctnrMethod = m;
        ControlFlowGraph cfg = m.getCFG();
        RegisterFactory rf = cfg.getRegisterFactory();
        jq_Type[] paramTypes = m.getParamTypes();
        int numArgs = paramTypes.length;
        for (int i = 0; i < numArgs; i++) {
            jq_Type t = paramTypes[i];
            Register v = rf.get(i);
            addVar(v);
        }
        for (BasicBlock bb: cfg.reversePostOrder()) {
            for (Iterator<Quad> it2 = bb.iterator(); it2.hasNext();) {
                Quad q = it2.next();
                process(q.getOp1());
                process(q.getOp2());
                process(q.getOp3());
                process(q.getOp4());
            }
        }
    }
  private void addVar(Register v) {
    varToMethodMap.put(v, ctnrMethod);
    getOrAdd(v);
  }
    private void process(Operand op) {
        if (op instanceof RegisterOperand) {
            RegisterOperand ro = (RegisterOperand) op;
            jq_Type t = ro.getType();
            if (t != null ) {
                Register v = ro.getRegister();
                addVar(v);
            }
        }
    }
  @Override
  public String toUniqueString(Register v) {
    return v + "!" + getMethod(v);
  }

}
