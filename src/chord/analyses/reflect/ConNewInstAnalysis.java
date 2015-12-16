package chord.analyses.reflect;

import java.util.List;

import chord.analyses.alloc.DomH;
import chord.analyses.invk.DomI;
import chord.analyses.method.DomM;
import chord.project.ClassicProject;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.soot.SootUtilities;
import chord.util.tuple.object.Pair;
import soot.RefLikeType;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

/**
 * Analysis producing relations conNewInstIH and conNewInstIM.
 *
 * <ul>
 *   <li>conNewInstIH: Relation containing each tuple (i,h) such that call site i
 *       calling method {@code Object newInstance(Object[])} defined in class 
 *       {@code java.lang.reflect.Constructor} is treated as object allocation
 *       site h.</li>
 *   <li>conNewInstIM: Relation containing each tuple (i,m) such that call site i
 *       calling method {@code Object newInstance(Object[])} defined in class
 *       {@code java.lang.reflect.Constructor} is treated as calling constructor
 *       m on the freshly created object.</li>
 * </ul>
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "conNewInst-java",
    consumes     = { "I", "H", "M" },
    produces     = { "conNewInstIH", "conNewInstIM" },
    namesOfSigns = { "conNewInstIH", "conNewInstIM" },
    signs        = { "I0,H0:I0_H0", "I0,M0:I0xM0" }
)
public class ConNewInstAnalysis extends JavaAnalysis {
    @Override
    public void run() {
        ProgramRel relConNewInstIH = (ProgramRel) ClassicProject.g().getTrgt("conNewInstIH");
        ProgramRel relConNewInstIM = (ProgramRel) ClassicProject.g().getTrgt("conNewInstIM");
        relConNewInstIH.zero();
        relConNewInstIM.zero();
        DomI domI = (DomI) ClassicProject.g().getTrgt("I");
        DomH domH = (DomH) ClassicProject.g().getTrgt("H");
        DomM domM = (DomM) ClassicProject.g().getTrgt("M");
        List<Pair<Unit, List<RefType>>> l =
            Program.g().getReflect().getResolvedConNewInstSites();
        for (Pair<Unit, List<RefType>> p : l) {
            Unit q = p.val0;
            int iIdx = domI.indexOf(q);
            assert (iIdx >= 0) : ("Quad " + SootUtilities.toLocStr(q) + " not found in domain I.");
            int hIdx = domH.indexOf(q);
            assert (hIdx >= 0) : ("Quad " + SootUtilities.toLocStr(q) + " not found in domain H.");
            relConNewInstIH.add(iIdx, hIdx);
            for (RefLikeType r : p.val1) {
                if (r instanceof RefType) {
                	RefType r1 = (RefType)r;
                    SootClass c = r1.getSootClass();
                    List<SootMethod> meths = c.getMethods();
                    for (int i = 0; i < meths.size(); i++) {
                    	SootMethod m = meths.get(i);
                    	if(m.isAbstract() || m.isStatic())
                    		continue;
                        if (m.getName().toString().equals("<init>")) {
                            int mIdx = domM.indexOf(m);
                            if (mIdx >= 0)
                                relConNewInstIM.add(iIdx, mIdx);
                        }
                    }
                }
            }
        }
        relConNewInstIH.save();
        relConNewInstIM.save();
    }
}

