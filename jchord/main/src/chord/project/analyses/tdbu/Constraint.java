package chord.project.analyses.tdbu;

/**
 * An interface to represent a Constraint. Interfaces added on demand.
 * @author xin
 *
 */
public interface Constraint {

	boolean isFalse();
	
	boolean isTrue();

	Constraint intersect(Constraint other);

	boolean contains(Constraint that);
}
