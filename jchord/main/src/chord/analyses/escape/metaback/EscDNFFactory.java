package chord.analyses.escape.metaback;

import chord.analyses.alloc.DomH;
import chord.project.analyses.metaback.dnf.DNFFactory;
import chord.project.analyses.metaback.dnf.Domain;
import chord.project.analyses.metaback.dnf.Variable;

public class EscDNFFactory implements DNFFactory {
	private final static EscDNFFactory singleton = new EscDNFFactory();
	private static DomH dom;

	public static void setDomH(DomH other) {
		dom = other;
	}

	private EscDNFFactory() { }

	public static EscDNFFactory getSingleton() {
		return singleton;
	}

	@Override
	public Domain genDomainFromStr(String str) {
		return Value.getValue(str);
	}

	@Override
	public Variable genVarFromStr(String str) {
		int idx = Integer.parseInt(str);
		return new EscHVariable(idx,dom);
	}
}
