import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;

import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

// usage: -[eqlist|eqset|subset] file1 file2
// returns: 0 if eqlist/eqset/subset holds, 1 if it does not hold, and
// 2 if it crashes (e.g. file1 is not found)
public class FileCmp {
	public static void main(String[] args) {
		assert (args != null && args.length == 3);
		String fileName1 = args[1];
		String fileName2 = args[2];
		int retVal;
		String opt = args[0];
		if (opt.equals("-eqlist")) {
			retVal = testEquals(fileName1, fileName2, true);
		} else if (opt.equals("-eqset")) {
			retVal = testEquals(fileName1, fileName2, false);
		} else {
			assert(args[0].equals("-subset"));
			retVal = testSubset(fileName1, fileName2);
		}
		System.exit(retVal);
	}

	private static int testEquals(String fileName1, String fileName2, boolean isList) {
		Collection<String> lines1, lines2;
		try {
			if (isList) {
				lines1 = readFileToList(fileName1);
				lines2 = readFileToList(fileName2);
			} else {
				lines1 = readFileToSet(fileName1);
				lines2 = readFileToSet(fileName2);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			return 2;
		}
		boolean isFirst = true;
		for (String s : lines1) {
			if (!lines2.contains(s)) {
				if (isFirst) {
					System.out.println("FAILED: File " + fileName1 +
						" is not equal to file " + fileName2 + ":");
					isFirst = false;
				}
				System.out.println("< " + s);
			}
		}
		if (isFirst && lines2.size() == lines1.size()) {
			System.out.println("PASSED: File " + fileName1 +
				" is equal to file " + fileName2 + ".");
			return 0;
		}
		for (String s : lines2) {
			if (!lines1.contains(s)) {
				if (isFirst) {
					System.out.println("FAILED: File " + fileName1 +
						" is not equal to file " + fileName2 + ":");
					isFirst = false;
				}
				System.out.println("> " + s);
			}
		}
		return 1;
	}
	private static int testSubset(String fileName1, String fileName2) {
		Set<String> lines1, lines2;
		try {
			lines1 = readFileToSet(fileName1);
			lines2 = readFileToSet(fileName2);
		} catch (IOException ex) {
			ex.printStackTrace();
			return 2;
		}
		boolean isFirst = true;
		for (String s : lines1) {
			if (!lines2.contains(s)) {
				if (isFirst) {
					System.out.println("FAILED: File " + fileName1 +
						" is not a subset of file " + fileName2 + ":");
					isFirst = false;
				}
				System.out.println("< " + s);
			}
		}
		if (isFirst) {
			System.out.println("PASSED: File " + fileName1 +
				" is a subset of file " + fileName2 + ".");
			return 0;
		}
		return 1;
	}
	private static Set<String> readFileToSet(String fileName) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		Set<String> set = new HashSet<String>();
		String s;
		while ((s = in.readLine()) != null) {
			set.add(s);
		}
		in.close();
		return set;
	}
	private static List<String> readFileToList(String fileName) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		List<String> list = new ArrayList<String>();
		String s;
		while ((s = in.readLine()) != null) {
			list.add(s);
		}
		in.close();
		return list;
	}
}
