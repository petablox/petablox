package stamp.analyses.string;

import soot.SootMethod;
import soot.Local;
import soot.Immediate;
import soot.Value;
import soot.SootField;
import soot.Unit;
import soot.RefType;
import soot.Type;
import soot.jimple.StringConstant;
import soot.jimple.Constant;
import soot.jimple.Stmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.ArrayRef;
import soot.jimple.ReturnStmt;
import soot.jimple.NewExpr;
import soot.jimple.CastExpr;

import shord.program.Program;
import shord.analyses.VarNode;
import shord.analyses.AllocNode;
import shord.analyses.LocalVarNode;
import shord.analyses.DomV;
import shord.project.ClassicProject;
import shord.project.analyses.ProgramRel;

import stamp.srcmap.sourceinfo.abstractinfo.AbstractSourceInfo;

import chord.bddbddb.Rel.RelView;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

import java.util.*;

/*
  @author Saswat Anand
*/
public class Slicer
{
	private List<Trio<Local,Stmt,SootMethod>> workList;

	private Set<Pair<Local,Stmt>> visited;

	private List<Pair<Local,Stmt>> mLocalList;
	private SootMethod m;

	private Map<Local,LocalVarNode> localToNode = new HashMap();

	private ProgramRel relIM;
	private ProgramRel relIpt;

	private Map<SootField,List<Trio<Local,Stmt,Immediate>>> fieldToInstanceStores = new HashMap();
	private Map<SootField,List<Pair<Stmt,Immediate>>> fieldToStaticStores = new HashMap();

	private Map<Stmt,List<Statement>> stmtToStatements = new HashMap();
	
	private StatementGraph sGraph;

	private static Type stringType = RefType.v("java.lang.String");
	private static Type stringBuilderType = RefType.v("java.lang.StringBuilder");
	private static Type stringBufferType = RefType.v("java.lang.StringBuffer");
	private static Type objectType = RefType.v("java.lang.Object");
	private static Type charSequenceType = RefType.v("java.lang.CharSequence");

	public Slicer()
	{
		init();
	}
		
	public void generate(Local l, Stmt s, SootMethod m)
	{
		this.workList = new LinkedList();
		this.visited = new HashSet();
		this.stmtToStatements = new HashMap();
		this.mLocalList = null;
		this.m = null;
		this.sGraph = new StatementGraph();

		Statement c = new Criterion(l, s);
		sGraph.addNode(c);
		List<Statement> ss = new ArrayList();
		ss.add(c);
		stmtToStatements.put(s, ss);

		workList.add(new Trio(l, s, m));

		while(!workList.isEmpty()){
			Trio<Local,Stmt,SootMethod> t = workList.remove(0);

			Local local = t.val0;
			Stmt stmt = t.val1;
			SootMethod method = t.val2;

			visit(method, stmt, local);			
		}
		
		System.out.println(sliceStr());
	}

	public Set<String> evaluate(Local l, Stmt stmt, SootMethod m)
	{
		generate(l, stmt, m);
		Evaluator evaluator = new Evaluator(this, this.sGraph);
		return evaluator.evaluate();
	}

