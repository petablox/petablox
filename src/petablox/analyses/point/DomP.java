package petablox.analyses.point;

import java.util.Iterator;

import soot.SootMethod;
import soot.toolkits.graph.Block;
import soot.Unit;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceFileTag;
import petablox.analyses.method.DomM;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.analyses.ProgramDom;
import petablox.util.soot.CFG;
import petablox.util.soot.SootUtilities;


/**
 * Domain of quads.
 * <p>
 * The 0th element in this domain is the unique entry basic block of the main method of the program.
 * <p>
 * The quads of each method in the program are assigned contiguous indices in this domain, with the
 * unique basic blocks at the entry and exit of each method being assigned the smallest and largest
 * indices, respectively, of all indices assigned to quads in that method.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name = "P", consumes = { "M" })
public class DomP extends ProgramDom<Unit> {
    protected DomM domM;

    @Override
    public void fill() {
        domM = (DomM) (Config.classic ?  ClassicProject.g().getTrgt("M") : consumes[0]);
        int numM = domM.size();
        for (int mIdx = 0; mIdx < numM; mIdx++) {
            SootMethod m = domM.get(mIdx);
            if (m.isAbstract())
                continue;
            CFG cfg = SootUtilities.getCFG(m);
            for (Block bb : cfg.reversePostOrder()) {
            	Iterator<Unit> uit = bb.iterator();
            	while(uit.hasNext()){
            		Unit u = uit.next();
            		add(u);
            	}  
            }
        }
    }

    @Override
    public String toUniqueString(Unit u) {
        String x = Integer.toString(SootUtilities.getID((Unit) u));                  
        return x + "!" + SootUtilities.getMethod(u);
    	//return "";
    }

    @Override
    public String toXMLAttrsString(Unit u) {
        SootMethod m = SootUtilities.getMethod(u);
        String file = ((SourceFileTag)m.getDeclaringClass().getTags().get(0)).getSourceFile();
        int line = ((LineNumberTag)u.getTag("LineNumberTag")).getLineNumber();
        int mIdx = domM.indexOf(m);
        return "file=\"" + file + "\" " + "line=\"" + line + "\" " + "Mid=\"M" + mIdx + "\"";
    	//return "";
    }
}
