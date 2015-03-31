package chord.analyses.confdep.docu;

import java.io.*;
import java.util.*;
import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.confdep.ConfDeps;
import chord.analyses.confdep.optnames.DomOpts;
import chord.bddbddb.Rel.RelView;
import chord.analyses.var.DomV;
import chord.analyses.alloc.DomH;

import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.OutDirUtils;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.Utils;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import edu.berkeley.confspell.OptDictionary;


@Chord(
  name = "ShowConfOptions"
)
public class ShowConfOptions extends JavaAnalysis {
  
  HashMap<String, String> methTypeTable = new HashMap<String, String>();
  
  {

    methTypeTable.put("java.net.InetSocketAddress <init> (Ljava/lang/String;I)V 1", "Address");
    methTypeTable.put("java.net.Socket <init> (Ljava/lang/String;I)V 1", "Address");
    methTypeTable.put("java.net.InetAddress getByName (Ljava/lang/String;)Ljava/net/InetAddress; 0", "Address");
    
    methTypeTable.put("java.lang.Boolean parseBoolean (Ljava/lang/String;)Z 0", "Boolean");
    methTypeTable.put("java.lang.Boolean valueOf (Ljava/lang/String;)Ljava/lang/Boolean; 0", "Boolean");
    
    methTypeTable.put("java.lang.Class forName (Ljava/lang/String;)Ljava/lang/Class; 0", "ClassName");
    methTypeTable.put("java.lang.Class forName (Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class; 0", "ClassName");
    methTypeTable.put("java.lang.ClassLoader loadClass (Ljava/lang/String;)Ljava/lang/Class; 1", "ClassName");
    methTypeTable.put("org.apache.hadoop.conf.Configuration getClassByName (Ljava/lang/String;)Ljava/lang/Class; 1", "ClassName");

    methTypeTable.put("java.text.SimpleDateFormat <init> (Ljava/lang/String;)V 1", "DateFormat");
    
    methTypeTable.put("java.io.File <init> (Ljava/lang/String;)V 1", "File");
    methTypeTable.put("java.io.File <init> (Ljava/lang/String;Ljava/lang/String;)V 1", "File");
    methTypeTable.put("java.io.FileReader <init> (Ljava/lang/String;)V 1", "File");
    
    methTypeTable.put("java.lang.Double parseDouble (Ljava/lang/String;)D 0", "Fraction");
    methTypeTable.put("java.lang.Float parseFloat (Ljava/lang/String;)F 0", "Fraction");
    methTypeTable.put("java.lang.Double valueOf (Ljava/lang/String;)Ljava/lang/Double; 0", "Fraction");
    methTypeTable.put("java.lang.Float valueOf (Ljava/lang/String;)Ljava/lang/Float; 0", "Fraction");
    methTypeTable.put("java.lang.Double <init> (Ljava/lang/String;)V 1", "Fraction");
    methTypeTable.put("java.lang.Float <init> (Ljava/lang/String;)V 1", "Fraction");

    methTypeTable.put("java.lang.Integer valueOf (Ljava/lang/String;)Ljava/lang/Integer; 0", "Integral");
    methTypeTable.put("java.lang.Long valueOf (Ljava/lang/String;)Ljava/lang/Long; 0", "Integral");
    methTypeTable.put("java.lang.Integer <init> (Ljava/lang/String;)V 1", "Integral");
    methTypeTable.put("java.lang.Long <init> (Ljava/lang/String;)V 1", "Integral");
    methTypeTable.put("java.lang.Integer parseInt (Ljava/lang/String;)I 0", "Integral");
    methTypeTable.put("java.lang.Long parseLong (Ljava/lang/String;)J 0", "Integral");
    
    methTypeTable.put("java.net.NetworkInterface getByName (Ljava/lang/String;)Ljava/net/NetworkInterface; 0", "NetworkInterface");

    methTypeTable.put("java.net.Socket <init> (Ljava/lang/String;I)V 2", "Portno");
    methTypeTable.put("java.net.InetSocketAddress <init> (Ljava/lang/String;I)V 2", "Portno");
    methTypeTable.put("java.net.InetSocketAddress <init> (Ljava/net/InetAddress;I)V 2", "Portno");
    
    methTypeTable.put("java.util.regex.Pattern matches (Ljava/lang/String;Ljava/lang/CharSequence;)Z 0", "Regex");
    methTypeTable.put("java.util.regex.Pattern compile (Ljava/lang/String;)Ljava.util.regex.Pattern; 0", "Regex");
    methTypeTable.put("java.util.regex.Pattern compile (Ljava/lang/String;I)Ljava.util.regex.Pattern; 0", "Regex");
    
    methTypeTable.put("java.util.Random <init> (J)V 1", "RandomSeed");
    methTypeTable.put("java.lang.Enum valueOf (Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum; 1", "Special");
    
    methTypeTable.put("java.lang.Thread sleep (J)V 0", "Time");
    methTypeTable.put("java.lang.Thread join (J)V 1", "Time");
    methTypeTable.put("java.nio.channels.Selector select (J)I 1 ", "Time");
    methTypeTable.put("java.util.Timer schedule (Ljava/util/TimerTask;JJ)V 2", "Time");
    methTypeTable.put("java.util.Timer schedule (Ljava/util/TimerTask;JJ)V 3", "Time");
        
    methTypeTable.put("java.net.URI create (Ljava/lang/String;)Ljava/net/URI; 0", "URI");
    methTypeTable.put("java.net.URI <init> (Ljava/lang/String;)V 1", "URI");
    methTypeTable.put("java.net.URL <init> (Ljava/lang/String;)V 1", "URL");
  }
  