	private void visit(SootMethod method, Stmt s, Local l)
	{
		this.m = method;
		this.mLocalList = new LinkedList();
		mLocalList.add(new Pair(l,s));
		
		ReachingDefsAnalysis ld = new ReachingDefsAnalysis(m.retrieveActiveBody());

		while(!mLocalList.isEmpty()){
			Pair<Local,Stmt> p = mLocalList.remove(0);
			if(visited.contains(p))
				continue;
			visited.add(p);

			Local local = p.val0;
			Stmt useStmt = p.val1;

			System.out.println("Processing local:"+ local + " useStmt:"+useStmt);

			for(Stmt stmt : ld.getDefsOf(local, useStmt)){
				if(stmt instanceof DefinitionStmt){
					DefinitionStmt ds = (DefinitionStmt) stmt;
					Value leftOp = ds.getLeftOp();
					Value rightOp = ds.getRightOp();
					
					if(local.equals(leftOp)){
						handleDefinitionStmt(ds, (Local) leftOp, rightOp, useStmt);
					}
				}
				
				if(!stmt.containsInvokeExpr())
					continue;

				InvokeExpr ie = stmt.getInvokeExpr();
				if(!(ie instanceof InstanceInvokeExpr))
					continue;
				Local rcvr = (Local) ((InstanceInvokeExpr) ie).getBase();
				if(!rcvr.equals(local))
					continue;
				String mSig = ie.getMethod().getSignature();

				if(mSig.equals("<java.lang.StringBuilder: void <init>(java.lang.String)>") ||
				   mSig.equals("<java.lang.StringBuffer: void <init>(java.lang.String)>") ||
				   mSig.equals("<java.lang.String: void <init>(java.lang.String)>")){
					Immediate arg = (Immediate) ie.getArg(0);
					//slice.add(new Assign(arg, rcvr));
					if(isInteresting(arg))
						addToSlice(new Assign(arg, rcvr, stmt), stmt, useStmt);
					if(arg instanceof Local){
						mLocalList.add(new Pair((Local) arg, stmt));
					}
				} else if(mSig.equals("<java.lang.StringBuilder: void <init>()>") ||
						  mSig.equals("<java.lang.StringBuffer: void <init>()>") ||
						  mSig.equals("<java.lang.String: void <init>()>")){
					//slice.add(new Assign(StringConstant.v(""), rcvr));
					addToSlice(new Assign(StringConstant.v(""), rcvr, stmt), stmt, useStmt);
				} else if(mSig.equals("<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>") ||
						  mSig.equals("<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.String)>")){
					Immediate arg = (Immediate) ie.getArg(0);
					//slice.add(new Concat(rcvr, arg, rcvr));
					addToSlice(new Concat(rcvr, arg, rcvr, stmt), stmt, useStmt);
					if(arg instanceof Local){
						mLocalList.add(new Pair((Local) arg, stmt));
					}
					mLocalList.add(new Pair(rcvr, stmt));
				}
			}
		}
	}

