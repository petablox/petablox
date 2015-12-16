package chord.analyses.inst;

import soot.Local;
import soot.RefLikeType;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ClassConstant;
import soot.jimple.internal.JAssignStmt;
import chord.program.visitors.IMoveInstVisitor;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,v,t) such that method m
 * contains a statement of the form <tt>v = t.class/tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "MclsValAsgnInst",
    sign = "M0,V0,T0:M0_V0_T0"
)
public class RelMclsValAsgnInst extends ProgramRel
        implements IMoveInstVisitor {
    private SootMethod ctnrMethod;
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        ctnrMethod = m;
    }
    public void visitMoveInst(Unit q) {
    	if(q instanceof JAssignStmt){
    		JAssignStmt j = (JAssignStmt)q;
    		Value left = j.leftBox.getValue();
    		Value right = j.rightBox.getValue();
    		if(right instanceof ClassConstant){
    			ClassConstant cc = (ClassConstant)right;
    			String s = cc.getValue();
    			if (s.startsWith("["))
                    s = Program.typesToStr(s);
    			RefLikeType t = Program.g().getClass(s);
    			assert t != null : s + "@" + ctnrMethod;
    			Local l = (Local)left;
    			add(ctnrMethod, l, t);
    		}
    	}
    }
}
