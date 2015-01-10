public class VirtualMethod
{
  public static void main(String[] ps)
  {
    test1();
    test2();
    Test3.test();
    Test4.test();
    Test5.test();
  }

  public static void test1()
  {
    new Test1().foo();
  }

  public static void test2()
  {
    Test2Base.invoke(new Test2Base());
    Test2Base.invoke(new Test2());
  }
}

class Test1
{
  public void foo()
  {
  }
}

class Test2Base
{
  public static void invoke(Test2Base x)
  {
    x.foo();
  }

  // only invoked with this = Test2Base
  public void foo()
  {
    bar();
  }

  public void bar()
  {
  }
}

class Test2 extends Test2Base
{
  public void foo()
  {
  }

  // not reachable
  public void bar()
  {
  }
}

class Test3
{
  public static void test()
  {
    Base base = new Extension();
    base.foo();
  }

  static class Base
  {
    public void foo() {}  
  }
  
  static class Extension extends Base
  {
  }
}

class Test4
{
  private static Object field;

  public static void test()
  {
    Extension ext = new Extension();
    field = ext.foo();
  }

  static class Base
  {
    public Object foo()
    {
      return new Object();
    }    
  }
  
  static class Extension extends Base
  {
  }
}

class Test5
{
  public static void test()
  {
    Base o = new Extension();
    o.fred();
    o.foo();
    o.bar();
  }

  static class Base
  {
    public native void fred();
    public native void foo();

    public void bar()
    {
    }
  }

  static class Extension extends Base
  {
    public void foo()
    {
    }

    public native void bar();
  }
}