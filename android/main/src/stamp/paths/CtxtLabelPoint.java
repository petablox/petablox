package stamp.paths;

import shord.analyses.Ctxt;

public class CtxtLabelPoint extends CtxtPoint {
	public final String label;

	public CtxtLabelPoint(Ctxt ctxt, String label) {
		super(ctxt);
		this.label = label;
	}

	@Override
	public String toString() {
		return ctxt.toString() + ":" + label;
	}

	@Override
	public String toShortString() {
		return label;
	}
}
