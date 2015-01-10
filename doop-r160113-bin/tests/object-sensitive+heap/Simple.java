/**
 * With a context-insensitive heap abstraction, the fields o1 and o2
 * will point to the same heap object.
 */
public class Simple
{
  static Object o;

  static void entry ()
  {
    Simple simple = new Simple();
    o = simple.allocate();
    o = simple.allocate();
  }

  Object allocate()
  {
    return new Object();
  }
}