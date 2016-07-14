package shord.analyses;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootField;
import soot.Local;
import soot.Immediate;
import soot.Value;
import soot.ValueBox;
import soot.Unit;
import soot.Body;
import soot.Type;
import soot.RefType;
import soot.ArrayType;
import soot.RefLikeType;
import soot.PrimType;
import soot.VoidType;
import soot.NullType;
import soot.AnySubType;
import soot.UnknownType;
import soot.FastHierarchy;
import soot.PatchingChain;
import soot.jimple.Constant;
import soot.jimple.StringConstant;
import soot.jimple.ClassConstant;
import soot.jimple.Stmt;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.AnyNewExpr;
import soot.jimple.ThrowStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.CastExpr;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.ParameterRef;
import soot.jimple.ThisRef;
import soot.jimple.ArrayRef;
import soot.jimple.BinopExpr;
import soot.jimple.NegExpr;
import soot.jimple.NewExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.spark.pag.ArrayElement;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.tagkit.Tag;
import soot.util.NumberedSet;
import soot.jimple.toolkits.callgraph.ReachableMethods;

import shord.project.analyses.JavaAnalysis;
import shord.project.analyses.ProgramRel;
import shord.project.analyses.ProgramDom;
import shord.project.ClassicProject;
import shord.program.Program;

import stamp.analyses.SootUtils;
import stamp.harnessgen.*;

import chord.project.Chord;
import chord.bddbddb.Rel.RelView;

import java.util.*;

/*
 * @author Saswat Anand
 * @author Yu Feng
*/
@Chord(name="base-java", 
	   consumes={"SC"},
	   produces={"M", "Z", "I", "H", "V", "T", "F", "U",
				 "GlobalAlloc", "Alloc", "NewH",
				 "Assign", "Load", "Store", 
				 "LoadStat", "StoreStat", 
				 "MmethArg", "MmethRet", 
				 "IinvkRet", "IinvkArg", 
				 "VT", "chaIM",
				 "HT", "HTFilter", "NewH",
				 "MI", "MH",
				 "MV", "MU",
				 "AssignPrim", 
				 "LoadPrim", "StorePrim",
				 "LoadStatPrim", "StoreStatPrim",
				 "MmethPrimArg", "MmethPrimRet", 
				 "IinvkPrimRet", "IinvkPrimArg",
	             "Stub",
				 "SpecIM", "StatIM", "VirtIM",
				 "SubSig", "Dispatch",
                 "ClassT", "Subtype", "StaticTM", "StaticTF", "ClinitTM",
				 "SCH"
                 },
       namesOfTypes = { "M", "Z", "I", "H", "V", "T", "F", "U", "S", "SC"},
       types = { DomM.class, DomZ.class, DomI.class, DomH.class, DomV.class, DomT.class, DomF.class, DomU.class, DomS.class, DomSC.class},
	   namesOfSigns = { "GlobalAlloc", "Alloc", "NewH",
						"Assign", "Load", "Store", 
						"LoadStat", "StoreStat", 
						"MmethArg", "MmethRet", 
						"IinvkRet", "IinvkArg", 
						"VT", "chaIM",
						"HT", "HTFilter", 
						"MI", "MH",
						"MV", "MU",
						"AssignPrim", 
						"LoadPrim", "StorePrim",
						"LoadStatPrim", "StoreStatPrim",
						"MmethPrimArg", "MmethPrimRet", 
						"IinvkPrimRet", "IinvkPrimArg",
                        "Stub",
						"SpecIM", "StatIM", "VirtIM",
						"SubSig", "Dispatch",
						"ClassT", "Subtype", "StaticTM", "StaticTF", "ClinitTM",
						"SCH",				 
						"ArgArgTransfer", "ArgRetTransfer", 
						"ArgArgFlow",
						"InLabelArg", "InLabelRet",
						"OutLabelArg", "OutLabelRet"
                        },
	   signs = { "V0,H0:V0_H0", "V0,H0:V0_H0", "H0:H0",
				 "V0,V1:V0xV1", "V0,V1,F0:F0_V0xV1", "V0,F0,V1:F0_V0xV1",
				 "V0,F0:F0_V0", "F0,V0:F0_V0",
				 "M0,Z0,V0:M0_V0_Z0", "M0,Z0,V0:M0_V0_Z0",
				 "I0,Z0,V0:I0_V0_Z0", "I0,Z0,V0:I0_V0_Z0",
				 "V0,T0:T0_V0", "I0,M0:I0_M0",
				 "H0,T0:H0_T0", "H0,T0:H0_T0",
				 "M0,I0:M0_I0", "M0,H0:M0_H0",
				 "M0,V0:M0_V0", "M0,U0:M0_U0",
				 "U0,U1:U0xU1",
				 "U0,V0,F0:U0_V0_F0", "V0,F0,U0:U0_V0_F0",
				 "U0,F0:U0_F0", "F0,U0:U0_F0",
				 "M0,Z0,U0:M0_U0_Z0", "M0,Z0,U0:M0_U0_Z0",
				 "I0,Z0,U0:I0_U0_Z0", "I0,Z0,U0:I0_U0_Z0",
                 "M0:M0",
				 "I0,M0:I0_M0", "I0,M0:I0_M0", "I0,S0:I0_S0",
				 "M0,S0:M0_S0", "T0,S0,M0:T0_M0_S0",
	             "T0:T0", "T0,T1:T0_T1", "T0,M0:T0_M0", "T0,F0:F0_T0", "T0,M0:T0_M0",
				 "SC0,H0:SC0_H0",
				 "M0,Z0,Z1:M0_Z0_Z1", "M0,Z0:M0_Z0", 
				 "M0,Z0,Z1:M0_Z0_Z1",
				 "L0,M0,Z0:L0_M0_Z0", "L0,M0:L0_M0",
				 "L0,M0,Z0:L0_M0_Z0", "L0,M0:L0_M0"
                 }
	   )
