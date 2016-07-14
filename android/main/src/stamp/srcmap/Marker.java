package stamp.srcmap;

/**
 * @author Saswat Anand
 * @author Osbert Bastani
 * 
 * Represents a point in program source code.
 */
public abstract class Marker {
	private int line;
	private String chordSig; //of the field
	private String markerType;
	
	public Marker(int line, String chordSig, String markerType) {
		this.line = line;
		this.chordSig = chordSig;
		this.markerType = markerType;
	}
	
	public int line() { return this.line; }
	public String chordSig() { return this.chordSig; }
	public String markerType() { return this.markerType; }
}