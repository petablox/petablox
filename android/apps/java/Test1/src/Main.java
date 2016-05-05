import edu.stanford.stamp.annotation.*;
/*
  Flow: no
 */
public class Main 
{
	public static void main(String[] args) 
	{
		Foo a = source();
		sink(new Foo());
	}

	@STAMP(flows={@Flow(from="$Test1Src",to="@return")})
	private static Foo source() {
		return new Foo();
	}

	@STAMP(flows={@Flow(from="x",to="!Test1Sink")})
	private static void sink(Foo x) {}
}

class Foo {
}
