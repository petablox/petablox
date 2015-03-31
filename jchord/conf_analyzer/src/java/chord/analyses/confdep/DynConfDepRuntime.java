package chord.analyses.confdep;

import chord.analyses.confdep.rels.RelAPIMethod;
import chord.analyses.confdep.rels.RelReadOnlyAPICall;
import chord.analyses.logging.RelLogStmts;
import chord.analyses.type.RelScopeExcludedT;
import chord.project.Config;
import chord.util.IndexMap;
import chord.util.Utils;
import chord.util.WeakIdentityHashMap;
import java.io.*;
import java.util.*;

public class DynConfDepRuntime {
	//extends DynamicAnalysis

	static PrintStream out;
	public static boolean FULL = true;
	protected static WeakIdentityHashMap labels;

	static class TaintList {
		static IndexMap<String> names = new IndexMap<String>();
		BitSet taints = new BitSet();

		public int size() {
			return taints.size();
		}

		public void add(String s) {

			int idx = names.getOrAdd(s);
			taints.set(idx);
		}

		public void addAll(TaintList t) {
			taints.or(t.taints);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();

			for (int i = taints.nextSetBit(0); i >= 0; i = taints.nextSetBit(i+1)) {
				sb.append(names.get(i));
				sb.append("|");
			}
			if(sb.length() > 0)
				sb.deleteCharAt(sb.length() -1);
			return sb.toString();
		}

		public TaintList copy() {
			TaintList t = new TaintList();
			t.addAll(this);
			return t;
		}
	}

	static TaintList EMPTY_TLIST = new TaintList();
	String[] scopeExcludeList; //a hack: explicitly copied into child.
	static {
		try {

			out = new PrintStream(new FileOutputStream(DynConfDep.results));
			labels = new WeakIdentityHashMap();
			
			//args(chord.scopeExclude)
//			scopeExcludeList = Utils.toArray(Config.scopeExcludeStr);

		} catch(IOException e) {
			e.printStackTrace();
		}
		out.println("// runtime event handler alive "  + new Date());
		
		out.flush();
	}

	public static String reformatArray(Object s) {
		Class aType = s.getClass().getComponentType();
		if(!aType.isPrimitive()) {
			Object[] arr = (Object[]) s;
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			for(Object a : arr) {
				if(a != null)
					sb.append(a.toString());
				else 
					sb.append("null");
				sb.append(",");
			}
			if(sb.length() > 0)
				sb.deleteCharAt(sb.length() -1);
			sb.append("}");
			return sb.toString();
		} else {
			return "PRIM-array";
		}
	}

	static HashSet<String> linesPrinted = new HashSet<String>(); //uniqueify

	public static void printOnce(String s) {
		if(linesPrinted.add(s))
			out.println(s);
	}
	

	public static String confOptAtSite(String cname, String mname, int iIdx, Object[] args) {

		boolean isConf = ConfDefines.isConf(cname, mname);
		int cOpt = 0;
		for(; cOpt < args.length; ++cOpt)
			if(args[cOpt] instanceof String)
				break;
		if(isConf && cOpt < args.length) {
			String confOpt = ConfDefines.optionPrefixByName(cname, mname) + (String) args[cOpt] +"-" +iIdx;
			return confOpt;
		}
		return null;
	}
	
	public static void init(String s) {
		
	}
	
	public static void done() {
		
	}

	public synchronized static void beforeMethodCall(int iIdx, String cname, String mname,
			Object tref, Object[] args) {
		String optName = confOptAtSite(cname, mname, iIdx, args);
		if(optName != null) {
			printOnce(iIdx +" calling " + cname + " " + mname + " returns option " + optName + " value=PENDING.");
		}
		
		String thisT = taintStr(tref);
		if(thisT.length() > 0) {
			printOnce(iIdx + " invoking " + cname + " " + mname + " 0=" + thisT);
		}
		
		boolean noTaintedArgs = true;
		for(int i= 0; i < args.length; ++i) {  //then all taints from other args
			String tStr = taintStr(args[i]);
			if(tStr.length() > 0) {
				noTaintedArgs = false;
				if(tref == null)//no this; static method
					printOnce(iIdx + " invoking " + cname + " " + mname + " " + (i) + "=" + tStr);
				else //instance method
					printOnce(iIdx + " invoking " + cname + " " + mname + " " + (i+1) + "=" + tStr);
			}
		}
		if(noTaintedArgs)
			out.println("// " + iIdx +" calling " + cname + " " + mname + " without taint");
	}

