package petablox.android.missingmodels.util.jcflsolver;


public class Node
{
	final Edges[] inEdges;
	final Edges[] outEdges;
	final String name;
	final int id;

	private static int nodeCount = 0;
	private static final boolean OUT_EDGES_NEXT = true;

	public Node(String name, int numSymbols)
	{
		this.name = name;
		this.inEdges = new Edges[numSymbols];
		this.outEdges = new Edges[numSymbols];
		this.id = ++nodeCount;
	}

    public String getName() {
	return name;
    }

	public Edges getOutEdges(int kind)
	{
		Edges edges = outEdges[kind];
		if(edges == null)
			edges = Edges.EMPTY;
		return edges;
	}
	
	public Edges getInEdges(int kind)
	{
		Edges edges = inEdges[kind];
		if(edges == null)
			edges = Edges.EMPTY;
		return edges;
	}
 
	Edge addOutEdge(Edge edge)
	{		
		int kind = edge.kind;
		Edges edges = outEdges[kind];
		if(edges == null) {
			edges = new /*EdgesSet*/EdgesSetCustom(OUT_EDGES_NEXT);
			outEdges[kind] = edges;
		}
		return edges.addEdge(edge);
	}

	/* 
	   called from FactsReader.
	 */
	void addInputOutEdge(Edge edge)
	{
		int kind = edge.kind;
		Edges edges = outEdges[kind];
		if(edges == null) {
			edges = new /*EdgesLinkedList*/EdgesSetCustom(OUT_EDGES_NEXT);
			outEdges[kind] = edges;
		}
		edges.addEdge(edge);
	}
	
	void addInEdge(Edge edge)
	{
		int kind = edge.kind;
		Edges edges = inEdges[kind];
		if(edges == null) {
			edges = new /*EdgesLinkedList*/EdgesListCustom(!OUT_EDGES_NEXT);
			inEdges[kind] = edges;
		}
		edges.addEdge(edge);
	}
	
}
