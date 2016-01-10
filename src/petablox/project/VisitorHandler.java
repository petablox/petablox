package petablox.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import petablox.program.Program;
import petablox.program.visitors.IAcqLockInstVisitor;
import petablox.program.visitors.ICastInstVisitor;
import petablox.program.visitors.IClassVisitor;
import petablox.program.visitors.IFieldVisitor;
import petablox.program.visitors.IHeapInstVisitor;
import petablox.program.visitors.IInstVisitor;
import petablox.program.visitors.IInvokeInstVisitor;
import petablox.program.visitors.IMethodVisitor;
import petablox.program.visitors.IMoveInstVisitor;
import petablox.program.visitors.INewInstVisitor;
import petablox.program.visitors.IPhiInstVisitor;
import petablox.program.visitors.IRelLockInstVisitor;
import petablox.program.visitors.IReturnInstVisitor;
import petablox.project.ITask;
import petablox.util.IndexSet;
import petablox.util.soot.ICFG;
import petablox.util.soot.SootUtilities;
import soot.ArrayType;
import soot.Local;
import soot.NullType;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootResolver;
import soot.Type;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JEnterMonitorStmt;
import soot.jimple.internal.JExitMonitorStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.internal.JThrowStmt;
import soot.shimple.PhiExpr;
import soot.toolkits.graph.Block;

/**
 * Utility for registering and executing a set of tasks
 * as visitors over program representation.
 *
 * @author Mayur Naik (mayur.naik@intel.com)
 */
public class VisitorHandler {
    private final Collection<ITask> tasks;
    private Collection<IClassVisitor> cvs;
    private Collection<IFieldVisitor> fvs;
    private Collection<IMethodVisitor> mvs;
    private Collection<IHeapInstVisitor> hivs;
    private Collection<INewInstVisitor> nivs;
    private Collection<IInvokeInstVisitor> iivs;
    private Collection<IReturnInstVisitor> rivs;
    private Collection<IAcqLockInstVisitor> acqivs;
    private Collection<IRelLockInstVisitor> relivs;
    private Collection<IMoveInstVisitor> mivs;
    private Collection<ICastInstVisitor> civs;
    private Collection<IPhiInstVisitor> pivs;
    private Collection<IInstVisitor> ivs;
    private boolean doCFGs;
    public VisitorHandler(ITask task) {
        tasks = new ArrayList<ITask>(1);
        tasks.add(task);
    }
    public VisitorHandler(Collection<ITask> tasks) {
        this.tasks = tasks;
    }
    private void visitFields(SootClass c) {
    	if(c.resolvingLevel()!=SootClass.BODIES)
    		SootResolver.v().reResolve(c, SootClass.BODIES);
        for (Object o : c.getFields()) {
            if (o instanceof SootField) {
                SootField f = (SootField) o;
                for (IFieldVisitor fv : fvs)
                    fv.visit(f);
            }
        }
    }
    private void visitMethods(SootClass c) {
    	if(c.resolvingLevel()!=SootClass.BODIES)
    		Scene.v().forceResolve(c.getName(), SootClass.BODIES);
        for (Object o : c.getMethods()) {
            if (o instanceof SootMethod) {
                SootMethod m = (SootMethod) o;
                if (!reachableMethods.contains(m))
                    continue;
                for (IMethodVisitor mv : mvs) {
                    mv.visit(m);
                    if (!doCFGs)
                        continue;
                    if (m.isAbstract())
                        continue;
                    ICFG cfg = SootUtilities.getCFG(m);
                    visitInsts(cfg);
                }
            }
        }
    }
    private void visitInsts(ICFG cfg) {
        for (Block bb : cfg.reversePostOrder()) {
        	Iterator<Unit> uit = bb.iterator();
            while(uit.hasNext()){
            	Unit q = uit.next();
                if (ivs != null) {
                    for (IInstVisitor iv : ivs)
                        iv.visit(q);
                }
                if (q instanceof Stmt) {
                    if (SootUtilities.isInvoke(q) && iivs != null) {
                        for (IInvokeInstVisitor iiv : iivs)
                            iiv.visitInvokeInst(q);
                    }else if(q instanceof JAssignStmt){
                    	JAssignStmt j =(JAssignStmt)q;
                    	if(SootUtilities.isLoadInst(j) || SootUtilities.isFieldLoad(j) ||
                    			SootUtilities.isStoreInst(j) || SootUtilities.isFieldStore(j) ||
                    			SootUtilities.isStaticGet(j) || SootUtilities.isStaticPut(j)){
                    		if (hivs != null) {
                                for (IHeapInstVisitor hiv : hivs)
                                    hiv.visitHeapInst(q);
                            }
                    	} else if(SootUtilities.isNewStmt(j) || SootUtilities.isNewArrayStmt(j)
                    			  || SootUtilities.isNewMultiArrayStmt(j)){
                    		if (nivs != null) {
                                for (INewInstVisitor niv : nivs)
                                    niv.visitNewInst(q);
                            }
                    	}else if(j.leftBox.getValue() instanceof Local &&
                    			j.rightBox.getValue() instanceof Local){
                    		if (mivs != null) {
                                for (IMoveInstVisitor miv : mivs)
                                    miv.visitMoveInst(q);
                            }
                    	} else if(j.rightBox.getValue() instanceof JCastExpr){
                    		if (civs != null) {
                                for (ICastInstVisitor civ : civs)
                                    civ.visitCastInst(q);
                            }
                    	} else if(j.rightBox.getValue() instanceof PhiExpr){
                    		if (pivs != null) {
                                for (IPhiInstVisitor piv : pivs)
                                    piv.visitPhiInst(q);
                            }
                    	}
                    } else if(q instanceof JReturnStmt || q instanceof JReturnVoidStmt
                    		&& !(q instanceof JThrowStmt)){
                    	if (rivs != null) {
                            for (IReturnInstVisitor riv : rivs)
                                riv.visitReturnInst(q);
                        }
                    } else if(q instanceof JEnterMonitorStmt){
                    	if (acqivs != null) {
                            for (IAcqLockInstVisitor acqiv : acqivs)
                                acqiv.visitAcqLockInst(q);
                        }
                    } else if(q instanceof JExitMonitorStmt){
                    	if (relivs != null) {
                            for (IRelLockInstVisitor reliv : relivs)
                                reliv.visitRelLockInst(q);
                        }
                    }
                }
            }
        }
    }
    private IndexSet<SootMethod> reachableMethods;

