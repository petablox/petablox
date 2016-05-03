package petablox.android.missingmodels.util.jcflsolver;

public abstract class Edge
{
	public final Node from;
	public final Node to;
	public final int kind;

	Edge nextA;
	Edge nextB;
	
	public short weight;
	public Edge nextWorklist;
	public Edge prevWorklist;
	
	public Edge firstInput;
	public Edge secondInput;
	
	Edge(int kind, Node from, Node to)
	{
		this.from = from;
		this.to = to;
		this.kind = kind;
	}

	protected abstract boolean matchesLabel(Edge other);

	protected abstract int label();

    public abstract EdgeData getData(Graph g);
}
