public class Main
{
  public static void main(String[] ps)
  {
    Heap1.test();
  }
}

class Heap1
{
  /**
   * With a context-insensitive heap abstraction, the fields o1 and o2
   * will point to the same heap object.
   */
  static Object o1;
  static Object o2;

  static void test()
  {
    o1 = allocate();
    o2 = allocate();
  }

  static Object allocate()
  {
    return new Object();
  }
}
