package chord.project.analyses.metaback.dnf;

import java.util.Set;

/**
 * Represent the possible values of a variable
 * @author xin
 *
 */
public interface Domain {
public int size();
public boolean equals(Domain other);
public String encode();
public Set<Domain> space();
}
