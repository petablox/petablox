package petablox.analyses.escape.hybrid.full;

import soot.SootField;

public class FldObj {
    public final SootField f;
    public final boolean isLoc;
    public final boolean isEsc;
    public FldObj(SootField f, boolean isLoc, boolean isEsc) {
        this.f = f;
        // to optimize space, forbid storing Obj.EMTY
        assert (isLoc || isEsc);
        this.isLoc = isLoc;
        this.isEsc = isEsc;
    }
    public boolean equals(Object o) {
        if (!(o instanceof FldObj))
            return false;
        FldObj that = (FldObj) o;
        return that.f == f && that.isLoc == isLoc && that.isEsc == isEsc;
    }
    public int hashCode() {
        return f == null ? 0 : f.hashCode();
    }
}
