package petablox.analyses.alias;

import java.util.Set;
import java.io.Serializable;
import soot.Unit;

/**
 * Representation of an object-insensitive abstract object.
 * <p>
 * It is a set of object allocation sites.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class CIObj implements Serializable {
    public final Set<Unit> pts;
    public CIObj(Set<Unit> pts) {
        assert (pts != null);
        this.pts = pts;
    }
    /**
     * Determines whether this abstract object may alias with a given abstract object.
     * 
     * @param that An abstract object.
     * 
     * @return true iff this abstract object may alias with the given abstract object.
     */
    public boolean mayAlias(CIObj that) {
        for (Unit e : pts) {
            if (that.pts.contains(e))
                return true;
        }
        return false;
    }
    public int hashCode() {
        return pts.hashCode();
    }
    public boolean equals(Object that) {
        if (that instanceof CIObj)
            return pts.equals(((CIObj) that).pts);
        return false;
    }
    public String toString() {
        String s = "[";
        for (Unit e : pts) {
            s += " " + e;
        }
        return s + " ]";
    }
}
