package test;

public class T {
	static C g;
	public static void main(String[] args) {
		A a = new A();		// h1
		if (args == null) {
			B b = new B();	// h2
			a.bf = b;		// e1
		} else {
			B b = new B();	// h3
			C c = new C();	// h4
 			D d = new D();	// h5
			a.bf = b;		// e2
			a.cf = c;		// e3
			b.df = d;		// e4
			c.df = d;		// e5
			g = c;
		}
		int i, j;
		{
			B b = a.bf;		// e6
			D d = b.df;		// e7
			i = d.i;		// e8
		}
		{
			C c = a.cf;		// e9
			D d = c.df;		// e10
			j = d.i;		// e11
		}
		System.out.println(i + j);
	}
}

class A {
	B bf;
	C cf;
}

class B {
	D df;
}

class C {
	D df;
}

class D {
	int i;
}

/*
path program analysis:
visitedE:
  e2, e3, e4, e5, e6, e7, e8, e9, e10, e11
escE:
  e8, e10, e11
locEH:
  e2, e3, e6, e9: h1
  e4, e7: h1, h3
  e5: h1, h4

full program analysis:
  must be able to prove: e2, e3, e4, e5, e6, e9
  must not be able to prove: e7 since h2 is not reached dynamically
*/

