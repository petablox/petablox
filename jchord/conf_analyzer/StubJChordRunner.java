package jchord;
import jchord.project.*;
import jchord.project.analyses.JavaAnalysis;

public class StubJChordRunner {

  /**
   * @param args
   */
  public static void main(String[] args) {
    makeATask(args[0]);
  }
  
  public static void makeATask(String s) {
    try {
      Class<ITask> tclass = (Class<ITask>) Class.forName(s);
      
      ITask t = tclass.newInstance();
      t.getName();
      t.run();
    } catch(Exception e) {}
  }
  
  public static void snameAndRun(JavaAnalysis a, String s) {
    a.setName(s);
    a.run();
  }

}
