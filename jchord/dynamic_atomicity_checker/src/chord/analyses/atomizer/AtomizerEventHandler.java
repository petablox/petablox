/*
 * Copyright (c) 2009-2010, The Hong Kong University of Science & Technology.
 * All rights reserved.
 * Licensed under the terms of the BSD License; see COPYING for details.
 */
package chord.analyses.atomizer;

import chord.project.analyses.DynamicAnalysis;
import chord.runtime.BasicEventHandler;

/**
 * @author Zhifeng Lai (zflai.cn@gmail.com)
 */
public class AtomizerEventHandler extends BasicEventHandler {
	private static DynamicAnalysis analysis;
	
	public synchronized static void getstaticPrimitiveEvent(int eId, Object b, int fId) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int bId = getObjectId(b);
			analysis.processGetstaticPrimitive(eId, tId, bId, fId);
			trace = true;
		}
	}
	
	public synchronized static void getstaticReferenceEvent(int eId, Object b, int fId,
			Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int bId = getObjectId(b);
			int oId = getObjectId(o);
			analysis.processGetstaticReference(eId, tId, bId, fId, oId);
			trace = true;
		}
	}
	
	public synchronized static void putstaticPrimitiveEvent(int eId, Object b, int fId) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int bId = getObjectId(b);
			analysis.processPutstaticPrimitive(eId, tId, bId, fId);
			trace = true;
		}
	}
	
	public synchronized static void putstaticReferenceEvent(int eId, Object b, int fId,
			Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int bId = getObjectId(b);
			int oId = getObjectId(o);
			analysis.processPutstaticReference(eId, tId, bId, fId, oId);
			trace = true;
		}
	}
	
	public synchronized static void getfieldPrimitiveEvent(int eId,
			Object b, int fId) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int bId = getObjectId(b);
			analysis.processGetfieldPrimitive(eId, tId, bId, fId);
			trace = true;
		}
	}
	
	public synchronized static void getfieldReferenceEvent(int eId,
			Object b, int fId, Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int bId = getObjectId(b);
			int oId = getObjectId(o);
			analysis.processGetfieldReference(eId, tId, bId, fId, oId);
			trace = true;
		}
	}
	
	public synchronized static void putfieldPrimitiveEvent(int eId,
			Object b, int fId) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int bId = getObjectId(b);
			analysis.processPutfieldPrimitive(eId, tId, bId, fId);
			trace = true;
		}
	}
	
	public synchronized static void putfieldReferenceEvent(int eId,
			Object b, int fId, Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int bId = getObjectId(b);
			int oId = getObjectId(o);
			analysis.processPutfieldReference(eId, tId, bId, fId, oId);
			trace = true;
		}
	}
	
	public synchronized static void aloadPrimitiveEvent(int eId,
			Object b, int iId) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int bId = getObjectId(b);
			analysis.processAloadPrimitive(eId, tId, bId, iId);
			trace = true;
		}
	}
	
	public synchronized static void aloadReferenceEvent(int eId,
			Object b, int iId, Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int bId = getObjectId(b);
			int oId = getObjectId(o);
			analysis.processAloadReference(eId, tId, bId, iId, oId);
			trace = true;
		}
	}
	
	public synchronized static void astorePrimitiveEvent(int eId,
			Object b, int iId) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int bId = getObjectId(b);
			analysis.processAstorePrimitive(eId, tId, bId, iId);
			trace = true;
		}
	}
	
	public synchronized static void astoreReferenceEvent(int eId,
			Object b, int iId, Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int bId = getObjectId(b);
			int oId = getObjectId(o);
			analysis.processAstoreReference(eId, tId, bId, iId, oId);
			trace = true;
		}
	}
	
	public synchronized static void threadStartEvent(int iId, Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int oId = getObjectId(o);
			analysis.processThreadStart(iId, tId, oId);
			trace = true;
		}
	}
	
	public synchronized static void acquireLockEvent(int lId, Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int oId = getObjectId(o);
			analysis.processAcquireLock(lId, tId, oId);
			trace = true;
		}
	}
	
	public synchronized static void releaseLockEvent(int rId, Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int oId = getObjectId(o);
			analysis.processReleaseLock(rId, tId, oId);
			trace = true;
		}
	}
	
	public synchronized static void enterMainMethodEvent() {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			analysis.processEnterMainMethod(tId);
			trace = true;
		}
	}
	
	public synchronized static void init(String args) {
		try {
			Class<?> t = Class.forName("chord.analyses.atomizer.AtomizerAnalysis");
			analysis = (DynamicAnalysis) t.newInstance();
			analysis.initPass();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		BasicEventHandler.init(args);
	}

	public synchronized static void done() {
		BasicEventHandler.done();
		analysis.donePass();
	}
}
