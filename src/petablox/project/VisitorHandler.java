package petablox.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
import soot.Value;
import soot.ValueBox;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JBreakpointStmt;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JEnterMonitorStmt;
import soot.jimple.internal.JExitMonitorStmt;
import soot.jimple.internal.JGotoStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JLookupSwitchStmt;
import soot.jimple.internal.JNopStmt;
import soot.jimple.internal.JRetStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.internal.JTableSwitchStmt;
import soot.jimple.internal.JThrowStmt;
import soot.shimple.PhiExpr;
import soot.toolkits.graph.Block;

import petablox.program.Program;
import petablox.program.visitors.IAcqLockInstVisitor;
import petablox.program.visitors.IAssignInstVisitor;
import petablox.program.visitors.IBreakInstVisitor;
import petablox.program.visitors.ICastInstVisitor;
import petablox.program.visitors.IClassVisitor;
import petablox.program.visitors.IEnterMonitorInstVisitor;
import petablox.program.visitors.IExitMonitorInstVisitor;
import petablox.program.visitors.IExprVisitor;
import petablox.program.visitors.IFieldVisitor;
import petablox.program.visitors.IGotoInstVisitor;
import petablox.program.visitors.IHeapInstVisitor;
import petablox.program.visitors.IIdentityInstVisitor;
import petablox.program.visitors.IIfInstVisitor;
import petablox.program.visitors.IInstVisitor;
import petablox.program.visitors.IInvokeExprVisitor;
import petablox.program.visitors.IInvokeInstVisitor;
import petablox.program.visitors.ILookupSwitchInstVisitor;
import petablox.program.visitors.IMethodVisitor;
import petablox.program.visitors.IMoveInstVisitor;
import petablox.program.visitors.INewInstVisitor;
import petablox.program.visitors.INopInstVisitor;
import petablox.program.visitors.IPhiInstVisitor;
import petablox.program.visitors.IRelLockInstVisitor;
import petablox.program.visitors.IReturnInstVisitor;
import petablox.program.visitors.IReturnVoidInstVisitor;
import petablox.program.visitors.IRetInstVisitor;
import petablox.program.visitors.ITableSwitchInstVisitor;
import petablox.program.visitors.IThrowInstVisitor;
import petablox.project.ITask;
import petablox.util.IndexSet;
import petablox.util.soot.ICFG;
import petablox.util.soot.SootUtilities;

/**
 * Utility for registering and executing a set of tasks
 * as visitors over program representation.
 *
 * @author Mayur Naik (mayur.naik@intel.com)
 */
