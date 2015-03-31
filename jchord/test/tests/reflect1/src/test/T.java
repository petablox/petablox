package test;

public class T extends B {
	int b;
	public T(int a, int b) {
		super((a < b) ? new Object() : new Object());
		this.b = b;
	}
    public static void main(String[] a) throws Exception {
		T t = new T(1, 1);
	}
}

class B {
	Object x;
	public B(Object x) { this.x = x; }
}
