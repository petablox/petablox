package chord.analyses.type;

import soot.NullType;
import soot.RefLikeType;
import soot.RefType;
import chord.analyses.invk.StubRewrite;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.IndexSet;
import chord.util.soot.SootUtilities;

/**
 * Relation containing each tuple (s,t) such that type s is a
 * subtype of type t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "sub",
    sign = "T1,T0:T0_T1"
)
public class RelSub extends ProgramRel {
	public static final NullType NULL_TYPE = NullType.v();
	
    public void fill() {
        Program program = Program.g();
        IndexSet<RefLikeType> classes = program.getClasses();
        for (RefLikeType t1 : classes) {
            //Add NULL_TYPE as a subclass of all classes
            add(NULL_TYPE,t1);
            
            RefLikeType stubForT1 = StubRewrite.fakeSubtype(t1);
            if(stubForT1 == null)
                stubForT1 = t1;
            add(t1, stubForT1); //a pointer-to-stub should be able to point to a base alloc site
            for (RefLikeType t2 : classes) {
                if (SootUtilities.isSubtypeOf(t1,t2)) {
                    add(t1, t2);
                    add(stubForT1, t2); //a stub implements or extends everything that the concrete class does.
                }
            }
        }
    }
}
