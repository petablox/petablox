package chord.analyses.alias;

import java.util.HashSet;
import java.util.Set;

import joeq.Class.jq_Field;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.bddbddb.Rel.PairIterable;
import chord.bddbddb.Rel.RelView;
import chord.bddbddb.Rel.TrioIterable;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;

import chord.util.SetUtils;
import chord.util.graph.MutableLabeledGraph;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

/**
 * Context-insensitive points-to analysis.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "cipa-java",
    consumes = { "VH", "FH", "HFH" }
)
public class CIPAAnalysis extends JavaAnalysis {
    private ProgramRel relVH;
    private ProgramRel relFH;
    private ProgramRel relHFH;
    private MutableLabeledGraph<Object, Object> graphedHeap = null;
    
    public void run() {
        relVH  = (ProgramRel) ClassicProject.g().getTrgt("VH");
        relFH  = (ProgramRel) ClassicProject.g().getTrgt("FH");
        relHFH = (ProgramRel) ClassicProject.g().getTrgt("HFH");
    }
    /**
     * Provides the abstract object to which a given local variable may point.
     * 
     * @param var A local variable.
     * 
     * @return The abstract object to which the given local variable may point.
     */
    public CIObj pointsTo(Register var) {
        if (!relVH.isOpen())
            relVH.load();
        RelView view = relVH.getView();
        view.selectAndDelete(0, var);
        Iterable<Quad> res = view.getAry1ValTuples();
        Set<Quad> pts = SetUtils.newSet(view.size());
        for (Quad inst : res)
            pts.add(inst);
        view.free();
        return new CIObj(pts);
    }
    /**
     * Provides the abstract object to which a given static field may point.
     * 
     * @param field A static field.
     * 
     * @return The abstract object to which the given static field may point.
     */
    public CIObj pointsTo(jq_Field field) {
        if (!relFH.isOpen())
            relFH.load();
        RelView view = relFH.getView();
        view.selectAndDelete(0, field);
        Iterable<Quad> res = view.getAry1ValTuples();
        Set<Quad> pts = SetUtils.newSet(view.size());
        for (Quad inst : res)
            pts.add(inst);
        view.free();
        return new CIObj(pts);
    }
    /**
     * Provides the abstract object to which a given instance field of a given abstract object may point.
     * 
     * @param obj   An abstract object.
     * @param field An instance field.
     * 
     * @return The abstract object to which the given instance field of the given abstract object may point.
     */
    public CIObj pointsTo(CIObj obj, jq_Field field) {
        if (!relHFH.isOpen())
            relHFH.load();
        Set<Quad> pts = new HashSet<Quad>();
        for (Quad site : obj.pts) {
            RelView view = relHFH.getView();
            view.selectAndDelete(0, site);
            view.selectAndDelete(1, field);
            Iterable<Quad> res = view.getAry1ValTuples();
            for (Quad inst : res)
                pts.add(inst);
            view.free();
        }
        return new CIObj(pts);
    }
    
    public boolean doesAliasExist(Quad q){
    	int aliases = 0;
    	{	
    		if (!relFH.isOpen())
    			relFH.load();
    		RelView view = relFH.getView();
    		view.selectAndDelete(1, q);
    		aliases = view.size();
    		view.free();
    		if(aliases > 1) return true;
    	}
    	{
    		if (!relVH.isOpen())
    			relVH.load();
    		RelView view = relVH.getView();
    		view.selectAndDelete(1, q);
    		aliases += view.size();
    		view.free();
    		if(aliases > 1) return true;
    	}
    	{
    		if (!relHFH.isOpen())
    			relHFH.load();
    		RelView view = relHFH.getView();
    		view.selectAndDelete(2, q);
    		aliases += view.size();
    		view.free();
    		if(aliases > 1) return true;
    	}
    	
    	return false;
    }
    
    /**
     * Generates a mutable labeled graph from the VH, FH & HFH relations
     * @return Labeled heap graph
     */
    public MutableLabeledGraph<Object, Object> getGraphedHeap(){
    	if(graphedHeap == null)
    		graphHeap();
    		
    	return graphedHeap;
    }
    
    private void graphHeap(){
    	graphedHeap = new MutableLabeledGraph<Object, Object>();
    	Object emptyRootNode = new Object();
    	graphedHeap.insertRoot(emptyRootNode);
    	if (!relFH.isOpen())
            relFH.load();
    	if (!relVH.isOpen())
            relVH.load();
    	if (!relHFH.isOpen())
            relHFH.load();
    	
    	PairIterable<jq_Field, Quad> itrFH = relFH.getAry2ValTuples();
    	for(Pair<jq_Field, Quad> p : itrFH)
    		graphedHeap.insertLabel(emptyRootNode, p.val1, p.val0);
    	
    	PairIterable<Register, Quad> itrVH = relVH.getAry2ValTuples();
    	for(Pair<Register, Quad> p : itrVH)
    		graphedHeap.insertLabel(emptyRootNode, p.val1, p.val0);
    	
    	TrioIterable<Quad, jq_Field, Quad> itrHFH = relHFH.getAry3ValTuples();
    	for(Trio<Quad, jq_Field, Quad> t : itrHFH)
    		graphedHeap.insertLabel(t.val0, t.val2, t.val1);
    }
    
    /**
     * Frees relations used by this program analysis if they are in memory.
     * <p>
     * This method must be called after clients are done exercising the interface of this analysis.
     */
    public void free() {
        if (relVH.isOpen())
            relVH.close();
        if (relFH.isOpen())
            relFH.close();
        if (relHFH.isOpen())
            relHFH.close();
    }
}
