package petablox.android.analyses;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootField;
import soot.Local;
import soot.Immediate;
import soot.Value;
import soot.Unit;
import soot.Body;
import soot.Type;
import soot.RefLikeType;
import soot.RefType;
import soot.PrimType;
import soot.VoidType;
import soot.NullType;
import soot.AnySubType;
import soot.UnknownType;
import soot.FastHierarchy;
import soot.PatchingChain;
import soot.jimple.Constant;
import soot.jimple.Stmt;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeStmt;
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
import soot.jimple.internal.JimpleLocal;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.tagkit.Tag;
import soot.util.NumberedSet;

import petablox.util.soot.SootUtilities;
import petablox.android.missingmodels.jimplesrcmapper.Printer;
import petablox.project.analyses.JavaAnalysis;         
import petablox.project.analyses.ProgramRel;			
import petablox.util.IndexSet;		
import petablox.project.ClassicProject;					
import petablox.analyses.alias.CICGAnalysis;
import petablox.analyses.alias.ICICG;
import petablox.program.Program;							
import petablox.project.Petablox;
import petablox.analyses.method.DomM;  
import petablox.analyses.var.DomV;  

import java.util.*;

@Petablox(name="base-java", 
	   produces={"U",
				 "Alloc", "Assign", 
				 "Load", "Store", 
				 "LoadStat", "StoreStat", 
				 "chaIM",
				 "HTFilter", 
				 "MU",
				 "AssignPrim", 
				 "LoadPrim", "StorePrim",
				 "LoadStatPrim", "StoreStatPrim",
				 "MmethPrimArg", "MmethPrimRet", 
				 "IinvkPrimRet", "IinvkPrimArg",
	             "Stub", "Framework"}  ,
       namesOfTypes = { "U"}, 
       types = { DomU.class },       
  		namesOfSigns = {    "Alloc", "Assign", 
							"Load", "Store", 
							"LoadStat", "StoreStat", 
							"chaIM",
							"HTFilter",
							"MU",
							"AssignPrim", 
							"LoadPrim", "StorePrim",
							"LoadStatPrim", "StoreStatPrim",
							"MmethPrimArg", "MmethPrimRet", 
							"IinvkPrimRet", "IinvkPrimArg",
							"Stub", "Framework"},

				signs = {   "V0,H0:V0_H0", "V0,V1:V0xV1",
							"V0,V1,F0:F0_V0xV1", "V0,F0,V1:F0_V0xV1",
							"V0,F0:F0_V0", "F0,V0:F0_V0",
							"I0,M0:I0_M0", 
							"H0,T0:H0_T0",                   
							"M0,U0:M0_U0",
							"U0,U1:U0xU1",
							"U0,V0,F0:U0_V0_F0", "V0,F0,U0:U0_V0_F0",
							"U0,F0:U0_F0", "F0,U0:U0_F0",
							"M0,Z0,U0:M0_U0_Z0", "M0,Z0,U0:M0_U0_Z0",
							"I0,Z0,U0:I0_U0_Z0", "I0,Z0,U0:I0_U0_Z0",
							"M0:M0", "M0:M0" }  
	   )
public class PAGBuilder extends JavaAnalysis
{
	private ProgramRel relAlloc;//(l:V,h:H)
	private ProgramRel relAssign;//(l:V,r:V)
	private ProgramRel relLoad;//(l:V,b:V,f:F)
	private ProgramRel relStore;//(b:V,f:F,r:V)
	private ProgramRel relLoadStat;//(l:V,f:F)
	private ProgramRel relStoreStat;//(f:F,r:V)

	private ProgramRel relAssignPrim;//(l:U,r:U)
	private ProgramRel relLoadPrim;//(l:U,b:V,f:F)
	private ProgramRel relStorePrim;//(b:V,f:F,r:U)
	private ProgramRel relLoadStatPrim;//(l:U,f:F)
	private ProgramRel relStoreStatPrim;//(f:F,r:U)

    private ProgramRel relMmethPrimArg;//(m:M,z:Z,u:U)
    private ProgramRel relMmethPrimRet;//(m:M,z:Z,u:U)
    private ProgramRel relIinvkPrimRet;//(i:I,n:Z,u:U)
    private ProgramRel relIinvkPrimArg;//(i:I,n:Z,u:U)

