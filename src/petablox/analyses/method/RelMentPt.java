package petablox.analyses.method;

import java.util.HashSet;
import soot.SootMethod;
import petablox.program.Program;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

/**
 * A relation over domain M containing entry points for the program.
 **/
@Petablox(
    name = "MentPt",
    sign = "M0:M0"
)
public class RelMentPt extends ProgramRel {

    @Override
    public void fill() {
        HashSet<SootMethod> entryMethods =  Program.g().getEntryMethods();
        for (SootMethod m: entryMethods) {
            add(m);
        }
    }
}
