import edu.stanford.stamp.annotation.*;
/*
  Flow: yes
 */
public class Main 
{
	private Foo f;

	Main(Foo f)
	{ 
		this.f = f;
	}

	public static void main(String[] args) 
	{
		Foo a = source();
		Main m = new Main(a);
		sink(m.f);
	}

	@STAMP(flows={@Flow(from="$Test2Src",to="@return")})
	private static Foo source() {
		return new Foo();
	}

	@STAMP(flows={@Flow(from="x",to="!Test2Sink")})
	private static void sink(Foo x) {}
}

class Foo {
}
