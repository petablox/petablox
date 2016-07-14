package stamp.srcmap;

/**
 * @author Saswat Anand
 * @author Osbert Bastani
 * 
 * Marker representing an expression.
 *
 */
public class SimpleMarker extends Marker {
	private Expr operand;
	
	public SimpleMarker(int line, String chordSig, String markerType, Expr operand) {
		super(line, chordSig, markerType);
		this.operand = operand;
	}

	public Expr operand() {
		return operand;
	}
}