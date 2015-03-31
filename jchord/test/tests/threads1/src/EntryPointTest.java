public class EntryPointTest implements Runnable{

 public static void main(String[] args) {
   new Thread(new EntryPointTest("foo")).start();
 }

  String s;
  public EntryPointTest(String s) {
    this.s = s;
  }

 public void run() {
       System.out.println(s);
 }

}

