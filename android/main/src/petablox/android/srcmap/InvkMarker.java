package petablox.android.srcmap;

import java.util.*;

public class InvkMarker extends Marker {
	public InvkMarker(int line, String chordSig, String markerType, String text) {
		super(line, chordSig, markerType);
		this.text = text;
	}

	private String text;
	private List<Expr> params = null;
	
	public void addParam(Expr param) {
		if(this.params == null) {
			this.params = new ArrayList<Expr>();
		}
		this.params.add(param);
	}

	public Expr getArg(int i) { 
		if(params.size() == i) {
			//it is the vararg-type param
			//and in the source code in the corresponding
			//invocation instr does not pass anything for this param
			return null;
		}
		return params.get(i); 
	}
	
	public int argCount() { return params.size(); }
	
	public String text() { return text; }

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(super.line() + ": " + super.chordSig() + "(");
		if(params != null){
			for(Expr e : params){
				builder.append((e == null ? "null" : e.toString())+", ");
			}
		}
		builder.append(")");
		return builder.toString();
	}
}
