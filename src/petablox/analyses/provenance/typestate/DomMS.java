package petablox.analyses.provenance.typestate;

import java.util.Set;

import petablox.project.analyses.ProgramDom;
import soot.Local;

/**
 * Domain of typestate must sets.
 * <p>
 * The 0th element in this domain denotes the distinguished empty must set.
 * 
 * @author Ravi Mangal
 */
public class DomMS extends ProgramDom<Set<Local>> {

}