public class PAGBuilder extends JavaAnalysis
{
	private ProgramRel relGlobalAlloc;//(l:V,h:H)
	private ProgramRel relAlloc;//(l:V,h:H)
	private ProgramRel relNewH;//(h:H)
	private ProgramRel relAssign;//(l:V,r:V)
	private ProgramRel relLoad;//(l:V,b:V,f:F)
	private ProgramRel relStore;//(b:V,f:F,r:V)
	private ProgramRel relLoadStat;//(l:V,f:F)
	private ProgramRel relStoreStat;//(f:F,r:V)

    private ProgramRel relMmethArg;//(m:M,z:Z,v:V)
    private ProgramRel relMmethRet;//(m:M,z:Z,v:V)
    private ProgramRel relIinvkRet;//(i:I,n:Z,v:V)
    private ProgramRel relIinvkArg;//(i:I,n:Z,v:V)
	
	private ProgramRel relAssignPrim;//(l:U,r:U)
	private ProgramRel relLoadPrim;//(l:U,b:V,f:F)
	private ProgramRel relStorePrim;//(b:V,f:F,r:U)
	private ProgramRel relLoadStatPrim;//(l:U,f:F)
	private ProgramRel relStoreStatPrim;//(f:F,r:U)

    private ProgramRel relMmethPrimArg;//(m:M,z:Z,u:U)
    private ProgramRel relMmethPrimRet;//(m:M,z:Z,u:U)
    private ProgramRel relIinvkPrimRet;//(i:I,n:Z,u:U)
    private ProgramRel relIinvkPrimArg;//(i:I,n:Z,u:U)

	private ProgramRel relSpecIM;//(i:I,m:M)
	private ProgramRel relStatIM;//(i:I,m:M)
	private ProgramRel relVirtIM;//(i:I,m:M)

	private ProgramRel relVT;
	private ProgramRel relHT;
	private ProgramRel relHTFilter;
	private ProgramRel relMI;
	private ProgramRel relMH;
	private ProgramRel relMV;
	private ProgramRel relMU;

	private ProgramRel relSCH;

	private DomV domV;
	private DomU domU;
	private DomH domH;
	private DomZ domZ;
	private DomI domI;
	private DomM domM;
	private DomF domF;
	private DomSC domSC;
	private DomS domS;

	private int maxArgs = -1;
	private FastHierarchy fh;
	//private Map<Type,StubAllocNode> typeToStubAllocNode = new HashMap();

	private GlobalStringConstantNode gscn = new GlobalStringConstantNode();

    static String gInstallAPK = "INSTALL_APK";

	private boolean ignoreStub;

	void openRels()
	{
		relGlobalAlloc = (ProgramRel) ClassicProject.g().getTrgt("GlobalAlloc");
		relGlobalAlloc.zero();
		relAlloc = (ProgramRel) ClassicProject.g().getTrgt("Alloc");
		relAlloc.zero();
		relNewH = (ProgramRel) ClassicProject.g().getTrgt("NewH");
		relNewH.zero();
		relAssign = (ProgramRel) ClassicProject.g().getTrgt("Assign");
		relAssign.zero();
		relLoad = (ProgramRel) ClassicProject.g().getTrgt("Load");
		relLoad.zero();
		relStore = (ProgramRel) ClassicProject.g().getTrgt("Store");
		relStore.zero();
		relLoadStat = (ProgramRel) ClassicProject.g().getTrgt("LoadStat");
		relLoadStat.zero();
		relStoreStat = (ProgramRel) ClassicProject.g().getTrgt("StoreStat");
		relStoreStat.zero();

		relMmethArg = (ProgramRel) ClassicProject.g().getTrgt("MmethArg");
		relMmethArg.zero();
		relMmethRet = (ProgramRel) ClassicProject.g().getTrgt("MmethRet");
		relMmethRet.zero();
		relIinvkRet = (ProgramRel) ClassicProject.g().getTrgt("IinvkRet");
		relIinvkRet.zero();
		relIinvkArg = (ProgramRel) ClassicProject.g().getTrgt("IinvkArg");
		relIinvkArg.zero();

		relSpecIM = (ProgramRel) ClassicProject.g().getTrgt("SpecIM");
		relSpecIM.zero();
		relStatIM = (ProgramRel) ClassicProject.g().getTrgt("StatIM");
		relStatIM.zero();
		relVirtIM = (ProgramRel) ClassicProject.g().getTrgt("VirtIM");
		relVirtIM.zero();

		relVT = (ProgramRel) ClassicProject.g().getTrgt("VT");
        relVT.zero();
		relHT = (ProgramRel) ClassicProject.g().getTrgt("HT");
        relHT.zero();
		relHTFilter = (ProgramRel) ClassicProject.g().getTrgt("HTFilter");
		relHTFilter.zero();
		relMI = (ProgramRel) ClassicProject.g().getTrgt("MI");
        relMI.zero();
		relMH = (ProgramRel) ClassicProject.g().getTrgt("MH");
        relMH.zero();
		relMV = (ProgramRel) ClassicProject.g().getTrgt("MV");
        relMV.zero();
		relMU = (ProgramRel) ClassicProject.g().getTrgt("MU");
        relMU.zero();

		relAssignPrim = (ProgramRel) ClassicProject.g().getTrgt("AssignPrim");
		relAssignPrim.zero();
		relLoadPrim = (ProgramRel) ClassicProject.g().getTrgt("LoadPrim");
		relLoadPrim.zero();
		relStorePrim = (ProgramRel) ClassicProject.g().getTrgt("StorePrim");
		relStorePrim.zero();
		relLoadStatPrim = (ProgramRel) ClassicProject.g().getTrgt("LoadStatPrim");
		relLoadStatPrim.zero();
		relStoreStatPrim = (ProgramRel) ClassicProject.g().getTrgt("StoreStatPrim");
		relStoreStatPrim.zero();

		relMmethPrimArg = (ProgramRel) ClassicProject.g().getTrgt("MmethPrimArg");
		relMmethPrimArg.zero();
		relMmethPrimRet = (ProgramRel) ClassicProject.g().getTrgt("MmethPrimRet");
		relMmethPrimRet.zero();
		relIinvkPrimRet = (ProgramRel) ClassicProject.g().getTrgt("IinvkPrimRet");
		relIinvkPrimRet.zero();
		relIinvkPrimArg = (ProgramRel) ClassicProject.g().getTrgt("IinvkPrimArg");
		relIinvkPrimArg.zero();

		relSCH = (ProgramRel) ClassicProject.g().getTrgt("SCH");
		relSCH.zero();
	}
	
