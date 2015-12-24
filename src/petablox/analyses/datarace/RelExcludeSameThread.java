package petablox.analyses.datarace;

import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

/**
 * Relation denoting whether races involving the same abstract thread must be checked.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "excludeSameThread",
    sign = "K0:K0"
)
public class RelExcludeSameThread extends ProgramRel {
    public void fill() {
        if (System.getProperty("chord.datarace.exclude.eqth", "true").equals("true"))
            add(1);
        else
        	add(0);
    }
}