	private void handleDefinitionStmt(Stmt dfnStmt, Local local, Value rightOp, Stmt useStmt)
	{
		//System.out.println("handleDefinitionStmt: "+dfnStmt);
		if(rightOp instanceof ParameterRef){
			//find caller
			int index = ((ParameterRef) rightOp).getIndex();
			for(Object cs : callsitesFor(m)){
				Stmt callsite = (Stmt) cs;
				InvokeExpr ie = callsite.getInvokeExpr();
				Immediate arg = (Immediate) ie.getArg(index);
				if(isInteresting(arg)){
					Statement s = new Assign(arg, local, dfnStmt);
					//slice.add(s); //System.out.println(stmtStr(s));
					//addToSlice(s, dfnStmt, useStmt);
					addToSlice(s, callsite, useStmt);
					if(arg instanceof Local){
						Local loc = (Local) arg;
						SootMethod containerMethod = containerMethodFor(loc);
						workList.add(new Trio(loc, callsite, containerMethod));
					}
				}
			}
		} else if(rightOp instanceof InstanceFieldRef){
			//alias
			Local base = (Local) ((InstanceFieldRef) rightOp).getBase();
			SootField field = ((InstanceFieldRef) rightOp).getField();
			for(Pair<Stmt,Immediate> pair : findAlias(base, field)){
				Stmt stmt = pair.val0;
				Immediate alias = pair.val1;
				if(isInteresting(alias)){
					Statement s = new Assign(alias, local, dfnStmt);
					//slice.add(s); //System.out.println(stmtStr(s));
					//addToSlice(s, dfnStmt, useStmt);
					addToSlice(s, stmt, useStmt);
					if(alias instanceof Local){
						SootMethod containerMethod = containerMethodFor((Local) alias);
						workList.add(new Trio((Local) alias, stmt, containerMethod));
					}
				}
			}
		} else if(rightOp instanceof StaticFieldRef){
			SootField field = ((StaticFieldRef) rightOp).getField();
			for(Pair<Stmt,Immediate> pair : findAlias(field)){
				Stmt stmt = pair.val0;
				Immediate alias = pair.val1;
				if(isInteresting(alias)){
					Statement s = new Assign(alias, local, dfnStmt);
					//slice.add(s); //System.out.println(stmtStr(s));
					//addToSlice(s, dfnStmt, useStmt);
					addToSlice(s, stmt, useStmt);
					if(alias instanceof Local){
						SootMethod containerMethod = containerMethodFor((Local) alias);
						workList.add(new Trio((Local) alias, stmt, containerMethod));
					}
				}
			}
		} else if(rightOp instanceof ArrayRef){
			Statement s = new Havoc(local, dfnStmt);
			//slice.add(s); //System.out.println(stmtStr(s));
			addToSlice(s, dfnStmt, useStmt);
		} else if(rightOp instanceof InvokeExpr){
			InvokeExpr ie = (InvokeExpr) rightOp;
			String mSig = ie.getMethod().getSignature();
			if(mSig.equals("<java.lang.StringBuilder: java.lang.String toString()>") ||
			   mSig.equals("<java.lang.StringBuffer: java.lang.String toString()>")){
				Local rcvr = (Local) ((VirtualInvokeExpr) ie).getBase();
				Statement s = new Assign(rcvr, local, dfnStmt);
				//slice.add(s); //System.out.println(stmtStr(s));
				addToSlice(s, dfnStmt, useStmt);
				mLocalList.add(new Pair(rcvr, dfnStmt));
			} else if(mSig.equals("<java.lang.String: java.lang.String toLowerCase()>")){
				Local rcvr = (Local) ((VirtualInvokeExpr) ie).getBase();
				Statement s = new ToLower(rcvr, local, dfnStmt);
				//slice.add(s); //System.out.println(stmtStr(s));
				addToSlice(s, dfnStmt, useStmt);
				mLocalList.add(new Pair(rcvr, dfnStmt));
			} else if(mSig.equals("<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>") ||
					  mSig.equals("<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.String)>")){
				Immediate arg = (Immediate) ie.getArg(0);
				Local rcvr = (Local) ((VirtualInvokeExpr) ie).getBase();
				Statement s = new Concat(rcvr, arg, local, dfnStmt);
				//slice.add(s); //System.out.println(stmtStr(s));
				addToSlice(s, dfnStmt, useStmt);
				if(arg instanceof Local){
					mLocalList.add(new Pair((Local) arg, dfnStmt));
				}
				mLocalList.add(new Pair(rcvr, dfnStmt));
			} else if(mSig.equals("<java.lang.String: java.lang.String concat(java.lang.String)>")){
				Immediate arg = (Immediate) ie.getArg(0);
				Local rcvr = (Local) ((VirtualInvokeExpr) ie).getBase();
				Statement s = new Concat(rcvr, arg, local, dfnStmt);
				//slice.add(s); //System.out.println(stmtStr(s));
				addToSlice(s, dfnStmt, useStmt);
				if(arg instanceof Local){
					mLocalList.add(new Pair((Local) arg, dfnStmt));
				}
				mLocalList.add(new Pair(rcvr, dfnStmt));
			} else {
				for(SootMethod callee : calleesFor(dfnStmt)){
					if(AbstractSourceInfo.isFrameworkClass(callee.getDeclaringClass())){
						String calleeSig = callee.getSignature();
						Statement s;
						if(calleeSig.equals("<java.util.Locale: java.lang.String getCountry()>") ||
						   calleeSig.equals("<java.util.Locale: java.lang.String getLanguage()>") ||
						   calleeSig.equals("<android.content.pm.PackageItemInfo: java.lang.CharSequence loadLabel(android.content.pm.PackageManager)>")){
							s = new Assign(StringConstant.v(String.format("$stamp$%s$stamp$", calleeSig)), local, dfnStmt);
						}
						else 
							s = new Havoc(local, dfnStmt);
						//slice.add(s); //System.out.println(stmtStr(s));
						addToSlice(s, dfnStmt, useStmt);
					} else {
						for(Pair<Stmt,Immediate> pair : retsFor(callee)){
							Stmt stmt = pair.val0;
							Immediate r = pair.val1;
							if(isInteresting(r)){
								Statement s = new Assign(r, local, dfnStmt);
								//slice.add(s); //System.out.println(stmtStr(s));
								//addToSlice(s, dfnStmt, useStmt);
								addToSlice(s, stmt, useStmt);
								if(r instanceof Local)
									workList.add(new Trio((Local) r, stmt, callee));
							}
						}
					}
				}
			}
		} else if(rightOp instanceof Immediate){
			if(isInteresting((Immediate) rightOp)){
				Statement s = new Assign((Immediate) rightOp, local, dfnStmt);
				//slice.add(s); //System.out.println(stmtStr(s));
				addToSlice(s, dfnStmt, useStmt);
				if(rightOp instanceof Local)
					mLocalList.add(new Pair((Local) rightOp, dfnStmt));
				
			}
		} else if(rightOp instanceof NewExpr){
			//dont cause havoc
		} else if(rightOp instanceof CastExpr){
			CastExpr ce = (CastExpr) rightOp;
			Immediate castOp = (Immediate) ce.getOp();
			if(isInteresting(castOp) && isStringType(ce.getCastType())){
				Statement s = new Assign(castOp, local, dfnStmt);
				addToSlice(s, dfnStmt, useStmt);
				if(castOp instanceof Local)
					mLocalList.add(new Pair((Local) castOp, dfnStmt));
			}
		} else {
			Statement s = new Havoc(local, dfnStmt);
			//slice.add(s); //System.out.println(stmtStr(s));
			addToSlice(s, dfnStmt, useStmt);
		}
	}