	void saveRels()
	{
		relGlobalAlloc.save();
		relAlloc.save();
		relNewH.save();
		relAssign.save();
		relLoad.save();
		relStore.save();
		relLoadStat.save();
		relStoreStat.save();

		relMmethArg.save();
		relMmethRet.save();
		relIinvkRet.save();
		relIinvkArg.save();

		relSpecIM.save();
		relStatIM.save();
		relVirtIM.save();

		relVT.save();
		relHT.save();
		relHTFilter.save();
		relMI.save();
		relMH.save();
		relMV.save();
		relMU.save();

		relAssignPrim.save();
		relLoadPrim.save();
		relStorePrim.save();
		relLoadStatPrim.save();
		relStoreStatPrim.save();

		relMmethPrimArg.save();
		relMmethPrimRet.save();
		relIinvkPrimRet.save();
		relIinvkPrimArg.save();
		
		relSCH.save();
	}

	void GlobalAlloc(VarNode l, GlobalAllocNode h)
	{
		assert l != null;
		relGlobalAlloc.add(l, h);
	}

	void Alloc(VarNode l, AllocNode h)
	{
		assert l != null;
		relAlloc.add(l, h);
	}

	void Assign(VarNode l, VarNode r)
	{
		if(l == null || r == null)
			return;
		relAssign.add(l, r);
	}

	void Load(LocalVarNode l, LocalVarNode b, SparkField f)
	{
		if(b == null || l == null)
			return;
		if(f instanceof SootField && (((SootField) f).getDeclaringClass().isPhantom()))
		   return;
		relLoad.add(l, b, f);
	}

	void Store(LocalVarNode b, SparkField f, VarNode r)
	{
		if(b == null || r == null)
			return;
		if(f instanceof SootField && (((SootField) f).getDeclaringClass().isPhantom()))
		   return;
		relStore.add(b, f, r);
	}

	void LoadStat(LocalVarNode l, SootField f)
	{
		if(l == null)
			return;
		if(f instanceof SootField && (((SootField) f).getDeclaringClass().isPhantom()))
		   return;
		relLoadStat.add(l, f);
	}

	void StoreStat(SootField f, VarNode r)
	{
		if(r == null)
			return;
		if(f instanceof SootField && (((SootField) f).getDeclaringClass().isPhantom()))
		   return;
		relStoreStat.add(f, r);
	}

	void MmethArg(SootMethod m, int index, VarNode v)
	{
		if(v == null)
			return;
		relMmethArg.add(m, new Integer(index), v);
	}

	void MmethRet(SootMethod m, RetVarNode v)
	{
		if(v == null)
			return;
		relMmethRet.add(m, new Integer(0), v);
	}
	
	void IinvkArg(Unit invkUnit, int index, VarNode v)
	{
		if(v == null)
			return;
		relIinvkArg.add(invkUnit, new Integer(index), v);
	}

	void IinvkRet(Unit invkUnit, LocalVarNode v)
	{
		if(v == null)
			return;
		relIinvkRet.add(invkUnit, new Integer(0), v);
	}

	void AssignPrim(VarNode l, VarNode r)
	{
		if(l == null || r == null)
			return;
		relAssignPrim.add(l, r);
	}

	void LoadPrim(LocalVarNode l, LocalVarNode b, SparkField f)
	{
		if(b == null || l == null)
			return;
		if(f instanceof SootField && (((SootField) f).getDeclaringClass().isPhantom()))
		   return;
		relLoadPrim.add(l, b, f);
	}

	void StorePrim(LocalVarNode b, SparkField f, LocalVarNode r)
	{
		if(b == null || r == null)
			return;
		if(f instanceof SootField && (((SootField) f).getDeclaringClass().isPhantom()))
		   return;
		relStorePrim.add(b, f, r);
	}

	void LoadStatPrim(LocalVarNode l, SootField f)
	{
		if(l == null)
			return;
		if(f instanceof SootField && (((SootField) f).getDeclaringClass().isPhantom()))
		   return;
		relLoadStatPrim.add(l, f);
	}

	void StoreStatPrim(SootField f, LocalVarNode r)
	{
		if(r == null)
			return;
		if(f instanceof SootField && (((SootField) f).getDeclaringClass().isPhantom()))
		   return;
		relStoreStatPrim.add(f, r);
	}

	void MmethPrimArg(SootMethod m, int index, VarNode v)
	{
		if(v == null)
			return;
		relMmethPrimArg.add(m, new Integer(index), v);
	}

