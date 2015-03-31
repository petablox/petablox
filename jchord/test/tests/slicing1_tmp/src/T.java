public class T extends B implements I, J{
	static A g = new A();
	static int g2 = 99999;
    public static void main(String[] a) {
		A v1 = new A();	
		A v2 = new A();	
		v1.f1 = v2;		
		int x = v2.f2;		
		g = v1;
		int y = v2.f2;		
		A v4;
		if (a != null) {
			A v3 = new A();	
			v4 = new A();	
			v3.f1 = v4;		
		} else
			v4 = new A();	
		int z = v4.f2;		
		g2 = z;
	}

  public int testAbst(){
    return g2;
  }

  public int foo(int i, A a){
    return i + a.f2;
  }
}

class A {
	A f1;
	int f2;
	public A() {
		this.f2 = 5;
	}
}

abstract class B {

  abstract public int testAbst();

}

interface I {

    char field1 = 0x00;

}

interface J {

    char field1 = 0x01;

}

