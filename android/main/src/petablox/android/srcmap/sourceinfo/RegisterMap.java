package petablox.android.srcmap.sourceinfo;

import java.util.Map;
import java.util.Set;

import soot.Local;
import petablox.android.srcmap.Expr;

/**
 * @author Saswat Anand 
 */
public interface RegisterMap {
	public Set<Expr> srcLocsFor(Local var);
	public Map<Local,Set<Expr>> allSrcLocs();
}