  public boolean miniStrings = false;
  DomV domV;
  OptDictionary dict;
  String[] inScopePrefixes;
  @Override
  public void run() {
    dict = new OptDictionary();
    
    inScopePrefixes = Utils.toArray(System.getProperty("dictionary.scope", ""));
    if(inScopePrefixes.length == 0)
      inScopePrefixes = new String[] {""};
    
    ClassicProject project = ClassicProject.g();
    miniStrings = Utils.buildBoolProperty("useMiniStrings", false);
    boolean argGuesses = Utils.buildBoolProperty("argGuesses", false);
    
    project.runTask("cipa-0cfa-arr-dlog");
    project.runTask("findconf-dlog");
    
    if(miniStrings)
      project.runTask("mini-str-dlog");
    else
      project.runTask("strcomponents-dlog");
    
    project.runTask("CnfNodeSucc");

    project.runTask("Opt");
    domV = (DomV) project.getTrgt("V");
    DomH domH = (DomH) project.getTrgt("H");

    ConfDeps c = new ConfDeps();
    c.slurpDoms();
      //this dumps reads
    ConfDeps.dumpOptRegexes("conf_regex.txt", DomOpts.optSites());
      //now dump writes
    ConfDeps.dumpOptRegexes("conf_writes.txt", DomOpts.computeOptNames("confOptWrites", "confOptWriteLen", "confWritesByName", domH));
    project.runTask("conf-flow-dlog");

    System.out.println(new Date() + ": Done classifying.  Used " + methTypeTable.size() + " inference rules for conf typing");

    dumpFieldContents();
    dumpOptTypeGuesses();
    dumpReturnTypeGuesses();
    if(argGuesses)
      dumpArgTypeGuesses();
//    System.out.println(new Date() + ": Inferring opt types")
//    recordClassOpts(returnedMap);
    project.runTask("classname-type-inf-dlog");
    readOptClassCasts();
    
    PrintWriter writer = OutDirUtils.newPrintWriter( System.getProperty("dictionary.name", "options.dict"));
    dict.dump(writer, true);
    writer.close();
  }


  private void readOptClassCasts() {
    PrintWriter writer =
      OutDirUtils.newPrintWriter("opt_casts.txt");
    
    ProgramRel confArgRel =  (ProgramRel) ClassicProject.g().getTrgt("castTypes");//t, opt, v
    confArgRel.load();  
    
    HashMap<String, String> castClasses = new HashMap<String, String>(); //foreach cast, list of classes cast to
    for(Trio<jq_Type, String, Register> castT:  confArgRel.<jq_Type, String, Register>getAry3ValTuples()) {
//      Quad q = castT.val1;
//      if(!returnedMap.containsKey(q))
 //       continue;
      
 //     String optName = ConfDefines.optionPrefix(q) + returnedMap.get(q);
      String optName = castT.val1;
      Register r = castT.val2;
      
      if(optName == null || optName.equals(DomOpts.NONE) || !dict.contains(optName)) {
        continue;
      }
      if(!"ClassName".equals(dict.get(optName)))
        continue;
      
      String castType = castT.val0.getName();
      writer.println(optName + " cast to " + castType + " in " + domV.getMethod(r));
      
      String prev = castClasses.get(optName);
      if(prev == null)
        castClasses.put(optName, castType);
      else if(!prev.contains(castType)) {
        castClasses.put(optName, prev + " " + castType);
      }
      
    }
    
    for(Map.Entry<String, String> e: castClasses.entrySet()) {
      String v = e.getValue();
      
      dict.annotate(e.getKey(), v);
    }
    
    confArgRel.close();
    writer.close();
  }

