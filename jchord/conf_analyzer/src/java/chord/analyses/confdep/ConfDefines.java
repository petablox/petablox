package chord.analyses.confdep;

import java.util.HashSet;

import chord.bddbddb.Rel;
import chord.bddbddb.Rel.RelView;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operator.Invoke;

public class ConfDefines {
  
  static HashSet<String> hadoopConfOptMethods = new HashSet<String>();
  static HashSet<String> primWrapConf = new HashSet<String>();
  public static HashSet<String> wrapperClasses;
  
  public static void wInit() {
    if(wrapperClasses != null)
      return;
    
    wrapperClasses = new HashSet<String>();
    wrapperClasses.add("java.lang.Boolean");
    wrapperClasses.add("java.lang.Long");
    wrapperClasses.add("java.lang.Double");
    wrapperClasses.add("java.lang.Float");
    wrapperClasses.add("java.lang.Integer");
    wrapperClasses.add("java.lang.Short");
    wrapperClasses.add("java.lang.Byte");
  }

  static {
    hadoopConfOptMethods.add("getInt");
    hadoopConfOptMethods.add("getFloat");
    hadoopConfOptMethods.add("getLong");
    hadoopConfOptMethods.add("get");
    hadoopConfOptMethods.add("getClass");
    hadoopConfOptMethods.add("getFile");
    hadoopConfOptMethods.add("getBoolean");
    hadoopConfOptMethods.add("getRange");
    hadoopConfOptMethods.add("getStringCollection");
    hadoopConfOptMethods.add("getStrings");
    hadoopConfOptMethods.add("getTrimmedStrings");//in Cloudera's distribution
    hadoopConfOptMethods.add("getTrimmedStringCollection"); //ditto
    hadoopConfOptMethods.add("getInstances"); //ditto

    hadoopConfOptMethods.add("getClasses");
    hadoopConfOptMethods.add("getLocalPath");
    
    wInit();
    
    for(String clName: wrapperClasses) {
      String coreName = clName.substring("java.lang.".length());
      primWrapConf.add(clName + " get" + coreName);
//      System.out.println("wrapper method get"+coreName);
    }
  }
  
  public static int confOptionPos(String classname, String methname) {
   if(classname.equals("java.lang.System")&& (methname.equals("getenv") || methname.equals("getProperty"))) {
      return 0;  
    } else if (primWrapConf.contains(classname +" " + methname)) { 
      return 0;
    } else if((classname.equals("org.apache.hadoop.conf.Configuration") || 
        classname.equals("org.apache.hadoop.mapred.JobConf")) &&
        hadoopConfOptMethods.contains(methname)) {
      return 1;
    }  else if(classname.startsWith("org.jivesoftware.resource") && (methname.equals("getURL") ||
        methname.equals("getString"))) {
      return 0;
    } else if (classname.equals("org.apache.cassandra.utils.XMLUtils")) {
      if(methname.startsWith("getNodeValue") || methname.equals("getRequestedNodeList"))
        return 1;
      if(methname.equals("getAttributeValue")) //FIXME: need something more subtle here
        return 1;
    } else if(classname.equals("java.util.Properties") && methname.equals("getProperty"))
      return 1;
    else if((classname.equals("rice.environment.params.Parameters") || classname.equals("rice.environment.params.simple.SimpleParameters"))
        && methname.startsWith("get"))
      return 1;
    else if((classname.endsWith("chord.project.Properties") ||classname.endsWith("chord.project.Config")) && (methname.equals("buildBoolProperty") ||
        methname.equals("mainRel2AbsPath") || methname.equals("workRel2AbsPath") || methname.equals("outRel2AbsPath")))
      return 0;
    else if(classname.equals("nachos.machine.Config") && methname.startsWith("get"))
      return 0;
    else if(classname.equals("org.apache.avalon.framework.configuration.Configuration")) {
      if(methname.startsWith("getAttribute")) {
        return 1;
      } else if(methname.startsWith("getValue"))
        return 0;
    } else if(classname.equals("org.apache.derby.iapi.services.property.PropertyUtil")) {
      return 0;
//      assert false: "should be handled below, using quad";
    } else if(classname.equals("org.apache.derby.impl.services.monitor.FileMonitor") && methname.endsWith("getJVMProperty"))
        return 1;
    else if ((classname.equals("org.apache.derby.impl.jdbc.authentication.AuthenticationServiceBase") 
        || classname.equals("org.apache.derby.iapi.util.DoubleProperties")) &&
        (methname.equals("getSystemProperty") || methname.equals("getProperty")))  
      return 1;
    else if(classname.equals("org.apache.derby.impl.tools.ij.util") && methname.equals("getSystemProperty"))
      return 0;
    else if(classname.equals("org.apache.derby.impl.store.access.PropertyConglomerate") && methname.equals("getProperty"))
      return 2;
//    else if(classname.equals("org.hsqldb.persist.HsqlProperties")) {
//    	if(methname.startsWith("get") && methname.endsWith("Property"))
//    		return 1;
 //   	else return -1;
 //   }
//    else if(classname.equals("org.apache.derby.iapi.tools.i18n") && methname.equals())
//    return -1;
   return -1;
  }
  
  
  public static boolean isConf(String classname, String methname) {
    if(classname.equals("org.apache.derby.iapi.services.property.PropertyUtil") &&
        methname.startsWith("get"))
      return true;
    return confOptionPos(classname, methname) != -1;
  }
  

