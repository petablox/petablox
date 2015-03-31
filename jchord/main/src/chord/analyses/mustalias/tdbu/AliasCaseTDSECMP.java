package chord.analyses.mustalias.tdbu;

import java.util.Set;

import chord.analyses.typestate.Edge;
import chord.analyses.typestate.EdgeKind;
import chord.project.analyses.tdbu.CaseTDSEComparator;
import chord.project.analyses.tdbu.Constraint;

public class AliasCaseTDSECMP extends CaseTDSEComparator<Edge> {

	public AliasCaseTDSECMP(Set<Edge> tdses) {
		super(tdses);
	}

	@Override
	protected boolean statisfy(Constraint constraint, Edge tdse) {
		if(tdse.type != EdgeKind.FULL )
			return false;
		AliasConstraint acons = (AliasConstraint)constraint;
		return acons.statisfy(tdse.srcNode.ms);
	}

}
