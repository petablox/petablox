package petablox.android.analyses;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import petablox.android.paths.CtxtLabelPoint;
import petablox.android.paths.CtxtObjPoint;
import petablox.android.paths.CtxtVarPoint;
import petablox.android.paths.Path;
import petablox.android.paths.PathsAdapter;
import petablox.android.paths.Point;
import petablox.android.paths.StatFldPoint;
import petablox.android.paths.Step;
import petablox.android.util.PropertyHelper;

import petablox.project.Petablox;
import petablox.project.analyses.JavaAnalysis;
import soot.Unit;

@Petablox(
	name = "paths-printer-java"
)
public class PathsPrinterAnalysis extends JavaAnalysis {
	List<Unit> stack = new ArrayList<Unit>(); // the partial stack, bottom is on the left
	PrintWriter pw = null;

	@Override
	public void run() {
		String schemaFile = PropertyHelper.getProperty("petablox.android.paths.schema");
		String rawPathsFile = PropertyHelper.getProperty("petablox.android.paths.raw");
		String normalPathsFile =
			PropertyHelper.getProperty("petablox.android.paths.normal");
		String flatPathsFile = PropertyHelper.getProperty("petablox.android.paths.flat");

		try {
			PathsAdapter adapter = new PathsAdapter(schemaFile);
			adapter.normalizeRawPaths(rawPathsFile, normalPathsFile);

			List<Path> paths = adapter.getFlatPaths(rawPathsFile);
			pw = new PrintWriter(flatPathsFile);
			pw.println("PATHS: " + paths.size());
			pw.println();

			for (Path p : paths) {
				int breaks = 0;
				initStack(p.start);
				pw.println(p.start + " --> " + p.end);
				for (Step s : p.steps) {
					if (!recordStep(s.symbol, s.target)) {
						breaks++;
					}
					pw.println((s.reverse ? "<-" : "--" ) + s.symbol +
							   (s.reverse ? "-- " : "-> " ) + s.target);
				}
				if (breaks > 0) {
					pw.println("INVALID");
				}
				pw.println();
			}

			pw.close();
			pw = null;
		} catch(Exception exc) {
			throw new RuntimeException(exc);
		}
	}

	private void initStack(Point source) {
		stack.clear();
		boolean validMatch = recordStep(null, source);
		assert(validMatch);
	}

	// TODO:
	// - when moving in the stack, check that it's a call or return
	// - also verify the direction
	// - record more information when the stack breaks unexpectedly
	// - this doesn't work in the presence of self-recursion (generally when
	//   we allow moves that can cause a move to leave the stack unchanged)
	//   we might accept a call to self as not affecting the call stack, and
	//   similarly for the return (this wouldn't be a problem if we kept calls,
	//   returns and assignments as separate symbols).
	// - this functionality should be broken off into other classes
	//   e.g. in PartialStack, Ctxt, and *Point classes
	//   Ctxt, in particular, should contain the information of whether it's a
	//   just a call stack or a contextified abstract object (as long as we're
	//   sticking to kCFA-sensitivity)

	private boolean recordStep(String symbol, Point target) {
		Unit[] elems = null;

		// UGLY: This code should be moved into the different *Point classes
		if (target instanceof CtxtLabelPoint) {
			elems = ((CtxtLabelPoint) target).ctxt.getElems();
		} else if (target instanceof CtxtObjPoint) {
			Unit[] elemsObj = ((CtxtObjPoint) target).ctxt.getElems();
			elems = Arrays.copyOfRange(elemsObj, 1, elemsObj.length);
		} else if (target instanceof CtxtVarPoint) {
			elems = ((CtxtVarPoint) target).ctxt.getElems();
		} else if (target instanceof StatFldPoint) {
			// Stores and loads to static fields break the stack.
			stack.clear();
			return true;
		} else {
			assert(false);
		}

		if (!matchStack(elems, 0) &&  // match an intra-procedural move
			!matchStack(elems, 1) &&  // match a return (stack pop)
			!matchStack(elems, -1)) { // match a call (stack push)
			pw.println(">>> BROKEN: " + symbol + " <<<");
			initStack(target);
			return false;
		}
		return true;
	}

	private boolean matchStack(Unit[] elems, int base) {
		int elemsIdx = 0;
		int stackIdx = base;

		while (elemsIdx < elems.length && stackIdx < stack.size()) {
			// Neither the partial stack or the target's context is exhausted,
			// cross-check the next statement.
			if (stackIdx >= 0 &&
				!stack.get(stackIdx).equals(elems[elemsIdx])) {
				// Failed to match up the partial stack with the context.
				return false;
			}
			elemsIdx++;
			stackIdx++;
		}

		// Exhausted either the context or the partial stack without detecting
		// any conflict. Update the stack and return successfully.

		// Extend the partial stack upwards if needed.
		for (; elemsIdx < elems.length; elemsIdx++) {
			stack.add(elems[elemsIdx]);
		}
		// Extend the partial stack downwards if needed.
		for (int i = -(base + 1); i >= 0; i--) {
			stack.add(0, elems[i]);
		}
		// Pop off frames after a return.
		for (int i = base; i > 0; i--) {
			stack.remove(0);
		}

		return true;
	}
}
