package chord.analyses.mantis;

public class FldInfo {
	public final FldKind kind;
	public final String fldBaseName;
	public final String javaPos;
	public FldInfo(FldKind kind, String fldBaseName, String javaPos) {
		this.kind = kind;
		this.fldBaseName = fldBaseName;
		this.javaPos = javaPos;
	}
}
