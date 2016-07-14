package stamp.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import chord.util.IndexMap;

/**
 * Generic template language transformer. Extend it (by implementing all
 * abstract functions) to define different template schemes.
 *
 * Two kinds of template elements are supported:
 * <ul>
 * <li>Processing commands, which are enclosed within &ldquo;command
 *     delimiters&rdquo;. A command can optionally accept one or more
 *     arguments separated from the command name, and from each other, by
 *     &ldquo;argument delimiters&rdquo;. For example, if the command delimiter
 *     is {@code @} and the argument delimiter is {@code :}, then the string
 *     {@code @FOO:1:2@} would be a call to command {@code FOO} with two
 *     arguments, {@code 1} and {@code 2}.
 *
 *     Any commands found on the input document are passed to the
 *     {@link #processCommand(String,String[])} callback for processing. This
 *     method may return zero or more strings as &ldquo;results&rdquo;, to be
 *     printed in place of the command. In case there are more than one
 *     results, the same line is printed multiple times, once for each
 *     result.</li>
 *
 * <li>Substitution instructions, which are formed by concatenating the
 *     &ldquo;substitution mark&rdquo; with a string, called the
 *     &ldquo;substitution pattern&rdquo;. For example, if the substitution
 *     mark is {@code $}, then {@code $FOO} is a substitution instruction for
 *     the pattern {@code FOO}.
 *
 *     A line that contains a substitution instruction will be printed multiple
 *     times on the output document, once for each of the substitutes returned
 *     from the {@link #getSubstitutes(String)} callback for its pattern. If a
 *     line contains processing instructions for more than one distinct
 *     patterns, the line will be printed once for each element of the
 *     cartesian product of the sustitutes returned for each of the patterns.
 *     This doesn't happen for different instances of the same pattern.
 *
 *     Substitution instructions are executed after processing commands.</li>
 *
 * <li>Accumulator ranges, which are enclosed between a &ldquo;range open
 *     mark&rdquo; and a &ldquo;range close mark&rdquo;. Each range includes
 *     the name of an &ldquo;accumulation set&rdquo; and a string value to add
 *     to that set, separated by a &ldquo;range separator mark&rdquo;. For
 *     example, if the range open and close marks are {@code <} and {@code >},
 *     and the range separator is {@code #}, then {@code <N#bla>} is an
 *     instruction to add string {@code "bla"} to set {@code N}.
 *
 *     Each distinct string added to some accumulation set is indexed with a
 *     unique number, starting from {@code 0}. The output of an accumulator
 *     range instruction is normally its string argument, unless property
 *     {@value #ACC_RANGE_PRINT_INDEX_PROP} is {@code true}, in which case its
 *     index is printed instead.
 *
 *     Accumulator ranges are processed after substitution instructions.</li>
 * </ul>
 *
 * Limitations:
 * <ul>
 * <li>All the different kinds of marks must be distinct from each other.</li>
 * <li>Only one processing command is allowed per line.</li>
 * <li>Pattern strings extend as far as possible, up to the first non-uppercase
 *     latin, non-underscore character.</li>
 * <li>Nesting of accumulator ranges is not allowed.</li>
 * <li>Accumulation set names must be non-empty and can consist only of
 *     uppercase latin characters and underscores.</li>
 * <li>Expects that the callbacks always give the same result when passed the
 *     same input.</li>
 * </ul>
 */
public abstract class TemplateTransformer extends LineFilter {
	private static final String MULT_CMDS_ERR_MSG =
		"Line contains more than one processing command, or a malformed " +
		"command";
	private static final String BAD_CMD_ERR_MSG =
		"Malformed processing command: Missing or zero-length command name";
	private static final String ACC_RANGE_PRINT_INDEX_PROP =
		"stamp.dumper.accrange.printindex";

	private final String cmdDelim;
	private final String argsDelim;
	private final String substMark;
	private final Pattern accRangePat;

	private final AccumulationSetMap accSets = new AccumulationSetMap();

	public TemplateTransformer(String cmdDelim, String argsDelim,
							   String substMark, String accRangeOpen,
							   String accRangeSep, String accRangeClose) {
		this.cmdDelim = cmdDelim;
		this.argsDelim = argsDelim;
		this.substMark = substMark;
		String patStr =
			Pattern.quote(accRangeOpen) +
			"([A-Z_]+)" +
			Pattern.quote(accRangeSep) +
			"([^" + Pattern.quote(accRangeClose) + "]*)" +
			Pattern.quote(accRangeClose);
		accRangePat = Pattern.compile(patStr);
	}

	public abstract Iterable<String> processCommand(String cmd,
													List<String> args)
		throws LineFilter.FilterException;

	// TODO: This method can't throw a LineFilter.FilterException, because it's
	//       used inside an iterator, which can't throw checked exceptions.
	public abstract Iterable<String> getSubstitutes(String pattern);

	@Override
	public void preProcessLine(String line) throws LineFilter.FilterException {
		// Produce the output lines, but only to accumulate the values.
		for (String outLine : processLine(line)) {}
	}

