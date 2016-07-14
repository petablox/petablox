package stamp.analyses.inferaliasmodel;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.Local;
import soot.Type;
import soot.RefLikeType;
import soot.jimple.Stmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.AnyNewExpr;
import soot.jimple.NewExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.tagkit.Tag;
import soot.tagkit.BytecodeOffsetTag;
import soot.util.ArrayNumberer;
import soot.util.Chain;

import stamp.analyses.IdentifyCallbackMethods;
import shord.project.analyses.JavaAnalysis;
import shord.program.Program;

import java.io.*;
import java.util.*;

import chord.project.Chord;
/*
 * @author Saswat Anand
 */
@Chord(name = "preinst")
public class PreInstrumentationAnalysis extends JavaAnalysis
{
	private int methIndex;
	private String methSig;
	private SootMethod meth;
	private Chain<Unit> units;
	private MatchAllocToInitAnalysis invkInitAnalysis;
	private CallGraph callGraph;
	private PrintWriter instrInfoWriter;
	private int eventId = 0;

	public enum EventType { METHCALLARG, METHPARAM };

	public void run()
	{
		try{
			String stampOutDir = System.getProperty("stamp.out.dir");
			
			File outDir = new File(stampOutDir, "inferaliasmodel");
			outDir.mkdirs();
			
			File instrInfoFile = new File(outDir, "instrinfo.txt");
			instrInfoWriter = new PrintWriter(new BufferedWriter(new FileWriter(instrInfoFile)));

			Program prog = Program.g();
			prog.runCHA();

			IdentifyCallbackMethods cbAnalysis = new IdentifyCallbackMethods();
			cbAnalysis.analyze();
			Set<SootMethod> callbacks = cbAnalysis.allCallbacks();
			
			File methodsInfoFile = new File(outDir, "methods.txt");
			PrintWriter methodInfoWriter = new PrintWriter(new BufferedWriter(new FileWriter(methodsInfoFile)));
			callGraph = Scene.v().getCallGraph();
			ArrayNumberer<SootMethod> methNumberer = Scene.v().getMethodNumberer();
			for(SootClass klass : prog.getClasses()){
				if(prog.isFrameworkClass(klass))
					continue;
				String className = klass.getName();
				if(className.startsWith("stamp.harness.") || className.equals("android.view.StampLayoutInflater"))
					continue;
				for(SootMethod method : klass.getMethods()){
					if(!method.isConcrete())
						continue;
					
					this.meth = method;
					this.methSig = bcSig(method);
					this.methIndex = (int) methNumberer.get(method);
					this.units = method.retrieveActiveBody().getUnits();
					this.invkInitAnalysis = null;

					methodInfoWriter.println(methIndex +" "+method.getSignature());
					System.out.println("preinst: "+method.getSignature());
					process(methIndex);

					if(callbacks.contains(method)){
						assert !method.isStatic();
						instrInfoWriter.println(EventType.METHPARAM+" "+methSig+" "+"0"+" "+eventId+" "+methIndex);
						eventId++;
						int paramIndex = 1;
						for(Type paramType : method.getParameterTypes()){
							if(paramType instanceof RefLikeType){
								instrInfoWriter.println(EventType.METHPARAM+" "+methSig+" "+paramIndex+" "+eventId+" "+methIndex);
								eventId++;
							}
							paramIndex++;
						}
					}
				}
			}
			
			methodInfoWriter.close();
			instrInfoWriter.close();
		}catch(IOException e){
			throw new Error(e);
		}
	}
	
	private void process(int methIndex)
	{


		Iterator<Unit> uit = units.snapshotIterator();
		int stmtIndex = 0;
		while(uit.hasNext()){
			Stmt stmt = (Stmt) uit.next();
			processNewStmt(stmt, stmtIndex);
			processInvkStmt(stmt, stmtIndex);
			stmtIndex++;
		}
	}

	private void processNewStmt(Stmt stmt, int stmtIndex)
	{
		if(!(stmt instanceof DefinitionStmt))
			return;
		Value leftOp = ((DefinitionStmt) stmt).getLeftOp();
		Value rightOp = ((DefinitionStmt) stmt).getRightOp();
		if(!(leftOp instanceof Local) || !(rightOp instanceof AnyNewExpr))
			return;
		if(rightOp instanceof NewExpr){
			String type = rightOp.getType().toString();
			if(type.equals("java.lang.String") || type.equals("java.lang.StringBuffer") || type.equals("java.lang.StringBuilder"))
				return;
			if(invkInitAnalysis == null)
				invkInitAnalysis = new MatchAllocToInitAnalysis(meth.retrieveActiveBody());
			int argIndex = 0;
			for(Stmt initInvkStmt : invkInitAnalysis.invokeInitStmtsFor(stmt)){
				int bytecodeOffset = bco(initInvkStmt);
				if(bytecodeOffset >= 0){
					instrInfoWriter.println(EventType.METHCALLARG+" "+methSig+" "+bytecodeOffset+" "+argIndex+" "+eventId+" "+methIndex+" "+stmtIndex);
					eventId++;
				} else
					System.out.println("bco unavailable: "+meth.getSignature());
			}
		} else {
			//TODO: handle array allocation sites
		}
	}

	private void processInvkStmt(Stmt stmt, int stmtIndex)
	{
		if(!stmt.containsInvokeExpr())
			return;

		if(!(stmt instanceof DefinitionStmt))
			return;

		Type retType = ((DefinitionStmt) stmt).getLeftOp().getType();
		if(!(retType instanceof RefLikeType))
			return;

		String retTypeStr = retType.toString();
		if(retTypeStr.equals("java.lang.String") || retTypeStr.equals("java.lang.StringBuffer") || retTypeStr.equals("java.lang.StringBuilder"))
			return;

		Iterator<Edge> edgeIt = callGraph.edgesOutOf(stmt);
		boolean instrument = false;
		while(edgeIt.hasNext()){
			SootMethod target = (SootMethod) edgeIt.next().getTgt();
			SootClass tgtClass = target.getDeclaringClass();
			String tgtClassName = tgtClass.getName();
			if(tgtClassName.startsWith("java.lang."))
				continue;
			if(Program.g().isFrameworkClass(tgtClass)){
				instrument = true;
				break;
			}
		}
		if(!instrument)
			return;

		int argIndex = -1;
		int bytecodeOffset = bco(stmt);
		if(bytecodeOffset >= 0){
			instrInfoWriter.println(EventType.METHCALLARG+" "+methSig+" "+bytecodeOffset+" "+argIndex+" "+eventId+" "+methIndex+" "+stmtIndex);
			eventId++;
		} else
			System.out.println("bco unavailable: "+meth.getSignature());
	}
		
	int bco(Stmt stmt)
	{
		int bytecodeOffset = -1;
		for(Tag tag : stmt.getTags()){
			if(tag instanceof BytecodeOffsetTag){
				bytecodeOffset = ((BytecodeOffsetTag) tag).getBytecodeOffset();
				break;
			}
		}
		return bytecodeOffset;
	}

	String bcSig(SootMethod method)
	{
		String sig = method.getBytecodeSignature();
		sig = sig.substring(sig.indexOf(' ')+1, sig.length()-1)+"@L"+method.getDeclaringClass().getName().replace('.', '/')+";";
		return sig;
	}
}