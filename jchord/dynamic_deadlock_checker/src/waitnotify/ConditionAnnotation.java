package waitnotify;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

import chord.runtime.BasicEventHandler;

public abstract class ConditionAnnotation {
	protected static Class globalLock = WNLoggerObserver.class;
	
	//list of condition(s) of interest for an object (we map the id of the object to the set of conditions)
	protected static Map<Integer, Set<ConditionAnnotation>> objects = 
					new HashMap<Integer, Set<ConditionAnnotation>>();
	
	//used to generate a unique id for a condition
	protected static int counter = 0;
	
	protected Object o1;
	protected Object o2;
	protected Object o3;
	protected Object o4;
	
	//id of the condition
	public int conditionId;
	
	//current value of the condition
	protected boolean curVal;
	
	public ConditionAnnotation(Object o1) {
		this.o1 = o1;
		synchronized (globalLock) {
			conditionId = getNextConditionId();
			put(o1);
			initCond();
		}
	}
	
	public ConditionAnnotation(Object o1, Object o2) {
		this.o1 = o1;
		this.o2 = o2;
		synchronized (globalLock) {
			conditionId = getNextConditionId();
			put(o1);
			put(o2);
			initCond();
		}
	}
	
	public ConditionAnnotation(Object o1, Object o2, Object o3) {
		this.o1 = o1;
		this.o2 = o2;
		this.o3 = o3;
		synchronized (globalLock) {
			conditionId = getNextConditionId();
			put(o1);
			put(o2);
			put(o3);
			initCond();
		}
	}
	
	public ConditionAnnotation(Object o1, Object o2, Object o3, Object o4) {
		this.o1 = o1;
		this.o2 = o2;
		this.o3 = o3;
		this.o4 = o4;
		synchronized (globalLock) {
			conditionId = getNextConditionId();
			put(o1);
			put(o2);
			put(o3);
			put(o4);
			initCond();
		}
	}
	
	private static int getNextConditionId() {
		return counter++;
	}
	
	private void put(Object o) {
		int objId = BasicEventHandler.getObjectId(o);
		Set<ConditionAnnotation> setOfConds = (Set<ConditionAnnotation>) objects.get(objId);
		if (setOfConds == null) {
			setOfConds = new HashSet<ConditionAnnotation>();
			objects.put(objId, setOfConds);
		}
		setOfConds.add(this);
	}
	
	public abstract boolean isConditionTrue();
	
	public void waitBegin(Object lock) {
		synchronized (globalLock) {
			int lockId = BasicEventHandler.getObjectId(lock);
			logCondForWaitOrNotify(true, lockId);
		}
	}
	
	public void waitEnd() {
		synchronized (globalLock) {
			logCondEndForWaitOrNotify();
		}
	}
	
	private void logCondForWaitOrNotify(boolean isWait, int lockId){
		int tId = BasicEventHandler.getObjectId(Thread.currentThread());
		boolean val = isConditionTrue();
		List<String> instrs = new LinkedList<String>();
		
		String tabs = WNLogger.getStringWithTabs(tId);
		String instr1 = tabs + "if (c" + conditionId + ") { ";
		instrs.add(instr1);
		WNLogger.condIds.add(conditionId);
		WNLogger.addInstr(instr1, tId, WNLogger.T_OTHER, WNLogger.LID_NA);
		
		WNLogger.incNumTabs(tId);
		
		if (!val) {
			if(isWait){
				tabs = WNLogger.getStringWithTabs(tId);
				String instr2 = tabs + "synchronized (l" +lockId + ") { l" + lockId + ".wait(); }";
				instrs.add(instr2);
				WNLogger.lockIds.add(lockId);
				WNLogger.addInstr(instr2, tId, WNLogger.T_WAIT, lockId);
			}
			else{
				tabs = WNLogger.getStringWithTabs(tId);
				String instr2 = tabs + "synchronized (l" +lockId + ") { l" + lockId + ".notify(); }";
				instrs.add(instr2);
				WNLogger.addInstr(instr2, tId, WNLogger.T_NOTF_OR_NOTFALL, lockId);
			}
		}
	}
	
	private void logCondEndForWaitOrNotify(){
		int tId = BasicEventHandler.getObjectId(Thread.currentThread());
		WNLogger.decNumTabs(tId);
		String instr = WNLogger.getStringWithTabs(tId) + "}";
		WNLogger.addInstr(instr, tId, WNLogger.T_OTHER, WNLogger.LID_NA);
	}
	
	public void notifyBegin(Object lock) {
		synchronized (globalLock) {
			int lockId = BasicEventHandler.getObjectId(lock);
			logCondForWaitOrNotify(false, lockId);
		}
	}
	
	public void notifyEnd() {
		synchronized (globalLock) {
			logCondEndForWaitOrNotify();
		}
	}
	
	private void initCond(){
		synchronized (globalLock) {
			curVal = isConditionTrue();	
			
			int tId = BasicEventHandler.getObjectId(Thread.currentThread());
			String tabs = WNLogger.getStringWithTabs(tId);
			String instr = tabs + "c" + conditionId + " = " + curVal + ";";
			WNLogger.condIds.add(conditionId);
			WNLogger.addInstr(instr, tId, WNLogger.T_OTHER, WNLogger.LID_NA);
		}
	}
	
	public String logChange(String iidDescr) {
		synchronized (globalLock) {
			boolean newVal = isConditionTrue();
			if (newVal != curVal) {
				int tId = BasicEventHandler.getObjectId(Thread.currentThread());
				String tabs = WNLogger.getStringWithTabs(tId);
				String instr = tabs + "c" + conditionId + " = " + newVal + ";" + " //"+iidDescr;
				curVal = newVal;
				
				WNLogger.condIds.add(conditionId);
				return instr;
			}
			return null;
		}
	}
	
	public void forciblyLogCondVal() {
		synchronized (globalLock) {
			boolean newVal = isConditionTrue();
			int tId = BasicEventHandler.getObjectId(Thread.currentThread());
			String tabs = WNLogger.getStringWithTabs(tId);
			String instr = tabs + "c" + conditionId + " = " + newVal + ";";
			if (newVal != curVal)
				curVal = newVal;
			
			WNLogger.condIds.add(conditionId);
			WNLogger.addInstr(instr, tId, WNLogger.T_OTHER, WNLogger.LID_NA);
			
		}
	}
	
	// to be called after every write to a field of o
	// astorePrimitive, astoreReference, putfieldPrimitive, putfieldReference, putstaticPrimitive,
	// putstaticReference, afterMethodCall)
	public static List<String> checkForCondChanges(int objId, String iidDescr) {
		List<String> instrs = new LinkedList<String>();
		synchronized (globalLock) {
			Set<ConditionAnnotation> conditions = (Set<ConditionAnnotation>)objects.get(objId);
			if (conditions == null)
				return null;
			for (ConditionAnnotation condition : conditions) {
				String instr = condition.logChange(iidDescr);
				if(instr != null){
					instrs.add(instr);
				}
			}
			if(instrs.size() == 0){
				return null;
			}
			return instrs;
		}
	}
	
	public int hashCode(){
		return conditionId;
	}
	
	public boolean equals(Object o){
		if(!(o instanceof ConditionAnnotation)){
			return false;
		}
		ConditionAnnotation otherCondAnnot = (ConditionAnnotation)o;
		return (conditionId == otherCondAnnot.conditionId);
	}
}


