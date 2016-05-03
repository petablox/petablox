package petablox.android.paths;

import petablox.analyses.alias.Ctxt;
import petablox.android.analyses.VarNode;

public class CtxtVarPoint extends CtxtPoint {
	public final VarNode var;

	public CtxtVarPoint(Ctxt ctxt, VarNode var) {
		// TODO: Check that it's a call stack, rather than a contextified
		// object.
		super(ctxt);
		// TODO: Check that it's a valid context for this variable.
		this.var = var;
	}

	@Override
	public String toString() {
		return ctxt.toString() + ":" + var.toString();
	}

	@Override
	public String toShortString() {
		return var.toString();
	}
}
