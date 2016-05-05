import edu.stanford.stamp.annotation.STAMP;
import edu.stanford.stamp.annotation.Flow;

// Should find a flow.
// Tests whether we are correctly modelling the constraint that no value
// transitively reachable from an escaping object can be tainted.
public class Main {
	public static void main(String[] args) {
		String tainted = source();
		Foo obj = new Foo();
		obj.put(tainted);
		leakFoo(obj);
	}

	@STAMP(flows={@Flow(from="obj",to="!SOMESINK")})
	private static void leakFoo(Foo obj) {}

	@STAMP(flows={@Flow(from="$SOMESRC",to="@return")})
	private static String source() {
		return new String();
	}
}

class Foo {
	public String f;
	public void put(String str) {
		f = str;
	}
}
