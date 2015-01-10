import java.io.*;
import java.util.Hashtable;

/**
 * Example from the paper by Chen Fu et al. in TSE'05 (extended
 * version of their ISSTA'04 paper)
 */
public class FuFig6 {
  public static void main(String[] ps) {
    read1();
    read2();
  }

  public static Exception fromRead1;
  public static Exception fromRead2;

  public static void read1() {
    try {
      A a = new A();
      Y y = new Y();
      a.m(y);
    }
    catch(IOException e) {
      fromRead1 = e;
    }
  }

  public static void read2() {
    try {
      A a = new A();
      Z z = new Z();
      a.m(z);
    }
    catch(IOException e) {
      fromRead2 = e;
    }
  }
}


class X {
  void read() throws IOException {
  }
}

class Y extends X {
  void read() throws IOException {
    throw new FileNotFoundException();  
  }
}

class Z extends X {
  void read() throws IOException {
    throw new EOFException();  
  }
}

class A {
  void m(X x) throws IOException {
    n(x);
    x.read();
  }

  void n(X x) {
    Hashtable ht = new Hashtable();
    ht.put("foo", x);
  }
}
