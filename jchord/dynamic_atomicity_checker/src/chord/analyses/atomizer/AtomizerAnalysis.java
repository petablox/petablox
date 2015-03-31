/*
 * Copyright (c) 2009-2010, The Hong Kong University of Science & Technology.
 * All rights reserved.
 * Licensed under the terms of the BSD License; see COPYING for details.
 */
package chord.analyses.atomizer;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIterator;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TLongIntHashMap;
import gnu.trove.TLongObjectHashMap;

import java.util.Stack;

import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.analyses.DynamicAnalysis;
import chord.runtime.BasicEventHandler;
import chord.project.Messages;

/**
 * Dynamic atomicity violations analysis.
 *
 * @author Zhifeng Lai (zflai.cn@gmail.com)
 */
@Chord(name="atomizer-java")
public class AtomizerAnalysis extends DynamicAnalysis {
	private static final boolean DEBUG = true;	
	
	private InstrScheme scheme;
	
	private TIntIntHashMap thr2AtomBlkCount;
	private TIntIntHashMap thr2CommState;
	private TIntObjectHashMap<Stack<StackTraceElement[]>> thr2AtomBlkStack;
	private TIntObjectHashMap<StackTraceElement[]> thr2CommPoint;
	private StackTraceElement[] violPoint;
	private TIntObjectHashMap<TIntHashSet> thr2Lcks;
	private TLongObjectHashMap<TIntHashSet> mem2AccLcks;
	private TLongObjectHashMap<TIntHashSet> mem2WrtLcks;
	private TLongIntHashMap mem2AccState;
	private TLongIntHashMap mem2OwnerThr;
	private TIntIntHashMap lck2AccState;
	private TIntIntHashMap lck2OwnerThr;	
	
	private IgnoreRentrantLock ignoreRentrantLock;
	
	private boolean stopProcess;
	
	@Override
	public InstrScheme getInstrScheme() {
		if (scheme != null) {
			return scheme;
		}
		scheme = new InstrScheme();
		
		scheme.setGetstaticPrimitiveEvent(true, true, true, true);
		scheme.setGetstaticReferenceEvent(true, true, true, true, false);
		scheme.setPutstaticPrimitiveEvent(true, true, true, true);
		scheme.setPutstaticReferenceEvent(true, true, true, true, false);
		scheme.setGetfieldPrimitiveEvent(true, true, true, true);
		scheme.setGetfieldReferenceEvent(true, true, true, true, false);
		scheme.setPutfieldPrimitiveEvent(true, true, true, true);
		scheme.setPutfieldReferenceEvent(true, true, true, true, false);
		scheme.setAloadPrimitiveEvent(true, true, true, true);
		scheme.setAloadReferenceEvent(true, true, true, true, false);
		scheme.setAstorePrimitiveEvent(true, true, true, true);	
		scheme.setAstoreReferenceEvent(true, true, true, true, false);
		
		scheme.setAcquireLockEvent(true, true, true);
		scheme.setReleaseLockEvent(true, true, true);
		
		scheme.setEnterMainMethodEvent(true);
		
		scheme.setThreadStartEvent(true, true, true);
		
		return scheme;
	}
	
	@Override
	public void initAllPasses() {
	}
	
	@Override
	public void initPass() {
		thr2AtomBlkCount = new TIntIntHashMap();
		thr2CommState = new TIntIntHashMap();
		thr2AtomBlkStack = new TIntObjectHashMap<Stack<StackTraceElement[]>>();
		thr2CommPoint = new TIntObjectHashMap<StackTraceElement[]>();
		violPoint = null;
		thr2Lcks = new TIntObjectHashMap<TIntHashSet>();
		mem2AccLcks = new TLongObjectHashMap<TIntHashSet>();
		mem2WrtLcks = new TLongObjectHashMap<TIntHashSet>();
		mem2AccState = new TLongIntHashMap();
		mem2OwnerThr = new TLongIntHashMap();
		lck2AccState = new TIntIntHashMap();
		lck2OwnerThr = new TIntIntHashMap();
		
		ignoreRentrantLock = new IgnoreRentrantLock(); 
				
		stopProcess = false;
	}

