import edu.stanford.stamp.annotation.*;
/*
  Flow: yes
 */
public class Main 
{
	static Bar manufacture(Foo f)
	{
		return new Bar(f);
	}

	private Bar f;

	Main(Bar f)
	{ 
		this.f = f;
	}

	public static void main(String[] args) 
	{
		Foo a1 = source();
		Bar b1 = manufacture(a1);

		Foo a2 = new Foo();
		Bar b2 = manufacture(a2);

		Main m = new Main(b1);
		m.baz();
	}

	void baz()
	{
		sink(f.getX());
	}

	@STAMP(flows={@Flow(from="$Test7Src",to="@return")})
	private static Foo source() {
		return new Foo();
	}

	@STAMP(flows={@Flow(from="x",to="!Test7Sink")})
	private static void sink(Foo x) {}
}

class Bar {
	private Foo x;

	Bar(Foo x)
	{
		this.x = x;
	}
	
	Foo getX()
	{
		return x;
	}
}

class Foo {
}