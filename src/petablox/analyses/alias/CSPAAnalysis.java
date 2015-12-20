package petablox.analyses.alias;

import java.util.Set;
import java.util.HashSet;

import soot.Local;
import soot.SootField;
import petablox.bddbddb.Rel.RelView;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;
import petablox.util.SetUtils;

/**
 * Context-sensitive points-to analysis.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "cs-alias-java"
)
public class CSPAAnalysis extends JavaAnalysis {
    private ProgramRel relCVC;
    private ProgramRel relFC;
    private ProgramRel relCFC;
    public void run() {
        relCVC = (ProgramRel) ClassicProject.g().getTrgt("CVC");
        relFC  = (ProgramRel) ClassicProject.g().getTrgt("FC");
        relCFC = (ProgramRel) ClassicProject.g().getTrgt("CFC");
    }
    /**
     * Provides the abstract object to which a given local variable
     * may point in a given abstract context of its declaring method.
     * 
     * @param    ctxt    An abstract context of a method.
     * @param    var        A local variable declared in the method.
     * 
     * @return    The abstract object to which the given local variable
     *          may point in the given abstract context.
     */
    public CSObj pointsTo(Ctxt ctxt, Local var) {
        if (!relCVC.isOpen())
            relCVC.load();
        RelView view = relCVC.getView();
        view.selectAndDelete(0, ctxt);
        view.selectAndDelete(1, var);
        Iterable<Ctxt> res = view.getAry1ValTuples();
        Set<Ctxt> pts = SetUtils.iterableToSet(
                res, view.size());
        view.free();
        return new CSObj(pts);
    }
    /**
     * Provides the abstract object to which a given static field
     * may point.
     * 
     * @param    field    A static field.

     * @return    The abstract object to which the given static field
     *             may point.
     */
    public CSObj pointsTo(SootField field) {
        if (!relFC.isOpen())
            relFC.load();
        RelView view = relFC.getView();
        view.selectAndDelete(0, field);
        Iterable<Ctxt> res = view.getAry1ValTuples();
        Set<Ctxt> pts = SetUtils.iterableToSet(
                res, view.size());
        view.free();
        return new CSObj(pts);
    }
    /**
     * Provides the abstract object to which a given instance field
     * of a given abstract object may point.
     * 
     * @param    obj        An abstract object.
     * @param    field    An instance field.
     * 
     * @return    The abstract object to which the given instance field
     *             of the given abstract object may point.
     */
    public CSObj pointsTo(CSObj obj, SootField field) {
        if (!relCFC.isOpen())
            relCFC.load();
        Set<Ctxt> pts = new HashSet<Ctxt>();
        for (Ctxt ctxt : obj.pts) {
            RelView view = relCFC.getView();
            view.selectAndDelete(0, ctxt);
            view.selectAndDelete(1, field);
            Iterable<Ctxt> res = view.getAry1ValTuples();
            for (Ctxt ctxt2 : res)
                pts.add(ctxt2);
            view.free();
        }
        return new CSObj(pts);
    }
    /**
     * Frees relations used by this program analysis if they are in
     * memory.
     * <p>
     * This method must be called after clients are done exercising
     * the interface of this analysis.
     */
    public void free() {
        if (relCVC.isOpen())
            relCVC.close();
        if (relFC.isOpen())
            relFC.close();
        if (relCFC.isOpen())
            relCFC.close();
    }
}
