package petablox.android.util;

import java.util.ArrayList;
import java.util.List;

import petablox.project.analyses.ProgramDom;
import petablox.project.analyses.ProgramRel;
import petablox.util.IndexMap;

/**
 * Processor for the Petablox facts dumping template language.
 *
 * Recognizes the following commands:
 * <ul>
 * <li>{@code @SIZE:K@}: Expands to the size of domain {@code K}.</li>
 * <li>{@code @BITS:K@}: Expands to the minimum number of bits required to
 *     index all the elements of domain {@code K}.</li>
 * <li>{@code @ACC_SIZE:N@}: Expands to the number of distinct values
 *     accumulated under set {@code N}.</li>
 * <li>{@code @ACC_BITS:N@}: Expands to the minimum number of bits required to
 *     index all the values accumulated under set {@code N}.</li>
 * <li>{@code @DUMP:foo(X,Y):bla_Y_X@}: Prints out the string {@code bla_Y_X}
 *     for each tuple {@code (X,Y)} in relation {@code foo} (also works for
 *     relations of different arity).</li>
 * <li>{@code @DOM_DUMP:K(I,V):bla_I_V@}: Prints out the string {@code bla_I_V}
 *     for each element of domain {@code K}, with {@code I} replaced by the
 *     element's index, and {@code V} replaced by its unique name.</li>
 * <li>{@code @ACC_DUMP:N(I,V):bla_I_V@}: Prints out the string {@code bla_I_V}
 *     for each value accumulated under set {@code N}, with {@code I} replaced
 *     by the value's index in the set, and {@code V} replaced by the value
 *     itself.</li>
 * </ul>
 *
 * Handles substitution instructions as follows: A line containing {@code $K}
 * is duplicated {@code n} times, where {@code n} is the size of domain
 * {@code K}, with each instance of {@code $K} replaced by {@code 0},
 * {@code 1}, &hellip;, {@code n-1}, i.e. once for each index present in domain
 * {@code K}.
 */
public class FactsDumper extends TemplateTransformer {
	private static final String BAD_DUMP_ERR_MSG = "Malformed dumping command";
	private static final String BAD_ARITY_ERR_MSG = "Wrong arity for relation";
	private static final String BAD_CMD_ERR_MSG = "Unrecognized command";

	private final DomMap doms = new DomMap();
	private final RelMap rels = new RelMap(false);

	public FactsDumper() {
		super("@", ":", "$", "<", "#", ">");
	}

	@Override
	public Iterable<String> processCommand(String cmd, List<String> args)
		throws LineFilter.FilterException {
		if (cmd.equals("SIZE") && args.size() == 1) {
			int size = doms.getSize(args.get(0));
			return new Cell<String>(Integer.toString(size));
		} else if (cmd.equals("BITS") && args.size() == 1) {
			int size = doms.getSize(args.get(0));
			int reqdBits = requiredBits(size);
			return new Cell<String>(Integer.toString(reqdBits));
		} else if (cmd.equals("ACC_SIZE") && args.size() == 1) {
			int size = getAccSet(args.get(0)).size();
			return new Cell<String>(Integer.toString(size));
		} else if (cmd.equals("ACC_BITS") && args.size() == 1) {
			int size = getAccSet(args.get(0)).size();
			int reqdBits = requiredBits(size);
			return new Cell<String>(Integer.toString(reqdBits));
		} else if (cmd.equals("DUMP") && args.size() == 2) {
			Pair<String,String[]> relAndParams = parseDumpPattern(args.get(0));
			ProgramRel rel = rels.get(relAndParams.getX());
			String[] params = relAndParams.getY();
			if (params.length != rel.getDoms().length) {
				throw new LineFilter.FilterException(BAD_ARITY_ERR_MSG);
			}
			Iterable<int[]> intTupleSrc = rel.getAryNIntTuples();
			Iterable<String[]> strTupleSrc = new IntTuplePrinter(intTupleSrc);
			return new MultiStringReplacer(strTupleSrc, params, args.get(1));
		} else if (cmd.equals("DOM_DUMP") && args.size() == 2) {
			Pair<String,String[]> domAndParams = parseDumpPattern(args.get(0));
			ProgramDom dom = doms.get(domAndParams.getX());
			String[] params = domAndParams.getY();
			if (params.length != 2) {
				throw new LineFilter.FilterException(BAD_DUMP_ERR_MSG);
			}
			return new MultiStringReplacer(new DomElemPrinter(dom), params,
										   args.get(1));
		} else if (cmd.equals("ACC_DUMP") && args.size() == 2) {
			Pair<String,String[]> setNameAndParams =
				parseDumpPattern(args.get(0));
			IndexMap<String> accSet = getAccSet(setNameAndParams.getX());
			String[] params = setNameAndParams.getY();
			if (params.length != 2) {
				throw new LineFilter.FilterException(BAD_DUMP_ERR_MSG);
			}
			return new MultiStringReplacer(new IndexAdder(accSet), params,
										   args.get(1));
		} else {
			throw new LineFilter.FilterException(BAD_CMD_ERR_MSG);
		}
	}

