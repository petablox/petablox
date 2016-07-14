package stamp.missingmodels.util;

import java.util.HashMap;

import shord.analyses.DomM;
import shord.project.ClassicProject;
import soot.SootMethod;
import stamp.missingmodels.util.StubLookup.StubLookupKey;
import stamp.missingmodels.util.StubLookup.StubLookupValue;

/*
 * This class stores stub information for edges in the JCFLSolver graph.
 * 
 * @author Osbert Bastani
 */
public class StubLookup extends HashMap<StubLookupKey,StubLookupValue> {
	private static final long serialVersionUID = 3338165327126122836L;

	/*
	 * A stub lookup key consists of a graph symbol, the source node,
	 * and the sink node (i.e. an edge in the graph).
	 */
	public static class StubLookupKey {
		public final String symbol;
		public final String source;
		public final String sink;

		public StubLookupKey(String symbol, String source, String sink) {
			this.symbol = symbol;
			this.source = source;
			this.sink = sink;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + symbol.hashCode();
			result = prime * result + source.hashCode();
			result = prime * result + sink.hashCode();
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
			StubLookupKey other = (StubLookupKey) obj;
			return symbol.equals(other.symbol)
					&& source.equals(other.source)
					&& sink.equals(other.sink);
		}
	}

	/*
	 * A stub lookup value contains information on the inferred model.
	 */
	public static class StubLookupValue {
		public final String relationName;
		public final int methodId;
		public final SootMethod method;
		public final Integer firstArg;
		public final Integer secondArg;

		public StubLookupValue(String relationName, int methodId, Integer firstArg, Integer secondArg) {
			this.relationName = relationName;
			this.methodId = methodId;


			DomM dom = (DomM)ClassicProject.g().getTrgt("M");
			this.method = dom.get(methodId);
			
			this.firstArg = firstArg;
			this.secondArg = secondArg;
		}

		public StubLookupValue(String relationName, int methodId, Integer arg) {
			this(relationName, methodId, arg, null);
		}

		public StubLookupValue(String relationName, int methodId) {
			this(relationName, methodId, null, null);
		}

		@Override
		public String toString() {
			return this.relationName + ":" + this.method.toString() + "[" + this.firstArg + "][" + this.secondArg + "]";
		}
		
		public String toStringShort() {
			return this.relationName + "[" + this.firstArg + "][" + this.secondArg + "]";
		}
	}
}
