public class StaticMethodReturn
{
  public static Object field;

  public static void main(String[] ps)
  {
    field = foo();
  }

  public static Object foo()
  {
    return new Object();
  }
}
