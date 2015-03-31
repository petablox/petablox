package chord.analyses.var;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.method.DomM;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramDom;

/**
 * Domain of local variables of reference type.
 * <p>
 * Each local variable declared in each block of each method is represented by a unique element in this domain.
 * Local variables that have the same name but are declared in different methods or in different blocks of the
 * same method are represented by different elements in this domain.
 * <p>
 * The set of local variables of a method is the disjoint union of its argument variables and its temporaries.
 * All local variables of the same method are assigned contiguous indices in this domain.  The argument variables
 * are assigned contiguous indices in order followed by the temporaries.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "V", consumes = { "M" })
public class DomV extends ProgramDom<Register> implements IMethodVisitor {
    protected DomM domM;
    protected Map<Register, jq_Method> varToMethodMap;

    public jq_Method getMethod(Register v) {
        return varToMethodMap.get(v);
    }

    @Override
    public void init() {
        domM = (DomM) (Config.classic ? ClassicProject.g().getTrgt("M") : consumes[0]);
        varToMethodMap = new HashMap<Register, jq_Method>();
    }

    @Override
    public void visit(jq_Class c) { }

    @Override
    public void visit(jq_Method m) {
        if (m.isAbstract())
            return;
        List<Register> vars = m.getLiveRefVars();
        for (Register v : vars) {
            varToMethodMap.put(v, m);
            add(v);
        }
    }

    @Override
    public String toUniqueString(Register v) {
        return v + "!" + getMethod(v);
    }

    @Override
    public String toXMLAttrsString(Register v) {
        jq_Method m = getMethod(v);
        int mIdx = domM.indexOf(m);
        String file = m.getDeclaringClass().getSourceFileName();
        List<Integer> lineArr = m.getLineNumber(v);
        String line = "";
        if (lineArr == null) {
            line = String.valueOf(m.getLineNumber(0));
        } else {
            for (int vline : lineArr) {
                if (!line.equals("")) line += ",";
                line += vline;
            }
        }
        return "file=\"" + file + "\" " + "line=\"" + line + "\" " + 
            "name=\"" + getMethod(v).getRegName(v) + "\" " + "Mid=\"M" + mIdx + "\"";
    }
}
