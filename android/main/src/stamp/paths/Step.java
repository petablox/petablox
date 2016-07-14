package stamp.paths;

public class Step {
	public final String symbol;
	public final boolean reverse;
	public final Point target;

	public Step(String symbol, boolean reverse, Point target) {
		// TODO: Check that the symbol is terminal.
		this.symbol = symbol;
		this.reverse = reverse;
		this.target = target;
	}
}