	private void addToSlice(Statement dependeeStatement, Stmt dependee, Stmt dependent)
	{
		sGraph.addNode(dependeeStatement);

		List<Statement> ss = stmtToStatements.get(dependee);
		if(ss == null){
			ss = new ArrayList();
			stmtToStatements.put(dependee, ss);
		}
		ss.add(dependeeStatement);

		System.out.println("mapping "+ dependee + " to " + stmtStr(dependeeStatement));

		//System.out.println("$$ "+dependent);
		List<Statement> dependentStatements = stmtToStatements.get(dependent);
		for(Statement dependentStatement : dependentStatements){
			sGraph.addEdge(dependeeStatement, dependentStatement);
			System.out.println("Adding edge from: "+stmtStr(dependeeStatement)+ " to: "+ stmtStr(dependentStatement));
		}
	}

	private void init()
	{
        DomV domV = (DomV) ClassicProject.g().getTrgt("V");
        for(VarNode node : domV){
			if(!(node instanceof LocalVarNode))
				continue;
			Local local = ((LocalVarNode) node).local;
			localToNode.put(local, (LocalVarNode) node);
		}
		
		relIM = (ProgramRel) ClassicProject.g().getTrgt("ci_IM");
		relIM.load();
		relIpt = (ProgramRel) ClassicProject.g().getTrgt("ci_pt");		
		relIpt.load();

		Iterator mIt = Program.g().scene().getReachableMethods().listener();
		while(mIt.hasNext()){
			SootMethod m = (SootMethod) mIt.next();
			if(!m.isConcrete())
				continue;
			for(Unit unit : m.retrieveActiveBody().getUnits()){
				Stmt stmt = (Stmt) unit;
				if(!stmt.containsFieldRef())
					continue;
				Value leftOp = ((DefinitionStmt) stmt).getLeftOp();
				Value rightOp = ((DefinitionStmt) stmt).getRightOp();
				if(leftOp instanceof InstanceFieldRef){
					InstanceFieldRef ifr = (InstanceFieldRef) leftOp;
					Local base = (Local) ifr.getBase();
					SootField field = ifr.getField();
					List<Trio<Local,Stmt,Immediate>> triples = fieldToInstanceStores.get(field);
					if(triples == null){
						triples = new ArrayList();
						fieldToInstanceStores.put(field, triples);
					}
					triples.add(new Trio(base, stmt, (Immediate) rightOp));
				} else if(leftOp instanceof StaticFieldRef){
					StaticFieldRef sfr = (StaticFieldRef) leftOp;
					SootField field = sfr.getField();
					List<Pair<Stmt,Immediate>> imms = fieldToStaticStores.get(field);
					if(imms == null){
						imms = new ArrayList();
						fieldToStaticStores.put(field, imms);
					}
					imms.add(new Pair(stmt, (Immediate) rightOp));
				}
			}
		}
	}

