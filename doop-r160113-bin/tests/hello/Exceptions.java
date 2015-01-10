public class Exceptions
{
  public static void main(String[] ps) throws Exception
  {
    Test1.test();
    Test2.test();
    Test3.test();
    Test4.test();
    Test5.test();
    Test6.test();
    Test7.test();
    Test8a.test();
    Test8b.test();
    Test8c.test();
    Test9.test();
    Test10.test();
    Test11.test();
    Test12.test();
    Test13.test();
    Test14.test();
    Test15.test();
    Test16.test();
    Test17.test();
  }

}

class Test1
{
  /* does not throw exception */
  public static void test() throws Exception
  {
  }
}

class Test2
{
  /* throws exception */
  public static void test() throws Exception
  {
    throw new Exception();
  }
}

class Test3
{
  /* throws exception */
  public static void test() throws Exception
  {
    helper();
  }

  public static void helper() throws Exception
  {
    throw new Exception();
  }
}

class Test4
{
  /* throws two exceptions */
  public static void test() throws Exception
  {
    helper1();
    helper2();
  }

  public static void helper1() throws Exception
  {
    throw new Exception();
  }

  public static void helper2() throws Exception
  {
    throw new Exception();
  }
}

class Test5
{
  /* throws two exceptions */
  public static void test() throws Exception
  {
    helper();
  }

  public static void helper() throws Exception
  {
    helper1();
    helper2();
  }

  public static void helper1() throws Exception
  {
    throw new Exception();
  }

  public static void helper2() throws Exception
  {
    throw new Exception();
  }
}

class Test6
{
  public Test6() throws Exception
  {
    throw new Exception();
  }

  /* throws exception through constructor invocation */
  public static void test() throws Exception
  {
    new Test6();
  }
}


class Test7
{
  public void helper() throws Exception
  {
    throw new Exception();
  }

  /* throws exception through virtual method invocation */
  public static void test() throws Exception
  {
    new Test7().helper();
  }
}

class Test8a
{
  /* does not throw Exception */
  public static void test() throws Exception
  {
    try
    {
      helper();
    }
    catch(Exception exc)
    {}
  }

  public static void helper() throws Exception
  {
    throw new Exception();
  }
}

class Test8b
{
  /* does not throw Exception */
  public static void test() throws Exception
  {
    try
    {
      helper();
    }
    catch(Exception exc)
    {}

    try
    {
      helper();
    }
    catch(Exception exc)
    {}
  }

  public static void helper() throws Exception
  {
    throw new Exception();
  }
}

class Test8c
{
  /* does not throw Exception */
  public static void test() throws Exception
  {
    try
    {
      helper1();
    }
    catch(Exception exc)
    {}

    try
    {
      helper2();
    }
    catch(Exception exc)
    {}
  }

  public static void helper1() throws Exception
  {
    throw new Exception();
  }

  public static void helper2() throws Exception
  {
    throw new Exception();
  }
}


class Test9
{
  /* does throw Exception: Exception is not a RuntimeException */
  public static void test() throws Exception
  {
    try
    {
      helper();
    }
    catch(RuntimeException exc)
    {}
  }

  public static void helper() throws Exception
  {
    throw new Exception();
  }
}

class Test10
{
  /* throws Exception only from helper2 */
  public static void test() throws Exception
  {
    try
    {
      helper1();
    }
    catch(Exception exc)
    {}

    helper2();
  }

  public static void helper1() throws Exception
  {
    throw new Exception();
  }

  public static void helper2() throws Exception
  {
    throw new Exception();
  }
}

class Test11
{
  /* throws Exception only from helper2 */
  public static void test() throws Exception
  {
    try
    {
      helper1();
    }
    catch(Exception exc)
    {
      helper2();
    }
  }

  public static void helper1() throws Exception
  {
    throw new Exception();
  }

  public static void helper2() throws Exception
  {
    throw new Exception();
  }
}

class Test12
{
  /* catches IOException */
  public static void test() throws Exception
  {
    try
    {
      helper();
    }
    catch(Exception exc)
    {
    }
  }

  public static void helper() throws Exception
  {
    throw new java.io.IOException();
  }
}


class Test13
{
  // o points to the allocated exception
  static Object o;


  // test does not throw exceptions
  public static void test() throws Exception
  {
    try
    {
      throw new Exception();
    }
    catch(Exception exc)
    {
      o = exc;
    }
  }
}

class Test14
{
  // o does not point anywhere
  static Object o;

  public static void test() throws Exception
  {
    try
    {
      throw new Exception();
    }
    catch(RuntimeException exc)
    {
      o = exc;
    }
  }
}

class Test15
{
  // o points to the exception allocated in helper
  static Object o;

  public static void test() throws Exception
  {
    try
    {
      helper();
    }
    catch(Exception exc)
    {
      o = exc;
    }
  }

  public static void helper() throws Exception
  {
    throw new Exception();
  }
}

class Test16
{
  // imprecision of not considering the order of exception handlers:
  // both o1 and o2 point to the allocated Exception.
  static Object o1;
  static Object o2;

  public static void test() throws Exception
  {
    try
    {
      throw new RuntimeException();
    }
    catch(RuntimeException exc)
    {
      o1 = exc;
    }
    catch(Exception exc)
    {
      o2 = exc;
    }
  }
}

class Test17
{
  // imprecision of not considering the order of exception handlers:
  // this method throws a RuntimeException if the order is ignored.

  public static void test() throws Exception
  {
    try
    {
      throw new RuntimeException();
    }
    catch(RuntimeException exc)
    {
    }
    catch(Exception exc)
    {
      throw exc;
    }
  }
}


