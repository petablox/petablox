package stamp.missingmodels.util.jcflsolver;

public class NonLabeledEdge extends Edge
{
	NonLabeledEdge(int kind, Node from, Node to)
	{
		super(kind, from, to);
	}

	public int hashCode()
	{
		return from.hashCode() + to.hashCode() + kind;
	}
	
	public boolean equals(Object o)
	{
		if(!(o instanceof NonLabeledEdge))
			return false;
		NonLabeledEdge other = (NonLabeledEdge) o;
		return from == other.from && to == other.to && kind == other.kind;
	}
	
	protected boolean matchesLabel(Edge other)
	{
		return true;
	}

	protected int label()
	{
		return -1;
	}

	public String toString()
	{
		return "NonLabeledEdge("+from.id+", "+to.id+", "+kind+")";
	}

    public EdgeData getData(Graph g) {
	return new EdgeData(this.from.name, this.to.name, g.kindToSymbol(kind), weight);
    }
}