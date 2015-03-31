package chord.analyses.typestate.metaback;

import chord.analyses.var.DomV;
import chord.project.analyses.metaback.dnf.DNFFactory;
import chord.project.analyses.metaback.dnf.Domain;
import chord.project.analyses.metaback.dnf.Variable;

public class TSDNFFactory implements DNFFactory {
	public static DomV domV;
	private final static TSDNFFactory singleton = new TSDNFFactory();

	private TSDNFFactory() {
	}

	public static void setDomV(DomV dom) {
		domV = dom;
	}

	public static TSDNFFactory getSingleton() {
		return singleton;
	}

	@Override
	public Domain genDomainFromStr(String str) {
		if (str.equals("T"))
			return TSBoolDomain.T();
		if (str.equals("F"))
			return TSBoolDomain.F();
		throw new RuntimeException("Unrecoginized string for domain: " + str);
	}

	@Override
	public Variable genVarFromStr(String str) {
		int idx = Integer.parseInt(str);
		return new TSPVariable(idx, domV);
	}

}
