package waitnotify;

import chord.util.Utils;

import chord.runtime.BasicEventHandler;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import java.util.List;

public class WNLoggerObserver extends BasicEventHandler {
	protected static List<String> Elist;
	protected static List<String> Ilist;
	protected static List<String> Llist;
	protected static List<String> Rlist;
	protected static String outDirName;

	public synchronized static void putstaticPrimitiveEvent(int eId, Object b, int fId) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int bId = getObjectId(b);
			WNLogger.doPutstaticPrimitive(eId, tId, bId, fId);
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
			WNLogger.doPutstaticReference(eId, tId, bId, fId, oId);
			trace = true;
		}
	}
	public synchronized static void putfieldPrimitiveEvent(int eId,
			Object b, int fId) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int bId = getObjectId(b);
			WNLogger.doPutfieldPrimitive(eId, tId, bId, fId);
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
			WNLogger.doPutfieldReference(eId, tId, bId, fId, oId);
			trace = true;
		}
	}
	public synchronized static void astorePrimitiveEvent(int eId,
			Object b, int iId) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int bId = getObjectId(b);
			WNLogger.doAstorePrimitive(eId, tId, bId, iId);
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
			WNLogger.doAstoreReference(eId, tId, bId, iId, oId);
			trace = true;
		}
	}
	public synchronized static void threadStartEvent(int iId, Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int oId = getObjectId(o);
			WNLogger.doThreadStart(iId, tId, oId);
			trace = true;
		}
	}
	public synchronized static void threadJoinEvent(int iId, Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int oId = getObjectId(o);
			WNLogger.doThreadJoin(iId, tId, oId);
			trace = true;
		}
	}
	public synchronized static void acquireLockEvent(int lId, Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int oId = getObjectId(o);
			WNLogger.doAcquireLock(lId, tId, oId);
			trace = true;
		}
	}
	public synchronized static void releaseLockEvent(int rId, Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int oId = getObjectId(o);
			WNLogger.doReleaseLock(rId, tId, oId);
			trace = true;
		}
	}
	public synchronized static void waitEvent(int iId, Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int oId = getObjectId(o);
			WNLogger.doWait(iId, tId, oId);
			trace = true;
		}
	}
	public synchronized static void notifyEvent(int iId, Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int oId = getObjectId(o);
			WNLogger.doNotify(iId, tId, oId);
			trace = true;
		}
	}
	public synchronized static void notifyAllEvent(int iId, Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int oId = getObjectId(o);
			WNLogger.doNotifyAll(iId, tId, oId);
			trace = true;
		}
	}
	public synchronized static void methodCallAftEvent(int iId, Object o) {
		if (trace) {
			trace = false;
			int tId = getObjectId(Thread.currentThread());
			int oId = getObjectId(o);
			WNLogger.doMethodCallAft(iId, tId, oId);
			trace = true;
		}
	}

	 public synchronized static void init(String args) {
		 outDirName = extractOutDirName(args);
		 Elist = Utils.readFileToList(new File(outDirName, "Elocs.dynamic.txt"));
		 Ilist = Utils.readFileToList(new File(outDirName, "Ilocs.dynamic.txt"));
		 Llist = Utils.readFileToList(new File(outDirName, "Llocs.dynamic.txt"));
		 Rlist = Utils.readFileToList(new File(outDirName, "Rlocs.dynamic.txt"));
		 BasicEventHandler.init(args);
	 }

	 public synchronized static void done() {
	        PrintWriter out = null;
	        try{	
			out = new PrintWriter(new FileWriter(outDirName + File.separator + "Test.java"));
		}
		catch(IOException e){
			System.err.println("IOException while opening file to write Java code");
			e.printStackTrace();
		}
		WNLogger.createJavaCode(out);
		out.close();
	 }

	 private static String extractOutDirName(String args){
		String[] vals = args.split("=");
		int idOfInstrSchemeFileName = -1;
		for(int i = 0; i < vals.length; i++){
			if(vals[i].equals("instr_scheme_file_name")){
				idOfInstrSchemeFileName = i;
				break;
			}
		}
		assert (idOfInstrSchemeFileName != -1);
		String schemeSerFilePath = vals[idOfInstrSchemeFileName+1];
		String[] pathElems = schemeSerFilePath.split("/");
	        String outDirName = "";	
	 	for(int i = 0; i < pathElems.length - 1 ; i++){
			if(pathElems[i].equals("")){
				continue;
			}
			outDirName += "/"+pathElems[i];
		}
		return outDirName;
	 }      
}

