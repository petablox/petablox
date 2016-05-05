package petablox.reporting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import petablox.android.analyses.LocalVarNode;
import petablox.android.analyses.ParamVarNode;
import petablox.android.analyses.RetVarNode;
import petablox.android.analyses.ThisVarNode;
import petablox.android.analyses.VarNode;
import petablox.program.Program;
import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;
import petablox.android.srcmap.sourceinfo.abstractinfo.AbstractSourceInfo;

import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.VoidType;
import soot.jimple.Stmt;
import petablox.android.paths.CtxtLabelPoint;
import petablox.android.paths.CtxtPoint;
import petablox.android.paths.CtxtVarPoint;
import petablox.android.paths.Path;
import petablox.android.paths.PathsAdapter;
import petablox.android.paths.Step;
import petablox.android.srcmap.sourceinfo.SourceInfo;
import petablox.android.util.PropertyHelper;
import petablox.android.util.tree.Node;
import petablox.android.util.tree.Tree;
import petablox.util.soot.SootUtilities;

/**
 * Generates a an xml report that represents flows
 * as callstacks of depth K given in the analysis.
 * An attempt is made to (conservatively) link 
 * contexts together so the full callstack is shown.
 * Top-level methods that belong to the harness/framework
 * are not included. 
 *
 * @author brycecr
 */
public class SrcSinkFlowViz extends XMLVizReport {
    protected enum StepActionType {
        SAME, // same parent context as last method added
        DROP, // new method called by last step's method
        POP, // new method at level of last context's caller
        BROKEN, // no context overlap with last; also initial step
        OTHER // self-explanatory
    }

    public SrcSinkFlowViz() {
        super("Flow Path Vizualization");
    }

    public void generate() {

        try {
            final ProgramRel relSrcSinkFlow = (ProgramRel)ClassicProject.g().getTrgt("flow");

            relSrcSinkFlow.load();

            System.out.println("SOLVERGENPATHS");

            ArrayList<Tree<SootMethod>> flows = new ArrayList<Tree<SootMethod>>();
            Map<SootMethod, ArrayDeque<CallSite>> callSites = new HashMap<SootMethod, ArrayDeque<CallSite>>();

            String schemaFile = PropertyHelper.getProperty("petablox.android.paths.schema");
            String rawPathsFile = PropertyHelper.getProperty("petablox.android.paths.raw");
            List<Path> paths = new PathsAdapter(schemaFile).getFlatPaths(rawPathsFile);

            int count = 0; //just counts for the sake of numbering. Will be phased out in future versions
            for (Path p : paths) {
                count += 1;
                CtxtLabelPoint start = (CtxtLabelPoint)p.start;
                CtxtLabelPoint end = (CtxtLabelPoint)p.end;
                String startLabel = start.label;
                String endLabel = end.label;
                String flowname = count + ") "+ startLabel + " --> " + endLabel;

                Tree<SootMethod> t = new Tree<SootMethod>(new SootMethod(flowname, Collections.<Type>emptyList(), VoidType.v()));
                Node<SootMethod> lastNode = t.getRoot();

                //TODO init?? Does this work?
                //Add ctxt for label
                //System.err.println(/* context */);

                for (Step s : p.steps) {
                    System.err.println("PRINT LASTNODE: " + lastNode);
                    if (!(s.target instanceof CtxtVarPoint)) {
                        continue;
                    }
                    SootMethod parentMethod = getBottomCtxtMethod(s);
                    SootMethod method = getMethod(s);
                    logCallSites(s, callSites);
                    System.err.print(((CtxtPoint)s.target).ctxt);

                    switch(getStepActionType(parentMethod, s, lastNode, t)) {

                        case SAME:
                            System.err.println("case SAME:");
                            // could have consecutive exact callstack repeat
                            if (!lastNode.getData().equals(method) && method != null) {
                               lastNode = t.getParent(lastNode).addChild(method);
                            }
                            break;

                        case DROP:
                            System.err.println("case DROP:");
                            lastNode = lastNode.addChild(method);
                            break;

                        case POP:
                            System.err.println("case POP:");
                            Node<SootMethod> grandFather = t.getParent(t.getParent(lastNode));
                            Node<SootMethod> greatGrandFather = t.getParent(grandFather);

                            if (t.isRoot(greatGrandFather)) {
                                greatGrandFather.replaceChild(grandFather, getTopCtxtMethod(s));
                            }
                            //grandfather shouldn't be root or anything like that
                            //if stuff
                            lastNode = grandFather.addChild(method);
                            break;

                        case BROKEN:
                            System.err.println("case BROKEN:");
                            lastNode = addCtxt(t, s);
                            lastNode = lastNode.addChild(getMethod(s));
                            break;

                        case OTHER:
                        default:
                            throw new Exception("Unrecognized StepActionType in SrcSinkFlowViz generate");
                    }
                }
                flows.add(t);
            }

        generateReport(flows, callSites);

        } catch (IllegalStateException ise) {
            // The hope is that this will be caught here if the error is simply that
            // no path solver was run. Try to provide some intelligable feeback...
            makeOrGetSubCat("Error: No Path Solver Found"); // TODO: undesireable b/c creates empty + drop-down
            System.out.println("No path solver found so no path visualization could be generated.");
            System.out.println("To visualize paths run with -Dstamp.backend=solvergen");

        } catch (Exception e) {
            //Something else went wrong...
            System.err.println("Problem producing FlowViz report");
            e.printStackTrace();
        }

    }