	private ProgramRel relHTFilter;
	private ProgramRel relMH;
	private ProgramRel relMU;

	private DomU domU;

	private int maxArgs = -1;
	private FastHierarchy fh;
	public static NumberedSet stubMethods;
	public static NumberedSet frameworkMethods;

	public static final boolean ignoreStubs = false;

	void openRels()
	{
		relAlloc = (ProgramRel) ClassicProject.g().getTrgt("Alloc");
		relAlloc.zero();
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

		relHTFilter = (ProgramRel) ClassicProject.g().getTrgt("HTFilter");
		relHTFilter.zero();
		relMH = (ProgramRel) ClassicProject.g().getTrgt("MH");
        relMH.zero();
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
	}
	
	void saveRels()
	{
		relAlloc.save();
		relAssign.save();
		relLoad.save();
		relStore.save();
		relLoadStat.save();
		relStoreStat.save();

		relHTFilter.save();
		relMH.save();
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
	}

	void Alloc(Local l, Stmt h)
	{
		assert l != null;
		relAlloc.add(l, h);
	}

	void Assign(Local l, Local r)
	{
		if(l == null || r == null)
			return;
		relAssign.add(l, r);
	}

	void Load(Local l, Local b, SparkField f)
	{
		if(b == null || l == null)
			return;
		if(f instanceof SootField && (((SootField) f).getDeclaringClass().isPhantom()))
		   return;
		relLoad.add(l, b, f);
	}

	void Store(Local b, SparkField f, Local r)
	{
		if(b == null || r == null)
			return;
		if(f instanceof SootField && (((SootField) f).getDeclaringClass().isPhantom()))
		   return;
		relStore.add(b, f, r);
	}

	void LoadStat(Local l, SootField f)
	{
		if(l == null)
			return;
		if(f instanceof SootField && (((SootField) f).getDeclaringClass().isPhantom()))
		   return;
		relLoadStat.add(l, f);
	}

	void StoreStat(SootField f, Local r)
	{
		if(r == null)
			return;
		if(f instanceof SootField && (((SootField) f).getDeclaringClass().isPhantom()))
		   return;
		relStoreStat.add(f, r);
	}

	void AssignPrim(Local l, Local r)
	{
		if(l == null || r == null)
			return;
		relAssignPrim.add(l, r);
	}

	void LoadPrim(Local l, Local b, SparkField f)
	{
		if(b == null || l == null)
			return;
		if(f instanceof SootField && (((SootField) f).getDeclaringClass().isPhantom()))
		   return;
		relLoadPrim.add(l, b, f);
	}

	void StorePrim(Local b, SparkField f, Local r)
	{
		if(b == null || r == null)
			return;
		if(f instanceof SootField && (((SootField) f).getDeclaringClass().isPhantom()))
		   return;
		relStorePrim.add(b, f, r);
	}

	void LoadStatPrim(Local l, SootField f)
	{
		if(l == null)
			return;
		if(f instanceof SootField && (((SootField) f).getDeclaringClass().isPhantom()))
		   return;
		relLoadStatPrim.add(l, f);
	}

	void StoreStatPrim(SootField f, Local r)
	{
		if(r == null)
			return;
		if(f instanceof SootField && (((SootField) f).getDeclaringClass().isPhantom()))
		   return;
		relStoreStatPrim.add(f, r);
	}

	void MmethPrimArg(SootMethod m, int index, Local v)
	{	if(v == null)
			return;
		relMmethPrimArg.add(m, new Integer(index), v);
	}

	void MmethPrimRet(SootMethod m, Local v)
	{
		if(v == null)
			return;
		relMmethPrimRet.add(m, new Integer(0), v);
	}
	
	void IinvkPrimArg(Unit invkUnit, int index, Local v)
	{
		if(v == null)
			return;
		relIinvkPrimArg.add(invkUnit, new Integer(index), v);
	}

