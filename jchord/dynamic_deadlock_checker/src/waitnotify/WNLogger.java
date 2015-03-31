package waitnotify;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.Collections;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;

import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.DynamicAnalysis;
import chord.program.MethodElem;
import chord.program.Program;
import chord.util.IndexMap;

import chord.util.tuple.object.Pair;
import chord.util.Utils;
import chord.project.Config;

import java.io.PrintWriter;

@Chord(
	name = "wn-log"
)
public class WNLogger extends DynamicAnalysis {

    protected InstrScheme instrScheme;

    protected static Map<Integer, List<StringIntIntTuple>> thrToInstrs = new TreeMap<Integer, List<StringIntIntTuple>>();
    protected static Map<Integer, List<String>> thrToSimplifiedInstrs = new TreeMap<Integer, List<String>>();
    protected static Set<Integer> thrIds = new HashSet<Integer>();
    protected static Set<Integer> lockIds = new HashSet<Integer>();
    protected static Set<Integer> condIds = new HashSet<Integer>();
    protected static Map<Integer, Set<Integer>> thrToLockIds = new TreeMap<Integer, Set<Integer>>();
  
    protected static final int T_SYNC_BEGIN = 1;
    protected static final int T_SYNC_END = 2;
    protected static final int T_WAIT = 3;
    protected static final int T_NOTF_OR_NOTFALL = 4;
    protected static final int T_OTHER = 5;
    
    protected static final int LID_NA = -1;
    
    
    //for indentation
    protected static Map<Integer, Integer> thrToNumTabs = new TreeMap<Integer, Integer>();
    
	@Override
    public Class getEventHandlerClass() {
		return WNLoggerObserver.class;
	}

	@Override
    public String getTraceKind() {
        return "none";
    }

	@Override
    public InstrScheme getInstrScheme() {
    	if (instrScheme != null)
    		return instrScheme;
    	instrScheme = new InstrScheme();
    	
    	instrScheme.setPutstaticPrimitiveEvent(true, true, true, true);
    	instrScheme.setPutfieldPrimitiveEvent(true, true, true, true);
    	instrScheme.setAstorePrimitiveEvent(true, true, true, true);

   		instrScheme.setPutstaticReferenceEvent(true, true, true, true, false);
   		instrScheme.setPutfieldReferenceEvent(true, true, true, true, false);
   		instrScheme.setAstoreReferenceEvent(true, true, true, true, false);

   		instrScheme.setThreadStartEvent(true, true, true);
   		instrScheme.setThreadJoinEvent(true, true, true);
   		instrScheme.setAcquireLockEvent(true, true, true);
   		instrScheme.setReleaseLockEvent(true, true, true);
   		instrScheme.setWaitEvent(true, true, true);
   		instrScheme.setNotifyEvent(true, true, true);
   		instrScheme.setMethodCallEvent(true, true, true, false, true);
   		
   		return instrScheme;
	}

	@Override
	public void initAllPasses() {
		writeProcessedMap("E", "Elocs.dynamic.txt", false);
		writeProcessedMap("I", "Ilocs.dynamic.txt", false);
		writeProcessedMap("L", "Llocs.dynamic.txt", true);
		writeProcessedMap("R", "Rlocs.dynamic.txt", true);
	}

	private void writeProcessedMap(String domName, String name, boolean isLmapOrRmap) {
		ProgramDom dom = (ProgramDom) ClassicProject.g().getTrgt(domName);
		ClassicProject.g().runTask(dom);
		LinkedList<String> locs = new LinkedList<String>();
		for(int i = 0; i < dom.size(); i++){
			String loc = iidToString(i, dom, isLmapOrRmap);		
			locs.add(loc);
		}
		String outDirName = Config.outDirName;
		Utils.writeListToFile(locs, outDirName+"/"+name);
	}

	public static void doPutstaticPrimitive(int eId, int tId, int bId,  int fId) {
		List<String> instrs = ConditionAnnotation.checkForCondChanges(bId, iidToLoc(eId, WNLoggerObserver.Elist));
		if(instrs != null){
			addInstrs(instrs, tId, T_OTHER, LID_NA);
		}
	}
	
	public static void doPutfieldPrimitive(int eId, int tId, int bId, int fId) {
		List<String> instrs = ConditionAnnotation.checkForCondChanges(bId, iidToLoc(eId, WNLoggerObserver.Elist));
		if(instrs != null){
			addInstrs(instrs, tId, T_OTHER, LID_NA);
		}
	}
	
	public static void doAstorePrimitive(int eId, int tId, int bId, int iId) {
		List<String> instrs = ConditionAnnotation.checkForCondChanges(bId, iidToLoc(eId, WNLoggerObserver.Elist));
		if(instrs != null){
			addInstrs(instrs, tId, T_OTHER, LID_NA);
		}
	}
	
	public static void doPutstaticReference(int eId, int tId, int bId, int fId, int oId) {
		List<String> instrs = ConditionAnnotation.checkForCondChanges(bId, iidToLoc(eId, WNLoggerObserver.Elist));
		if(instrs != null){
			addInstrs(instrs, tId, T_OTHER, LID_NA);
		}
	}
	
