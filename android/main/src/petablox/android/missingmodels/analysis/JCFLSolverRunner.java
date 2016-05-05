package petablox.android.missingmodels.analysis;

import java.util.Collection;

import petablox.android.missingmodels.analysis.Experiment.ProposedStubModelSet;
import petablox.android.missingmodels.util.StubLookup;
import petablox.android.missingmodels.util.StubLookup.StubLookupKey;
import petablox.android.missingmodels.util.StubModelSet;
import petablox.android.missingmodels.util.StubModelSet.ModelType;
import petablox.android.missingmodels.util.StubModelSet.StubModel;
import petablox.android.missingmodels.util.Util.MultivalueMap;
import petablox.android.missingmodels.util.Util.Pair;
import petablox.android.missingmodels.util.jcflsolver.Edge;
import petablox.android.missingmodels.util.jcflsolver.EdgeData;
import petablox.android.missingmodels.util.jcflsolver.Graph;

public abstract class JCFLSolverRunner {
	
	/*
	 * An interface to handle chord relation lookups.
	 */
	public interface RelationAdder {
		public abstract Collection<String> addEdges(Graph g, StubLookup s, StubModelSet m);
	}
	
	// initialize and run
	public abstract void run(Class<? extends Graph> c, StubModelSet m, RelationAdder relationLookup);
	
	// basic getter methods
	public abstract Graph g();
	public abstract StubLookup s();
	public abstract StubModelSet m(); // input models, just for reference
	
	// methods for running experiments
	public abstract ProposedStubModelSet getProposedModels(int round);
	public ProposedStubModelSet getProposedModels() {
		return this.getProposedModels(Experiment.ProposedStubModelSet.DEFAULT_ROUND);
	}
	
	/*
	 * Returns set of models such that if all are true, then the
	 * analysis is complete.
	 */
	public static class JCFLSolverSingle extends JCFLSolverRunner {
		private Graph g;
		private StubLookup s;
		private StubModelSet m;

		public Graph g() {
			return this.g;
		}

		public StubLookup s() {
			return this.s;
		}

		public StubModelSet m() {
			return this.m;
		}

		/*
		 * The following code is for running the JCFLSolver analysis.
		 */
		private void fillTerminalEdges(RelationAdder relationLookup) {
			for(String relationNotFound : relationLookup.addEdges(this.g, this.s, this.m)) {
				System.out.println("No edges found for terminal relation " + relationNotFound + "!");
			}
		}
		
		// proposals are true (1)
		public ProposedStubModelSet getProposedModels(int round) {
			ProposedStubModelSet proposals = new ProposedStubModelSet();
			MultivalueMap<Edge,Pair<Edge,Boolean>> positiveWeightEdges = this.g.getPositiveWeightEdges("Src2Sink");
			for(Edge edge : positiveWeightEdges.keySet()) {
				for(Pair<Edge,Boolean> pair : positiveWeightEdges.get(edge)) {
					EdgeData data = pair.getX().getData(this.g);
					StubModel model = new StubModel(this.s.get(new StubLookupKey(data.symbol, data.from, data.to)));
					// IMPORTANT: Only return models that have unknown ground truth. We don't want to count
					// models that are already assumed to be true, but still have weight 1, in the set
					// of proposed models.
					if(this.m.get(model) == ModelType.UNKNOWN) {
						proposals.put(model, ModelType.UNKNOWN, ModelType.TRUE, round);
					}
				}
			}
			return proposals;
		}

		public void run(Class<? extends Graph> c, StubModelSet m, RelationAdder relationLookup) {
			// STEP 0: Set up the fields.
			try {
				this.g = c.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
				throw new RuntimeException("Error creating graph: " + c.toString() + "!");
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				throw new RuntimeException("Error creating graph: " + c.toString() + "!");
			}
			this.s = new StubLookup();
			this.m = m;

			// STEP 1: Fill the edges in the graph.
			this.fillTerminalEdges(relationLookup);

			// STEP 2: Run the algorithm.
			this.g.algo.process();
		}
	}

	/*
	 * Returns set of models such that if all are false, then
	 * analysis is complete. Does so by iteratively running
	 * the previous algorithm and rejecting proposed models.
	 */
	public static class JCFLSolverStubs extends JCFLSolverRunner {
		private JCFLSolverSingle j;
		private StubModelSet m;
		private Class<? extends Graph> c;
		private StubModelSet allProposed;

		/*
		 * Returns the last executed graph.
		 * @see petablox.android.missingmodels.analysis.JCFLSolverRunner#g()
		 */
		public Graph g() {
			if(j == null) {
				return null;
			}
			return this.j.g;
		}

		public StubLookup s() {
			if(j == null) {
				return null;
			}
			return this.j.s;
		}

		public StubModelSet m() {
			if(j == null) {
				return null;
			}
			return this.j.m;
		}
		
		/*
		 * Proposals are false (2).
		 * @see petablox.android.missingmodels.analysis.JCFLSolverRunner#getProposedModels()
		 */
		public ProposedStubModelSet getProposedModels(int round) {
			ProposedStubModelSet proposed = new ProposedStubModelSet();
			proposed.setDefaultValue(ModelType.FALSE);
			proposed.putAll(this.allProposed);
			return proposed;
		}

		private void runHelper(RelationAdder relationLookup) {
			this.allProposed = new StubModelSet(); // proposed models
			StubModelSet total = new StubModelSet(); // all models
			total.putAll(this.m); // initialize to the given known models
			
			// keep running and rejecting
			StubModelSet curProposed;
			do {
				// run the solver
				this.j = new JCFLSolverSingle();
				j.run(this.c, total, relationLookup);
				
				// add the current proposed models to all proposed and total
				// (note that this won't include anything that has known ground
				// truth, since this is true about j)
				curProposed = this.j.getProposedModels();
				total.putAllToValue(curProposed, 2);
				this.allProposed.putAll(curProposed);
			} while(!curProposed.isEmpty());
		}

		public void run(Class<? extends Graph> c, StubModelSet m, RelationAdder relationLookup) {
			this.c = c;
			this.m = m;
			runHelper(relationLookup);
		}
	}
}