  private static String condense(String v) {
    if(!v.contains(" "))
      return v;
    String[] classNames = v.split(" ");
    Class<?>[] classes = new Class<?>[classNames.length];
    for(int i=0; i < classNames.length; ++i) {
       try{ 
         classes[i] = Class.forName(classNames[i]);
       } catch (ClassNotFoundException ex) {
         ex.printStackTrace();
       }
    }
    
    for(int i= 0; i < classNames.length -1; ++i) {
      for(int j = 1; j < classNames.length; ++j) {

        // TODO Auto-generated method stub    
      }
    }
    
    
    
    StringBuilder sb = new StringBuilder();

    // TODO Auto-generated method stub
    sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
  }
  
  private void dumpReturnTypeGuesses() {
    PrintWriter writer =
      OutDirUtils.newPrintWriter("returned_conf_values.txt");
    
    ProgramRel confReturns =  (ProgramRel) ClassicProject.g().getTrgt("confReturn");//outputs m,Opt
    confReturns.load();  
    for(Pair<jq_Method, String> methReturn:  confReturns.<jq_Method, String>getAry2ValTuples()) {
      String optName = methReturn.val1;

      if(highContention(optName))
        continue;
      
      writer.println(methReturn.val0.getDeclaringClass() + " " + methReturn.val0.getName() + " returns " +  optName);
    }
    
    confReturns.close();
    writer.close();
  }
  
  private void dumpArgTypeGuesses() {
    PrintWriter writer =
      OutDirUtils.newPrintWriter("arg_conf_values.txt");
    
    ProgramRel confArgRel =  (ProgramRel) ClassicProject.g().getTrgt("confArg");//outputs m,z,opt
    confArgRel.load();  
    for(Trio<jq_Method, Integer, String> methArg:  confArgRel.<jq_Method, Integer, String>getAry3ValTuples()) {
      String optName = methArg.val2;
      int z = methArg.val1;
      
      if(highContention(optName))
        continue;
      
      writer.println(methArg.val0.getDeclaringClass() + " " + methArg.val0.getNameAndDesc().toString() + " arg " + z +  " "+  optName);
    }
    
    confArgRel.close();
    writer.close();
  }
  
  private void dumpFieldContents() {
    PrintWriter writer =
      OutDirUtils.newPrintWriter("field_conf_contents.txt");
    ProgramRel dynFldContents =  (ProgramRel) ClassicProject.g().getTrgt("instFieldHolds");//outputs h,f,i
    dynFldContents.load();  
    
    RelView fldView = dynFldContents.getView();
    fldView.delete(0);
    dumpFldVals("dynamic", writer, fldView);
    fldView.free();
    dynFldContents.close();
    
    ProgramRel statFldContents =  (ProgramRel) ClassicProject.g().getTrgt("statFieldHolds");//outputs f,i
    statFldContents.load();  
    fldView = statFldContents.getView();
    dumpFldVals("static ",writer, fldView);
    fldView.free();
    statFldContents.close();
    
    writer.close();
  }

  private void dumpFldVals(String nm, PrintWriter writer,
      RelView fldView) {

    for(Pair<jq_Field, String> fldHolds:  fldView.<jq_Field, String>getAry2ValTuples()) {
      String optName =  fldHolds.val1;

      if(fldHolds.val0 != null) {
        String fldName = fldHolds.val0.getDeclaringClass() + "."+ fldHolds.val0.getName() ;
        writer.println(nm + " field " + fldName+ " can hold " + optName);
      }
    }
  }
  
  private boolean highContention(String s) {
    int parts = 0;
//    if(s == null)
//      return false;
    
    for(int i=0; i < s.length(); ++i) 
      if(s.charAt(i) == '|')
        parts ++;
    
    return parts > 4;
  }