	public static void doPutfieldReference(int eId, int tId, int bId, int fId, int oId) { 
		List<String> instrs = ConditionAnnotation.checkForCondChanges(bId, iidToLoc(eId, WNLoggerObserver.Elist));
		if(instrs != null){
			addInstrs(instrs, tId, T_OTHER, LID_NA);
		}
	}
	
	public static void doAstoreReference(int eId, int tId, int bId, int iId, int oId) {
		List<String> instrs = ConditionAnnotation.checkForCondChanges(bId, iidToLoc(eId, WNLoggerObserver.Elist));
		if(instrs != null){
			addInstrs(instrs, tId, T_OTHER, LID_NA);
		}
	}

	//oId would be zero for static method calls
	public static void doMethodCallAft(int iId, int tId, int oId){
		List<String> instrs = ConditionAnnotation.checkForCondChanges(oId, iidToLoc(iId, WNLoggerObserver.Ilist));
		if(instrs != null){
			addInstrs(instrs, tId, T_OTHER, LID_NA);
		}
	}

	public static void doAcquireLock(int pId, int tId, int lId) {
		String tabs = getStringWithTabs(tId);
		String instr = tabs + "synchronized(l"+lId+"){ //"+iidToLoc(pId, WNLoggerObserver.Llist);
		addInstr(instr, tId, T_SYNC_BEGIN, lId);
		lockIds.add(lId);
		incNumTabs(tId);
		Set<Integer> lockIdsForTId = thrToLockIds.get(tId);
	        if(lockIdsForTId == null){
			lockIdsForTId = new HashSet<Integer>();
			thrToLockIds.put(tId, lockIdsForTId);
		}	
		lockIdsForTId.add(lId);
	}

	public static void doReleaseLock(int pId, int tId, int lId) {	
		decNumTabs(tId);
		String tabs = getStringWithTabs(tId);
		String instr = tabs + "} //"+iidToLoc(pId, WNLoggerObserver.Rlist);
		addInstr(instr, tId, T_SYNC_END, lId);
	}
	
	public static void doThreadStart(int pId, int tId, int oId) { 
		String tabs = getStringWithTabs(tId);
		String instr = tabs + "t"+oId+".start(); //"+iidToLoc(pId, WNLoggerObserver.Ilist);
		addInstr(instr, tId, T_OTHER, LID_NA);
		thrIds.add(oId);
	}
	
	public static void doThreadJoin(int pId, int tId, int oId) {
		String tabs = getStringWithTabs(tId);
		String instr = tabs + "t"+oId+".join(); //"+iidToLoc(pId, WNLoggerObserver.Ilist);
		addInstr(instr, tId, T_OTHER, LID_NA);
		thrIds.add(oId);
	}
	
	public static void doWait(int pId, int tId, int lId) { 
		String tabs = getStringWithTabs(tId);
		String instr = tabs + "l"+lId+".wait(); //"+iidToLoc(pId, WNLoggerObserver.Ilist);
		addInstr(instr, tId, T_WAIT, lId);
		lockIds.add(lId);
	}
	
	public static void doNotify(int pId, int tId, int lId) { 
		String tabs = getStringWithTabs(tId);
		String instr = tabs + "l"+lId+".notify(); //"+iidToLoc(pId, WNLoggerObserver.Ilist);
		addInstr(instr, tId, T_NOTF_OR_NOTFALL, lId);
		lockIds.add(lId);
	}

	public static void doNotifyAll(int pId, int tId, int lId) { 
		String tabs = getStringWithTabs(tId);
		String instr = tabs + "l"+lId+".notifyAll(); //"+iidToLoc(pId, WNLoggerObserver.Ilist);
		addInstr(instr, tId, T_NOTF_OR_NOTFALL, lId);
		lockIds.add(lId);
	}

	public static String getStringWithTabs(int tId){
		Integer numTabs = getNumTabs(tId);
		String tabs = getStringWithTabsGivenNumTabs(numTabs);
		return tabs;
	}

	public static String getStringWithTabsGivenNumTabs(int nTabs){
		String tabs = "";
		for(int i = 0; i < nTabs; i++){
			tabs += "\t";		
		}
		return tabs;
	}

	public static void incNumTabs(int tId){
		Integer numTabs = getNumTabs(tId);
		thrToNumTabs.put(tId, numTabs+1);
	}

	public static void decNumTabs(int tId){
		Integer numTabs = getNumTabs(tId);
		if(numTabs > 0)
			thrToNumTabs.put(tId, numTabs-1);
	}

	private static Integer getNumTabs(int tId){
		Integer numTabs = thrToNumTabs.get(tId);
		if(numTabs == null){
			numTabs = new Integer(0);
			thrToNumTabs.put(tId, numTabs);
		}
		return numTabs;
	}
	
