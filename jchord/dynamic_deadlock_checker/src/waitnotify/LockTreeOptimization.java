package waitnotify;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class LockTreeOptimization {
	Map<Integer, List<StringIntIntTuple>> thrToInstrs;
	
	public LockTreeOptimization(Map<Integer, List<StringIntIntTuple>> tToInstrs){
		thrToInstrs = tToInstrs;
	}
	
	private int lastIndx = -1;
	private int curIndxOfInstr = -1;
	
	public Map<Integer, List<String>> getInstrsAfterOptimization(){
		Map<Integer, List<String>> optimInstrs = new TreeMap<Integer, List<String>>();
		
		for(Map.Entry<Integer, List<StringIntIntTuple>> mEntry : thrToInstrs.entrySet()){
			Integer tId = mEntry.getKey();
			List<StringIntIntTuple> instrs = mEntry.getValue();
			List<String> newInstrs = new LinkedList<String>();
			
			LockStack ls = new LockStack();
			LockTree lt = new LockTree();
			
			lastIndx = -1;
			boolean waitingForLSToBeEmpty = false;
			
			curIndxOfInstr = -1;
			for(StringIntIntTuple instrTypeLId : instrs){
				String instr = instrTypeLId.fst;
				int type = instrTypeLId.snd;
				int lId = instrTypeLId.thrd;

				curIndxOfInstr++;

				if(type == WNLogger.T_SYNC_BEGIN){
					processSYNCBEGIN(instr, lId, waitingForLSToBeEmpty, newInstrs, ls, lt);
				}
				if(type == WNLogger.T_SYNC_END){
					processSYNCEND(instr, lId, waitingForLSToBeEmpty, newInstrs, ls, lt);
				}
				if(type == WNLogger.T_OTHER){
					processOTHER(instr, lId, waitingForLSToBeEmpty, newInstrs, ls, lt, instrs);
				}
			}
			addStmtsFromLT(newInstrs, lt);
			optimInstrs.put(tId, newInstrs);	
			
		}
		removeEmptyThreads(optimInstrs);
		return optimInstrs;
	}
	
	private void processSYNCBEGIN(String instr, int lId, boolean waitingForLSToBeEmpty, 
			List<String> newInstrs, LockStack ls, LockTree lt){
		String locId = getLocId(instr);
		boolean wasLocked = ls.lock(lId);
		if(waitingForLSToBeEmpty){
			newInstrs.add(instr);
		}
		else{
			if(wasLocked){
				lt.addChildToCurNode(lId, locId);
			}
		}
	}
	
	private void processSYNCEND(String instr, int lId, boolean waitingForLSToBeEmpty, 
			List<String> newInstrs, LockStack ls, LockTree lt){
		boolean wasLockRel = ls.unLock();
		if(waitingForLSToBeEmpty){
			newInstrs.add(instr);
			if(ls.isEmpty()){
				waitingForLSToBeEmpty = false;
				lastIndx = curIndxOfInstr;
			}
		}
		else{
			if(wasLockRel){
				lt.moveUp();
			}
			if(ls.isEmpty()){
				lt.resetNewlyAddedNodes();
				lastIndx = curIndxOfInstr;
			}
		}
	}
	
	private void processOTHER(String instr, int lId, boolean waitingForLSToBeEmpty, 
			List<String> newInstrs, LockStack ls, LockTree lt, List<StringIntIntTuple> origInstrs){
		if(waitingForLSToBeEmpty == false){
			addStmtsFromLT(newInstrs, lt);
			for(int j = lastIndx+1; j <= curIndxOfInstr; j++){
				newInstrs.add((origInstrs.get(j)).fst);
			}
			lastIndx = curIndxOfInstr;
			lt.reset();
			if(!ls.isEmpty()){
				waitingForLSToBeEmpty = true;
			}
		}
		else{
			newInstrs.add(instr);
		}
	}
	
	private static void addStmtsFromLT(List<String> newInstrs, LockTree lt){
		List<String> instrsFromLT = lt.dumpToStmts();
		if(!instrsFromLT.isEmpty()){
			newInstrs.addAll(instrsFromLT);
		}
	}	
	
	/***
	private static int getLId(String instr){
		assert(instr.contains("synchronized"));
		StringTokenizer st = new StringTokenizer(instr,"l)");
		st.nextToken();
		String idStr = st.nextToken();
		int id = Integer.parseInt(idStr);
		return id;
	}
	***/
	
	private static String getLocId(String instr){
		assert(instr.contains("synchronized"));
		StringTokenizer st = new StringTokenizer(instr,"{");
		st.nextToken();
		String locStr = st.nextToken();
		return locStr;
	}
	
	

	private static void removeEmptyThreads(Map<Integer, List<String>> thrToInstrs){
		Set<Integer> thrsWEmptyBodies = new HashSet<Integer>();
		for(Map.Entry<Integer, List<String>> entry : thrToInstrs.entrySet()){
			if(entry.getValue().isEmpty()){
				thrsWEmptyBodies.add(entry.getKey());
			}
		}
		
		for(Integer tId : thrsWEmptyBodies){
			thrToInstrs.remove(tId);
		}
		
		Set<Integer> thrsWOEmptyBodies = thrToInstrs.keySet();

		for(Map.Entry<Integer, List<String>> entry : thrToInstrs.entrySet()){
			List<String> instrs = entry.getValue();
			Iterator<String> instrsItr = instrs.iterator();
			while(instrsItr.hasNext()){
				String instr = instrsItr.next();
				if(doesInstrStartThrWEmptyBody(instr, thrsWOEmptyBodies)){
					instrsItr.remove();
				}
			}
		}

	}
	
	private static boolean doesInstrStartThrWEmptyBody(String instr, Set<Integer> tIdsWOEmptyBodies){
		if(instr.contains(".start()")){
			StringTokenizer st = new StringTokenizer(instr, "\tt.");
			String tIdStr = st.nextToken();
			int tId = Integer.parseInt(tIdStr);
			if(!tIdsWOEmptyBodies.contains(tId))
				return true;
		}
		return false;
	}
}
