package chord.project.analyses.metaback;

import chord.project.analyses.metaback.dnf.DNF;

/**
 * A helper class to generate the abstraction
 * @author xin
 *
 */
public interface AbstractionFactory {
	public Abstraction genAbsFromNC(DNF dnf);
	public Abstraction genAbsFromStr(String s);
}
