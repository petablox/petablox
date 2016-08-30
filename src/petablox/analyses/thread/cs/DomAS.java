/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package petablox.analyses.thread.cs;

import soot.Unit;
import soot.SootMethod;
import petablox.project.ClassicProject;
import petablox.project.Project;
import petablox.project.analyses.ProgramDom;
import petablox.analyses.alias.Ctxt;
import petablox.analyses.alias.DomC;
import petablox.analyses.invk.DomI;
import petablox.analyses.method.DomM;
import petablox.util.tuple.object.Pair;

/**
 * Domain of abstract threads.
 * <p>
 * An abstract thread is a double <tt>(c,m)</tt> denoting the thread
 * which starts at method 'm' in abstract context 'c'.
 *
 * @see petablox.analyses.thread.ThreadsAnalysis
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
/*public class DomAS extends ProgramDom<Pair<Ctxt, SootMethod>> {
        private DomC domC;
        private DomM domM;
        public String toXMLAttrsString(Pair<Ctxt, SootMethod> aVal) {
                if (domC == null)
                        domC = (DomC) ClassicProject.g().getTrgt("C");
                if (domM == null)
                        domM = (DomM) ClassicProject.g().getTrgt("M");
                if (aVal == null)
                        return "";
                int c = domC.indexOf(aVal.val0);
                int m = domM.indexOf(aVal.val1);
                return "Cid=\"C" + c + "\" Mid=\"M" + m + "\"";
        }
}*/

public class DomAS extends ProgramDom<Pair<Unit, SootMethod>> {
    private DomI domI;
    private DomM domM;
    public String toXMLAttrsString(Pair<Ctxt, SootMethod> aVal) {
            if (domI == null)
                    domI = (DomI) ClassicProject.g().getTrgt("I");
            if (domM == null)
                    domM = (DomM) ClassicProject.g().getTrgt("M");
            if (aVal == null)
                    return "";
            int i = domI.indexOf(aVal.val0);
            int m = domM.indexOf(aVal.val1);
            return "Iid=\"I" + i + "\" Mid=\"M" + m + "\"";
    }
}