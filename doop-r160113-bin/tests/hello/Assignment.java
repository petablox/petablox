/**
 * Example from Lhotak's Ph.D. thesis page 8.
 */
public class Assignment {
  private static Object a;
  private static Object b;
  private static Object c;

  public static void main(String[] ps) {
    a = new Object();
    b = new Object();
    c = new Object();
    a = b;
    b = a;
    c = b;
  }

  // a = {Object/0, Object/1}
  // b = {Object/0, Object/1}
  // c = {Object/0, Object/1, Object/2}
}