import edu.stanford.stamp.annotation.*;
/*
  Flow: yes
 */
public class Main 
{
	private Foo[] fs;

	public static void main(String[] args) 
	{
		Foo a1 = source();
		Main b1 = new Main();
		b1.add(a1);

		Foo a2 = new Foo();
		Main b2 = new Main();
		b2.add(a2);

		Foo a3 = b1.get(); 
		Foo a4 = b2.get();

		nonsink(a4);
		sink(a3);
	} 

	Main()
	{
		fs = new Foo[8];
	}

	void add(Foo a)
	{
		fs[0] = a;
	}
	 
	Foo get()
	{
		return fs[0];
	}

	@STAMP(flows={@Flow(from="$Test11Src",to="@return")})
	private static Foo source() {
		return new Foo();
	}

	@STAMP(flows={@Flow(from="x",to="!Test11Sink")})
	private static void sink(Foo x) {}

	private static void nonsink(Foo x) {}
}


class Foo {
}