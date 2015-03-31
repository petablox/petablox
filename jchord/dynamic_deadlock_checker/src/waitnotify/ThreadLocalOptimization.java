package waitnotify;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ThreadLocalOptimization {
	//do not modify thrToInstrs and thrToLockIds
	protected Map<Integer, List<StringIntIntTuple>> thrToInstrs;
	protected Map<Integer, Set<Integer>> thrToLockIds;
	
	private Set<Integer> sharedLocks;
	private Set<Integer> lIdsInWaits;
	
	public ThreadLocalOptimization(Map<Integer, List<StringIntIntTuple>> tToInstrs, 
			Map<Integer, Set<Integer>> tToLIds){
		thrToInstrs = tToInstrs;
		thrToLockIds = tToLIds;
	}
	
	private void computeThreadSharedLocks(){
		sharedLocks = new HashSet<Integer>();
		List<Set<Integer>> locksForThrds = new LinkedList<Set<Integer>>();
		for(Map.Entry<Integer, Set<Integer>> entry : thrToLockIds.entrySet()){
			locksForThrds.add(entry.getValue());
		}
		int size = locksForThrds.size();
		for(int i = 0; i < size-1; i++){
			Set<Integer> lIdsSet1 = locksForThrds.get(i);
			for(int j = i+1; j < size; j++){
				Set<Integer> lIdsSet2 = locksForThrds.get(j);
				Set<Integer> lIdsSet1Copy = new HashSet<Integer>(lIdsSet1);
				lIdsSet1Copy.retainAll(lIdsSet2);
				sharedLocks.addAll(lIdsSet1Copy);
			}
		}
	}
	
	private void computeLIdsInWaits(){
		lIdsInWaits = new HashSet<Integer>();
		for(Map.Entry<Integer, List<StringIntIntTuple>> mEntry : thrToInstrs.entrySet()){
			List<StringIntIntTuple> instrs = mEntry.getValue();
			for(StringIntIntTuple instr : instrs){
				int type = instr.snd;
				if(type == WNLogger.T_WAIT){
					int lId = instr.thrd;
					lIdsInWaits.add(lId);
				}
			}
		}
	}
	
	public Set<Integer> getLIdsAfterOptim(){
		Set<Integer> lIds = new HashSet<Integer>(sharedLocks);
		lIds.addAll(lIdsInWaits);
		return lIds;
	}
	
	protected void optimizeInstrs(){
		computeThreadSharedLocks();
		computeLIdsInWaits();
		for(Map.Entry<Integer, List<StringIntIntTuple>> mEntry : thrToInstrs.entrySet()){
			List<StringIntIntTuple> instrs = mEntry.getValue();
			Iterator<StringIntIntTuple> instrsItr = instrs.iterator();
			while(instrsItr.hasNext()){
				StringIntIntTuple instr = (StringIntIntTuple)instrsItr.next();
				int type = instr.snd;
				if(type != WNLogger.T_OTHER){
					int lId = instr.thrd;
					if(!sharedLocks.contains(lId) && !lIdsInWaits.contains(lId)){
						instrsItr.remove();
					}
				}
			}
		}
	}
}