	private static Pair<String,String[]> parseDumpPattern(String str)
		throws LineFilter.FilterException {
		String[] setNameAndParamsStr = str.split("\\(");
		if (setNameAndParamsStr.length != 2
			|| !setNameAndParamsStr[1].endsWith(")")) {
			throw new LineFilter.FilterException(BAD_DUMP_ERR_MSG);
		}
		String setName = setNameAndParamsStr[0];
		String paramsStr = setNameAndParamsStr[1];
		// remove the trailing ')'
		paramsStr = paramsStr.substring(0, paramsStr.length() - 1);
		String[] params = paramsStr.split(",");
		return new Pair<String,String[]>(setName, params);
	}

	private static int requiredBits(int size) {
		int usedBits = Integer.SIZE - Integer.numberOfLeadingZeros(size);
		return Math.max(1, usedBits);
	}

	@Override
	public Iterable<String> getSubstitutes(String pattern) {
		return makeNumRange(doms.getSize(pattern));
	}

	private static Iterable<String> makeNumRange(int limit) {
		List<String> list = new ArrayList<String>(limit);
		for (int i = 0; i < limit; i++) {
			list.add(Integer.toString(i));
		}
		return list;
	}

	@Override
	public void cleanup() {
		doms.clear();
		rels.clear();
	}

	private static class MultiStringReplacer extends Mapper<String,String[]> {
		private final String[] patterns;
		private final String baseStr;

		public MultiStringReplacer(Iterable<String[]> substsSrc,
								   String[] patterns, String baseStr) {
			super(substsSrc);
			this.patterns = patterns;
			this.baseStr = baseStr;
		}

		@Override
		public String mapElem(String[] substs) {
			assert substs.length == patterns.length;
			String res = baseStr;
			for (int i = 0; i < substs.length; i++) {
				res = res.replace(patterns[i], substs[i]);
			}
			return res;
		}
	}

	private static class IntTuplePrinter extends Mapper<String[],int[]> {
		public IntTuplePrinter(Iterable<int[]> intsSrc) {
			super(intsSrc);
		}

		@Override
		public String[] mapElem(int[] ints) {
			String[] strings = new String[ints.length];
			for (int i = 0; i < ints.length; i++) {
				strings[i] = Integer.toString(ints[i]);
			}
			return strings;
		}
	}

	private static class DomElemPrinter extends Mapper<String[],Object> {
		private final ProgramDom dom;

		public DomElemPrinter(ProgramDom dom) {
			super(dom);
			this.dom = dom;
		}

		@Override
		public String[] mapElem(Object value) {
			String[] idxAndUniqStr = new String[2];
			idxAndUniqStr[0] = Integer.toString(dom.indexOf(value));
			idxAndUniqStr[1] = dom.toUniqueString(value);
			return idxAndUniqStr;
		}
	}

	private static class IndexAdder extends Mapper<String[],String> {
		private final IndexMap<String> map;

		public IndexAdder(IndexMap<String> map) {
			super(map);
			this.map = map;
		}

		@Override
		public String[] mapElem(String value) {
			String[] idxAndVal = new String[2];
			idxAndVal[0] = Integer.toString(map.indexOf(value));
			idxAndVal[1] = value;
			return idxAndVal;
		}
	}
}
