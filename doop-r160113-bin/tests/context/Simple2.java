/**
 * Example requiring 2-call-site calling context..
 */
public class Simple2 
{
  static Object o3;
  static Object o4;

  public static void main(String[] ps)
  {
    Object o1 = new Object();
    Object o2 = new Object();
    o3 = call(o1);
    o4 = call(o2);
  }

  static Object call(Object o) {
    return identity(o);
  }

  static Object identity(Object o) {
    return o;
  }
}
