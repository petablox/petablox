package test;

public class T {
	static A g;
	static int g2;
    public static void main(String[] a) {
		A v1 = new A();		// h1
		A v2 = new A();		// h2
		v1.f1 = v2;			// e1
		int x = v2.f2;		// e2
		g = v1;
		int y = v2.f2;		// e3
		A v4;
		if (a != null) {
			A v3 = new A();	// h3
			v4 = new A();	// h4
			v3.f1 = v4;		// e4
		} else
			v4 = new A();	// h5
		int z = v4.f2;		// e5
		g2 = z;
	}
}

class A {
	A f1;
	int f2;
}

/*
path program analysis:
visitedE:
  e1, e2, e3, e4, e5
escE:
  e3
locEH:
  e1: h1, 
  e2: h1, h2
  e4: h3 
  e5: h3, h4

full program analysis:
  must be able to prove: e1, e2, e4
  must not be able to prove: e5 since h5 is not reached dynamically
*/