	void IinvkPrimRet(Unit invkUnit, Local v)
	{
		if(v == null)
			return;
		relIinvkPrimRet.add(invkUnit, new Integer(0), v);
	}

	class MethodPAGBuilder
	{
		private Local thisVar;
		private Local retVar;
		private Local[] paramVars;
		private SootMethod method;
		private Set<Local> nonPrimLocals;
		private Set<Local> primLocals;
		private Map<Stmt,CastExpr> stmtToCastNode;
		private Tag containerTag;

		MethodPAGBuilder(SootMethod method)
		{
			this.method = method;
		}
		
		void pass1()
		{	int count = method.getParameterCount();
			Local[] locals = SootUtilities.getMethArgLocals(method);
			if(locals == null)
				return;
			if(!method.isStatic()) {
				thisVar = locals[0];
				paramVars = new Local[locals.length - 1];
				for(int i=1; i<locals.length; i++){
					paramVars[i-1] = locals[i];
				}
			}
			else{
				paramVars = new Local[locals.length];
				for(int i=0; i<locals.length; i++){
					paramVars[i] = locals[i];
				}

			} 		
			if(count > 0){				
				int j = 0;		
				for(Type pType : method.getParameterTypes()){
					if(pType instanceof PrimType){
						if(paramVars != null)
							domU.add(paramVars[j], method);
						j++;
					}
				}
			}
			
			Type retType = method.getReturnType();
			if(!(retType instanceof VoidType)){
				retVar = SootUtilities.getReturnLocal(method);   
				if(retType instanceof PrimType && retVar!= null)
						domU.add(retVar,method);	
			} 

			if(!method.isConcrete())
				return;

			Body body = method.retrieveActiveBody();
			LocalsClassifier lc = new LocalsClassifier(body);
			primLocals = lc.primLocals();
			nonPrimLocals = lc.nonPrimLocals();
			for(Local l : body.getLocals()){
				boolean isPrim = primLocals.contains(l);
				if(isPrim)
					domU.add(l,method);
			}

			stmtToCastNode = new HashMap();
			for(Unit unit : body.getUnits()){
				Stmt s = (Stmt) unit;
				if(s instanceof AssignStmt) {
					Value rightOp = ((AssignStmt) s).getRightOp();
					if(rightOp instanceof CastExpr){
						CastExpr castExpr = (CastExpr) rightOp;
						Type castType = castExpr.getCastType();
						if(castType instanceof RefLikeType){			//TODO
							stmtToCastNode.put(s, castExpr);
						}
					}
				}
			}						
		}

		void pass2()
		{
			int i = 0;			
			if(paramVars != null){
				for(int j = 0; j < paramVars.length; j++){
					Type paramType = method.getParameterType(j);
					Local node = paramVars[j];
					if(paramType instanceof PrimType){
						MmethPrimArg(method, i, node);
						relMU.add(method, node);
					} 
					i++;
				}
			}
			
			if(retVar != null){
				Type retType = method.getReturnType();
				if(retType instanceof PrimType){
					MmethPrimRet(method, retVar);
					relMU.add(method, retVar);
				} 
			}

			if(!method.isConcrete())
				return;
			
			Body body = method.retrieveActiveBody();
			for(Local Local : body.getLocals()){
				if(primLocals.contains(Local))
					relMU.add(method, Local);
			}
			for(Map.Entry<Stmt,CastExpr> e : stmtToCastNode.entrySet()){
				Type castType = ((CastExpr) ((AssignStmt) e.getKey()).getRightOp()).getCastType();
			}
			
			containerTag = new ContainerTag(method);
			for(Unit unit : body.getUnits()){
				handleStmt((Stmt) unit);
			}
		}

		Local nodeFor(Immediate i)
		{
			if(i instanceof Constant)
				return null;
			return (Local) i;
		}
		
