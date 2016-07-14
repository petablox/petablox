package stamp.missingmodels.util.jcflsolver;

import java.util.*;

public class RepsAlgo extends Algorithm
{
	private List<Edge> workList = new LinkedList();

	public RepsAlgo(Graph g)
	{
		super(g);
	}

	private int count = 0;
	public void addEdge(Edge newEdge, Edge oldEdge)
	{
		if(oldEdge != null)
			return;
		count++;
		workList.add(newEdge);
	}
	
	public Edge nextEdgeToProcess()
	{
		Edge edge = null;
		if(!workList.isEmpty())
			edge = workList.remove(0);
		return edge;
	}

	protected void beforeProcess()
	{
		System.out.println("Initial num of edges = "+workList.size());
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