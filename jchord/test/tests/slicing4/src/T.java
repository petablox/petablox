// test that t (and its alloc site) is included in slice although
// t is not directly relevant to slicing criterion.  this is
// necessary because the call to foo is relevant to the slicing
// criterion as foo contains a statement g = 1, and therefore
// t is relevant (without t, the call to instance method foo is
// meaningless).

public class T {
	static int g;
	public static void main(String[] a) {
		T t = new T();
		t.foo();
	}
	public void foo() {
		g = 1;
	}
}

