package stamp.missingmodels.util.jcflsolver;


public class LabeledEdge extends Edge
{
	public final int label;

	public LabeledEdge(int kind, Node from, Node to, int label)
	{
		super(kind, from, to);
		this.label = label;
	}

	public int hashCode()
	{
		return from.hashCode() + to.hashCode() + kind + label;
	}

	public boolean equals(Object o)
	{
		if(!(o instanceof LabeledEdge))
			return false;
		LabeledEdge other = (LabeledEdge) o;
		return from == other.from && to == other.to && kind == other.kind && label == other.label;
	}
	
	protected boolean matchesLabel(Edge other)
	{
		if(other instanceof LabeledEdge)
			return this.label == ((LabeledEdge) other).label;
		else
			return true;
	}
	
	protected int label()
	{
		return label;
	}

	public String toString()
	{
		return "LabeledEdge("+from.id+", "+to.id+", "+kind+", "+label+")";
	}

    public EdgeData getData(Graph g) {
	return new EdgeData(this.from.name, this.to.name, g.kindToSymbol(kind), weight, Integer.toString(label()));
    }
}