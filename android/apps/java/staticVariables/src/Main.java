import edu.stanford.stamp.annotation.STAMP;
import edu.stanford.stamp.annotation.Flow;

// Regression tests on capturing flow through static fields
public class Main {
	private static String f;

	public static void main(String[] args) {
		f = source();
		leak(f);
	}

	@STAMP(flows={@Flow(from="str",to="!SOMESINK")})
	private static void leak(String str) {}

	@STAMP(flows={@Flow(from="$SOMESRC",to="@return")})
	private static String source() {
		return new String();
	}
}
