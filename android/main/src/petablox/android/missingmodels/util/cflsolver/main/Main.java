package petablox.android.missingmodels.util.cflsolver.main;

import petablox.android.missingmodels.util.cflsolver.cfg.ContextFreeGrammar;
import petablox.android.missingmodels.util.cflsolver.graph.Graph;
import petablox.android.missingmodels.util.cflsolver.solver.ReachabilitySolver;

public class Main {
	public static void main(String[] args) {
		ContextFreeGrammar c = new ContextFreeGrammar(4);
		c.addBinaryProduction("()", "(", ")");
		System.out.println("CFG:");
		System.out.println(c.toString());
		Graph g = new Graph(c);
		g.addEdge("x", "y", "(");
		g.addEdge("y", "z", ")");
		System.out.println("Graph:");
		System.out.println(g.toString());
		new ReachabilitySolver().solve(c, g);
		System.out.println("Solution:");
		System.out.println(g.toString());
	}
}