  private void dumpOptTypeGuesses() {
    
    Map<String,String> enumStrs = getOptEnums();
    
    PrintWriter writer =
      OutDirUtils.newPrintWriter("conf_dependency.txt");
    ProgramRel optSinks =
//      (ProgramRel) Project.getTrgt("constrainedMethI");//outputs m,i,z,opt
    (ProgramRel) ClassicProject.g().getTrgt("confMethCall");//outputs i,z,opt
    optSinks.load();
    
    ProgramRel timeOpts = (ProgramRel) ClassicProject.g().getTrgt("timeConf"); //outputs i
    timeOpts.load();

    ProgramRel opts = (ProgramRel) ClassicProject.g().getTrgt("OptNames");
    opts.load();

    for( Pair<String,Quad> opt: opts.<String,Quad>getAry2ValTuples() ) {

      Quad q = opt.val1;
      String optName = opt.val0;
      if(optName.equals(DomOpts.NONE))
        continue;
 //     String optName = ConfDefines.optionPrefix(q) + opt.getValue();
      
      String readBy = Invoke.getMethod(q).getMethod().getName().toString();
      Register r = Invoke.getDest(q).getRegister();
      jq_Method readingMeth = q.getMethod();
      
      writer.println("option " + optName + " read via " + readBy + " into " + r + "!" + readingMeth.getNameAndDesc()
         + "@" + readingMeth.getDeclaringClass().getName());
      
      if(highContention(optName)) {
        System.out.println("\tHigh contention. Skipping " + optName+ ".");
        continue;
      }
      
      RelView v = optSinks.getView();
      v.selectAndDelete(2, optName);

      HashSet<String> sinksSeen = new HashSet<String>();
      String optionTypeByCall = null;
      
      String splitPat = null;
      int orCount = 0;
      
      for(Pair<Quad, Integer> t: v.<Quad, Integer>getAry2ValTuples()) {
        jq_Method calledMethod = Invoke.getMethod(t.val0).getMethod();
        String calledMethName = calledMethod.getNameAndDesc().toString();
        String calledClassName = calledMethod.getDeclaringClass().toString();
        String callSpec = calledClassName+ " " + calledMethName + " param " + t.val1;
        if(sinksSeen.contains(callSpec))
          continue;
        if(callSpec.equals("java.lang.String split (Ljava/lang/String;)[Ljava/lang/String; param 0") ||
            callSpec.equals("java.lang.String split (Ljava/lang/String;I)[Ljava/lang/String; param 0")) {
          //Register rreg = Invoke.getParam(t.val0,1).
//          writer.println("SAW SPLIT");
          splitPat = ",";//JUST FOR TEST PURPOSES.
        }

        
        sinksSeen.add(callSpec);
        String guessedType = typeByMethod(calledMethName, calledClassName, t.val1);
        if(optionTypeByCall == null) {
          optionTypeByCall = guessedType;
        } else 
          if(guessedType != null && !optionTypeByCall.contains(guessedType)) {
            if(optionTypeByCall.equals("Integral") && guessedType.equals("Time"))
              optionTypeByCall = "Time";
            else {
              optionTypeByCall = optionTypeByCall + " or " + guessedType;
              orCount += 1;
            }
          }
        
        jq_Method containingMethod = t.val0.getMethod();
        String useLocation =  containingMethod.getDeclaringClass().getName() + ":"  + 
            t.val0.getLineNumber() + " (" + containingMethod.getName()+ " )";
        
        writer.print("\t" + callSpec  + " at ");
        writer.print(useLocation);
        if(guessedType != null) {
          writer.print("  ******");
        }
        writer.println(""); 
      }
      v.free();
      
      String guess = null;
      String reason = "";
      
      //if we can get a type using the readBy technique, use that. 
      //  else try meth call.  Time is LAST option
      if(readBy.contains("Boolean") || readBy.contains("Bool")) {
         guess = "Boolean";
         reason = "read by " + readBy;
      } else if(readBy.contains("Double") ||  readBy.contains("Float")) {
        guess = "Fraction";
        reason = "read by " + readBy;
      } else if(readBy.contains("Class") ) {
        guess = "ClassName";
        reason = "read by " + readBy;
      } else if(readBy.contains("getRange")) {
        guess = "IntRange";
        reason = "read by " + readBy;
      }  else if(readBy.contains("getInetAddress") || readBy.contains("getInetSocketAddress")) {
        guess = "Address";
        reason = "read by " + readBy;
      } else {
        String compareSet = null;
        

        if((optionTypeByCall == null || optionTypeByCall.equals("Integral")) && timeOpts.contains(optName)) {
          guess = "Time";
          reason = "mingled with curtime";
        } else if(orCount < 3) {
          
          if(optionTypeByCall != null) {
            guess =  optionTypeByCall;
            reason = "based on called methods";
            if(optionTypeByCall.equals("Special")) {
              reason = "enum set {" + enumStrs.get(optName)+"}";
              dict.annotate(optName , "{" + enumStrs.get(optName) + "}");
            }
          } else if( (compareSet = getCompareSet(optName)) != null && compareSet.indexOf(',') > 0){
            guess = "Special";
            reason = "compared against set {" + compareSet+"}";
          }
          if(compareSet != null && compareSet.length() > 0)
            dict.annotate(optName , "{" + compareSet + "}"); //even if only one element, even if not special

        }
      } //end not-read-by
            
      if(guess != null) {
        writer.println("guessing " + optName + " is " + guess + " -- " +  reason);
        updateDict(q, optName, guess + (splitPat == null? "": " list"));
      } else if(readBy.contains("getInt") || readBy.contains("getLong")) {
//      writer.println("guessing " + optName + " is Size or Count -- read by " + readBy);
        writer.println("no guess for " + optName + " (but it's integer)");
        updateDict(q, optName, "Integral"+ (splitPat == null? "": " list"));
      } else {
        writer.println("no guess for " + optName);
        updateDict(q, optName, null);
      }
      writer.println();
    //leave a blank line between options
      
    }//end loop over options
    
    timeOpts.close();
    optSinks.close();
    writer.close();
    
  }

