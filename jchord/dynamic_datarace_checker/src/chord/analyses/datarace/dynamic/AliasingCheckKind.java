package chord.analyses.datarace.dynamic;

public enum AliasingCheckKind {
	CONCRETE,
	WEAK_CONCRETE, // does not distinguish different elements of same array
	ABSTRACT
}