	void MmethPrimRet(SootMethod m, RetVarNode v)
	{
		if(v == null)
			return;
		relMmethPrimRet.add(m, new Integer(0), v);
	}
	
	void IinvkPrimArg(Unit invkUnit, int index, LocalVarNode v)
	{
		if(v == null)
			return;
		relIinvkPrimArg.add(invkUnit, new Integer(index), v);
	}

	void IinvkPrimRet(Unit invkUnit, LocalVarNode v)
	{
		if(v == null)
			return;
		relIinvkPrimRet.add(invkUnit, new Integer(0), v);
	}

    public void growZIfNeeded(int newSize) 
	{
        int oldSize = maxArgs;
		if(newSize <= oldSize)
			return;
        for(int i = oldSize+1; i <= newSize; i++)
            domZ.add(new Integer(i));
        maxArgs = newSize;
    }

	class MethodPAGBuilder
	{
		private ThisVarNode thisVar;
		private RetVarNode retVar;
		private ParamVarNode[] paramVars;
		private Map<Local,LocalVarNode> localToVarNode;
		private Set<Local> nonPrimLocals;
		private Set<Local> primLocals;
		private Map<Stmt,CastVarNode> stmtToCastNode;
		private Tag containerTag;

		private final SootMethod method;
		private final List<StubAllocNode> stubAllocNodes = new ArrayList();
		private final Map<Unit,SiteAllocNode> stmtToAllocNode = new HashMap();
		private final Map<String,StringConstNode> stringConstantToAllocNode = new HashMap();
		private final Map<String,VarNode> stringConstantToVarNode = new HashMap();

		private StringConstantVarNode globalSCVarNode;

		//private Map<String, Set<AllocNode>> action2Node = new HashMap();
		//private Map<String, AllocNode> dataType2Node = new HashMap();

		private final boolean isStub;

		MethodPAGBuilder(SootMethod method, boolean isStub)
		{
			this.method = method;
			this.isStub = isStub;
		}
		
		void pass1()
		{
			growZIfNeeded(method.getParameterCount());
			
			if(!method.isStatic()) {
				thisVar = new ThisVarNode(method);
				domV.add(thisVar);
 			}

			int count = method.getParameterCount();
			if(count > 0){
				paramVars = new ParamVarNode[count];
				int j = 0;
				for(Type pType : method.getParameterTypes()){
					ParamVarNode node = new ParamVarNode(method, j);
					if(pType instanceof RefLikeType){
						domV.add(node);
					} else if(pType instanceof PrimType){
						domU.add(node);
					} else
						assert false;
					paramVars[j++] = node;
				}
			}
			
			Type retType = method.getReturnType();
			if(!(retType instanceof VoidType)){
				retVar = new RetVarNode(method);
				if(retType instanceof RefLikeType) {
					domV.add(retVar);
				} else if(retType instanceof PrimType) {
					domU.add(retVar);
				} else
					assert false;
			} 

			if(!method.isConcrete())
				return;

			if(isStub){
				if(!ignoreStub) {
					if(retType instanceof RefType){
						for(SootClass st: SootUtils.subTypesOf(((RefType) retType).getSootClass())){
							//for each concrete subtype of method's return type, 
							//we add a stub alloc node
							if(!st.isConcrete())
								continue;
							assert !st.isInterface();
							// StubAllocNode n = stubAllocNodeFor(st.getType());
							StubAllocNode n = new StubAllocNode(st.getType(), method);
							domH.add(n);
							stubAllocNodes.add(n);
							//System.out.println("OO "+method+" "+n);
						}
					
					} else if(retType instanceof ArrayType){
						//TODO: introduce stub alloc node for arrays 
					}
				}
				//don't process bodies of stub methods
				return;
			} 

		localToVarNode = new HashMap();
			Body body = method.retrieveActiveBody();
			LocalsClassifier lc = new LocalsClassifier(body);
			primLocals = lc.primLocals();
			nonPrimLocals = lc.nonPrimLocals();
			for(Local l : body.getLocals()){
				boolean isPrim = primLocals.contains(l);
				boolean isNonPrim = nonPrimLocals.contains(l);
				if(isPrim || isNonPrim){
					LocalVarNode node = new LocalVarNode(l, method);
					localToVarNode.put(l, node);
					if(isNonPrim)
						domV.add(node);
					if(isPrim)
						domU.add(node);
				}
			}

			stmtToCastNode = new HashMap();
			for(Unit unit : body.getUnits()){
				Stmt s = (Stmt) unit;
				if(s.containsInvokeExpr()){
					int numArgs = s.getInvokeExpr().getArgCount();
					growZIfNeeded(numArgs);
					domI.add(s);
				} else if(s instanceof AssignStmt) {
					Value rightOp = ((AssignStmt) s).getRightOp();

					if(rightOp instanceof AnyNewExpr){
						SiteAllocNode n = new SiteAllocNode(s);
						domH.add(n);
						stmtToAllocNode.put(s, n);
					}
					else if(rightOp instanceof CastExpr){
						CastExpr castExpr = (CastExpr) rightOp;
						Type castType = castExpr.getCastType();
						if(castType instanceof RefLikeType){
							CastVarNode node =
								new CastVarNode(method, castExpr);
							domV.add(node);
							stmtToCastNode.put(s, node);
						}
					}
				}
			}	
			
			//handle string constants
			for(ValueBox vb : body.getUseBoxes()){
				Value val = vb.getValue();
				if(!(val instanceof StringConstant))
					continue;
				String str = ((StringConstant) val).value;

				StringConstantVarNode vn;
				StringConstNode an;
				if(domSC.contains(str)){
					//special string constants. track precisely
					vn = new StringConstantVarNode(method, str);
					domV.add(vn);
					stringConstantToVarNode.put(str, vn);
					
					an = new StringConstNode(str);
					domH.add(an);
				} else {
					//general string constants
					if(globalSCVarNode == null){
						globalSCVarNode = new StringConstantVarNode(method, null);
						domV.add(globalSCVarNode);
					}
					vn = globalSCVarNode;
					an = gscn;
				}
				stringConstantToVarNode.put(str, vn);
				stringConstantToAllocNode.put(str, an);
			}
		}