    /**
     * Add the entire context for the step s to the tree t
     * @return the node for the bottom context method
     */
    public Node<SootMethod> addCtxt(Tree<SootMethod> t, Step s) {
        Unit[] context = ((CtxtPoint)s.target).ctxt.getElems();
        Node<SootMethod> lastNode = t.getRoot();
       
        for (int i = context.length - 1; i >= 0; --i) {
            Stmt stm = (Stmt)context[i];
            lastNode = lastNode.addChild(getMethod(stm));
        }

        return lastNode;
    }

    /**
     * Frome the callgraph tree and map of methods to callsites
     * provided as parameters, generates the XML report
     */
    protected void generateReport(ArrayList<Tree<SootMethod>> flows, Map<SootMethod, ArrayDeque<CallSite>> callSites) {
        System.out.println("Generating viz report");
        for (Tree<SootMethod> t : flows) {
            System.out.println(t.toString());
            Category c = makeOrGetSubCat(t.getRoot().getData().getName());
            Tree<SootMethod>.TreeIterator itr = t.iterator();
            
            Deque<Category> stack = new ArrayDeque<Category>();
            while (itr.hasNext()) {
                int oldDepth = itr.getDepth();
                SootMethod meth = itr.next();
                int newDepth = itr.getDepth();

                if (filter(meth, stack.size(), t)) {
                    continue;
                } else if (oldDepth < newDepth) { // DROP down
                    assert newDepth - oldDepth == 1;
                    stack.push(c);
                    c = c.makeOrGetSubCat(meth);

                
                } else if (oldDepth > newDepth) { //POP up
                    int del = oldDepth - newDepth;
                    System.out.println("Del: " + del);
                    for (; del > 0 && !stack.isEmpty(); del--) {
                        c = stack.pop();
                    }
                    c = stack.peek();
                    if (c == null) { // end condition FIXME (hack)
                        break;
                    }
                    c = c.makeOrGetSubCat(meth);

                } else { // stay SAME
                    c.makeOrGetSubCat(meth);

                }
                //add classinfo data
            }
        }
    }

    protected boolean filter(SootMethod method, int depth, Tree<SootMethod> t) {
        if (t.isRoot(method)) {
            return false;
        }
        return (AbstractSourceInfo.isFrameworkClass(method.getDeclaringClass()) && depth == 0);
    }

    /**
     * Returns the method object for the top (outermost)
     * context level associated with the parameter Step 
     */
    private SootMethod getTopCtxtMethod(Step s) {
        CtxtPoint point = (CtxtPoint)s.target;
        Unit[] ctxt = point.ctxt.getElems();

        if (ctxt.length > 1) {
            SootMethod method = getMethod((Stmt)ctxt[ctxt.length-1]);
            if (method == null) {
                return new SootMethod("No Method", Collections.<Type>emptyList(), VoidType.v()); // TODO check is this OK?
            }
            return method;
        } 
        // Ought to happen rarely or not at all...
        return getMethod(s);
    }

    /**
     * Returns the method object for the top (outermost)
     * context level associated with the parameter Step 
     */
    private SootMethod getBottomCtxtMethod(Step s) {
        CtxtPoint point = (CtxtPoint)s.target;
        Unit[] ctxt = point.ctxt.getElems();

        if (ctxt.length >= 1) {
            SootMethod method = getMethod((Stmt)ctxt[0]);
            if (method == null) {
                return new SootMethod("No Method", Collections.<Type>emptyList(), VoidType.v()); // TODO check is this OK?
            }
            return method;
        } 
        // Ought to happen rarely or not at all...
        return getMethod(s);
    }


