package stamp.missingmodels.util;

import shord.project.ClassicProject;
import shord.project.analyses.ProgramRel;
import stamp.missingmodels.util.StubLookup.StubLookupKey;
import stamp.missingmodels.util.StubLookup.StubLookupValue;
import stamp.missingmodels.util.StubModelSet.ModelType;
import stamp.missingmodels.util.jcflsolver.Graph;

/*
 * This class handles the conversion from Shord tuples to
 * JCFLSolver graph edges.
 * 
 * @author Osbert Bastani
 */
public abstract class Relation {
	/*
	 * This returns the Shord name of the relation.
	 * 
	 * @return: The name of the Shord relation being processed.
	 */
	protected abstract String getRelationName();

	/*
	 * This returns the source of the edge.
	 * 
	 * @param tuple: The Shord tuple being converted to a graph edge.
	 * @return: String representing the graph edge's source.
	 */
	protected abstract String getSourceFromTuple(int[] tuple);
	
	/*
	 * This returns the sink of the edge.
	 * 
	 * @param tuple: The Shord tuple being converted to a graph edge.
	 * @return: String representing the graph edge's sink. 
	 */
	protected abstract String getSinkFromTuple(int[] tuple);

	/*
	 * This returns whether or not the relation has edge labels.
	 * 
	 * @return: True if the edge is labeled, false if the edge
	 * is unlabeled.
	 */
	protected abstract boolean hasLabel();
	
	/*
	 * Returns the label on the edge.
	 * 
	 * @param tuple: The Shord tuple being converted to a graph edge.
	 * @return: The label on the edge as an integer (unspecified if there
	 * is no label).
	 */
	protected abstract int getLabelFromTuple(int[] tuple);

	/*
	 * Returns whether or not the relation is a stub relation. Stub edges
	 * are stored indexed.
	 * 
	 * @return: True if the relation is a stub relation, false if the
	 * relation is not a stub relation.
	 */
	protected abstract boolean isStub();
	
	/*
	 * Returns the stub lookup value corresponding to the tuple.
	 * NOTE: the stub lookup key is the triple
	 * (graph relation name, source name, sink name).
	 * 
	 * @param tuple: The Shord tuple being converted to a graph edge.
	 * @return: The stub lookup value.
	 */
	protected abstract StubLookupValue getStubLookupValueFromTuple(int[] tuple);
	
	/*
	 * A filter on the tuple. The algorithm only adds the tuple if this
	 * returns true.
	 * 
	 * @param tuple: The Shord tuple being converted to a graph edge.
	 * @return: True if the tuple should be added, false if the tuple
	 * @param stubModelSet: A set of models to filter by.
	 * should be discarded.
	 */
	protected abstract boolean filter(int[] tuple, StubModelSet stubModelSet);

	/*
	 * Returns the stub lookup key corresponding to the tuple. By default,
	 * this is the triple
	 * (graph relation name, source name, sink name).
	 * 
	 * @param tuple: The Shord tuple being converted to a graph edge.
	 * @param edgeName: The name of the edge in the graph.
	 */
	protected StubLookupKey getStubLookupKeyFromTuple(int[] tuple, String edgeName) {
		return new StubLookupKey(edgeName, this.getSourceFromTuple(tuple), this.getSinkFromTuple(tuple));
	}

	/*
	 * Converts the Shord tuple to the graph edges in the manner specified
	 * by the above methods.
	 * 
	 * @param edgename: The name of the edge in the graph.
	 * @param g: The graph to which we are adding the edges.
	 * @param stubLookup: The lookup to which to add the methods.
	 * @param stubModelSet: Argument for the filter. 
	 */
	public void addEdges(String edgeName, Graph g, StubLookup stubLookup, StubModelSet stubModelSet) {
		// STEP 0: Get some basic information about the kind of edge we are adding.
		int kind = g.symbolToKind(edgeName);
		short weight = g.kindToWeight(kind);

		// STEP 1: Load the Shord relation.
		final ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt(getRelationName());
		rel.load();
		Iterable<int[]> res = rel.getAryNIntTuples();

		// STEP 2: Iterate over relation and add to the graph.
		for(int[] tuple : res) {
			if(this.filter(tuple, stubModelSet)) {
				String sourceName = getSourceFromTuple(tuple);
				String sinkName = getSinkFromTuple(tuple);

				if(hasLabel()) {
					g.addWeightedInputEdge(sourceName, sinkName, kind, getLabelFromTuple(tuple), weight);
				} else {
					g.addWeightedInputEdge(sourceName, sinkName, kind, weight);
				}

				if(isStub()) {
					stubLookup.put(this.getStubLookupKeyFromTuple(tuple, edgeName), this.getStubLookupValueFromTuple(tuple));
				}
			}
		}

		rel.close();
	}


	public static class IndexRelation extends Relation {
		protected final int firstVarIndex;
		protected final int firstCtxtIndex;

		protected final int secondVarIndex;
		protected final int secondCtxtIndex;

		protected final int labelIndex;

		protected final String firstVarType;
		protected final String secondVarType;

		protected final String relationName;

		protected final boolean hasFirstCtxt;
		protected final boolean hasSecondCtxt;
		protected final boolean hasLabel;

