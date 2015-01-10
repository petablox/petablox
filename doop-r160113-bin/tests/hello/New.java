public class New
{
  public static String s1;
  public static String s2;
  public static String s3;

  public static int[] a1;
  public static String[] a2;

  public static Object o1;  
  public static Object o2;

  public static void main(String[] ps)
  {
    s1 = "Foo";
    s2 = "Foo";
    s3 = new String(s1);

    a1 = new int[5];
    a2 = new String[5];

    o1 = new Object();
    o2 = new Object();

    /**
     * Finalization
     */
    Object o1 = new TestFinalize1();
    Object o2 = new TestFinalize2();

    TestClone1.test();
    TestClone2.test();
    TestClone3.test();
  }
}

class TestFinalize1
{
}

class TestFinalize2
{
  protected void finalize()
  {
  }
}

class TestClone1 implements Cloneable
{
  static TestClone1 field;

  static void test()
  {
    try
    {
      TestClone1 o = new TestClone1();
      field = (TestClone1) o.clone();
    }
    catch(CloneNotSupportedException exc)
    {
    }
  }
}

class TestClone2 implements Cloneable
{
  static TestClone2 field;

  static void test()
  {
    TestClone2 o = new TestClone2();
    field = (TestClone2) o.clone();
  }

  public Object clone()
  {
    try
    {
      return super.clone();
    }
    catch(CloneNotSupportedException exc)
    {
      return null; // impossible
    }
  }
}

class TestClone3 implements Cloneable
{
  static TestClone3 field;

  static void test()
  {
    TestClone3 o = new TestClone3();
    field = (TestClone3) o.clone();
  }

  public Object clone()
  {
    return new TestClone3();
  }
}