  /**
   * Should be only used in dynamic analysis
   */
  public static String optionPrefixByName(String classname, String methname) {
    if(classname.equals("java.lang.System")) {
      if(methname.equals("getProperty"))
        return "PROP-";
      else if(methname.equals("getenv"))
        return "$";
      else return "XXX-System."+ methname+ "-XXX";
    } else
      if(classname.startsWith("java.lang"))
        return "PROP-";
    
    if(classname.equals("java.util.Properties") && methname.equals("getProperty"))
      return "PROP-";
    if(classname.equals("rice.environment.params.Parameters") || classname.equals("rice.environment.params.simple.SimpleParameters")) {
      return "PROP-";
    }
    if(classname.equals("org.apache.hadoop.conf.Configuration") || classname.equals("org.apache.hadoop.mapred.JobConf"))
      return "CONF-";
    if(classname.equals("org.apache.cassandra.utils.XMLUtils"))
      return "CXCONF-";
    if(classname.startsWith("org.jivesoftware.resource"))
      return "JIVECONF-"; 
    if(classname.startsWith("nachos.machine.Config"))
        return "PROP-";
    if(classname.startsWith("org.apache.tools.ant"))
    	return "PROP-";
    if(classname.startsWith("chord") || classname.startsWith("jchord"))
    	return "PROP-";
    else 
      return "";
  }
  
  public static String pruneName(String s) {
    if(s.startsWith("CONF-") || s.startsWith("PROP-"))
      return s.substring(5);
    if(s.startsWith("$"))
      return s.substring(1);
    else if(s.startsWith("CXCONF-"))
      return s.substring(7);
    else return s;
  }
  public static boolean EXPAND_RECURSIVE_CONF = true;
  
  static ProgramRel baseConfCall = null;
  public static String optionPrefix(Quad inst) {
    	if(baseConfCall == null && EXPAND_RECURSIVE_CONF) {
    		ClassicProject project = ClassicProject.g();
    		baseConfCall = (ProgramRel) project.getTrgt("confWrapper");
    		baseConfCall.load();
    	}
    	if(baseConfCall != null) {
    		RelView baseV = baseConfCall.getView();
    		baseV.selectAndDelete(0, inst);
    		for(Quad q: baseV.<Quad>getAry1ValTuples()) {
    			inst = q;
    			break;
    		}
    		baseV.free();
    	}
    	
//    if(o instanceof Quad) {
 //     Quad inst = (Quad) o;
      if(inst.getOperator() instanceof Invoke) {
        jq_Method m = Invoke.getMethod(inst).getMethod();
        String classname = m.getDeclaringClass().getName();
        String methname = m.getName().toString();
        return optionPrefixByName(classname, methname);
      }
      return "";
//    } //else if(o instanceof jq_Class) { } 
 //   else return "";
  }


  public static boolean isConf(Quad inst) {
    jq_Method container = inst.getMethod();
    
    if(isConf(container.getDeclaringClass().getName(),container.getName().toString()))
      return false;
    
    return confOptionPos(inst) != -1;
  }
  
  /**
   * If inst is a config option READ, returns the index of the parameter name argument
   * @param classname
   * @param methname
   * @return
   */
  public static int confOptionPos(Quad inst) {
    jq_Method container = inst.getMethod();

    if(isConf(container.getDeclaringClass().getName(),container.getName().toString()))
      return -1;
    
    if(inst.getOperator() instanceof Invoke) {
      jq_Method m = Invoke.getMethod(inst).getMethod();
      String classname = m.getDeclaringClass().getName();
      String methname = m.getName().toString();
      int args = m.getParamTypes().length;
      //special-case derby since need full quad, not just name, for resolution
      if(classname.equals("org.apache.derby.iapi.services.property.PropertyUtil")) {
        if(methname.startsWith("getSystem"))
          return 0;
        else if(methname.startsWith("getService") || methname.startsWith("getPropertyFromSet")) {
          jq_Type[] parms = m.getParamTypes(); //methods are static, so question of how to count 'this' is moot
          for(int i= 0; i< parms.length; ++i)
            if(parms[i].getName().equals("java.lang.String"))
              return i;
          System.out.println("WARN: didn't expect lack of string args to " + methname);
        } 
        return -1;//shouldn't get here
      }  else if(classname.equals("org.apache.tools.ant.PropertyHelper") && methname.equals("getProperty")) {
      	//ant has a getProperty with a namespace param.
      	return args -1;
      } else
        return confOptionPos(classname, methname);
    } else
      return -1;
  }
  
  
  /**
   * If inst is a config option WRITE, returns the index of the parameter name argument
   * @param classname
   * @param methname
   * @return
   */
  public static int confOptionWritePos(Quad inst) {
    if(inst.getOperator() instanceof Invoke) {
      jq_Method m = Invoke.getMethod(inst).getMethod();
      String classname = m.getDeclaringClass().getName();
      String methname = m.getName().toString();
      
      ParamListOperand plo = Invoke.getParamList(inst);

      if(classname.equals("java.lang.System")&&  methname.equals("setProperty")) {
        return 0;  
      } else if(methname.equals("put") || methname.equals("setProperty")) {
        jq_Type opType = Invoke.getParam(inst, 0).getType();
        if(classname.equals("java.util.Properties")  ||  //classname.equals("java.util.Hashtable"))
            "java.util.Properties".equals(opType.toString()))
           return 1;
      } //NOT else.  Need to move on to other puts  
      
      if((classname.equals("org.apache.hadoop.conf.Configuration") || 
        classname.equals("org.apache.hadoop.mapred.JobConf")) &&
        methname.startsWith("set") && (plo.length() > 2)) {
        return 1;
      } else  if(classname.equals("rice.environment.params.Parameters") || classname.equals("rice.environment.params.simple.SimpleParameters")) {
        if(methname.startsWith("set"))
          return 1;
      } else if(classname.equals("org.apache.derby.impl.store.access.PropertyConglomerate") &&
          methname.contains("setProperty"))
        return 2;
      //other invoke
      return -1;
      
    } else  //end if invoke
      return -1;
  }

}