		void handleStmt(Stmt s)
		{
			if(s.containsInvokeExpr()){
				InvokeExpr ie = s.getInvokeExpr();
				SootMethod callee = ie.getMethod();
				int numArgs = ie.getArgCount();
				s.addTag(containerTag);

				//handle receiver
				int j = 0;
				if(ie instanceof InstanceInvokeExpr)
					j++;
				
				//handle args
				for(int i = 0; i < numArgs; i++,j++){
					Immediate arg = (Immediate) ie.getArg(i);
					Type argType = callee.getParameterType(i);
					if(argType instanceof PrimType)
						IinvkPrimArg(s, j, nodeFor(arg));
				}
				
				//return value
				if(s instanceof AssignStmt){
					Local lhs = (Local) ((AssignStmt) s).getLeftOp();
					Type retType = callee.getReturnType();
					if(retType instanceof PrimType)
						IinvkPrimRet(s, nodeFor(lhs));
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
							LoadStat(nodeFor((Local) leftOp), field);
						else if(fieldType instanceof PrimType)
							LoadStatPrim(nodeFor((Local) leftOp), field);
					} else{
						Immediate base = (Immediate) ((InstanceFieldRef) fr).getBase();
						if(fieldType instanceof RefLikeType)
							Load(nodeFor((Local) leftOp), nodeFor(base), field);
						else if(fieldType instanceof PrimType)
							LoadPrim(nodeFor((Local) leftOp), nodeFor(base), field);
					}
				}else{
					//store
					assert leftOp == fr;
					Immediate rightOp = (Immediate) as.getRightOp();
					if(field.isStatic()){
						if(fieldType instanceof RefLikeType)
							StoreStat(field, nodeFor(rightOp));
						else if(fieldType instanceof PrimType)
							StoreStatPrim(field, nodeFor(rightOp));
					} else{
						Immediate base = (Immediate) ((InstanceFieldRef) fr).getBase();
						if(fieldType instanceof RefLikeType)
							Store(nodeFor(base), field, nodeFor(rightOp));
						else if(fieldType instanceof PrimType)
							StorePrim(nodeFor(base), field, nodeFor(rightOp));		
					}
				}
			} else if(s.containsArrayRef()) {
				AssignStmt as = (AssignStmt) s;
				Value leftOp = as.getLeftOp();
				ArrayRef ar = s.getArrayRef();
				Immediate base = (Immediate) ar.getBase();
				SparkField field = null;
				if(leftOp instanceof Local){
					//array read
					Local l = (Local) leftOp;
					if(nonPrimLocals.contains(l))
						Load(nodeFor(l), nodeFor(base), field);
					if(primLocals.contains(l)) {
						LoadPrim(nodeFor(l), nodeFor(base), field);
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
							Store(nodeFor(base), field, nodeFor(r));
						if(primLocals.contains(r))
							StorePrim(nodeFor(base), field, nodeFor(r));
					}
				}
			} else if(s instanceof AssignStmt) {
				AssignStmt as = (AssignStmt) s;
				Value leftOp = as.getLeftOp();
				Value rightOp = as.getRightOp();

				if(rightOp instanceof AnyNewExpr){
					Alloc(nodeFor((Local) leftOp), s);
					relMH.add(method, s);
					s.addTag(containerTag);
					Iterator<Type> typesIt = Program.g().getTypes().iterator();
					while(typesIt.hasNext()){
						Type varType = typesIt.next();
						if(canStore(rightOp.getType(), varType))
							relHTFilter.add(s, varType);
					}
				} else if(rightOp instanceof CastExpr){
					Type castType = ((CastExpr) rightOp).getCastType();
					Immediate op = (Immediate) ((CastExpr) rightOp).getOp();
					if(castType instanceof RefLikeType){
						//Local castNode = stmtToCastNode.get(s);	//TODO
						//Assign(castNode, nodeFor(op));
						//Assign(nodeFor((Local) leftOp), castNode);
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
					Local leftNode = nodeFor((Local) leftOp);
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
					if(type instanceof RefLikeType){
						Assign(nodeFor(leftOp), paramVars[index]);
					}
						
					else if(type instanceof PrimType)
						AssignPrim(nodeFor(leftOp), paramVars[index]);
				}
			}
		}
	}
	void populateMethods()
	{
		Program program = Program.g();
		stubMethods = new NumberedSet(Scene.v().getMethodNumberer());
		frameworkMethods = new NumberedSet(Scene.v().getMethodNumberer());
		IndexSet<SootMethod> methods = program.getMethods();
		for(SootMethod m : methods){                    
			if(isStub(m)){
				stubMethods.add(m);
			}
			if(isFramework(m)) {
				frameworkMethods.add(m);
			}
		}

		ProgramRel relStub = (ProgramRel) ClassicProject.g().getTrgt("Stub");
        relStub.zero();
		for(Iterator it = stubMethods.iterator(); it.hasNext();){
			SootMethod stub = (SootMethod) it.next();
			relStub.add(stub);
		}
		relStub.save();

		ProgramRel relFramework = (ProgramRel) ClassicProject.g().getTrgt("Framework");
        relFramework.zero();
		for(Iterator it = frameworkMethods.iterator(); it.hasNext();){
			SootMethod framework = (SootMethod) it.next();
			relFramework.add(framework);
		}
		relFramework.save();
	}