	@Override
	public Iterable<String> processLine(String line)
		throws LineFilter.FilterException {
		Iterable<String> linesSrc;
		List<String> toks = StringHelper.split(line, cmdDelim);
		switch (toks.size()) {
		case 1: // no command sequence
			// only a single line to pass to the substitution engine
			linesSrc = new Cell(line);
			break;
		case 3: // a single command sequence
			List<String> cmdToks = StringHelper.split(toks.get(1), argsDelim);
			String cmd = cmdToks.get(0);
			if (cmd.length() == 0) {
				throw new LineFilter.FilterException(BAD_CMD_ERR_MSG);
			}
			List<String> args = cmdToks.subList(1, cmdToks.size());
			Iterable<String> cmdOutput = processCommand(cmd, args);
			linesSrc =
				new PrefixSuffixAdder(cmdOutput, toks.get(0), toks.get(2));
			break;
		default: // multiple command sequences (or malformed command sequence)
			throw new LineFilter.FilterException(MULT_CMDS_ERR_MSG);
		}
		boolean printIdx = Boolean.getBoolean(ACC_RANGE_PRINT_INDEX_PROP);
		return new AccRangeHandler(new PatternSubstituter(linesSrc), printIdx);
	}

	private class PatternSubstituter extends IterableWrapper<String,String> {
		public PatternSubstituter(Iterable<String> linesSrc) {
			super(linesSrc);
		}

		@Override
		public Iterable<String> processElem(String line) {
			// Collect all the distinct substitution patterns appearing in
			// the input line.
			List<String> patterns = new ArrayList<String>();
			int index = 0;
			while ((index = line.indexOf(substMark, index)) >= 0) {
				index += substMark.length();
				String pat = StringHelper.uppercasePrefix(line, index);
				if (!patterns.contains(pat)) {
					patterns.add(pat);
				}
				index += pat.length();
			}
			if (patterns.size() == 0) { // no substitution instruction
				return new Cell(line);
			}
			// Substitute each of the patterns, in the order encountered while
			// scanning the line. For each pattern, substitute all its
			// instances simultaneously.
			Iterable<String> outLines = null;
			for (String pat : patterns) {
				String instr = substMark + pat;
				Iterable<String> substs = getSubstitutes(pat);
				if (outLines == null) {
					outLines = new StringReplacer(substs, instr, line);
				} else {
					outLines =
						new WrapperStringReplacer(substs, instr, outLines);
				}
			}
			return outLines;
		}
	}

	private class AccRangeHandler extends Mapper<String,String> {
		private final boolean printIndex;

		public AccRangeHandler(Iterable<String> linesSrc, boolean printIndex) {
			super(linesSrc);
			this.printIndex = printIndex;
		}

		@Override
		public String mapElem(String line) {
			// TODO: Could check for nested ranges and malformed ranges (e.g.
			//       mismatched range open marks, or multiple occurrences of
			//       the range separator mark within a range).
			Matcher m = accRangePat.matcher(line);
			StringBuffer sb = new StringBuffer();
			while (m.find()) {
				String accSetName = m.group(1);
				IndexMap<String> accSet = accSets.get(accSetName);
				String accElem = m.group(2);
				// TODO: Could check that we don't find new elements on the
				//       second pass.
				int accElemIndex = accSet.getOrAdd(accElem);
				String output =
					(printIndex) ? Integer.toString(accElemIndex) : accElem;
				m.appendReplacement(sb, output);
			}
			m.appendTail(sb);
			return sb.toString();
		}
	}

	public IndexMap<String> getAccSet(String accSetName) {
		return accSets.get(accSetName);
	}

	private static class AccumulationSetMap
		extends LazyMap<String,IndexMap<String>> {
		@Override
		public IndexMap<String> lazyFill(String accSetName) {
			return new IndexMap<String>();
		}
	}

	private static class StringReplacer extends Mapper<String,String> {
		// TODO: Move this to an external class, base off MultiStringReplacer.
		private final String pattern;
		private final String str;

		public StringReplacer(Iterable<String> substsSrc, String pattern,
							  String str) {
			super(substsSrc);
			this.pattern = pattern;
			this.str = str;
		}

		@Override
		public String mapElem(String subst) {
			return str.replace(pattern, subst);
		}
	}

	private static class WrapperStringReplacer
		extends IterableWrapper<String,String> {
		private final Iterable<String> substsSrc;
		private final String pattern;

		public WrapperStringReplacer(Iterable<String> substsSrc,
									 String pattern,
									 Iterable<String> stringsSrc) {
			super(stringsSrc);
			this.substsSrc = substsSrc;
			this.pattern = pattern;
		}

		@Override
		public Iterable<String> processElem(String str) {
			return new StringReplacer(substsSrc, pattern, str);
		}
	}

	private static class PrefixSuffixAdder extends Mapper<String,String> {
		private final String prefix;
		private final String suffix;

		public PrefixSuffixAdder(Iterable<String> stringsSrc, String prefix,
								 String suffix) {
			super(stringsSrc);
			this.prefix = prefix;
			this.suffix = suffix;
		}

		@Override
		public String mapElem(String str) {
			return prefix + str + suffix;
		}
	}
}
