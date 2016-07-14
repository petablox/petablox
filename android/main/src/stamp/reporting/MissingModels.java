package stamp.reporting;

import stamp.analyses.JCFLSolverAnalysis;
import stamp.missingmodels.util.ConversionUtils;
import stamp.missingmodels.util.StubLookup;
import stamp.missingmodels.util.StubLookup.StubLookupKey;
import stamp.missingmodels.util.StubLookup.StubLookupValue;
import stamp.missingmodels.util.Util.MultivalueMap;
import stamp.missingmodels.util.Util.Pair;
import stamp.missingmodels.util.jcflsolver.Edge;
import stamp.missingmodels.util.jcflsolver.EdgeData;
import stamp.missingmodels.util.jcflsolver.Graph;

/*
 * @author Osbert Bastani
 **/
public class MissingModels extends XMLReport {
	public MissingModels() {
		super("Missing-Models");
	}

	public void generate() {
		try {
			Graph g = JCFLSolverAnalysis.g();
			StubLookup s = JCFLSolverAnalysis.s();

			MultivalueMap<Edge,Pair<Edge,Boolean>> positiveWeightEdges = JCFLSolverAnalysis.g().getPositiveWeightEdges("Src2Sink");
			for(Edge edge : positiveWeightEdges.keySet()) {
				String source = ConversionUtils.getNodeInfoTokens(this.sourceInfo, edge.from.getName())[1];
				String sink = ConversionUtils.getNodeInfoTokens(this.sourceInfo, edge.to.getName())[1];
				for(Pair<Edge,Boolean> pair : positiveWeightEdges.get(edge)) {
					EdgeData data = pair.getX().getData(g);

					StubLookupValue info = s.get(new StubLookupKey(data.symbol, data.from, data.to));
					String line;
					if(info != null) {
						line = info.toStringShort();
					} else {
						line = "ERROR_NOT_FOUND";
					}

					newTuple().addValue(source + " -> " + sink).addValue(info.method).addValue(line);
				}
			}
		} catch (Exception e) {
			System.err.println("Error in Missing Models Report Generation. Perhaps analysis was run without JCFL?");
			e.printStackTrace();
		}
	}
}