	public void finish()
	{
		relIM.close();
		relIpt.close();
	}

	private Iterable<Object> callsitesFor(SootMethod meth) 
	{
		RelView viewIM = relIM.getView();
        viewIM.selectAndDelete(1, meth);
        return viewIM.getAry1ValTuples();
    }

	private Iterable<SootMethod> calleesFor(Stmt stmt) 
	{
		RelView viewIM = relIM.getView();
        viewIM.selectAndDelete(0, stmt);
        Iterable<SootMethod> it = viewIM.getAry1ValTuples();
		return it;
    }

	private SootMethod containerMethodFor(Local local) 
	{
		return localToNode.get(local).meth;
    }
	
	private Iterable<Pair<Stmt,Immediate>> findAlias(Local local, SootField f)
	{
		VarNode vn = localToNode.get(local);
		RelView viewIpt1 = relIpt.getView();
		viewIpt1.selectAndDelete(0, vn);
		Iterable<AllocNode> os = viewIpt1.getAry1ValTuples();

		Iterable<Pair<Stmt,Immediate>> ret;	

		Iterable<Trio<Local,Stmt,Immediate>> it = fieldToInstanceStores.get(f);
		if(it == null){
			ret = handleSpecialField(f);
			if(ret == null){
				System.out.println("Warning: No stores found for field "+f);
				ret = Collections.emptyList();
			}
		} else {
			List<Pair<Stmt,Immediate>> aliases = new ArrayList();
			ret = aliases;
			for(Trio<Local,Stmt,Immediate> trio : it){
				Local base = trio.val0;
				Stmt stmt = trio.val1;
				Immediate alias = trio.val2;
			
				//check if base and local can point to a common object
				VarNode baseNode = localToNode.get(base);
				RelView viewIpt2 = relIpt.getView();
				viewIpt2.selectAndDelete(0, baseNode);
				
				boolean isAlias = false;
				for(AllocNode o : os){
					if(viewIpt2.contains(o)){
						isAlias = true;
						break;
					}
				}
		
				if(isAlias){
					aliases.add(new Pair(stmt,alias));
				}
				viewIpt2.free();
			}
		}
		
		viewIpt1.free();
		return ret;
	}
	
	private Iterable<Pair<Stmt,Immediate>> findAlias(SootField f)
	{
		Iterable<Pair<Stmt,Immediate>> ret = handleSpecialField(f);
		if(ret == null){
			ret = fieldToStaticStores.get(f);
			if(ret == null)
				ret = Collections.emptySet();
		}
		return ret;
	}

