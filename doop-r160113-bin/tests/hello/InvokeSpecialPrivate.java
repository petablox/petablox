/**
 * Test for invokespecial on a private method.
 */
public class InvokeSpecialPrivate
{
  public static void main(String[] ps)
  {
    Test1.test();
    Test2.test();
  }
}

class Test1
{
  public static Object baseField; // should point to Object allocated in Base
  public static Object extField; // should point to Object allocated in Extension

  public static void test()
  {
    Extension ext = new Extension();
    baseField = ext.callPrivate();
    extField = ext.foo();
  }
}

class Base
{
  /**
   * callPrivate will use an invokespecial to call foo(). If the
   * access of foo() is changed to public, then callPrivate will call
   * the overriding method foo in Extension.
   */
  public Object callPrivate()
  {
    return foo();
  }

  private Object foo()
  {
    return new Object();
  }
}

class Extension extends Base
{
  public Object foo()
  {
    return new Object();
  }
}

class Test2
{
  private void foo()
  {
  }

  public static void bar(Test2 t)
  {
    if(t != null)
    {
      t.foo();
    }
  }

  public static void test()
  {
    bar(null);
  }
}