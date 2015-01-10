public class Super {
  private static Object field;

  public static void main(String[] ps) {
    Base b = new Extension();
    field = b.foo();
  }
}

class Base {
  public Object foo() {
    return new Object();
  }
}

class Extension extends Base {
  public Object foo() {
    return super.foo();
  }
}