	private Iterable<Pair<Stmt,Immediate>> handleSpecialField(SootField f)
	{
		String s = null;

		String cName = f.getDeclaringClass().getName();
		String fSig = f.getSignature();
		String fName = f.getName();

		if(cName.equals("android.os.Build$VERSION") || cName.equals("android.os.Build"))
			s = String.format("$stamp$%s: %s$stamp$", cName, fName);
		else if(fSig.equals("<android.content.pm.PackageInfo: java.lang.String versionName>") ||
				fSig.equals("<android.content.pm.PackageInfo: java.lang.String packageName>"))
			s = String.format("$stamp$%s: %s$stamp$", cName, fName);

		if(s != null){
			List<Pair<Stmt,Immediate>> list = new ArrayList();
			list.add(new Pair(null, StringConstant.v(s)));
			return list;
		} else
			return null;
	}

	private Iterable<Pair<Stmt,Immediate>> retsFor(SootMethod m)
	{
		if(!m.isConcrete())
			return Collections.EMPTY_LIST;
		List<Pair<Stmt,Immediate>> rets = new ArrayList();
		for(Unit unit : m.retrieveActiveBody().getUnits()){
			if(!(unit instanceof ReturnStmt))
				continue;
			Immediate retOp = (Immediate) ((ReturnStmt) unit).getOp();
			rets.add(new Pair((Stmt) unit, retOp));
		}
		return rets;
	}
	
	public String sliceStr()
	{
		Map<Statement,Integer> stmtToId = new HashMap();
		int count = 0;
		StringBuilder sb = new StringBuilder("Statement Graph:\n");
		for(Statement stmt : sGraph.stmts()){
			stmtToId.put(stmt, count);
			sb.append(String.format("%d: %s", count++, stmtStr(stmt)));
		}
		sb.append("Edges:\n");
		for(Statement stmt : sGraph.stmts()){
			Integer toId = stmtToId.get(stmt);
			sb.append("{");
			boolean first = true;
			for(Statement pred : sGraph.predsOf(stmt)){
				Integer fromId = stmtToId.get(pred);
				if(first)
					first = false;
				else 
					sb.append(", ");
				sb.append(fromId);
			}
			sb.append(String.format("} -> %d\n", toId));
		}
		return sb.toString();
	}

	public String stmtStr(Statement stmt)
	{
		if(stmt instanceof ToLower){
			return String.format("tolower %s %s %%%% %s\n", toStr(((Assign) stmt).left), toStr(((Assign) stmt).right), stmt.stmt);
		} else if(stmt instanceof Assign){
			return String.format("assign %s %s %%%% %s\n", toStr(((Assign) stmt).left), toStr(((Assign) stmt).right), stmt.stmt);
		} else if(stmt instanceof Concat){
			Concat concat = (Concat) stmt;
			return String.format("concat %s %s %s %%%% %s\n", toStr(concat.left), toStr(concat.right1), toStr(concat.right2), stmt.stmt);
		} else if(stmt instanceof Havoc){
			return String.format("havoc %s %%%% %s\n", toStr(((Havoc) stmt).local), stmt.stmt);
		} else if(stmt instanceof Criterion){
			return String.format("criterion %s %%%% %s\n", toStr(((Criterion) stmt).local), stmt.stmt);
		} else
			assert false;
		return null;
	}
	
	private String toStr(Immediate i)
	{
		if(i instanceof StringConstant)
			return String.format("\"%s\"", ((StringConstant) i).value);
		else{
			if(!(i instanceof Local))
				throw new RuntimeException(i+" is not local");
			Local l = (Local) i;
			SootMethod m = containerMethodFor(l);
			return String.format("%s!%s@%s", l.getName(), l.getType().toString(), m.getSignature());
		}
	}

	public static boolean isStringType(Type type)
	{
		return
			type.equals(stringType) ||
			type.equals(stringBuilderType) ||
			type.equals(stringBufferType) ||
			type.equals(charSequenceType);
	}

	public static boolean isInteresting(Immediate i)
	{
		boolean interesting;
		if(i instanceof Constant)
			interesting = i instanceof StringConstant;
		else{
			Type type = ((Local) i).getType();
			if(isStringType(type))
				interesting = true;
			else
				interesting = type.equals(objectType);
		}
		return interesting;
	}

}