package chord.util;

import java.io.File;
import java.util.Collection;

/** String utilities.
 * Say {@code import static chord.Util.StringUtil.*;} */
public final class StringUtil {
	static public String path(String ... xs) {
		boolean first = true;
		StringBuilder b = new StringBuilder();
		for (String x : xs) {
			if (!first) b.append(File.separator); else first = false;
			b.append(x);
		}
		return b.toString();
	}

	static public String join(Collection parts, String sep){
		StringBuffer sb = new StringBuffer();
		int counter = 0;
		for(Object o : parts){
			if(counter != 0){
				sb.append(sep);
			}
			sb.append(o);
			counter++;
		}
		return sb.toString();
	}
	private StringUtil() {}
}

