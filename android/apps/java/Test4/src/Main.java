import edu.stanford.stamp.annotation.*;
/*
  Flow: yes
 */
public class Main 
{
	private Bar f;

	Main(Bar f)
	{ 
		this.f = f;
	}

	public static void main(String[] args) 
	{
		Foo a = source();
		Bar b = new Bar(a);
		Main m = new Main(b);
		m.baz();
	}

	void baz()
	{
		sink(f.getX());
	}

	@STAMP(flows={@Flow(from="$Test4Src",to="@return")})
	private static Foo source() {
		return new Foo();
	}

	@STAMP(flows={@Flow(from="x",to="!Test4Sink")})
	private static void sink(Foo x) {}
}

class Bar {
	private Foo x;
	private Foo y;

	Bar(Foo x)
	{
		this.x = x;
		this.y = new Foo();
	}
	
	Foo getX()
	{
		return x;
	}
	
	Foo getY()
	{
		return y;
	}
}

class Foo {
}