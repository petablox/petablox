package petablox.util.soot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.SootMethod;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.ValueBox;
import soot.jimple.IdentityStmt;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.StmtBox;
import soot.shimple.PhiExpr;
import soot.shimple.Shimple;
import soot.shimple.ShimpleBody;
import soot.toolkits.scalar.ValueUnitPair;

public class SSAUtilities {
	enum SSAKind {
		NONE, PHI, NO_PHI, NO_MOVE, NO_MOVE_PHI
	};
	public static SSAKind ssaKind = SSAKind.NONE;
	
	public static void doSSA(boolean hasPhi, boolean noMove){
		if (hasPhi) {
			ssaKind = noMove ? SSAKind.NO_MOVE_PHI : SSAKind.PHI;
		} else {
			ssaKind = noMove ? SSAKind.NO_MOVE : SSAKind.NO_PHI;
		}
	}
	
	public static void process(SootMethod m) {
		switch (ssaKind) {
		case NONE:
			break;
		case PHI:
			installShimpleBody(m);
			break;
		case NO_PHI:
			installShimpleBody(m);
			removePhis(m);
			break;
		case NO_MOVE:
			installShimpleBody(m);
			removeMoves(m);
			break;
		case NO_MOVE_PHI:
			installShimpleBody(m);
			removeMoves(m);
			removePhis(m);
			break;
		default:
			break;
		}
		return;
	}
	
	public static void installShimpleBody(SootMethod m) {
		Body b = m.retrieveActiveBody();
		ShimpleBody sb = Shimple.v().newBody(b);
		m.setActiveBody(sb);
		return;
	}
	
	public static void removeMoves(SootMethod m) {
		List<Unit> moves = new ArrayList<Unit>();
		Body b = m.retrieveActiveBody();
		PatchingChain<Unit> upc = b.getUnits();
		Iterator<Unit> uit = upc.iterator();
		while (uit.hasNext()) {
			Unit u = (Unit)uit.next();
			if (!(u instanceof IdentityStmt) && (u instanceof JAssignStmt) && SootUtilities.isMoveInst((JAssignStmt)u))
				moves.add(u);		
		}
		
		for (int i = 0; i < moves.size(); i++) {
			JAssignStmt curr = (JAssignStmt)moves.get(i);
			Local left = (Local)curr.leftBox.getValue();
			Local right = (Local)curr.rightBox.getValue();
			Unit currSucc = upc.getSuccOf(curr);
				moveLabel(m, curr, currSucc);
				upc.remove(curr);
				List<ValueBox> useList = b.getUseBoxes();
				for (int j = 0; j < useList.size(); j++) {
					if (useList.get(j).getValue() == left)
						useList.get(j).setValue(right);
				}
		}	
	}
	
	public static void removePhis(SootMethod m) {
		List<Unit> phis = new ArrayList<Unit>();
		Body b = m.retrieveActiveBody();
		PatchingChain<Unit> upc = b.getUnits();
		Iterator<Unit> uit = upc.iterator();
		while (uit.hasNext()) {
			Unit u = (Unit)uit.next();
			if (!(u instanceof IdentityStmt) && (u instanceof JAssignStmt) && SootUtilities.isPhiInst((JAssignStmt)u))
				phis.add(u);		
		}
		for (int i = 0; i < phis.size(); i++){
			Unit phiStmt = phis.get(i);
			PhiExpr currPhi = (PhiExpr) ((JAssignStmt)phiStmt).rightBox.getValue();
			Value lhs = ((JAssignStmt)phis.get(i)).leftBox.getValue();
			int phiArgCnt = currPhi.getArgCount();
			List<ValueUnitPair> phiArgs = currPhi.getArgs();
			for (int j = 0; j < phiArgCnt; j++) {
				ValueUnitPair vu = phiArgs.get(j);
				Value rhs = vu.getValue();
				if (!((Local)lhs).getName().equals(((Local)rhs).getName())) {
					Unit u = vu.getUnit();
					Unit newMv = Jimple.v().newAssignStmt(lhs, rhs);
					if (SootUtilities.isBranch(u)) {
						upc.insertBefore(newMv, u);
						moveLabel(m, u, newMv);
					} else {	
						upc.insertAfter(newMv, u);
					}
				}
			}
			Unit phiSucc = upc.getSuccOf(phiStmt);
			moveLabel(m, phiStmt, phiSucc);
			upc.remove(phiStmt);
		}
	}
	
	private static void moveLabel(SootMethod m, Unit from, Unit to) {
		Body b = m.retrieveActiveBody();
		PatchingChain<Unit> upc = b.getUnits();
		Iterator<Unit> uit = upc.iterator();
		while (uit.hasNext()) {
			Unit u = uit.next();
			if (SootUtilities.isBranch(u)) {
				Iterator<UnitBox> ubIt = u.getUnitBoxes().iterator();
			    while (ubIt.hasNext())
			    {
			        StmtBox tb = (StmtBox)ubIt.next();
			        Stmt targ = (Stmt)tb.getUnit();
			        if (targ == from)
			            tb.setUnit(to);
			    }
			} else if ((u instanceof JAssignStmt) && SootUtilities.isPhiInst((JAssignStmt)u)) {
				PhiExpr currPhi = (PhiExpr) ((JAssignStmt)u).rightBox.getValue();
				List<ValueUnitPair> phiArgs = currPhi.getArgs();
				for (int j = 0; j < phiArgs.size(); j++) {
					ValueUnitPair vu = phiArgs.get(j);
					Unit pred = vu.getUnit();
					if (pred == from)
						currPhi.setPred(j, to);
				}
			}
		}
		return;
	}
}
