package petablox.analyses.reflect;

import java.util.List;

import petablox.analyses.alloc.DomH;
import petablox.analyses.invk.DomI;
import petablox.analyses.method.DomM;
import petablox.program.Program;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;
import petablox.util.tuple.object.Pair;
import soot.RefLikeType;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

/**
 * Analysis producing relations objNewInstIH and objNewInstIM.
 *
 * <ul>
 *   <li>objNewInstIH: Relation containing each tuple (i,h) such that call site i
 *       calling method {@code Object newInstance()} defined in class
 *       {@code java.lang.Class} is treated as object allocation site h.</li>
 *   <li>objNewInstIM: Relation containing each tuple (i,m) such that call site i
 *       calling method {@code Object newInstance()} defined in class 
 *       {@code java.lang.Class} is treated as calling the nullary constructor m
 *       on the freshly created object.</li>
 * </ul>
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name         = "objNewInst-java",
    consumes     = { "I", "H", "M" },
    produces     = { "objNewInstIH", "objNewInstIM" },
    namesOfSigns = { "objNewInstIH", "objNewInstIM" },
    signs        = { "I0,H0:I0_H0", "I0,M0:I0xM0" }
)
public class ObjNewInstAnalysis extends JavaAnalysis {
    @Override
    public void run() {
        ProgramRel relObjNewInstIH = (ProgramRel) ClassicProject.g().getTrgt("objNewInstIH");
        ProgramRel relObjNewInstIM = (ProgramRel) ClassicProject.g().getTrgt("objNewInstIM");
        relObjNewInstIH.zero();
        relObjNewInstIM.zero();
        DomI domI = (DomI) ClassicProject.g().getTrgt("I");
        DomH domH = (DomH) ClassicProject.g().getTrgt("H");
        DomM domM = (DomM) ClassicProject.g().getTrgt("M");
        List<Pair<Unit, List<RefLikeType>>> l =
            Program.g().getReflect().getResolvedObjNewInstSites();
        for (Pair<Unit, List<RefLikeType>> p : l) {
			Unit q = p.val0;
            int iIdx = domI.indexOf(q);
            if(iIdx < 0)
                System.err.println("ObjNewInstAnalysis can't resolve quad " + q + " in " + SootUtilities.getMethod(q) + " " + SootUtilities.getMethod(q).getDeclaringClass());
            assert (iIdx >= 0);
            int hIdx = domH.indexOf(q);
            assert (hIdx >= 0);
            relObjNewInstIH.add(iIdx, hIdx);
            for (RefLikeType r : p.val1) {
                if (r instanceof RefType) {
                	RefType r1 = (RefType)r;
                	SootClass c = r1.getSootClass();
                	List<SootMethod> meths = c.getMethods();
					try{
						SootMethod m = c.getMethod("void <init>()");
						int mIdx = domM.indexOf(m);
						if (mIdx >= 0)
							   relObjNewInstIM.add(iIdx, mIdx);
						else{
							System.out.println("RelObjNewInstIM: Method "+m+" not found in DomM");
						}
					}catch(Exception e){
						System.out.println("RelObjNewInstIM Could not get method init from "+c);
					}
                }
            }
        }
        relObjNewInstIH.save();
        relObjNewInstIM.save();
    }
}

