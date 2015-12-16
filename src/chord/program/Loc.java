package chord.program;


import soot.Unit;


/**
 * Representation of the location of a statement.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Loc {
    public final Unit i;
    public final int qIdx;

    public Loc(Unit i, int qIdx) {
        this.i = i;
        this.qIdx = qIdx;
    }

    public int hashCode() { return i.hashCode(); }

    public boolean equals(Object o) {
        if (!(o instanceof Loc)) return false;
        Loc that = (Loc) o;
        return this.i == that.i;
    }

    public String toString() {
        return "<" + i.getTag("LineNumberTag") + ", " + i + ">";
    }
}