		void pass2()
		{
			int i = 0;
			if(thisVar != null){
				MmethArg(method, i++, thisVar);
				relVT.add(thisVar, method.getDeclaringClass().getType());
				relMV.add(method, thisVar);
			}
			
			if(paramVars != null){
				for(int j = 0; j < paramVars.length; j++){
					Type paramType = method.getParameterType(j);
					ParamVarNode node = paramVars[j];
					if(paramType instanceof RefLikeType){
						MmethArg(method, i, node);
						relVT.add(node, paramType);
						relMV.add(method, node);
					} else if(paramType instanceof PrimType){
						MmethPrimArg(method, i, node);
						relMU.add(method, node);
					} else
						assert false;
					i++;
				}
			}
			
			if(retVar != null){
				Type retType = method.getReturnType();
				if(retType instanceof RefLikeType){
					MmethRet(method, retVar);
					relVT.add(retVar, retType);
					relMV.add(method, retVar);
				} else if(retType instanceof PrimType){
					MmethPrimRet(method, retVar);
					relMU.add(method, retVar);
				} else
					assert false;
			}

			if(!method.isConcrete())
				return;

			if(isStub){
				if(!ignoreStub){
					for(StubAllocNode an : stubAllocNodes){
						populateHT_HTFilter(an);
						Alloc(retVar, an);
						relMH.add(method, an);
					}
				}
				return;
			} 

			for(SiteAllocNode an : stmtToAllocNode.values()){
				populateHT_HTFilter(an);
				relMH.add(method, an);
				relNewH.add(an);
			}
			
			for(Map.Entry<String,StringConstNode> entry : stringConstantToAllocNode.entrySet()){
				String sc = entry.getKey();
				StringConstNode an = entry.getValue();
				if(an != gscn)
					populateHT_HTFilter(an);
				
				VarNode vn = stringConstantToVarNode.get(sc);
				GlobalAlloc(vn, an);
				
				if(domSC.contains(sc))
					relSCH.add(sc, an);
				
				relVT.add(vn, RefType.v("java.lang.String"));
				relMV.add(method, vn);
			}

			for(Map.Entry<Local,LocalVarNode> e : localToVarNode.entrySet()){
				LocalVarNode varNode = e.getValue();
				if(nonPrimLocals.contains(e.getKey())){
					relVT.add(varNode, e.getKey().getType()/*UnknownType.v()*/);
					relMV.add(method, varNode);
				}
				if(primLocals.contains(e.getKey())){
					relMU.add(method, varNode);
				}
			}
			for(Map.Entry<Stmt,CastVarNode> e : stmtToCastNode.entrySet()){
				Type castType = ((CastExpr) ((AssignStmt) e.getKey()).getRightOp()).getCastType();
				relVT.add(e.getValue(), castType);
				relMV.add(method, e.getValue());
			}
			
			containerTag = new ContainerTag(method);
			Body body = method.retrieveActiveBody();
			for(Unit unit : body.getUnits()){
				handleStmt((Stmt) unit);
			}
		}


		VarNode nodeFor(Immediate i)
		{
			if(i instanceof Constant){
				if(i instanceof StringConstant){
					String sc = ((StringConstant) i).value;
					return stringConstantToVarNode.get(sc);
				} else if(i instanceof ClassConstant){
					//TODO
				}
				return null;
			}
			return localToVarNode.get((Local) i);
		}

		LocalVarNode localNodeFor(Immediate i)
		{
			return (LocalVarNode) nodeFor(i);
		}
		