	public void processReadAfter(int t, long m) {
		if (stopProcess) { return; }
		assert (m >= 0);
		
		if (DEBUG) {			
			Messages.log("processReadAfter: t = " + t + ", m = " + m);
		}		
		
		updateMemoryAccessState(t, m, false);
		
		if (mem2AccState.get(m) == MemoryAccessState.SHARED_MODIFIED) {			
			TIntHashSet mwLcks = mem2WrtLcks.get(m);
			TIntHashSet maLcks = mem2AccLcks.get(m);
			TIntHashSet thLcks = thr2Lcks.get(t);
			assert (thLcks != null);
			if (isIntersectionEmpty(mwLcks, thLcks)) {
				maLcks = intersect(maLcks, thLcks);
				mem2AccLcks.put(m, maLcks);
			} else {
				int state = thr2CommState.get(t);
				if (state == CommitState.PRE_COMM) {
					maLcks = intersect(maLcks, thLcks);
					mem2AccLcks.put(m, maLcks);
					thr2CommState.put(t, CommitState.POS_COMM);
				}
				if (state == CommitState.POS_COMM) {
					wrongDataRace();
				}
			}
		}
	}	

	public void processWriteAfter(int t, long m) {
		if (stopProcess) { return; }
		assert (m >= 0);
		
		if (DEBUG) {			
			Messages.log("processWriteAfter: t = " + t + ", m = " + m);
		}
		
		updateMemoryAccessState(t, m, true);
		
		if (mem2AccState.get(m) == MemoryAccessState.SHARED_MODIFIED) {
			TIntHashSet mwLcks = mem2WrtLcks.get(m);
			TIntHashSet maLcks = mem2AccLcks.get(m);
			TIntHashSet thLcks = thr2Lcks.get(t);
			assert (thLcks != null);
			if (isIntersectionEmpty(maLcks, thLcks)) {
				mwLcks = intersect(mwLcks, thLcks);
				mem2WrtLcks.put(m, mwLcks);
				maLcks = intersect(maLcks, thLcks);
				mem2AccLcks.put(m, maLcks);
			} else {
				int state = thr2CommState.get(t); 
				if (state == CommitState.PRE_COMM) {
					mwLcks = intersect(mwLcks, thLcks);
					mem2WrtLcks.put(m, mwLcks);
					maLcks = new TIntHashSet();
					mem2AccLcks.put(m, maLcks);
					thr2CommState.put(t, CommitState.POS_COMM);
				}			
				if (state == CommitState.POS_COMM) {
					wrongDataRace();
				}
			}
		}	
	}
	
	public void processAtomicBegin(int t) {
		if (stopProcess) { return; }
		
		Stack<StackTraceElement[]> atomBlkStack = thr2AtomBlkStack.get(t);
		if (!thr2AtomBlkCount.containsKey(t)) {
			thr2AtomBlkCount.put(t, 0);
			thr2AtomBlkStack.put(t, atomBlkStack = new Stack<StackTraceElement[]>());
		}
		if (thr2AtomBlkCount.get(t) == 0) {
			thr2CommState.put(t, CommitState.PRE_COMM);
		}
		thr2AtomBlkCount.adjustValue(t, 1);
		atomBlkStack.push(Thread.currentThread().getStackTrace());
	}
	
	public void processAtomicEnd(int t) {
		if (stopProcess) { return; }
		
		int value = thr2AtomBlkCount.get(t);
		Stack<StackTraceElement[]> atomBlkStack = thr2AtomBlkStack.get(t);
		assert thr2AtomBlkCount.containsKey(t) && (value > 0);
		if (value > 0) {
			if (value == 1) {
				thr2CommState.put(t, CommitState.OUT_SIDE);
			}
			thr2AtomBlkCount.adjustValue(t, -1);
			atomBlkStack.pop();
		}		
	}
	
	@Override
	public void processAcquireLock(int l, int t, int o) {		
		if (l < 0) { return; }
		if (stopProcess) { return; }
		
		if (DEBUG) {			
			Messages.log("processAcquireLock: t = " + t + ", o = " + o);
		}
		
		// Synch heuristics: All synchronized blocks and synchronized methods
		//                   are atomic.
		processAtomicBegin(t);
		
		if (ignoreRentrantLock.processAcquireLock(t, o)) {
			updateLockAccessState(t, o);
			if (lck2AccState.get(o) == LockAccessState.SHARED_MODIFIED) {
				TIntHashSet lcks = thr2Lcks.get(t);
				lcks.add(o);
				
				if (thr2CommState.get(t) == CommitState.POS_COMM) {
					violPoint = Thread.currentThread().getStackTrace();
					wrongAtomViol(t);
				}
			}
		}
	}
	
