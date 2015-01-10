public class VirtualMethodParam {

  public static void main(String[] ps) {
    new VirtualMethodParam().foo(new Object());
  }

  public void foo(Object o) {
    Object o1 = o;
  }
}
