package petablox.analyses.escape;

import petablox.analyses.heapacc.DomE;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;

@Petablox(name = "queryE", sign = "E0:E0", consumes = { "checkExcludedE" })
public class RelQueryE extends ProgramRel {
    @Override
    public void fill() {
        ProgramRel relCheckExcludedE = (ProgramRel) ClassicProject.g().getTrgt("checkExcludedE");
        relCheckExcludedE.load();
        DomE domE = (DomE) doms[0];
        for (Unit q : domE) {
        	if(q instanceof JAssignStmt){
        		JAssignStmt jas = (JAssignStmt)q;
        		if(SootUtilities.isStaticGet(jas) || SootUtilities.isStaticPut(jas))
        			continue;
        	}
            if (!relCheckExcludedE.contains(q))
                add(q);
        }
        relCheckExcludedE.close();
    }
}
