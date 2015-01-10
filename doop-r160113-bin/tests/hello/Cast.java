public class Cast
{
  public static Object o1;
  public static Object o2;
  public static Object o3;
  public static Object o4;

  public static void main(String[] ps)
  {
    o1 = new String();
    o2 = (String) o1;
    foo(new Object());
    foo(new String());

    fail(new Integer(5));
    succeed(new String());
  }

  public static void foo(Object o)
  {
    o4 = o;

    if(o instanceof String)
    {
      o3 = (String) o;
    }
  }


  public static void fail(Object o)
  {
    String s = (String) o;
  }

  public static void succeed(Object o)
  {
    String s = (String) o;
  }
}
