package chord.analyses.inst;

import soot.Local;
import soot.RefLikeType;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JAssignStmt;
import soot.shimple.PhiExpr;
import soot.toolkits.graph.Block;
import soot.toolkits.scalar.ValueUnitPair;

import java.util.Iterator;
import java.util.List;

import chord.analyses.point.DomP;
import chord.analyses.var.DomV;
import chord.program.Program;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.soot.CFG;
import chord.util.soot.SootUtilities;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "phi-java",
       consumes = { "P", "V", "Z" },
       produces = { "objPhiSrc!sign=P0,Z0,V0:P0_V0_Z0", "objPhiDst!sign=P0,V0:P0_V0", "objPhiMax!sign=P0,Z0:P0_Z0" }
)
public class PhiInstAnalysis extends JavaAnalysis {
    public void run() {
        DomP domP = (DomP) ClassicProject.g().getTrgt("P");
        DomV domV = (DomV) ClassicProject.g().getTrgt("V");
        ProgramRel relPhiSrc = (ProgramRel) ClassicProject.g().getTrgt("objPhiSrc");
        ProgramRel relPhiDst = (ProgramRel) ClassicProject.g().getTrgt("objPhiDst");
        ProgramRel relPhiMax = (ProgramRel) ClassicProject.g().getTrgt("objPhiMax");
        relPhiSrc.zero();
        relPhiDst.zero();
        relPhiMax.zero();
        for (SootMethod m : Program.g().getMethods()) {
            if (m.isAbstract())
                continue;
            CFG cfg = SootUtilities.getCFG(m);
            for (Block bb : cfg.reversePostOrder()) {
            	Iterator<Unit> itr = bb.iterator();
                while(itr.hasNext()) {
                	Unit q = itr.next();
                    if(q instanceof JAssignStmt){
                    	JAssignStmt j = (JAssignStmt)q;
                    	Value left = j.leftBox.getValue();
                    	Value right = j.rightBox.getValue();
                    	if(!(right instanceof PhiExpr))
                    		continue;
                    	Local l = (Local)left;
                    	Type t = l.getType();
                    	if(t==null || t instanceof RefLikeType){
                    		List<ValueUnitPair> vup = ((PhiExpr)right).getArgs();
                    		int pId = domP.indexOf(q);
            				int n = vup.size();
                            relPhiMax.add(pId, n - 1);
                            int lId = domV.indexOf(l);
                            relPhiDst.add(pId, lId);
                    		for(int zId = 0; zId < n; zId++){
                    			Value v = vup.get(zId).getValue();
                    			if(v instanceof Local){
                    				Local r = (Local)v;
                    				int rId = domV.indexOf(r);
                                    relPhiSrc.add(pId, zId, rId);
                    			}
                    		}
                    	}
                    }
                }
            }
        }
        relPhiSrc.save();
        relPhiDst.save();
        relPhiMax.save();
    }
}