	public synchronized static void afterMethodCall(int iIdx, String cname, String mname, Object ret,
			Object tref, Object[] args) {

		//find the first string option; treat that as option name.
		String optName = confOptAtSite(cname, mname, iIdx, args);
		if(optName != null) {

			if(ret != null) {
				String prettyRet;
				if(ret.getClass().isArray())
					prettyRet = reformatArray(ret);
				else 
					prettyRet = ret.toString();
				printOnce(iIdx +" calling " + cname + " " + mname + " returns option " + optName + " value=" + prettyRet);
				addLabel(ret,  optName);
			}
			else
				printOnce(iIdx +" calling " + cname + " " + mname + " returns option " + optName + " value=null");
		} else {
			boolean taintedCall = false;
			TaintList returnTaints = new TaintList();
			returnTaints.addAll(taintlist(tref)); // start by adding all taints from this

			if(RelLogStmts.isLogStmt(cname, mname)) {
				for(int i= 0; i < args.length; ++i)
					if(args[i] != null && args[i] instanceof String)
						out.println("LOG " + mname +" " + args[i]);
			}

			String thisT = taintStr(tref);
			if(thisT.length() > 0) {
//				printOnce(iIdx + " invoking " + cname + " " + mname + " 0=" + thisT);
				taintedCall = true;
			}

			for(int i= 0; i < args.length; ++i) {  //then all taints from other args
				TaintList tList = taintlist(args[i]);
				returnTaints.addAll(tList);
				if(tList.size() > 0) {
					String tStr = tList.toString();

					if(tref == null)
						printOnce("// " + iIdx + " invoked " + cname + " " + mname + " " + (i) + "=" + tStr);
					else
						printOnce("// " + iIdx + " invoked " + cname + " " + mname + " " + (i+1) + "=" + tStr);
					taintedCall = true;
				}
			}

			if(!taintedCall && FULL) {
				out.println("//  call to "+ cname + " " + mname  + " without taint at " + iIdx);
			}


			if(RelAPIMethod.isAPI(cname, mname) ) {
				if(ret == null) {
					printOnce(iIdx + " returns null");
						
				} else {
					setTaints(ret, returnTaints); //mark return value
				}
				//potentially this regardless of whether we returned null
				//Ctors should reach this point
				if(!RelReadOnlyAPICall.isReadOnly(cname, mname))
					setTaints(tref, returnTaints.copy());   //and add taints to this
				
				//DEBUG
			}
			if(cname.equals("org.apache.hadoop.fs.Path") && mname.equals("<init>")) {
				out.println("// Found call to Path <init>, afterwards, tref taints = " + taintStr(tref) + 
						" and returnTaints itself is " + returnTaints.toString());
				if(RelAPIMethod.isAPI(cname, mname))
					out.println("// <init> is an API method");
				if(RelReadOnlyAPICall.isReadOnly(cname, mname))
					out.println("// also a read only API method");
				if(RelScopeExcludedT.isExcluded(cname))
					out.println("// class is excluded from scope");
				else
					out.println("// class is NOT excluded from scope");
			}

		}
		out.flush();
	}


	public synchronized static void astoreReferenceEvent(int eId,Object array, int iId, Object parm) {
		//this message is just for debugging
		//    out.println("store to array with taints: " + taintStr(parm));
		if(array == parm)
			return;
		TaintList taintlist = taintlist(parm);
		addTaints(array, taintlist);
	}

	//load from tainted array/object should taint loaded object
	public synchronized static void aloadReferenceEvent(int eId,Object array, int iId, Object result) {
		//message is just for debugging
		//    out.println("load from array with taints: " + taintStr(array));
		//   out.println("(array contents were " + reformatArray(array) +")");

		if(array == result) //can this happen, if an object points to itself?
			return;

		TaintList taintlist = taintlist(array);
		TaintList objTaintlist = taintlist(result);
		if(taintlist == objTaintlist && taintlist.size() > 0) {
			//this is probably not an error; sensible way to model
			//a getArray conf read method
			
			
//			System.out.println("AAARGH somehow same array is used for taintlist of both array and contents. " +
//					"Array was " + reformatArray(array) + " and content was " + result + 
//					" Taintlist was '" + taintlist+"', size = "+ taintlist.size());
		} else {
			addTaints(result, taintlist);
		}
	}

	private static void setTaints(Object o, TaintList newTList) {
		if(o != null)
			labels.put(o, newTList);    
	}

	//guaranteed to never be null
	public static TaintList taintlist(Object o) {
		if(o == null)
			return EMPTY_TLIST;
		Object l = labels.get(o);
		if(l != null) {
			TaintList l2 = (TaintList) l;
			return l2;
		} else {
			return EMPTY_TLIST;
		}
	}


	public static String taintStr(Object o) {
		if(o == null)
			return "";
		Object l = labels.get(o);
		if(l != null) {
			TaintList l2 = (TaintList) l;
			return l2.toString();
		} else {
			return "";
		}
	}

	public static void addLabel(Object o, String label) {
		if(o == null)
			return;
		if(label.length() < 1) {
			System.out.println("AARGH.  DynConfDep trying to add a zero-length label");
		}
		Object l = labels.get(o);
		if(l != null) {
			((TaintList)l).add(label);
		} else {
			TaintList l2 = new TaintList();
			l2.add(label);
			labels.put(o, l2);
		}
	}


	//adds taints from taintlist to those for object.
	//taintlist is not modified, assuming taintlist isn't already the taint list for o
	private static void addTaints(Object o, TaintList taintlist) {
		if(o == null)
			return;

		Object l = labels.get(o);
		if(l != null) {
			((TaintList) l).addAll(taintlist);
		} else {
			labels.put(o, taintlist.copy());
		}		
	}

	public static void returnReferenceEvent(int pId, Object o) {
		if(FULL)
			out.println("returning " + o + " at " + pId);
	}

}
