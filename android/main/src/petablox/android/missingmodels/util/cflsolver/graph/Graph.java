package petablox.android.missingmodels.util.cflsolver.graph;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import petablox.android.missingmodels.util.cflsolver.cfg.ContextFreeGrammar;

public final class Graph {
	private final Map<String,Vertex> vertices = new HashMap<String,Vertex>();
	private final Map<String,Integer> fields = new HashMap<String,Integer>();
	public final List<Edge> edges = new LinkedList<Edge>();
	private final ContextFreeGrammar contextFreeGrammar;
	private final int numLabels;
	
	private int curField = 0;
	
	public Graph(ContextFreeGrammar contextFreeGrammar) {
		this.contextFreeGrammar = contextFreeGrammar;
		this.numLabels = contextFreeGrammar.unaryProductionsByInput.length;
	}
	
	public Vertex getVertex(String name) {
		Vertex vertex = this.vertices.get(name);
		if(vertex == null) {
			vertex = new Vertex(name, this.numLabels);
			this.vertices.put(name, vertex);
		}
		return vertex;
	}
	
	public int getField(String field) {
		Integer intField = this.fields.get(field);
		if(intField == null) {
			intField = curField++;
			this.fields.put(field, intField);
		}
		return intField;
	}
	
	public Edge addEdge(String source, String sink, String label) {
		return this.addEdge(this.getVertex(source), this.getVertex(sink), this.contextFreeGrammar.getLabel(label), -1);
	}
	
	public Edge addEdge(String source, String sink, String label, String field) {
		return this.addEdge(this.getVertex(source), this.getVertex(sink), this.contextFreeGrammar.getLabel(label), this.getField(field));
	}
	
	public Edge addEdge(Vertex source, Vertex sink, int label, int field) {
		Edge edge = new Edge(source, sink, label, field);
		if(field == -2 || source.outgoingEdgesByLabel[label].contains(edge)) {
			return null;
		}
		source.outgoingEdgesByLabel[label].add(edge);
		sink.incomingEdgesByLabel[label].add(edge);
		this.edges.add(edge);
		return edge;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Edge edge : this.edges) {
			sb.append(edge.toString()).append("\n");
		}
		return sb.toString();
	}
}