	void populateDomains(List<MethodPAGBuilder> mpagBuilders)
	{
		populateMethods();
		domU = (DomU) ClassicProject.g().getTrgt("U");
		
		
		DomM domM = (DomM) ClassicProject.g().getTrgt("M");   
		int numM = domM.size();
		for (int mIdx = 0; mIdx < numM; mIdx++) {
			SootMethod m = domM.get(mIdx);
			if(ignoreStubs){
				if(stubMethods.contains(m))
					continue;
			}
			MethodPAGBuilder mpagBuilder = new MethodPAGBuilder(m);
			mpagBuilder.pass1();
			mpagBuilders.add(mpagBuilder);
		}
		domU.save();
	}
	
	boolean isFramework(SootMethod method) {
		if(!method.isConcrete())
			return false;
		return Printer.isFrameworkClass(method.getDeclaringClass());		
	}

	boolean isStub(SootMethod method)
	{
		if(!method.isConcrete())
			return false;
		PatchingChain<Unit> units = method.retrieveActiveBody().getUnits();
		Unit unit = units.getFirst();
		while(unit instanceof IdentityStmt)
			unit = units.getSuccOf(unit);

		//if method is <init>, then next stmt could be a call to super.<init>
		if(method.getName().equals("<init>")){
		    if(unit instanceof InvokeStmt){
			if(((InvokeStmt) unit).getInvokeExpr().getMethod().getName().equals("<init>"))
			    unit = units.getSuccOf(unit);
		    }
		}

		if(!(unit instanceof AssignStmt))
			return false;
		Value rightOp = ((AssignStmt) unit).getRightOp();
		if(!(rightOp instanceof NewExpr))
			return false;
		if(!((NewExpr) rightOp).getType().toString().equals("java.lang.RuntimeException"))
			return false;
		Local e = (Local) ((AssignStmt) unit).getLeftOp();
		
		//may be there is an assignment (if soot did not optimized it away)
		Local f = null;
		unit = units.getSuccOf(unit);
		if(unit instanceof AssignStmt){
			f = (Local) ((AssignStmt) unit).getLeftOp();
			if(!((AssignStmt) unit).getRightOp().equals(e))
				return false;
			unit = units.getSuccOf(unit);
		}
		//it should be the call to the constructor
		Stmt s = (Stmt) unit;
		if(!s.containsInvokeExpr())
			return false;
		if(!s.getInvokeExpr().getMethod().getSignature().equals("<java.lang.RuntimeException: void <init>(java.lang.String)>"))
			return false;
		unit = units.getSuccOf(unit);
		if(!(unit instanceof ThrowStmt))
			return false;
		Immediate i = (Immediate) ((ThrowStmt) unit).getOp();
		return i.equals(e) || i.equals(f);
	}
	
	void populateRelations(List<MethodPAGBuilder> mpagBuilders)
	{
		openRels();
		for(MethodPAGBuilder mpagBuilder : mpagBuilders)
			mpagBuilder.pass2();
		saveRels();
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

	public void run()
	{
		Program program = Program.g();		
		fh = Scene.v().getOrMakeFastHierarchy();
		List<MethodPAGBuilder> mpagBuilders = new ArrayList();
		populateDomains(mpagBuilders);
		populateRelations(mpagBuilders);
		fh = null;
	}
	
}