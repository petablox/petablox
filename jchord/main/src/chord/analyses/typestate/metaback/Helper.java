package chord.analyses.typestate.metaback;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Field;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alias.CIObj;
import chord.util.tuple.object.Pair;

public class Helper {

	public static boolean mayPointsTo(Register var, Quad alloc,
			CIPAAnalysis cipa) {
		// CIObj obj = cipa.pointsTo(var);
		// return obj.pts.contains(alloc);
		return pointsTo(var, alloc, cipa);
	}

	private static Map<Register, Set<Quad>> VpointsToMap = new HashMap<Register, Set<Quad>>();
	private static Map<jq_Field, Set<Quad>> FpointsToMap = new HashMap<jq_Field, Set<Quad>>();
	private static Map<Pair<Quad, jq_Field>, Set<Quad>> HFHMap = new HashMap<Pair<Quad, jq_Field>, Set<Quad>>();
	private static Map<Quad,Boolean> msfMap = new HashMap<Quad,Boolean>();

	private static Set<Quad> pointsTo(Set<Quad> quads, jq_Field f,
			CIPAAnalysis cipa) {
		Set<Quad> retQuads = new HashSet<Quad>();
		Set<Quad> temp = new HashSet<Quad>(1);
		for (Quad q : quads) {
			Pair<Quad, jq_Field> p = new Pair<Quad, jq_Field>(q, f);
			Set<Quad> pts = HFHMap.get(p);
			if (pts == null) {
				temp.clear();
				temp.add(q);
				CIObj obj = new CIObj(temp);
				pts = cipa.pointsTo(obj, f).pts;
				HFHMap.put(p, pts);
			}
			retQuads.addAll(pts);
		}
		return retQuads;
	}

	private static boolean pointsTo(Register v, Quad q, CIPAAnalysis cipa) {
		Set<Quad> pts = VpointsToMap.get(v);
		if (pts == null) {
			pts = cipa.pointsTo(v).pts;
			VpointsToMap.put(v, pts);
		}
		return pts.contains(q);
	}

	public static boolean mayStoredInField(Quad q, CIPAAnalysis cipa) {
		Boolean ret = msfMap.get(q);
		if (ret == null) {
			ret = cipa.mayStoreInField(q);
			msfMap.put(q, ret);
		}
		return ret.booleanValue();
	}
	
	private static Set<Quad> pointsTo(Register v, CIPAAnalysis cipa) {
		Set<Quad> pts = VpointsToMap.get(v);
		if (pts == null) {
			pts = cipa.pointsTo(v).pts;
			VpointsToMap.put(v, pts);
		}
		return pts;
	}

	private static Set<Quad> pointsTo(jq_Field f, CIPAAnalysis cipa) {
		Set<Quad> pts = FpointsToMap.get(f);
		if (pts == null) {
			pts = cipa.pointsTo(f).pts;
			FpointsToMap.put(f, pts);
		}
		return pts;
	}
}
