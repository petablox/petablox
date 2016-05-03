import edu.stanford.stamp.annotation.*;
/*
  Flow: yes
 */
public class Main 
{
	public static void main(String[] args) 
	{
		Foo a = source();
		sink(a);
	}

	@STAMP(flows={@Flow(from="$Test0Src",to="@return")})
	private static Foo source() {
		return new Foo();
	}

	@STAMP(flows={@Flow(from="x",to="!Test0Sink")})
	private static void sink(Foo x) {}
}


class Foo {
}
