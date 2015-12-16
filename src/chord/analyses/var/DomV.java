package chord.analyses.var;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Iterator;

import soot.SootClass;
import soot.SootMethod;
import soot.Local;
import soot.RefLikeType;
import soot.util.Chain;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceFileTag;
import chord.analyses.method.DomM;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramDom;
import chord.util.Utils;
import chord.util.soot.SootUtilities;

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
public class DomV extends ProgramDom<Local> implements IMethodVisitor {
    protected DomM domM;
    protected Map<Local, SootMethod> varToMethodMap;

    public SootMethod getMethod(Local v) {
        return varToMethodMap.get(v);
    }

    @Override
    public void init() {
        domM = (DomM) (Config.classic ? ClassicProject.g().getTrgt("M") : consumes[0]);
        varToMethodMap = new HashMap<Local, SootMethod>();
    }

    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) {
        if (m.isAbstract())
            return;
        Chain<Local> vars = m.retrieveActiveBody().getLocals();
        Iterator<Local> varsIt = vars.iterator();
        while (varsIt.hasNext()) {
        	Local v=varsIt.next();
        	if(v.getType() instanceof RefLikeType){
        		varToMethodMap.put(v, m);
            	add(v);
        	}
        }
    }

    @Override
    public String toUniqueString(Local v) {
        return v + "!" + getMethod(v);
    }

    @Override
    public String toFIString(Local v) {
    	StringBuilder sb = new StringBuilder();
    	boolean printId = Utils.buildBoolProperty("chord.printrel.printID", false);
    	if(printId) sb.append("(" + indexOf(v) + ")");
    	sb.append("LCL:" + getMethod(v).getName() + "@" + getMethod(v).getDeclaringClass().getName());
    	return sb.toString();
    }
    
    @Override
    public String toXMLAttrsString(Local v) {
        SootMethod m = getMethod(v);
        int mIdx = domM.indexOf(m);
        String file =((SourceFileTag)m.getDeclaringClass().getTags().get(0)).getSourceFile();
        List<Integer> lineArr = SootUtilities.getLineNumber(m,v);
        String line = "";
        if (lineArr == null) {
            line = String.valueOf(SootUtilities.getLineNumber(m,0));
        } else {
            for (int vline : lineArr) {
                if (!line.equals("")) line += ",";
                line += vline;
            }
        }
        return "file=\"" + file + "\" " + "line=\"" + line + "\" " + 
            "name=\"" + SootUtilities.getRegName(getMethod(v),v) + "\" " + "Mid=\"M" + mIdx + "\"";
    }
}
