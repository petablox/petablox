package test;

public class T {
	static A g;
    public static void main(String[] a) {
		A v1 = new A();
		A v2 = new A();
		System.out.println(v1.f[0]);
	}
}

class A {
	Object[] f;
	public A() {
		this.f = new Object[10];
	}
}
