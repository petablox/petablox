package stamp.droidrecord;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;

import edu.stanford.droidrecord.logreader.EventLogStream;
import edu.stanford.droidrecord.logreader.analysis.CallArgumentValueAnalysis;
import edu.stanford.droidrecord.logreader.events.info.ParamInfo;

public class StampCallArgumentValueAnalysis {
    
    private CallArgumentValueAnalysis cavAnalysis;
    
    public StampCallArgumentValueAnalysis(EventLogStream els) {
        cavAnalysis = new CallArgumentValueAnalysis(els);
    }
    
    public boolean isReady() {
        return cavAnalysis.isReady();
    }
    
    public void run() {
        cavAnalysis.run();
    }
    
    private int stmtToSourcePosition(Unit u) {
    	for (@SuppressWarnings("rawtypes")
			 Iterator j = u.getTags().iterator(); j.hasNext(); ) {
		    Tag tag = (Tag)j.next();
		    if (tag instanceof LineNumberTag) {
		    	LineNumberTag lnTag = (LineNumberTag) tag;
			    return lnTag.getLineNumber();
		    }
	    }
		return -1; // Line number information unavailable
	}
    
    public List<ParamInfo> queryArgumentValues(SootMethod caller, 
                                               Stmt stmt, int argNum) {
        if(!stmt.containsInvokeExpr()) {
            return new ArrayList<ParamInfo>();
        }
        SootMethod method = stmt.getInvokeExpr().getMethod();
        String methodSignature = method.getSignature();
        String parentMethodSignature = caller.getSignature();
        long srcLineNumber = stmtToSourcePosition(stmt);
        return cavAnalysis.queryArgumentValues(methodSignature, 
                                               parentMethodSignature,
                                               srcLineNumber,
                                               argNum);
    }

}