    public void visitProgram() {
        for (ITask task : tasks) {
            if (task instanceof IClassVisitor) {
                if (cvs == null)
                    cvs = new ArrayList<IClassVisitor>();
                cvs.add((IClassVisitor) task);
            }
            if (task instanceof IFieldVisitor) {
                if (fvs == null)
                    fvs = new ArrayList<IFieldVisitor>();
                fvs.add((IFieldVisitor) task);
            }
            if (task instanceof IMethodVisitor) {
                if (mvs == null)
                    mvs = new ArrayList<IMethodVisitor>();
                mvs.add((IMethodVisitor) task);
            }
            if (task instanceof IInstVisitor) {
                if (ivs == null)
                    ivs = new ArrayList<IInstVisitor>();
                ivs.add((IInstVisitor) task);
            }
            if (task instanceof IHeapInstVisitor) {
                if (hivs == null)
                    hivs = new ArrayList<IHeapInstVisitor>();
                hivs.add((IHeapInstVisitor) task);
            }
            if (task instanceof IInvokeInstVisitor) {
                if (iivs == null)
                    iivs = new ArrayList<IInvokeInstVisitor>();
                iivs.add((IInvokeInstVisitor) task);
            }
            if (task instanceof INewInstVisitor) {
                if (nivs == null)
                    nivs = new ArrayList<INewInstVisitor>();
                nivs.add((INewInstVisitor) task);
            }
            if (task instanceof IMoveInstVisitor) {
                if (mivs == null)
                    mivs = new ArrayList<IMoveInstVisitor>();
                mivs.add((IMoveInstVisitor) task);
            }
            if (task instanceof ICastInstVisitor) {
                if (civs == null)
                    civs = new ArrayList<ICastInstVisitor>();
                civs.add((ICastInstVisitor) task);
            }
            if (task instanceof IPhiInstVisitor) {
                if (pivs == null)
                    pivs = new ArrayList<IPhiInstVisitor>();
                pivs.add((IPhiInstVisitor) task);
            }
            if (task instanceof IReturnInstVisitor) {
                if (rivs == null)
                    rivs = new ArrayList<IReturnInstVisitor>();
                rivs.add((IReturnInstVisitor) task);
            }
            if (task instanceof IAcqLockInstVisitor) {
                if (acqivs == null)
                    acqivs = new ArrayList<IAcqLockInstVisitor>();
                acqivs.add((IAcqLockInstVisitor) task);
            }
            if (task instanceof IRelLockInstVisitor) {
                if (relivs == null)
                    relivs = new ArrayList<IRelLockInstVisitor>();
                relivs.add((IRelLockInstVisitor) task);
            }
        }
        Program program = Program.g();
        reachableMethods = program.getMethods();
        doCFGs = (ivs != null) || (hivs != null) ||
            (iivs != null) || (nivs != null) || (mivs != null) ||
            (civs != null) || (pivs != null) || (rivs != null) ||
            (acqivs != null) || (relivs != null);
        if (cvs != null) {
            IndexSet<RefLikeType> classes = program.getClasses();
            for (Type r : classes) {
                if (r instanceof ArrayType || r instanceof NullType)
                    continue;
                SootClass c =  ((RefType)r).getSootClass();
                for (IClassVisitor cv : cvs)
                    cv.visit(c);
                if (fvs != null)
                    visitFields(c);
                if (mvs != null)
                    visitMethods(c);
            }
        }
        
    }
}
