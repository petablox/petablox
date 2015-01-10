public class InstanceField
{
  private Object field;

  private void f()
  {
    field = new Object();
    g();
  }

  private void g()
  {
    Object local = field;
  }

  public static void main(String[] ps)
  {
    entry1();
    entry2();
  }

  public static void entry1()
  {
    new InstanceField().f();
  }

  public static void entry2()
  {
    new InstanceField().f();
  }
}
