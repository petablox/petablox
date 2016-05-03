package petablox.android.missingmodels.util.cflsolver.cfg;

public final class UnaryProduction {
	public final int target;
	public final int input;
	
	public UnaryProduction(int target, int input) {
		this.target = target;
		this.input = input;
	}
	
	@Override
	public String toString() {
		return this.target + " :- " + this.input + ".";
	}
}
