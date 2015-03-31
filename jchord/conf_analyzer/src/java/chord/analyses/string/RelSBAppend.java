package chord.analyses.string;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.analyses.primtrack.*;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.analyses.argret.DomK;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import java.util.*;

/**
 * (i,v1,comp,z) is in this relation if instruction i appends comp to v1 as the z'th element
 * @author asrabkin
 *
 */
@Chord(
    name = "SBAppend",
    
    consumes = { "I","V", "U", "K"},
    produces = { "SBAppendU", "SBAppendV"},
     signs = {"I0,V0,U0,K0:I0_V0_U0_K0","I0,V0,V1,K0:I0_V0_V1_K0"},
  namesOfSigns = { "SBAppendU", "SBAppendV"},
  namesOfTypes = { "I" ,"V", "U", "K"},
  types = { DomI.class, DomV.class, DomU.class, DomK.class }

//    sign = "I0,V0,UV0,Z0:I0_V0_UV0_Z0"
  )
public class RelSBAppend extends JavaAnalysis {
  DomI domI;
  DomV domV;
  DomU domU;
  DomK domK;
  jq_Method method;
  ProgramRel relRef, relPrim;
  public void init() {
    ClassicProject project = ClassicProject.g();
    domI = (DomI) project.getTrgt("I");
    domV = (DomV) project.getTrgt("V");
    domU = (DomU) project.getTrgt("U");
    domK = (DomK) project.getTrgt("K");
    relRef = (ProgramRel) project.getTrgt("SBAppendV");
    relPrim = (ProgramRel) project.getTrgt("SBAppendU");
    relRef.zero();
    relPrim.zero();

    
  }

  @Override
  public void run() {
    
    init();

    
    System.out.println("running; " + domI.size() + " invokes to visit");

    jq_Method m = null;
    vidxToLen = new HashMap<Integer,Integer>();

    for (Inst inst : domI) {
//      if(!inst.getMethod().equals(m)) {
//        vidxToLen = new HashMap<Integer,Integer>();
//        m = inst.getMethod();
//      }
       visitInvokeInst((Quad) inst);
    }
    vidxToLen = null; //gc won't find it automatically unless we do this; it's a 
    //field ref, not a local
    
    relPrim.save();
    relRef.save();
  }
  
  HashMap<Integer, Integer> vidxToLen;

  public void visitInvokeInst(Quad q) {
    jq_Method meth = Invoke.getMethod(q).getMethod();
    String classname = meth.getDeclaringClass().getName();
    String methname = meth.getName().toString();

    if(classname.equals("java.lang.StringBuilder") && methname.equals("<init>")) {
      
    }
    if((classname.equals("java.lang.StringBuilder") || classname.equals("java.lang.StringBuffer")) &&
        methname.equals("append")) {
      int iIdx = domI.indexOf(q);
      int v1, comp;
      RegisterOperand i0 = Invoke.getParam(q, 0);
      v1 = domV.indexOf(i0.getRegister());


      Integer z0 = vidxToLen.get(v1);
      int z = z0 == null ? 1 : z0;
      int retVarIdx = domV.indexOf(Invoke.getDest(q).getRegister());
      if(retVarIdx > 0)
        vidxToLen.put(retVarIdx, z+1);
      if(v1 != -1 && (domK.size() > z)) {
            
        RegisterOperand i1 = Invoke.getParam(q, 1);
        if(i1.getType().isReferenceType()) {
          comp = domV.indexOf(i1.getRegister());
          relRef.add(iIdx,v1,comp, z);
        } else {
          comp = domU.indexOf(i1.getRegister());
          relPrim.add(iIdx,v1,comp, z);
        }
      }
        
    }
  }
}
