package stamp.paths;

import shord.analyses.Ctxt;

public abstract class CtxtPoint implements Point {
    public final Ctxt ctxt;

    public CtxtPoint(Ctxt ctxt) {
    	this.ctxt = ctxt;
    }
}
