package petablox.android.missingmodels.util.jcflsolver;


public class KnuthsAlgo extends Algorithm
{
	private BucketHeap worklist = new BucketHeap();

	public KnuthsAlgo(Graph g)
	{
		super(g);
	}

	private int count = 0;
	public void addEdge(Edge newEdge, Edge oldEdge)
	{
		if(oldEdge == null) {
			count++;
			worklist.push(newEdge);
		} else {
			worklist.update(oldEdge, newEdge);
		}
	}

	public Edge nextEdgeToProcess()
	{
		Edge edge = worklist.pop();
		/*
		if(edge != null && edge.kind == 0 && edge.firstInput.kind != 0) {
			System.out.println("HERE: (" + edge.from.id + "," + edge.to.id + "); (" + edge.firstInput.from.id + "," + edge.firstInput.to.id + "); (" + edge.secondInput.from.id + "," + edge.secondInput.to.id + ");");
			System.out.println("MORE: " + g.kindToSymbol(edge.kind) + " <- " + g.kindToSymbol(edge.firstInput.kind) + ", " + g.kindToSymbol(edge.secondInput.kind));
		}
		*/
		return edge;
	}

	protected void beforeProcess()
	{
		System.out.println("Initial num of edges = " + worklist.size());
	}

	protected void afterProcess()
	{
		System.out.println("Total num of edges = "+count);
		/*
		//debugging
		int[] counts = new int[g.numKinds()];
		for(Node node : g.allNodes()){
			Edges[] outEdges = node.outEdges;
			for(int i = 0; i < g.numKinds(); i++){
				Edges edges = outEdges[i];
				if(edges == null)
					continue;
				counts[i] += ((EdgesSetCustom) edges).count();
			}
		}
		int total = 0;
		for(int i = 0; i < counts.length; i++){
			System.out.println(g.kindToSymbol(i) + " "+counts[i]);
			total += counts[i];
		}
		System.out.println("edges with different labels = "+total);
		 */
	}

}
