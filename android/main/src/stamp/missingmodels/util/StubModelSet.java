package stamp.missingmodels.util;

import java.util.HashMap;

import stamp.missingmodels.util.StubLookup.StubLookupValue;
import stamp.missingmodels.util.StubModelSet.ModelType;
import stamp.missingmodels.util.StubModelSet.StubModel;

/*
 * A set of stub models, with their setting.
 * 0/null - no (or imprecise) information available
 * 1 - true model
 * 2 - false model
 * 
 * Currently, we only reject false models.
 */
public class StubModelSet extends HashMap<StubModel,ModelType> {
	private static final long serialVersionUID = -6501310257337445078L;
	
	public static enum ModelType {
		UNKNOWN(0), TRUE(1), FALSE(2);
		
		private final int value;
		ModelType(int value) {
			this.value = value;
		}
		
		public int toInt() {
			return this.value;
		}
		
		public static ModelType getModelType(int value) {
			switch(value) {
			case 0:
				return UNKNOWN;
			case 1:
				return TRUE;
			case 2:
				return FALSE;
			default:
				return null;
			}
		}
		
		@Override
		public String toString() {
			return Integer.toString(this.value);
		}
	}
	
	public StubModelSet() {
		super();
	}

	@Override
	public ModelType get(Object stubModel) {
		ModelType value = super.get(stubModel);
		if(value == null) {
			return ModelType.UNKNOWN;
		} else {
			return value;
		}
	}
	
	public ModelType get(StubLookupValue value) {
		return this.get(new StubModel(value));
	}
	
	public int getInt(StubLookupValue value) {
		return this.get(new StubModel(value)).toInt();
	}
	
	public void putAllToValue(StubModelSet m, int value) {
		this.putAllToValue(m, ModelType.getModelType(value));
	}
	
	public void putAllToValue(StubModelSet m, ModelType value) {
		for(StubModel model : m.keySet()) {
			super.put(model, value);
		}
	}
	
	/*
	 * Represents a stub model. 
	 */
	public static class StubModel {
		private static final String SEPARATOR = ";";
		
		public final String relationName;
		public final String methodName;
		public final Integer firstArg;
		public final Integer secondArg;
		
		public StubModel(String relationName, String methodName, Integer firstArg, Integer secondArg) {
			this.relationName = relationName;
			this.methodName = methodName;
			this.firstArg = firstArg;
			this.secondArg = secondArg;
		}
		
		public StubModel(StubLookupValue value) {
			this.relationName = value.relationName;
			this.methodName = value.method.toString();
			this.firstArg = value.firstArg;
			this.secondArg = value.secondArg;
		}
		
		public StubModel(String representation) {
			String[] tokens = representation.substring(1, representation.length()-1).split(SEPARATOR);
			this.relationName = tokens[0];
			this.methodName = tokens[1];
			this.firstArg = tokens[2].equals("null") ? null : Integer.parseInt(tokens[2]);
			this.secondArg = tokens[3].equals("null") ? null : Integer.parseInt(tokens[3]);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((firstArg == null) ? 0 : firstArg.hashCode());
			result = prime * result
					+ ((methodName == null) ? 0 : methodName.hashCode());
			result = prime * result
					+ ((relationName == null) ? 0 : relationName.hashCode());
			result = prime * result
					+ ((secondArg == null) ? 0 : secondArg.hashCode());
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
			StubModel other = (StubModel) obj;
			if (firstArg == null) {
				if (other.firstArg != null)
					return false;
			} else if (!firstArg.equals(other.firstArg))
				return false;
			if (methodName == null) {
				if (other.methodName != null)
					return false;
			} else if (!methodName.equals(other.methodName))
				return false;
			if (relationName == null) {
				if (other.relationName != null)
					return false;
			} else if (!relationName.equals(other.relationName))
				return false;
			if (secondArg == null) {
				if (other.secondArg != null)
					return false;
			} else if (!secondArg.equals(other.secondArg))
				return false;
			return true;
		}
		
		@Override public String toString() {
			String firstArgStr = this.firstArg == null ? "null" : this.firstArg.toString();
			String secondArgStr = this.secondArg == null ? "null" : this.secondArg.toString();
			return "{" + this.relationName + SEPARATOR + this.methodName + SEPARATOR + firstArgStr + SEPARATOR + secondArgStr + "}";
		}
	}
}