	public static String iidToString(int iid, ProgramDom dom, boolean isLmapOrRmap) {
		String s;
		String res = "";
		Program program = Program.g();
		
		s = dom.toUniqueString(iid);
	
		MethodElem elem = MethodElem.parse(s);
		int bci = elem.offset;
		if (isLmapOrRmap && (bci == -1 || bci == -2))
			bci = 0;
		String mName = elem.mName;
		String mDesc = elem.mDesc;
		String cName = elem.cName;
		jq_Class c = (jq_Class) program.getClass(cName);
		assert (c != null);
		String fileName = c.getSourceFileName();
		jq_Method m = program.getMethod(mName, mDesc, cName);
		assert (m != null);
		int lineNum = m.getLineNumber(bci);
		res += fileName + ":" + lineNum ;
		return res;
	}

	public static String iidToLoc(int iid, List<String> list){
		String loc = list.get(iid);
		return loc;
	}
	
	public static void addInstr(String instr, int tId, int type, int lId){
		List<StringIntIntTuple> instrsForTId = thrToInstrs.get(tId);
		if(instrsForTId == null){
			instrsForTId = new LinkedList<StringIntIntTuple>();
			thrToInstrs.put(tId, instrsForTId);
		}
		instrsForTId.add(new StringIntIntTuple(instr, type, lId));
	}
	
	public static void addInstrs(List<String> instrs, int tId, int type, int lId){
		List<StringIntIntTuple> instrsForTId = thrToInstrs.get(tId);
		if(instrsForTId == null){
			instrsForTId = new LinkedList<StringIntIntTuple>();
			thrToInstrs.put(tId, instrsForTId);
		}
		List<StringIntIntTuple> instrsToBeAdded = new LinkedList<StringIntIntTuple>();
		for(String s : instrs){
			instrsToBeAdded.add(new StringIntIntTuple(s, type, lId));
		}
		instrsForTId.addAll(instrsToBeAdded);
	}
	
	private static Set<Integer> thrsWEmptyBodies = null;

	protected static void createJavaCode(PrintWriter out){
		try{
			ThreadLocalOptimization thrLocalOptim = new ThreadLocalOptimization(thrToInstrs, thrToLockIds);
			thrLocalOptim.optimizeInstrs();
			
			Set<Integer> thrSharedLocksOrLocksInWaits = thrLocalOptim.getLIdsAfterOptim();
			out.println("public class Test {");
			initLocks(out, thrSharedLocksOrLocksInWaits);
			initConds(out);
			
			LockTreeOptimization lTreeOptim = new LockTreeOptimization(thrToInstrs);
			Map<Integer, List<String>> thrToInstrsAfterLockTreeOptim = lTreeOptim.getInstrsAfterOptimization();	
		
			initThreads(out, thrToInstrsAfterLockTreeOptim);
		
			out.println("\t public static void main(String[] args){");
		
			Set<Integer> thrds = thrToInstrsAfterLockTreeOptim.keySet();
			//mainThrs has all the tIds for which we did not see a tId.start()
			//This should only happen for the main thread
			//We generate code to start all such threads 
			Set<Integer> mainThrs = new HashSet<Integer>(thrds);
			mainThrs.removeAll(thrIds);
			
			for(Integer tId : mainThrs){
				startMainThread(tId, out);
			}
			
			out.println("\t }");
			out.println("}");
		}
		catch(Exception e){
			System.out.println("Exception in createJavaCode");
			e.printStackTrace();
		}
	}
	
	private static void initLocks(PrintWriter out, Set<Integer> lIdsSharedOrInWaits){
		for(Integer lId : lIdsSharedOrInWaits){
			out.println("\t public static Object l"+lId+" = new Object();");
		}
	}
	
	private static void initConds(PrintWriter out){
		for(Integer cId : condIds){
			out.println("\t public static boolean c"+cId+";");
		}
	}

	private static void initThreads(PrintWriter out, Map<Integer, List<String>> tToInstrs){
		for(Map.Entry<Integer, List<String>> instrsForT : tToInstrs.entrySet()){
			Integer tId = instrsForT.getKey();
			List<String> instrs = instrsForT.getValue();
			
			out.println("\t public static Thread t"+tId+" = new Thread(){");
			out.println("\t\t public void run(){");
			out.println("\t\t\t try{");
		
			if(instrs != null){
				for(String instr : instrs){
					out.println("\t\t\t\t"+instr);
				}
			}
		
			printEnclosingBraces(tId, out);
			out.println("\t\t\t}");
			out.println("\t\t\tcatch(Exception e){");
			out.println("\t\t\t\t System.out.println(\"Exception caught in run\");");
			out.println("\t\t\t\t e.printStackTrace();");
			out.println("\t\t\t}");
			out.println("\t\t}");
			out.println("\t};");
		}
	}

	private static void printEnclosingBraces(int tId, PrintWriter out){
		int nTabs = getNumTabs(tId);
		for(int i = nTabs; i > 0; i--){
                    String tabs = getStringWithTabsGivenNumTabs(i-1);
		    out.println("\t\t\t\t"+tabs+"}");		    
		}	
	}
	
	private static void startMainThread(int tId, PrintWriter out){
		out.println("\t\tt"+tId+".start();");
	}
}
