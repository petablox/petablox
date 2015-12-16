package chord.analyses.thread;

import soot.SootMethod;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramDom;
import chord.analyses.method.DomM;

/**
 * Domain of abstract threads.
 * <p>
 * An abstract thread is a triple (o,c,m) denoting the thread whose abstract object
 * is 'o' and which starts at method 'm' in abstract context 'c'.
 *
 * @see chord.analyses.thread.ThreadsAnalysis
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DomA extends ProgramDom<SootMethod> {
    private DomM domM;

    @Override
    public String toXMLAttrsString(SootMethod m) {
        if (m == null) return "";
        if (domM == null) domM = (DomM) ClassicProject.g().getTrgt("M");
        int mIdx = domM.indexOf(m);
        return "Mid=\"M" + mIdx + "\"";
    }
}
