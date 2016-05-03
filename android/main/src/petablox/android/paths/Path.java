package petablox.android.paths;

import java.util.List;

public class Path {
	public final Point start;
	public final Point end;
	public final List<Step> steps;

	public Path(Point start, Point end, List<Step> steps) {
		this.start = start;
		this.end = end;
		// TODO: Check that the final step targets 'end'.
		this.steps = steps;
	}
}
