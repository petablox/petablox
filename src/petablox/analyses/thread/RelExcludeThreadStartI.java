package petablox.analyses.thread;

import soot.SootMethod;
import soot.Unit;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;
import petablox.util.Utils;
import petablox.util.soot.SootUtilities;

@Petablox(name="excludeThreadStartI", sign="I0:I0", consumes = { "threadStartI", "PM" })
public class RelExcludeThreadStartI extends ProgramRel {
    private static final String[] threadExcludeAry;
    static {
        String threadExcludeStr = System.getProperty("chord.thread.exclude", "sun.,java.");
        threadExcludeAry = Utils.toArray(threadExcludeStr);
    }
    @Override
    public void fill() {
        ProgramRel rel = (ProgramRel) ClassicProject.g().getTrgt("threadStartI");
        rel.load();
        Iterable<Unit> tuples = rel.getAry1ValTuples();
        for (Unit q : tuples) {
            String c = SootUtilities.getMethod(q).getDeclaringClass().getName();
            for (String c2 : threadExcludeAry) {
                if (c.startsWith(c2)) {
                    add(q);
                    break;
                }
            }
        }
        rel.close();
    }
}
