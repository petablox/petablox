public class T {
	static int g1;
	static int g2;
    public static void main(String[] args) {
		A a = new A();
		A b = new A();
		g1 = a.f;
		g2 = b.f;
	}
}

class A {
	int f;
	public A() {
		this.f = 5;
	}
}

