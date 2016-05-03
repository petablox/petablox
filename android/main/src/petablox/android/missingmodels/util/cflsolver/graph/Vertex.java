package petablox.android.missingmodels.util.cflsolver.graph;

import java.util.HashSet;
import java.util.Set;

public final class Vertex {
	public final String name;
	public final Set[] incomingEdgesByLabel; // list of type Edge
	public final Set[] outgoingEdgesByLabel; // list of type Edge
	
	public Vertex(String name, int numLabels) {
		this.name = name;
		this.incomingEdgesByLabel = new Set[numLabels];
		for(int i=0; i<numLabels; i++) {
			this.incomingEdgesByLabel[i] = new HashSet();
		}
		this.outgoingEdgesByLabel = new Set[numLabels];
		for(int i=0; i<numLabels; i++) {
			this.outgoingEdgesByLabel[i] = new HashSet();
		}
	}
	
	@Override
	public String toString() {
		return this.name;
	}
}
