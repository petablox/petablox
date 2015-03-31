package chord.analyses.confdep.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import chord.analyses.invk.DomI;
import chord.program.visitors.*;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.IndexSet;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Chord(
    name = "FailurePath",
    sign = "I0:I0"
  )
public class RelFailurePath extends ProgramRel implements IInvokeInstVisitor{
  
  public static String FAILTRACE_OPT = "chord.failtrace.file";
  DomI domI;
//  DomM domM;
  boolean methodMatches;
  jq_Method cur_m;
  
  Set<String> callsOnFailPath = new HashSet<String>();
  Map<String, Quad> pointOnTrace = new HashMap<String, Quad>();
  
  public void init() {
    domI = (DomI) doms[0];
    //domM = (DomM) doms[1];
    
    try {
      String straceFName = System.getProperty(FAILTRACE_OPT);
      if(straceFName == null) {
        System.err.println("need to set option failtrace.file if using super-context sensitivity");
        System.exit(-1);
      }
      else {
        File straceFile = new File(straceFName);
        if(!straceFile.exists()) {
          System.err.println("no such file " + straceFile);
          System.exit(-1);
        } else {
          slurpStacktrace(straceFile);
          System.out.println("FailurePath read stacktrace, found " + callsOnFailPath.size() + " methods on-path");
        }
      }
    } catch(IOException e) {
      e.printStackTrace();
    }
  }
  
  Pattern atLine = Pattern.compile(".*at ([^(]*)\\.([^:.]*)\\([^:]*:([0-9]*)\\).*");
  private void slurpStacktrace(File straceFile) throws IOException{
    
    BufferedReader br = new BufferedReader(new FileReader(straceFile));
    String ln;
    jq_Method prevM = null;
    while((ln= br.readLine()) != null) {
      Matcher m = atLine.matcher(ln);
      if(m.matches()) {
        String classname = m.group(1);
        String methname = m.group(2);
        int lNum = Integer.parseInt(m.group(3));

        callsOnFailPath.add(classname+ " " + methname+ " " + lNum);

        /*
        if(prevM != null)
        
        jq_Class cl = (jq_Class) jq_Type.parseType(classname);
        prevM = cl.getDeclaredMethod(methname);
*/        
//        else
//          System.err.println("call to " + prevM.getName());

      }
    }
    br.close();
  }

  public void visit(jq_Class c) { }
  public void visit(jq_Method m) {
    this.cur_m = m;
  }

  /**
   * We've build a list of class-lineno-method tuples from the stacktrace.
   * If a given quad matches, we add that quad to the relation.
   * 
   * We're adding call points, so no need to resolve callees
   */
  @Override
  public void visitInvokeInst(Quad q) {
    int lineNo = q.getLineNumber();
    String query = cur_m.getDeclaringClass().getName() + " " + cur_m.getName() + " " + lineNo;
//    System.err.println("query for " + query);
    if(callsOnFailPath.contains(query)) {
      super.add(q);
      callsOnFailPath.remove(query);
    }
  }
  
  
  
  @Override
  public void save() {
    //create relation here
    super.save();
    if(callsOnFailPath.size() > 0) {
    	System.out.println("couldn't find DomM/DomI entries to match some stacktrace lines: ");
	    for(String s: callsOnFailPath) {
	    	System.out.println(s);
	    }
    }
  }
  
}
