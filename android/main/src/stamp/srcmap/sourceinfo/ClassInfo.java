package stamp.srcmap.sourceinfo;

import java.util.List;
import java.util.Map;

/**
 * @author Saswat Anand
 */
public interface ClassInfo {
	public abstract int lineNum();
	public abstract MethodInfo methodInfo(String chordSig);
	public abstract int lineNum(String chordMethSig);
	public abstract List<String> aliasSigs(String chordMethSig);
	public abstract Map<String,List<String>> allAliasSigs();
}