		void handleStmt(Stmt s)
		{
			if(s.containsInvokeExpr()){
				InvokeExpr ie = s.getInvokeExpr();
				SootMethod callee = ie.getMethod();
				int numArgs = ie.getArgCount();
				relMI.add(method, s);
				s.addTag(containerTag);

                //only consider methods that are availalbe (i.e., not phantom)
                if(domM.contains(callee)){
                    //handle different types of invk stmts
                    if(ie instanceof SpecialInvokeExpr){
                        relSpecIM.add(s, callee);
                    }                
                    else if(isQuasiStaticInvk(s)){
                        relStatIM.add(s, callee);
                    }
				}
				if(ie instanceof VirtualInvokeExpr || ie instanceof InterfaceInvokeExpr){
					String subsig = callee.getSubSignature();
					if(domS.contains(subsig))
						relVirtIM.add(s, subsig);
				}

				//handle receiver
				int j = 0;
				if(ie instanceof InstanceInvokeExpr){
					InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
					IinvkArg(s, j, localNodeFor((Immediate) iie.getBase()));
					j++;
				}
				
				//handle args
				for(int i = 0; i < numArgs; i++,j++){
					Immediate arg = (Immediate) ie.getArg(i);
					Type argType = callee.getParameterType(i);
					if(argType instanceof RefLikeType)
						IinvkArg(s, j, nodeFor(arg));
					else if(argType instanceof PrimType)
						IinvkPrimArg(s, j, localNodeFor(arg));
				}
				
				//return value
				if(s instanceof AssignStmt){
					Local lhs = (Local) ((AssignStmt) s).getLeftOp();
					Type retType = callee.getReturnType();
					if(retType instanceof RefLikeType)
						IinvkRet(s, localNodeFor(lhs));
					else if(retType instanceof PrimType)
						IinvkPrimRet(s, localNodeFor(lhs));
				}
			} else if(s.containsFieldRef()){
				AssignStmt as = (AssignStmt) s;
				Value leftOp = as.getLeftOp();
				FieldRef fr = s.getFieldRef();
				SootField field = fr.getField();
				Type fieldType = field.getType();
				if(leftOp instanceof Local){
					//load
					if(field.isStatic()){
						if(fieldType instanceof RefLikeType)
							LoadStat(localNodeFor((Local) leftOp), field);
						else if(fieldType instanceof PrimType)
							LoadStatPrim(localNodeFor((Local) leftOp), field);
					} else{
						Immediate base = (Immediate) ((InstanceFieldRef) fr).getBase();
						if(fieldType instanceof RefLikeType)
							Load(localNodeFor((Local) leftOp), localNodeFor(base), field);
						else if(fieldType instanceof PrimType)
							LoadPrim(localNodeFor((Local) leftOp), localNodeFor(base), field);
					}
				} else{
					//store
					assert leftOp == fr;
					Immediate rightOp = (Immediate) as.getRightOp();
					if(field.isStatic()){
						if(fieldType instanceof RefLikeType)
							StoreStat(field, nodeFor(rightOp));
						else if(fieldType instanceof PrimType)
							StoreStatPrim(field, localNodeFor(rightOp));
					} else{
						Immediate base = (Immediate) ((InstanceFieldRef) fr).getBase();
						if(fieldType instanceof RefLikeType)
							Store(localNodeFor(base), field, nodeFor(rightOp));
						else if(fieldType instanceof PrimType)
							StorePrim(localNodeFor(base), field, localNodeFor(rightOp));		
					}
				}
			} else if(s.containsArrayRef()) {
				AssignStmt as = (AssignStmt) s;
				Value leftOp = as.getLeftOp();
				ArrayRef ar = s.getArrayRef();
				Immediate base = (Immediate) ar.getBase();
				SparkField field = ArrayElement.v();
				if(leftOp instanceof Local){
					//array read
					Local l = (Local) leftOp;
					if(nonPrimLocals.contains(l))
						Load(localNodeFor(l), localNodeFor(base), field);
					if(primLocals.contains(l)) {
						LoadPrim(localNodeFor(l), localNodeFor(base), field);
						//implicit flow
						AssignPrim(nodeFor(l), nodeFor((Immediate) ar.getIndex()));
					}
				}else{
					//array write
					assert leftOp == ar;
					Value rightOp = as.getRightOp();
					if(rightOp instanceof Local){
						Local r = (Local) rightOp;
						if(nonPrimLocals.contains(r))
							Store(localNodeFor(base), field, localNodeFor(r));
						if(primLocals.contains(r))
							StorePrim(localNodeFor(base), field, localNodeFor(r));
					}
				}
			} else if(s instanceof AssignStmt) {
				AssignStmt as = (AssignStmt) s;
				Value leftOp = as.getLeftOp();
				Value rightOp = as.getRightOp();

				if(rightOp instanceof AnyNewExpr){
					AllocNode an = stmtToAllocNode.get(s);
					Alloc(nodeFor((Local) leftOp), an);
					s.addTag(containerTag);
				} else if(rightOp instanceof CastExpr){
					Type castType = ((CastExpr) rightOp).getCastType();
					Immediate op = (Immediate) ((CastExpr) rightOp).getOp();
					if(castType instanceof RefLikeType){
						CastVarNode castNode = stmtToCastNode.get(s);
						Assign(castNode, nodeFor(op));
						Assign(nodeFor((Local) leftOp), castNode);
					} else if(castType instanceof PrimType)
						AssignPrim(nodeFor((Local) leftOp), nodeFor(op));
				} else if(leftOp instanceof Local && rightOp instanceof Immediate){
					Local l = (Local) leftOp;
					Immediate r = (Immediate) rightOp;
					if(nonPrimLocals.contains(l))
						Assign(nodeFor(l), nodeFor(r));
					if(primLocals.contains(l))
						AssignPrim(nodeFor(l), nodeFor(r));
				} if(rightOp instanceof NegExpr){
					AssignPrim(nodeFor((Local) leftOp), nodeFor((Immediate) ((NegExpr) rightOp).getOp()));
				}else if(rightOp instanceof BinopExpr){
					VarNode leftNode = nodeFor((Local) leftOp);
					BinopExpr binExpr = (BinopExpr) rightOp;
					Immediate op1 = (Immediate) binExpr.getOp1();
					if(op1 instanceof Local){
						Local l = (Local) op1;
						if(primLocals.contains(l))
							AssignPrim(leftNode, nodeFor(l));	
					}
					Immediate op2 = (Immediate) binExpr.getOp2();
					if(op2 instanceof Local){
						Local l = (Local) op2;
						if(primLocals.contains(l))
							AssignPrim(leftNode, nodeFor(l));
					}
				}
			}else if(s instanceof ReturnStmt){
				Type retType = method.getReturnType();
				Immediate retOp = (Immediate) ((ReturnStmt) s).getOp();
				if(retType instanceof RefLikeType)
					Assign(retVar, nodeFor(retOp));
				else if(retType instanceof PrimType)
					AssignPrim(retVar, nodeFor(retOp));
			}else if(s instanceof IdentityStmt){
				IdentityStmt is = (IdentityStmt) s;
				Local leftOp = (Local) is.getLeftOp();
				Value rightOp = is.getRightOp();
				if(rightOp instanceof ThisRef){
					Assign(nodeFor(leftOp), thisVar);
				} else if(rightOp instanceof ParameterRef){
					int index = ((ParameterRef) rightOp).getIndex();
					Type type = method.getParameterType(index);
					if(type instanceof RefLikeType)
						Assign(nodeFor(leftOp), paramVars[index]);
					else if(type instanceof PrimType)
						AssignPrim(nodeFor(leftOp), paramVars[index]);
				}
			}
		}
	}

