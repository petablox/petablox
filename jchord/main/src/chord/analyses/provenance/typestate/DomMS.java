package chord.analyses.provenance.typestate;

import java.util.Set;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.project.analyses.ProgramDom;

/**
 * Domain of typestate must sets.
 * <p>
 * The 0th element in this domain denotes the distinguished empty must set.
 * 
 * @author Ravi Mangal
 */
public class DomMS extends ProgramDom<Set<Register>> {

}
