package petablox.project.analyses.rhs;


public interface IWrappedPE<PE extends IEdge, SE extends IEdge> {
    public Object getInst();
    public PE getPE();
    public IWrappedPE<PE, SE> getWPE();
    public IWrappedSE<PE, SE> getWSE();
}

