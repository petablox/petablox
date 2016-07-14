package stamp.reporting;

import java.util.HashMap;
import java.util.Map;

import stamp.analyses.JCFLSolverAnalysis;
import stamp.missingmodels.util.StubLookup;
import stamp.missingmodels.util.StubLookup.StubLookupKey;
import stamp.missingmodels.util.StubLookup.StubLookupValue;
import stamp.missingmodels.util.Util.Counter;
import stamp.missingmodels.util.Util.MultivalueMap;
import stamp.missingmodels.util.Util.Pair;
import stamp.missingmodels.util.jcflsolver.Edge;
import stamp.missingmodels.util.jcflsolver.EdgeData;
import stamp.missingmodels.util.jcflsolver.Graph;

/*
 * @author Osbert Bastani
 **/
public class AllMissingModels extends XMLReport {
	public AllMissingModels() {
		super("All Missing-Models");
	}

	public void generate() {
        try {
		Graph g = JCFLSolverAnalysis.g();
		StubLookup s = JCFLSolverAnalysis.s();
		Counter<String> keys = new Counter<String>();
		Map<String,StubLookupValue> values = new HashMap<String,StubLookupValue>(); 
		
		MultivalueMap<Edge,Pair<Edge,Boolean>> positiveWeightEdges = JCFLSolverAnalysis.g().getPositiveWeightEdges("Src2Sink");
		for(Edge edge : positiveWeightEdges.keySet()) {
			for(Pair<Edge,Boolean> pair : positiveWeightEdges.get(edge)) {
				EdgeData data = pair.getX().getData(g);
				
				StubLookupValue info = s.get(new StubLookupKey(data.symbol, data.from, data.to));
				String line;
				if(info != null) {
					line = info.toStringShort();
				} else {
					line = "ERROR_NOT_FOUND";
				}

				keys.increment(line);
				values.put(line, info);
			}
		}
		for(String line : keys.sortedKeySet()) {
			newTuple().addValue(values.get(line).method).addValue(line).addValue("Count: " + Integer.toString(keys.getCount(line)));
		}
        } catch (Exception e) {
            System.err.println("Exception in All Missing Models Generation. Maybe was run without JCFL?");
            e.printStackTrace();
        }
	}
}
