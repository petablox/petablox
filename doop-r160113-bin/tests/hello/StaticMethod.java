public class StaticMethod
{
  public static void main(String[] ps)
  {
    foo(new Object());
  }

  public static void foo(Object o2)
  {
    Object o1 = o2;
  }
}
