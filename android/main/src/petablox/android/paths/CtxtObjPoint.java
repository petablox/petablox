package petablox.android.paths;

import petablox.analyses.alias.Ctxt;
import petablox.util.soot.SootUtilities;

public class CtxtObjPoint extends CtxtPoint {

	public CtxtObjPoint(Ctxt ctxt) {
		// TODO: Check that it's a contextified object, rather than a call
		// stack.
		super(ctxt);
	}

	@Override
	public String toString() {
		return ctxt.toString(true);
	}

	@Override
	public String toShortString() {
		return SootUtilities.toByteLocStr(ctxt.getElems()[0]);
	}
}
