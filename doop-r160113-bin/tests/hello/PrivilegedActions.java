import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.AccessController;
import java.security.AccessControlContext;

public class PrivilegedActions
{
  public static Object o1;
  public static Object o2;
  public static Object o3;
  public static Object o4;
  public static Object o5;

  public static void main(String[] ps)
  {
    test1();
    test2();
    test3();
    test4();
    test5();
  }

  public static void test1()
  {
    o1 = AccessController.doPrivileged(new Test1());
  }

  public static void test2()
  {
    o2 = AccessController.doPrivileged(new Test2());
  }

  public static void test3()
  {
    try
    {
      o3 = AccessController.doPrivileged(new Test3());
    }
    catch(Exception exc)
    {
      // TODO test the exception
    }
  }

  public static void test4()
  {
    AccessControlContext acc = AccessController.getContext();
    o4 = AccessController.doPrivileged(new Test4(), acc);
  }

  public static void test5()
  {
    try
    {
      AccessControlContext acc = AccessController.getContext();
      o5 = AccessController.doPrivileged(new Test5(), acc);
    }
    catch(Exception exc)
    {
      // TODO test the exception
    }
  }
}

class Test1 implements PrivilegedAction
{
  public static Object o;

  public Object run()
  {
    o = new Object();
    return o;
  }
}

/**
 * Test virtual method resolution
 */
class Test2Base implements PrivilegedAction
{
  public static Object o;

  public Object run()
  {
    o = new Object();
    return o;
  }
}

class Test2 extends Test2Base
{
}

class Test3 implements PrivilegedExceptionAction
{
  public static Object o;

  public Object run()
  {
    o = new Object();
    return o;
  }
}

class Test4 implements PrivilegedAction
{
  public static Object o;

  public Object run()
  {
    o = new Object();
    return o;
  }
}

class Test5 implements PrivilegedExceptionAction
{
  public static Object o;

  public Object run()
  {
    o = new Object();
    return o;
  }
}
