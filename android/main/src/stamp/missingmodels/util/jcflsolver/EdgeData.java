package stamp.missingmodels.util.jcflsolver;

public class EdgeData {
	public final String from;
	public final String to;
	public final String symbol;
	public final String label;
	public final short weight;

	public EdgeData(String from, String to, String symbol, short weight) {
		this(from, to, symbol, weight, null);
	}

	public EdgeData(String from, String to, String symbol, short weight, String label) {
		this.from = from;
		this.to = to;
		this.symbol = symbol;
		this.weight = weight;
		this.label = label;
	}

	public String toString(boolean forward) {
		return this.symbol + (forward ? "" : "Bar") + (this.label == null ? "" : ("[" + this.label + "]")) + (this.weight > 0 ? " " + Short.toString(weight) : "");
	}

	public boolean hasLabel() {
		return label != null;
	}

	public String getTo(boolean forward) {
		return forward ? to : from;
	}

	public String getFrom(boolean forward) {
		return forward ? from : to;
	}

	public String getSymbol(boolean forward) {
		return symbol + (forward ? "" : "Bar");
	}
}