	void populateCallgraph()
	{
		Program prog = Program.g();
		CallGraph cg = prog.scene().getCallGraph();
		ProgramRel relChaIM = (ProgramRel) ClassicProject.g().getTrgt("chaIM");
        relChaIM.zero();
		Iterator<Edge> edgeIt = cg.listener();
		while(edgeIt.hasNext()){
			Edge edge = edgeIt.next();
			if(!edge.isExplicit())
				continue;
			Stmt stmt = edge.srcStmt();
			//int stmtIdx = domI.getOrAdd(stmt);
			SootMethod tgt = (SootMethod) edge.tgt();
			SootMethod src = (SootMethod) edge.src();
			if(tgt.isAbstract())
				assert false : "tgt = "+tgt +" "+tgt.isAbstract();
			if(tgt.isPhantom())
				continue;
			if(prog.isStub(src))
				continue;
			//System.out.println("stmt: "+stmt+" tgt: "+tgt+ "abstract: "+ tgt.isAbstract());
			if(prog.exclude(tgt) || (src != null && prog.exclude(src)))
				continue;
			relChaIM.add(stmt, tgt);
		}
		relChaIM.save();
	}

	void populateMethods()
	{
		domM = (DomM) ClassicProject.g().getTrgt("M");
		domS = (DomS) ClassicProject.g().getTrgt("S");
		Program program = Program.g();

		Iterator<SootMethod> mIt = program.getMethods();
        domM.add(program.getMainMethod()); //important to add main before any other method
		while(mIt.hasNext()){
			SootMethod m = mIt.next();
			growZIfNeeded(m.getParameterCount());
			if(program.exclude(m))
				continue;
			//System.out.println("adding subsig "+m.getSubSignature());
			domM.add(m);
			domS.add(m.getSubSignature());
		}
		domM.save();
		domS.save();

		ProgramRel relStub = (ProgramRel) ClassicProject.g().getTrgt("Stub");
        relStub.zero();
		for(Iterator it = program.getMethods(); it.hasNext();){
			SootMethod m = (SootMethod) it.next();
			if(program.isStub(m) && !program.exclude(m))
				relStub.add(m);
		}
		relStub.save();
	}

	void populateFields()
	{
		domF = (DomF) ClassicProject.g().getTrgt("F");
		Program program = Program.g();
		domF.add(ArrayElement.v()); //first add array elem so that it gets index 0
		for(SootClass klass : program.getClasses()){
			for(SootField field : klass.getFields()){
				domF.add(field);
			}
		}
		domF.save();
	}
	
	void populateTypes()
	{
		DomT domT = (DomT) ClassicProject.g().getTrgt("T");
		Program program = Program.g();
        Iterator<Type> typesIt = program.getTypes().iterator();
		while(typesIt.hasNext())
            domT.add(typesIt.next());
		domT.save();
	}
		
	void populateDomains(List<MethodPAGBuilder> mpagBuilders)
	{
		domZ = (DomZ) ClassicProject.g().getTrgt("Z");

		populateMethods();
		populateFields();
		populateTypes();

		domH = (DomH) ClassicProject.g().getTrgt("H");
		domV = (DomV) ClassicProject.g().getTrgt("V");
		domI = (DomI) ClassicProject.g().getTrgt("I");
		domU = (DomU) ClassicProject.g().getTrgt("U");

		domH.add(gscn);
		
		Program prog = Program.g();
		Iterator mIt = prog.scene().getReachableMethods().listener();
		while(mIt.hasNext()){
			SootMethod m = (SootMethod) mIt.next();
			if(prog.exclude(m))
				continue;
			MethodPAGBuilder mpagBuilder = new MethodPAGBuilder(m, prog.isStub(m));
			mpagBuilder.pass1();
			mpagBuilders.add(mpagBuilder);
		}

		domH.save();
		domZ.save();
		domV.save();
		domI.save();
		domU.save();
	}
	
	void populateSubSigs()
	{
		ProgramRel relSubSig = (ProgramRel) ClassicProject.g().getTrgt("SubSig");
		relSubSig.zero();
		Iterator mIt = Program.g().getMethods();
        DomM domM = (DomM) ClassicProject.g().getTrgt("M");
		while(mIt.hasNext()){
			SootMethod m = (SootMethod) mIt.next();
            if(domM.contains(m))
			    relSubSig.add(m, m.getSubSignature());
		}
		relSubSig.save();
	}

