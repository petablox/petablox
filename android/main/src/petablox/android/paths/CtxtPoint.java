package petablox.android.paths;

import petablox.analyses.alias.Ctxt;

public abstract class CtxtPoint implements Point {
    public final Ctxt ctxt;

    public CtxtPoint(Ctxt ctxt) {
    	this.ctxt = ctxt;
    }
}
