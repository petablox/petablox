package chord.project.analyses.parallelizer;

/**
 * Interface seen by the workers for invoking the client analysis.
 * The apply method should execute the analysis being parallelized.
 */
public interface BlackBox {
	public String apply(String line);
}
