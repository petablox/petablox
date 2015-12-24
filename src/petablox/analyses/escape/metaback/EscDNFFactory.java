package petablox.analyses.escape.metaback;

import petablox.analyses.alloc.DomH;
import petablox.project.analyses.metaback.dnf.DNFFactory;
import petablox.project.analyses.metaback.dnf.Domain;
import petablox.project.analyses.metaback.dnf.Variable;

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
