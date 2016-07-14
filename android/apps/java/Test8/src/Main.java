import edu.stanford.stamp.annotation.*;
/*
  Flow: no
 */
public class Main 
{
	public static void main(String[] args) 
	{
		Foo a1 = source();
		Foo b1 = foo(a1);

		Foo a2 = new Foo();
		Foo b2 = foo(a2);

		sink(b2);
	}

	static Foo foo(Foo a)
	{
		return a;
	}

	@STAMP(flows={@Flow(from="$Test8Src",to="@return")})
	private static Foo source() {
		return new Foo();
	}

	@STAMP(flows={@Flow(from="x",to="!Test8Sink")})
	private static void sink(Foo x) {}
}


class Foo {
}