public class VisitorHandler {
    private final Collection<ITask> tasks;
    private Collection<IAssignInstVisitor> asvs;
    private Collection<IBreakInstVisitor> bpVisitors;
    private Collection<IClassVisitor> cvs;
    private Collection<IEnterMonitorInstVisitor> enterMonitorVisitors;
    private Collection<IExitMonitorInstVisitor> exitMonitorVisitors;
    private Collection<IFieldVisitor> fvs;
    private Collection<IGotoInstVisitor> gotoVisitors;
    private Collection<IIdentityInstVisitor> identityVisitors;
    private Collection<IHeapInstVisitor> hivs;
    private Collection<INewInstVisitor> nivs;
    private Collection<IIfInstVisitor> ifVisitors;
    private Collection<IInvokeInstVisitor> iivs;
    private Collection<INopInstVisitor> nopVisitors;
    private Collection<ILookupSwitchInstVisitor> lookupVisitors;
    private Collection<IReturnInstVisitor> rivs;
    private Collection<IReturnVoidInstVisitor> returnVoidVisitors;
    private Collection<IRetInstVisitor> retVisitors;
    private Collection<IThrowInstVisitor> throwVisitors;
    private Collection<IAcqLockInstVisitor> acqivs;
    private Collection<IRelLockInstVisitor> relivs;
    private Collection<ITableSwitchInstVisitor> tableVisitors;
    private Collection<IMoveInstVisitor> mivs;
    private Collection<ICastInstVisitor> civs;
    private Collection<IPhiInstVisitor> pivs;
    private Collection<IInstVisitor> ivs;
    private Collection<IExprVisitor> evs;
    private Collection<IInvokeExprVisitor> invokeExprVisitors;
    private Collection<IMethodVisitor> mvs;

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
                    if (!m.isConcrete())
                        continue;
                    ICFG cfg = SootUtilities.getCFG(m);
                    visitInsts(cfg);
                }
            }
        }
    }

    private void visitExprs(Unit q) {
        if (evs != null) {
            for (IExprVisitor ev : evs) {
                List<ValueBox> dubox = q.getUseAndDefBoxes();
                for (ValueBox vb : dubox)
                    ev.visit(vb.getValue());
                if (q instanceof JLookupSwitchStmt) {
                    JLookupSwitchStmt s = (JLookupSwitchStmt) q;
                    for (Value v : s.getLookupValues())
                        ev.visit(v);
                } else if (q instanceof JTableSwitchStmt) {
                    JTableSwitchStmt s = (JTableSwitchStmt) q;
                    for (int i = s.getLowIndex(); i <= s.getHighIndex(); i++)
                        ev.visit(IntConstant.v(i));
                }
            }
        }
        if (invokeExprVisitors != null) {
            for (IInvokeExprVisitor v : invokeExprVisitors) {
                List<ValueBox> dubox = q.getUseAndDefBoxes();
                for (ValueBox vb : dubox) {
                    Value value = vb.getValue();
                    if (value instanceof InvokeExpr)
                        v.visit((InvokeExpr) value);
                }
            }
        }
    }

    private void visitEnterMonitorInsts(JEnterMonitorStmt s) {
        if (acqivs != null) {
            for (IAcqLockInstVisitor acqiv : acqivs)
                acqiv.visitAcqLockInst((Unit) s);
        }
        if (enterMonitorVisitors != null) {
            for (IEnterMonitorInstVisitor v : enterMonitorVisitors)
                v.visit(s);
        }
    }
    
    private void visitExitMonitorInsts(JExitMonitorStmt s) {
        if (relivs != null) {
            for (IRelLockInstVisitor reliv : relivs)
                reliv.visitRelLockInst((Unit) s);
        }
        if (exitMonitorVisitors != null) {
            for (IExitMonitorInstVisitor v : exitMonitorVisitors)
                v.visit(s);
        }
    }

    private void visitIdendityInsts(JIdentityStmt s) {
        if (identityVisitors != null) {
            for (IIdentityInstVisitor v : identityVisitors)
                v.visit(s);
        }
    }
    
    private void visitGotoInsts(JGotoStmt s) {
        if (gotoVisitors != null) {
            for (IGotoInstVisitor v : gotoVisitors)
                v.visit(s);
        }
    }

    private void visitIfInsts(JIfStmt s) {
        if (ifVisitors != null) {
            for (IIfInstVisitor v : ifVisitors)
                v.visit(s);
        }
    }

    private void visitBreakInsts(JBreakpointStmt s) {
        if (bpVisitors != null) {
            for (IBreakInstVisitor v : bpVisitors)
                v.visit(s);
        }
    }

    private void visitNopInsts(JNopStmt s) {
        if (nopVisitors != null) {
            for (INopInstVisitor v : nopVisitors)
                v.visit(s);
        }
    }

    private void visitLookupSwitchInsts(JLookupSwitchStmt s) {
        if (lookupVisitors != null) {
            for (ILookupSwitchInstVisitor v : lookupVisitors)
                v.visit(s);
        }
    }

    private void visitTableSwitchInsts(JTableSwitchStmt s) {
        if (tableVisitors != null) {
            for (ITableSwitchInstVisitor v : tableVisitors)
                v.visit(s);
        }
    }

    private void visitRetInsts(JRetStmt s) {
        if (retVisitors != null) {
            for (IRetInstVisitor v : retVisitors) {
                v.visit(s);
            }
        }
    }

    private void visitReturnInsts(JReturnStmt s) {
        if (rivs != null) {
            for (IReturnInstVisitor v : rivs) {
                v.visit(s);
            }
        }
    }

    private void visitReturnVoidInsts(JReturnVoidStmt s) {
        if (returnVoidVisitors != null) {
            for (IReturnVoidInstVisitor v : returnVoidVisitors) {
                v.visit(s);
            }
        }
    }

    private void visitThrowInsts(JThrowStmt s) {
        if (throwVisitors != null) {
            for (IThrowInstVisitor v : throwVisitors)
                v.visit(s);
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
                visitExprs(q);
                if (q instanceof Stmt) {
                    if(q instanceof JAssignStmt) {
                        JAssignStmt j = (JAssignStmt) q;
                        if (asvs != null) {
                            for (IAssignInstVisitor asv : asvs)
                                asv.visit(j);
                        }
                        if ((j.rightBox.getValue()) instanceof InvokeExpr) {
                            if (iivs != null) { 
                                for (IInvokeInstVisitor iiv : iivs)
                                    iiv.visitInvokeInst(q);
                            }
                        } else if(SootUtilities.isLoadInst(j) || SootUtilities.isFieldLoad(j) ||
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
                    } else if (q instanceof JBreakpointStmt) {
                        visitBreakInsts((JBreakpointStmt) q);
                    } else if(q instanceof JEnterMonitorStmt) {
                        visitEnterMonitorInsts((JEnterMonitorStmt) q);
                    } else if(q instanceof JExitMonitorStmt) {
                        visitExitMonitorInsts((JExitMonitorStmt) q);
                    } else if (q instanceof JGotoStmt) {
                        visitGotoInsts((JGotoStmt) q);
                    } else if (q instanceof JIdentityStmt) {
                        visitIdendityInsts((JIdentityStmt) q);
                    } else if (q instanceof JIfStmt) {
                        visitIfInsts((JIfStmt) q);
                    } else if (q instanceof JInvokeStmt) {
                        if (iivs != null) {
                            for (IInvokeInstVisitor iiv : iivs) {
                                iiv.visitInvokeInst(q);
                                iiv.visit((JInvokeStmt) q);
                            }
                        }
                    } else if (q instanceof JLookupSwitchStmt) {
                        visitLookupSwitchInsts((JLookupSwitchStmt) q);
                    } else if (q instanceof JNopStmt) {
                        visitNopInsts((JNopStmt) q);
                    } else if (q instanceof JRetStmt) {
                        visitRetInsts((JRetStmt) q);
                    } else if (q instanceof JReturnStmt) {
                        visitReturnInsts((JReturnStmt) q);
                    } else if (q instanceof JReturnVoidStmt) {
                        visitReturnVoidInsts((JReturnVoidStmt) q);
                    } else if (q instanceof JTableSwitchStmt) {
                        visitTableSwitchInsts((JTableSwitchStmt) q);
                    } else if (q instanceof JThrowStmt) {
                        visitThrowInsts((JThrowStmt) q);
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
                doCFGs = true;
                if (ivs == null)
                    ivs = new ArrayList<IInstVisitor>();
                ivs.add((IInstVisitor) task);
            }
            if (task instanceof IExprVisitor) {
                doCFGs = true;
                if (evs == null)
                    evs = new ArrayList<IExprVisitor>();
                evs.add((IExprVisitor) task);
            }
            if (task instanceof IAssignInstVisitor) {
                doCFGs = true;
                if (asvs == null)
                    asvs = new ArrayList<IAssignInstVisitor>();
                asvs.add((IAssignInstVisitor) task);
            }
            if (task instanceof IBreakInstVisitor) {
                doCFGs = true;
                if (bpVisitors == null)
                    bpVisitors = new ArrayList<IBreakInstVisitor>();
                bpVisitors.add((IBreakInstVisitor) task);
            }
            if (task instanceof IEnterMonitorInstVisitor) {
                doCFGs = true;
                if (enterMonitorVisitors == null)
                    enterMonitorVisitors = new ArrayList<IEnterMonitorInstVisitor>();
                enterMonitorVisitors.add((IEnterMonitorInstVisitor) task);
            }
            if (task instanceof IExitMonitorInstVisitor) {
                doCFGs = true;
                if (exitMonitorVisitors == null)
                    exitMonitorVisitors = new ArrayList<IExitMonitorInstVisitor>();
                exitMonitorVisitors.add((IExitMonitorInstVisitor) task);
            }
            if (task instanceof IGotoInstVisitor) {
                doCFGs = true;
                if (gotoVisitors == null)
                    gotoVisitors = new ArrayList<IGotoInstVisitor>();
                gotoVisitors.add((IGotoInstVisitor) task);
            }
            if (task instanceof IIdentityInstVisitor) {
                doCFGs = true;
                if (identityVisitors == null)
                    identityVisitors = new ArrayList<IIdentityInstVisitor>();
                identityVisitors.add((IIdentityInstVisitor) task);
            }
            if (task instanceof IIfInstVisitor) {
                doCFGs = true;
                if (ifVisitors == null)
                    ifVisitors = new ArrayList<IIfInstVisitor>();
                ifVisitors.add((IIfInstVisitor) task);
            }
            if (task instanceof IHeapInstVisitor) {
                doCFGs = true;
                if (hivs == null)
                    hivs = new ArrayList<IHeapInstVisitor>();
                hivs.add((IHeapInstVisitor) task);
            }
            if (task instanceof IInvokeExprVisitor) {
                doCFGs = true;
                if (invokeExprVisitors == null)
                    invokeExprVisitors = new ArrayList<IInvokeExprVisitor>();
                invokeExprVisitors.add((IInvokeExprVisitor) task);
            }
            if (task instanceof IInvokeInstVisitor) {
                doCFGs = true;
                if (iivs == null)
                    iivs = new ArrayList<IInvokeInstVisitor>();
                iivs.add((IInvokeInstVisitor) task);
            }
            if (task instanceof INewInstVisitor) {
                doCFGs = true;
                if (nivs == null)
                    nivs = new ArrayList<INewInstVisitor>();
                nivs.add((INewInstVisitor) task);
            }
            if (task instanceof INopInstVisitor) {
                doCFGs = true;
                if (nopVisitors == null)
                    nopVisitors = new ArrayList<INopInstVisitor>();
                nopVisitors.add((INopInstVisitor) task);
            }
            if (task instanceof ILookupSwitchInstVisitor) {
                doCFGs = true;
                if (lookupVisitors == null)
                    lookupVisitors = new ArrayList<ILookupSwitchInstVisitor>();
                lookupVisitors.add((ILookupSwitchInstVisitor) task);
            }
            if (task instanceof ITableSwitchInstVisitor) {
                doCFGs = true;
                if (tableVisitors == null)
                    tableVisitors = new ArrayList<ITableSwitchInstVisitor>();
                tableVisitors.add((ITableSwitchInstVisitor) task);
            }
            if (task instanceof IMoveInstVisitor) {
                doCFGs = true;
                if (mivs == null)
                    mivs = new ArrayList<IMoveInstVisitor>();
                mivs.add((IMoveInstVisitor) task);
            }
            if (task instanceof ICastInstVisitor) {
                doCFGs = true;
                if (civs == null)
                    civs = new ArrayList<ICastInstVisitor>();
                civs.add((ICastInstVisitor) task);
            }
            if (task instanceof IPhiInstVisitor) {
                doCFGs = true;
                if (pivs == null)
                    pivs = new ArrayList<IPhiInstVisitor>();
                pivs.add((IPhiInstVisitor) task);
            }
            if (task instanceof IRetInstVisitor) {
                doCFGs = true;
                if (retVisitors == null)
                    retVisitors = new ArrayList<IRetInstVisitor>();
                retVisitors.add((IRetInstVisitor) task);
            }
            if (task instanceof IReturnInstVisitor) {
                doCFGs = true;
                if (rivs == null)
                    rivs = new ArrayList<IReturnInstVisitor>();
                rivs.add((IReturnInstVisitor) task);
            }
            if (task instanceof IReturnVoidInstVisitor) {
                doCFGs = true;
                if (returnVoidVisitors == null)
                    returnVoidVisitors = new ArrayList<IReturnVoidInstVisitor>();
                returnVoidVisitors.add((IReturnVoidInstVisitor) task);
            }
            if (task instanceof IAcqLockInstVisitor) {
                doCFGs = true;
                if (acqivs == null)
                    acqivs = new ArrayList<IAcqLockInstVisitor>();
                acqivs.add((IAcqLockInstVisitor) task);
            }
            if (task instanceof IRelLockInstVisitor) {
                doCFGs = true;
                if (relivs == null)
                    relivs = new ArrayList<IRelLockInstVisitor>();
                relivs.add((IRelLockInstVisitor) task);
            }
            if (task instanceof IThrowInstVisitor){
                doCFGs = true;
                if (throwVisitors == null)
                    throwVisitors = new ArrayList<IThrowInstVisitor>();
                throwVisitors.add((IThrowInstVisitor) task);
            }
        }
        Program program = Program.g();
        reachableMethods = program.getMethods();
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
