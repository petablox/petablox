package petablox.android.srcmap.sourceinfo.abstractinfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Local;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.LengthExpr;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import petablox.android.srcmap.Expr;
import petablox.android.srcmap.InvkMarker;
import petablox.android.srcmap.Marker;
import petablox.android.srcmap.SimpleMarker;
import petablox.android.srcmap.sourceinfo.MethodInfo;
import petablox.android.srcmap.sourceinfo.RegisterMap;
import petablox.android.srcmap.sourceinfo.SourceInfo;

/**
 * @author Saswat Anand 
 */
public class DefaultRegisterMap implements RegisterMap {
    private Map<Local,Set<Expr>> varToExprs;
    private SourceInfo sourceInfo;

	public DefaultRegisterMap(SourceInfo sourceInfo, SootMethod meth, MethodInfo mi) {
		this.sourceInfo = sourceInfo;
		if(meth.isConcrete() && mi != null){
			//System.out.println("=== registermap " + meth);
			//for(InvkExpr ie : mi.invkExprs()){
			//	System.out.println(ie.text());
			//}
			//System.out.println("===");

			varToExprs = new HashMap();

            Visitor visitor = new Visitor(mi);
			for(Unit u : meth.retrieveActiveBody().getUnits()){
				Stmt stmt = (Stmt) u;
				visitor.handleStmt(stmt);
			}
		}
	}
	
	public Set<Expr> srcLocsFor(Local var) {
        if(varToExprs == null)
            return null;
        Set<Expr> result = varToExprs.get(var);
        return result;
	}

	public Map<Local,Set<Expr>> allSrcLocs() {
		return varToExprs;
	}

	private class Visitor {
		private MethodInfo mi;
		private List<Marker> paramMarkers;

		Visitor(MethodInfo mi2) {
			this.mi = mi2;
			this.paramMarkers = mi2.markers(-1, "param", null);
		}
		
		private void addSrcLoc(Value v, Expr srcLoc) {
			if(!(v instanceof Local))
				return;
			if(srcLoc == null)
				return;
			Local l = (Local) v;
			Set<Expr> srcLocs = varToExprs.get(l);
			if(srcLocs == null){
				srcLocs = new HashSet();
				varToExprs.put(l, srcLocs);
			}
			//System.out.println("register " + v.toString() + " " + srcLoc.text());
			srcLocs.add(srcLoc);
		}

		private Marker lookupMarker(int lineNum, String markerType, String calleeSig) {
			//System.out.println("Query: line = " + lineNum + " chordSig = " + calleeSig + " index = " + index);
			Marker marker = null;
			for(Marker m : mi.markers(lineNum, markerType, calleeSig)){
				//System.out.println("** " + marker);
				if(marker == null)
					marker = m;
				else{
					//at least two matching markers
					System.out.println("Multiple markers");
					return null;
				}
			}
			return marker;
		}

		private void map(int lineNum, Value v, String markerType, String chordSig) {
			if(!(v instanceof Local))
				return;
			Marker marker = lookupMarker(lineNum, markerType, chordSig);
			if(marker == null)
				return;
			addSrcLoc((Local) v, ((SimpleMarker) marker).operand());
		}
	
		void handleStmt(Stmt s) {
			int lineNum = sourceInfo.stmtLineNum(s);

			if(s.containsInvokeExpr()){
				InvokeExpr ie = s.getInvokeExpr();
				String calleeSig = sourceInfo.chordSigFor(ie.getMethod());

                InvkMarker marker = (InvkMarker) lookupMarker(lineNum, "invoke", calleeSig);
                if(marker != null){
					//handle receiver
					int j = 0;
					if(ie instanceof InstanceInvokeExpr){
						InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
						addSrcLoc(iie.getBase(), marker.getArg(j));
						j++;
					}
				
					//handle args
					int numArgs = ie.getArgCount();
					for(int i = 0; i < numArgs; i++,j++){
						addSrcLoc(ie.getArg(i), marker.getArg(j));
					}
				}
				
				//return value
				if(s instanceof AssignStmt){
					map(lineNum, ((AssignStmt) s).getLeftOp(), "invk.lhs", calleeSig);
				}
			} else if(s.containsFieldRef()){
				AssignStmt as = (AssignStmt) s;
				Value leftOp = as.getLeftOp();
				FieldRef fr = s.getFieldRef();
				SootField field = fr.getField();
				String fieldSig = sourceInfo.chordSigFor(field);
				if(fr instanceof InstanceFieldRef){
					map(lineNum, ((InstanceFieldRef) fr).getBase(), "fieldexpr", fieldSig);
				}
				if(leftOp instanceof Local){
					//load
					if(field.isStatic()){
						//TODO
					} else{						
						//android google magic school bus octonauts
                        map(lineNum, leftOp, "load.lhs", fieldSig);
					}
				}else{
					//store
				}
			} else if(s.containsArrayRef()) {
				AssignStmt as = (AssignStmt) s;
				Value leftOp = as.getLeftOp();
				ArrayRef ar = s.getArrayRef();
				if(leftOp instanceof Local){
					//array read
					map(lineNum, leftOp, "aload.lhs", sourceInfo.chordTypeFor(ar.getBase().getType()));
				}else{
					//array write
					//TODO
				}
			} else if(s instanceof AssignStmt) {
				AssignStmt as = (AssignStmt) s;
				Value leftOp = as.getLeftOp();
				Value rightOp = as.getRightOp();

				if(rightOp instanceof LengthExpr){
					Value base = ((LengthExpr) rightOp).getOp();
					map(lineNum, base, "arraylen", sourceInfo.chordTypeFor(base.getType()));
				}
			} else if(s instanceof ReturnStmt){
				map(lineNum, ((ReturnStmt) s).getOp(), "return", null);
			} else if(s instanceof IdentityStmt){
				IdentityStmt is = (IdentityStmt) s;
				Local leftOp = (Local) is.getLeftOp();
				Value rightOp = is.getRightOp();
				if(rightOp instanceof ParameterRef){
					try {
						int index = ((ParameterRef) rightOp).getIndex();
						Expr expr = ((SimpleMarker) paramMarkers.get(index)).operand();
						addSrcLoc(leftOp, expr);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			} else if(s instanceof TableSwitchStmt){
				map(lineNum, ((TableSwitchStmt) s).getKey(), "switch", null);
			} else if(s instanceof LookupSwitchStmt){
				map(lineNum, ((LookupSwitchStmt) s).getKey(), "switch", null);
			}
		}

	}
}