		public IndexRelation(String relationName, String firstVarType, int firstVarIndex, Integer firstCtxtIndex, String secondVarType, int secondVarIndex, Integer secondCtxtIndex, Integer labelIndex) {
			this.relationName = relationName;

			this.hasFirstCtxt = firstCtxtIndex != null;
			this.hasSecondCtxt = secondCtxtIndex != null;
			this.hasLabel = labelIndex != null;

			this.firstVarIndex = firstVarIndex;
			this.firstCtxtIndex = this.hasFirstCtxt ? firstCtxtIndex : 0;

			this.secondVarIndex = secondVarIndex;
			this.secondCtxtIndex = this.hasSecondCtxt ? secondCtxtIndex : 0;

			this.labelIndex = this.hasLabel ? labelIndex : 0;

			this.firstVarType = firstVarType;
			this.secondVarType = secondVarType;
		}

		public IndexRelation(String relationName, String firstVarType, int firstVarIndex, Integer firstCtxtIndex, String secondVarType, int secondVarIndex, Integer secondCtxtIndex) {
			this(relationName, firstVarType, firstVarIndex, firstCtxtIndex, secondVarType, secondVarIndex, secondCtxtIndex, null);
		}

		@Override protected String getRelationName() {
			return this.relationName;
		}

		@Override protected String getSourceFromTuple(int[] tuple) {
			try {
				return firstVarType + Integer.toString(tuple[firstVarIndex]) + (hasFirstCtxt ? "_" + Integer.toString(tuple[firstCtxtIndex]) : "");
			} catch(Exception e) {
				throw new RuntimeException("Error parsing relation " + getRelationName() + "!");
			}
		}

		@Override protected String getSinkFromTuple(int[] tuple) {
			try {
				return secondVarType + Integer.toString(tuple[secondVarIndex]) + (hasSecondCtxt ? "_" + Integer.toString(tuple[secondCtxtIndex]) : "");
			} catch(Exception e) {
				throw new RuntimeException("Error parsing relation " + getRelationName() + "!");
			}
		}

		@Override protected int getLabelFromTuple(int[] tuple) {
			try {
				return tuple[labelIndex];
			} catch(Exception e) {
				throw new RuntimeException("Error parsing relation " + getRelationName() + "!");
			}
		}

		@Override protected boolean hasLabel() {
			return hasLabel;
		}

		@Override protected boolean isStub() {
			return false;
		}

		@Override protected StubLookupValue getStubLookupValueFromTuple(int[] tuple) {
			return null;
		}

		@Override protected boolean filter(int[] tuple, StubModelSet stubModelSet) {
			return true;
		}
	}

	public static class StubIndexRelation extends IndexRelation {
		private int methodIndex;
		private Integer firstArgIndex;
		private Integer secondArgIndex;

		public StubIndexRelation(String relationName, String firstVarType, int firstVarIndex, Integer firstCtxtIndex, String secondVarType, int secondVarIndex, Integer secondCtxtIndex, Integer labelIndex, int methodIndex, Integer firstArgIndex, Integer secondArgIndex) {
			super(relationName, firstVarType, firstVarIndex, firstCtxtIndex, secondVarType, secondVarIndex, secondCtxtIndex, labelIndex);
			this.methodIndex = methodIndex;
			this.firstArgIndex = firstArgIndex;
			this.secondArgIndex = secondArgIndex;
		}

		public StubIndexRelation(String relationName, String firstVarType, int firstVarIndex, Integer firstCtxtIndex, String secondVarType, int secondVarIndex, Integer secondCtxtIndex, int methodIndex, Integer firstArgIndex, Integer secondArgIndex) {
			this(relationName, firstVarType, firstVarIndex, firstCtxtIndex, secondVarType, secondVarIndex, secondCtxtIndex, null, methodIndex, firstArgIndex, secondArgIndex);
		}

		public StubIndexRelation(String relationName, String firstVarType, int firstVarIndex, Integer firstCtxtIndex, String secondVarType, int secondVarIndex, Integer secondCtxtIndex, int methodIndex, Integer argIndex) {
			this(relationName, firstVarType, firstVarIndex, firstCtxtIndex, secondVarType, secondVarIndex, secondCtxtIndex, null, methodIndex, argIndex, null);
		}

		public StubIndexRelation(String relationName, String firstVarType, int firstVarIndex, Integer firstCtxtIndex, String secondVarType, int secondVarIndex, Integer secondCtxtIndex, int methodIndex) {
			this(relationName, firstVarType, firstVarIndex, firstCtxtIndex, secondVarType, secondVarIndex, secondCtxtIndex, null, methodIndex, null, null);
		}

		@Override protected boolean isStub() {
			return true;
		}

		@Override protected StubLookupValue getStubLookupValueFromTuple(int[] tuple) {
			Integer firstArg = this.firstArgIndex == null ? null : tuple[this.firstArgIndex];
			Integer secondArg = this.secondArgIndex == null ? null : tuple[this.secondArgIndex];
			return new StubLookupValue(this.getRelationName(), tuple[this.methodIndex], firstArg, secondArg);
		}

		@Override protected boolean filter(int[] tuple, StubModelSet stubModelSet) {
			return stubModelSet.get(this.getStubLookupValueFromTuple(tuple)) != ModelType.FALSE;
		}
	}
}

