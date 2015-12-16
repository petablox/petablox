package chord.program;

import soot.RefLikeType;

public class PhantomObjVal {
    public final RefLikeType r;
    public PhantomObjVal(RefLikeType r) {
        assert (r != null);
        this.r = r;
    }
    @Override
    public int hashCode() {
        return r.hashCode();
    }
    @Override
    public boolean equals(Object o) {
        if (o instanceof PhantomObjVal) {
            return ((PhantomObjVal) o).r == this.r;
        }
        return false;
    }
}
