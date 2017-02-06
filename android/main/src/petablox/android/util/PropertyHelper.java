package petablox.android.util;

/**
 * Collection of useful functions for manipulating system properties.
 */
public class PropertyHelper {
	public static String getProperty(String propName) {
		String propValue = System.getProperty(propName);
		if (propValue == null) {
			String msg = "Required system property " + propName + " not set";
			throw new IllegalStateException(msg);
		}
		return propValue;
	}

	public static boolean getBoolProp(String propName) {
		String strValue = getProperty(propName);
		if (strValue.equals("true")) {
			return true;
		} else if (strValue.equals("false")) {
			return false;
		} else {
			String msg =
				"Required boolean-type system property " + propName +
				" has non-boolean value " + strValue;
			throw new IllegalStateException(msg);
		}
	}
}
