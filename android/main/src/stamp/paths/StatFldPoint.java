package stamp.paths;

import soot.jimple.spark.pag.SparkField;

public class StatFldPoint implements Point {
	public final SparkField field;

	public StatFldPoint(SparkField field) {
		// TODO: Check that it's a static field.
		this.field = field;
	}

	@Override
	public String toString() {
		return field.toString();
	}

	@Override
	public String toShortString() {
		return toString();
	}
}