    /**
     * Returns the "StepActionType" of the Step s.
     * In other words, returns the code for how the callgraph tree
     * will be modified by s. See @StepActionType for information
     * on return types.
     */
    private StepActionType getStepActionType(SootMethod method, Step s, Node<SootMethod> lastNode, Tree t) {
         
        // Throughout these we follow a minimal detection which, to my knowledge, ought to suffice.
        // However, it may be wiser in order to be certain we report the correct type and catch edge
        // cases to check that all conditions for each type apply
        System.err.println("NODE" + lastNode.getData().getName());
        System.err.println("PARENT" + ((Node<SootMethod>)t.getParent(lastNode)).getData().getName());
        System.err.println("GRANDPARENT" + ((Node<SootMethod>)t.getParent(t.getParent(lastNode))).getData().getName());
        System.err.println("NEWMETH" + method.getName());

        if (t.isRoot(lastNode) /* other condition? */ ) {
            return StepActionType.BROKEN;
        } else if (t.getParent(lastNode).getData().equals(method)) {
            return StepActionType.SAME;
        } else if (lastNode.getData().equals(method)) {
            return StepActionType.DROP;
        } else if (t.getParent(t.getParent(lastNode)).getData().equals(method)) {
            return StepActionType.POP;
        }         

        return StepActionType.BROKEN;
        /* return OTHER? */
    }

    /**
     * Save take variable and context information
     * find the associated source line, file, and class
     * (i.e. the CallSite object) and save that into
     * the map pamameter
     */
    private void logCallSites(Step s, Map<SootMethod, ArrayDeque<CallSite>> callSites) {
        CtxtPoint point = (CtxtPoint)s.target;
        Unit[] context = point.ctxt.getElems();
        SootMethod method = getMethod(s);
        Stmt stm = null;

        for (int i = -1; i < context.length; ++i) {
            // First iteration uses step target variable inited above
            if (i >= 0) {
                stm = (Stmt)context[i];
                method = getMethod(stm);
            }
            // We could filter methods here (i.e. framework), 
            // but might make sense to do it
            // instead while generating the XML report itself

            // Is this too general? Maybe we should handle these
            // statements differently based on their type?
            if (method == null) {
                continue;
            }

            // We add callsite for all but the topmost context
            // level, because we don't know the context for that
            if (i < context.length - 1) {
                CallSite cs = generateCallSite(method, (Stmt)context[i+1]);
                if (!callSites.containsKey(method)) {
                    callSites.put(method, new ArrayDeque<CallSite>());
                }
                callSites.get(method).addLast(cs);
            }
        }
    }


    /**
     * Generates a callsite object for the method called by statement caller
     */
    private CallSite generateCallSite(SootMethod method, Stmt caller) {

        try {
            String locStr = this.sourceInfo.javaLocStr(caller);
            String[] locStrTokens = locStr.split(":");
            assert locStrTokens.length >= 1;

            // Create callsite 
            String methName = method.getName();
            int lineNumber = (locStrTokens.length > 1) ? Integer.parseInt(locStrTokens[1]) : 0;
            String className = this.sourceInfo.srcClassName(caller);
            String srcFilePath = locStrTokens[0];
            /* Some weird gui behavior associated with line number -1 so...
               ...This may be useful: if (methodLineNum < 0) {methodLineNum = 0;}
             */

            CallSite cs = new CallSite(methName, className, lineNumber, srcFilePath);
            return cs;

        } catch (NumberFormatException nfe) {
            System.err.println("Line number format was incorrect for callsite. Expecting "
                    + "[srcFilePath]:[line number] (no brackets)");
            nfe.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the method object associated with the 
     * CtxtPoint of the parameter step
     */
    private SootMethod getMethod(Step s) {
        VarNode v = ((CtxtVarPoint)s.target).var;
        SootMethod method = null;

        if (v instanceof LocalVarNode) {
            LocalVarNode localRegister = (LocalVarNode)v;
            method = localRegister.meth;
        } else if (v instanceof ThisVarNode) {
            ThisVarNode thisRegister = (ThisVarNode)v;
            method = thisRegister.method;
        } else if (v instanceof ParamVarNode) {
            ParamVarNode paramRegister = (ParamVarNode)v;
            method = paramRegister.method;
        } else if (v instanceof RetVarNode) {
            RetVarNode retRegister = (RetVarNode)v;
            method = retRegister.method;
        } 

        return method;
    }

    /**
     * Returns the method object associated with the 
     * method that contains the statement parameter
     */
    private SootMethod getMethod(Stmt stm) {
        return SootUtilities.getMethod(stm);
    }

    /**
     * A class representing the data needed to represent a callsite
     * in terms of data necessary for the frontend to locate and highlight
     * the correct location in the correct source file.
     * The method name identifier may not be strictly necessary and is more
     * of an identifier. As far as the class is concerned, any parameter
     * to the contstructor may be null
     */
    class CallSite {
        String className;
        String srcFilePath;
        int lineNumber;
        String methodName;

        public CallSite(String methodName, String className, int lineNumber, String srcFilePath) {
            this.methodName = methodName;
            this.className = className;
            this.srcFilePath = srcFilePath;
            this.lineNumber = lineNumber;
        }
    }
}
