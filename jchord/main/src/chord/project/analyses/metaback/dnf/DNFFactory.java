package chord.project.analyses.metaback.dnf;


/**
 * A helper class to generate DNF related classes from object, acting as a decoder.
 * @author xin
 *
 */
public interface DNFFactory {
 public Domain genDomainFromStr(String str);
 public Variable genVarFromStr(String str);
}