	@Override
	public void processReleaseLock(int r, int t, int o) {
		if (r < 0) { return; }
		if (stopProcess) { return; }
		
		if (DEBUG) {
			Messages.log("processReleaseLock: t = " + t + ", o = " + o);
		}
		
		if (ignoreRentrantLock.processReleaseLock(t, o)) {
			updateLockAccessState(t, o);
			if (lck2AccState.get(o) == LockAccessState.SHARED_MODIFIED) {
				TIntHashSet lcks = thr2Lcks.get(t);
				assert lcks.contains(o);
				lcks.remove(o);
				
				if (thr2CommState.get(t) == CommitState.PRE_COMM) {
					thr2CommState.put(t, CommitState.POS_COMM);
					thr2CommPoint.put(t, Thread.currentThread().getStackTrace());
				}
			}
		}
		
		processAtomicEnd(t);
	}

	@Override
	public void processGetstaticPrimitive(int e, int t, int b, int f) {
		processReadAfter(t, BasicEventHandler.getPrimitiveId(b, f));
	}
	
	@Override
	public void processGetstaticReference(int e, int t, int b, int f, int o) {		
		processReadAfter(t, BasicEventHandler.getPrimitiveId(b, f));
	}
	
	@Override
	public void processPutstaticPrimitive(int e, int t, int b, int f) {		
		processWriteAfter(t, BasicEventHandler.getPrimitiveId(b, f));		
	}
	
	@Override
	public void processPutstaticReference(int e, int t, int b, int f, int o) {		
		processWriteAfter(t, BasicEventHandler.getPrimitiveId(b, f));		
	}
	
	@Override
	public void processGetfieldPrimitive(int e, int t, int b, int f) {
		processReadAfter(t, BasicEventHandler.getPrimitiveId(b, f));
	}
	
	@Override
	public void processGetfieldReference(int e, int t, int b, int f, int o) {		
		processReadAfter(t, BasicEventHandler.getPrimitiveId(b, f));
	}
	
