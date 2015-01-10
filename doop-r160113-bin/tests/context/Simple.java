/**
 * With a context-insensitive analysis, o3 and o4 will point to both
 * o1 and o2 because the parameter and return of the identity function
 * are modeled as a single variable.
 */
public class Simple {
  static Object o3;
  static Object o4;

  public static void main(String[] ps) {
    Object o1 = new Object();
    Object o2 = new Object();
    o3 = identity(o1);
    o4 = identity(o2);
  }

  static Object identity(Object o) {
    return o;
  }
}
