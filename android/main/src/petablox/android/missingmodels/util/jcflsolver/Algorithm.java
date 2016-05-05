package petablox.android.missingmodels.util.jcflsolver;

public abstract class Algorithm
{	
	public abstract void addEdge(Edge oldEdge, Edge newEdge);
	
	public abstract Edge nextEdgeToProcess();

	protected final Graph g;
	
	protected Algorithm(Graph g)
	{
		this.g = g;
	}

	public void process()
	{
		beforeProcess();
		Edge edge;
		while((edge = nextEdgeToProcess()) != null)
			g.process(edge);
		afterProcess();
	}
	
	protected void beforeProcess(){}

	protected void afterProcess(){}
}
