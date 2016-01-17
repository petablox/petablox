package petablox.project.analyses.rhs;

import soot.Unit;

public interface IWrappedPE<PE extends IEdge, SE extends IEdge> {
    public Unit getInst();
    public PE getPE();
    public IWrappedPE<PE, SE> getWPE();
    public IWrappedSE<PE, SE> getWSE();
}

