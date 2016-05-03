import edu.stanford.stamp.annotation.*;
/*
  Flow: no
 */
public class Main 
{
	private Foo f;
	private Foo g;

	Main(Foo f)
	{ 
		this.f = f;
		this.g = new Foo();
	}

	public static void main(String[] args) 
	{
		Foo a = source();
		Main m = new Main(a);
		sink(m.g);
	}

	@STAMP(flows={@Flow(from="$Test3Src",to="@return")})
	private static Foo source() {
		return new Foo();
	}

	@STAMP(flows={@Flow(from="x",to="!Test3Sink")})
	private static void sink(Foo x) {}
}

class Foo {
}
