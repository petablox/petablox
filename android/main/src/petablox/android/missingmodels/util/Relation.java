package petablox.android.missingmodels.util;

import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;
import petablox.android.missingmodels.util.StubLookup.StubLookupKey;
import petablox.android.missingmodels.util.StubLookup.StubLookupValue;
import petablox.android.missingmodels.util.StubModelSet.ModelType;
import petablox.android.missingmodels.util.jcflsolver.Graph;

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
	public abstract String getRelationName();

	/*
	 * This returns the source of the edge.
	 * 
	 * @param tuple: The Shord tuple being converted to a graph edge.
	 * @return: String representing the graph edge's source.
	 */
	public abstract String getSourceFromTuple(int[] tuple);
	
	/*
	 * This returns the sink of the edge.
	 * 
	 * @param tuple: The Shord tuple being converted to a graph edge.
	 * @return: String representing the graph edge's sink. 
	 */
	public abstract String getSinkFromTuple(int[] tuple);

	/*
	 * This returns whether or not the relation has edge labels.
	 * 
	 * @return: True if the edge is labeled, false if the edge
	 * is unlabeled.
	 */
	public abstract boolean hasLabel();
	
	/*
	 * Returns the label on the edge.
	 * 
	 * @param tuple: The Shord tuple being converted to a graph edge.
	 * @return: The label on the edge as an integer (unspecified if there
	 * is no label).
	 */
	public abstract int getLabelFromTuple(int[] tuple);

	/*
	 * Returns whether or not the relation is a stub relation. Stub edges
	 * are stored indexed.
	 * 
	 * @return: True if the relation is a stub relation, false if the
	 * relation is not a stub relation.
	 */
	public abstract boolean isStub();
	
	/*
	 * Returns the stub lookup value corresponding to the tuple.
	 * NOTE: the stub lookup key is the triple
	 * (graph relation name, source name, sink name).
	 * 
	 * @param tuple: The Shord tuple being converted to a graph edge.
	 * @return: The stub lookup value.
	 */
	public abstract StubLookupValue getStubLookupValueFromTuple(int[] tuple);
	
	/*
	 * A filter on the tuple. The algorithm only adds the tuple if this
	 * returns true.
	 * 
	 * @param tuple: The Shord tuple being converted to a graph edge.
	 * @return: True if the tuple should be added, false if the tuple
	 * @param stubModelSet: A set of models to filter by.
	 * should be discarded.
	 */
	public abstract boolean filter(int[] tuple, StubModelSet stubModelSet);

	/*
	 * Returns the stub lookup key corresponding to the tuple. By default,
	 * this is the triple
	 * (graph relation name, source name, sink name).
	 * 
	 * @param tuple: The Shord tuple being converted to a graph edge.
	 * @param edgeName: The name of the edge in the graph.
	 */
	public StubLookupKey getStubLookupKeyFromTuple(int[] tuple, String edgeName) {
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
		try {
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
		} catch(RuntimeException e) {
			e.printStackTrace();
		}
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

		@Override
		public String getRelationName() {
			return this.relationName;
		}

		@Override
		public String getSourceFromTuple(int[] tuple) {
			try {
				return firstVarType + Integer.toString(tuple[firstVarIndex]) + (hasFirstCtxt ? "_" + Integer.toString(tuple[firstCtxtIndex]) : "");
			} catch(Exception e) {
				throw new RuntimeException("Error parsing relation " + getRelationName() + "!");
			}
		}

		@Override
		public String getSinkFromTuple(int[] tuple) {
			try {
				return secondVarType + Integer.toString(tuple[secondVarIndex]) + (hasSecondCtxt ? "_" + Integer.toString(tuple[secondCtxtIndex]) : "");
			} catch(Exception e) {
				throw new RuntimeException("Error parsing relation " + getRelationName() + "!");
			}
		}

		@Override
		public int getLabelFromTuple(int[] tuple) {
			try {
				return tuple[labelIndex];
			} catch(Exception e) {
				throw new RuntimeException("Error parsing relation " + getRelationName() + "!");
			}
		}

		@Override
		public boolean hasLabel() {
			return hasLabel;
		}

		@Override
		public boolean isStub() {
			return false;
		}

		@Override
		public StubLookupValue getStubLookupValueFromTuple(int[] tuple) {
			return null;
		}

		@Override
		public boolean filter(int[] tuple, StubModelSet stubModelSet) {
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

		@Override
		public boolean isStub() {
			return true;
		}

		@Override
		public StubLookupValue getStubLookupValueFromTuple(int[] tuple) {
			Integer firstArg = this.firstArgIndex == null ? null : tuple[this.firstArgIndex];
			Integer secondArg = this.secondArgIndex == null ? null : tuple[this.secondArgIndex];
			return new StubLookupValue(this.getRelationName(), tuple[this.methodIndex], firstArg, secondArg);
		}

		@Override
		public boolean filter(int[] tuple, StubModelSet stubModelSet) {
			//return stubModelSet.get(this.getStubLookupValueFromTuple(tuple)) != ModelType.FALSE;
			return true;
		}
	}
}

