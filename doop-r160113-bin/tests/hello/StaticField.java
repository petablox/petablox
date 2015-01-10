public class StaticField {

  private static Object field;

  public static void f() {
    field = new Object();
  }

  public static void g() {
    field = new Object();
  }

  public static void main(String[] ps) {
    f();
    g();
  }
}