	@Override
	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		processWriteAfter(t, BasicEventHandler.getPrimitiveId(b, f));		
	}
	
	@Override
	public void processPutfieldReference(int e, int t, int b, int f, int o) {		
		processWriteAfter(t, BasicEventHandler.getPrimitiveId(b, f));		
	}
	
	@Override
	public void processAloadPrimitive(int e, int t, int b, int i) {
		processReadAfter(t, BasicEventHandler.getPrimitiveId(b, i));
	}
	
	@Override
	public void processAloadReference(int e, int t, int b, int i, int o) {
		processReadAfter(t, BasicEventHandler.getPrimitiveId(b, i));		
	}
	
	@Override
	public void processAstorePrimitive(int e, int t, int b, int i) {
		processWriteAfter(t, BasicEventHandler.getPrimitiveId(b, i));		
	}
	
	@Override
	public void processAstoreReference(int e, int t, int b, int i, int o) {
		processWriteAfter(t, BasicEventHandler.getPrimitiveId(b, i));		
	}	
	
	@Override
	public void processEnterMainMethod(int t) {
		if (DEBUG) {			
			Messages.log("processEnterMainMethod: t = " + t);
		}
		StackTraceElement[] elems = Thread.currentThread().getStackTrace();
		int i = getIndexOfFirstUserMethod(elems);
		assert (elems[i].getMethodName().equals("main"));
		assert (!thr2CommState.containsKey(t));
		thr2CommState.put(t, CommitState.OUT_SIDE);
		assert (!thr2Lcks.containsKey(t));
		thr2Lcks.put(t, new TIntHashSet());
	}
	
	public void processThreadStart(int i, int t, int o) {
		if (i < 0) { return; }
		
		if (DEBUG) {			
			Messages.log("processThreadStart: i = " + i + ", t = " + t + ", o = " + o);
		}
				
		if (!thr2CommState.containsKey(o)) {
			thr2CommState.put(o, CommitState.OUT_SIDE);
		}
		if (!thr2Lcks.containsKey(o)) {
			thr2Lcks.put(o, new TIntHashSet());
		}		
	}
	
	private void updateMemoryAccessState(int t, long m, boolean isWrite) {
		if (!mem2AccState.containsKey(m)) {
			mem2AccState.put(m, MemoryAccessState.THREAD_LOCAL);
			mem2OwnerThr.put(m, t);
			return;
		}
		
		int state = mem2AccState.get(m);
		switch (state) {
		case MemoryAccessState.THREAD_LOCAL:
			if (t != mem2OwnerThr.get(m)) {
				mem2AccState.put(m, MemoryAccessState.THREAD_LOCAL_2);
				mem2OwnerThr.put(m, t);
			}
			break;
		case MemoryAccessState.THREAD_LOCAL_2:
			if (t != mem2OwnerThr.get(m)) {
				if (isWrite) {
					mem2AccState.put(m, MemoryAccessState.SHARED_MODIFIED);					
				} else {
					mem2AccState.put(m, MemoryAccessState.READ_SHARED);
				}
				mem2OwnerThr.put(m, t);
			}
			break;
		case MemoryAccessState.READ_SHARED:
			if (isWrite) {
				mem2AccState.put(m, MemoryAccessState.SHARED_MODIFIED);
				mem2OwnerThr.put(m, t);
			}
			break;
		case MemoryAccessState.SHARED_MODIFIED:
			mem2OwnerThr.put(m, t);
			break;
		default:
			throw new RuntimeException("Invalide memory access state.");
		}
	}
	
	private void updateLockAccessState(int t, int o) {
		if (!lck2AccState.containsKey(o)) {
			lck2AccState.put(o, LockAccessState.THREAD_LOCAL);
			lck2OwnerThr.put(o, t);
			return;
		}
		
		int state = lck2AccState.get(o);
		switch (state) {
		case LockAccessState.THREAD_LOCAL:
			if (t != lck2OwnerThr.get(o)) {
				lck2AccState.put(o, LockAccessState.THREAD_LOCAL_2);
				lck2OwnerThr.put(o, t);
			}
			break;
		case LockAccessState.THREAD_LOCAL_2:
			if (t != lck2OwnerThr.get(o)) {
				lck2AccState.put(o, LockAccessState.SHARED_MODIFIED);
				lck2OwnerThr.put(o, t);
			}
			break;
		case LockAccessState.SHARED_MODIFIED:
			lck2OwnerThr.put(o, t);
			break;
		default:
			throw new RuntimeException("Invalide lock access state.");
		}
	}
	
	private boolean isIntersectionEmpty(TIntHashSet mLcks, TIntHashSet tLcks) {
		if (mLcks == null) { // "null" represents all locks
			return tLcks.isEmpty();
		} else {
			boolean empty = true;
			TIntIterator it = mLcks.iterator();
			while (it.hasNext()) {
				int lck = it.next();
				if (tLcks.contains(lck)) {
					empty = false;
					break;
				}
			}
			return empty;
		}
	}
	
	private TIntHashSet intersect(TIntHashSet mLcks, TIntHashSet tLcks) {
		if (tLcks.isEmpty()) {
			return new TIntHashSet(); // empty lockset
		}
		TIntHashSet tmpLcks = new TIntHashSet();
		if (mLcks == null) { // tmpLcks := tLcks ("null" represents all locks)
			TIntIterator it = tLcks.iterator();
			while (it.hasNext()) {
				tmpLcks.add(it.next());
			}
		} else { // tmpLcks := mLcks \intersect tLcks
			TIntIterator it = mLcks.iterator();
			while (it.hasNext()) {
				int lck = it.next();
				if (tLcks.contains(lck)) {
					tmpLcks.add(lck);
				}
			}
		}
		return tmpLcks;
	}
				
	private void wrongDataRace() {
		Messages.log("Data race");
		stopProcess = true;
	}
	
	private void wrongAtomViol(int t) {		
		// the outmost atomic block
		StackTraceElement[] entePoint = thr2AtomBlkStack.get(t).firstElement();
		StackTraceElement[] commPoint = thr2CommPoint.get(t);
		assert (entePoint != null) && (commPoint != null) && (violPoint != null);
		
		int i = getIndexOfFirstUserMethod(entePoint);
		System.out.println("= * = * = * = * = * = * = * = * = * =");
		System.out.println(entePoint[i].getClassName() + "."
				+ entePoint[i].getMethodName() + " is not atomic");
		System.out.println("\tAtomic block entered:");
		while (i < entePoint.length) {
			System.out.println("\t\tat " + entePoint[i]);
			i++;
		}
		
		System.out.println();
		System.out.println("\tAtomic block commits at lock release:");
		i = getIndexOfFirstUserMethod(commPoint);
		while (i < commPoint.length) {
			System.out.println("\t\tat " + commPoint[i]);
			i++;
		}
		
		System.out.println();
		System.out.println("\tAtomicity violation at lock acquire:");
		i = getIndexOfFirstUserMethod(violPoint);
		while (i < violPoint.length) {
			System.out.println("\t\tat " + violPoint[i]);
			i++;
		}
				
		stopProcess = true;
	}
	
	private int getIndexOfFirstUserMethod(StackTraceElement[] elems) {
		assert (elems != null);
		int i = 0;
		while (i < elems.length) {
			if (elems[i].getClassName().equals(
					"chord.analyses.atomizer.AtomizerEventHandler")) {
				i++;
				break;
			}			
			i++;
		}
		return i;
	}	
}
