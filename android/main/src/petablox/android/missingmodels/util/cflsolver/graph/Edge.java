package petablox.android.missingmodels.util.cflsolver.graph;

public final class Edge {
	public final Vertex source;
	public final Vertex sink;
	public final int label;
	public final int field;
	
	public Edge(Vertex source, Vertex sink, int label, int field) {
		this.source = source;
		this.sink = sink;
		this.label = label;
		this.field = field;
	}
	
	public Edge(Vertex source, Vertex sink, int label) {
		this.source = source;
		this.sink = sink;
		this.label = label;
		this.field = -1;
	}
	
	@Override
	public String toString() {
		return this.source.toString() + " " + this.sink.toString() + " " + this.label + "[" + this.field + "]"; 
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + field;
		result = prime * result + label;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Edge other = (Edge) obj;
		if (field != other.field)
			return false;
		if (label != other.label)
			return false;
		return true;
	}
}
