package petablox.android.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import petablox.project.analyses.ProgramRel;

/**
 * Semi-generic parser for reading in Petablox relations from arbitrary, non-BDD
 * format files.
 *
 * The parser is guided by a set of rules specified in the templates file used
 * to initialize it. This file should contain zero or more lines of the format:
 * {@code "<pattern>:<relation>(<param-list>)"}, e.g.
 * {@code "src<%A> sink<%B>:RevFlow(%B,%A)"}, where:
 *
 * <ul>
 * <li>{@code <pattern>} is a string that will be compared against every line
 *     of the parsed file. The characters in this string will be compared
 *     verbatim with those on the input line, except for instances of strings
 *     enclosed between {@code '<'} and {@code '>'}, which will be matched
 *     against numbers. To trigger a match, the whole input line must be
 *     matched.
 *
 *     The above example will match lines like {@code "src2 sink6"}.</li>
 * <li>{@code <relation>} is the Petablox relation that will be updated with the
 *     tuple encoded in the matched line.
 *
 *     The above example would add tuples to relation {@code RelFlow}.</li>
 * <li>{@code <param-list>} is a comma-separated list of identifiers that will
 *     be replaced for each matching line according to the numbers matched by
 *     {@code "<...>"} instances inside {@code <pattern>}.
 *
 *     The above example, when matched against the line "src2 sink6", will add
 *     tuple (6,2) to Petablox relation RevFlow.</li>
 * </ul>
 *
 * After initializing the parser, you can use it to parse in multiple files
 * using {@link #parse(String)}.
 */
public class RelParser extends LineFilter {
	private final RelMap rels = new RelMap(true);
	private final List<LineMatcher> matchers = new ArrayList<LineMatcher>();
	private int processedLines = 0;
	private int matchedLines = 0;

	public RelParser(String ruleFile) throws IOException,
											 LineFilter.FilterException {
		BufferedReader reader = new BufferedReader(new FileReader(ruleFile));
		String rule;
		while ((rule = reader.readLine()) != null) {
			matchers.add(new LineMatcher(rule));
		}
	}

	public void parse(String inFile) throws IOException,
											LineFilter.FilterException {
		run(inFile, null);
	}

	@Override
	public void init() {
		processedLines = 0;
		matchedLines = 0;
	}

	@Override
	public void preProcessLine(String line) {
		boolean matchedAny = false;
		for (LineMatcher m : matchers) {
			if (m.match(line)) {
				matchedAny = true;
			}
		}
		processedLines++;
		if (matchedAny) {
			matchedLines++;
		}
	}

	@Override
	public void cleanup() {
		rels.clear();
	}

	public int getProcessedLines() {
		return processedLines;
	}

	public int getMatchedLines() {
		return matchedLines;
	}

	private class LineMatcher {
		private final Pattern linePat;
		private final int[] param2group;
		private final String relStr;

		public LineMatcher(String matchSpec)
			throws LineFilter.FilterException {
			// To showcase how this code works, we will use a running example:
			// matchSpec = "src<%A> sink<%B>:RevFlow(%B,%A)"

			List<String> toks = StringHelper.split(matchSpec, ":");
			checkSpec(toks.size() == 2, matchSpec);
			String matchStr = toks.get(0);
			String relAdderStr = toks.get(1);

			// matchStr = "src<%A> sink<%B>"
			// relAdderStr = "RevFlow(%B,%A)"

			Map<String,Integer> matcher2group = new HashMap<String,Integer>();
			Pattern outPat = Pattern.compile("^[^<>]*");
			Pattern inPat = Pattern.compile("^<([^<>]+)>");
			StringBuffer sb = new StringBuffer();
			int currGroup = 1;
			do {

				// 1st pass:
				//   matchStr = "src<%A> sink<%B>"
				//   sb = ""
				//   matcher2group = {}
				// 2nd pass:
				//   matchStr = " sink<%B>"
				//   sb = "src([0-9]+)"
				//   matcher2group = {"%A" -> 1}

				Matcher outMat = outPat.matcher(matchStr);
				outMat.lookingAt();
				sb.append(Pattern.quote(outMat.group()));
				matchStr = matchStr.substring(outMat.end());
				if (matchStr.equals("")) {
					break;
				}

				// 1st pass:
				//   matchStr = "<%A> sink<%B>"
				//   sb = "src"
				//   matcher2group = {}
				// 2nd pass:
				//   matchStr = "<%B>"
				//   sb = "src([0-9]+) sink"
				//   matcher2group = {"%A" -> 1}

				Matcher inMat = inPat.matcher(matchStr);
				inMat.lookingAt();
				matcher2group.put(inMat.group(1), new Integer(currGroup));
				currGroup++;
				sb.append("([0-9]+)");
				matchStr = matchStr.substring(inMat.end());

				// 1st pass:
				//   matchStr = " sink<%B>"
				//   sb = "src([0-9]+)"
				//   matcher2group = {"%A" -> 1}
				// 2nd pass:
				//   matchStr = ""
				//   sb = "src([0-9]+) sink([0-9]+)"
				//   matcher2group = {"%A" -> 1, "%B" -> 2}

			} while (!matchStr.equals(""));
			linePat = Pattern.compile(sb.toString());

			List<String> relAndParams = StringHelper.split(relAdderStr, "(");
			checkSpec(relAndParams.size() == 2, matchSpec);
			relStr = relAndParams.get(0);
			String paramsStr = relAndParams.get(1);
			checkSpec(paramsStr.endsWith(")"), matchSpec);
			paramsStr = paramsStr.substring(0, paramsStr.length() - 1);

			// relStr = "RevFlow"
			// paramsStr = "%B,%A"

			List<String> params = StringHelper.split(paramsStr, ",");
			int num_params = params.size();
			param2group = new int[num_params];
			for (int i = 0; i < num_params; i++) {
				param2group[i] = matcher2group.get(params.get(i)).intValue();
			}

			// param2group = [2,1]
			// This means: To construct a tuple to add to RevFlow, take the
			// second group matched by linePat and use it as the first element,
			// and take ths first group matched by linePat and use it as the
			// second element.

			// We retrieve the relation from the map at this point, to force
			// Petablox to open it, so that it gets saved as empty even if no
			// lines match.
			rels.get(relStr);
		}

		private void checkSpec(boolean assertion, String matchSpec)
			throws LineFilter.FilterException {
			if (!assertion) {
				String msg = "Invalid match specification: " + matchSpec;
				throw new LineFilter.FilterException(msg);
			}
		}

		public boolean match(String line) {
			Matcher m = linePat.matcher(line);
			if (m.matches()) { // the whole line must match
				int[] params = new int[param2group.length];
				for (int i = 0; i < param2group.length; i++) {
					params[i] = Integer.parseInt(m.group(param2group[i]));
				}
				// TODO: We have to retrieve the ProgramRel from the map on the
				// parent object instead of caching a local reference, to make
				// sure we renew our reference whenever the relation is removed
				// from memory.
				rels.get(relStr).add(params);
				return true;
			}
			return false;
		}
	}
}
