public class Simple3
{
  static Object o;

  static void entry ()
  {
    Simple3 simple1 = new Simple3();
    Simple3 simple2 = new Simple3();
    o = simple1.call();
    o = simple2.call();
  }

  Object call()
  {
    return allocate();
  }

  Object allocate()
  {
    return new Object();
  }
}
