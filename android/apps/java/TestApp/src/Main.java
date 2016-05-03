public class Main {
	public static void main(String[] args) {
		Foo a = new Foo();
		Foo b = new Foo();
		store_both(a, b);
		bar(a);
		loo(b);
	}

	private static void store_both(Foo k, Foo l) {
		Foo m = new Foo();
		k.f = m;
		l.f = m;
	}

	private static void bar(Foo y) {
		Foo t1 = source();
		Foo t2 = y.f;
		t2.g = t1;
	}

	private static void loo(Foo z) {
		Foo t3 = z.f;
		Foo t4 = t3.g;
		sink(t4);
	}

	private static Foo source() {
		return new Foo();
	}

	private static void sink(Foo x) {}
}

class Foo {
	public Foo f;
	public Foo g;
}
