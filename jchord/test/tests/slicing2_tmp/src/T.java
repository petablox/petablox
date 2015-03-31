public class T {
	static A g;

	static int g2;

  private static int foo(int i) {
    return i + 1; 
  }

  public static void main(String[] a) {
      int k;
      g = new A();

      if (a != null) {
        k = 4;
      } else {
        g2 = g.f2; 
      }

      switch (g.f3) {
        case 10 :
          System.out.println("Hello");
          break;
        case 20 :
          g2 = g.f3;
          break;
        default :
          g2 = foo(g2);    
          break;
      }

	}
}

class A {
	A f1;
	int f2;
  int f3;

  public A(){
      f2 = 3;
      f3 = 4;
  }
}
