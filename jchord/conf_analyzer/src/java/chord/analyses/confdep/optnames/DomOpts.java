package chord.analyses.confdep.optnames;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.confdep.ConfDefines;
import chord.bddbddb.Rel.RelView;
import chord.analyses.alloc.DomH;
import chord.analyses.argret.DomK;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

@Chord(name = "Opt",
		consumes = { "VH", "VConstFlow", "CnfNodeSucc", "confOpts", "confOptLen", "confOptName"}
)
public class DomOpts extends ProgramDom<String> {

	static Set<Pair<Quad,String>> optSites= null;//the bare option names, without prefix
	public static final String NONE = "UNKNOWN";

	public static Set<Pair<Quad,String>> optSites() {
		if(optSites == null) {
			ClassicProject project = ClassicProject.g();
			DomH domH = (DomH) project.getTrgt("H");

			optSites = computeOptNames("confOpts", "confOptLen", "confOptName", domH);
		}
		return optSites;
	}

	public void fill() {
		System.out.println("constructing opts domain");
		//    super.init();
		getOrAdd(NONE);

		for( Pair<Quad, String> e: optSites()) {
			String prefix = ConfDefines.optionPrefix(e.val0);
			getOrAdd(prefix + e.val1);
		}
	}


	//reconstruct the string at program point quad, using relation logStrings, of form I,Cst,Z
	public static String[] reconcatenate(Quad quad, ProgramRel logStrings, boolean makeRegex, int maxFilled) {
		RelView v = logStrings.getView();
		v.selectAndDelete(0, quad);
		String[] wordsByPos = new String[DomK.MAXZ];

		if(v.size() == 0)
			if(makeRegex)
				return new String[] {".*"};
			else
				return new String[] {"X"};

		for(Pair<String,Integer> t: v.<String,Integer>getAry2ValTuples()) {
			int i = t.val1;
			String optPart = t.val0; //.replace(".", "\\.");//escape periods in regexes?
			if(wordsByPos[i] == null)
				wordsByPos[i] = optPart;
			else 
				wordsByPos[i] = wordsByPos[i]+"|"+optPart;
			maxFilled = Math.max(maxFilled, i);
		}

		StringBuilder sb = new StringBuilder();
		boolean needParens = maxFilled > 0;
		for(int i =0; i < maxFilled+1 ; ++ i) {
			if(wordsByPos[i] != null)
				if(wordsByPos[i].contains("|") && needParens)
					sb.append("(" +wordsByPos[i]+")");
				else
				sb.append(wordsByPos[i]);
			else
				if(makeRegex)
					sb.append(".*");
				else
					sb.append(" X ");
		}
		v.free();

		//prune prefix from options starting with .*
		String trimmed;
		if(sb.toString().startsWith(".*") && (!makeRegex || sb.length() > 2))
			trimmed = sb.substring(2);
		else
			trimmed = sb.toString();

		if(maxFilled == 0) {
			return sb.toString().split("\\|");
		} else {
			return new String[] {trimmed};
		}
	}



	/**
	 * Returns the raw names, without prefix
	 */
	public static Set<Pair<Quad,String>> computeOptNames(String ptRel, String lenRel, String nameRel, DomH domH) {

		ClassicProject Project = ClassicProject.g();

		Set<Pair<Quad,String>> returnedMap = new LinkedHashSet<Pair<Quad,String>>();

		ProgramRel relConfOptStrs =
			(ProgramRel) Project.getTrgt(nameRel);//outputs I, Str, K
		relConfOptStrs.load();
		ProgramRel relConfOptLens =  (ProgramRel) Project.getTrgt(lenRel);
		relConfOptLens.load();
		ProgramRel opts = (ProgramRel) Project.getTrgt(ptRel);
		opts.load();

		ProgramRel vh = (ProgramRel) Project.getTrgt("VH");
		vh.load();
		ProgramRel strs = (ProgramRel) Project.getTrgt("VConstFlow"); //v:V0, cst:StrConst
		strs.load();
		ProgramRel cnfNodeSucc = (ProgramRel) Project.getTrgt("CnfNodeSucc");
		cnfNodeSucc.load();

		for(Object q: opts.getAry1ValTuples()) {
			Quad quad = (Quad) q;

			RelView lenV = relConfOptLens.getView();
			lenV.selectAndDelete(0, quad);
			int lenMax = -1;
			for(Integer aLen: lenV.<Integer>getAry1ValTuples()) {
				if(aLen > lenMax)
					lenMax = aLen;
			}
			String regexStrs[] = reconcatenate(quad, relConfOptStrs, true, lenMax);
			for(String regexStr: regexStrs) {
				System.out.println("reconcatenate found " + regexStr);
				String s  = supplementName(quad, vh, strs, cnfNodeSucc, domH);
				if(s.length() > 1 ) {
					if(s.length() > regexStr.length()) {
						System.out.println("RENAME: changing '"+regexStr + "' to " + s);
						regexStr = s;
					} else if(!s.equals(regexStr)) {
						System.err.println("WARN: rename wants to turn " + regexStr + " into " + s);
					}
				}

				returnedMap.add(new Pair<Quad,String>(quad, regexStr));
			}
		}
		opts.close();
		relConfOptStrs.close();
		relConfOptLens.close();

		vh.close();
		strs.close();
		cnfNodeSucc.close();

		return returnedMap;
	}

