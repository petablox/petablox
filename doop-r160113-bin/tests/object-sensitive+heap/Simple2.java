/**
 * With a context-insensitive heap abstraction, the fields o1 and o2
 * will point to the same heap object.
 */
public class Simple2
{
  static Object o;

  static void entry ()
  {
    Simple2 simple1 = new Simple2();
    Simple2 simple2 = new Simple2();
    o = simple1.allocate();
    o = simple2.allocate();
  }

  Object allocate()
  {
    return new Object();
  }
}
