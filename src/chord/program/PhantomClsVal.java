package chord.program;

import soot.RefLikeType;

public class PhantomClsVal {
    public final RefLikeType r;
    public PhantomClsVal(RefLikeType r) {
        assert (r != null);
        this.r = r;
    }
    @Override
    public int hashCode() {
        return r.hashCode();
    }
    @Override
    public boolean equals(Object o) {
        if (o instanceof PhantomClsVal) {
            return ((PhantomClsVal) o).r == this.r;
        }
        return false;
    }
}