	void populateDispatch()
	{
		ProgramRel relDispatch = (ProgramRel) ClassicProject.g().getTrgt("Dispatch");
		relDispatch.zero();
		
        Map<SootClass, Set<SootMethod>> dispatchMap = new HashMap<SootClass, Set<SootMethod>>();
		Program program = Program.g();		

		List<SootClass> workList = new LinkedList();
		workList.add(Scene.v().getSootClass("java.lang.Object"));
		while(!workList.isEmpty()){
			SootClass klass = workList.remove(0);
			
			Set<SootMethod> clMethods = new HashSet();
			for(SootMethod m: klass.getMethods()){
				if(m.isAbstract() || m.isPrivate() || m.isStatic() || m.isConstructor()) 
					continue;
				clMethods.add(m);
			}

			if(klass.hasSuperclass()){
				//propagate from superclass. 
				for(SootMethod sm: dispatchMap.get(klass.getSuperclass())){
					//check whether exists the same subsignature in cl.
					boolean isOveride = false;
					String ss = sm.getSubSignature();
					for(SootMethod m : clMethods){
						if(m.getSubSignature().equals(ss)){
							isOveride = true;
							//System.out.println("override:" + cl + m + " || " + supercl + sm);
							break;
						}
					}
					if(!isOveride) clMethods.add(sm);
				}
			}

			dispatchMap.put(klass, clMethods);

			for(Object subClass : fh.getSubclassesOf(klass))
				workList.add((SootClass) subClass);
		}

        DomM domM = (DomM) ClassicProject.g().getTrgt("M");
		//create dispatch tuple based on the map.
		ReachableMethods reachableMethods = Program.g().scene().getReachableMethods();
		for(Map.Entry<SootClass,Set<SootMethod>> entry : dispatchMap.entrySet()){
			SootClass clazz = entry.getKey();
			Set<SootMethod> cMeths = entry.getValue();
			for(SootMethod m : cMeths){
				if(!reachableMethods.contains(m))
					continue;
				if(domM.contains(m))
					relDispatch.add(clazz.getType(), m.getSubSignature(), m);
			}
		}
		
		relDispatch.save();
	}

	void populateRelations(List<MethodPAGBuilder> mpagBuilders)
	{
		openRels();
		
		//for(StubAllocNode an : typeToStubAllocNode.values()){
		//	populateHT_HTFilter(an);
		//}
		populateHT_HTFilter(gscn);

		for(MethodPAGBuilder mpagBuilder : mpagBuilders)
			mpagBuilder.pass2();

		saveRels();

		populateSubSigs();
		populateDispatch();
		populateMisc();

		populateCallgraph();
	}

	void populateHT_HTFilter(AllocNode an)
	{
		Type type = an.getType();			
		relHT.add(an, type);
		
		Iterator<Type> typesIt = Program.g().getTypes().iterator();
		while(typesIt.hasNext()){
			Type varType = typesIt.next();
			if(canStore(type, varType))
				relHTFilter.add(an, varType);
		}
	}
	
    void populateMisc()
    {
        ProgramRel relClassT = (ProgramRel) ClassicProject.g().getTrgt("ClassT");
        relClassT.zero();
        ProgramRel relSubtype = (ProgramRel) ClassicProject.g().getTrgt("Subtype");
        relSubtype.zero();
        ProgramRel relStaticTM = (ProgramRel) ClassicProject.g().getTrgt("StaticTM");
        relStaticTM.zero();
        ProgramRel relStaticTF = (ProgramRel) ClassicProject.g().getTrgt("StaticTF");
        relStaticTF.zero();
        ProgramRel relClinitTM = (ProgramRel) ClassicProject.g().getTrgt("ClinitTM");
        relClinitTM.zero();

        DomM domM = (DomM) ClassicProject.g().getTrgt("M");
		Program program = Program.g();		
        for(SootClass klass : program.getClasses()){
			if(klass.isPhantom())
				continue;
			Type type = klass.getType();

			relClassT.add(klass.getType());

            for(SootField field : klass.getFields())
                if(field.isStatic())
                    relStaticTF.add(type, field);

            for(SootMethod meth : klass.getMethods())
                if(meth.isStatic() && domM.contains(meth))
                    relStaticTM.add(type, meth);
			
			if(klass.declaresMethodByName("<clinit>")){
				SootMethod clinit = klass.getMethodByName("<clinit>");
				if(domM.contains(clinit))
					relClinitTM.add(type, clinit);
			}

            for(SootClass clazz : SootUtils.subTypesOf(klass))
				relSubtype.add(clazz.getType(), type);//clazz is subtype of klass
        }
		
        relClassT.save();
		relSubtype.save();
		relStaticTM.save();
		relStaticTF.save();
		relClinitTM.save();
    }

	final public boolean canStore(Type objType, Type varType) 
	{
		if(varType instanceof UnknownType) return true;
		if(!(varType instanceof RefLikeType)) return false;
        if(varType == objType) return true;
        if(varType.equals(objType)) return true;
        if(objType instanceof AnySubType) return true;
        if(varType instanceof NullType) return false;
        if(varType instanceof AnySubType) return false;
        return fh.canStoreType(objType, varType);
    }

	public static boolean isQuasiStaticInvk(Unit invkUnit)
	{
		InvokeExpr ie = ((Stmt) invkUnit).getInvokeExpr();
		return isQuasiStaticMeth(ie.getMethod());
	}

	public static boolean isQuasiStaticMeth(SootMethod m)
	{
		if(m.isStatic())
			return true;
		String klassName = m.getDeclaringClass().getName();
		if(klassName.equals("android.app.AlarmManager"))
			return true;
		if(klassName.equals("android.app.Notification"))
			return true;
		return false;
	}

	/*
	private StubAllocNode stubAllocNodeFor(Type type)
	{
		StubAllocNode node = typeToStubAllocNode.get(type);
		if(node == null){
			node = new StubAllocNode(type);
			typeToStubAllocNode.put(type, node);
		}
		return node;
	}
	*/

	public void run()
	{
		Program program = Program.g();		
		//for(SootClass k : program.getClasses()) System.out.println("kk "+k + (k.hasSuperclass() ? k.getSuperclass() : ""));
		//program.buildCallGraph();
		program.runSpark();
		ignoreStub = program.ignoreStub();

		fh = Program.g().scene().getOrMakeFastHierarchy();
		List<MethodPAGBuilder> mpagBuilders = new ArrayList();
		
		domSC = (DomSC) ClassicProject.g().getTrgt("SC");

		populateDomains(mpagBuilders);
		populateRelations(mpagBuilders);

		fh = null;
	}
}
