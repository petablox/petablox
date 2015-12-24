package petablox.analyses.datarace;

import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

/**
 * Relation denoting whether races on accesses in constructor methods must be checked.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "excludeInitMethods",
    sign = "K0:K0"
)
public class RelExcludeInitMethods extends ProgramRel {
    public void fill() {
        if (System.getProperty("chord.datarace.exclude.init", "true").equals("true"))
            add(1);
    }
}
