package stamp.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collection of string manipulation functions.
 */
public class StringHelper {
	public static List<String> split(String str, String delim) {
		List<String> toks = new ArrayList<String>();
		int prev = 0;
		int index;
		while ((index = str.indexOf(delim, prev)) >= 0) {
			toks.add(str.substring(prev, index));
			prev = index + delim.length();
		}
		toks.add(str.substring(prev, str.length()));
		return toks;
	}

	public static String uppercasePrefix(String str, int start) {
		Pattern pat = Pattern.compile("[A-Z_]*");
		Matcher matcher = pat.matcher(str).region(start, str.length());
		matcher.lookingAt();
		return matcher.group();
	}
}
