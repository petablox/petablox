package stamp.util;

import java.util.Arrays;

/**
 * Collection of array manipulation functions.
 */
public class ArrayHelper {
	public static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
}