  private void updateDict(Quad q, String optName, String string) {
    String srcOfRead = q.getMethod().getDeclaringClass().getName();
    if(Utils.prefixMatch(srcOfRead, inScopePrefixes)) {
      dict.update(optName, string);
      return;
    }
  }

  private String getCompareSet(String src) {
    ProgramRel allowableOpts = (ProgramRel) ClassicProject.g().getTrgt("allowableOpts");//outputs opt,const
    allowableOpts.load();
    
    RelView allowedFor = allowableOpts.getView();
    allowedFor.selectAndDelete(0, src);
    
    String rVal = null;
    
    if(allowedFor.size() > 0) {
      StringBuffer sb = new StringBuffer();
      for(String s: allowedFor.<String>getAry1ValTuples()) {
        if(s.length() > 0) {
          sb.append(s);
          sb.append(",");
        }
      }
      if(sb.length() > 0)
        sb.deleteCharAt(sb.length() -1);
      rVal = sb.toString();
    }
    allowedFor.free();
    allowableOpts.close();
    return rVal;
  }

  private String typeByMethod(String calledMethName, String calledClassName,
      Integer parmNo) {
    String lookupKey = calledClassName + " " + calledMethName + " " + parmNo;
    String val =  methTypeTable.get(lookupKey);
    if(val != null)
      return val;
    if(calledMethName.equals("sleep") || calledMethName.equals("wait"))
      return "Time";
    else
      return null;
  }
  
  private Map<String,String> getOptEnums() {
    HashMap<String,String> optEnums = new HashMap<String,String>();
    ProgramRel enumOpt = (ProgramRel) ClassicProject.g().getTrgt("enumOpts");//outputs Opt,const
    enumOpt.load();
    
    for(Pair<String, jq_Type> t: enumOpt.<String, jq_Type>getAry2ValTuples()) {
    	
    	if(!optEnums.containsKey(t.val0))
    		System.out.println("adding enum values for " + t.val0 + " , using fields from type " + t.val1.getName());
    	
      jq_Class eType =  (jq_Class) t.val1;
      StringBuilder vals = new StringBuilder("");
      Class<?> cl = eType.getJavaLangClassObject();
      if(cl == null)
        continue;
      if(cl.getEnumConstants() == null || cl.getEnumConstants().length == 0) {
      	System.err.println("WARN: no enum constants for " + t.val0 + " of type " + t.val1.getName());
      	continue;
      }
      
      for(Object f: cl.getEnumConstants()) {
        vals.append(f.toString());
        vals.append(",");
      } 
      vals.deleteCharAt(vals.length() -1);
      
      optEnums.put(t.val0, vals.toString());
    }
    enumOpt.close();
    return optEnums;
  }
  

}
