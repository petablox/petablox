package petablox.android.srcmap;

/**
 * @author Saswat Anand
 * @author Osbert Bastani
 * 
 * Represents an expression in program source code.
 */
public class Expr {
	private int start;
	private int length;
	private int line;
	private String text;
	private String type;
	
	public Expr(int start, int length, int line, String text, String type) {
		this.start = start;
		this.length = length;
		this.line = line;
		this.text = text;
		this.type = type;
	}

	public int start() { return start; }

	public int length() { return length; }
	
	public String text() { return text; }
	
	public int line() { return line; }
	
	public String type() { return type; }

	public String toString() {
		return "<"+text+","+start+","+length+","+line+","+type+">";
	}
	
	public int hashCode() {
		return start + length + line + text.hashCode() + 
			(type == null ? 0 : type.hashCode());
	}
	
	public boolean equals(Object other) {
		if(!(other instanceof Expr))
			return false;
		Expr e = (Expr) other;
		boolean res = start == e.start && length == e.length && line == e.line && text.equals(e.text);
		if(res) 
			return type == null ? e.type == null : type.equals(e.type);
		else
			return false;
	}
}
