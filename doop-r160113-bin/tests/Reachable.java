public class Reachable
{
  public static void main(String[] ps)
  {
    Test1.test();
    Test2.test();
    Test3.test();
  }
}

/**
 * From the entry point, the function g is not reachable. If the
 * function is still analyzed (i.e. reachability is not considered),
 * then the points-to set of the field will contain two object. If the
 * function g is not analyzed, then the set will contain only one
 * object.
 */
class Test1
{
  static Object field;

  public static void test()
  {
    f();
  }

  static void f()
  {
    field = new Object();
  }

  static void g()
  {
    field = new Object();
  }
}

/**
 * The assignment to field in Base.f() is not reachable, but without
 * on-the-fly call graph construction the method will still be part of
 * the call graph.
 *
 * With partly on-the-fly call graph construction (i.e. based on an
 * initial call graph) the method will be part on the call graph as
 * well (unless the inital call graph is constructed using a pointer
 * analysis).
 *
 * In both cases, the points-to set of field will contain two
 * objects. Using fully on-the-fly call graph construction, the
 * points-to set will contain one object.
 */
class Test2
{
  static Object field;

  static void test()
  {
    Base b = new Extension();
    b.f();
  }


  static class Base
  {
    void f()
    {
      field = new Object();
    }
  }
  
  static class Extension extends Base
  {
    void f()
    {
      field = new Object();
    }
  }
}


/**
 * From the entry point, the function g is not reachable. There should
 * be no facts about the local variable in g.
 */
class Test3
{
  static Object field;
  static Object fieldNull;

  static void test()
  {
    f();
  }

  static void f()
  {
    field = new Object();
  }

  static void g()
  {
    Object local = field;
    fieldNull = local;
  }
}
