package petablox.android.missingmodels.util.cflsolver.solver;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import petablox.android.missingmodels.util.cflsolver.cfg.BinaryProduction;
import petablox.android.missingmodels.util.cflsolver.cfg.ContextFreeGrammar;
import petablox.android.missingmodels.util.cflsolver.cfg.UnaryProduction;
import petablox.android.missingmodels.util.cflsolver.graph.Edge;
import petablox.android.missingmodels.util.cflsolver.graph.Graph;
import petablox.android.missingmodels.util.cflsolver.graph.Vertex;

public class ReachabilitySolver {
	private int getField(int firstField, int secondField) {
		if(firstField == secondField) {
			return -1;
		}
		if(firstField == -1) {
			return secondField;
		}
		if(secondField == -1) {
			return firstField;
		}
		return -2;
	}
	
	private void addEdge(Graph graph, Vertex source, Vertex sink, int label, int field, LinkedList<Edge> worklist) {
		Edge edge = graph.addEdge(source, sink, label, field);
		if(edge != null) {
			worklist.add(edge);
		}
	}
	
	public void solve(ContextFreeGrammar c, Graph g) {
		// Initialize the worklist
		LinkedList<Edge> worklist = new LinkedList<Edge>(); // list of type Edge
		for(Edge edge : g.edges) {
			worklist.add(edge);
		}
		
		// Process edges in worklist until empty  
		while(!worklist.isEmpty()) {
			Edge edge = worklist.removeFirst();
			Edge newEdge;
			// ->
			for(UnaryProduction unaryProduction : (List<UnaryProduction>)c.unaryProductionsByInput[edge.label]) {
				if((edge = g.addEdge(edge.source, edge.sink, unaryProduction.target, edge.field)) != null) {
					worklist.add(edge);
				}
			}
			for(BinaryProduction binaryProduction : (List<BinaryProduction>)c.binaryProductionsByFirstInput[edge.label]) {
				// <- <-
				// <- <-
				// <- ->
				// -> <-
				// -> ->
				if(binaryProduction.isFirstInputBackwards && binaryProduction.isSecondInputBackwards) {
					for(Edge secondEdge : (Set<Edge>)edge.source.incomingEdgesByLabel[binaryProduction.secondInput]) {
						this.addEdge(g, edge.sink, secondEdge.source, binaryProduction.target, this.getField(edge.field, secondEdge.field), worklist);
					}
				} else if(binaryProduction.isFirstInputBackwards) {
					for(Edge secondEdge : (Set<Edge>)edge.source.outgoingEdgesByLabel[binaryProduction.secondInput]) {
						this.addEdge(g, edge.sink, secondEdge.sink, binaryProduction.target, this.getField(edge.field, secondEdge.field), worklist);
					}
				} else if(binaryProduction.isSecondInputBackwards) {
					for(Edge secondEdge : (Set<Edge>)edge.sink.incomingEdgesByLabel[binaryProduction.secondInput]) {
						this.addEdge(g, edge.source, secondEdge.source, binaryProduction.target, this.getField(edge.field, secondEdge.field), worklist);
					}
				} else {
					for(Edge secondEdge : (Set<Edge>)edge.sink.outgoingEdgesByLabel[binaryProduction.secondInput]) {
						this.addEdge(g, edge.source, secondEdge.sink, binaryProduction.target, this.getField(edge.field, secondEdge.field), worklist);
					}
				}
				
			}
			for(BinaryProduction binaryProduction : (List<BinaryProduction>)c.binaryProductionsBySecondInput[edge.label]) {
				// <- <-
				// <- ->
				// -> <-
				// -> ->
				if(binaryProduction.isFirstInputBackwards && binaryProduction.isSecondInputBackwards) {
					for(Edge firstEdge : (Set<Edge>)edge.sink.outgoingEdgesByLabel[binaryProduction.firstInput]) {
						this.addEdge(g, firstEdge.sink, edge.source, binaryProduction.target, this.getField(edge.field, firstEdge.field), worklist);
					}
				} else if(binaryProduction.isFirstInputBackwards) {
					for(Edge firstEdge : (Set<Edge>)edge.source.outgoingEdgesByLabel[binaryProduction.firstInput]) {
						this.addEdge(g, firstEdge.sink, edge.sink, binaryProduction.target, this.getField(edge.field, firstEdge.field), worklist);
					}
				} else if(binaryProduction.isSecondInputBackwards) {
					for(Edge firstEdge : (Set<Edge>)edge.sink.incomingEdgesByLabel[binaryProduction.firstInput]) {
						this.addEdge(g, firstEdge.source, edge.source, binaryProduction.target, this.getField(edge.field, firstEdge.field), worklist);
					}
				} else {
					for(Edge firstEdge : (Set<Edge>)edge.source.incomingEdgesByLabel[binaryProduction.firstInput]) {
						this.addEdge(g, firstEdge.source, edge.sink, binaryProduction.target, this.getField(edge.field, firstEdge.field), worklist);
					}
				}
			}
		}
	}
}