	static String supplementName(Quad q, ProgramRel vh, ProgramRel strs, ProgramRel cnfNodeSucc, DomH domH) {

		Quad prevHop = q;
		ArrayList<String> pathParts = new ArrayList<String>();
		int hIdx = domH.indexOf(prevHop);
		while(prevHop != null && hIdx > 0 && cnfNodeSucc.contains(prevHop)) {
			//      System.err.println("Renamer: supplementing quad " + q + " in " +
			//          q.getMethod().getDeclaringClass().getName() + " " + q.getMethod().getName() + ":" + q.getLineNumber());
			Pair<Register,String> prevAndComponent = getCompAt(prevHop, strs,cnfNodeSucc);
			//      System.err.println("Renamer: backtracking on " + prevAndComponent.val0 + " and appending " + prevAndComponent.val1);
			pathParts.add(prevAndComponent.val1);
			prevHop = getPrevHop(vh, prevAndComponent.val0);
			hIdx = domH.indexOf(q);
		}

		StringBuilder res = new StringBuilder();
		int i = pathParts.size() -1;
		//    if( i >= 0)
		//      System.err.println("Renamer: found  " + i + " supplemental path parts");
		for(; i >= 0; --i) {
			res.append(pathParts.get(i));
		}

		return res.toString();
	}

	static private Quad getPrevHop(ProgramRel vh, Register val0) {
		if(val0 == null)
			return null;

		RelView view = vh.getView();
		view.selectAndDelete(0, val0);
		Quad res = null;
		//    System.err.println("Renamer: in getPrevHop, have " + view.size() + " candidate prevs for "
		//        + val0.toString() + " of type " + val0.getType());
		for(Object q: view.<Object>getAry1ValTuples()) {
			if(q instanceof Quad &&  ((Quad) q).getOperator() instanceof Invoke) {
				res = (Quad) q;
				break;
			}
		}
		//    if(res == null)
		//      System.err.println("prev didn't pan out");
		//    else
		//      System.err.println("found prev; it was " + res);
		view.free();
		return res;
	}

	private static Pair<Register, String> getCompAt(Quad prevHop, ProgramRel strs,
			ProgramRel cnfNodeSucc) {
		RelView view = cnfNodeSucc.getView();
		view.selectAndDelete(0, prevHop);
		jq_Method methForComponent = Invoke.getMethod(prevHop).getMethod();


		Register pathPartV = null;
		Register basePart = null;

		if(view.size() < 1 || view.size() > 1) {
			System.err.println("Renamer:  didn't expect cnfNodeSucc to have " + view.size() + " elems at point");
		}
		for(Pair<Register,Register> parms: view.<Register,Register>getAry2ValTuples()) {
			basePart = parms.val0;
			pathPartV = parms.val1;
			break;
		}
		view.free();

		String str="/.*";
		if(pathPartV != null) {
			RelView strsAt = strs.getView();
			strsAt.selectAndDelete(0, pathPartV);
			//      System.err.println("Renamer: in getCompAt, have " + strsAt.size() + " candidate strings");
			StringBuffer sb;
			if(methForComponent.getName().toString().contains("Attribute"))
				sb = new StringBuffer("@");
			else sb= new StringBuffer("/");

			for(String s: strsAt.<String>getAry1ValTuples()) {
				sb.append(s);
				sb.append("|");
			}
			if(sb.length() > 1) {
				sb.deleteCharAt(sb.length() -1);
				str = sb.toString();
			}

			strsAt.free();
		}

		return new Pair<Register,String>(basePart, str);
	}

	public int addPt(Quad quad, String cst) {
		if(optSites == null)
			optSites = new LinkedHashSet< Pair<Quad, String>>();
		int idx = getOrAdd(cst);
		optSites.add( new Pair<Quad,String>(quad, cst));
		return idx;
	}

}
