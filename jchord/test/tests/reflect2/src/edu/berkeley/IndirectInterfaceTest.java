package edu.berkeley;

interface Root {
  public void aMethod();
}

public class IndirectInterfaceTest implements Root {
  
  public void aMethod() {
    System.out.println("in parent implementation");
  }
  
  static class LeafClass extends IndirectInterfaceTest {
    public void aMethod() {
      System.out.println("in derived method");
    }
  }
  
  public static void main(String[] args) {
    try {
      String s = args[0]; //"edu.berkeley.IndirectInterfaceTest$LeafClass" ?
      Root h = (Root) Class.forName(s).newInstance();
      h.aMethod();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  

   
}

