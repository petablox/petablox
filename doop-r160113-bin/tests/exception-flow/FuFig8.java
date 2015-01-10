import java.io.IOException;

/**
 * Example from the paper by Chen Fu et al. in TSE'05 (extended
 * version of their ISSTA'04 paper)
 *
 * This example requires the V-DataReach analysis, which is not
 * benchmarked in the TSE paper.
 */
public class FuFig8 {
  public static void main(String[] ps) {
    test1();
    test2();
  }

  static void test1() {
    M m = new M();
    m.getDmy();
  }

  static void test2() {
    M m = new M();
    m.getRes();
  }
}

class A {
  void read() throws IOException {}
}

class Dmy extends A {
  void read() {}
}

class Res extends A {
  void read() throws IOException {
    throw new IOException();
  }
}

class W {
  A f;

  W(A a) { f = a; }

  void read() throws IOException {
    A a = this.f;
    a.read();
  }
}

class M {
  void getData(A a) throws IOException {
    W w = new W(a);
    w.read();
  }

  void getDmy() {
    try {
      A dmy = new Dmy();
      getData(dmy);
    }
    catch(IOException exc) {
    }
  }

  void getRes() {
    try {
      A res = new Res();
      getData(res);
    }
    catch(IOException exc) {
    }
  }
}