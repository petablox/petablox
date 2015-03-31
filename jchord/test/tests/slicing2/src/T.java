// check that backward propagation does not stop
// in the middle of a method (foo in the below case)
public class T {
	static A g;

	private static A foo() {
		return new A(0);
	}

	public static void main(String[] a) {
		g = foo();
	}
}

class B {
	int i;
    public B(int i) { this.i = i; }
}

class A extends B {
	public A(int i) {
		super(i);
	}
}
