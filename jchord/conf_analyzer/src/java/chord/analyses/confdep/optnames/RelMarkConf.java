/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.confdep.optnames;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.analyses.confdep.ConfDefines;
import chord.analyses.primtrack.DomUV;
import chord.analyses.string.DomStrConst;

import chord.analyses.alloc.DomH;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;


@Chord(
//    name = "static-markconf-java",
    name = "ImarkConf",
    consumes = { "H","UV", "StrConst"},
//    produces = { "ImarkConf" },
      sign = "H0,UV0,StrConst0:H0_UV0_StrConst0"
//      namesOfSigns = { "ImarkConf" },
//      namesOfTypes = { "H" ,"UV","StrConst"},
//      types = { DomH.class, DomUV.class, DomStrConst.class }
  )
public class RelMarkConf extends ProgramRel implements IMethodVisitor {
  private Set<BasicBlock> visited = new HashSet<BasicBlock>();

  DomH domH;
  DomUV domUV;
  DomStrConst domConst;
  public void init() {
    domH = (DomH) doms[0];
    domUV = (DomUV) doms[1];
    domConst = (DomStrConst) doms[2];
  }
 
  
  public void visit(jq_Class c) { }
  public void visit(jq_Method m) {
    if (m.isAbstract())
      return;
    if(m.getDeclaringClass().getName().equals("org.apache.hadoop.conf.Configuration"))
      return;
    
    if(m.isStatic() && m.getDesc().toString().equals("([Ljava/lang/String;)V")) {
      System.out.println("spotted main!");
      ControlFlowGraph cfg = m.getCFG();
      RegisterFactory rf = cfg.getRegisterFactory();
      RegisterFactory.Register argReg = rf.get(0);
      int vIdx = domUV.indexOf(argReg);
      System.out.println("main arg reg has vIdx " + vIdx+ " and name " + argReg.toString());
//      m.
      if(vIdx >= 0)
        add(0, vIdx, 0);
    }
    
    ControlFlowGraph cfg = m.getCFG();
    BasicBlock entry = cfg.entry();
    processBB(entry);
    visited.clear();
  }

  private void processBB(BasicBlock bb) {
    HashMap<Integer, String> constVals = new HashMap<Integer, String>();
    
    int n = bb.size();
    for (int j = 0; j < n; j++) {
      Quad q = bb.getQuad(j);
      Operator op = q.getOperator();
      if(op instanceof Invoke) {
        RegisterOperand vo = Invoke.getDest(q);
        if (vo != null) {
          Register v = vo.getRegister();
//          if (v.getType().isReferenceType()) {
            int vIdx = domUV.indexOf(v);
            int hIdx = domH.indexOf(q);
            jq_Method meth = Invoke.getMethod(q).getMethod();
            
            String classname = meth.getDeclaringClass().getName();
            String methname = meth.getName().toString();

            if(hIdx == -1) {
//              System.out.println("WARN: markconf doesn't know what counts as an alloc; call to " + fqName);
              continue;
            }
            int optionPos = ConfDefines.confOptionPos(q);
            if(optionPos != -1) {

              if(Invoke.getParamList(q).length() <= optionPos) {
                System.out.println("WARN: expected more params to " + classname + "." + methname);
              } else {
                RegisterOperand parm = Invoke.getParam(q, optionPos);
                String cval = constVals.get( parm.getRegister().getNumber());
                if(cval == null)
                  add(hIdx, vIdx, 0);
                else
                  add(hIdx, vIdx, domConst.getOrAdd(cval));
              }
  //          }
          }
        }
        /*
        ParamListOperand argList = Invoke.getParamList(q);
        for(int i=0; i < argList.length(); ++i) {
          RegisterOperand argi = argList.get(i);
          String cname = constVals.get(argi.getRegister().getNumber());
          if(cname != null) {
            System.out.println("PARAM: " + argList.get(i) + " of " + Invoke.getMethod(q)+ " = " + cname);            
          } else {
            String type = argList.get(i).getClass().getCanonicalName();
            System.out.println("PARAM: " + argList.get(i) + " of " + Invoke.getMethod(q)+ " type = " + type);
          }
        } */
        
      } else if(op instanceof Move) {
        Operand srcOperand = Move.getSrc(q);
//        System.out.println("moving " + srcOperand + " to " + Move.getDest(q));
        if(srcOperand instanceof Operand.AConstOperand) {
//          System.out.println("storing constant into " + Move.getDest(q));
          Object wrapped = ((Operand.AConstOperand)srcOperand).getWrapped();
          if(wrapped != null) {
            constVals.put(Move.getDest(q).getRegister().getNumber(), wrapped.toString());
          }
        }
        
      }
    }
    for (Object o : bb.getSuccessors()) {
      BasicBlock bb2 = (BasicBlock) o;
      if (!visited.contains(bb2)) {
        visited.add(bb2);
        processBB(bb2);
      }
    }

  }
  
}
