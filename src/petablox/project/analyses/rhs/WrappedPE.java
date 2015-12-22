package petablox.project.analyses.rhs;

import petablox.util.Utils;

public class WrappedPE<PE extends IEdge, SE extends IEdge> implements IWrappedPE<PE, SE> {
    private final Object i;
    private final PE pe;
    private IWrappedPE<PE, SE> wpe;
    private IWrappedSE<PE, SE> wse;
    private int len;

    public WrappedPE(Object i, PE pe, IWrappedPE<PE, SE> wpe, IWrappedSE<PE, SE> wse, int len) {
        assert (len >= 0);
        this.i = i;
        this.pe = pe;
        this.wpe = wpe;
        this.wse = wse;
        this.len = len;
    }

    public void update(IWrappedPE<PE, SE> newWPE, IWrappedSE<PE, SE> newWSE, int newLen) {
        assert (newLen >= 0);
        this.wpe = newWPE;
        this.wse = newWSE;
        this.len = newLen;
    }

    public int getLen() { return len; }

    @Override
    public Object getInst() { return i; }

    @Override
    public PE getPE() { return pe; }

    @Override
    public IWrappedPE<PE, SE> getWPE() { return wpe; }

    @Override
    public IWrappedSE<PE, SE> getWSE() { return wse; }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((i == null) ? 0 : i.hashCode());
        result = prime * result + ((pe == null) ? 0 : pe.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WrappedPE)) return false;
        WrappedPE that = (WrappedPE) obj;
        return this.i == that.i && Utils.areEqual(this.pe, that.pe);
    }

    @Override
    public String toString() {
        return "WrappedEdge [Inst=" + i + ", PE="+pe+"]";
    }
}
